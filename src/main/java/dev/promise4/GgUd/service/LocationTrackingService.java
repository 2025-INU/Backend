package dev.promise4.GgUd.service;

import dev.promise4.GgUd.controller.dto.LocationUpdateMessage;
import dev.promise4.GgUd.controller.dto.ParticipantLocationResponse;
import dev.promise4.GgUd.entity.Promise;
import dev.promise4.GgUd.entity.PromiseStatus;
import dev.promise4.GgUd.entity.User;
import dev.promise4.GgUd.repository.PromiseRepository;
import dev.promise4.GgUd.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

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
    private final RedisTemplate<String, Object> redisTemplate;

    // Redis Key 패턴: location:promise:{promiseId}
    private static final String LOCATION_KEY_PREFIX = "location:promise:";
    // 위치 데이터 TTL: 2시간 (약속 종료 후 자동 삭제)
    private static final Duration LOCATION_TTL = Duration.ofHours(2);

    /**
     * 위치 업데이트
     */
    public LocationUpdateMessage updateLocation(Long promiseId, Long userId, double latitude, double longitude) {
        Promise promise = promiseRepository.findById(promiseId)
                .orElseThrow(() -> new IllegalArgumentException("약속을 찾을 수 없습니다"));

        // 상태 확인 (CONFIRMED ~ IN_PROGRESS)
        if (!isTrackingAllowed(promise)) {
            throw new IllegalStateException("위치 공유가 허용되지 않는 상태입니다: " + promise.getStatus());
        }

        // 시간 확인 (약속 5분 전부터 가능)
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

        log.debug("Location updated in Redis: promiseId={}, userId={}, lat={}, lon={}",
                promiseId, userId, latitude, longitude);

        return location;
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
        return status == PromiseStatus.CONFIRMED || status == PromiseStatus.IN_PROGRESS;
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
