# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Maven-based monorepo for the Selfcare platform — a suite of Quarkus microservices for managing identity, onboarding, products, and webhooks for the Italian public sector (PagoPA).

## Build & Test Commands

```bash
# Build all modules
mvn clean package

# Build without tests
mvn clean package -DskipTests

# Build a library and propagate to all dependents
mvn --projects :cucumber-sdk --also-make-dependents clean package -DskipTests

# Build a single app
mvn -f apps/product/pom.xml clean package

# Run unit tests for a specific app
mvn -f apps/product/pom.xml test

# Run a single test class
mvn -f apps/product/pom.xml test -Dtest=MyServiceTest

# Run integration tests (Cucumber BDD)
mvn -f apps/webhook/pom.xml verify

# Skip integration tests
mvn clean package -DskipITs

# Dev mode with live reload
mvn -f apps/webhook/pom.xml quarkus:dev

# Test coverage (Jacoco, requires profile per module)
mvn --projects :test-coverage --also-make verify -Pproduct,report

# Change library version
mvn versions:set -DnewVersion=0.1.1 --projects :cucumber-sdk && mvn versions:commit
```

## Architecture

### Microservices (`apps/`)

| App | Purpose |
|-----|---------|
| `auth` | Authentication (SAML, OTP) |
| `iam` | Identity and Access Management |
| `product` | Product configuration CRUD |
| `product-cdc` | Change Data Capture — syncs MongoDB to Azure Blob Storage |
| `webhook` | Webhook management with retry logic and async delivery |
| `document-ms` | Document management |
| `onboarding-ms` | Onboarding microservice |
| `onboarding-bff` | Onboarding Backend-for-Frontend |
| `onboarding-cdc` | Onboarding Change Data Capture |
| `onboarding-functions` | Azure Functions for onboarding |
| `registry-proxy` | Registry proxy service |

### Shared Libraries (`libs/`)

- **`cucumber-sdk`** — BDD testing helpers integrating Testcontainers + REST Assured; used by all integration test suites
- **`selfcare-sdk-security`** — JWT authentication and Quarkus security utilities shared across services
- **`selfcare-sdk-pom`** — Parent POM with shared dependency versions and plugin config
- **`selfcare-onboarding-sdk-*`** — Onboarding-specific SDKs (common, crypto, azure-storage, product)

### Key Technologies

- Java 17–23, Quarkus 3.31.x
- MongoDB with Panache ORM
- MapStruct for object mapping, Lombok for boilerplate
- OpenAPI Generator for client/server stubs from API contracts
- JUnit 5 + Mockito (unit), Cucumber + Testcontainers (integration)

### Standard Package Layout

Each microservice follows this layered structure under `it.pagopa.selfcare.<app>/`:

```
controller/   # REST endpoints
service/      # Business logic
repository/   # MongoDB Panache repositories
entity/       # Domain entities (also model/ in some apps)
dto/          # Data transfer objects
mapper/       # MapStruct mappers
config/       # Configuration classes
exception/    # Custom exceptions and global handlers
```

## Coding Conventions

### MapStruct Mappers
Use `@Mapper(componentModel = "cdi")` for CDI/Quarkus integration. Mappers are interfaces; implementations are generated at compile time. Include `mapstruct-lombok` dependency when using Lombok.

### MongoDB Panache
- Entities extend `PanacheMongoEntity` or `PanacheMongoEntityBase`
- Repositories extend `PanacheMongoRepository<EntityType>`

### Integration Tests
Cucumber `.feature` files live in `src/test/resources/features/`. Test steps use `@QuarkusTest` with custom test profiles. Run with `mvn verify`; skip with `-DskipITs`.

## Maven Repositories

Builds require access to custom Maven repositories:
- Azure DevOps: `selfcare-platform-app-projects/_packaging/selfcare-platform`
- GitHub Packages: `pagopa/selfcare`

Credentials must be configured in `~/.m2/settings.xml`.

## Deployment

- **SELC**: Self Care main platform
- **PNPG**: Piattaforma Notifiche PG

Environments: **DEV** (auto-deploy on `main`), **UAT** (auto-deploy on `releases/*`), **PROD** (manual approval required).

Infrastructure is Terraform-based under `infra/apps/<app>/` mirroring the microservice layout.
