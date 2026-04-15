FROM quay.io/quarkus/ubi-quarkus-mandrel-builder-image:23.0-java17 AS builder

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
    -Pnative \
    -DrepositoryId=${REPO_SELFCARE} -DrepoLogin=${REPO_USERNAME} -DrepoPwd=${REPO_PASSWORD} \
    clean package -DskipTests

FROM registry.access.redhat.com/ubi8/ubi-minimal AS runtime

WORKDIR /app

COPY --from=builder /src/apps/onboarding-bff/target/*-runner /app/application

RUN chmod 755 /app/application
USER 1001

EXPOSE 8080

ENTRYPOINT ["/app/application", "-Dquarkus.http.host=0.0.0.0"]
