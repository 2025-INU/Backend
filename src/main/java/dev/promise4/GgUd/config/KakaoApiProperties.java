package dev.promise4.GgUd.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 카카오 API 설정 프로퍼티
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "kakao.api")
public class KakaoApiProperties {

    /**
     * REST API 키
     */
    private String restApiKey;

    /**
     * JavaScript 키 (프론트엔드용)
     */
    private String javascriptKey;

    /**
     * 카카오 API 베이스 URL
     */
    private String baseUrl = "https://kapi.kakao.com";

    /**
     * 카카오 모빌리티 API 베이스 URL
     */
    private String mobilityBaseUrl = "https://apis-navi.kakaomobility.com";

    /**
     * 연결 타임아웃 (밀리초)
     */
    private int connectTimeout = 5000;

    /**
     * 읽기 타임아웃 (밀리초)
     */
    private int readTimeout = 10000;
}
