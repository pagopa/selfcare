FROM maven:3-eclipse-temurin-17@sha256:6ca4f2d4c7a8affa4a7cdec526dd2aa5d433ddd63f8ed3337a543408136b174e AS builder

WORKDIR /src

COPY --link ./apps/onboarding-backend/pom.xml ./apps/onboarding-backend/pom.xml
COPY --link ./apps/onboarding-backend/connector-api ./apps/onboarding-backend/connector-api
COPY --link ./apps/onboarding-backend/connector ./apps/onboarding-backend/connector
COPY --link ./apps/onboarding-backend/core ./apps/onboarding-backend/core
COPY --link ./apps/onboarding-backend/web ./apps/onboarding-backend/web
COPY --link ./apps/onboarding-backend/app ./apps/onboarding-backend/app

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

RUN mvn --global-settings settings.xml -f apps/onboarding-backend/pom.xml -pl app -am \
    -DrepositoryId=${REPO_SELFCARE} -DrepoLogin=${REPO_USERNAME} -DrepoPwd=${REPO_PASSWORD} \
    clean package -DskipTests

FROM openjdk:17-jdk@sha256:528707081fdb9562eb819128a9f85ae7fe000e2fbaeaf9f87662e7b3f38cb7d8 AS runtime

ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en'

WORKDIR /app

COPY --from=builder /src/apps/onboarding-backend/target/*.jar ./app.jar

ADD https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.2.11/applicationinsights-agent-3.2.11.jar ./applicationinsights-agent.jar
RUN chmod 755 ./applicationinsights-agent.jar

EXPOSE 8080
USER 1001

ENTRYPOINT ["java", "-jar", "app.jar"]
