# Swagger Detect Conflict and Update with Spring

This GitHub Action is designed to facilitate the detection of conflicts in Swagger documentation files within a Selfcare repo Spring based and to automatically update.

- Checkout HEAD ref and a BASE ref, for ex. a feature branch and main branch
- Launch  ```mvn test -Dtest=SwaggerConfigTest#swaggerSpringPlugin -DfailIfNoTests=false``` on HEAD ref to create a swagger documentation updated
- Detect if the newer (HEAD) spec introduces breaking or non-breaking changes, in case it will fail
- Commit and push an updated openapi.docs

## Usage

``` yaml
- name: Swagger Detect Conflict and Update
  id: swagger-conflict-update
  uses: pagopa/selfcare-commons/github-actions-template/swagger-detect-conflict-spring@{sha-commit}
  with:
    path_openapi_docs: app/src/main/resources/swagger/api-docs.json
    branch_ref: release-dev
```