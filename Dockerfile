FROM eclipse-temurin:21-jre-jammy

WORKDIR /app


ENV PATH="build/resoueces/main/.env:${PATH}"

ENV JAR_FILE=trobl-0.0.1-SNAPSHOT
ARG PROFILE=dev
ARG ENV

# 로컬에서 빌드된 JAR 파일만 복사
COPY build/libs/${JAR_FILE}.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
ENTRYPOINT ["java", "-jar", "/app/app.jar", "-Dspring.profiles.active=${PROFILE}", "-Dserver.env=${ENV}"]