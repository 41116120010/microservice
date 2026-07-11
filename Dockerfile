FROM maven:3.9.9-eclipse-temurin-17 AS build

ARG SERVICE_DIR
WORKDIR /workspace

COPY ${SERVICE_DIR}/.mvn ${SERVICE_DIR}/.mvn
COPY ${SERVICE_DIR}/mvnw ${SERVICE_DIR}/pom.xml ${SERVICE_DIR}/
RUN chmod +x ${SERVICE_DIR}/mvnw
COPY ${SERVICE_DIR}/src ${SERVICE_DIR}/src
RUN ${SERVICE_DIR}/mvnw -f ${SERVICE_DIR}/pom.xml package -DskipTests

FROM eclipse-temurin:17-jre

ARG SERVICE_DIR
WORKDIR /app
COPY --from=build /workspace/${SERVICE_DIR}/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
