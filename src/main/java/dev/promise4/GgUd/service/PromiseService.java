package dev.promise4.GgUd.service;

import dev.promise4.GgUd.controller.dto.*;
import dev.promise4.GgUd.entity.*;
import dev.promise4.GgUd.exception.*;
import dev.promise4.GgUd.repository.ParticipantRepository;
import dev.promise4.GgUd.repository.PromiseRepository;
import dev.promise4.GgUd.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 약속 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromiseService {

    private final PromiseRepository promiseRepository;
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepository;

    /**
     * 약속 생성
     */
    @Transactional
    public PromiseResponse createPromise(Long userId, CreatePromiseRequest request) {
        User host = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // 약속 생성
        Promise promise = Promise.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .promiseDateTime(request.getPromiseDateTime())
                .host(host)
                .build();

        promise = promiseRepository.save(promise);

        // 호스트를 참여자로 자동 등록
        Participant hostParticipant = Participant.builder()
                .promise(promise)
                .user(host)
                .isHost(true)
                .build();

        participantRepository.save(hostParticipant);

        // 모집 시작
        promise.startRecruiting();

        log.info("Promise created: id={}, title={}, hostId={}",
                promise.getId(), promise.getTitle(), userId);

        return PromiseResponse.from(promise);
    }

    /**
     * 초대 정보 조회
     */
    @Transactional(readOnly = true)
    public PromiseResponse getInviteInfo(String inviteCode) {
        Promise promise = promiseRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new InvalidInviteCodeException(inviteCode));

        return PromiseResponse.from(promise);
    }

    /**
     * 약속 참여 (Pessimistic Lock으로 동시성 제어)
     */
    @Transactional
    public PromiseResponse joinPromise(Long userId, String inviteCode) {
        // Pessimistic Write Lock을 사용하여 동시 참여 요청 시 race condition 방지
        Promise promise = promiseRepository.findByInviteCodeWithLock(inviteCode)
                .orElseThrow(() -> new InvalidInviteCodeException(inviteCode));

        // 초대 만료 검증
        if (!promise.isInviteValid()) {
            throw new InviteExpiredException();
        }

        // 중복 참여 검증
        if (participantRepository.existsByPromiseIdAndUserId(promise.getId(), userId)) {
            throw new AlreadyJoinedException();
        }

        // 최대 참여자 수 검증 (Lock 상태에서 정확한 count 조회)
        long currentCount = participantRepository.countByPromiseId(promise.getId());
        if (currentCount >= promise.getMaxParticipants()) {
            throw new MaxParticipantsExceededException(promise.getMaxParticipants());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // 참여자 등록
        Participant participant = Participant.builder()
                .promise(promise)
                .user(user)
                .isHost(false)
                .build();

        participantRepository.save(participant);

        log.info("User joined promise: promiseId={}, userId={}", promise.getId(), userId);

        return PromiseResponse.from(promise);
    }

    /**
     * 출발지 입력 (RECRUITING 상태에서만 가능)
     */
    @Transactional
    public ParticipantResponse submitDepartureLocation(Long promiseId, Long userId, UpdateDepartureRequest request) {
        Promise promise = promiseRepository.findById(promiseId)
                .orElseThrow(() -> new IllegalArgumentException("약속을 찾을 수 없습니다"));

        if (promise.getStatus() != PromiseStatus.RECRUITING) {
            throw new IllegalStateException("모집 중인 약속에만 출발지를 입력할 수 있습니다. 현재 상태: " + promise.getStatus());
        }

        Participant participant = participantRepository.findByPromiseIdAndUserId(promiseId, userId)
                .orElseThrow(() -> new IllegalArgumentException("참여자를 찾을 수 없습니다"));

        participant.submitDepartureLocation(
                request.getLatitude(),
                request.getLongitude(),
                request.getAddress());

        log.debug("Departure location submitted: promiseId={}, userId={}", promiseId, userId);

        return ParticipantResponse.from(participant);
    }

    /**
     * 중간지점 선택 시작 (호스트 전용)
     * 모든 참여자가 출발지를 입력했을 때만 가능
     */
    @Transactional
    public void startSelectingMidpoint(Long promiseId, Long userId) {
        Promise promise = promiseRepository.findById(promiseId)
                .orElseThrow(() -> new IllegalArgumentException("약속을 찾을 수 없습니다"));

        if (!promise.getHost().getId().equals(userId)) {
            throw new IllegalStateException("호스트만 다음 단계로 진행할 수 있습니다");
        }

        if (promise.getStatus() != PromiseStatus.RECRUITING) {
            throw new IllegalStateException("모집 중 상태에서만 진행할 수 있습니다. 현재 상태: " + promise.getStatus());
        }

        boolean allSubmitted = participantRepository.allParticipantsSubmittedLocation(promiseId);
        if (!allSubmitted) {
            throw new IllegalStateException("모든 참여자가 출발지를 입력해야 합니다");
        }

        promise.startSelectingMidpoint();

        log.info("Midpoint selection started: promiseId={}, hostId={}", promiseId, userId);
    }

    /**
     * 참여자 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ParticipantResponse> getParticipants(Long promiseId) {
        return participantRepository.findByPromiseId(promiseId).stream()
                .map(ParticipantResponse::from)
                .toList();
    }

    /**
     * 내 약속 목록 조회 (키워드 검색 지원)
     */
    @Transactional(readOnly = true)
    public Page<PromiseResponse> getMyPromises(Long userId, PromiseStatus status, String keyword, Pageable pageable) {
        Page<Promise> promises;

        boolean hasKeyword = keyword != null && !keyword.isBlank();

        if (hasKeyword && status != null) {
            promises = promiseRepository.findByUserParticipationAndStatusAndKeyword(userId, status, keyword.trim(), pageable);
        } else if (hasKeyword) {
            promises = promiseRepository.findByUserParticipationAndKeyword(userId, keyword.trim(), pageable);
        } else if (status != null) {
            promises = promiseRepository.findByUserParticipationAndStatus(userId, status, pageable);
        } else {
            promises = promiseRepository.findByUserParticipation(userId, pageable);
        }

        return promises.map(PromiseResponse::from);
    }

    /**
     * 약속 요약 조회 (제목, 일시, 주최자)
     */
    @Transactional(readOnly = true)
    public PromiseSummaryResponse getPromiseSummary(Long promiseId) {
        Promise promise = promiseRepository.findByIdWithHost(promiseId)
                .orElseThrow(() -> new IllegalArgumentException("약속을 찾을 수 없습니다"));

        return PromiseSummaryResponse.from(promise);
    }

    /**
     * 약속 상세 조회
     */
    @Transactional(readOnly = true)
    public PromiseResponse getPromise(Long promiseId) {
        Promise promise = promiseRepository.findByIdWithHost(promiseId)
                .orElseThrow(() -> new IllegalArgumentException("약속을 찾을 수 없습니다"));

        long participantCount = participantRepository.countByPromiseId(promiseId);
        return PromiseResponse.from(promise, participantCount);
    }

    /**
     * 약속 취소 (호스트 전용)
     * COMPLETED, CANCELLED 상태가 아니면 언제든 취소 가능
     */
    @Transactional
    public void cancelPromise(Long promiseId, Long userId) {
        Promise promise = promiseRepository.findById(promiseId)
                .orElseThrow(() -> new IllegalArgumentException("약속을 찾을 수 없습니다"));

        if (!promise.getHost().getId().equals(userId)) {
            throw new IllegalStateException("호스트만 약속을 취소할 수 있습니다");
        }

        promise.cancel();
        log.info("Promise cancelled by host: promiseId={}, hostId={}", promiseId, userId);
    }

    /**
     * 약속 종료 (호스트 전용)
     * IN_PROGRESS 상태에서 호스트가 종료 버튼 누를 때 호출
     */
    @Transactional
    public void completePromise(Long promiseId, Long userId) {
        Promise promise = promiseRepository.findById(promiseId)
                .orElseThrow(() -> new IllegalArgumentException("약속을 찾을 수 없습니다"));

        if (!promise.getHost().getId().equals(userId)) {
            throw new IllegalStateException("호스트만 약속을 종료할 수 있습니다");
        }

        if (promise.getStatus() != PromiseStatus.IN_PROGRESS) {
            throw new IllegalStateException("진행 중인 약속만 종료할 수 있습니다. 현재 상태: " + promise.getStatus());
        }

        promise.complete();
        log.info("Promise completed by host: promiseId={}, hostId={}", promiseId, userId);
    }

    /**
     * 약속 상태 조회
     */
    @Transactional(readOnly = true)
    public PromiseStatus getPromiseStatus(Long promiseId) {
        Promise promise = promiseRepository.findById(promiseId)
                .orElseThrow(() -> new IllegalArgumentException("약속을 찾을 수 없습니다"));

        return promise.getStatus();
    }

}
