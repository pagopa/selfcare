FROM maven:3-eclipse-temurin-17@sha256:0d328fa6843bb26b60cf44d69833f241ffe96218fb29fa19df7a6603863eaae7 AS builder

WORKDIR /src
COPY --link pom.xml .

WORKDIR /src/test-coverage
COPY --link ./test-coverage/pom.xml .

WORKDIR /src/apps
COPY --link ./apps/pom.xml .

WORKDIR /src/libs
COPY --link ./libs/ .

WORKDIR /src/apps/document-ms
COPY --link ./apps/document-ms/pom.xml .
COPY ./apps/document-ms/src/main/ ./src/main/

WORKDIR /src

RUN echo "<settings>\n" \
         "<servers>\n" \
         "<server>\n" \
         "<id>\${repositoryId}</id>\n" \
         "<username>\${repoLogin}</username>\n" \
         "<password>\${repoPwd}</password>\n" \
         "</server>\n" \
         "<server>\n" \
         "<id>\${selfcareOnboardingId}</id>\n" \
         "<username>\${repoLogin}</username>\n" \
         "<password>\${repoPwd}</password>\n" \
         "</server>\n" \
         "</servers>\n" \
         "</settings>\n" > settings.xml

ARG REPO_SELFCARE
ARG REPO_ONBOARDING
ARG REPO_USERNAME
ARG REPO_PASSWORD

RUN mvn --global-settings settings.xml --projects :document-ms -DrepositoryId=${REPO_SELFCARE} -DselfcareOnboardingId=${REPO_ONBOARDING} -DrepoLogin=${REPO_USERNAME} -DrepoPwd=${REPO_PASSWORD} --also-make clean package -DskipTests

FROM eclipse-temurin:17@sha256:cc8d306395be4a8375277887d83e9e77eae05c26b6eba8ef320e5f0640e3a1fe AS runtime

ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en'
ENV JAVA_OPTIONS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"

WORKDIR /app

COPY --from=builder /src/apps/document-ms/target/quarkus-app/lib/ ./lib/
COPY --from=builder /src/apps/document-ms/target/quarkus-app/*.jar ./
COPY --from=builder /src/apps/document-ms/target/quarkus-app/app/ ./app/
COPY --from=builder /src/apps/document-ms/target/quarkus-app/quarkus/ ./quarkus/

ADD https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.2.11/applicationinsights-agent-3.2.11.jar ./applicationinsights-agent.jar
RUN chmod 755 ./applicationinsights-agent.jar

EXPOSE 8080
USER 1001

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTIONS -jar /app/quarkus-run.jar"]
