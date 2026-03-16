# Build stage
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src src
RUN mvn package -DskipTests -B

# Run stage — JVM options via JAVA_OPTS (ex: -Xms2g -Xmx24g -XX:+UseG1GC)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN adduser -D -s /bin/sh appuser
USER appuser

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8085
# Permet de surcharger les options JVM depuis docker-compose (production 16c/64G)
ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS:--Xms512m -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200} -jar app.jar"]
