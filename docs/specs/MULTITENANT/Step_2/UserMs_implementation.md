# user-ms multitenant-ready (Step 2): piano attività

Lista delle attività per rendere `apps/user-ms` multitenant-ready come `apps/onboarding-ms`.
Riferimenti: [ARCHITECTURE](ARCHITECTURE.md), [Database](Database_identification.md),
[Storage](Storage_identification.md), [JWT](JWT_Key_Resolution.md), [multitenancy](../../../multitenancy.md).

## Obiettivo e perimetro

- Il codice deve poter servire AR e PNPG dallo stesso processo: `TenantContext`, tenant registry e
  routing per tenant di Mongo, storage e credenziali.
- Il deploy resta invariato: `selc-<env>-user-ms` (AR) e `selc-<env>-pnpg-user-ms` (PNPG). Ogni stack
  abilita un solo tenant (`TENANT_SUPPORTED_TENANTS=AR` oppure `PNPG`), come fa onboarding-ms
  (`infra/resources/onboarding-ms/dev-ar/onboarding.tf:208-214`).
- Non è prevista alcuna modifica al contratto OpenAPI: il tenant arriva nell'header `X-Tenant-Id`
  (query param disabilitato).

## Decisioni

| # | Decisione | Modello di riferimento |
|---|---|---|
| D1 | Catalogo prodotti letto dal blob del tenant (storage logico `products` nel registry), tramite un `ProductService` tenant-aware. | `apps/institution-ms/core/src/main/java/it/pagopa/selfcare/mscore/core/config/TenantProductCatalogService.java` |
| D2 | Query con filtro `tenantId = T OR tenantId IS NULL`, che diventa strict con `SELFCARE_TENANT_STRICT_DATA_ISOLATION=true`. | `apps/document-ms/src/main/java/it/pagopa/selfcare/document/repository/DocumentRepository.java:175-186` |
| D3 | Le configurazioni per tenant non previste dal `TenantDefinition` Quarkus (OneMail, URL, template, Event Hub, webhook, internal events) diventano proprietà `tenant.<id>.*` di user-ms. `selfcare-sdk-tenant` non si modifica. | `apps/auth/src/main/java/it/pagopa/selfcare/auth/conf/TenantOutboundMailConfig.java` |
| D4 | Enforcement attivo (`tenant.enforcement.enabled=true`): una richiesta senza `X-Tenant-Id` riceve 400. Prima va corretto l'unico chiamante che non invia l'header (UMS-01). | `apps/onboarding-ms/src/main/resources/application.properties` |

## Stato dell'implementazione

| Task | Stato |
|---|---|
| UMS-01 | Header configurabile e test implementati; rilascio scheduler ancora da eseguire prima di user-ms |
| UMS-02 | **Operativo, da eseguire**: verificare issuer/claim dei token reali, senza copiarli nel repository |
| UMS-03–17, UMS-19–24 | Codice, configurazioni, Terraform e test implementati; Container App e risorse restano separate |
| UMS-18 | Lookup prodotti differito sul worker pool; verifica della propagazione inclusa nella regressione |
| UMS-25 | README, documentazione applicativa, multitenancy e runbook aggiornati |
| UMS-26 | **Operativo, da eseguire**: deploy ordinato e smoke test DEV → UAT → PROD |

Le mail restano in background, come confermato: `TenantMailJob` riceve il tenant catturato dalla
richiesta e mantiene un contesto indipendente fino alla fine di filtri OneMail e retry. La risposta HTTP
non attende OneMail. Non sono stati eseguiti deploy, modifiche ai segreti remoti o backfill.

### Verifiche locali eseguite

| Verifica | Comando / perimetro | Esito |
|---|---|---|
| Unit test dei due moduli, come nel build Sonar | `mvn -T 1C -q clean test --projects apps,apps/user-ms,apps/institution-send-mail-scheduler --also-make -Dquarkus.http.test-port=0 -Dquarkus.management.test-port=0 -Dquarkus.generate-code.serialize-test-model=false` | **421 test user-ms e 11 scheduler passati**, nessun errore o test saltato |
| Cucumber, comando CI dalla root | `mvn --projects :user-ms --also-make test -Dtest=CucumberSuite -Dsurefire.failIfNoSpecifiedTests=false` | **291 scenari passati**, 3150 step; AR e PNPG nello stesso processo |
| Packaging e controlli finali codec/worker | `mvn -q -f apps/user-ms/pom.xml package '-Dtest=MongoConfigTest,UserServiceTest#testSendMail'` | Passato |
| Header scheduler | `mvn -q -f apps/institution-send-mail-scheduler/pom.xml test -Dtest=AuthenticationPropagationHeadersFactoryTest` | **2 test passati** |
| Terraform | `terraform validate -no-color` in ciascuno dei sei stack; `terraform fmt -check infra/resources/user-ms/{dev,uat,prod}-{ar,pnpg}/main.tf` | Tutti validi/formattati; nessun apply |
| Compatibilità | Diff OpenAPI; confronto dei valori app settings e riferimenti Key Vault prima/dopo la rinomina | OpenAPI invariata; valori esistenti preservati, nessun secret duplicato |

Correzioni CI: cleanup delle fixture completato prima della chiusura del contesto tenant; raccolta
JaCoCo estesa a tutti i package applicativi e ai test JUnit non Quarkus. La copertura locale di righe
e condizioni nuove, dai soli unit test dopo `clean`, è **87,8%**; la soglia Sonar resta **80%**.
Il valore del quality gate sulla PR richiede una nuova analisi SonarCloud.

Maven è il fallback locale: Nx non è utilizzabile per la configurazione TypeScript del plugin del
workspace. La verifica operativa dei token, i deploy e gli smoke test sugli ambienti restano da eseguire.

## Stato iniziale (prima delle modifiche)

- **Librerie**: nessuna libreria tenant; `selfcare-sdk-security` è alla 0.3.0 (`apps/user-ms/pom.xml:30`).
- **Mongo**:
  - connessione unica: `quarkus.mongodb.connection-string` e `quarkus.mongodb.database=selcUser`;
  - `UserInstitution` non ha `tenantId`;
  - tutti gli accessi sono chiamate statiche Panache in `service/UserInstitutionServiceDefault.java` (righe 62, 68, 74, 174, 246, 319, 335, 366, 388, 419, 432, 436, 454).
- **Prodotti e health**: un solo blob client per i prodotti (`conf/UserMsConfig.java`); readiness check a tenant singolo (`health/*`).
- **`X-Tenant-Id`**: viene solo copiato dall'header in ingresso verso i client REST (`client/auth/AuthenticationPropagationHeadersFactory.java`) e scritto nei log.
- **Valori fissi per stack**:
  - tenant del webhook: `USER_MS_WEBHOOK_TENANT_ID` (`service/UserNotificationServiceImpl.java:66-67,148`);
  - template e chiave OneMail, `SELFCARE_URL`;
  - Event Hub e internal events abilitati solo su AR (`infra/resources/user-ms/*/main.tf`).
- **Flussi fuori da una richiesta HTTP**: `internalevents/InternalEvents.java` (Service Bus) e `util/product/ProductIdNormalizer.java:70` (`@Scheduled` ogni 10 minuti).

## Attività

Dimensione indicativa: **S** ≤ 1 gg, **M** 2–3 gg, **L** > 3 gg.

### A. Prerequisiti (fuori da user-ms)

- **UMS-01 (S) – `X-Tenant-Id` in institution-send-mail-scheduler.**
  - Oggi `apps/institution-send-mail-scheduler/src/main/java/.../client/auth/AuthenticationPropagationHeadersFactory.java` invia solo `Authorization`.
  - L'app è deployata solo su AR: aggiungere `X-Tenant-Id: AR` (valore da config).
  - Va rilasciata **prima** di user-ms, altrimenti con D4 le sue chiamate ricevono 400.
  - Gli altri chiamanti (auth, onboarding-ms, onboarding-functions, dashboard-bff, external-api, onboarding-bff, institution-ms, APIM via `jwt_base_policy.xml.tpl`) inviano già l'header.
- **UMS-02 (S) – Verifica dei token tecnici dei chiamanti.**
  - Indicizzando `selfcare-sdk-security` (UMS-04) si attiva `JwtTenantValidationFilter`.
  - Un token `SPID` senza claim `tenant_id` viene attribuito a `DEFAULT_TENANT` (default `PNPG`). Se il tenant non coincide con `X-Tenant-Id`, la richiesta riceve 401 (`libs/selfcare-sdk-security/.../JwtTenantValidator.java`).
  - Verificare issuer e claim dei token statici `JWT_BEARER_TOKEN` usati verso user-ms (es. institution-send-mail-scheduler) e dei token APIM e auth (questi includono `tenant_id`).

### B. Dipendenze e configurazione di base

- **UMS-03 (S) – `pom.xml`.**
  - Aggiungere `selfcare-sdk-tenant` e `selfcare-sdk-tenant-mongodb`; aggiornare `selfcare-sdk-security` (oggi 0.3.0).
  - Versioni correnti in `libs/`: security 0.8.2, tenant 0.7.0, tenant-mongodb 0.4.2 (onboarding-ms usa 0.8.0 / 0.2.0 / 0.1.0). Verificare che siano pubblicate su GitHub Packages.
  - Valutare l'allineamento di Quarkus da 3.11.1 a 3.11.2, il BOM delle librerie.
- **UMS-04 (M) – `src/main/resources/application.properties`, come in onboarding-ms.**
  - `tenant.registry.json`: JSON letterale con AR e PNPG, senza `${VAR:default}` (vedi il commento in onboarding-ms). Per ogni tenant:
    - `mongo` (`database=selcUser`, `connectionStringEnvVar=MONGODB_CONNECTION_STRING_<ID>`);
    - `jwt.publicKeyEnvVar=JWT_PUBLIC_KEY_<ID>`;
    - `storages.products`;
    - `userRegistry.apiKeyEnvVar=USER_REGISTRY_API_KEY_<ID>`.
  - Proprietà tenant:
    - `tenant.supported-tenants=${TENANT_SUPPORTED_TENANTS:AR,PNPG}`;
    - `tenant.enforcement.enabled=${TENANT_ENFORCEMENT_ENABLED:true}`;
    - `tenant.default=${TENANT_DEFAULT:AR}`;
    - `tenant.storage.mandatory-keys=products`;
    - `tenant.user-registry.mandatory-tenants=${TENANT_SUPPORTED_TENANTS:AR,PNPG}`. Deve essere un sottoinsieme dei tenant supportati, altrimenti l'avvio fallisce (`TenantRegistry.java:396-402`).
  - Filtro di risoluzione: `selfcare.tenant.resolution-filter.enabled=true`, `selfcare.tenant.resolution-filter.query-param-enabled=false`.
  - Isolamento dati: `selfcare.tenant.strict-data-isolation=${SELFCARE_TENANT_STRICT_DATA_ISOLATION:false}`.
  - Indicizzazione: `quarkus.index-dependency.selfcare-sdk-security.group-id` e `.artifact-id`, necessarie per registrare `JwtTenantValidationFilter`.
  - `mp.jwt.verify.publickey` resta come fallback legacy.
  - Profilo test: `%test.tenant.enforcement.enabled=false` e `%test.tenant.default=AR`.
- **UMS-05 (S) – Mappatura degli errori tenant.**
  - In `exception/handler/ExceptionHandler.java`: `UnknownTenantException` → 400, `UnresolvedTenantException` → 401, entrambe come `application/problem+json`.
  - Modello: `apps/document-ms/.../exception/handler/ExceptionHandler.java:154-170`.
- **UMS-06 (S) – Log.**
  - Allineare `filter/CustomLoggingFilter.java` e i filtri client a `TenantLogUtils` di `selfcare-sdk-security`, come in onboarding-ms (`filter/*`).

### C. Persistenza Mongo

- **UMS-07 (S) – Client Mongo per tenant.**
  - Rimuovere `quarkus.mongodb.connection-string` e `quarkus.mongodb.database`, poi impostare `quarkus.mongodb.health.enabled=false`. È una proprietà build-time: il check Quarkus non ha un tenant.
  - Non usare `database` o `clientName` in `@MongoEntity`.
  - Convertire il vecchio producer `CodecRegistry` di `conf/MongoConfig.java` in un `CodecProvider` CDI con `DateCodec`, raccolto da `TenantMongoClientProducer`. Verificare con i test la (de)serializzazione di `OffsetDateTime`.
- **UMS-08 (S) – Discriminatore.** Aggiungere il campo `tenantId` a `entity/UserInstitution.java`. Valutare l'indice Cosmos su `tenantId` (onboarding-ms non l'ha aggiunto).
- **UMS-09 (L) – Repository tenant-scoped.**
  - Creare `repository/UserInstitutionRepository`, modellato su `apps/onboarding-ms/.../repository/OnboardingRepository.java`, con il predicato di D2.
  - Spostarvi tutte le chiamate statiche di `UserInstitutionServiceDefault` (righe elencate sopra), comprese `mongoCollection().find(...)` (174), `mongoCollection().updateMany(...)` (246) e `update(...).where(...)` (319, 366, 419).
  - `findById` filtra il tenant dopo la lettura.
  - `persistOrUpdate` imposta il `tenantId` corrente e rifiuta un documento di un altro tenant: in modalità non strict marca anche i documenti legacy che riscrive.
  - Adeguare i test che oggi usano `PanacheMock` su `UserInstitution`.

### D. Catalogo prodotti (D1)

- **UMS-10 (M) – `ProductService` tenant-aware.**
  - Sostituire `productBlobClient` e `productService` in `conf/UserMsConfig.java` con una `ProductServiceCacheable` per tenant, creata in modo lazy.
  - Il blob client per tenant si costruisce da `tenantRegistry.storage(tenant, "products")` più `storageConnectionString` o `storageManagedIdentityClientId`. Usare i costruttori di `AzureBlobClientDefault` già presenti in `UserMsConfig`.
  - Esempio Quarkus di client blob dal registry: `apps/product/.../storage/TenantBlobClientProvider.java`.
  - Rimuovere `user-ms.blob-storage.*`, tranne `filepath-product`.
- **UMS-11 (S) – `util/product/ProductIdNormalizer.java`.**
  - La mappa statica `PARENT_IDS` è globale e `updateParentIds()` (`@Scheduled`) gira senza `TenantContext`.
  - Renderla una mappa per tenant, aggiornata iterando `tenantRegistry.supportedTenantIds()` con il tenant esplicito.
  - `normalize()` usa il tenant corrente.

### E. Chiamate in uscita

- **UMS-12 (S) – Propagazione del tenant.**
  - `client/auth/AuthenticationPropagationHeadersFactory.java` prende `X-Tenant-Id` da `TenantContext` (con fallback sull'header in ingresso), lo normalizza e lo valida con il registry; un conflitto è un errore.
  - Copiare `apps/onboarding-ms/.../client/auth/AuthenticationPropagationHeadersFactory.java:40-48`.
  - Riguarda i client `onboarding_ms`, `webhook_ms` e `user_group_ms`.
- **UMS-13 (S) – Chiave PDV per tenant.**
  - Per `user_registry_json` impostare `enable-security-generation=false` e rimuovere `quarkus.openapi-generator.user_registry_json.auth.api_key.api-key`.
  - Registrare un `ClientRequestFilter` che imposti `x-api-key = tenantRegistry.userRegistryApiKey(tenant)`.
  - Modello: `apps/auth/.../client/TenantUserRegistryApiKeyFilter.java`.

### F. Notifiche, mail ed eventi (D3)

Prefisso proposto: `tenant.<id>.*`, variabili d'ambiente `TENANT_<ID>_*` (convenzione di auth).

- **UMS-14 (M) – Mail e OneMail per tenant.** Configurazione per tenant (`TenantNotificationConfig`, validata all'avvio per i tenant supportati):
  - `selfcare-url`;
  - `mail.enabled`;
  - `mail-sender`;
  - `one-mail.api-key`;
  - i 7 template `onemail.template.*`.

  Altre modifiche:
  - `one_mail_json`: `enable-security-generation=false` più un `ClientRequestFilter` per `x-api-key`.
  - Usare la config del tenant corrente in `service/OneMailServiceImpl.java` e `service/UserNotificationServiceImpl.java`.
  - `ONE_MAIL_URL` e `USER_REGISTRY_URL` restano globali: oggi hanno lo stesso valore su AR e PNPG di ogni ambiente.
  - Conservare l'invio in background con un contesto tenant dedicato al job, indipendente dalla richiesta HTTP.
- **UMS-15 (S) – Webhook.**
  - In `service/UserNotificationServiceImpl.java:148` il `tenantId` della `NotificationRequest` deve venire da `TenantContext`.
  - Sostituire `user-ms.webhook.enabled` / `tenant-id` con `tenant.<id>.webhook.enabled` (AR `true`, PNPG `false`, come oggi).
  - `user-ms.webhook.topic` e `WEBHOOK_URL` restano globali.
- **UMS-16 (S) – Event Hub `sc-users`.**
  - Abilitazione per tenant: `tenant.<id>.eventhub.users.enabled`, oggi solo AR.
  - Endpoint e credenziali restano unici (AR); un'abilitazione PNPG viene rifiutata all'avvio finché non esiste un endpoint dedicato.
  - L'injection del client `selfcare-fd` era inutilizzata: rimossa, insieme ai flag Terraform inutilizzati. Nessun nuovo flusso FD introdotto.

### G. Flussi senza richiesta HTTP

- **UMS-17 (M) – `internalevents/InternalEvents.java`.**
  - Configurazione per tenant: `tenant.<id>.internalevents.{enabled,namespace,client-id,connection-string}`, oggi solo AR.
  - Un receiver per ogni tenant abilitato.
  - L'handler attiva un request context (es. `@ActivateRequestContext`; non ci sono precedenti nel repo) e imposta `TenantContext` al tenant del receiver.
  - Se l'evento contiene `eventTenant` (`InternalEvent.java:22`) ed è diverso dal tenant del receiver, l'evento va rifiutato (dead-letter). institution-cdc oggi imposta `AR` fisso (`InternalEventsMapper.java:12`).
- **UMS-18 (S) – Verifica del worker pool.** Controllare la propagazione di `TenantContext` sul worker pool in `service/UserServiceImpl.java:676-678` (`runSubscriptionOn`); `quarkus-smallrye-context-propagation` è già presente.

### H. Health check

- **UMS-19 (S) – Mongo.** `health/UserMongoReadinessCheck.java` deve fare il ping per ogni tenant supportato con `TenantMongoClientProducer.clientForTenant(...)`. Modello: `apps/onboarding-ms/.../health/OnboardingMongoReadinessCheck.java`.
- **UMS-20 (S) – Blob.** `health/ProductBlobStorageReadinessCheck.java` deve verificare `products.json` per ogni tenant. Modello: `apps/document-ms/.../health/TenantBlobStorageReadinessCheck.java`.

### I. Test

- **UMS-21 (M) – Unit test.**
  - Configurare il registry AR+PNPG in `src/test/resources/application.properties`.
  - Coprire i casi di isolamento:
    - repository: scoping, `findById` di un altro tenant, `persistOrUpdate` in conflitto, modalità strict e non strict;
    - header factory e filtri PDV/OneMail;
    - mail, webhook ed Event Hub per tenant;
    - `ProductIdNormalizer`;
    - readiness;
    - `InternalEvents`.
  - Modelli: `apps/onboarding-ms/src/test/java/.../client/auth/*Test.java`, `conf/TenantRegistryConfigurationTest.java`.
- **UMS-22 (M) – Cucumber.**
  - In `src/test/resources/application-test.properties` usare il registry con due tenant e database distinti sullo stesso Mongo.
  - Aggiungere `tenantId` ai fixture `src/test/resources/db/userInstitutions.json`, più alcuni documenti legacy senza `tenantId`.
  - In `blobStorageInit/initAzurite.sh` creare un container prodotti per tenant.
  - Scenari: stesso utente o ente su tenant diversi non visibile, tenant mancante → 400, tenant non supportato → 400.
  - Aggiungere `libs/selfcare-sdk-tenant/**` e `libs/selfcare-sdk-tenant-mongodb/**` ai `paths` di `.github/workflows/integration_tests_user_ms.yml`, come fa onboarding-ms.

### J. Infrastruttura (6 stack `infra/resources/user-ms/{dev,uat,prod}-{ar,pnpg}/main.tf`)

- **UMS-23 (M) – App settings.**
  - Aggiungere:
    - `TENANT_SUPPORTED_TENANTS` (`AR` o `PNPG`);
    - `TENANT_REGISTRY_JSON` con il solo tenant dello stack: `mongo` (account, `selcUser`), `jwt`, `userRegistry` e `storages.products` (account `data.azurerm_storage_account.product_storage.name`, container `selc-<env_short>-product`, `MANAGED_IDENTITY` con `managedIdentityClientIdEnvVar`), come in onboarding-ms;
    - `SELFCARE_TENANT_STRICT_DATA_ISOLATION = tostring(module.local.config.strict_tenant_data_isolation)`.
  - Rinominare le variabili per tenant (D3) in `TENANT_<ID>_*`: `SELFCARE_URL`, `MAIL_*`, `*_TEMPLATE_ID`, `USER_MS_WEBHOOK_*`, `USER_MS_EVENTHUB_*_ENABLED`, `INTERNALEVENTS_*`.
  - Rimuovere `BLOB-STORAGE-PRODUCT-ACCOUNT-NAME` e `STORAGE_CONTAINER_PRODUCT`. L'env con il client id della managed identity resta, ma è referenziato dal registry.
- **UMS-24 (S) – `secrets_names`.** Rinominare le chiavi; i secret Key Vault non cambiano:

  | Prima | Dopo |
  |---|---|
  | `MONGODB-CONNECTION-STRING` | `MONGODB_CONNECTION_STRING_<ID>` |
  | `JWT-PUBLIC-KEY` | `JWT_PUBLIC_KEY_<ID>` |
  | `USER-REGISTRY-API-KEY` | `USER_REGISTRY_API_KEY_<ID>` |
  | `ONE_MAIL_API_KEY` | `TENANT_<ID>_ONE_MAIL_API_KEY` |

  Il modulo `container_app_microservice` non deduplica i valori (`_modules/container_app_microservice/locals.tf:20-31`): non si possono mappare il vecchio e il nuovo nome sullo stesso secret, quindi codice e infrastruttura vanno rilasciati insieme.

### K. Documentazione e rilascio

- **UMS-25 (S) – Documentazione.** Aggiornare:
  - `apps/user-ms/README.md`;
  - `docs/apps/user-ms.md`, sezioni "Configuration" e "Multitenancy";
  - la tabella "Status per app" e le variabili d'ambiente in `docs/multitenancy.md`;
  - l'elenco dei servizi collegati al flag strict in `docs/specs/MULTITENANT/Step_1/scripts/README.md`.
- **UMS-26 (S) – Rilascio e verifica.**
  - Ordine: UMS-01, poi user-ms (codice e Terraform insieme), DEV → UAT → PROD.
  - In ogni ambiente:
    - readiness `UP`;
    - richieste senza o con altro `X-Tenant-Id` rifiutate;
    - flussi BFF, APIM e onboarding invariati;
    - mail abilitate in PROD su AR **e PNPG**, disabilitate in DEV/UAT; webhook, Event Hub e internal events solo AR, come prima.
  - Il passaggio a strict (`strict_tenant_data_isolation_by_env`) è una decisione d'ambiente, da prendere solo dopo `backfill_tenant_id.py --verify` su `selcUser` per entrambi i tenant.

## Fuori perimetro (necessario solo per consolidare in un unico container)

- **Key Vault**: `container_app_microservice` legge i secret da un solo Key Vault (`locals.tf:20-25`), mentre i secret AR e PNPG sono in Key Vault diversi.
- **URL dei servizi a valle**: `ONBOARDING_URL`, `USER_GROUP_URL` e `WEBHOOK_URL` puntano oggi a Container App diverse per tenant. Vanno consolidati anche i servizi a valle, oppure servono URL per tenant.
- **Endpoint per PNPG**: Event Hub e internal events non hanno oggi endpoint PNPG.
- **APIM**: punta a due backend distinti (`apim_external_api/apim.tf` e `apim_pnpg.tf`, `MS_BACKEND_URL`).
- **user-cdc**: legge il change stream di `userInstitutions` e va valutato separatamente.
