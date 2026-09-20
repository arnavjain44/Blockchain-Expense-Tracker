# Dockerfile for ExpenseChain Spring Boot Backend (Java 8)
FROM eclipse-temurin:8-jre-focal

WORKDIR /app

# Create unprivileged application user
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Copy the pre-built backend executable jar
COPY build/libs/backend-1.0.0.jar app.jar

# Render automatically supplies $PORT
ENV PORT=8080
EXPOSE 8080

USER appuser

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
