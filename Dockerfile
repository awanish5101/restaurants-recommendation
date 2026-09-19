# syntax=docker/dockerfile:1

# --- build stage ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Cache dependencies first for faster incremental builds
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# --- runtime stage ---
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --create-home appuser
COPY --from=build /app/target/restaurant-0.0.1-SNAPSHOT.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
