FROM maven:3-eclipse-temurin-17@sha256:6ca4f2d4c7a8affa4a7cdec526dd2aa5d433ddd63f8ed3337a543408136b174e AS builder

WORKDIR /src
COPY --link ./pom.xml .

WORKDIR /src/test-coverage
COPY --link ./test-coverage/pom.xml .

WORKDIR /src/apps
COPY --link ./apps/pom.xml .

WORKDIR /src/apps/institution-ms
COPY --link ./apps/institution-ms/pom.xml .
COPY ./apps/institution-ms/ ./

RUN echo "<settings>\n" \
         "<servers>\n" \
         "<server>\n" \
         "<id>\${repositoryId}</id>\n" \
         "<username>\${repoLogin}</username>\n" \
         "<password>\${repoPwd}</password>\n" \
         "</server>\n" \
         "</servers>\n" \
         "</settings>\n" > settings.xml

ARG REPO_ONBOARDING
ARG REPO_USERNAME
ARG REPO_PASSWORD

RUN mvn --global-settings settings.xml --projects :institution-ms -DrepositoryId=${REPO_ONBOARDING} -DrepoLogin=${REPO_USERNAME} -DrepoPwd=${REPO_PASSWORD} --also-make-dependents clean package -DskipTests

FROM eclipse-temurin:17@sha256:d838da85ec7aa8fd72b613e30b5f7629d8f5ac70a4264d45735a8a2ed9af447c AS runtime

ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en'

WORKDIR /app

COPY --from=builder /src/apps/institution-ms/target/*.jar ./app.jar

ADD https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.2.11/applicationinsights-agent-3.2.11.jar ./applicationinsights-agent.jar
RUN chmod 755 ./applicationinsights-agent.jar

EXPOSE 8080
USER 1001

ENTRYPOINT ["java", "-jar", "app.jar"]
