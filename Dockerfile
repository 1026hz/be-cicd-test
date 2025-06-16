# ┌──────────────────────── Build Stage ────────────────────────┐
FROM gradle:7.6-jdk21 AS builder
WORKDIR /home/gradle/project

# 캐시 활용을 위해 gradle 설정 먼저 복사
COPY build.gradle settings.gradle gradlew gradle.properties ./
COPY gradle ./gradle
RUN chmod +x gradlew

# 소스 복사 후 jar 빌드
COPY src ./src
RUN ./gradlew bootJar --no-daemon

# └─────────────────────── End Build Stage ────────────────────┘


# ┌──────────────────────── Runtime Stage ──────────────────────┐
FROM openjdk:21-jdk-slim
WORKDIR /app

# 빌드 스테이지에서 만든 JAR만 복사
COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
# └─────────────────────── End Runtime Stage ───────────────────┘
