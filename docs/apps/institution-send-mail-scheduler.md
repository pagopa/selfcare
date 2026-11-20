# institution-send-mail-scheduler

> Batch job that periodically e-mails each institution's PEC address a summary of the users added to and removed from its products.

| | |
|---|---|
| Path | `apps/institution-send-mail-scheduler` |
| Stack | Quarkus 3.11.3, Java 17, reactive (Mutiny + reactive Panache MongoDB), Quarkus Mailer |
| Type | Batch job (Container App Job, `@QuarkusMain` that exits when done) |
| Tenants | SELC (`ar`) only |
| Infra | `infra/resources/institution-send-mail-scheduler/` |

## Responsibilities

- Read the `MailNotification` documents (written by [institution-ms](institution-ms.md)) due today.
- For each institution, ask [user-ms](user-ms.md) for its users on the notified products and count the users added (`ACTIVE`/`SUSPENDED`, created in the last N days) and removed (`DELETED`, updated in the last N days) per product.
- Render the HTML template stored in the contracts blob container and send it by SMTP to the institution `digitalAddress` (PEC).
- No REST API, no events.

## Architecture

Package `it.pagopa.selfcare.institution` (no README in the app folder):

| Class | Role |
|---|---|
| `Application` | `@QuarkusMain`: runs `retrieveInstitutionFromMailNotificationAndSendMail()` once and exits with code 0 |
| `service/InstitutionSendMailScheduledServiceImpl` | Paging over `MailNotification`, user counting, template choice, mail parameters (`id_institution`, `added_users_list`, `removed_users_list`) |
| `service/MailServiceImpl` | Loads the template from blob storage (`AzureBlobClient`) and sends via Quarkus Mailer; redirects every mail to `MAIL_DESTINATION_TEST_ADDRESS` when `MAIL_DESTINATION_TEST` is true (default `true`; `false` in prod) |
| `entity/MailNotification` | `@MongoEntity(collection = "MailNotification")`: `institutionId`, `productIds`, `digitalAddress`, `moduleDayOfTheEpoch`, `createdAt`, `updatedAt` |
| `config/*` | `AzureStorageConfig`, `ProductConfig` (product catalogue from `products.json`), `InstitutionSendMailConfig` |
| `client/auth/AuthenticationPropagationHeadersFactory` | Adds `Authorization: Bearer $JWT_BEARER_TOKEN` to user-ms calls |

### Scheduling logic

- `moduleDayOfTheEpoch = days(2024-01-01 → today) % PEC_NOTIFICATION_FREQUENCY` (default 30): only notifications with that value are processed, which spreads institutions across the period.
- Pages of `MAIL_QUERY_SIZE` (default 10) are processed with a 30 s delay between pages. With `SEND_ALL_NOTIFICATION=false` only the first notification of each page is sent (`false` in dev/uat, `true` in prod).
- Nothing is sent if the onboarding happened today or `digitalAddress` is missing. The "first notification" template is used while the notification is younger than the frequency period.

## API

None. The job is not exposed through APIM.

## Data and messaging

| Resource | Usage |
|---|---|
| MongoDB `selcMsCore.MailNotification` | Read only |
| Blob container `STORAGE_CONTAINER_CONTRACT` (documents storage) | Mail templates `contracts/template/mail/institution-user-list-{first-,}notification/<version>.json` |
| Blob container `STORAGE_CONTAINER_PRODUCT` | `products.json` (product titles in the mail) |
| SMTP server (PEC) | Outbound mail |

## Integrations

| Client | Spec | Used for |
|---|---|---|
| [user-ms](user-ms.md) `InstitutionApi` | `src/main/openapi/selfcare-user.json` (committed copy, generated with Quarkus OpenAPI Generator, Mutiny) | `GET /institutions/{institutionId}/user-institutions` |

Libraries: [selfcare-onboarding-sdk-azure-storage](../libs/selfcare-onboarding-sdk-azure-storage.md), [selfcare-onboarding-sdk-product](../libs/selfcare-onboarding-sdk-product.md), [selfcare-onboarding-sdk-common](../libs/selfcare-onboarding-sdk-common.md).

## Configuration

From `src/main/resources/application.properties` (secrets from Key Vault: `mongodb-connection-string`, `smtp-usr`, `smtp-psw`, `jwt-bearer-token-functions`):

| Group | Env vars |
|---|---|
| MongoDB | `MONGODB_CONNECTION_STRING` (database fixed to `selcMsCore`) |
| user-ms | `SELFCARE_USER_URL`, `JWT_BEARER_TOKEN` |
| Scheduling | `PEC_NOTIFICATION_FREQUENCY`, `MAIL_QUERY_SIZE`, `SEND_ALL_NOTIFICATION` |
| Mail | `MAIL_SERVER_HOST`, `MAIL_SERVER_PORT`, `MAIL_SERVER_USERNAME`, `MAIL_SERVER_PASSWORD`, `MAIL_SERVER_SMTP_AUTH`, `MAIL_SERVER_SMTP_TLS_ENABLE`, `MAIL_SERVER_SSL`, `MAIL_SENDER_ADDRESS`, `MAIL_DESTINATION_TEST`, `MAIL_DESTINATION_TEST_ADDRESS`, `MAIL_TEMPLATE_NOTIFICATION_PATH`, `MAIL_TEMPLATE_FIRST_NOTIFICATION_PATH` |
| Storage | `STORAGE_CONTAINER_CONTRACT`, `BLOB-STORAGE-CONTRACT-*`, `STORAGE_CONTAINER_PRODUCT`, `BLOB_STORAGE_PRODUCT_*` |

## Multitenancy

- SELC only: there is no PNPG stack and no tenant handling in the code; `MailNotification` is read without any tenant filter.
- The `MailNotification` collection in `selcMsCore` is in scope of the institution-ms multitenant persistence change (see [institution-ms](institution-ms.md#multitenancy)); this job does not use it yet.
- See [../multitenancy.md](../multitenancy.md) and [Database identification](../specs/MULTITENANT/Step_2/Database_identification.md).

## Build, run and test

```bash
mvn -f apps/institution-send-mail-scheduler/pom.xml clean package
mvn -f apps/institution-send-mail-scheduler/pom.xml test
mvn -f apps/institution-send-mail-scheduler/pom.xml quarkus:dev   # runs the job once against the configured env
mvn --projects :test-coverage --also-make verify -Pinstitution-send-mail-scheduler,report
```

Unit tests only (`InstitutionSendMailScheduledServiceImplTest`, `MailServiceImplTest`, `SwaggerConfigTest`); there is no Cucumber suite. The image is built by `apps/institution-send-mail-scheduler/Dockerfile` from the repository root.

## Deployment

- Container App Job `selc-<env>-inst-send-mail` (module `_modules/container_app_job`), image `selfcare-institution-send-mail-scheduler`, `replica_timeout_in_seconds = 28800`.
- Trigger: manual in `dev-ar` and `uat-ar`; cron `00 06 * * *` in `prod-ar`.
- PR: `.github/workflows/pr_institution_send_mail_scheduler.yml`, `.github/workflows/pr_institution_send_mail_scheduler_infra.yml`.
- Release: `.github/workflows/trigger_release_institution_send_mail_scheduler.yml` → `release_app.yml` with `target=ar`; DEV auto-release via `trigger_deploy_after_merge.yml`.

## References

- [institution-ms](institution-ms.md), [user-ms](user-ms.md)
- [Architecture](../architecture.md), [Infrastructure](../infrastructure.md), [CI/CD](../ci-cd.md), [Testing](../testing.md)
