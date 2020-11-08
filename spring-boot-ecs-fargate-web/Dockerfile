FROM alpine:latest
RUN apk add openjdk11

COPY . /app
WORKDIR /app

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "target/spring-boot-ecs-fargate-web-1.0-SNAPSHOT.jar"]
