# Multi-stage Dockerfile for Spring Boot + Maven
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy project files from subfolder into build container
COPY ["MMO_Market/MMO_Market (3)/MMO_Market/apps/backend", "./apps/backend"]
COPY ["MMO_Market/MMO_Market (3)/MMO_Market/apps/frontend", "./apps/frontend"]

# Build the executable JAR package
WORKDIR /app/apps/backend
RUN mvn clean package -Dmaven.test.skip=true -B

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy built JAR file
COPY --from=build /app/apps/backend/target/mmo-market.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]
