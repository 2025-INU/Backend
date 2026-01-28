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
     * 출발지 입력
     */
    @Transactional
    public ParticipantResponse submitDepartureLocation(Long promiseId, Long userId, UpdateDepartureRequest request) {
        Participant participant = participantRepository.findByPromiseIdAndUserId(promiseId, userId)
                .orElseThrow(() -> new IllegalArgumentException("참여자를 찾을 수 없습니다"));

        participant.submitDepartureLocation(
                request.getLatitude(),
                request.getLongitude(),
                request.getAddress());

        // 모든 참여자 출발지 입력 완료 시 상태 변경
        checkAndUpdatePromiseStatus(promiseId);

        log.debug("Departure location submitted: promiseId={}, userId={}", promiseId, userId);

        return ParticipantResponse.from(participant);
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
     * 내 약속 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<PromiseResponse> getMyPromises(Long userId, PromiseStatus status, Pageable pageable) {
        Page<Promise> promises;

        if (status != null) {
            promises = promiseRepository.findByHostIdAndStatus(userId, status, pageable);
        } else {
            promises = promiseRepository.findByUserParticipation(userId, pageable);
        }

        return promises.map(PromiseResponse::from);
    }

    /**
     * 약속 상세 조회
     */
    @Transactional(readOnly = true)
    public PromiseResponse getPromise(Long promiseId) {
        Promise promise = promiseRepository.findById(promiseId)
                .orElseThrow(() -> new IllegalArgumentException("약속을 찾을 수 없습니다"));

        return PromiseResponse.from(promise);
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

    /**
     * 모든 참여자 출발지 입력 완료 시 상태 변경
     */
    private void checkAndUpdatePromiseStatus(Long promiseId) {
        Promise promise = promiseRepository.findById(promiseId)
                .orElseThrow(() -> new IllegalArgumentException("약속을 찾을 수 없습니다"));

        if (promise.getStatus() == PromiseStatus.WAITING_LOCATIONS) {
            boolean allSubmitted = participantRepository.allParticipantsSubmittedLocation(promiseId);
            if (allSubmitted) {
                promise.startSelectingMidpoint();
                log.info("All participants submitted locations, status changed: promiseId={}", promiseId);
            }
        }
    }
}
