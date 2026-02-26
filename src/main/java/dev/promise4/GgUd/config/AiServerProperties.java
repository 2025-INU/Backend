package dev.promise4.GgUd.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI 서버(장소 추천) 설정 프로퍼티
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.ai")
public class AiServerProperties {

    /**
     * AI 서버 베이스 URL (예: http://localhost:8000)
     */
    private String serverUrl = "http://localhost:8000";

    /**
     * 연결 타임아웃 (밀리초)
     */
    private int connectTimeout = 5000;

    /**
     * 읽기 타임아웃 (밀리초)
     */
    private int readTimeout = 30000;
}
