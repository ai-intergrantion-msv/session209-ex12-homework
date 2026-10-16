# Multi-stage Dockerfile for Spring Boot Gradle Application
# Stage 1: Build the JAR
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Copy gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Copy source code and build bootJar
COPY src src
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon

# Stage 2: Runtime Environment
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy executable jar from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose HTTP port 8080
EXPOSE 8080

# Healthcheck for container monitoring
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Start Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
