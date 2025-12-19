# ===================================
# Stage 1: Build Stage
# ===================================
FROM gradle:8.5-jdk17 AS builder

# 작업 디렉토리 설정
WORKDIR /app

# Gradle 캐시를 활용하기 위해 의존성 파일만 먼저 복사
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# 의존성 다운로드 (캐시 레이어)
RUN gradle dependencies --no-daemon || true

# 소스 코드 복사
COPY src ./src

# 애플리케이션 빌드 (테스트 스킵, bootJar 생성)
RUN gradle bootJar --no-daemon -x test

# JAR 파일 위치 확인 및 이름 변경
RUN mv build/libs/*.jar build/libs/app.jar

# ===================================
# Stage 2: Runtime Stage
# ===================================
FROM eclipse-temurin:17-jre

# 메타데이터
LABEL maintainer="ggud-team"
LABEL description="GgUd Meeting Management Application"
LABEL version="1.0"

# 비root 사용자 생성 (보안 강화)
RUN groupadd -r spring && useradd -r -g spring spring

# 작업 디렉토리 설정
WORKDIR /app

# 로그 디렉토리 생성
RUN mkdir -p /app/logs && chown -R spring:spring /app

# 타임존 및 필수 패키지 설치
RUN apt-get update && \
    apt-get install -y --no-install-recommends tzdata wget && \
    ln -sf /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 빌드 스테이지에서 JAR 파일 복사
COPY --from=builder /app/build/libs/app.jar app.jar

# 소유권 변경
RUN chown -R spring:spring /app

# 비root 사용자로 전환
USER spring:spring

# 포트 노출
EXPOSE 8080

# JVM 옵션 설정
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Djava.security.egd=file:/dev/./urandom"

# 헬스체크
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
