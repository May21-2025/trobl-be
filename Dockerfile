FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

ENV JAR_FILE=trobl-0.0.1-SNAPSHOT
ENV PROFILE=dev
ENV ENV=dev

COPY build/libs/${JAR_FILE}.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Duser.timezone=Asia/Seoul -jar /app/app.jar --spring.profiles.active=${PROFILE} --server.env=${ENV}"]
