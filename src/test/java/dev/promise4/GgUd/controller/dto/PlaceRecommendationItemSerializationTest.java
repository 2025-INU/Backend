package dev.promise4.GgUd.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceRecommendationItemSerializationTest {

    @Test
    void aiSummary_isSerializedEvenWhenNull_underNonNullPolicy() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        PlaceRecommendationItem item = new PlaceRecommendationItem();
        item.setPlaceId("2025551608");
        item.setPlaceName("피롤츠커피하우스 시청점");
        item.setAiSummary(null);

        String json = objectMapper.writeValueAsString(item);

        assertThat(json).contains("\"ai_summary\":null");
    }
}
