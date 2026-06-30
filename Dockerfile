# ============================================================================
# CES Service — Backend Dockerfile (multi-stage)  — SRS §89-90
# ============================================================================

# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Cache dependencies first (wrapper + pom), then copy sources.
COPY .mvn ./.mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline -DskipTests || true

COPY src ./src
RUN ./mvnw -B package -DskipTests

# ── Stage 2: Development runtime (hot-reload / remote debug) ─────────────────
FROM eclipse-temurin:21-jdk-alpine AS dev
WORKDIR /app
COPY .mvn ./.mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline -DskipTests || true
COPY src ./src
EXPOSE 8080 5005
ENV SPRING_PROFILES_ACTIVE=dev
ENTRYPOINT ["./mvnw", "spring-boot:run", \
    "-Dspring-boot.run.jvmArguments=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"]

# ── Stage 3: Production runtime ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS prod
WORKDIR /app
RUN addgroup -S ces && adduser -S ces -G ces
USER ces
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "app.jar"]
