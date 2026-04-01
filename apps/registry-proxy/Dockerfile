FROM maven:3-eclipse-temurin-17@sha256:6ca4f2d4c7a8affa4a7cdec526dd2aa5d433ddd63f8ed3337a543408136b174e AS builder

WORKDIR /src

COPY --link ./apps/registry-proxy/pom.xml ./apps/registry-proxy/pom.xml
COPY --link ./apps/registry-proxy/connector-api ./apps/registry-proxy/connector-api
COPY --link ./apps/registry-proxy/connector ./apps/registry-proxy/connector
COPY --link ./apps/registry-proxy/core ./apps/registry-proxy/core
COPY --link ./apps/registry-proxy/web ./apps/registry-proxy/web
COPY --link ./apps/registry-proxy/app ./apps/registry-proxy/app

RUN echo "<settings>\n" \
    "<servers>\n" \
    "<server>\n" \
    "<id>\${repositoryId}</id>\n" \
    "<username>\${repoLogin}</username>\n" \
    "<password>\${repoPwd}</password>\n" \
    "</server>\n" \
    "</servers>\n" \
    "</settings>\n" > settings.xml

ARG REPO_SELFCARE
ARG REPO_USERNAME
ARG REPO_PASSWORD

RUN mvn --global-settings settings.xml -f apps/registry-proxy/pom.xml -pl app -am \
    -DrepositoryId=${REPO_SELFCARE} -DrepoLogin=${REPO_USERNAME} -DrepoPwd=${REPO_PASSWORD} \
    clean package -DskipTests

FROM openjdk:17-jdk@sha256:528707081fdb9562eb819128a9f85ae7fe000e2fbaeaf9f87662e7b3f38cb7d8 AS runtime

ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en'

WORKDIR /app

COPY --from=builder /src/apps/registry-proxy/target/*-FATJAR.jar ./app.jar

ADD https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.2.11/applicationinsights-agent-3.2.11.jar ./applicationinsights-agent.jar
RUN chmod 755 ./applicationinsights-agent.jar

EXPOSE 8080
USER 1001

ENTRYPOINT ["java","-jar","app.jar"]
