# Selfcare Monorepo

Maven-based monorepo for the Selfcare platform built with Quarkus 3.28.3 and Java 17.

## Repository Structure

```
selfcare/
├── apps/              # Quarkus microservices (auth, iam, product, product-cdc, webhook)
├── libs/              # Shared libraries (cucumber-sdk, selfcare-sdk-security, selfcare-sdk-pom)
├── infra/             # Terraform infrastructure code
└── test-coverage/     # Jacoco aggregate test coverage
```

## Build & Test Commands

### Full Build
```bash
# Build all modules
mvn clean package

# Build without tests
mvn clean package -DskipTests
```

### Building Specific Modules
```bash
# Build a library and its dependents
mvn --projects :cucumber-sdk --also-make-dependents clean package -DskipTests

# Build a single app (from root)
mvn -f apps/webhook/pom.xml clean package
```

### Running Tests
```bash
# Run tests for a specific app
mvn -f apps/product/pom.xml test

# Run integration tests (Cucumber-based)
mvn -f apps/webhook/pom.xml verify

# Skip integration tests
mvn clean package -DskipITs
```

### Test Coverage
```bash
# Generate coverage for specific module (requires profile)
mvn --projects :test-coverage --also-make verify -Puser-ms,report

# With SonarCloud
mvn --projects :test-coverage --also-make verify -Puser-ms,report,coverage \
  -Dsonar.organization=xxx -Dsonar.projectKey=yyy -Dsonar.token=zzz
```

### Running Apps Locally
```bash
# Dev mode with live reload
mvn -f apps/webhook/pom.xml quarkus:dev

# Run packaged app
java -jar apps/webhook/target/quarkus-app/quarkus-run.jar
```

### Version Management
```bash
# Change version of a library
mvn versions:set -DnewVersion=0.0.3 --projects :cucumber-sdk

# Persist version change
mvn versions:commit
```

## Architecture Overview

### Microservices (apps/)
- **auth**: Authentication service (SAML, OTP)
- **iam**: Identity and Access Management
- **product**: Product configuration management (CRUD + API)
- **product-cdc**: Change Data Capture - syncs MongoDB changes to Azure Blob Storage
- **webhook**: Webhook management with retry logic and async notification delivery

### Shared Libraries (libs/)
- **cucumber-sdk**: BDD testing framework integration with Testcontainers and REST Assured
- **selfcare-sdk-security**: JWT authentication and security utilities for Quarkus
- **selfcare-sdk-pom**: Parent POM with shared dependencies and plugin configurations

### Key Dependencies
All apps use:
- MongoDB with Panache for persistence
- MapStruct for object mapping
- Lombok for boilerplate reduction
- OpenAPI Generator for client/server code generation

## Coding Conventions

### Package Structure
Standard layered architecture per microservice:
```
it.pagopa.selfcare.<app>/
├── controller/     # REST endpoints (or resource/)
├── service/        # Business logic
├── repository/     # MongoDB Panache repositories
├── entity/         # Domain models (or model/)
├── dto/            # Data transfer objects
├── mapper/         # MapStruct mappers
├── config/         # Configuration classes
└── exception/      # Custom exceptions and handlers
```

### Testing
- **Unit tests**: JUnit 5 with Mockito
- **Integration tests**: Cucumber BDD with `.feature` files in `src/test/resources/features/`
- Cucumber tests use `@QuarkusTest` and custom test profiles
- Integration tests run with `mvn verify`, skipped with `-DskipITs`

### MapStruct Mappers
- Use `@Mapper(componentModel = "cdi")` for CDI integration in Quarkus
- Declare as interfaces, implementation generated at compile time
- Include `mapstruct-lombok` dependency for Lombok compatibility

### MongoDB Panache
- Entities extend `PanacheMongoEntity` or `PanacheMongoEntityBase`
- Repositories extend `PanacheMongoRepository<EntityType>`
- Use Panache query methods: `find`, `list`, `count`, `delete`, etc.

### Resource Filtering
Some modules use Maven resource filtering to inject project version into `banner.txt`:
```xml
<build>
    <resources>
        <resource>
            <directory>src/main/resources</directory>
            <filtering>true</filtering>
        </resource>
    </resources>
</build>
```
Reference version with `${project.version}` in banner files.

## Maven Repository Configuration

Apps depend on custom Maven repositories hosted on:
- Azure DevOps: `selfcare-platform`
- GitHub Packages: `pagopa/selfcare`, `pagopa/selfcare-onboarding`

Authentication required for builds - see repository settings in root `pom.xml`.

## Deployment Targets

- **SELC**: Self Care main platform
- **PNPG**: Piattaforma Notifiche PG

Environments: DEV (auto-deploy on `main`), UAT (auto-deploy on `releases/*`), PROD (manual approval required).

## Infrastructure

Terraform code in `infra/` with per-app configurations. Each app has corresponding infrastructure:
- `infra/apps/auth/`
- `infra/apps/product/`
- etc.

## Related Documentation

- [RELEASE.md](../RELEASE.md) - Complete release process and branching strategy
- [Quarkus_banner.md](../Quarkus_banner.md) - Custom banner configuration guide
- [test-coverage/README.md](../test-coverage/README.md) - Test coverage details
- Individual app READMEs in `apps/<app>/README.md`
