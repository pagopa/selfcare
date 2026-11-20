# selfcare-user-sdk-pom

> Aggregator/parent POM of the User SDK modules ([`selfcare-user-sdk-model`](selfcare-user-sdk-model.md) and [`selfcare-user-sdk-event`](selfcare-user-sdk-event.md)).

| | |
|---|---|
| Path | `libs/selfcare-user-sdk-pom` (only `pom.xml`) |
| Artifact | `it.pagopa.selfcare:selfcare-user-sdk-pom:1.1.1` (packaging `pom`, parent `it.pagopa.selfcare:libs:0.0.1`) |
| Stack | Maven parent POM |
| Used by | No app references it directly. Parent of `selfcare-user-sdk-model` and `selfcare-user-sdk-event` |

## What it provides

From `libs/selfcare-user-sdk-pom/pom.xml`:

- `modules`: `../selfcare-user-sdk-model`, `../selfcare-user-sdk-event`.
- `maven.deploy.skip=true` for the POM itself; both children set `maven.deploy.skip=false`.
- Test dependencies: `junit-jupiter-api`/`-engine` 5.7.2, `mockito-core` 5.7.0.

The children inherit version `1.1.1`.

## Build and release

```bash
mvn --projects :selfcare-user-sdk-pom --also-make-dependents clean package -DskipTests
```

Release: `.github/workflows/release_selfcare_user_sdk_pom.yml` ("Release User sdk"), push on `main` touching
`libs/selfcare-user-sdk-{pom,model,event}/**` or `workflow_dispatch`; deploys with
`mvn -B deploy -f libs/selfcare-user-sdk-pom/pom.xml`.
Coverage: profiles `selfcare-user-sdk-model` and `selfcare-user-sdk-event` in `test-coverage/pom.xml`.

## References

- [selfcare-user-sdk-model](selfcare-user-sdk-model.md), [selfcare-user-sdk-event](selfcare-user-sdk-event.md)
