FROM maven:3-eclipse-temurin-17@sha256:6ca4f2d4c7a8affa4a7cdec526dd2aa5d433ddd63f8ed3337a543408136b174e AS builder

WORKDIR /src

COPY --link . .

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

FROM eclipse-temurin:17@sha256:cc8d306395be4a8375277887d83e9e77eae05c26b6eba8ef320e5f0640e3a1fe AS runtime

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
