package dev.promise4.GgUd.scheduler;

import dev.promise4.GgUd.entity.Participant;
import dev.promise4.GgUd.entity.Promise;
import dev.promise4.GgUd.entity.PromiseStatus;
import dev.promise4.GgUd.repository.ParticipantRepository;
import dev.promise4.GgUd.repository.PromiseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 약속 상태 자동 전환 스케줄러
 * - CONFIRMED → IN_PROGRESS: 약속 시간 5분 전 자동 전환
 * - RECRUITING → WAITING_LOCATIONS: 초대 링크 만료 시 자동 전환
 * - 미응답 참여자 알림
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromiseScheduler {

    private final PromiseRepository promiseRepository;
    private final ParticipantRepository participantRepository;

    /**
     * CONFIRMED 상태 약속 → IN_PROGRESS 자동 전환
     * 약속 시간 5분 전에 자동으로 진행 상태로 변경
     * 매 1분마다 실행
     */
    @Scheduled(fixedRate = 60000)  // 1분마다 실행
    @Transactional
    public void autoStartConfirmedPromises() {
        LocalDateTime threshold = LocalDateTime.now().plusMinutes(5);

        List<Promise> promisesToStart = promiseRepository
                .findByStatusAndPromiseDateTimeBefore(PromiseStatus.CONFIRMED, threshold);

        for (Promise promise : promisesToStart) {
            try {
                promise.startProgress();
                log.info("Promise auto-started: promiseId={}, title={}, promiseDateTime={}",
                        promise.getId(), promise.getTitle(), promise.getPromiseDateTime());
            } catch (Exception e) {
                log.error("Failed to auto-start promise: promiseId={}, error={}",
                        promise.getId(), e.getMessage());
            }
        }

        if (!promisesToStart.isEmpty()) {
            log.info("Auto-started {} promises", promisesToStart.size());
        }
    }

    /**
     * 초대 링크 만료된 RECRUITING 상태 약속 처리
     * 만료 시 WAITING_LOCATIONS 상태로 전환
     * 매 1시간마다 실행
     */
    @Scheduled(fixedRate = 3600000)  // 1시간마다 실행
    @Transactional
    public void processExpiredInvites() {
        LocalDateTime now = LocalDateTime.now();

        List<Promise> expiredPromises = promiseRepository.findExpiredRecruitingPromises(now);

        for (Promise promise : expiredPromises) {
            try {
                promise.closeRecruiting();
                log.info("Invite expired, recruiting closed: promiseId={}, title={}, expiredAt={}",
                        promise.getId(), promise.getTitle(), promise.getInviteExpiredAt());
            } catch (Exception e) {
                log.error("Failed to close recruiting for expired promise: promiseId={}, error={}",
                        promise.getId(), e.getMessage());
            }
        }

        if (!expiredPromises.isEmpty()) {
            log.info("Processed {} expired invites", expiredPromises.size());
        }
    }

    /**
     * 출발지 미제출 참여자 알림 (약속 2시간 전)
     * 매 30분마다 실행
     */
    @Scheduled(fixedRate = 1800000)  // 30분마다 실행
    @Transactional(readOnly = true)
    public void remindLocationSubmission() {
        LocalDateTime twoHoursLater = LocalDateTime.now().plusHours(2);

        // WAITING_LOCATIONS 상태이면서 약속 시간이 2시간 이내인 약속 조회
        List<Promise> upcomingPromises = promiseRepository
                .findByStatusAndPromiseDateTimeBefore(PromiseStatus.WAITING_LOCATIONS, twoHoursLater);

        for (Promise promise : upcomingPromises) {
            List<Participant> pendingParticipants = participantRepository
                    .findByPromiseIdAndLocationNotSubmitted(promise.getId());

            if (!pendingParticipants.isEmpty()) {
                log.info("Location submission reminder: promiseId={}, pendingCount={}",
                        promise.getId(), pendingParticipants.size());

                // TODO: 실제 알림 발송 로직 (푸시 알림, WebSocket 등)
                for (Participant participant : pendingParticipants) {
                    log.debug("Reminder needed for: userId={}, promiseId={}",
                            participant.getUser().getId(), promise.getId());
                }
            }
        }
    }

    /**
     * IN_PROGRESS 상태 약속 → COMPLETED 자동 전환
     * 약속 시간 + 3시간 경과 시 자동 완료 처리
     * 매 1시간마다 실행
     */
    @Scheduled(fixedRate = 3600000)  // 1시간마다 실행
    @Transactional
    public void autoCompletePromises() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(3);

        List<Promise> promisesToComplete = promiseRepository
                .findByStatusAndPromiseDateTimeBefore(PromiseStatus.IN_PROGRESS, threshold);

        for (Promise promise : promisesToComplete) {
            try {
                promise.complete();
                log.info("Promise auto-completed: promiseId={}, title={}",
                        promise.getId(), promise.getTitle());
            } catch (Exception e) {
                log.error("Failed to auto-complete promise: promiseId={}, error={}",
                        promise.getId(), e.getMessage());
            }
        }

        if (!promisesToComplete.isEmpty()) {
            log.info("Auto-completed {} promises", promisesToComplete.size());
        }
    }
}
