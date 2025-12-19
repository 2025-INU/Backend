package dev.promise4.GgUd.controller.dto;

import dev.promise4.GgUd.entity.MidpointVote;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 투표 정보 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "투표 정보")
public class VoteResponse {

    @Schema(description = "투표 ID", example = "1")
    private Long voteId;

    @Schema(description = "참여자 ID", example = "1")
    private Long participantId;

    @Schema(description = "참여자 닉네임", example = "홍길동")
    private String participantNickname;

    @Schema(description = "투표한 역 ID", example = "1")
    private Long stationId;

    @Schema(description = "투표한 역 이름", example = "강남")
    private String stationName;

    @Schema(description = "투표 시간")
    private LocalDateTime votedAt;

    public static VoteResponse from(MidpointVote vote) {
        return VoteResponse.builder()
                .voteId(vote.getId())
                .participantId(vote.getParticipant().getId())
                .participantNickname(vote.getParticipant().getUser().getNickname())
                .stationId(vote.getSubwayStation().getId())
                .stationName(vote.getSubwayStation().getStationName())
                .votedAt(vote.getVotedAt())
                .build();
    }
}
