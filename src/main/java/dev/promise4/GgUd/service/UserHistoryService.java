package dev.promise4.GgUd.service;

import dev.promise4.GgUd.entity.UserPlaceHistory;
import dev.promise4.GgUd.entity.UserQueryHistory;
import dev.promise4.GgUd.repository.UserPlaceHistoryRepository;
import dev.promise4.GgUd.repository.UserQueryHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserHistoryService {

    private final UserQueryHistoryRepository userQueryHistoryRepository;
    private final UserPlaceHistoryRepository userPlaceHistoryRepository;

    @Transactional
    public UserQueryHistory saveQueryHistory(Long userId, String query) {
        UserQueryHistory history = UserQueryHistory.builder()
                .userId(userId)
                .queryText(query)
                .build();
        UserQueryHistory saved = userQueryHistoryRepository.save(history);
        log.debug("Query history saved: userId={}, queryId={}", userId, saved.getId());
        return saved;
    }

    @Transactional
    public void savePlaceHistory(Long userId, String placeId, Long queryId) {
        UserPlaceHistory history = UserPlaceHistory.builder()
                .userId(userId)
                .placeId(placeId)
                .queryId(queryId)
                .build();
        userPlaceHistoryRepository.save(history);
        log.debug("Place history saved: userId={}, placeId={}, queryId={}", userId, placeId, queryId);
    }

    @Transactional(readOnly = true)
    public List<String> getRecentQueries(Long userId) {
        return userQueryHistoryRepository.findTop10ByUserIdOrderBySearchedAtDesc(userId)
                .stream()
                .map(UserQueryHistory::getQueryText)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> getRecentPlaceIds(Long userId) {
        return userPlaceHistoryRepository.findTop20ByUserIdOrderBySelectedAtDesc(userId)
                .stream()
                .map(UserPlaceHistory::getPlaceId)
                .toList();
    }
}
