# Si le JAR est déjà construit localement, on peut l'utiliser directement
# Option 1: Utiliser le JAR local pré-construit (plus rapide)
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/backend.jar backend.jar
COPY src/main/resources/serviceAccountKey.json /app/serviceAccountKey.json
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "backend.jar"]

# Option 2: Construire depuis les sources (décommenter si besoin)
# FROM maven:3.9.2-eclipse-temurin-17 AS build
# WORKDIR /app
# COPY pom.xml .
# COPY src ./src
# RUN mvn clean package -DskipTests
#
# FROM eclipse-temurin:17-jre
# WORKDIR /app
# COPY --from=build /app/target/*.jar backend.jar
# EXPOSE 8080
# ENTRYPOINT ["java", "-jar", "backend.jar"]