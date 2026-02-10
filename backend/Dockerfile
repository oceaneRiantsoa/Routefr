# Multi-stage build : compilation depuis les sources
FROM maven:3.9.2-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar backend.jar
COPY src/main/resources/serviceAccountKey.json /app/serviceAccountKey.json
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "backend.jar"]