FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY services/api-gateway/pom.xml services/api-gateway/pom.xml
COPY services/api-gateway/src services/api-gateway/src
RUN mvn -f services/api-gateway/pom.xml -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S foodloop && adduser -S foodloop -G foodloop
WORKDIR /app
COPY --from=build /workspace/services/api-gateway/target/api-gateway.jar app.jar
USER foodloop
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
