# ─────────────────────────────────────────────────────────────────────────────
# Multi-stage Dockerfile for NightOut Spring Boot app
#
# WHY MULTI-STAGE?
# Stage 1 (builder): uses Maven + JDK to compile and package the JAR.
#                    This image is ~500MB — too large to ship.
# Stage 2 (runner):  copies ONLY the compiled JAR into a tiny JRE image (~200MB).
#                    The Maven toolchain, source code, and intermediate files
#                    are discarded. The final image is lean and secure.
#
# The result: a ~200MB production image instead of ~700MB.
# ─────────────────────────────────────────────────────────────────────────────

# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /build

# Copy Gradle wrapper first — the wrapper downloads the exact Gradle version
# specified in gradle/wrapper/gradle-wrapper.properties so every developer and
# CI environment uses the same Gradle version automatically.
COPY gradlew .
COPY gradle ./gradle
RUN chmod +x gradlew

# Copy build files before source — same layer-caching trick as Maven:
# if build.gradle.kts hasn't changed, dependency downloads are cached.
COPY build.gradle.kts settings.gradle.kts ./
RUN ./gradlew dependencies --no-daemon   # Pre-download all dependencies

# Now copy source code and build the fat JAR
COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon  # Skip tests, tests run in CI pipeline

# ── Stage 2: Run ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre AS runner

# Run as a non-root user — security best practice
# Never run production services as root inside containers.
RUN groupadd -r nightout && useradd -r -g nightout nightout
USER nightout

WORKDIR /app

# Copy only the fat JAR from the build stage
COPY --from=builder /build/build/libs/nightout.jar app.jar

# Create the logs directory (app writes logs here)
RUN mkdir -p logs

# Expose the port Spring Boot listens on
EXPOSE 8080

# Health check — Docker uses this to determine if the container is healthy.
# /actuator/health returns 200 OK when the app is ready to serve requests.
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# Entrypoint — run the JAR.
# JAVA_OPTS is expanded from the environment variable set in docker-compose.yml
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]