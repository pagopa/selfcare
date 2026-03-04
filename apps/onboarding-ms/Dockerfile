# syntax=docker/dockerfile:1.6@sha256:ac85f380a63b13dfcefa89046420e1781752bab202122f8f50032edf31be0021
FROM maven:3-eclipse-temurin-17@sha256:6ca4f2d4c7a8affa4a7cdec526dd2aa5d433ddd63f8ed3337a543408136b174e AS builder

WORKDIR /src
COPY --link ./pom.xml .

WORKDIR /src/libs
COPY --link ./libs/ .

WORKDIR /src/test-coverage
COPY --link ./test-coverage/pom.xml .

WORKDIR /src/apps
COPY --link ./apps/pom.xml .

WORKDIR /src/apps/onboarding-ms
COPY --link ./apps/onboarding-ms/pom.xml .
COPY ./apps/onboarding-ms/src/main/ ./src/main/

WORKDIR /src

RUN echo "<settings>\n" \
         "<servers>\n" \
         "<server>\n" \
         "<id>\${repositoryId}</id>\n" \
         "<username>\${repoLogin}</username>\n" \
         "<password>\${repoPwd}</password>\n" \
         "</server>\n" \
         "<server>\n" \
         "<id>\${selfcareRepositoryId}</id>\n" \
         "<username>\${repoLogin}</username>\n" \
         "<password>\${repoPwd}</password>\n" \
         "</server>\n" \
         "</servers>\n" \
         "</settings>\n" > settings.xml

ARG REPO_ONBOARDING
ARG REPO_USERNAME
ARG REPO_PASSWORD
ARG REPO_SELFCARE

# RUN mvn --projects :onboarding-ms --also-make clean package -DskipTests
RUN mvn --global-settings settings.xml --projects :onboarding-ms -DrepositoryId=${REPO_ONBOARDING} -DselfcareRepositoryId=${REPO_SELFCARE} -DrepoLogin=${REPO_USERNAME} -DrepoPwd=${REPO_PASSWORD} --also-make clean package -DskipTests

FROM openjdk:17-jdk@sha256:528707081fdb9562eb819128a9f85ae7fe000e2fbaeaf9f87662e7b3f38cb7d8 AS runtime

ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en'
ENV JAVA_OPTIONS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"

WORKDIR /app

COPY --from=builder /src/apps/onboarding-ms/target/quarkus-app/lib/ ./lib/
COPY --from=builder /src/apps/onboarding-ms/target/quarkus-app/*.jar ./
COPY --from=builder /src/apps/onboarding-ms/target/quarkus-app/app/ ./app/
COPY --from=builder /src/apps/onboarding-ms/target/quarkus-app/quarkus/ ./quarkus/

ADD https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.2.11/applicationinsights-agent-3.2.11.jar ./applicationinsights-agent.jar
RUN chmod 755 ./applicationinsights-agent.jar

EXPOSE 8080
USER 1001

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTIONS -jar /app/quarkus-run.jar"]
