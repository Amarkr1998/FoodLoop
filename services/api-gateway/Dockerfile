FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
# Full monorepo context (see .dockerignore for what's excluded) so Maven's
# reactor can resolve every module declared in the root pom.xml, even
# though -pl/-am below only actually builds this service and what it
# depends on. Keeps every service's Dockerfile identical in shape as more
# modules are added in later phases, instead of hand-listing sibling paths.
COPY . .
RUN mvn -B -ntp -q -DskipTests -pl services/api-gateway -am package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S foodloop && adduser -S foodloop -G foodloop
WORKDIR /app
COPY --from=build /workspace/services/api-gateway/target/api-gateway.jar app.jar
USER foodloop
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
