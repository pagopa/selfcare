# Microservice User

Repository that contains backend services synch for selfcare user.

It implements CRUD operations for the 'user' object and the business logic for the user.

## Configuration Properties

Requests require `X-Tenant-Id`; the security SDK resolves and validates the tenant before accessing
MongoDB, PDV or product storage. The API response models do not expose the internal `tenantId`.

| Configuration | Environment variable |
|---|---|
| Enabled tenants | `TENANT_SUPPORTED_TENANTS` (default `AR,PNPG`) |
| Resource registry | `TENANT_REGISTRY_JSON`; JSON with `mongo`, `jwt`, `userRegistry` and logical storage `products` for each enabled tenant |
| Mongo connection | `MONGODB_CONNECTION_STRING_<ID>` in the default registry |
| JWT public key | `JWT_PUBLIC_KEY_<ID>` in the registry; `JWT-PUBLIC-KEY` remains a legacy fallback |
| PDV API key | `USER_REGISTRY_API_KEY_<ID>` in the default registry |
| Product blob authentication | Connection-string or managed-identity environment variable **names**, specified in the registry |
| Legacy data compatibility | `SELFCARE_TENANT_STRICT_DATA_ISOLATION` (default `false`) |
| Tenant enforcement | `TENANT_ENFORCEMENT_ENABLED` (default `true`; do not disable in deployed stacks) |

`<ID>` is `AR` or `PNPG`. Registry entries reference secret variable names, never secret values.
See `src/main/resources/application.properties` for the registry structure and
[the app documentation](../../docs/apps/user-ms.md) for deployment settings.

All six Terraform stacks still enable **one tenant per Container App**, using the existing database,
storage account, identity and Key Vault. With strict mode disabled, reads include `tenantId=null`
documents **only in that tenant's database**. Do not mix untagged AR and PNPG data in one database.
Enable strict mode only after the documented backfill verification.

Product catalogues are loaded at startup and refreshed every minute. Lookups read the published
tenant snapshot without storage I/O or waiting for refresh. A failed refresh is logged and keeps
the last loaded catalogue; readiness remains DOWN if any tenant has not been loaded yet.


## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

For some endpoints

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8083/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Related Guides


### RESTEasy Reactive

Easily start your Reactive RESTful Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)

### OpenAPI Generator

Rest client are generated using a quarkus' extension.

[Related guide section...](hhttps://github.com/quarkiverse/quarkus-openapi-generator)

## Notifications and downstream services

| Purpose | Environment variables |
|---|---|
| REST endpoints (global per process) | `USER_REGISTRY_URL`, `ONBOARDING_URL`, `USER_GROUP_URL`, `WEBHOOK_URL`, `ONE_MAIL_URL` |
| Tenant mail | `TENANT_<ID>_MAIL_ENABLED`, `TENANT_<ID>_MAIL_SENDER`, `TENANT_<ID>_ONE_MAIL_API_KEY`, `TENANT_<ID>_SELFCARE_URL` |
| Tenant templates | `TENANT_<ID>_{ACTIVATE,DELETE,SUSPEND,REQUEST,CONVENTION,CREATE_MULTI_ROLE,CREATE_SINGLE_ROLE}_TEMPLATE_ID` |
| Tenant notifications | `TENANT_<ID>_WEBHOOK_ENABLED`, `TENANT_<ID>_EVENTHUB_USERS_ENABLED` |
| Tenant Service Bus receiver | `TENANT_<ID>_INTERNALEVENTS_{ENABLED,NAMESPACE,CLIENT_ID,CONNECTION_STRING}` |
| Mail retry | `USER_MS_RETRY_MIN_BACKOFF` (5 seconds), `USER_MS_RETRY_MAX_BACKOFF` (60 seconds), `USER_MS_RETRY` (3) |

Mail remains enabled for **AR and PNPG in PROD**, disabled in DEV/UAT. Sending stays asynchronous:
each job receives the resolved tenant ID and sends that tenant's key explicitly on every OneMail
attempt, without relying on a request context.
Webhooks, `sc-users` publishing and internal-event receivers remain AR-only in the current deployments.
The application does not publish `selfcare-fd` messages.

## EventHub messages

Messages via EventHub are sent in the following cases:
- Creation of a new user or assignment of a new role to an already registered user
- Modification of data for an already registered user (first name, last name, email)
- Update of the status of a product associated with the user

To perform local tests of functionalities that involve sending messages to the SC-User topic, the following environment variables must be set with the values corresponding to the environment to which you want to send the messages:
- EVENT_HUB_BASE_PATH
- SHARED_ACCESS_KEY_NAME
- EVENTHUB-SC-USERS-SELFCARE-WO-KEY-LC

## Cucumber Tests (Integration Tests)

From the repository root, run unit tests with `mvn -f apps/user-ms/pom.xml test`.
For Cucumber, start Docker and run **from this app's directory**:

```shell
mvn test -Dtest=CucumberSuite
```

The suite starts `docker-compose.yml`, with separate AR/PNPG Mongo databases and product containers.
`multitenancy.feature` checks isolation, legacy records and rejected headers/JWT mismatches. On ARM
hosts, pull the user-group dependency with `docker pull --platform=linux/amd64 ghcr.io/pagopa/selfcare-user-group-ms:latest`.

## Rollout

Release the scheduler's `X-Tenant-Id` header change first, verify technical-token compatibility, then
release user-ms **code and Terraform together** through DEV, UAT and PROD. Secret names in Key Vault
are unchanged, but their Container App environment-variable names have changed. No deployment,
backfill or switch to strict mode is performed by these code changes.
