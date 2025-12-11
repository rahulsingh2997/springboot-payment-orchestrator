# Multi-stage Dockerfile: build with Maven, run with minimal JRE
FROM eclipse-temurin:11-jre
WORKDIR /app
# Copy pre-built jar from host (built via mvn package)
COPY target/springboot-payment-orchestrator-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar","--spring.profiles.active=docker"]
