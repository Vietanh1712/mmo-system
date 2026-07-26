# Multi-stage Dockerfile for Spring Boot + Maven at Git repo root
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy project files from subfolder into build container using JSON array syntax for paths with spaces
COPY ["MMO_Market/MMO_Market (3)/MMO_Market/pom.xml", "./pom.xml"]
COPY ["MMO_Market/MMO_Market (3)/MMO_Market/apps/backend", "./apps/backend"]
COPY ["MMO_Market/MMO_Market (3)/MMO_Market/apps/frontend", "./apps/frontend"]

# Build the executable WAR package
WORKDIR /app/apps/backend
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy built WAR file
COPY --from=build /app/apps/backend/target/mmo-market.war app.war

EXPOSE 8080

ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "app.war"]
