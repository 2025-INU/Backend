package dev.promise4.GgUd.repository;

import dev.promise4.GgUd.entity.AiPlaceRecommendationsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiPlaceRecommendationsRepository extends JpaRepository<AiPlaceRecommendationsEntity, Long> {

    List<AiPlaceRecommendationsEntity> findByPromiseIdOrderByRankingAsc(Long promiseId);

    void deleteByPromiseId(Long promiseId);
}

