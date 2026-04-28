package dev.promise4.GgUd.config;

import io.netty.channel.ChannelOption;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class TMapWebClientConfig {

    private final TMapApiProperties tMapApiProperties;

    @Bean(name = "tMapWebClient")
    public WebClient tMapWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, tMapApiProperties.getConnectTimeout())
                .responseTimeout(Duration.ofMillis(tMapApiProperties.getReadTimeout()));

        return WebClient.builder()
                .baseUrl(tMapApiProperties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("appKey", tMapApiProperties.getAppKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
