FROM gradle:9.2.1-jdk21 AS build
WORKDIR /app

COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon

COPY src ./src
RUN gradle bootJar --no-daemon

FROM bellsoft/liberica-openjre-debian:25-cds AS builder
WORKDIR /builder

ARG JAR_FILE=build/libs/*.jar
COPY --from=build /app/${JAR_FILE} application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

ENTRYPOINT ["java", "-jar", "application.jar"]