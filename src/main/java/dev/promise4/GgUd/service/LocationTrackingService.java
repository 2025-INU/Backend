package dev.promise4.GgUd.service;

import dev.promise4.GgUd.controller.dto.LocationUpdateMessage;
import dev.promise4.GgUd.controller.dto.ParticipantLocationResponse;
import dev.promise4.GgUd.entity.Participant;
import dev.promise4.GgUd.entity.Promise;
import dev.promise4.GgUd.entity.PromiseStatus;
import dev.promise4.GgUd.entity.User;
import dev.promise4.GgUd.repository.ParticipantRepository;
import dev.promise4.GgUd.repository.PromiseRepository;
import dev.promise4.GgUd.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 실시간 위치 추적 서비스
 * 위치 정보는 Redis에 저장됨
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocationTrackingService {

    private final PromiseRepository promiseRepository;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    // Redis Key 패턴: location:promise:{promiseId}
    private static final String LOCATION_KEY_PREFIX = "location:promise:";
    // 위치 데이터 TTL: 2시간 (약속 종료 후 자동 삭제)
    private static final Duration LOCATION_TTL = Duration.ofHours(2);
    // 도착 감지 반경 (미터)
    private static final double ARRIVAL_RADIUS_METERS = 100.0;

    /**
     * 위치 업데이트 및 도착 감지
     */
    @Transactional
    public LocationUpdateMessage updateLocation(Long promiseId, Long userId, double latitude, double longitude) {
        Promise promise = promiseRepository.findById(promiseId)
                .orElseThrow(() -> new IllegalArgumentException("약속을 찾을 수 없습니다"));

        if (!isTrackingAllowed(promise)) {
            throw new IllegalStateException("위치 공유가 허용되지 않는 상태입니다: " + promise.getStatus());
        }

        if (!isWithinTrackingWindow(promise)) {
            throw new IllegalStateException("위치 공유는 약속 시간 5분 전부터 가능합니다");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        LocationUpdateMessage location = LocationUpdateMessage.of(userId, user.getNickname(), latitude, longitude);

        // Redis Hash에 저장
        String key = LOCATION_KEY_PREFIX + promiseId;
        redisTemplate.opsForHash().put(key, userId.toString(), location);
        redisTemplate.expire(key, LOCATION_TTL);

        // IN_PROGRESS 상태이고 약속 장소가 확정된 경우 도착 감지
        if (promise.getStatus() == PromiseStatus.IN_PROGRESS
                && promise.getConfirmedLatitude() != null
                && promise.getConfirmedLongitude() != null) {
            checkAndMarkArrival(promise, userId, latitude, longitude);
        }

        return location;
    }

    /**
     * 도착 감지 및 처리
     */
    private void checkAndMarkArrival(Promise promise, Long userId, double latitude, double longitude) {
        double distance = calculateDistance(
                latitude, longitude,
                promise.getConfirmedLatitude(), promise.getConfirmedLongitude());

        if (distance > ARRIVAL_RADIUS_METERS) return;

        Participant participant = participantRepository
                .findByPromiseIdAndUserId(promise.getId(), userId)
                .orElse(null);

        if (participant == null || participant.isArrived()) return;

        participant.markAsArrived();

        long arrivedCount = participantRepository.countByPromiseIdAndArrivedAtIsNotNull(promise.getId());
        long totalCount = participantRepository.countByPromiseId(promise.getId());

        // 도착 이벤트 WebSocket 브로드캐스트
        Map<String, Object> arrivalEvent = Map.of(
                "userId", userId,
                "nickname", participant.getUser().getNickname(),
                "arrivedCount", arrivedCount,
                "totalCount", totalCount
        );
        messagingTemplate.convertAndSend(
                "/topic/promises/" + promise.getId() + "/arrivals", arrivalEvent);

        log.info("Arrival detected: promiseId={}, userId={}, distance={}m, arrivedCount={}/{}",
                promise.getId(), userId, (int) distance, arrivedCount, totalCount);
    }

    /**
     * Haversine 공식으로 두 좌표 간 거리 계산 (미터)
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * 참여자 위치 목록 조회
     */
    public ParticipantLocationResponse getParticipantLocations(Long promiseId) {
        String key = LOCATION_KEY_PREFIX + promiseId;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

        List<LocationUpdateMessage> locations = new ArrayList<>();
        for (Object value : entries.values()) {
            if (value instanceof LocationUpdateMessage) {
                locations.add((LocationUpdateMessage) value);
            }
        }

        return ParticipantLocationResponse.builder()
                .promiseId(promiseId)
                .locations(locations)
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    /**
     * 약속 종료 시 위치 데이터 삭제
     */
    public void clearLocations(Long promiseId) {
        String key = LOCATION_KEY_PREFIX + promiseId;
        Boolean deleted = redisTemplate.delete(key);
        log.info("Location cache cleared for promiseId={}, deleted={}", promiseId, deleted);
    }

    /**
     * 위치 추적 가능 상태 확인
     */
    private boolean isTrackingAllowed(Promise promise) {
        PromiseStatus status = promise.getStatus();
        return status == PromiseStatus.PLACE_CONFIRMED || status == PromiseStatus.IN_PROGRESS;
    }

    /**
     * 약속 시간 5분 전부터 추적 가능
     */
    private boolean isWithinTrackingWindow(Promise promise) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime trackingStartTime = promise.getPromiseDateTime().minusMinutes(5);
        return now.isAfter(trackingStartTime);
    }
}
