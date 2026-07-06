# =============================================================================
# Commerce Insight AI — Backend Dockerfile
# Multi-stage build: Builder stage compiles the JAR, Runtime stage runs it.
# =============================================================================

# ── Stage 1: Build ────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and POM first (Docker layer cache optimization)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies (cached layer if pom.xml doesn't change)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B -q

# Copy source and build
COPY src ./src
RUN ./mvnw package -DskipTests -B -q

# ── Stage 2: Runtime ─────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime

# Security: run as non-root user
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Set ownership
RUN chown -R appuser:appgroup /app

USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# JVM tuning for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:InitialRAMPercentage=50.0 \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.profiles.active=prod"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
