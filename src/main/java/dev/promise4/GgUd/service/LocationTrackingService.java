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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 실시간 위치 추적 서비스
 * 위치 정보는 메모리에 캐싱됨
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocationTrackingService {

    private final PromiseRepository promiseRepository;
    private final UserRepository userRepository;

    // 약속별 참여자 위치 캐시 (promiseId -> (userId -> location))
    private final Map<Long, Map<Long, LocationUpdateMessage>> locationCache = new ConcurrentHashMap<>();

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

        // 캐시에 저장
        locationCache
                .computeIfAbsent(promiseId, k -> new ConcurrentHashMap<>())
                .put(userId, location);

        log.debug("Location updated: promiseId={}, userId={}, lat={}, lon={}",
                promiseId, userId, latitude, longitude);

        return location;
    }

    /**
     * 참여자 위치 목록 조회
     */
    public ParticipantLocationResponse getParticipantLocations(Long promiseId) {
        Map<Long, LocationUpdateMessage> locations = locationCache.getOrDefault(promiseId, Map.of());

        return ParticipantLocationResponse.builder()
                .promiseId(promiseId)
                .locations(List.copyOf(locations.values()))
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    /**
     * 약속 종료 시 위치 데이터 삭제
     */
    public void clearLocations(Long promiseId) {
        locationCache.remove(promiseId);
        log.info("Location cache cleared for promiseId={}", promiseId);
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
