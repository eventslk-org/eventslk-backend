# syntax=docker/dockerfile:1.7
###############################################################################
# EventsLK :: event-registration-api — Java 21 / Spring Boot 3.5.14
# Multi-stage, digest-pinned, non-root, healthchecked.
###############################################################################

# ---------- Stage 1: Build ----------
# maven:3.9-eclipse-temurin-21 (digest resolved 2026-06-08), pinned by digest.
FROM maven@sha256:d7e7f57407437c014571f1ad5a9955f03fc3edcb1d964067ef351fa38e798665 AS build

WORKDIR /workspace

# POM first -> dependency layer is cached unless pom.xml changes.
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp clean package -DskipTests

# ---------- Stage 2: Runtime ----------
# eclipse-temurin:21-jre-jammy (digest resolved 2026-06-08), pinned by digest.
FROM eclipse-temurin@sha256:199aebeb3adcde4910695cdebfe782ada38dadb6cc8013159b58d3724451befd AS runtime

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 spring \
    && useradd  --system --uid 10001 --gid spring --home-dir /app --no-create-home spring

WORKDIR /app

# *.jar matches the repackaged fat JAR (event-registration-api-1.0-SNAPSHOT.jar)
# and skips the *.jar.original sidecar produced by spring-boot-maven-plugin.
COPY --from=build --chown=spring:spring /workspace/target/*.jar app.jar

USER 10001:10001

# SERVER_PORT defaults to 8081 in application.yml.
EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
    CMD curl -fsS http://127.0.0.1:8081/actuator/health || exit 1

# All secrets/config are injected as env vars at runtime — never baked in.
# Required: DB_NAME, DB_USERNAME, DB_PASSWORD, JWT_SECRET
# Optional: DB_HOST (localhost), DB_PORT (5432), SERVER_PORT (8081)
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
