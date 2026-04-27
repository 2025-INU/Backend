package dev.promise4.GgUd.repository;

import dev.promise4.GgUd.entity.AiPlaceRecommendationsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiPlaceRecommendationsRepository extends JpaRepository<AiPlaceRecommendationsEntity, Long> {

    List<AiPlaceRecommendationsEntity> findByPromiseIdOrderByRankingAsc(Long promiseId);

    Optional<AiPlaceRecommendationsEntity> findByPromiseIdAndPlaceId(Long promiseId, String placeId);

    void deleteByPromiseId(Long promiseId);
}

