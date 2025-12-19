package dev.promise4.GgUd.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * 카카오 API 클라이언트
 */
@Slf4j
@Component
public class KakaoApiClient {

    private final WebClient kakaoWebClient;
    private final WebClient kakaoMobilityWebClient;

    public KakaoApiClient(
            @Qualifier("kakaoWebClient") WebClient kakaoWebClient,
            @Qualifier("kakaoMobilityWebClient") WebClient kakaoMobilityWebClient) {
        this.kakaoWebClient = kakaoWebClient;
        this.kakaoMobilityWebClient = kakaoMobilityWebClient;
    }

    /**
     * 카카오 API GET 요청
     */
    public <T> Mono<T> get(String uri, Class<T> responseType) {
        return kakaoWebClient.get()
                .uri(uri)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .flatMap(body -> {
                            log.error("Kakao API error: {} - {}", response.statusCode(), body);
                            return Mono.error(new KakaoApiException(response.statusCode().value(), body));
                        }))
                .bodyToMono(responseType)
                .retryWhen(Retry.backoff(3, Duration.ofMillis(500))
                        .filter(this::isRetryable))
                .doOnError(e -> log.error("Kakao API call failed: {}", e.getMessage()));
    }

    /**
     * 카카오 API POST 요청
     */
    public <T, R> Mono<T> post(String uri, R body, Class<T> responseType) {
        return kakaoWebClient.post()
                .uri(uri)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .flatMap(responseBody -> {
                            log.error("Kakao API error: {} - {}", response.statusCode(), responseBody);
                            return Mono.error(new KakaoApiException(response.statusCode().value(), responseBody));
                        }))
                .bodyToMono(responseType)
                .retryWhen(Retry.backoff(3, Duration.ofMillis(500))
                        .filter(this::isRetryable))
                .doOnError(e -> log.error("Kakao API call failed: {}", e.getMessage()));
    }

    /**
     * 카카오 API POST (폼 데이터)
     */
    public <T> Mono<T> postForm(String uri, String formData, Class<T> responseType) {
        return kakaoWebClient.post()
                .uri(uri)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue(formData)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .flatMap(body -> {
                            log.error("Kakao API error: {} - {}", response.statusCode(), body);
                            return Mono.error(new KakaoApiException(response.statusCode().value(), body));
                        }))
                .bodyToMono(responseType)
                .retryWhen(Retry.backoff(3, Duration.ofMillis(500))
                        .filter(this::isRetryable));
    }

    /**
     * 카카오 모빌리티 API (길찾기)
     */
    public <T> Mono<T> getDirections(String uri, Class<T> responseType) {
        return kakaoMobilityWebClient.get()
                .uri(uri)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .flatMap(body -> {
                            log.error("Kakao Mobility API error: {} - {}", response.statusCode(), body);
                            return Mono.error(new KakaoApiException(response.statusCode().value(), body));
                        }))
                .bodyToMono(responseType)
                .retryWhen(Retry.backoff(3, Duration.ofMillis(500))
                        .filter(this::isRetryable));
    }

    /**
     * 사용자 액세스 토큰으로 요청 (메시지 전송용)
     */
    public <T> Mono<T> postWithUserToken(String uri, String userAccessToken, Object body, Class<T> responseType) {
        return kakaoWebClient.post()
                .uri(uri)
                .header("Authorization", "Bearer " + userAccessToken)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .flatMap(responseBody -> {
                            log.error("Kakao API error: {} - {}", response.statusCode(), responseBody);
                            return Mono.error(new KakaoApiException(response.statusCode().value(), responseBody));
                        }))
                .bodyToMono(responseType);
    }

    /**
     * 재시도 가능 여부 판단
     */
    private boolean isRetryable(Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            // 5xx 에러만 재시도
            return ex.getStatusCode().is5xxServerError();
        }
        return false;
    }

    /**
     * 카카오 API 예외
     */
    public static class KakaoApiException extends RuntimeException {
        private final int statusCode;

        public KakaoApiException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}
