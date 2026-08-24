# Swagger Detect Conflict and Update with Quarkus in monorepo

This GitHub Action is designed to facilitate the detection of conflicts in Swagger documentation files within a Selfcare monorepo environment and to automatically update them using Quarkus.

- Checkout HEAD ref and a BASE ref, for ex. a feature branch and main branch
- Launch maven package on HEAD ref to create a swagger documentation updated (quarkus.smallrye-openapi.store-schema-directory in resources must be present)
- Detect if the newer (HEAD) spec introduces breaking or non-breaking changes, in case it will fail
- Commit and push an updated openapi.docs

## Usage

``` yaml
- name: Swagger Detect Conflict and Update
  id: swagger-conflict-update
  uses: pagopa/selfcare-commons/github-actions-template/swagger-detect-conflict-quarkus@{sha-commit}
  with:
    path_openapi_docs: src/main/docs
    branch_ref: main
    module: onboarding-ms
```