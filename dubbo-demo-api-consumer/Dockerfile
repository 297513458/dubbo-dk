# dockerfile 基础配置
FROM openjdk:8-jre
MAINTAINER daxingxing
VOLUME /tmp
COPY target/*.jar /app/app.jar
WORKDIR /app/
EXPOSE 20880
ENTRYPOINT ["/usr/bin/java","-jar","./app.jar"]