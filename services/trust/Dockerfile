FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
# See services/api-gateway/Dockerfile for why this copies the full monorepo
# context rather than hand-listing sibling module paths.
COPY . .
RUN mvn -B -ntp -q -DskipTests -pl services/trust -am package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S foodloop && adduser -S foodloop -G foodloop
WORKDIR /app
COPY --from=build /workspace/services/trust/target/trust.jar app.jar
USER foodloop
EXPOSE 8091
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
