package dev.promise4.GgUd.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 투표 현황 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "투표 현황")
public class VoteSummaryResponse {

    @Schema(description = "전체 참여자 수", example = "5")
    private int totalParticipants;

    @Schema(description = "투표 완료 수", example = "3")
    private int votedCount;

    @Schema(description = "투표 목록")
    private List<VoteResponse> votes;

    @Schema(description = "역별 투표 수")
    private List<StationVoteCount> stationVoteCounts;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "역별 투표 수")
    public static class StationVoteCount {
        @Schema(description = "역 ID", example = "1")
        private Long stationId;

        @Schema(description = "역 이름", example = "강남")
        private String stationName;

        @Schema(description = "투표 수", example = "3")
        private int count;
    }
}
