FROM bellsoft/liberica-openjre-debian:25-cds AS builder
WORKDIR /builder

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} application.jar

ENTRYPOINT ["java", "-jar", "application.jar"]