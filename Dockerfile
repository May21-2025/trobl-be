
FROM openjdk:21-jdk-slim AS builder
WORKDIR /build
ARG ENV
ENV ENV=${ENV}
COPY . .
RUN ./gradlew clean build -x test -Denv=${ENV}

FROM eclipse-temurin:21-jre-jammy
WORKDIR /trobl
ARG ENV
ENV ENV=${ENV}

# 애플리케이션 파일 복사
COPY --from=builder /build/build/libs/*.jar app.jar
COPY --from=builder /build/build/resources/main/env/.env.${ENV} .env

EXPOSE 8080
# 실행 명령
CMD ["java", "-jar", "app.jar"]
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=${ENV}", "-Dserver.env=${ENV}", "/trobl/app.jar"]
