package dev.promise4.GgUd.config;

import io.netty.channel.ChannelOption;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * 카카오 API용 WebClient 설정
 */
@Configuration
@RequiredArgsConstructor
public class KakaoWebClientConfig {

    private final KakaoApiProperties kakaoApiProperties;

    @Bean(name = "kakaoWebClient")
    public WebClient kakaoWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, kakaoApiProperties.getConnectTimeout())
                .responseTimeout(Duration.ofMillis(kakaoApiProperties.getReadTimeout()));

        return WebClient.builder()
                .baseUrl(kakaoApiProperties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Authorization", "KakaoAK " + kakaoApiProperties.getRestApiKey())
                .build();
    }

    @Bean(name = "kakaoMobilityWebClient")
    public WebClient kakaoMobilityWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, kakaoApiProperties.getConnectTimeout())
                .responseTimeout(Duration.ofMillis(kakaoApiProperties.getReadTimeout()));

        return WebClient.builder()
                .baseUrl(kakaoApiProperties.getMobilityBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Authorization", "KakaoAK " + kakaoApiProperties.getRestApiKey())
                .build();
    }
}
