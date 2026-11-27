# MULTI_ONBOARDING — Report impatti per layer

Impatti dell'adesione multipla allo stesso prodotto con `institutionType` diversi, raggruppati per layer e per
microservizio. Fonte: [SPEC.md](SPEC.md) e [TASKS.md](TASKS.md).

**Regole comuni a tutti i layer:**
- `institutionType` è l'unico campo aggiunto alla chiave naturale delle entità.
- Senza `institutionType`, le API retrocompatibili restituiscono il risultato relativo all'ultimo onboarding.
- I dati esistenti vengono completati con un aggiornamento massivo, senza fallback a runtime.
- Gli indici Mongo non garantiscono unicità: i vincoli restano nel codice.
- Fuori ambito: `document-ms`, onboarding, product e deduplica di `delegation-cdc`.

## 1. Layer API (interne ed esterne)

Censimento di tutti gli endpoint REST di `institution-ms`, `user-ms`, `user-group-ms`, `iam`, `dashboard-bff` ed
`external-api`.

**Impatto:**
- **Nessuno:** l'endpoint non legge né scrive dati per prodotto.
- **Additivo:** parametro opzionale `institutionType` e/o campo in risposta.
- **Comportamento:** contratto invariato, ma la logica oggi sceglie la prima adesione o agisce su tutte le adesioni
  del prodotto; deve filtrare per tipo o, senza tipo, usare l'ultimo onboarding.
- **Nuova versione:** non risolvibile in modo compatibile.

**Esposizione:** tag APIM (`external-v2`, `internal-v1`, `support`, `contract`, `external-pnpg`, `support-pnpg`,
`internal-pnpg`) oppure «interna». Gli endpoint di `user-ms` senza tag sono comunque pubblicati nell'API APIM
`internal/user`, la cui specifica è mantenuta a mano.

### Vincoli trasversali
- **Optic:** `institutionType` può essere aggiunto solo come campo opzionale. Dove deve essere obbligatorio, ad
  esempio per una nuova adesione o un nuovo gruppo, va controllato nel codice.
- **Fonte dell'ultimo onboarding:** `user-ms` e `user-group-ms` non hanno client verso `institution-ms`. Il
  `createdAt` dei ruoli utente non è affidabile, perché `PUT /institutions/{id}/products/{pid}/created-at` lo
  sovrascrive su tutti i prodotti. Va deciso se il tipo viene risolto dai chiamanti (`dashboard-bff`,
  `external-api`) oppure se si aggiunge un client verso `institution-ms`.
- **Policy APIM:** le policy che impongono il `productId` della subscription non bloccano `institutionType`, che
  arriva al backend senza modifiche.
- **`POST /v3/users`:** `remove_v_and_tags.cjs` toglie il prefisso `/vX` dai path, quindi `/v3/users` entrerebbe in
  conflitto con `/v2/users` nelle API APIM attuali. Serve una nuova versione APIM `ms_external_api/v3`, con
  configurazione di merge, step nei workflow e controllo Optic, oppure una modifica manuale della specifica unita.
- **Ruoli per tipo:** `validateProductRole` della libreria product accetta ruoli di qualsiasi tipo, perché usa
  `getAllRoleMappings()`. Va usato `getRoleMappings(institutionType)`.
- **Token v2:** il modello `Product` di `selfcare-onboarding-sdk-product` non contiene ancora `exchangeVersion`.
  Va verificato che sia incluso nelle modifiche già fatte sul dominio product.
- **Tipi delle deleghe:** la SPEC considera `fromType` e `toType` già corretti. `institution-ms` però, alla
  creazione, li ricava ancora dalla prima adesione al prodotto (`InstitutionMapper:22-35`). Da verificare.
- **Copie delle specifiche:** dopo la rigenerazione OpenAPI, riallineare le copie usate per generare i client in
  `dashboard-bff`, `external-api`, `iam`, `onboarding-ms`, `onboarding-functions` e `onboarding-bff`.

### institution-ms — 40 endpoint

| Metodo | Path | Esposizione | Impatto | Cosa cambia |
|---|---|---|---|---|
| POST | `/institutions/{id}/onboarding` | interna (onboarding-functions) | Comportamento | **Bloccante:** oggi rifiuta un tipo diverso con errore 500 (`OnboardingServiceImpl:111-134`). Cercare per prodotto + tipo, scrivere in modo atomico e decidere la PEC sull'adesione appena salvata. |
| DELETE | `/institutions/{id}/products/{productId}` | interna (onboarding-functions) | Additivo | Oggi segna `DELETED` tutte le adesioni del prodotto e rimuove la PEC (`InstitutionConnectorImpl:289-293`). Aggiungere `institutionType`. |
| GET | `/institutions/{institutionId}/onboardings` | external-v2 | Comportamento + Additivo | La proiezione `$elemMatch` restituisce una sola adesione (`InstitutionConnectorImpl:237`). Restituirle tutte e aggiungere il filtro opzionale. |
| GET | `/institutions/{id}` | external-v2, internal-v1, external-pnpg | Comportamento + Additivo | Con `productId`, tipo e origine vengono dalla prima adesione (`InstitutionResourceMapper:42-67`). Filtro opzionale; senza tipo, ultimo onboarding. |
| GET | `/institutions` | external-v2, support, support-pnpg | Comportamento + Additivo | Stessa selezione della prima adesione. Filtro opzionale. |
| PUT | `/institutions/{id}` | support, support-pnpg | Comportamento + Additivo | `onboardings[]` aggiorna fatturazione e origine su tutte le adesioni del prodotto (`InstitutionMapperHelper:48`). Aggiungere `institutionType` a `OnboardingPut`. |
| PUT | `/institutions/{institutionId}/created-at` | internal-v1 | Additivo | Oggi aggiorna tutte le adesioni attive del prodotto (`InstitutionConnectorImpl:342`). Aggiungere `institutionType` a `CreatedAtRequest`. |
| GET | `/institutions/{id}/products` | interna | Additivo | Lo stesso prodotto compare due volte senza distinzione. Aggiungere il campo a `InstitutionProduct`. |
| GET | `/institutions/products/{productId}` | interna | Comportamento | `onboardings` è una mappa per prodotto e ne conserva una sola (`InstitutionMapperCustom:202-227`). Usare l'ultimo onboarding; per restituirle tutte serve una nuova versione. |
| GET | `/institutions/{productId}/brokers/{institutionType}` | interna | Nessuno | Filtra già per tipo. |
| POST | `/institutions/onboarded/{productId}` (deprecato) | interna | Nessuno | Verifica solo se esiste un'adesione al prodotto. |
| POST, GET | `/institutions`, `/from-ipa`, `/from-anac`, `/from-ivass`, `/from-pda`, `/from-infocamere`, `/pg`, `/{externalId}`, `/insert/{externalId}`, `/{id}/geotaxonomies` | interna | Nessuno | Creazione dell'ente o dati a livello di ente. |
| POST | `/delegations`, `/delegations/from-taxcode` | support, internal-v1 | Comportamento | L'unicità ignora i tipi (`DelegationConnectorImpl:69-77`). La riattivazione cerca solo delegante, delegato e prodotto (`:205-216`). Includere `fromType` e `toType`. |
| GET | `/delegations/delegators/{id}`, `/delegations/delegates/{id}` | external-v2, support | Comportamento + Additivo | Restituiscono tutte le adesioni del prodotto e un tipo preso dalla prima (`DelegationServiceImpl:200-209`). Filtrare per `fromType` o `toType` della delega. |
| GET | `/delegations`, `/v2/delegations`, `/v2/delegations/delegations-with-pagination` | external-v2, support, internal-v1 | Additivo | Filtri opzionali `fromType` e `toType`. |
| DELETE, GET | `/delegations/{id}`, `/delegations/count` | support | Nessuno | Lavorano per ID o per prodotto. |
| HEAD | `/onboarding/verify` | interna (onboarding-bff) | Additivo | Risponde positivo se esiste un'adesione di qualsiasi tipo, quindi può impedire di avviare l'adesione PT. Aggiungere il filtro opzionale. |
| HEAD | `/onboarding`, `/onboarding/institution/{externalId}/products/{productId}` (deprecati) | interna | Additivo | Filtro opzionale, se serve la verifica per tipo. |
| GET | `/external/institutions/{externalId}/products/{productId}/billing` (deprecato) | interna | Comportamento | Prende la fatturazione dall'ultima adesione trovata e il tipo dalla prima (`InstitutionMapperCustom:40-61`). Usare l'ultimo onboarding per entrambi. |
| GET | `/external/institutions/{externalId}/products` (deprecato) | interna | Additivo | Aggiungere il tipo per prodotto. |
| POST | `/bulk/institutions` (deprecato) | interna | Comportamento | La mappa per prodotto risolve i conflitti in base allo stato (`InstitutionMapperCustom:275-290`). Usare l'ultimo onboarding. |
| GET, POST | altri `/external/institutions/*`, `/events/delegations` | interna | Nessuno | Dati a livello di ente; gli eventi delega riportano già i tipi. |

### user-ms — 31 endpoint

| Metodo | Path | Esposizione | Impatto | Cosa cambia |
|---|---|---|---|---|
| POST | `/users/{userId}/onboarding` | internal-v1 (onboarding-functions) | Comportamento + Additivo | Ruolo e stato vengono dal primo ruolo trovato sul prodotto. Un nuovo `MANAGER` segna `DELETED` i ruoli di entrambi i tipi (`UserServiceImpl:939-1061`). Aggiungere `institutionType` a `AddUserRoleDto.Product`. |
| POST | `/users/{userId}`, `/users` | interna, internal-pnpg | Comportamento + Additivo | Il controllo «ruolo diverso» impedisce di aggiungere un ruolo PT a un amministratore PSP. I nuovi ruoli vengono salvati senza tipo e i controlli multiruolo usano il mapping di default. |
| PUT | `/users/{id}/status`, `/users/{id}/institution/{iid}/product/{pid}/status` | internal-v1, interna | Comportamento + Additivo | Cambiano lo stato su qualsiasi tipo e possono usare il `tokenId` dell'altra adesione (`UserInstitutionServiceDefault:161-305`). |
| DELETE | `/users/{userId}/institutions/{iid}/products/{pid}` | interna | Comportamento + Additivo | Usa il primo `tokenId` trovato e aggiorna il primo ruolo corrispondente. |
| GET | `/users/{userId}/institutions/{institutionId}` | interna (dashboard) | Comportamento + Additivo | Restituisce ruoli e azioni di entrambi i tipi. Aggiungere `institutionType` a `OnboardedProductWithActions`. |
| GET | `/users/{id}` | external-v2, support, support-pnpg, external-pnpg | Additivo | Filtri opzionali `institutionId`, `productId` e `institutionType`. |
| GET | `/users/emails` | interna | Comportamento + Additivo | Restituisce le email degli amministratori di entrambi i tipi. |
| GET | `/users`, `/users/ids`, `/users/notification`, `/users/{userId}/institution/{institutionId}` | interna | Additivo | Filtro e campo `institutionType`. L'ID notifica collide quando lo stesso ruolo esiste su due tipi. |
| PUT | `/institutions/{id}/products/{pid}/users` | interna (onboarding-functions) | Comportamento + Additivo | La sincronizzazione degli amministratori revoca quelli dell'altro tipo e li rimuove dai gruppi. Aggiungere `institutionType` a `UpdateProductUsersDto`. |
| DELETE | `/institutions/{id}/products/{pid}/users` | interna (onboarding-functions) | Comportamento + Additivo | Cancella gli utenti di entrambi i tipi (`UserInstitutionServiceDefault:209-254`). |
| PUT | `/institutions/{id}/products/{pid}/created-at` | interna | Comportamento + Additivo | `products.$[]` sovrascrive `createdAt` su tutti i prodotti dell'utente. È un bug già presente; usare filtri per prodotto e tipo. |
| GET, POST | `/institutions/{id}/products/{pid}/users/count`, `/institutions/{id}/product/{pid}/check-user` | interna (dashboard) | Comportamento + Additivo | Considerano tutti i tipi. Parametro opzionale; senza tipo, ultimo onboarding. |
| GET | `/institutions/{id}/users` | support, support-pnpg | Additivo | Filtro e campo `institutionType`. |
| GET | `/institutions/{id}/user-institutions` | interna (token exchange, gruppi) | Additivo | Campo `institutionType`, necessario ai chiamanti per distinguere i ruoli. |
| POST | `/institutions/{id}/send-mail` | interna | Additivo | Destinatari filtrabili per tipo. |
| GET | `/authorize` | interna | Comportamento + Additivo | Un `ADMIN` su un tipo risulta autorizzato anche sull'altro. |
| POST | `/events/sc-users` | interna | Additivo | Payload degli eventi reinviati con `institutionType`. |
| GET, POST, PUT | `/users/{userId}/institutions`, `/users/{id}/details`, `/users/search`, `/users/{id}/user-registry`, `/users/{userId}/send-mail-request`, `/users/{userId}/otp-info`, `/institutions/{id}` | varie | Nessuno | Dati anagrafici, email o dati a livello di ente. |

**DTO da estendere con `institutionType`:**
- **Richieste:** `AddUserRoleDto.Product`, `CreateUserDto.Product`, `UpdateProductUsersDto`, `SendInstitutionMailDto`.
- **Risposte:** `OnboardedProductResponse` e le risposte che lo contengono, `OnboardedProductWithActions`,
  `UsersCountResponse`, `DeletedUserCountResponse`, `UserNotificationResponse`.

### user-group-ms — 10 endpoint

| Metodo | Path | Esposizione | Impatto | Cosa cambia |
|---|---|---|---|---|
| POST | `/v1/user-groups` | interna (dashboard) | Comportamento + Additivo | Aggiungere `institutionType` a DTO, entity e risposta. L'unicità del nome ignora il tipo (`UserGroupServiceImpl:266-275`). |
| PUT | `/v1/user-groups/{id}` | interna | Comportamento | L'unicità del nome deve considerare il tipo. Il costruttore di copia deve copiare il tipo, altrimenti ogni modifica lo cancella (`UserGroupEntity:19-33`). |
| GET | `/v1/user-groups` | external-v2, support, external-pnpg, support-pnpg | Comportamento + Additivo | Senza tipo restituisce i gruppi di tutti i tipi. Aggiungere il filtro opzionale; per l'ultimo onboarding vedi i vincoli trasversali. |
| GET | `/v1/user-groups/{id}` | external-v2 | Additivo | Campo `institutionType` in risposta. |
| PUT | `/v1/user-groups/members` | internal-v1 (user-cdc) | Comportamento + Additivo | L'upsert del gruppo aggregatore unisce membri PSP e PT. |
| DELETE | `/v1/user-groups/members` | internal-v1 (user-cdc) | Comportamento + Additivo | Con due gruppi aggregatori genera `IllegalStateException` (`UserGroupServiceImpl:307-318`). |
| DELETE | `/v1/user-groups/members/{memberId}` | support | Comportamento + Additivo | Rimuove il membro dai gruppi di tutti i tipi. |
| DELETE, POST, PUT | `/{id}`, `/{id}/activate`, `/{id}/suspend`, `/{id}/members/{memberId}` | interna, support | Nessuno | Operano tramite l'ID del gruppo. |

### iam — 8 endpoint

I ruoli IAM sono legati a prodotto e tenant, non all'ente, quindi non possono entrare in conflitto per tipo. Tutti
gli endpoint risultano **Nessuno**. Fa eccezione `GET /iam/users/{uid}/permissions/{permission}`, usato da
`dashboard-bff`: può ricevere un `institutionType` opzionale per verificare che quel tipo sia effettivamente
aderito. Resta da decidere se gli operatori PagoPA vanno autorizzati per prodotto o per tipo.

### dashboard-bff — 47 endpoint

Tutte le API sono esposte tramite APIM con base path `dashboard` (AR) e `imprese/dashboard` (PNPG).

**Controllo permessi, valido per tutti gli endpoint con `@PreAuthorize`:** `FilterAuthorityDomain` contiene solo
ente, prodotto e gruppo, quindi un permesso su un tipo vale anche sull'altro
(`SelfCarePermissionEvaluatorV2:201-211`). Va aggiunto `institutionType`, da passare a `user-ms`; per i gruppi va
usato il tipo del gruppo.

| Metodo | Path | Impatto | Cosa cambia |
|---|---|---|---|
| GET | `/v2/token/exchange`, `/v2/products/{productId}/back-office` | Comportamento | I ruoli sono raggruppati per prodotto e `partyRole` viene dal primo ruolo trovato (`ExchangeTokenServiceV2:279-291`). Token v2 con `institutionTypes[]`; token v1 con ruoli e gruppi dell'ultimo onboarding. |
| GET | `/v2/token/exchange/fatturazione` | Comportamento | Mescola ruoli di tipi diversi e include i gruppi di tutti i prodotti (`:197-205`). Usare l'ultimo onboarding per ogni prodotto fatturabile. |
| GET | `/v2/institutions/{institutionId}` | Comportamento | Ruolo e azioni per prodotto vengono dal primo ruolo trovato (`InstitutionV2ServiceImpl:152-161, 263-265`). Collegarli a prodotto + tipo. |
| POST, PUT | `/v2/institutions/{id}/products/{pid}/users[/{userId}]` | Comportamento + Additivo | Valida i ruoli sul tipo della prima adesione attiva e non passa il tipo a `user-ms` (`UserV2ServiceImpl:242-299`). |
| POST, DELETE | `/v2/users/{userId}/suspend`, `/activate`, `/v2/users/{userId}` | Comportamento + Additivo | Modificano i ruoli e puliscono i gruppi su tutti i tipi (`UserV2ServiceImpl:104-128`, `UserGroupV2ServiceImpl:330-340`). |
| GET | `/v2/users/institution/{id}`, `/v2/users/all/institution/{id}` | Comportamento + Additivo | Una riga per prodotto con i ruoli di entrambi i tipi; `getLatestProduct` prende il primo ruolo attivo. |
| GET | `/v2/institutions/{id}/users/{userId}`, `/v2/institutions/all/{id}/users/{userId}` | Additivo | Ruoli raggruppati per prodotto. Aggiungere il tipo a `ProductRoleInfoResource`. |
| GET, POST | `/v2/institutions/{id}/products/{pid}/users/count`, `/v2/institutions/{id}/product/{pid}/check-user` | Comportamento + Additivo | Passare il tipo a `user-ms`. Oggi `check-user` impedisce di aggiungere all'adesione PT un utente già PSP. |
| POST | `/v2/user-groups` | Comportamento + Additivo | Tipo opzionale; senza tipo, ultimo onboarding. I membri vengono validati solo per prodotto. |
| PUT, POST, GET | `/v2/user-groups/{id}`, `/{id}/members/{userId}`, `/{id}`, `/me/{id}`, `/v2/user-groups`, `/me` | Comportamento + Additivo | Membri e liste vanno filtrati per il tipo del gruppo; aggiungere il campo nelle risposte. |
| DELETE, POST | `/v2/user-groups/{id}`, `/activate`, `/suspend`, `/{groupId}/members/{userId}` | Comportamento | La logica usa l'ID del gruppo; cambia solo il controllo dei permessi. |
| GET | `/v1/institutions/{institutionId}` | Comportamento | Assegna lo stesso ruolo alle due adesioni dello stesso prodotto. |
| GET | `/v2/institutions/all/{institutionId}` | Comportamento | Permessi IAM per prodotto; dipende dalla decisione aperta su IAM. |
| GET | `/v2/institutions/{id}/product/{pid}/attachment/status`, `/v1/documents/institutions/{id}/products/{pid}[/document]` | Additivo | Usano già l'ultimo onboarding attivo; aggiungere il tipo opzionale per accedere all'altro. |
| GET | `/v2/institutions/onboardings/{productId}/pending`, `/v1/documents/status`, deleghe di `/v1/institutions/{id}/partners`, `/v1/institutions/{id}/institutions`, `/v2/institutions/{id}/institutions` | Additivo (basso) | Filtro o campo opzionale per tipo. |
| POST | `/v1/delegations` | Additivo | Tipo opzionale nella richiesta; vedi i tipi delle deleghe nei vincoli trasversali. |
| — | altri endpoint: logo, tassonomie, prodotti, ruoli prodotto, broker, `my-permissions`, `contract-update`, utenti in anagrafica, OTP, supporto, `back-office/admin` | Nessuno | Dati a livello di ente o utente, oppure già tipizzati. |

### external-api — 14 endpoint esistenti più `/v3/users`

| Metodo | Path | Esposizione | Impatto | Cosa cambia |
|---|---|---|---|---|
| GET | `/v2/institutions/{institutionId}/contract` | external-v2, support, contract | Comportamento + Additivo | Prende la prima adesione della lista (`ContractServiceImpl:63-75`). Aggiungere `institutionType`; senza tipo, ultimo onboarding. Il download continua a usare le API documentali tramite `tokenId`. |
| POST | `/v2/users` (`getUserInfo`) | external-v2, internal-v1, support, support-pnpg | Comportamento | Tiene una sola adesione per prodotto in base alla data del ruolo, non dell'adesione (`UserServiceImpl:158-162`). La fatturazione viene dalla prima adesione dell'ente. |
| POST | `/v3/users` (`getUserInfo` v3) | da pubblicare | Nuova versione | Una voce per ogni adesione, con `institutionType`. Vedi il vincolo APIM. |
| GET | `/v2/institutions/{id}/users` | external-v2, internal-v1, external-pnpg | Comportamento + Additivo | Mescola i ruoli delle due adesioni (`InstitutionServiceImpl:156-169`). Serve il filtro per tipo in `user-ms`. |
| GET | `/v2/institutions/{id}/products` | external-v2, external-pnpg | Comportamento | Template di contratto e ruoli vengono dal tipo a livello di ente (`InstitutionServiceImpl:127,142`). Usare il tipo dell'ultimo onboarding. |
| GET | `/v2/institutions` (deprecato) | external-pnpg; io-sign tramite policy SELC | Comportamento | Stato minimo tra le adesioni, tipo a livello di ente e prima fatturazione. Usare l'ultimo onboarding. |
| GET | `/v2/users` | external-v2, external-pnpg | Additivo | Campo `institutionType` per prodotto, quando `user-ms` lo restituirà. |
| POST | `/v2/onboarding/users` | internal-v1, support, support-pnpg | Additivo | Scrive ruoli utente senza tipo. Aggiungere `institutionType` opzionale; senza tipo, ultimo onboarding. |
| GET | `/v2/users/{id}/onboarded-product` | non pubblicato in APIM | Comportamento (basso) | Ruolo e data vengono dal primo elemento. |
| POST, GET | `/v2/onboarding`, `/v2/onboarding/import`, `/v2/onboarding/aggregation/{taxCode}`, `/v1/tokens/products/{productId}`, `/v1/pn-pg/institutions/add`, `/v2/national-registries/legal-tax/verification` | varie | Nessuno | Inoltro al dominio onboarding o al registro. |

### APIM, specifiche e test
- **Specifiche unite da aggiornare per ambiente:**
  - `api/ms_external_api/v2`
  - `api/ms_internal_api/v1`
  - `api/selfcare_support_service/v1`
  - `api/external_api_contract/v1`
  - `api_pnpg/external_api_for_pnpg/v2`
  - `api_pnpg/pnpg_support_service/v1`
  - `api/internal_user_api/v1`, da aggiornare a mano
- **Merge automatico:** non aggiorna la specifica di `external-api` (`create_openapi_merge.yml:91`). Serve
  un'esecuzione manuale del workflow oppure una modifica manuale.
- **Specifica e client di `external-api`:** rigenerare `swagger/api-docs.json` e aggiornare
  `docs/openapi/api-selfcare-{user,core}-docs.json`.
- **Bruno:** aggiornare le richieste:
  - `getContract` in `External`, `InstitutionV2` e `Support`
  - `getInstitutionUsers*` in `External`, `InstitutionV2` e `Internal`
  - `getInstitutionProducts` in `External` e `InstitutionV2`
  - `UserV2/getUserInfo`, `UserV2/getUserInstitution`, `Support/getUserById`

  Aggiungere `UserV3/getUserInfo` e allineare la collection Newman eseguita in CI.

### Endpoint più critici
1. `POST /institutions/{id}/onboarding`: oggi impedisce di salvare la seconda adesione.
2. `DELETE /institutions/{id}/products/{productId}` e `DELETE /institutions/{id}/products/{pid}/users`: chiudere
   un'adesione disattiva anche l'altra e i suoi utenti.
3. `PUT /institutions/{id}/products/{pid}/users`: la sincronizzazione revoca gli amministratori dell'altro tipo.
4. `POST /users/{userId}/onboarding`: un nuovo `MANAGER` cancella i ruoli di entrambi i tipi.
5. `GET /authorize` e controllo permessi di `dashboard-bff`: un permesso su un tipo vale anche sull'altro.
6. `/v2/token/exchange`: il token mescola ruoli e gruppi dei due tipi.
7. `GET /institutions/{id}/onboardings`: nasconde la seconda adesione anche alle API esterne.
8. `DELETE /v1/user-groups/members`: fallisce quando esistono due gruppi aggregatori.

## 2. Modello dati e CRUD su database

### institution-ms (`selcMsCore`)
- **Institution:** `onboarding[]` può contenere più adesioni con lo stesso `productId` e tipi diversi.
  L'inserimento deve diventare atomico, con una scrittura condizionata su prodotto e `institutionType`; oggi il
  servizio prima legge e poi aggiunge l'adesione.
- **DAO:** usare `institutionType` in cancellazione, `createdAt` e aggiornamenti di fatturazione o origine; oggi
  questi metodi filtrano solo per prodotto. Rimuovere la proiezione che restituisce una sola adesione per prodotto.
- **Delegations:** includere `fromType` e `toType` nei controlli di unicità e riattivazione. Nessuna modifica ai
  dati: i tipi sono già valorizzati.
- **MailNotification:** mantenere il prodotto finché esiste un'adesione diversa da PT.
- **Indici:** nessun nuovo indice unico. L'annotazione `@Indexed(unique = true)` su `OnboardingEntity.productId`
  non viene applicata: rimuoverla evita ambiguità.

### user-ms (`selcUser`) e libs/selfcare-user-sdk-model
- **OnboardedProduct:** aggiungere `institutionType`; deprecare gradualmente `partnerTechRole`.
- **Filtri e aggiornamenti:** aggiungere `institutionType` a `OnboardedProductFilter`, ai filtri sugli array per
  stato e cancellazione e alle ricerche di ruolo e stato per prodotto.
- **Controlli multiruolo:** applicarli a prodotto + `institutionType`, usando il mapping dei ruoli del tipo.

### user-group-ms (`selcUserGroup`)
- **UserGroupEntity:** aggiungere `institutionType` ai criteri di ricerca e al costruttore di copia usato nei
  salvataggi.
- **Unicità del nome:** controllare tenant, ente, prodotto, tipo e nome. Anche l'upsert dei gruppi aggregatori e
  le rimozioni dei membri devono usare il tipo.

### Aggiornamento massivo
- Completare `Institution.onboarding[].institutionType` dove manca, poi
  `userInstitutions.products[].institutionType` e `UserGroups.institutionType`.
- `Delegations` è esclusa: `fromType` e `toType` sono già corretti.

## 3. Integrazioni via coda e CDC

### user-ms
- Eventi `sc-users` su Event Hub e webhook `SC-Users`: aggiungere `institutionType` al payload.

### user-cdc e libs/selfcare-user-sdk-event
- **Raggruppamento ruoli:** usare prodotto, `institutionType` e ruolo in `groupingProduct*`. Oggi ruoli identici su
  tipi diversi verrebbero fusi.
- **ID evento:** includere `institutionType` in `uniqueIdNotification`.
- **Payload:** aggiungere `institutionType` agli eventi utente e FD.
- **Aggregati:** propagare i ruoli usando la delega EA e il tipo dell'aggregato; passare `institutionType` nelle
  chiamate ai gruppi aggregatori.

### user-group-cdc
- Eventi `sc-usergroups`: aggiungere `institutionType` al payload.

### institution-cdc
- `InstitutionUpdatedEvent` non cambia: verificare che più adesioni nello stesso ente non alterino il flusso.

### Aggiornamento massivo e CDC
- I servizi CDC devono ignorare le modifiche che toccano solo `institutionType`. L'aggiornamento massivo non deve
  aggiornare `updatedAt`, per evitare notifiche massive ai prodotti.

### Fuori ambito
- `delegation-cdc`: deduplica già implementata.
- `onboarding-functions`: già adeguato.
- Consumatori Service Bus di `user-ms` e `user-group-ms`: nessun impatto.

## 4. Altre

### Token Exchange (`dashboard-bff`)
- **Token v2:** con `exchangeVersion: "v2"`, emettere `organization.institutionTypes[]` con ruoli e gruppi per tipo.
- **Token v1 e billing:** mantenere il formato attuale con ruoli e gruppi dell'ultimo onboarding; oggi i ruoli sono
  raggruppati solo per prodotto.
- **Permessi:** il controllo dei permessi deve usare `institutionType` anche per i gruppi.

### Librerie
- Rilasciare `selfcare-user-sdk-model` e `selfcare-user-sdk-event`, poi aggiornarle in `user-ms` e `user-cdc`.
- `selfcare-onboarding-sdk-product`: verificare la presenza di `exchangeVersion` e la validazione dei ruoli per
  tipo (vedi i vincoli trasversali del layer API).

### Migrazione dati
- Batch ripetibile per tenant AR e PNPG, eseguito a lotti, con prova senza scritture e report finale.
- Seguire l'impostazione dello [script di migrazione `tenantId`](../MULTITENANT/Step_1/scripts/README.md).
- I casi senza una fonte certa del tipo vanno corretti manualmente.

### Rilascio e configurazione
- **Rilascio in fasi:** prima la scrittura del tipo, poi l'aggiornamento massivo, infine l'attivazione dei filtri
  per tipo, della seconda adesione e del token v2.
- **Flag di attivazione:** configurarli in `infra/resources/<app>`. Non servono modifiche Terraform agli indici.

### Test e documentazione
- Cucumber: scenari PSP + PT sullo stesso ente, regola dell'ultimo onboarding e creazione concorrente.
- Bruno: test per v2 e v3.
- Aggiornare `docs/apps/*` e la Privacy Review.
