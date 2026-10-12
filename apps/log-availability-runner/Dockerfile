# syntax=docker/dockerfile:1.6@sha256:ac85f380a63b13dfcefa89046420e1781752bab202122f8f50032edf31be0021
FROM maven:3-eclipse-temurin-17@sha256:0d328fa6843bb26b60cf44d69833f241ffe96218fb29fa19df7a6603863eaae7 AS builder

WORKDIR /src
COPY --link pom.xml .

WORKDIR /src/apps
COPY --link ./apps/pom.xml .

WORKDIR /src/apps/log-availability-runner
COPY --link ./apps/log-availability-runner/pom.xml .
COPY ./apps/log-availability-runner/src/main/ ./src/main/

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

RUN mvn --global-settings settings.xml --file /src/apps/log-availability-runner/pom.xml -DrepositoryId=${REPO_SELFCARE} -DrepoLogin=${REPO_USERNAME} -DrepoPwd=${REPO_PASSWORD} clean package -DskipTests

FROM eclipse-temurin:17@sha256:d838da85ec7aa8fd72b613e30b5f7629d8f5ac70a4264d45735a8a2ed9af447c AS runtime

WORKDIR /app

COPY --from=builder /src/apps/log-availability-runner/target/quarkus-app/lib/ ./lib/
COPY --from=builder /src/apps/log-availability-runner/target/quarkus-app/*.jar ./
COPY --from=builder /src/apps/log-availability-runner/target/quarkus-app/app/ ./app/
COPY --from=builder /src/apps/log-availability-runner/target/quarkus-app/quarkus/ ./quarkus/

USER 1001

ENTRYPOINT ["java", "-Djava.util.logging.manager=org.jboss.logmanager.LogManager", "-jar", "/app/quarkus-run.jar"]
