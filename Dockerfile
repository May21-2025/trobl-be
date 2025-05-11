FROM openjdk:21-jdk-slim

WORKDIR /trobl

# 환경 인자 정의
ARG PROFILE=build
ARG ENV

# 애플리케이션 파일 복사
COPY build/libs/*.jar app.jar

# 환경별 .env 파일 복사 (resources/env 디렉토리에서)
COPY build/resources/main/env/.env.${ENV} .env

EXPOSE 8080
# 실행 명령
CMD ["java", "-jar", "app.jar"]
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=${PROFILE}", "-Dserver.env=${ENV}", "/trobl/app.jar"]
