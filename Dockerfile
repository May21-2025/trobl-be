FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

ARG PROFILE
ARG ENV

ENV JAR_FILE=trobl-0.0.1-SNAPSHOT
ENV PROFILE=${PROFILE}
ENV ENV=${ENV}

COPY build/libs/${JAR_FILE}.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Dspring.profiles.active=${PROFILE} -Dserver.env=${ENV} -Duser.timezone=Asia/Seoul -jar /app/app.jar"]
