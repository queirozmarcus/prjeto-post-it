# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy only the POM first so dependency resolution is cached as a separate layer.
# This layer is only invalidated when pom.xml changes, not when source files change.
COPY pom.xml ./
RUN mvn dependency:go-offline -B -q

# Now copy source and build. Cache miss here only on source changes.
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user before copying any files
RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /app/target/*.jar app.jar

# Ownership set explicitly so the non-root user can read the jar
RUN chown spring:spring app.jar

USER spring:spring

EXPOSE 8080

# -XX:+UseContainerSupport  — reads cgroup memory/cpu limits instead of host values
# -XX:MaxRAMPercentage=75.0 — leaves 25% headroom for the OS and non-heap allocations
# -XX:+ExitOnOutOfMemoryError — crash fast instead of limping; compose restart policy handles recovery
# Spring Boot 3.x handles SIGTERM gracefully when server.shutdown=graceful is set in application.yml
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-jar", "app.jar"]
