# Multi-stage Dockerfile: build with Maven, run with minimal JRE
FROM maven:3.8.7-jdk-11 AS build
WORKDIR /workspace
# copy pom and maven wrapper if present
COPY pom.xml ./
COPY mvnw .
COPY .mvn .mvn
# copy sources
COPY src/ ./src/
RUN mvn -B -DskipTests clean package -U

FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
