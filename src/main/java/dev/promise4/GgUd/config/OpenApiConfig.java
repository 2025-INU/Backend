package dev.promise4.GgUd.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) 설정
 * JWT 인증을 위한 Security Scheme 정의
 */
@Configuration
public class OpenApiConfig {

        @Bean
        public OpenAPI openAPI() {
                // Security Scheme 이름
                String securitySchemeName = "bearerAuth";

                // Security Requirement 추가
                SecurityRequirement securityRequirement = new SecurityRequirement()
                                .addList(securitySchemeName);

                // Security Scheme 정의 (JWT Bearer Token)
                SecurityScheme securityScheme = new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT 토큰을 입력하세요. 카카오 로그인 후 받은 accessToken을 사용합니다.");

                return new OpenAPI()
                                .info(new Info()
                                                .title("GgUd API")
                                                .version("1.0")
                                                .description("GgUd 약속 관리 시스템 API 문서"))
                                .servers(List.of(
                                                new Server().url("http://localhost:8080").description("로컬 서버"),
                                                new Server().url("https://api.ggud.com").description("운영 서버")))
                                .addSecurityItem(securityRequirement)
                                .components(new Components()
                                                .addSecuritySchemes(securitySchemeName, securityScheme));
        }
}
