package dev.promise4.GgUd.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "tmap.api")
public class TMapApiProperties {

    private String appKey;
    private String baseUrl = "https://apis.openapi.sk.com";
    private int connectTimeout = 5000;
    private int readTimeout = 10000;
}
