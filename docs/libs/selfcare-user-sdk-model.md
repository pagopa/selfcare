# selfcare-user-sdk-model

> Shared user-domain model for the user services: `OnboardedProduct`, user state and action constants, telemetry event names.

| | |
|---|---|
| Path | `libs/selfcare-user-sdk-model` |
| Artifact | `it.pagopa.selfcare:selfcare-user-sdk-model:1.1.1` (parent [`selfcare-user-sdk-pom:1.1.1`](selfcare-user-sdk-pom.md)) |
| Stack | Plain Java 17, Lombok, MapStruct; depends on [`selfcare-onboarding-sdk-common:0.17.5`](selfcare-onboarding-sdk-common.md) |
| Used by | `user-ms`; lib [`selfcare-user-sdk-event`](selfcare-user-sdk-event.md) (so transitively `user-cdc`, `user-group-cdc`) |

## What it provides

Java package `it.pagopa.selfcare.user.model`. Note: the source directory is literally named
`src/main/java/it.pagopa.selfcare.user.model/` (one dotted folder instead of nested folders); `javac` accepts it
because the package is declared in the files, but IDEs may flag it.

| Type | Content |
|---|---|
| `OnboardedProduct` | Product membership of a user in an institution: `productId`, `tokenId`, `status` (`OnboardedProductState`), `productRole`, `role` (`PartyRole`), `partnerTechRole`, `env` (`Env`), `createdAt`, `updatedAt`, `delegationId`, `toAddOnAggregates`, `roleId` |
| `UserAction` | Permissions/actions (namespace `Selc` + functionality), e.g. `UPLOAD_LOGO`, `VIEW_BILLING`, `MANAGE_PRODUCT_USER`, `MANAGE_PRODUCT_GROUPS`, `CREATE_DELEGATION`, `UPDATE_INSTITUTION_DATA` (13 values) |
| `constants.OnboardedProductState` | `ACTIVE`, `PENDING`, `TOBEVALIDATED`, `SUSPENDED`, `DELETED`, `REJECTED` |
| `constants.EventsName` | Application Insights event names: `USER_MS`, `USER_CDC`, `FD_USER_CDC`, `USER_GROUP_CDC` |
| `constants.EventsMetric` | Custom metric names, e.g. `EventsUserInstitution_success` / `_failures`, `EventsUserGroup_success` / `_failures`, `UserInfoUpdate_success` / `_failure` |

`PartyRole` and `Env` come from `it.pagopa.selfcare.onboarding.common`.

## Usage

```xml
<dependency>
  <groupId>it.pagopa.selfcare</groupId>
  <artifactId>selfcare-user-sdk-model</artifactId>
  <version>1.1.1</version>
</dependency>
```

No configuration properties.

## Build and release

```bash
mvn --projects :selfcare-user-sdk-pom --also-make-dependents clean package -DskipTests
```

Released together with the user SDK parent by `.github/workflows/release_selfcare_user_sdk_pom.yml`
(push on `main` with path filter on the user-sdk folders, or `workflow_dispatch`), which runs
`mvn -B deploy -f libs/selfcare-user-sdk-pom/pom.xml`. Coverage profile `selfcare-user-sdk-model` in `test-coverage/pom.xml`.

## References

- No README in the lib folder.
- [user-ms app](../apps/user-ms.md)
