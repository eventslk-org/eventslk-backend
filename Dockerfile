# ---- Stage 1: Build ----
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copy pom.xml first and download dependencies — this layer is cached
# unless pom.xml changes, so subsequent builds are faster.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build the JAR (skip tests — run them in CI separately)
COPY src/ ./src/
RUN mvn clean package -DskipTests -B

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy only the built JAR from the builder stage
COPY --from=builder /app/target/EventRegistrationAPI-1.0-SNAPSHOT.jar app.jar

EXPOSE 8080

# All secrets and config are supplied as environment variables at runtime.
# Required: DB_NAME, DB_USERNAME, DB_PASSWORD, JWT_SECRET
# Optional (have defaults): DB_HOST (localhost), DB_PORT (3306)
ENTRYPOINT ["java", "-jar", "app.jar"]
