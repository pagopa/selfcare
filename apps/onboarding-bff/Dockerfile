FROM maven:3-eclipse-temurin-17@sha256:6ca4f2d4c7a8affa4a7cdec526dd2aa5d433ddd63f8ed3337a543408136b174e AS builder

WORKDIR /src

COPY --link ./apps/onboarding-bff/pom.xml ./apps/onboarding-bff/pom.xml
COPY --link ./apps/onboarding-bff/src ./apps/onboarding-bff/src

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

RUN mvn --global-settings settings.xml -f apps/onboarding-bff/pom.xml \
    -DrepositoryId=${REPO_SELFCARE} -DrepoLogin=${REPO_USERNAME} -DrepoPwd=${REPO_PASSWORD} \
    clean package -DskipTests

FROM openjdk:17-jdk@sha256:528707081fdb9562eb819128a9f85ae7fe000e2fbaeaf9f87662e7b3f38cb7d8 AS runtime

ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en'
ENV JAVA_OPTIONS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"

WORKDIR /app

COPY --from=builder /src/apps/onboarding-bff/target/quarkus-app/lib/ ./lib/
COPY --from=builder /src/apps/onboarding-bff/target/quarkus-app/*.jar ./
COPY --from=builder /src/apps/onboarding-bff/target/quarkus-app/app/ ./app/
COPY --from=builder /src/apps/onboarding-bff/target/quarkus-app/quarkus/ ./quarkus/

ADD https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.2.11/applicationinsights-agent-3.2.11.jar ./applicationinsights-agent.jar
RUN chmod 755 ./applicationinsights-agent.jar

EXPOSE 8080
USER 1001

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTIONS -jar /app/quarkus-run.jar"]
