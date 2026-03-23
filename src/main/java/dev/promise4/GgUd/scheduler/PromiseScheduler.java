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
 * - CONFIRMED → IN_PROGRESS: 약속 시간 1시간 전 자동 전환
 * - 출발지 미제출 참여자 알림 (약속 2시간 전)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromiseScheduler {

    private final PromiseRepository promiseRepository;
    private final ParticipantRepository participantRepository;

    /**
     * CONFIRMED 상태 약속 → IN_PROGRESS 자동 전환
     * 약속 시간 1시간 전에 자동으로 진행 상태로 변경
     * 매 1분마다 실행
     */
    @Scheduled(fixedRate = 60000)  // 1분마다 실행
    @Transactional
    public void autoStartConfirmedPromises() {
        LocalDateTime threshold = LocalDateTime.now().plusHours(1);

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
     * 출발지 미제출 참여자 알림 (약속 2시간 전)
     * RECRUITING 상태에서 아직 출발지를 입력하지 않은 참여자에게 알림
     * 매 30분마다 실행
     */
    @Scheduled(fixedRate = 1800000)  // 30분마다 실행
    @Transactional(readOnly = true)
    public void remindLocationSubmission() {
        LocalDateTime twoHoursLater = LocalDateTime.now().plusHours(2);

        // RECRUITING 상태이면서 약속 시간이 2시간 이내인 약속 조회
        List<Promise> upcomingPromises = promiseRepository
                .findByStatusAndPromiseDateTimeBefore(PromiseStatus.RECRUITING, twoHoursLater);

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

}
