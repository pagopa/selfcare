# selfcare-starter-parent

> Shared Spring Boot parent POM (`selc-starter-parent`) for the SelfCare Spring Boot services and for `selfcare-commons`.

| | |
|---|---|
| Path | `libs/selfcare-starter-parent` (`pom.xml`, `README.md`) |
| Artifact | `it.pagopa.selfcare:selc-starter-parent:0.3.3` (packaging `pom`) |
| Stack | Parent `org.springframework.boot:spring-boot-starter-parent:3.3.0`, Spring Cloud `2023.0.3`, Java 17 |
| Used by | `dashboard-bff` (0.3.2-SNAPSHOT), `external-api` (0.3.3), `institution-ms` (0.3.2-SNAPSHOT), `onboarding-bff`, `registry-proxy`, `user-group-ms` (0.3.1-SNAPSHOT); [`selfcare-commons`](selfcare-commons.md) (0.3.3, `relativePath ../selfcare-starter-parent/pom.xml`) |

## What it provides

From `libs/selfcare-starter-parent/pom.xml`:

- Properties: `java.version=17`, `spring-cloud.version=2023.0.3`, `lombok.version=1.18.22`, `mapstruct.version=1.5.5.Final`, `sonar.skip=true`.
- Dependency management: `spring-cloud-dependencies`, `springdoc-openapi-starter-webmvc-ui/-api` 2.0.4, `jjwt` 0.11.5, `jaxb-api`, `system-stubs-jupiter`, `janino`.
- Dependencies inherited by every consumer: Spring Boot validation, actuator, security, `spring-data-commons`, `micrometer-registry-prometheus`, `micrometer-tracing`, `aspectjweaver`, `janino`, `mapstruct`, `lombok` (provided), `spring-boot-properties-migrator` (runtime), test starters (`spring-boot-starter-test`, `spring-security-test`, `system-stubs-jupiter`).
- Compiler plugin with annotation processors, Sonar plugin; `distributionManagement` -> `https://maven.pkg.github.com/pagopa/selfcare`.

`sonar.skip=true` is inherited: consumer services must set `<sonar.skip>false</sonar.skip>` to keep being analyzed (README, step 4).

## Usage

```xml
<parent>
  <groupId>it.pagopa.selfcare</groupId>
  <artifactId>selc-starter-parent</artifactId>
  <version>0.3.3</version>
  <relativePath>../../libs/selfcare-starter-parent/pom.xml</relativePath>
</parent>
```

The README documents the migration of consumers from older versions (still on the Azure DevOps feed) to `0.3.3` on GitHub Packages.

## Build and release

```bash
mvn -B -ntp -f libs/selfcare-starter-parent/pom.xml clean verify
```

- PR: `.github/workflows/pr_starter_parent.yml` (changes to `libs/selfcare-starter-parent/**`, `libs/pom.xml` or the two workflows).
- Release: `.github/workflows/release_starter_parent.yml`, push on `main` changing `libs/selfcare-starter-parent/pom.xml`
  or `workflow_dispatch`; runs `mvn -B -ntp -f libs/selfcare-starter-parent/pom.xml clean deploy`.
- Every release needs a new version: published versions must not be overwritten.

## References

- [`libs/selfcare-starter-parent/README.md`](../../libs/selfcare-starter-parent/README.md) (Italian; import history from `pagopa/selfcare-starter-parent`)
- [selfcare-commons](selfcare-commons.md)
