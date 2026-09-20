# Build the Spring Boot gateway with the same Java/Gradle baseline as Corda 4.11.
FROM gradle:6.9.3-jdk8 AS build
WORKDIR /workspace
COPY . .
RUN chmod +x gradlew && ./gradlew :backend:bootJar --no-daemon

FROM openjdk:8-jre
WORKDIR /app
COPY --from=build /workspace/backend/build/libs/backend-*.jar /app/app.jar
ENV SERVER_PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseG1GC", "-Xms256m", "-Xmx768m", "-jar", "/app/app.jar"]
