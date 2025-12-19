package dev.promise4.GgUd.service;

import dev.promise4.GgUd.client.KakaoApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 카카오 메시지 서비스 (초대 링크 공유)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoMessageService {

    private final KakaoApiClient kakaoApiClient;

    private static final String SEND_MESSAGE_URI = "/v2/api/talk/memo/default/send";

    /**
     * 초대 메시지 전송 (나에게 보내기)
     * 
     * @param userAccessToken 사용자 카카오 액세스 토큰
     * @param promiseTitle    약속 제목
     * @param promiseDateTime 약속 일시
     * @param inviteUrl       초대 링크
     */
    public Mono<Boolean> sendInviteMessage(String userAccessToken, String promiseTitle,
            LocalDateTime promiseDateTime, String inviteUrl) {
        String templateObject = buildInviteTemplate(promiseTitle, promiseDateTime, inviteUrl);
        String formData = "template_object=" + URLEncoder.encode(templateObject, StandardCharsets.UTF_8);

        return kakaoApiClient.postWithUserToken(
                SEND_MESSAGE_URI,
                userAccessToken,
                formData,
                Map.class).map(response -> {
                    log.info("Invite message sent successfully");
                    return true;
                }).onErrorResume(e -> {
                    log.error("Failed to send invite message: {}", e.getMessage());
                    return Mono.just(false);
                });
    }

    /**
     * 메시지 동의 여부 확인
     */
    public Mono<Boolean> checkMessagePermission(String userAccessToken) {
        return kakaoApiClient.get("/v2/api/talk/memo/scopes", Map.class)
                .map(response -> true)
                .onErrorResume(e -> {
                    log.warn("Message permission check failed: {}", e.getMessage());
                    return Mono.just(false);
                });
    }

    /**
     * 초대 메시지 템플릿 생성
     */
    private String buildInviteTemplate(String promiseTitle, LocalDateTime promiseDateTime, String inviteUrl) {
        String formattedDate = promiseDateTime.format(DateTimeFormatter.ofPattern("M월 d일 (E) HH:mm"));

        return String.format("""
                {
                    "object_type": "feed",
                    "content": {
                        "title": "약속에 초대합니다!",
                        "description": "%s\\n📅 %s",
                        "image_url": "https://example.com/promise-invite.png",
                        "link": {
                            "web_url": "%s",
                            "mobile_web_url": "%s"
                        }
                    },
                    "buttons": [
                        {
                            "title": "약속 참여하기",
                            "link": {
                                "web_url": "%s",
                                "mobile_web_url": "%s"
                            }
                        }
                    ]
                }
                """, promiseTitle, formattedDate, inviteUrl, inviteUrl, inviteUrl, inviteUrl);
    }
}
