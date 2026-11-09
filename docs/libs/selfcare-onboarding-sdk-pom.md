# selfcare-onboarding-sdk-pom

> Aggregator/parent POM of the Onboarding SDK modules (common, product, crypto, azure-storage), released together with a single version.

| | |
|---|---|
| Path | `libs/selfcare-onboarding-sdk-pom` (only `pom.xml`) |
| Artifact | `it.pagopa.selfcare:selfcare-onboarding-sdk-pom:0.18.3` (packaging `pom`) |
| Stack | Maven parent POM, Java 17, imports `io.quarkus.platform:quarkus-bom:3.4.2` |
| Used by | No app references it directly. Parent and aggregator of [`selfcare-onboarding-sdk-common`](selfcare-onboarding-sdk-common.md), [`selfcare-onboarding-sdk-product`](selfcare-onboarding-sdk-product.md), [`selfcare-onboarding-sdk-crypto`](selfcare-onboarding-sdk-crypto.md), [`selfcare-onboarding-sdk-azure-storage`](selfcare-onboarding-sdk-azure-storage.md) |

## What it provides

From `libs/selfcare-onboarding-sdk-pom/pom.xml`:

- `modules`: `../selfcare-onboarding-sdk-common`, `../selfcare-onboarding-sdk-product`, `../selfcare-onboarding-sdk-crypto`, `../selfcare-onboarding-sdk-azure-storage`.
- Test dependencies for all children: `junit-jupiter-api`/`-engine` 5.8.0, `mockito-core` 5.7.0.
- Plugins: compiler 3.13.0, JaCoCo, `maven-deploy-plugin` with `skip=false`; `sonar.skip=true`.
- `distributionManagement`: `selfcare` -> `https://maven.pkg.github.com/pagopa/selfcare`.

All four modules inherit version `0.18.3`: bump it here (and in the children `parent` blocks) for a release.

## Usage

Consumers depend on the single modules, not on this POM, e.g.:

```xml
<dependency>
  <groupId>it.pagopa.selfcare</groupId>
  <artifactId>selfcare-onboarding-sdk-common</artifactId>
  <version>0.18.3</version>
</dependency>
```

## Build and release

```bash
mvn -f libs/selfcare-onboarding-sdk-pom/pom.xml clean package
```

Release: `.github/workflows/release_onboarding_sdk.yml` ("Release Onboarding SDK"), **manual only**
(`workflow_dispatch`): `mvn -f libs/selfcare-onboarding-sdk-pom/pom.xml clean package -DskipTests` then
`mvn -B -f libs/selfcare-onboarding-sdk-pom/pom.xml deploy -DskipTests`.
Coverage: profile `onboarding-sdk` in `test-coverage/pom.xml`.

## References

- Module pages linked above
