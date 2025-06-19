FROM maven:3.8-openjdk-11 AS builder

WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY k8s ./k8s

# Build the application
RUN mvn clean package -DskipTests

FROM openjdk:11-jre-slim

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
COPY --from=builder /app/k8s ./k8s

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"] 