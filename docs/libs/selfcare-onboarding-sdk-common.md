# selfcare-onboarding-sdk-common

> Shared onboarding domain constants (enums) used across SelfCare services: institution types, onboarding status, party roles, product IDs, workflow types, origins.

| | |
|---|---|
| Path | `libs/selfcare-onboarding-sdk-common` |
| Artifact | `it.pagopa.selfcare:selfcare-onboarding-sdk-common:0.18.3` (version inherited from [`selfcare-onboarding-sdk-pom`](selfcare-onboarding-sdk-pom.md)) |
| Stack | Plain Java 17, no runtime dependencies |
| Used by | `dashboard-bff`, `delegation-cdc`, `document-ms`, `external-api`, `institution-send-mail-scheduler`, `onboarding-bff` (`connector-api`, `core`, `web`), `onboarding-functions`, `onboarding-ms`, `product-cdc`, `registry-proxy`; libs [`selfcare-onboarding-sdk-product`](selfcare-onboarding-sdk-product.md), [`selfcare-user-sdk-model`](selfcare-user-sdk-model.md) |

## What it provides

Package `it.pagopa.selfcare.onboarding.common` (`libs/selfcare-onboarding-sdk-common/src/main/java/it/pagopa/selfcare/onboarding/common/`):

| Enum | Values (summary) |
|---|---|
| `InstitutionType` | `PA`, `PG`, `GSP`, `SA`, `PT`, `SCP`, `PSP`, `AS`, `REC`, `CON`, `PRV`, `PRV_PF`, `GPU`, `SCEC` |
| `OnboardingStatus` | `REQUESTING`, `REQUEST`, `TOBEVALIDATED`, `PENDING`, `PENDING_IN_REVIEW`, `COMPLETED`, `FAILED`, `REJECTED`, `DELETED`, `OVERRIDDEN` |
| `WorkflowType` | 16 workflows, e.g. `CONTRACT_REGISTRATION`, `FOR_APPROVE`, `CONFIRMATION`, `USERS`, `IMPORT`, `CONTRACT_REGISTRATION_AGGREGATOR`, `USERS_PG` |
| `PartyRole` | `MANAGER`, `DELEGATE`, `SUB_DELEGATE`, `OPERATOR`, `ADMIN_EA` |
| `ProductId` | 15 product identifiers, e.g. `PROD_PAGOPA`, `PROD_IO`, `PROD_PN`, `PROD_INTEROP`, `PROD_FD`, `PROD_IDPAY_MERCHANT` |
| `Origin` | Registry sources, e.g. `IPA`, `INFOCAMERE`, `ANAC`, `IVASS`, `ADE`, `PDND_INFOCAMERE`, `SELC`, `MOCK`, `UNKNOWN` |
| `Env`, `PricingPlan`, `DocumentType`, `InstitutionPaSubunitType` | Product environment (`ROOT`, `DEV`, `COLL`, `PROD`), pricing plans (`FA`, `BASE`, `PREMIUM`), document types (`INSTITUTION`, `LEGALS`, `USER`, `ATTACHMENT`), PA subunit types (`AOO`) |

Check the source for the authoritative list of values.

## Usage

```xml
<dependency>
  <groupId>it.pagopa.selfcare</groupId>
  <artifactId>selfcare-onboarding-sdk-common</artifactId>
  <version>0.18.3</version>
</dependency>
```

No configuration properties.

## Build and release

```bash
mvn -f libs/selfcare-onboarding-sdk-pom/pom.xml clean package
```

Released together with the other Onboarding SDK modules by `.github/workflows/release_onboarding_sdk.yml`
(manual `workflow_dispatch`) to `https://maven.pkg.github.com/pagopa/selfcare`. See [selfcare-onboarding-sdk-pom](selfcare-onboarding-sdk-pom.md).

## References

- [`libs/selfcare-onboarding-sdk-common/README.md`](../../libs/selfcare-onboarding-sdk-common/README.md) (version numbers in the README are outdated)
- Spring/legacy duplicates of some enums: `selc-commons-base` in [selfcare-commons](selfcare-commons.md)
