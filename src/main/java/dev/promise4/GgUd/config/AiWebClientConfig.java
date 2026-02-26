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
 * AI 서버(장소 추천) 호출용 WebClient 설정
 */
@Configuration
@RequiredArgsConstructor
public class AiWebClientConfig {

    private final AiServerProperties aiServerProperties;

    @Bean(name = "aiServerWebClient")
    public WebClient aiServerWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, aiServerProperties.getConnectTimeout())
                .responseTimeout(Duration.ofMillis(aiServerProperties.getReadTimeout()));

        return WebClient.builder()
                .baseUrl(aiServerProperties.getServerUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
