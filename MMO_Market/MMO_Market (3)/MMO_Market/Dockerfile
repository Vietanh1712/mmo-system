# Multi-stage Dockerfile for Spring Boot + Maven
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy project root pom and app sources
COPY pom.xml .
COPY apps/backend ./apps/backend
COPY apps/frontend ./apps/frontend

# Build the executable WAR/JAR package
WORKDIR /app/apps/backend
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy built WAR file
COPY --from=build /app/apps/backend/target/mmo-market.war app.war

# Render provides PORT env variable dynamically
EXPOSE 8080

ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "app.war"]
