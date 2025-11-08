# Multi-stage Dockerfile for PYMERP Backend
# Stage 1: Build stage with Gradle
FROM gradle:8.10.2-jdk21-alpine AS builder

WORKDIR /app

# Copy gradle files first for better caching
COPY backend/build.gradle backend/settings.gradle backend/gradlew ./
COPY backend/gradle ./gradle

# Download dependencies (cached if no changes to build.gradle)
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY backend/src ./src
COPY backend/config ./config

# Build application (skip tests in Docker build, run in CI)
RUN gradle clean build -x test --no-daemon

# Stage 2: Runtime stage with JRE
FROM eclipse-temurin:21-jre-alpine

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user
RUN addgroup -g 1001 -S appuser && \
    adduser -u 1001 -S appuser -G appuser

WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Create logs directory with proper permissions
RUN mkdir -p logs && chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Environment variables (overridable)
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
