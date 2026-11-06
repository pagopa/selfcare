# selfcare-sdk-pom

> Parent POM shared by the Quarkus-oriented SelfCare SDKs (security, tenant, tenant-mongodb, health, health-spring): Java 17, Quarkus BOM, common test dependencies, annotation processors and GitHub Packages distribution.

| | |
|---|---|
| Path | `libs/selfcare-sdk-pom` (only `pom.xml`) |
| Artifact | `it.pagopa.selfcare:selfcare-sdk-pom:1.0.0` (packaging `pom`) |
| Stack | Maven parent POM; imports `io.quarkus.platform:quarkus-bom:3.11.2` and `org.junit:junit-bom:5.10.1` |
| Used by | No app references it directly (grep of `apps/**/pom.xml`). Parent of [`selfcare-sdk-tenant`](selfcare-sdk-tenant.md), [`selfcare-sdk-tenant-mongodb`](selfcare-sdk-tenant-mongodb.md), [`selfcare-sdk-security`](selfcare-sdk-security.md), [`selfcare-sdk-health`](selfcare-sdk-health.md), [`selfcare-sdk-health-spring`](selfcare-sdk-health-spring.md) (via `relativePath ../selfcare-sdk-pom`) |

## What it provides

From `libs/selfcare-sdk-pom/pom.xml`:

- Properties: `java.version=17`, `lombok.version=1.18.36`, `mapstruct.version=1.5.5.Final`, `quarkus.platform.version=3.11.2`, `sonar.skip=true`.
- Dependencies inherited by every child: `lombok` (provided), `slf4j-api`, JUnit 5, `quarkus-junit5`, `quarkus-junit5-mockito`, `mockito-core` (test).
- Plugins: `maven-compiler-plugin` with MapStruct/Lombok annotation processors, JaCoCo, `maven-deploy-plugin` with `skip=false` (overrides `maven.deploy.skip=true` of `libs/pom.xml`).
- `distributionManagement`: repository `selfcare` at `https://maven.pkg.github.com/pagopa/selfcare`.

## Usage

```xml
<parent>
  <groupId>it.pagopa.selfcare</groupId>
  <artifactId>selfcare-sdk-pom</artifactId>
  <version>1.0.0</version>
  <relativePath>../selfcare-sdk-pom</relativePath>
</parent>
```

## Build and release

```bash
mvn -f libs/selfcare-sdk-pom/pom.xml clean install
```

There is no workflow deploying this POM by itself. The workflows named "Release SDK"/"Release SDKs"
(`.github/workflows/release_sdk_pom.yml`, `.github/workflows/release-sdk.yml`) release its children
security, tenant and tenant-mongodb. Changes to `libs/selfcare-sdk-pom/**` trigger
`.github/workflows/pr_health_sdk_spring.yml` on PRs.

## References

- [selfcare-onboarding-sdk-pom](selfcare-onboarding-sdk-pom.md), [selfcare-user-sdk-pom](selfcare-user-sdk-pom.md), [selfcare-starter-parent](selfcare-starter-parent.md) (other parent POMs)
