# Multi-stage Dockerfile for Spring Boot + Maven
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy only the sources required by the backend build
COPY apps/backend ./apps/backend
COPY apps/frontend ./apps/frontend

# Build the executable Spring Boot JAR.
# Test compilation is skipped because deployment should not depend on test sources.
WORKDIR /app/apps/backend
RUN mvn clean package -DskipTests=true -Dmaven.test.skip=true

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/apps/backend/target/mmo-market.jar mmo-market.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "mmo-market.jar"]
