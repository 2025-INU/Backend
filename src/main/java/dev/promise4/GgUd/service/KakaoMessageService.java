package dev.promise4.GgUd.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.promise4.GgUd.controller.dto.ExpenseRecordResponse;
import dev.promise4.GgUd.controller.dto.SettlementResponse;
import dev.promise4.GgUd.controller.dto.SettlementTransferResponse;
import dev.promise4.GgUd.entity.Participant;
import dev.promise4.GgUd.service.kakao.SettlementMessageTemplateBuilder;
import dev.promise4.GgUd.service.kakao.SettlementMessageTemplateBuilder.SettlementPersonalData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 카카오 메시지 전송 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoMessageService {

    private static final String SEND_TO_ME_URL = "https://kapi.kakao.com/v2/api/talk/memo/default/send";

    private final SettlementMessageTemplateBuilder templateBuilder;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    /**
     * 정산 완료 시 모든 참여자에게 카카오톡 알림 전송
     */
    public void sendSettlementMessages(SettlementResponse settlement, List<Participant> participants) {
        for (Participant participant : participants) {
            try {
                sendSettlementMessageToParticipant(settlement, participant);
            } catch (Exception e) {
                log.warn("정산 메시지 전송 실패: userId={}, error={}", participant.getUser().getId(), e.getMessage());
            }
        }
    }

    private void sendSettlementMessageToParticipant(SettlementResponse settlement, Participant participant) {
        Long userId = participant.getUser().getId();
        String kakaoAccessToken = participant.getUser().getKakaoAccessToken();

        if (kakaoAccessToken == null || kakaoAccessToken.isBlank()) {
            log.debug("카카오 액세스 토큰 없음, 메시지 전송 생략: userId={}", userId);
            return;
        }

        ExpenseRecordResponse myExpense = settlement.getExpenses().stream()
                .filter(e -> e.getUserId().equals(userId))
                .findFirst()
                .orElse(null);

        if (myExpense == null) {
            log.debug("지출 기록 없음, 메시지 전송 생략: userId={}", userId);
            return;
        }

        List<SettlementTransferResponse> myTransfers = settlement.getTransfers().stream()
                .filter(t -> t.getFromUserId().equals(userId) || t.getToUserId().equals(userId))
                .collect(Collectors.toList());

        SettlementPersonalData data = new SettlementPersonalData(
                settlement.getPromiseName(),
                myExpense.getPaidAmount(),
                settlement.getPerPersonAmount(),
                myExpense.getBalanceAmount(),
                myTransfers
        );

        Map<String, Object> template = switch (myExpense.getStatus()) {
            case "SENDER" -> templateBuilder.buildSenderMessage(data);
            case "RECEIVER" -> templateBuilder.buildReceiverMessage(data);
            default -> templateBuilder.buildSettledMessage(data);
        };

        sendKakaoMessage(kakaoAccessToken, template, userId);
    }

    private void sendKakaoMessage(String kakaoAccessToken, Map<String, Object> template, Long userId) {
        try {
            String templateJson = objectMapper.writeValueAsString(template);
            String formBody = "template_object=" + java.net.URLEncoder.encode(templateJson, java.nio.charset.StandardCharsets.UTF_8);

            webClientBuilder.build()
                    .post()
                    .uri(SEND_TO_ME_URL)
                    .header("Authorization", "Bearer " + kakaoAccessToken)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .bodyValue(formBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnSuccess(response -> log.info("정산 메시지 전송 성공: userId={}", userId))
                    .doOnError(e -> log.warn("정산 메시지 전송 실패: userId={}, error={}", userId, e.getMessage()))
                    .subscribe();

        } catch (JsonProcessingException e) {
            log.error("메시지 템플릿 직렬화 실패: userId={}", userId, e);
        }
    }
}
