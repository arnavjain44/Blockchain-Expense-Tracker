FROM eclipse-temurin:8-jdk-focal AS builder

WORKDIR /app

COPY gradlew .
COPY gradle ./gradle
COPY gradle.properties* ./
COPY settings.gradle .
COPY build.gradle .

COPY contracts-java ./contracts-java
COPY workflows-java ./workflows-java
COPY backend ./backend

RUN chmod +x gradlew
RUN ./gradlew :backend:bootJar --no-daemon

RUN find backend/build/libs -maxdepth 1 -type f -name "*.jar" ! -name "*-plain.jar" -exec cp {} /app/app.jar \;

FROM eclipse-temurin:8-jre-focal

WORKDIR /app

RUN groupadd -r appuser && useradd -r -g appuser appuser

COPY --from=builder /app/app.jar app.jar

ENV PORT=8080
EXPOSE 8080

USER appuser

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
