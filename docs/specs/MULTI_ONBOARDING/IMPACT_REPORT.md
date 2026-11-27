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

### institution-ms
- `POST /institutions/{id}/onboarding`: `institutionType` obbligatorio. Una richiesta con un tipo già presente
  restituisce l'adesione esistente; un tipo diverso crea una nuova adesione.
- `GET /institutions/{id}?productId`, billing per `externalId` e risposte bulk: aggiungere `institutionType`
  opzionale. Senza tipo, tipo, fatturazione e origine si leggono dall'ultimo onboarding.
- `GET /institutions/{id}/onboardings`: restituire tutte le adesioni del prodotto, con filtro opzionale
  `institutionType`.
- `DELETE /institutions/{id}/products/{productId}`, `PUT /institutions/{id}/created-at`, aggiornamento adesioni in
  `PUT /institutions/{id}`: accettare `institutionType`. La regola senza tipo va confermata, vedi
  [TASKS.md](TASKS.md#decisioni-ancora-aperte).
- API deleghe: filtri opzionali `fromType` e `toType`. Deleganti e delegati devono mostrare solo l'adesione del
  tipo della delega.
- Rigenerare OpenAPI e aggiornare le copie della specifica in `dashboard-bff`, `external-api`, `onboarding-ms`,
  `onboarding-functions` e `iam`.

### user-ms
- DTO di creazione e aggiornamento ruoli (`AddUserRoleDto`, `CreateUserDto`, `UpdateProductUsersDto`): aggiungere
  `institutionType` al prodotto.
- Cambio stato, cancellazione utente per prodotto, cancellazione utenti di prodotto, `check-user`, conteggio utenti
  e `/authorize`: aggiungere `institutionType` opzionale; senza tipo, usare l'ultimo onboarding.
- `GET /users/{id}`: aggiungere i filtri opzionali `institutionId`, `productId` e `institutionType`.
- Risposte con i prodotti dell'utente: aggiungere `institutionType`.

### user-group-ms
- Creazione gruppo: `institutionType` obbligatorio.
- Ricerca gruppi, operazioni sui membri per `parentInstitutionId` e rimozione di un membro da tutti i gruppi:
  aggiungere filtro e parametro `institutionType`.
- Risposte: aggiungere `institutionType`.

### iam
- `getInstitutionProducts`: distinguere le adesioni dello stesso prodotto per `institutionType`. È ancora da
  decidere se gli operatori PagoPA restano autorizzati per prodotto o per tipo.

### dashboard-bff
- `/v2/token/exchange` e token di billing: vedi layer 4.
- API di gestione utenti e gruppi: accettare e restituire `institutionType`, e passarlo a `user-ms` e
  `user-group-ms`.
- Vista dell'ente e permessi: usare `institutionType` o, senza tipo, l'ultimo onboarding; oggi alcuni flussi usano
  la prima adesione attiva.

### external-api e APIM
- `GET /v2/institutions/{id}/contract`: aggiungere `institutionType` opzionale; senza tipo, usare l'ultimo
  onboarding. L'adesione si sceglie in `institution-ms`; il contratto si recupera tramite le API documentali
  esistenti.
- `POST /v3/users` (`getUserInfo` v3): nuova versione con tutte le associazioni e il relativo `institutionType`.
- `/v2/users/{id}/onboarded-product`, `/v2/users`, `/v2/institutions/{id}/products`, `/v2/institutions/{id}/users`:
  aggiungere `institutionType` opzionale in ingresso e in risposta. Oggi alcune risposte mantengono una sola
  adesione per prodotto.
- APIM: aggiornare merge OpenAPI e pubblicazione v3, controlli Optic, richieste Bruno e documentazione per i
  prodotti. Le chiavi APIM non cambiano.

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
- **UserGroupEntity:** aggiungere `institutionType` ai criteri di ricerca.
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
