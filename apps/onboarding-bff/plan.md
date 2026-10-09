# Piano di completamento della migrazione onboarding-bff

## Obiettivo e stato

**Epic BFF-ARCH-01:** riallineare onboarding-bff alla struttura e ai pattern Quarkus di
`apps/document-ms` e `apps/product`, mantenendo il contratto esterno richiesto.
L'obiettivo non e aumentare il numero di classi o ridurne arbitrariamente la lunghezza:
e eliminare responsabilita improprie e compatibilita custom non necessaria.

Piano del **2026-10-08**, sul branch **`feature/migration-bff-quarkus`**.
Baseline da cui partire: `07844c25c05ae511dfecb1767886185a47fd89e4`.
**BFF-ST01 completata il 2026-10-08:** inventario, consumer e decisioni tracciati,
regressioni di caratterizzazione aggiunte e riferimento Spring riconfermato.
Commit ST01: `2f821d6cd2f586bca3c7f0d178d0818cb844f88d`.
**BFF-ST02 completata il 2026-10-08:** layout, DTO, injection e conversioni pure
riallineati, mantenendo contratto pubblico e comportamento downstream.
**BFF-ST03 in corso dal 2026-10-09:** traccia completa e primo lotto reattivo
verificati; la storia non e ancora completata. **ST04-ST10 restano da fare.**
Gli identificativi sono locali al piano, non ticket Jira gia creati.

**Integrazione main del 2026-10-09:** incorporato `cd0a2751f`, preservando ST01/ST02.
Il cambiamento funzionale `5c7f53b8e` viene portato nel Quarkus: `/contract`
resta autenticato ma non controlla IAM; `/backstage/contract` controlla il permesso
documentale. Il fallback per i soli permessi view include `userRequester.userRequestUid`.
I conflitti modify/delete sono risolti nei sorgenti e test Quarkus, senza ripristinare
i moduli Spring eliminati. L'oracolo per i prossimi lotti e ora Spring main
`cd0a2751f`, esportato senza modifiche: SHA-256 FATJAR
`3f9e66de01d3c2ab5491223c3b188803b0a36b7d4e81196f457afb64ba9c946d`.
Il precedente FATJAR `ad37c6738` resta immutato. Il documento Spring di riferimento
proviene dal main aggiornato; canonical e alias Quarkus sono rigenerati da Maven.
Verifica del lotto: 667 test senza failure/errori/skipped, inclusi 297 casi
token/sicurezza/spec-driven su ciascun runtime e i gate OpenAPI esatti.
Le regressioni onboarding-ms arrivate da main passano: 178 test senza skip.
Le fixture Cucumber del contratto sono aggiornate e verificate insieme al primo
lotto ST03: 10 scenari contratto/prodotti passati con Docker. La suite completa
Cucumber non e stata rieseguita in questo lotto.

La baseline ha gia evidenze di parita HTTP (538 scenari Spring e 538 Quarkus),
59 scenari Cucumber e verifica del runtime container. Queste evidenze sono storiche:
non certificano il risultato del futuro refactoring. Il builder Maven del Dockerfile
e la pubblicazione dell'immagine completa non sono ancora certificati.
Restano inoltre gate amministrativi e di review, separati dal lavoro architetturale.

## Perimetro e regole di esecuzione

- Modificare il BFF e i suoi test, contratti generati e documentazione direttamente collegati.
  `document-ms` e `product` sono riferimenti in sola lettura.
- Lavorare sul branch corrente, con commit piccoli e coerenti; non creare altri branch
  o worktree e non riscrivere lo storico.
- Non introdurre nuove funzionalita, persistenza, framework interni, manager generici
  o infrastruttura per risolvere un problema di struttura.
- Preservare endpoint, JSON, errori, sicurezza, tenant, header, multipart, binari,
  timeout, numero e ordine delle chiamate downstream e politiche di retry.
- Una differenza intenzionale richiede una decisione esplicita prima di cambiare codice,
  golden file o comparatori. Il refactoring non autorizza variazioni di contratto.
- Non rinominare automaticamente le classi DTO: nomi degli schemi OpenAPI e messaggi
  di validazione possono dipenderne. Non accorpare modelli downstream e pubblici
  soltanto perche hanno campi simili.
- Eseguire regressioni mirate per ogni lotto. Aggiornare stato ed evidenze solo dopo
  aver verificato il risultato; non dichiarare completata una storia al solo commit.

### Struttura di riferimento

Adottare il layout di `document-ms` e i pattern comuni ai due riferimenti.
`product` colloca le implementazioni direttamente in `service`: non serve modificarlo.
I package sotto indicati descrivono responsabilita, non obbligano a creare classi vuote
o un'interfaccia aggiuntiva per ogni bean.

```text
controller/             binding HTTP e costruzione della risposta
service/                contratti dei servizi
service/impl/           orchestrazione e regole applicative
client/                 integrazioni HTTP e adattatori downstream
model/dto/request/      DTO pubblici di ingresso
model/dto/response/     DTO pubblici di uscita
model/error/            modelli degli errori pubblici
mapper/                 conversioni tra modelli
exception/handler/      traduzione centralizzata delle eccezioni
config/                 configurazione dichiarativa
security/               sole policy applicative non delegate a Quarkus/SDK
```

Non aggiungere `repository` o `entity`: il BFF non ha persistenza propria.

## Backlog e dipendenze

**ST01 e ST02 sono completate; ST03 e in corso; ST04-ST10 sono da fare.** Le decisioni esterne bloccate sono
elencate nel registro ST01 e non autorizzano rimozioni. Il completamento
dell'inventario non chiude l'epic. Le dipendenze sono prerequisiti di implementazione, non un obbligo di
lavorare in parallelo; i task di una storia si eseguono nell'ordine indicato.

| Storia | Risultato | Prerequisiti |
|--------|-----------|--------------|
| BFF-ST01 | Inventario e contratto di compatibilita motivato | Nessuno |
| BFF-ST02 | Package, DTO e mapping coerenti | BFF-ST01 |
| BFF-ST03 | Primo flusso reattivo completo: prodotti | BFF-ST02 |
| BFF-ST04 | Istituzioni, utenti, IAM e registri reattivi | BFF-ST03 |
| BFF-ST05 | Onboarding, token e documenti senza I/O nei controller | BFF-ST04 |
| BFF-ST06 | Binding e gestione errori essenziali e centralizzati | BFF-ST02 |
| BFF-ST07 | Sicurezza e policy HTTP con responsabilita distinte | BFF-ST01 |
| BFF-ST08 | OpenAPI e route di compatibilita ridotti al necessario | BFF-ST01 |
| BFF-ST09 | Trasporto e retry senza duplicazioni | BFF-ST05 |
| BFF-ST10 | Pulizia e certificazione della nuova architettura | BFF-ST05, BFF-ST06, BFF-ST07, BFF-ST08, BFF-ST09 |

Ordine operativo consigliato: ST01, ST02, ST03, ST04, ST05, ST06, ST07, ST08,
ST09, ST10. ST06/ST07/ST08 non devono attendere necessariamente tutti i flussi
verticali, ma modifiche concorrenti a filtri, configurazione o mapper vanno coordinate.
La rimozione finale di adattatori condivisi avviene solo quando tutti i consumer sono aggiornati.

### BFF-ST01 - Inventario e confine del contratto

**Storia:** come manutentore voglio sapere perche ogni adattatore esiste, per
semplificare senza eliminare comportamenti richiesti.

**Dove:** `src/main/java/it/pagopa/selfcare/onboarding/`, configurazione, catalogo
`src/test/java/it/pagopa/selfcare/onboarding/parity`, baseline Spring e consumer.

- [x] **BFF-ST01-T01 - Censire le responsabilita.** Confrontare le classi di produzione
  con la baseline Spring e distinguere connector rinominati, logica preesistente e
  aggiunte della migrazione. Registrare qui classe/metodo, responsabilita, consumer,
  requisito, azione proposta e test che lo protegge.
- [x] **BFF-ST01-T02 - Verificare i consumer reali.** Ricostruire uso di frontend,
  codegen, APIM, probe e integrazioni. Per ciascun dettaglio di compatibilita
  stabilire se e contrattuale, interno o non ancora verificato; non dedurlo dal solo OpenAPI.
- [x] **BFF-ST01-T03 - Definire le decisioni di rimozione.** Classificare in
  mantenere, semplificare o rimuovere, motivando ogni scelta. Per requisiti non chiari
  mantenere il comportamento corrente e segnare la decisione bloccata, senza inventare fallback.
- [x] **BFF-ST01-T04 - Fissare il riferimento verificabile.** Associare ai comportamenti
  i casi di parita esistenti e aggiungere soltanto le regressioni mancanti necessarie.
  Conservare immutabile l'oracolo Spring; registrare SHA e comandi delle evidenze.

**Accettazione:** tutte le classi di compatibilita hanno origine, requisito,
consumer e decisione tracciati; nessuna eliminazione e giustificata soltanto dal
numero di righe. I casi non verificabili hanno un blocco esplicito.

#### Registro ST01 - Inventario e origine

Stato dei quattro task: **completati**. Commit di riferimento della produzione:
`07844c25c05ae511dfecb1767886185a47fd89e4`; lotto ST01 identificato dal subject
`Record BFF compatibility inventory`. Questa storia non elimina adattatori, non sposta DTO e non
cambia firme, golden file, comparatori o documenti OpenAPI.

L'inventario comprende **225 file Java di produzione** nel package
`it.pagopa.selfcare.onboarding`; la baseline Spring ne contiene 257 nei suoi
moduli. I conteggi descrivono il perimetro, non un criterio di qualita.
I 48 nomi di file assenti nella baseline includono rinominazioni e sostituzioni
di classi Spring/SDK: non sono 48 nuove responsabilita applicative.
I client generati in `target` non sono inclusi: la loro origine e
`src/main/openapi` e la configurazione codegen in `application.properties`.

Nelle tabelle i package sono relativi al package BFF; i test sono relativi a
`src/test/java/it/pagopa/selfcare/onboarding`. "Mantenere" riguarda il
comportamento, non impone di conservarne l'implementazione attuale.
"Semplificare" o "rimuovere" e un'azione del lotto indicato, subordinata alla
stessa regressione verde.

| Perimetro completo | File | Origine e responsabilita | Consumer / requisito | Azione e protezione |
|--------------------|------|--------------------------|----------------------|---------------------|
| `controller` | 6 | Controller Spring `web/controller`; `ProductV1Controller` diventa `ProductController` | Frontend e integratori v1/v2; binding e risposta HTTP | Semplificare ST03-ST05, preservando `*ControllerTest` e gruppi `products`, `institutions`, `users`, `tokens` |
| `controller/request`, `controller/response` | 52 | Modelli pubblici Spring `web/model` | JSON, schema names, operation signatures e nomi delle violazioni | Spostare senza rinominare in ST02; test `model`, mapper e gate OpenAPI |
| `client/model` | 82 | Modelli connector Spring; `BinaryData`/`UploadedFile` sostituiscono `Resource`/`MultipartFile`, `RegistryUser` distingue l'utente certificato dal modello applicativo | Servizi, mapper e payload downstream; filename, bytes, campi certificati | Mantenere confini; `DocumentServiceTest`, `FileDataTest`, `UserServiceImplTest` e parita `tokens`/`users` |
| `model` | 5 | `AggregateInstitution`, `OnboardingVerify`, `RecipientCodeStatus`, `VerifyType` preesistenti; `UserAuthority` localizza l'enum dell'identita Spring/commons | Servizi, mapper e `InstitutionResource`; valori pubblici invariati | Mantenere; test mapper/modelli e gate OpenAPI |
| `mapper` | 7 | Mapper connector/web Spring consolidati, incluso `CertifiedFieldMapper` | Conversioni, null, ordine liste e date; nessuna decisione applicativa | Semplificare conversioni manuali in ST02; test mapper e parita dei flussi |
| `service`, `service/impl` | 19 | Servizi core Spring e facciate connector descritte sotto | Controller, autorizzazioni e altri servizi; sequenza e numero delle chiamate | Semplificare ST02-ST05; test `service` e parita HTTP |
| `client`, `client/transport`, `client/util` | 13 | 7 client/decoder, 5 adattatori di trasporto, parser filename descritti sotto | Servizi e client generati; wire downstream | Mantenere requisiti, semplificare ST04/ST05/ST09; test client e trasporto |
| `config` | 4 | Integrazione Jackson, adattamento springdoc e route framework Spring | JSON, frontend/codegen, APIM e probe | Decisioni puntuali sotto; ST06/ST08 |
| `security` | 9 | `AuthorizationService` preesistente; adattatori di identita, tenant, header e sicurezza Spring/commons | Tutte le richieste e integrazioni downstream | Decisioni puntuali sotto; ST07 |
| `exception`, `exception/handler`, `model/error` | 15 | 12 eccezioni, traduttore/risposte e `Problem`; dominio Spring e adattamenti del framework | Errori pubblici, decoder frontend e failure downstream | Mantenere contratto; semplificare ST06, test mapper/decoder e gruppi errori |
| `util` | 8 | `FileValidationUtils`, `PermissionConstants`, `PgManagerVerifier`, `Utils` preesistenti; 4 sostituzioni Spring/commons descritte sotto | Validazione file, regole dominio, binding, identita e logging | Mantenere regole; spostare solo responsabilita improprie; test `util` |
| `filter` | 5 | Adattatori Quarkus di logging server/client e MDC | Osservabilita, non contratto HTTP; nessun nuovo header pubblicato | Mantenere finche chiarito il requisito operativo; blocco ST01-D04 |

Le sei coppie `IamService/Impl`, `InstitutionService/Impl`, `ProductService/Impl`,
`TokenService/Impl`, `UserInstitutionService/Impl`, `UserService/Impl` derivano da
`core`, con integrazioni prima separate nei connector: `ProductMsConnectorImpl`
nel servizio prodotti, `IamConnectorImpl` nel servizio IAM,
`MsCoreConnectorImpl` e `OnboardingFunctionsConnectorImpl` nel servizio
istituzioni, `MsUserInstitutionConnectorImpl` nel servizio user-institution.
Le facciate seguenti derivano da connector, non da nuove funzionalita:

| Classe / metodo | Origine Spring | Responsabilita e consumer | Decisione / regressione |
|-----------------|----------------|--------------------------|------------------------|
| `DocumentService` | `DocumentMsConnectorImpl` | Download, HEAD, multipart e lettura/chiusura `Response`; usato da token e controller documentali | Semplificare ST05; mantenere retry solo dove presenti, bytes e filename; `DocumentServiceTest`, `tokens` |
| `OnboardingService` / `OnboardingServiceImpl` | `OnboardingMsConnectorImpl` | Facciata onboarding/billing/aggregati; usata da istituzioni e token | Semplificare ST05, senza introdurre un altro manager; `OnboardingServiceImplTest`, `institutions`/`tokens` |
| `PartyService.onboardingOrganization`, `getInstitutionsByUser` | `PartyConnectorImpl` | Mapping manuale, lookup figli/genitore, filtro tipo ente e arricchimento sequenziale | Conversioni pure nei mapper ST02; composizione ST04; `InstitutionServiceImplTest`, `institutions` |
| `PartyRegistryProxyService` | `PartyRegistryProxyConnectorImpl` | Lookup IPA/AOO/UO/geografie/Infocamere; usato dai servizi istituzioni | Semplificare ST04; preservare eccezione al retry Infocamere; `PartyRegistryProxyServiceTest`, `institutions`/`transport-failures` |
| `UserRegistryService` | `UserRegistryConnectorImpl` | Campi certificati, ordine `fl`, UUID e operazioni registry; usato da utenti/istituzioni | Mantenere 404 reale, non dedurre `Optional.empty` dal catch Spring non raggiunto dal decoder; `users :: validate-unknown-user-is-not-found` |
| `ClientRequestValidator.validated` | Bean Validation dei proxy generati Spring | Validazione del DTO downstream prima del wire, diversa dalla validazione del DTO pubblico | Mantenere finche provata equivalenza ST06; `ClientRequestValidatorTest`, parita `institutions` e assenza di chiamate |
| `*.await().indefinitely()` nei servizi | Ponte della migrazione tra API sincrona e client Mutiny | Scelta interna, non requisito di bloccare il thread | Rimuovere ST03-ST05 propagando `Uni` a tutti i consumer; preservare ordine, retry e contesto |
| `InstitutionV2Controller.toUploadedFile`, `TokenV2Controller.toUploadedFile` | Lettura `MultipartFile` Spring, ora `Files.readAllBytes` | I/O nel controller, non requisito pubblico | Rimuovere dal controller ST05 con boundary worker e durata file verificata; parita upload `institutions`/`tokens` |

#### Registro ST01 - Adattatori e decisioni

| Classe / metodo | Origine e requisito | Consumer | Decisione e storia | Protezione |
|-----------------|--------------------|----------|--------------------|------------|
| `OpenApiContractFilter.declareAuthentication`, `declareGlobals`, `problemSchema` | `SwaggerConfig` e advice Spring: bearerAuth, operation security, risposte e `Problem` | Frontend generato e APIM; contrattuale | Semplificare dichiarazioni in ST08, non riempire arbitrariamente lo schema `Problem` | `QuarkusOpenApiInventoryTest`, `PublishedOpenApiGateTest`, `OpenApiIdenticalDocumentGateTest` |
| `filterOperation`, `filterParameter`, `filterRequestBody`, `filterSchema`, `expand` | Rappresentazione springdoc: placeholder, optional/required, style e vincoli non pubblicati | Codegen reale e gate di uguaglianza; requisito di rimozione non provato per ogni trasformazione | Mantenere; semplificazione ST08 bloccata da ST01-D01 dove cambia il documento | Stessi gate OpenAPI |
| `inlineLeafSchemas`, `inline`, `absorb`, `describeItems`, `describeBinaryContent` | Enum/date/UUID inline, allOf, ref, descrizioni e binari come springdoc | Converter Swagger 2, generator e patch frontend | Mantenere finche eseguita la catena consumer ST08; ST01-D01 | Gate di uguaglianza e alias |
| `declareServers`, `declareTags`, `sortResponses`, `keepTrailingSlashes` | Host, tag, ordine e slash springdoc | Pubblicazione stabile, APIM/frontend e gate; non requisito di ordinamento HTTP | Mantenere rappresentazione, non indebolire il comparatore ST08; ST01-D01 | `OpenApiIdenticalDocumentGateTest`, `LegacyOpenApiAliasGateTest` |
| `SpringCompatibleRoutes.register` - health | Actuator Spring, stato ora letto da SmallRye Health | Probe liveness/readiness/startup Terraform | Mantenere `/actuator/health`, metodi e media type ST08 | `http-contract :: health-is-public`, `health-head-is-public` |
| Stesso metodo - actuator index, `/error`, Swagger, `/v3/api-docs` | Route framework e whitelist Spring | Docs: parita HTTP; consumer operativi aggiuntivi non dimostrati | Mantenere alias/semantica; rimozione route accessorie bloccata ST01-D03 | `actuator-index-is-public`, `error-endpoint-is-public`, `api-docs-are-public`, `swagger-ui-html-redirects` |
| `BffSecurityFilter.filter`, `check` | Security chain Spring/commons e firewall URL/metodi | Tutte le richieste, anche route inesistenti | Semplificare responsabilita ST07; mantenere 400/401/403/500, ordine JWT/tenant e URL ammessi | `SecurityGateRulesTest`, `security`, `http-contract`, `http-boundaries` |
| `BffSecurityFilter.responseHeaders`, `isCorsPreflight` | Header Spring Security e CORS applicativo disabilitato | Browser; CORS consentito da APIM, non dal BFF | Semplificare solo con equivalenza ST07; forwarding deployato bloccato ST01-D02 | `http-contract` su successo/errori, `HttpsParityTest` su entrambi i runtime |
| `BffJwtCallerPrincipalFactory.parse`, `Verifier` | Strategie JWT commons: firma, issuer SPID/PAGOPA, exp/nbf solo se presenti, uid opzionale | Sessioni frontend e integratori | Mantenere; non attivare automaticamente factory SDK con requisiti diversi ST07 | `BffJwtCallerPrincipalFactoryTest`, `security` |
| `TenantPolicy.isValid`, `IdentityClaims.areStrings` | `JwtAuthenticationFilter` / costruzione identita Spring | SPID: tenant esatto AR/PNPG, default token PNPG; PAGOPA: policy distinta | Mantenere; nessuna normalizzazione/obbligatorieta aggiuntiva ST07 | `SecurityGateRulesTest`, `security` |
| `SecurityPaths.isPublic`, `SecurityProblems.*` | Whitelist ed entry point Spring | Probe, docs e chiamanti non autenticati | Mantenere whitelist e corpo/header errori; coordinare con route ST07/ST08 | `SecurityGateRulesTest`, `security`/`http-contract` |
| `AuthorizationService.hasPermission` | Servizio web Spring e method security | Token/documenti: IAM e richiedente onboarding solo per permessi view | Mantenere policy; semplificare catena e precondizioni duplicate ST04/ST07 | `AuthorizationServiceTest`, `tokens :: retrieve-requester-allowed-by-onboarding-user`, `approve-denied-even-for-requester` |
| `SecurityIdentityUtils.*` | `SelfCareUser` e claim letti dalle strategie commons | Controller/servizi/autorizzazioni | Mantenere uid, claim raw e precedenze fiscal code; semplificare solo con stessa identita ST07 | `SecurityIdentityUtilsTest`, `security`/`tokens` |
| `AuthenticationPropagationHeadersFactory.update` | Interceptor Authorization commons e `TenantHeaderInterceptor` | Tutti i client REST | Mantenere primo bearer/tenant non blank, senza aggiungere forwarding di trace header ST04/ST07 | `AuthenticationPropagationHeadersFactoryTest`, `retrieve-propagates-identity`, `tracing-headers-are-not-forwarded-downstream` |
| `DownstreamApiKeyFilter.filter` | `ApiKeyRequestInterceptor` globale Spring | Tutti i downstream, non solo registry | Mantenere comportamento osservato; non restringere scope ST07/ST09 | `DownstreamApiKeyFilterTest`, controllo api-key comune in `ParityCatalog.all` |
| `RequestParams.*` | Binding/conversioni MVC implicite | Tutti i controller; parametri ripetuti, mancanti/vuoti, booleani e numeri | Semplificare solo casi JAX-RS equivalenti ST06; non sostituire con converter universale | `RequestParamsTest`, `binding`, `binding-boundaries`, `tokens` |
| `Preconditions.*` | `org.springframework.util.Assert` | Servizi e utility; stessi tipi eccezione e messaggi | Mantenere semantica; centralizzare duplicazioni puntuali ST06 | `PreconditionsTest`, `downstream-errors`/`institutions` |
| `OnboardingExceptionMapper.*`, `ProblemResponses.*`, `model/error/Problem` | Advice BFF, handler commons e errori MVC/servlet | Frontend legge detail/errori; integratori dipendono da status e media type | Mantenere dominio distinto da binding/trasporto; semplificare ST06 senza formato product | `OnboardingExceptionMapperTest`, `downstream-errors`, `security`, `http-contract` |
| `AccessDeniedException`, `DownstreamServiceException` | Sostituzioni Spring Security / FeignException | Controller autorizzati e decoder | Mantenere significato e status, non aggiungere fallback; ST06/ST07 | Test exception mapper e parita errori |
| `DownstreamResponseExceptionMapper.toThrowable` | `FeignErrorDecoder` | Tutti i client; passthrough errors[], 400/404/409, 5xx -> gateway | Mantenere decoder condiviso e confine 599 ST06/ST09 | `DownstreamResponseExceptionMapperTest`, `downstream-errors` |
| `IamRestClient`, `IamServiceImpl.hasIamUserPermission` | `MsIamRestClient`, `IamConnectorImpl` | Autorizzazioni token/istituzioni; risposta raw e query vuote omesse | Mantenere assente diverso da spazi; rendere reattivo ST04 | `IamServiceImplTest`, `tokens`, `IntegrationFixtureTest` |
| `DocumentContentRestClient`, `OnboardingUploadRestClient` | Client Feign content/upload; adattamento al codegen Quarkus che perde filename/content type | `DocumentService`, onboarding e token | Mantenere finche il client generato preserva metadati/binari ST05 | `tokens` multipart/download; `DocumentServiceTest`, `OnboardingServiceImplTest` |
| `PartyProcessRestClient`, `PartyRegistryProxyRestClient` | Client omonimi Spring | Facciate enti e registri | Mantenere wire, rendere reattivi ST04 senza accorpare modelli pubblici | `institutions`, `transport-failures` |
| `UserRegistryRestClient`, `RegistryUser` | Client Spring e modello utente certificato registry | `UserRegistryService`, mapper, utenti/istituzioni | Mantenere ordine/codifica `fl`, UUID, campi certificati e boundary PATCH ST04 | `users`, `transport-failures :: user-registry-patch-keeps-the-legacy-transport-error` |
| `BinaryData`, `UploadedFile`, `ContentDispositions.filename` | `Resource`, `MultipartFile`, parser `ContentDisposition` Spring | DocumentService e controller file; filename* ha precedenza | Mantenere bytes e filename, non esporre tipi file temporanei ST05 | `FileDataTest`, `ContentDispositionsTest`, `tokens` |
| `ReplayOnConnectionDrop`, `ReplayOnConnectionDropInterceptor.replay` | Replay implicito HttpURLConnection usato da Feign | 6 client manuali e client generati annotati | Mantenere un replay solo senza body, prima della risposta; semplificare ST09 dopo ST05 | `TransportReplayHttpTest`, `transport-failures` |
| `ConnectionDrops.isDroppedBeforeAnswer`, `RequestBodies.carriesBody` | Discriminazione introdotta per riprodurre il trasporto Spring | Interceptor replay | Mantenere esclusione timeout, body streaming e risposte troncate ST09 | `ConnectionDropsTest`, `RequestBodiesTest`, `ms-product-truncated-body-is-not-replayed`, `bodyless-put-dropped-once-is-replayed-immediately` |
| `LegacyHttpMethodFilter.filter` | Limite HttpURLConnection su user-registry | PATCH creazione/aggiornamento utenti | Mantenere errore prima del wire; abilitare PATCH sarebbe funzionalita separata ST04/ST09 | `LegacyHttpMethodFilterTest`, scenario PATCH con zero scritture |
| `@Retry` nei servizi e timeout REST | Resilience4j retryTimeout e timeout Feign effettivi | Onboarding, document reads, proxy tranne Infocamere, party lookup per tax code | Mantenere 3 tentativi logici/5s, fino a 6 wire calls col replay; 10s connect/60s read ST09 | `RetryPolicyTest`, `RestClientTimeoutDefaultsTest`, `transport-failures` |
| `JacksonConfiguration.customize` | `BaseWebConfig.objectMapper` commons e wiring Quarkus | DTO pubblici e client; NON_NULL, date ISO e unknown fields | Mantenere semantica JSON ST02/ST06; nessun cambio di visibilita/default nel refactoring | Nuovo `JacksonConfigurationTest`, parita flussi e gate OpenAPI |
| `OffsetDateTimeDeserializer.deserialize` | Adattatore della migrazione per modelli downstream OffsetDateTime | Jackson e mapper con `toLocalDateTime` | Mantenere offset o UTC se assente; scelta interna, non nuova policy pubblica. Rimozione richiede dimostrare equivalenza ST02 | Nuovo `JacksonConfigurationTest`; non presentare test interno come prova di ogni formato su Spring |
| `CustomLoggingFilter`, `CustomServerResponseLoggingFilter` | `LogFilter` Spring e lifecycle MDC Quarkus | Log richieste e pulizia X-Client-Ip/sc_operation_id | Mantenere; semplificazione/rimozione ST10 bloccata ST01-D04 | Parita HTTP protegge assenza di effetti pubblici; nessun gate operativo dei log eseguito |
| `CustomClientRequestLoggingFilter`, `CustomClientResponseLoggingFilter`, `MDCUtils` | Logging Feign/commons, integrazione MDC aggiunta nella migrazione | Log downstream e operazione | Mantenere fino a verifica osservabilita ST10; ST01-D04 | Nessuna certificazione telemetria dedotta dai test HTTP |
| `LogUtils.sanitize`, `CONFIDENTIAL_MARKER` | Utility commons sostituita localmente, sanitizzazione gia integrata | Log applicativi/diagnostici | Mantenere marker e sanitizzazione; non loggare token/claim ST10 | `LogUtilsTest`, `AuthorizationServiceTest` |

#### Registro ST01 - Consumer verificati e blocchi

Frontend verificato in sola lettura su
`pagopa/selfcare-onboarding-frontend@51c55ac6f8c18319e896b10f64f3011e0a03f2e1`.
Le evidenze sono codice/configurazione, non smoke di un ambiente deployato.

| Consumer / fonte | Requisito osservato | Classificazione / decisione |
|------------------|--------------------|-----------------------------|
| Frontend `src/api/OnboardingApiClient.ts` | Client generato, bearerAuth, operation ID, status 200/201/204 e redirect 401/403; upload `attachment`/`aggregates`, HEAD via wrapper axios | Contrattuale; mantenere routing, risposte, autenticazione e multipart |
| Frontend `package.json`, `openApi/scripts/api-onboarding_fixPreGen.js`, `api-onboarding_fixPostGen.js` | OpenAPI 3 -> Swagger 2 -> `gen-api-models` con request types/response decoders; patch dipendenti da schema enum, `Problem`, operation ID, HEAD, slash e multipart | Consumer reale confermato; non dedurre che una diversa rappresentazione sia equivalente |
| Frontend `.github/workflows/sync_bff_openapi.yml` | Scarica ancora `apps/onboarding-bff/app/src/main/resources/swagger/api-docs.json` dal source SHA | Alias legacy consumato: mantenere copia generata byte-for-byte |
| `.github/workflows/trigger_onboarding_frontend_openapi_sync.yml` e release/docs | Trigger segue `src/main/docs/openapi.json`, consumer frontend scarica alias | I due path hanno ruoli diversi; mantenerli allineati, non cancellarne uno |
| `infra/resources/onboarding-bff/{dev,uat,prod}-{ar,pnpg}/main.tf` | Tutti i sei ambienti importano l'alias legacy nel modulo APIM | Contrattuale per delivery; mantenere path e sintassi `templatefile` |
| `infra/resources/_modules/apim_api/main.tf` | Import `openapi+json`, CORS in APIM, tenant da host autorizzato e override `X-Tenant-Id` | Policy infrastrutturale distinta da policy BFF; niente rimozione controlli applicativi |
| `infra/resources/_modules/container_app_microservice/variables.tf` | Tre probe HTTP `actuator/health`, porta 8080; BFF usa il default del modulo | Route health consumata, non sostituibile solo con `/q/health` |
| Catalogo e client downstream | JWT/tenant/api-key, query mancanti/vuote/spazi, errori, bytes, numero e ordine chiamate, replay e PATCH | Contrattuale osservato nei test Spring/Quarkus; mantenere anche se non descritto da OpenAPI |
| Host generato, ordinamenti e dettagli springdoc | Il gate attuale richiede uguaglianza strutturale; i tool consumer trasformano la specifica | Vincolo vigente; il bisogno di ogni trasformazione oltre al gate resta non dimostrato |
| MDC, marker e formato log | Requisito interno di osservabilita, non requisito HTTP | Non verificato operativamente: nessuna autorizzazione a rimuovere |

| Decisione bloccata | Motivo e comportamento conservato | Condizione di sblocco / storia |
|-------------------|----------------------------------|-------------------------------|
| ST01-D01 - Rimuovere trasformazioni OpenAPI | La catena frontend e stata letta, non eseguita col documento candidato; uguaglianza vigente non modificata | Eseguire generate/build e decoder test del consumer reale; approvare prima eventuale differenza di rappresentazione, ST08 |
| ST01-D02 - Ridurre policy CORS/HSTS/forwarding | Listener HTTP/HTTPS locali verificati; APIM, proxy trusted e TLS deployati non testati | Smoke autorizzato in ambiente e confine proxy confermato, ST07/ST10/G03 |
| ST01-D03 - Rimuovere route accessorie o whitelist | Health/alias hanno consumer reali; assenza di consumer per `/actuator`, `/error`, Swagger, `/dapr` non dimostrata | Verifica operativa degli integratori e decisione esplicita; fino ad allora preservare anche risposte delle route non mappate, ST07/ST08 |
| ST01-D04 - Rimuovere filtri/MDC | Nessuna verifica di dashboard, correlazione o Application Insights del candidato | Requisito operativo e smoke telemetria autorizzato, ST10/G03 |

Nessuna classe di compatibilita e autorizzata alla rimozione immediata.
Le rimozioni gia motivate riguardano **ponti await e I/O nei controller**,
non i comportamenti di trasporto o le route. ST02 puo iniziare dagli spostamenti
di package a comportamento invariato; questi blocchi non autorizzano a saltare
i gate successivi.

#### Registro ST01 - Riferimento ed evidenze del 2026-10-08

Baseline Spring immutata:
`ad37c6738939e707068f3c8fa0e0dceb16ee3f19`. Il vecchio checkout non era
disponibile: ricostruita una copia con `git archive` nello spazio di sessione,
senza branch/worktree e senza modificare sorgenti o dipendenze dell'oracolo.
Confrontati i blob dei **455 file tracciati** dopo la build: **0 modificati**.
Il FATJAR eseguibile e stato conservato read-only fuori dal repository.

- SHA-256 FATJAR: `ab30d47f9c8fcb50de6b4a52c907f5174f7adbaf104f025dab33d3ab154628c3`.
- SHA-256 di `src/test/resources/parity/spring-api-docs.json`:
  `428feb68e64436ac605a453f1aaea4ce968aa5a57d792f94beeae89ebb0bd137`.
- Artefatto di sessione da assegnare a `SPRING_ORACLE_JAR`:
  `files/bff-spring-ad37c6738-FATJAR.jar`, fuori dal working tree.
- Nuove regressioni interne: `JacksonConfigurationTest` **11 casi** su date
  con/senza offset, precisione, null/blank, input invalido, NON_NULL, ISO e unknown
  fields; `ParityCatalogTest` ora protegge anche i gruppi `binding`,
  `binding-boundaries`, `http-boundaries`. Il catalogo resta **538 scenari**:
  nessuna nuova aspettativa pubblica inventata e nessun floor abbassato.

Comandi eseguiti con Java 17; nei comandi sotto `SPRING_SOURCE` indica la copia
estratta e `SPRING_ORACLE_JAR` il FATJAR read-only.
`pnpm nx show project onboarding-bff --json`
non disponibile (`pnpm: command not found`). Usato il fallback Maven previsto,
senza intervenire sulla configurazione Nx.

```shell
# Solo la copia estratta dello SHA Spring, non il BFF Quarkus corrente.
mvn -B -ntp -f "$SPRING_SOURCE/apps/onboarding-bff/pom.xml" \
  -pl app -am package -DskipTests

# Caratterizzazione, tutti i 538 scenari Quarkus e gate contrattuali.
mvn -B -ntp -f apps/onboarding-bff/pom.xml test \
  -Dtest=JacksonConfigurationTest,ParityCatalogTest,QuarkusParityTest,SpringReferenceParityTest,QuarkusOpenApiInventoryTest,PublishedOpenApiGateTest,OpenApiIdenticalDocumentGateTest,LegacyOpenApiAliasGateTest,RequestParamsTest,PreconditionsTest,ClientRequestValidatorTest,DownstreamResponseExceptionMapperTest,OnboardingExceptionMapperTest,SecurityGateRulesTest,BffJwtCallerPrincipalFactoryTest,AuthenticationPropagationHeadersFactoryTest,DownstreamApiKeyFilterTest,AuthorizationServiceTest,SecurityIdentityUtilsTest,RestClientTimeoutDefaultsTest,ContentDispositionsTest,ConnectionDropsTest,LegacyHttpMethodFilterTest,RequestBodiesTest,RetryPolicyTest,TransportReplayHttpTest

# Oracolo recuperato: stessa parita e contratto HTTPS su entrambi i runtime.
mvn -B -ntp -f apps/onboarding-bff/pom.xml test \
  -Dtest=SpringReferenceParityTest,HttpsParityTest \
  -Dparity.spring.jar="$SPRING_ORACLE_JAR"

# Ricontrollo finale dei nuovi test e dei tre gruppi aggiunti al guard.
mvn -B -ntp -f apps/onboarding-bff/pom.xml test \
  -Dtest=JacksonConfigurationTest,ParityCatalogTest
```

| Esecuzione | Esito |
|------------|-------|
| Build copia Spring | SUCCESS; input tracciati immutati |
| Prima selezione ST01 | SUCCESS: 792 test riportati, 0 failure/errori; unico skipped Spring senza jar, recuperato nell'esecuzione successiva |
| Catalogo Quarkus | `PARITY quarkus scenarios=538 attempted=538 passed=538 failed=0` |
| Oracolo Spring + HTTPS | SUCCESS: 540 test, 0 failure/errori/skipped; include HTTPS Spring e Quarkus |
| Catalogo Spring | `PARITY spring-reference scenarios=538 attempted=538 passed=538 failed=0` |
| Gate OpenAPI generato/servito/pubblicato e alias | Passati nella prima selezione; nessun documento/golden/comparatore modificato |
| Ricontrollo finale caratterizzazione/catalogo | SUCCESS: 17 test, 0 failure/errori/skipped |

Il tentativo intermedio con il jar **plain** `app/target` e fallito per manifest
non eseguibile, prima di tentare scenari Spring. Non e una prova di parita:
e stato corretto usando il FATJAR in `onboarding-bff/target`, come documentato
nel README. Nessuna aspettativa e stata rilassata.

Cucumber/Docker, codegen/build frontend, import APIM e smoke deployato,
coverage/Sonar e builder/publish dell'immagine **non eseguiti per ST01**.
Restano gate delle storie successive: le evidenze qui non chiudono ST10/G01-G03.

### BFF-ST02 - Layout, DTO e mapper

**Storia:** come manutentore voglio package con responsabilita riconoscibili, per
leggere il BFF con le stesse convenzioni degli altri servizi Quarkus.

**Dove:** `controller/request`, `controller/response`, `service`, `service/impl`,
`mapper`, relativi test e riferimenti di configurazione.

- [x] **BFF-ST02-T01 - Spostare i DTO pubblici.** Portare richieste e risposte in
  `model/dto/request` e `model/dto/response`, aggiornando tutti gli import, i test e
  gli eventuali riferimenti per nome. Non cambiare contemporaneamente campi o nomi pubblici.
- [x] **BFF-ST02-T02 - Uniformare servizi e injection.** Collocare le implementazioni
  concrete in `service/impl` e mantenere i contratti in `service` dove utili.
  Riutilizzare le convenzioni CDI dei riferimenti, senza introdurre interfacce o wrapper vuoti.
- [x] **BFF-ST02-T03 - Ricollocare le conversioni pure.** Usare i mapper MapStruct
  esistenti per conversioni oggi manuali, incluso `PartyService`. Lasciare nei
  servizi le decisioni applicative, preservando null, default e ordine delle collezioni.
- [x] **BFF-ST02-T04 - Verificare tutte le dipendenze.** Compilare produzione e test,
  controllare che i servizi non importino package dei controller e confrontare
  JSON, schemi OpenAPI e percorsi degli errori di validazione.

**Accettazione:** DTO fuori dai controller; nessuna dipendenza service -> controller;
layout e injection coerenti; nessuna variazione di serializzazione, validazione o
identita degli schemi introdotta dal solo spostamento.

#### Registro ST02 - Modifiche ed evidenze del 2026-10-08

Lotto successivo a ST01, con baseline
`2f821d6cd2f586bca3c7f0d178d0818cb844f88d`, identificato dal subject
`Align BFF DTO and service responsibilities`. Nessun nuovo branch/worktree.
L'inventario ST01 resta una fotografia storica dei package precedenti.

Spostati **25 DTO request e 27 DTO response**, mantenendo nomi semplici, campi,
annotazioni Jackson/OpenAPI e vincoli. Aggiornati import e riferimenti riflessivi
nel solo BFF; i modelli omonimi di onboarding-ms non sono consumer da modificare.
Spostati in `service/impl` i cinque bean concreti `DocumentService`, `PartyService`,
`PartyRegistryProxyService`, `UserRegistryService`, `ClientRequestValidator`.
Restano **7 contratti utili in `service` e 12 classi in `service/impl`**:
nessuna nuova interfaccia, wrapper o classe di produzione.

Conversioni ricollocate nei mapper gia esistenti:

| Mapper | Conversioni estratte |
|--------|---------------------|
| `InstitutionMapper` | Request party-process, proiezione selettiva di `InstitutionUpdate`, utenti e contratto; request IPA e lista ID; assembly `InstitutionOnboardingData` e proiezione della location |
| `DocumentMapper` | Request per upload attachment e user attachment |
| `RegistryProxyMapper` | Wrapper/filter del lookup per codice fiscale del legale |
| `UserMapper` | Wrapper `UserId` e filtri `UserInstitutionRequest`, con split e sentinella vuota originali |

Riutilizzati metodi `default` nei mapper MapStruct per conservare esattamente
omissioni, null, riferimenti condivisi e ordine delle liste. La request party
non diventa una copia integrale dell'istituzione: restano omessi ID, GPU, forma
giuridica, origin/originId e gli altri campi prima non inoltrati.
Precondizioni, policy, lookup sequenziali, arricchimenti geografici, retry,
gestione delle risposte binarie/multipart e await restano nei servizi.
La propagazione di `Uni` appartiene alle storie successive.

**Compatibilita dei dettagli di errore.** Lo spostamento esponeva il nuovo
FQCN in conversioni enum e messaggi Jackson. `RequestParams` conserva il nome
storico di `DownloadDocumentType`; `OnboardingExceptionMapper` conserva i
package storici soltanto nei descrittori di tipo Jackson, anche per collezioni
generiche ed eccezioni wrappate. Non riscrive i valori rifiutati inviati dal
client. Le stringhe `controller.request`/`controller.response` rimaste in questi
due punti e nelle aspettative dei test sono parte del dettaglio pubblico,
non dipendenze da classi obsolete o import dei controller.

Le tre nuove regressioni Jackson sono inizialmente fallite dopo lo spostamento,
misurando la differenza di FQCN; passano dopo l'adattamento mirato.
Le regressioni HTTP verificano errore JSON, percorso
`userTaxCodeDto.taxCode`, reason del Validator CDI effettivo e assenza di chiamate
downstream. Il reason non assume una lingua diversa da quella del runtime.
Aggiornati package dei test, riferimenti riflessivi, injection e guard del retry,
senza nuovi framework di test.

Comandi eseguiti con `JAVA_HOME=$(/usr/libexec/java_home -v 17)`, usando il
fallback Maven gia motivato in ST01. `SPRING_ORACLE_JAR` indica lo stesso FATJAR
Spring read-only, con SHA-256 invariato.

```shell
# Compilazione pulita e regressioni dei componenti riallineati.
mvn -f apps/onboarding-bff/pom.xml clean test \
  '-Dtest=*MapperTest,*ServiceImplTest,*ControllerTest,*DtoTest,*ResourceTest,*ResourceICTest,DocumentServiceTest,PartyRegistryProxyServiceTest,ClientRequestValidatorTest,PartyServiceTest,UserRegistryServiceTest,RequestParamsTest,RetryPolicyTest,JacksonConfigurationTest'

# Errori JSON/enum, serializzazione e validazione sul runtime HTTP.
mvn -f apps/onboarding-bff/pom.xml test \
  -Dtest=OnboardingExceptionMapperTest,RuntimeWiringTest,RequestParamsTest,JacksonConfigurationTest

# Gate finali su entrambi i runtime e confezionamento degli artefatti.
mvn -f apps/onboarding-bff/pom.xml package \
  -Dtest=QuarkusParityTest,SpringReferenceParityTest,HttpsParityTest,ParityCatalogTest,QuarkusOpenApiInventoryTest,OpenApiInventoryTest,OpenApiExactDiffTest,PublishedOpenApiGateTest,OpenApiIdenticalDocumentGateTest,LegacyOpenApiAliasGateTest,RuntimeWiringTest,RuntimeProbeIsolationTest,RuntimeSecurityHttpTest,RuntimeAuthorizationHttpTest,RuntimeIamErrorsHttpTest,TransportReplayHttpTest,OnboardingExceptionMapperTest,JacksonConfigurationTest \
  -Dparity.spring.jar="$SPRING_ORACLE_JAR"

# Nessun documento pubblicato o golden cambiato dopo package.
git diff --exit-code HEAD -- apps/onboarding-bff/src/main/docs \
  apps/onboarding-bff/app/src/main/resources/swagger/api-docs.json \
  apps/onboarding-bff/src/test/resources/parity
```

| Esecuzione | Esito |
|------------|-------|
| Compilazione pulita e test mirati | SUCCESS: 382 test, 0 failure/errori/skipped |
| Errori e validazione HTTP dopo il fix | SUCCESS: 72 test, 0 failure/errori/skipped |
| Gate finali e package | SUCCESS: 1436 test, 0 failure/errori/skipped |
| Catalogo Quarkus | `PARITY quarkus scenarios=538 attempted=538 passed=538 failed=0` |
| Catalogo Spring | `PARITY spring-reference scenarios=538 attempted=538 passed=538 failed=0` |
| Gate OpenAPI, alias, HTTPS, sicurezza e replay | Passati nella selezione finale |
| Audit dei sorgenti DTO | Tutti i 52 file identici alla baseline dopo la sola sostituzione dei package |
| Audit layout e dipendenze | 225 file Java di produzione; nessuna dipendenza service -> controller; nessun vecchio riferimento Java attivo |
| Documenti pubblicati, golden e oracolo | Invariati; confrontati nuovamente dopo package |

Il package finale rigenera i documenti della sola produzione e l'alias legacy:
le route di probe dei profili di test non entrano negli artefatti pubblicati.
Log conservati nello spazio di sessione: `bff-st02-targeted-tests.log`,
`bff-st02-jackson-type-regression.log`, `bff-st02-error-compatibility-tests.log`,
`bff-st02-final-gates.log`.

Cucumber/Docker, codegen/build frontend, import APIM e smoke deployato,
coverage/Sonar e builder/publish dell'immagine **non eseguiti per ST02**.
I blocchi D01-D04 e i gate finali ST10/G01-G03 non sono chiusi da questo lotto.

### BFF-ST03 - Prodotti come primo flusso Quarkus nativo

**Storia:** come manutentore voglio un flusso prodotti interamente reattivo, da
usare come modello concreto per gli altri flussi senza creare un nuovo framework.

**Dove:** `ProductController`, `ProductV2Controller`, `ProductService`,
`service/impl/ProductServiceImpl`, mapper, client e tutti i chiamanti del servizio.

- [x] **BFF-ST03-T01 - Ricostruire il percorso completo.** Elencare endpoint,
  chiamanti, mapping, failure e retry dei prodotti, associandoli ai casi esistenti.
  Includere i chiamanti interni prima di cambiare le firme.
- [ ] **BFF-ST03-T02 - Propagare Uni end-to-end.** Restituire e comporre `Uni<T>`
  gia prodotti dai client attraverso servizio e controller. Rimuovere gli await
  ordinari; aggiornare tutti i consumer nello stesso lotto compilabile.
- [ ] **BFF-ST03-T03 - Preservare semantica e contesto.** Mantenere filtri,
  ordinamenti, tenant, gestione errori, retry e numero delle chiamate. Non rendere
  parallele chiamate sequenziali senza averne provato l'equivalenza.
- [ ] **BFF-ST03-T04 - Verificare il modello prima di estenderlo.** Adeguare i test
  al risultato asincrono e coprire successo, vuoto e failure. Eseguire
  `ProductServiceImplTest`, `ProductControllerTest` e i casi di parita prodotti
  di entrambe le versioni API; confrontare il risultato con i servizi di riferimento.

**Accettazione:** client -> servizio -> controller usa `Uni` senza attese
bloccanti sul percorso ordinario; i controller non orchestrano il dominio;
i contratti v1/v2 e gli effetti downstream non cambiano.

#### Registro ST03 - Traccia e primo lotto del 2026-10-09

**T01 completato. T02-T04 in corso, con primo lotto verificato; storia aperta.**
Baseline: merge `08522fc5d`, che incorpora main `cd0a2751f` e mantiene ST01/ST02.
Il lotto e identificato dal subject `Start reactive BFF product flows`.
Nessuna modifica ai servizi di riferimento, nuovo framework o bridge sincrono.

| Endpoint | Percorso e stato nel primo lotto |
|----------|--------------------------------|
| `GET /v1/products` | `ProductApi.getProducts -> ProductService.getProducts -> ProductController.getProducts`: `Uni` end-to-end |
| `GET /v1/products/admin` | Stessa catena con `rootOnly=true`, filtro del contratto DEFAULT nel controller: `Uni` end-to-end |
| `GET /v2/product` | `ProductApi.getProductOriginsById -> ProductService.getOrigins -> ProductV2Controller.getOrigins`: `Uni` end-to-end |
| `GET /v2/product/{productId}/required-documents/enabled` | HEAD downstream -> flag letto e Response chiusa -> DTO HTTP: `Uni` end-to-end |
| `GET /v1/product/{id}` | Ancora sincrono: `getProduct` e condiviso con i flussi istituzioni; prossimo lotto |
| `GET /v2/product/{productId}/required-documents` | Ancora sincrono: `getRequiredDocuments` e condiviso con upload attachment; prossimo lotto |

**Chiamanti interni ricostruiti (otto invocazioni):**

| Chiamante | Invocazione e ordine da preservare nel prossimo lotto |
|-----------|-----------------------------------------------------|
| `InstitutionServiceImpl.onboardingProduct` | `getProduct` dopo le precondizioni; delegabilita, phase-out, contratto e ruoli prima di party/registry e scritture |
| `checkIfProductIsActiveAndSetUserProductRole` | `getProduct` del padre soltanto per i figli; validazione enabled/tax-code, verifica onboarding del padre e ruolo prima delle scritture |
| `InstitutionServiceImpl.getInstitutions` | `getProduct` prima della ricerca party; stesso messaggio 404 del controller v1 |
| `validateOnboardingByProductOrInstitutionTaxCode` | `isProductEnabled` poi `verifyAllowedByInstitutionTaxCode`, sempre due chiamate sequenziali anche se la prima e true |
| `TokenServiceImpl.getTemplateAttachment` | Onboarding -> `getProductValid` -> selezione template -> documento |
| `TokenServiceImpl.uploadAttachment/findRequiredDocument` | Onboarding -> `getRequiredDocuments`; `getProductValid` solo nel ramo SYSTEM, poi upload; ramo USER senza lookup valid-product |

La chiamata multilinea a `getRequiredDocuments` e parte del perimetro, non un
consumer assente. Lasciare quelle firme sincrone in questo primo lotto evita di
spostare gli await nei chiamanti o dichiarare reattive orchestrazioni ancora bloccanti.
**Prossimo lotto T02:** migrare insieme le due route rimanenti e le orchestrazioni
interne che consumano i lookup/documenti, con composizione sequenziale e boundary
di I/O reale esplicite. La dipendenza ST04 -> ST03 non e ancora soddisfatta.

Nel lotto verificato, i tre metodi del servizio ritornano direttamente/componendo
il `Uni` dei client generati: nessun await, `runSubscriptionOn`, wrapper o retry
nuovo. Conservati encoding, valid/rootOnly, filtro ACTIVE (inclusi i disabled),
ordine delle liste, filtro contratto admin, errori/null del downstream e flag
false per header HEAD mancante. La Response HEAD viene chiusa anche quando la
lettura dell'header fallisce. I controller fanno solo binding/mapping e risposta.

I test adeguati usano await solo nel test oppure `UniAssertSubscriber`. Due test
con emitter controllato e timeout di 2 secondi verificano il ritorno senza attendere
il downstream e il mapping solo dopo l'item. Test di failure verificano la stessa
eccezione, senza mapping, retry o fallback false. Gli emitter Mockito richiedono
un tipo esplicito (`<ProductOriginResponse>` / `<List<Product>>`): corretto il
fallimento iniziale di inferenza senza cast.

| Esecuzione | Esito |
|------------|-------|
| Test mirati prodotti, controller v1/v2, retry e parita gruppo products | SUCCESS: 106 test, zero failure/errori/skipped; 30 scenari per runtime |
| Gate finali e package | SUCCESS: 1347 test, zero failure/errori/skipped |
| Catalogo completo Quarkus | `PARITY quarkus scenarios=547 attempted=547 passed=547 failed=0` |
| Catalogo completo Spring main | `PARITY spring-reference scenarios=547 attempted=547 passed=547 failed=0` |
| OpenAPI esatto, alias, HTTPS, sicurezza, autorizzazione, IAM e replay | Passati nella selezione finale |
| Fixture/catalogo e Cucumber mirato | 11 test Surefire e 10 scenari Cucumber passati; 17 test Failsafe inclusi i lifecycle, zero skip |
| Documenti pubblicati, golden e oracoli | Invariati rispetto al merge dopo package/verify; floor del catalogo alzato a 547 solo dopo la doppia verifica |
| Cleanup Docker | Container/reti del test assenti; immagine residua e 106.2 MB di cache del task rimossi; immagini preesistenti preservate; dati Azurite temporanei rimossi |

Comandi del lotto, Java 17 e fallback Maven gia documentato:

```shell
mvn -B -ntp -f apps/onboarding-bff/pom.xml package \
  -Dtest=ProductServiceImplTest,ProductControllerTest,ProductV2ControllerTest,RetryPolicyTest,QuarkusParityTest,SpringReferenceParityTest \
  '-Dparity.only=^products ::' -Dparity.spring.jar="$SPRING_ORACLE_JAR"

mvn -B -ntp -f apps/onboarding-bff/pom.xml package \
  -Dtest=ProductServiceImplTest,ProductControllerTest,ProductV2ControllerTest,RetryPolicyTest,TokenV2ControllerTest,AuthorizationServiceTest,QuarkusParityTest,SpringReferenceParityTest,ParityCatalogTest,HttpsParityTest,QuarkusOpenApiInventoryTest,PublishedOpenApiGateTest,OpenApiIdenticalDocumentGateTest,LegacyOpenApiAliasGateTest,RuntimeWiringTest,RuntimeSecurityHttpTest,RuntimeAuthorizationHttpTest,RuntimeIamErrorsHttpTest,TransportReplayHttpTest \
  -Dparity.spring.jar="$SPRING_ORACLE_JAR"

mvn -B -ntp -f apps/onboarding-bff/pom.xml verify -Pintegration-tests \
  -Dtest=IntegrationFixtureTest,ParityCatalogTest \
  '-Dcucumber.filter.name=(?i).*contract.*|.*getProducts.*|GET /v2/product.*'
```

Log: `bff-st03-targeted.log`, `bff-st03-final-gates.log`,
`bff-merge-st03-cucumber.log` nello spazio di sessione. Il FATJAR corrente resta
quello immutabile del main con SHA-256 registrato sopra.
La suite completa Cucumber, reactor misto, frontend/APIM, coverage/Sonar,
builder/publish e rollout non sono certificati da questo lotto.

### BFF-ST04 - Istituzioni, utenti, IAM e registri

**Storia:** come manutentore voglio applicare il modello verificato ai flussi
istituzioni e utenti, mantenendo le autorizzazioni e gli effetti delle integrazioni.

**Dove:** `InstitutionController`, `InstitutionV2Controller`, `UserController`,
servizi istituzioni/utenti/IAM, `PartyService`, `PartyRegistryProxyService`,
`UserRegistryService` e relativi client.

- [ ] **BFF-ST04-T01 - Migrare istituzioni e aggregazioni.** Applicare la composizione
  `Uni` alle catene di `InstitutionService` e `PartyService`; mantenere mapping nei
  mapper e aggiornare tutti i controller e i consumer delle firme modificate.
- [ ] **BFF-ST04-T02 - Migrare utenti e autorizzazioni.** Adeguare `UserService`,
  `UserInstitutionService`, `IamService` e i loro chiamanti. Verificare la
  propagazione del contesto tenant anche quando l'esecuzione cambia thread.
- [ ] **BFF-ST04-T03 - Preservare i confini dei registri.** Eliminare ponti bloccanti
  sui client reattivi e documentare l'eventuale I/O realmente bloccante. Conservare
  l'omissione di `institutionId` vuoto sul wire, senza normalizzare gli spazi;
  conservare il rifiuto del PATCH legacy prima della scrittura downstream.
- [ ] **BFF-ST04-T04 - Verificare aggregazioni e failure.** Eseguire i test dei
  servizi/controller interessati e i casi di parita per autorizzazioni, tenant,
  liste vuote, errori parziali, ordine e conteggio delle richieste downstream.

**Accettazione:** nessun await ordinario sui client gia reattivi; nessuna nuova
scrittura o richiesta downstream; differenze tra valore assente, vuoto e spazi
preservate dove previste dal contratto.

### BFF-ST05 - Onboarding, token, documenti e I/O

**Storia:** come manutentore voglio controller sottili anche nei flussi multipart
e documentali, con orchestrazione e I/O nei componenti appropriati.

**Dove:** `TokenV2Controller`, `TokenServiceImpl`, `OnboardingServiceImpl`,
`DocumentService`, modelli multipart, mapper e client documentali.

- [ ] **BFF-ST05-T01 - Spostare l'orchestrazione.** Portare selezione dei documenti,
  sequenza delle operazioni e decisioni applicative dal controller ai servizi
  esistenti. Lasciare nel controller binding e costruzione della risposta HTTP.
- [ ] **BFF-ST05-T02 - Comporre i flussi asincroni.** Propagare `Uni` attraverso
  onboarding, token e documenti, incluse le catene di errore e autorizzazione.
  Mantenere l'ordine delle operazioni con effetti collaterali.
- [ ] **BFF-ST05-T03 - Isolare il vero I/O bloccante.** Togliere `Files.readAllBytes`
  e operazioni analoghe dai controller. Stabilire una boundary worker Quarkus
  esplicita per file/SDK sincroni, gestendo durata dei file temporanei, risorse e
  contesto tenant; non usare un semplice wrapper `Uni` sul thread event-loop.
- [ ] **BFF-ST05-T04 - Verificare file e autorizzazioni.** Coprire multipart,
  filename, content type, byte scaricati, header, documenti mancanti, permessi,
  errori downstream e chiusura delle risorse, con test mirati e parita HTTP.

**Accettazione:** nessuna lettura di file o orchestrazione business nei controller;
nessun blocco dell'event-loop; upload/download e failure mantengono il contratto
e non lasciano risorse o file temporanei aperti.

### BFF-ST06 - Binding e gestione errori

**Storia:** come integratore voglio gli stessi errori pubblici, senza che il BFF
debba ricostruire genericamente il comportamento di Spring.

**Dove:** `util/RequestParams`, `util/Preconditions`, vincoli sui DTO,
`exception/handler/OnboardingExceptionMapper`, modelli degli errori e controller.

- [ ] **BFF-ST06-T01 - Classificare conversioni e validazioni.** Per ciascun uso
  di `RequestParams` verificare parametri ripetuti, vuoti, enum, numeri e alias
  booleani. Confrontare binding JAX-RS e Bean Validation con i casi osservati.
- [ ] **BFF-ST06-T02 - Usare il framework dove equivalente.** Sostituire soltanto
  i casi realmente equivalenti; mantenere adattamenti puntuali per quelli richiesti.
  Non aggiungere un nuovo convertitore universale o fallback silenziosi.
- [ ] **BFF-ST06-T03 - Centralizzare le eccezioni.** Usare il pattern
  `@ServerExceptionMapper` dei riferimenti, distinguendo dominio, downstream e
  parsing/validazione. Non copiare il formato errori di product se diverso dal
  contratto BFF e non alterare status, body o content type.
- [ ] **BFF-ST06-T04 - Verificare richieste invalide.** Rieseguire regressioni
  binding/errori, inclusi `PreconditionsTest`, input null/vuoti/spazi, route
  inesistenti e media type errati. Tracciare ogni adattamento rimasto.

**Accettazione:** nessuna validazione o traduzione duplicata tra controller e
servizi; gli errori sono espliciti e coerenti; ogni conversione non nativa e
giustificata da requisito e test. Nessuna soppressione Sonar per mascherare problemi.

### BFF-ST07 - Sicurezza, tenant e policy HTTP

**Storia:** come integratore voglio gli stessi controlli di accesso, implementati
con policy applicative limitate e meccanismi Quarkus dove compatibili.

**Dove:** `BffSecurityFilter`, `BffJwtCallerPrincipalFactory`,
`AuthorizationService`, configurazione sicurezza e test HTTP/HTTPS.

- [ ] **BFF-ST07-T01 - Separare requisiti di accesso e trasporto.** Mappare
  autenticazione, tenant, autorizzazioni, URL, metodi, CORS e header, identificando
  per ciascuno l'alternativa Quarkus e il comportamento da conservare.
- [ ] **BFF-ST07-T02 - Verificare il riuso degli SDK.** Confrontare claim
  facoltativi/obbligatori, issuer, firma e risoluzione chiavi. Non attivare
  automaticamente i bean SDK: anche document-ms ne esclude alcuni.
  Conservare la factory BFF se il contratto ne dimostra ancora la necessita.
- [ ] **BFF-ST07-T03 - Ridurre il filtro.** Delegare a configurazione e componenti
  standard solo cio che conserva gli accessi e gli header richiesti. Mantenere
  policy applicative esplicite, senza distribuire la stessa emulazione in tanti helper.
- [ ] **BFF-ST07-T04 - Verificare il confine di sicurezza.** Eseguire i casi
  JWT/tenant/autorizzazioni, 401/403, preflight, URL anomali, route pubbliche,
  header cache/Vary e HSTS HTTP/HTTPS, anche per risposte di errore.

**Accettazione:** stessi token e richieste accettati/rifiutati; firma e tenant
sempre verificati dove richiesti; nessun ampliamento degli accessi; ogni policy
custom residua e motivata. Le verifiche APIM/proxy deployati restano un gate di ambiente.

### BFF-ST08 - OpenAPI e route di compatibilita

**Storia:** come consumer voglio specifiche e route utilizzabili senza mantenere
una riscrittura generale di springdoc dentro la configurazione Quarkus.

**Dove:** `OpenApiContractFilter`, `SpringCompatibleRoutes`, annotazioni dei
controller/DTO, `application.properties`, `src/main/docs` e alias legacy.

- [ ] **BFF-ST08-T01 - Scomporre i requisiti OpenAPI.** Distinguere security,
  risposte, schemi e operation ID da ordine, server, slash e inlining. Associare
  ciascuna trasformazione ai consumer e ai gate esistenti.
- [ ] **BFF-ST08-T02 - Spostare le dichiarazioni nella sede corretta.** Usare
  annotazioni OpenAPI, schemi di sicurezza e configurazione SmallRye dove
  equivalenti. Lasciare solo trasformazioni minime con un requisito dimostrato.
- [ ] **BFF-ST08-T03 - Riesaminare le route legacy.** Verificare health/actuator,
  `/error`, Swagger e `/v3/api-docs` rispetto a probe e consumer. Coordinare errori
  e accessi con ST06/ST07; non rimuovere alias ancora consumati.
- [ ] **BFF-ST08-T04 - Rigenerare e verificare tutti gli artefatti.** Usare la
  build, non modificare a mano OpenAPI o alias. Eseguire i gate strutturali e
  di uguaglianza esistenti, verificare `templatefile` APIM e il codegen consumer
  reale. Se il consumer non e disponibile, registrare il gate come bloccato.

**Accettazione:** specifiche generate, servite e alias legacy coerenti; nessuna
perdita di compatibilita per codegen/APIM/probe. L'uguaglianza strutturale rimane
richiesta finche una diversa rappresentazione non e esplicitamente approvata:
non indebolire il comparatore per eliminare il filtro.

### BFF-ST09 - Trasporto e retry

**Storia:** come manutentore voglio una politica di trasporto comprensibile,
senza retry sovrapposti o replay che possano duplicare le operazioni.

**Dove:** `client/transport`, `ReplayOnConnectionDropInterceptor`, configurazione
REST client, annotazioni di retry dei servizi e test di trasporto.

- [ ] **BFF-ST09-T01 - Inventariare i livelli di ripetizione.** Distinguere retry
  applicativi, replay impliciti JDK/Feign e comportamento dei client Quarkus,
  annotando timeout, metodi HTTP, trigger e numero atteso delle richieste.
- [ ] **BFF-ST09-T02 - Consolidare senza cambiare la politica.** Eliminare
  duplicazioni solo dopo aver provato l'equivalenza sul flusso reattivo.
  Non confondere una failure asincrona con un'eccezione prima della sottoscrizione.
- [ ] **BFF-ST09-T03 - Verificare gli effetti sul wire.** Coprire disconnessione,
  timeout, errori HTTP, numero/ordine dei tentativi, assenza di scritture aggiuntive
  e restrizione PATCH legacy, usando stub e regressioni gia disponibili.
- [ ] **BFF-ST09-T04 - Rimuovere configurazione morta.** Eliminare interceptor,
  proprieta e helper non piu usati; aggiornare il README per gli override effettivi,
  mantenendo eventuali adattatori ancora richiesti.

**Accettazione:** nessun retry duplicato, richiesta o scrittura aggiuntiva;
timeout e failure pubbliche invariati; ogni adattatore di trasporto rimasto
ha una motivazione e un caso di regressione.

### BFF-ST10 - Pulizia e certificazione finale

**Storia:** come reviewer voglio evidenze riferite al codice riallineato, per
valutare la migrazione senza affidarmi ai risultati della vecchia baseline.

**Dove:** intero BFF, test, documentazione e gate CI esistenti.

- [ ] **BFF-ST10-T01 - Completare la pulizia strutturale.** Cercare dipendenze
  service -> controller, await di produzione, I/O nei controller, codice morto
  e responsabilita duplicate. Esaminare ogni occorrenza, senza aggiungere un
  framework di architettura solo per automatizzare lo stile.
- [ ] **BFF-ST10-T02 - Certificare il contratto completo.** Rieseguire l'intero
  catalogo Spring/Quarkus, Cucumber, OpenAPI/alias/codegen e HTTP/HTTPS. Attendere
  almeno 547 casi per runtime e 59 scenari Cucumber, tutti tentati e passati;
  aggiunte lecite aumentano i conteggi, riduzioni richiedono una decisione esplicita.
- [ ] **BFF-ST10-T03 - Verificare integrazione e coverage.** Rieseguire il reactor
  seriale con le altre app Quarkus coinvolte, la coverage e i gate CI. Verificare
  la soglia vigente di nuova coverage (80% nella baseline), senza nuove esclusioni
  o riduzione dello scope di analisi. Riesaminare i finding Sonar sul nuovo SHA.
- [ ] **BFF-ST10-T04 - Rinnovare evidenze runtime e documentazione.** Rieseguire
  gli smoke runtime pertinenti, distinguendo test senza agente e con Application
  Insights. Aggiornare README e questo piano con comandi, risultati, SHA e limiti.
  Non dichiarare certificato il builder Docker completo usando solo un artefatto
  Maven iniettato come contesto `builder`.

**Accettazione:** checklist architetturale soddisfatta, regressioni complete verdi
e riferite allo stesso candidato, nessun gate saltato presentato come passato.
I blocchi amministrativi e il rilascio rimangono espliciti nei gate seguenti.

## Gate operativi gia aperti

Questi gate non sono nuove funzionalita o autorizzazioni a intervenire su CI,
impostazioni del repository o ambienti. Lo stato riportato e quello della baseline:
va riconfermato sul candidato finale.

| Gate | Stato di partenza | Condizione per chiuderlo |
|------|-------------------|-------------------------|
| BFF-G01 - Qualita e dependency submission | Sonar reliability D su otto finding; submission autenticata riuscita, submission automatica gestita ancora rossa | Nuova analisi accettata e cutover amministrativo autorizzato, senza aggirare i controlli |
| BFF-G02 - Review e merge | PR #220 aperta; nessuna approvazione umana registrata | ST10 e G01 chiusi, approvazioni richieste ottenute e merge esplicitamente autorizzato |
| BFF-G03 - Delivery e rollout | Runtime provato sulla baseline; builder/publish completi e ambiente non certificati | Pipeline immagine completa verificata, rilascio autorizzato e smoke in ambiente con rollback disponibile |

- [ ] **BFF-G01-T01 - Riconciliare la quality gate.** Dopo ST10 confrontare i finding
  del nuovo SHA; correggere difetti reali e richiedere triage autorizzato per quelli
  residui. Non sopprimere controlli o dichiarare risolti i precedenti otto finding
  usando soltanto i test di intenzionalita.
- [ ] **BFF-G01-T02 - Concordare il cutover dependency submission.** L'amministratore
  verifica lo snapshot autenticato gia disponibile e decide la disattivazione
  dell'automatismo gestito non autenticato. Questa attivita puo procedere
  indipendentemente dal refactoring; non disattivare nulla senza autorizzazione.
- [ ] **BFF-G02-T01 - Aggiornare la review sul risultato effettivo.** Fornire
  evidenze e diff dei lotti completati nella PR esistente, mantenendo il template.
  Non duplicare richieste di review o notifiche ai reviewer gia contattati.
- [ ] **BFF-G02-T02 - Effettuare il merge soltanto dopo i gate.** Riconfermare
  check, approvazioni e autorizzazione; niente self-approval, bypass o force push.
- [ ] **BFF-G03-T01 - Verificare builder e immagine completa.** Eseguire il percorso
  reale Maven dentro Docker e la pubblicazione nella pipeline autorizzata, senza
  sostituzione del builder. Registrare immagine/digest e risultato: lo smoke del
  runtime da solo non chiude questo task.
- [ ] **BFF-G03-T02 - Eseguire il rollout autorizzato.** Concordare ambiente e
  rollback, poi verificare probe, APIM, proxy/TLS, tenant, downstream e flussi
  onboarding nel deployment. Nessun deploy di produzione implicito nel piano.

G03 dipende dal candidato approvato in G02 per il rollout; la verifica tecnica
del builder puo essere anticipata in un contesto autorizzato.

## Modalita di lavoro e verifiche

Per ogni task annotare sotto la sua voce **stato, commit, comando/esito e decisioni**.
Stati ammessi: `da fare`, `in corso`, `bloccato` con motivo, `completato` con evidenza.
Se un requisito esterno non e verificabile, non trasformarlo in un'assunzione.
Ogni lotto deve compilare e includere tutti i chiamanti, i test e gli artefatti
generati pertinenti, senza commit intermedi deliberatamente rotti.

Preferire i target Nx gia configurati quando disponibili. Nella baseline la
risoluzione del workspace Nx e bloccata; i comandi Maven seguenti sono il fallback
gia documentato nel [README](README.md#tests), non un'attivita di riparazione Nx.
Richiedono Java 17 e le credenziali Maven previste dal repository.

Per i gate Spring impostare `SPRING_ORACLE_JAR` al FATJAR immutabile del main
`cd0a2751f`, con lo SHA-256 registrato nell'integrazione del 2026-10-09,
senza ricostruirlo dal BFF Quarkus modificato. Le evidenze ST01/ST02 conservano
il proprio oracolo storico `ad37c6738939e707068f3c8fa0e0dceb16ee3f19`.
I comandi seguenti sono **da eseguire durante l'implementazione**, non risultati
gia ottenuti con questa modifica documentale.

```shell
# Primo lotto prodotti; selezionare i test pertinenti per i lotti successivi.
mvn -f apps/onboarding-bff/pom.xml test \
  -Dtest=ProductServiceImplTest,ProductControllerTest

# Catalogo completo, integrazione Cucumber e gate contrattuali; richiede Docker.
: "${SPRING_ORACLE_JAR:?Impostare il percorso del FATJAR Spring immutabile}"
mvn -f apps/onboarding-bff/pom.xml clean verify -Pintegration-tests \
  -Dparity.spring.jar="$SPRING_ORACLE_JAR"

# Compatibilita con il reactor misto; non utilizzare -T 1C.
mvn -T 1 -B -ntp \
  --projects :onboarding-bff,:iam,:institution-send-mail-scheduler \
  --also-make clean test \
  -Dquarkus.http.test-port=0 -Dquarkus.management.test-port=0 \
  -Dparity.spring.jar="$SPRING_ORACLE_JAR"

# Coverage aggregata, separata dalla sola verifica funzionale.
mvn -T 1 --projects :test-coverage --also-make verify -Ponboarding-bff,report \
  -Dparity.spring.jar="$SPRING_ORACLE_JAR"
```

Nel catalogo finale controllare le righe `PARITY ... attempted=... passed=...`
e l'assenza di skip dovuti all'oracolo mancante. Per le iterazioni usare i selettori
gia documentati nel README; non eseguire l'intero reactor a ogni piccolo cambiamento.
Dopo ogni uso di Docker rimuovere container, immagini, cache e reti creati dal task,
senza toccare risorse preesistenti o di altre attivita.

## Definition of Done

- [ ] BFF-ST01..BFF-ST10 completate con evidenze, non soltanto con checkbox.
- [ ] Servizi indipendenti dai package dei controller e DTO nei package previsti.
- [ ] Nessun await sui client gia reattivi nel percorso ordinario di produzione.
- [ ] Nessun I/O di file o orchestrazione business nei controller; boundary
  bloccanti esplicite e contesto tenant preservato.
- [ ] Ogni adattatore residuo ha requisito, consumer e regressione associati;
  nessuna ricostruzione generica di Spring dentro config/security.
- [ ] Nessun cambiamento funzionale, ampliamento degli accessi o modifica dei test
  usati per nascondere una regressione.
- [ ] Parita, Cucumber, OpenAPI/consumer, coverage e runtime verificati sul candidato
  finale, con limiti e blocchi dichiarati.
- [ ] BFF-G01..BFF-G03 completati per dichiarare conclusa l'intera migrazione.

Il completamento dell'epic architetturale non equivale automaticamente al merge o
al rilascio: approvazioni, builder/publish e verifiche in ambiente restano necessari.
