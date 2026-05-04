FROM maven:3-eclipse-temurin-17@sha256:0d328fa6843bb26b60cf44d69833f241ffe96218fb29fa19df7a6603863eaae7 AS builder

COPY . .

RUN echo "<settings>\n" \
         "<servers>\n" \
         "<server>\n" \
         "<id>\${repositoryOnboarding}</id>\n" \
         "<username>\${repoLogin}</username>\n" \
         "<password>\${repoPwd}</password>\n" \
         "</server>\n" \
         "</servers>\n" \
         "</settings>\n" > settings.xml

ARG REPO_ONBOARDING
ARG REPO_USERNAME
ARG REPO_PASSWORD

RUN mvn --global-settings settings.xml -DrepositoryOnboarding=${REPO_ONBOARDING} -DrepoLogin=${REPO_USERNAME} -DrepoPwd=${REPO_PASSWORD} clean package -DskipTests=true

FROM eclipse-temurin:17@sha256:d838da85ec7aa8fd72b613e30b5f7629d8f5ac70a4264d45735a8a2ed9af447c AS runtime

ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en'

WORKDIR /app

COPY --from=builder ./target/*.jar ./app.jar

ADD https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.2.11/applicationinsights-agent-3.2.11.jar ./applicationinsights-agent.jar
RUN chmod 755 ./applicationinsights-agent.jar

EXPOSE 8080
USER 1001

ENTRYPOINT ["java", "-jar", "app.jar"]
