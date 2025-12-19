package dev.promise4.GgUd.service;

import dev.promise4.GgUd.controller.dto.VoteResponse;
import dev.promise4.GgUd.controller.dto.VoteSummaryResponse;
import dev.promise4.GgUd.entity.*;
import dev.promise4.GgUd.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 투표 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoteService {

    private final PromiseRepository promiseRepository;
    private final ParticipantRepository participantRepository;
    private final SubwayStationRepository subwayStationRepository;
    private final MidpointVoteRepository voteRepository;

    /**
     * 투표하기
     */
    @Transactional
    public VoteResponse vote(Long promiseId, Long userId, Long stationId) {
        Promise promise = promiseRepository.findById(promiseId)
                .orElseThrow(() -> new IllegalArgumentException("약속을 찾을 수 없습니다"));

        if (promise.getStatus() != PromiseStatus.SELECTING_MIDPOINT) {
            throw new IllegalStateException("투표 가능한 단계가 아닙니다");
        }

        Participant participant = participantRepository.findByPromiseIdAndUserId(promiseId, userId)
                .orElseThrow(() -> new IllegalArgumentException("참여자가 아닙니다"));

        SubwayStation station = subwayStationRepository.findById(stationId)
                .orElseThrow(() -> new IllegalArgumentException("역을 찾을 수 없습니다"));

        // 기존 투표 확인
        MidpointVote vote = voteRepository.findByPromiseIdAndParticipantId(promiseId, participant.getId())
                .map(existingVote -> {
                    existingVote.changeVote(station);
                    return existingVote;
                })
                .orElseGet(() -> MidpointVote.builder()
                        .promise(promise)
                        .participant(participant)
                        .subwayStation(station)
                        .build());

        voteRepository.save(vote);
        log.info("Vote recorded: promiseId={}, userId={}, stationId={}", promiseId, userId, stationId);

        return VoteResponse.from(vote);
    }

    /**
     * 투표 현황 조회
     */
    @Transactional(readOnly = true)
    public VoteSummaryResponse getVoteSummary(Long promiseId) {
        List<Participant> participants = participantRepository.findByPromiseId(promiseId);
        List<MidpointVote> votes = voteRepository.findByPromiseId(promiseId);

        // 역별 투표 수 집계
        Map<SubwayStation, Long> stationCounts = votes.stream()
                .collect(Collectors.groupingBy(MidpointVote::getSubwayStation, Collectors.counting()));

        List<VoteSummaryResponse.StationVoteCount> stationVoteCounts = stationCounts.entrySet().stream()
                .map(e -> VoteSummaryResponse.StationVoteCount.builder()
                        .stationId(e.getKey().getId())
                        .stationName(e.getKey().getStationName())
                        .count(e.getValue().intValue())
                        .build())
                .sorted((a, b) -> b.getCount() - a.getCount())
                .toList();

        return VoteSummaryResponse.builder()
                .totalParticipants(participants.size())
                .votedCount(votes.size())
                .votes(votes.stream().map(VoteResponse::from).toList())
                .stationVoteCounts(stationVoteCounts)
                .build();
    }

    /**
     * 중간지점 확정 (호스트만)
     */
    @Transactional
    public void confirmMidpoint(Long promiseId, Long userId, Long stationId) {
        Promise promise = promiseRepository.findById(promiseId)
                .orElseThrow(() -> new IllegalArgumentException("약속을 찾을 수 없습니다"));

        // 호스트 확인
        if (!promise.getHost().getId().equals(userId)) {
            throw new IllegalStateException("호스트만 확정할 수 있습니다");
        }

        if (promise.getStatus() != PromiseStatus.SELECTING_MIDPOINT) {
            throw new IllegalStateException("확정 가능한 단계가 아닙니다");
        }

        SubwayStation station = subwayStationRepository.findById(stationId)
                .orElseThrow(() -> new IllegalArgumentException("역을 찾을 수 없습니다"));

        // 확정
        promise.confirmLocation(station.getLatitude(), station.getLongitude(), station.getStationName());

        log.info("Midpoint confirmed: promiseId={}, stationId={}, stationName={}",
                promiseId, stationId, station.getStationName());
    }
}
