# Selfcare Monorepo

Maven-based monorepo for the Selfcare platform — a suite of Quarkus 3.31.x microservices (Java 17) for managing identity, onboarding, products, and webhooks for the Italian public sector (PagoPA).

## Build & Test Commands

```bash
# Build all modules
mvn clean package

# Build without tests
mvn clean package -DskipTests

# Build a single app
mvn -f apps/product/pom.xml clean package

# Build a library and propagate to all dependents
mvn --projects :selfcare-cucumber-sdk --also-make-dependents clean package -DskipTests

# Run all unit tests for a specific app
mvn -f apps/product/pom.xml test

# Run a single test class
mvn -f apps/product/pom.xml test -Dtest=ProductServiceImplTest

# Run a single test method
mvn -f apps/product/pom.xml test -Dtest=ProductServiceImplTest#myMethod

# Run integration tests (Cucumber BDD — requires Docker)
mvn -f apps/product/pom.xml verify

# Skip integration tests
mvn clean package -DskipITs

# Dev mode with live reload
mvn -f apps/product/pom.xml quarkus:dev

# Test coverage (Jacoco aggregate, requires profile per module)
mvn --projects :test-coverage --also-make verify -Pproduct,report

# Change library version
mvn versions:set -DnewVersion=0.1.2 --projects :selfcare-cucumber-sdk && mvn versions:commit
```

> Builds require credentials in `~/.m2/settings.xml` for two custom repositories: Azure DevOps (`selfcare-platform`) and GitHub Packages (`pagopa/selfcare`).

## Architecture

### Microservices (`apps/`)

| App | Purpose |
|-----|---------|
| `auth` | Authentication (SAML, OTP) |
| `iam` | Identity and Access Management |
| `product` | Product configuration CRUD |
| `product-cdc` | Change Data Capture — syncs MongoDB to Azure Blob Storage |
| `webhook` | Webhook management with retry logic and async delivery |
| `institution-ms` | Institution management |
| `user-ms` | User management |
| `user-group-ms` | User group management |
| `external-api` | External-facing API gateway |
| `dashboard-bff` | Dashboard Backend-for-Frontend |
| `onboarding-ms` | Onboarding microservice |
| `onboarding-bff` | Onboarding Backend-for-Frontend |
| `onboarding-cdc` | Onboarding Change Data Capture |
| `onboarding-functions` | Azure Functions for onboarding |
| `document-ms` | Document management |
| `registry-proxy` | Registry proxy service |
| `delegation-cdc` | Delegation Change Data Capture |
| `user-cdc` | User Change Data Capture |
| `user-group-cdc` | User group Change Data Capture |
| `institution-send-mail-scheduler` | Scheduled email sender for institutions |

### Shared Libraries (`libs/`)

- **`selfcare-cucumber-sdk`** — BDD testing helpers integrating Testcontainers + REST Assured; used by all integration test suites
- **`selfcare-sdk-security`** — JWT authentication and Quarkus security utilities shared across services
- **`selfcare-sdk-pom`** — Parent POM with shared dependency versions and plugin config
- **`selfcare-onboarding-sdk-*`** — Onboarding-specific SDKs (common, crypto, azure-storage, product)
- **`selfcare-user-sdk-*`** — User-specific SDKs (event, model, pom)

## Coding Conventions

### Package Structure

Each microservice follows this layered structure under `it.pagopa.selfcare.<app>/`:

```
controller/   # REST endpoints (JAX-RS with @Path, @Authenticated)
service/      # Business logic interfaces + Impl classes
repository/   # MongoDB Panache repositories
entity/       # Domain entities (also model/ in some apps)
dto/          # Data transfer objects (request/ and response/ sub-packages)
mapper/       # MapStruct mappers
config/       # Configuration classes
exception/    # Custom exceptions + ExceptionHandler
```

### Reactive Programming Model

All services and repositories use **SmallRye Mutiny** reactive types:
- `Uni<T>` for single async values (used everywhere)
- `Multi<T>` for streams
- Repositories extend `ReactivePanacheMongoRepositoryBase<Entity, IdType>`
- Test reactive code with `UniAssertSubscriber` or `.await().indefinitely()`

```java
// Repository
public class ProductRepository implements ReactivePanacheMongoRepositoryBase<Product, String> {
  public Uni<Product> findProductById(String id) { ... }
}

// Service returns Uni
public Uni<ProductResponse> getProduct(String id) { ... }

// Controller chains .onItem() / .onFailure()
return productService.getProduct(id)
    .onItem().transform(p -> Response.ok(p).build())
    .onFailure(NotFoundException.class).recoverWithItem(...);
```

### Unit Testing

Service and controller tests use `@QuarkusTest` with `@InjectMock` (not `@Mock`):

```java
@QuarkusTest
class ProductServiceImplTest {
  @Inject ProductServiceImpl service;
  @InjectMock ProductRepository repository;
  @InjectMock ProductMapperResponse mapper;
}
```

### Integration Tests (Cucumber)

- `.feature` files live in `src/test/resources/features/`
- `CucumberSuiteTest` extends `CucumberQuarkusTest`, annotated with `@TestProfile`
- Each app has an `IntegrationProfile` overriding config (e.g., injecting a test JWT public key from `src/test/resources/certs/pk-key.pub`)
- Docker Compose setup is managed by Testcontainers `ComposeContainer` pointing to `src/test/resources/docker-compose.yml`
- Run with `mvn verify`; skipped by default via `-DskipITs=true` in pom.xml

### MapStruct Mappers

```java
@Mapper(componentModel = "cdi", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapperResponse {
  ProductResponse toProductResponse(Product product);
  @Mapping(target = "origins", source = "institutionOrigins")
  ProductOriginResponse toProductOriginResponse(Product product);
}
```

Use `unmappedTargetPolicy = ReportingPolicy.IGNORE` when the target has fields not present in the source.

### Exception Handling

Each app has a single `ExceptionHandler` bean using Quarkus RESTEasy Reactive's `@ServerExceptionMapper`:

```java
@ApplicationScoped
public class ExceptionHandler {
  @ServerExceptionMapper
  public Response toResponse(ResourceNotFoundException ex) { ... }
  // one method per custom exception type
}
```

Error responses use the `Problem` DTO (RFC 7807 style) as `application/problem+json`.

### Security

- Controllers are secured with `@Authenticated` (class-level) from `io.quarkus.security`
- JWT public key is injected via `mp.jwt.verify.publickey=${JWT_PUBLIC_KEY}`
- `smallrye.jwt.claims.sub=uid` maps the JWT `uid` claim to the security identity subject

### Input Sanitization

User-controlled string inputs are sanitized with the OWASP Java Encoder before use:

```java
import org.owasp.encoder.Encode;
String safe = Encode.forHtml(userInput);
String safeUri = Encode.forUriComponent(userInput);
```

### OpenAPI

- OpenAPI schema is stored at `src/main/docs/` via `quarkus.smallrye-openapi.store-schema-directory=src/main/docs`
- API operation IDs use method names: `mp.openapi.extensions.smallrye.operationIdStrategy=METHOD`
- Bearer JWT security scheme declared in `OpenApiSecurityConfig.java` via `@SecuritySchemes`

### Banner / Resource Filtering

`banner.txt` uses Maven resource filtering to display the project version at startup:

```xml
<resource>
  <directory>src/main/resources</directory>
  <filtering>true</filtering>
</resource>
```

Reference `${project.version}` inside `banner.txt`.

## Deployment

- **SELC**: Self Care main platform
- **PNPG**: Piattaforma Notifiche PG

Environments: **DEV** (auto-deploy on `main`), **UAT** (auto-deploy on `releases/*`), **PROD** (manual approval required).

Infrastructure is Terraform-based under `infra/apps/<app>/` mirroring the microservice layout.
