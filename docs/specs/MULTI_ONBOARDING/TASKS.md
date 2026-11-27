# MULTI_ONBOARDING — Task

Task derivati da [SPEC.md](SPEC.md) e dalla
[Design Review](https://pagopa.atlassian.net/wiki/spaces/SCP/pages/3495526860/DR+-+Gestione+multi+contratto+per+singolo+prodotto)
(bozza del 26/11/2026). Ambito: Institution, User, UserGroup, Delegation, Token Exchange ed External APIs.

## Presupposti

- Onboarding e product sono già adeguati, compreso il passaggio di `institutionType` da `onboarding-functions`
  verso `institution-ms` e `user-ms`.
- `institutionType` è l'unico campo propagato e aggiunto alla chiave naturale delle entità coinvolte.
- `document-ms` è fuori ambito.
- I dati esistenti vengono corretti con un aggiornamento massivo che imposta il corretto `institutionType`.
- Le API retrocompatibili, quando `institutionType` non è indicato, restituiscono il risultato relativo
  all'ultimo onboarding.
- Su `Delegations`, `fromType` e `toType` sono già valorizzati correttamente e la deduplica di `delegation-cdc` è
  già implementata.

### Verifica degli indici

Nel Terraform delle collection coinvolte non ci sono indici unici sulle chiavi interessate. In particolare
`Institution.onboarding.productId` è `unique = false`. L'annotazione Java `@Indexed(unique = true)` su
`OnboardingEntity.productId` non viene applicata, perché `auto-index-creation` è disattivato. L'unicità delle
adesioni va quindi garantita nel codice applicativo.

Gli unici indici unici rilevanti sono `Institution.externalId` e `MailNotification.institutionId`, ma non
bloccano due adesioni sullo stesso ente.

## T1 — Institution e Delegation

**Componenti:** `institution-ms`, `institution-cdc`

- **Adesioni:** aggiungere `institutionType` alla chiave naturale ente + prodotto e renderlo obbligatorio nelle
  nuove adesioni. Una seconda richiesta con lo stesso tipo non deve creare un duplicato, anche con richieste
  concorrenti. Oggi il servizio rifiuta i tipi diversi e controlla i duplicati prima di scrivere, separatamente.
  [Persistenza](../../../apps/institution-ms/core/src/main/java/it/pagopa/selfcare/mscore/core/OnboardingServiceImpl.java#L98)
  · [Inserimento](../../../apps/institution-ms/connector/dao/src/main/java/it/pagopa/selfcare/mscore/connector/dao/InstitutionConnectorImpl.java#L156)
- **Operazioni sulla singola adesione:** cancellazione, modifica di `createdAt` e aggiornamenti di fatturazione o
  origine devono usare `institutionType`. Oggi agiscono su tutte le adesioni del prodotto.
  [DAO](../../../apps/institution-ms/connector/dao/src/main/java/it/pagopa/selfcare/mscore/connector/dao/InstitutionConnectorImpl.java#L281)
  · [Aggiornamenti](../../../apps/institution-ms/connector/dao/src/main/java/it/pagopa/selfcare/mscore/connector/dao/model/mapper/InstitutionMapperHelper.java#L33)
- **Letture retrocompatibili:** se `institutionType` è presente, filtrare per tipo; se manca, usare l'ultimo
  onboarding per tipo, fatturazione e origine. Oggi alcuni metodi prendono la prima adesione trovata. Le liste
  devono restituire tutte le adesioni.
  [Lettura adesioni](../../../apps/institution-ms/connector/dao/src/main/java/it/pagopa/selfcare/mscore/connector/dao/InstitutionConnectorImpl.java#L225)
  · [Mapper](../../../apps/institution-ms/web/src/main/java/it/pagopa/selfcare/mscore/web/model/mapper/InstitutionMapperCustom.java#L26)
- **PEC periodica:** mantenerla solo per le adesioni diverse da PT. Chiudere l'adesione PT non deve rimuovere la
  PEC dell'adesione PSP, PA o GSP.
  [PEC](../../../apps/institution-ms/connector/dao/src/main/java/it/pagopa/selfcare/mscore/connector/dao/MailNotificationConnectorImpl.java#L50)
- **Deleghe:** aggiungere `fromType` e `toType` ai controlli di unicità e riattivazione; oggi la riattivazione usa
  solo delegante, delegato e prodotto. Nelle letture di deleganti e delegati, mostrare solo l'adesione
  corrispondente al tipo della delega.
  [Deleghe](../../../apps/institution-ms/connector/dao/src/main/java/it/pagopa/selfcare/mscore/connector/dao/DelegationConnectorImpl.java#L68)
  · [Letture](../../../apps/institution-ms/core/src/main/java/it/pagopa/selfcare/mscore/core/DelegationServiceImpl.java#L200)
- **Contratti API:** verificare gli eventi di `institution-cdc`, rigenerare OpenAPI e aggiornare le copie della
  specifica nei client.

**Done:** due adesioni con tipi diversi coesistono; ogni operazione tocca solo quella indicata; senza tipo si
ottiene l'ultimo onboarding.

## T2 — User: ruoli separati per `institutionType`

**Componenti:** `user-ms`, `user-cdc`, `libs/selfcare-user-sdk-model`, `libs/selfcare-user-sdk-event`, gestione
utenti in `dashboard-bff`

- **Modello:** aggiungere `institutionType` a `OnboardedProduct` e valorizzarlo in ogni scrittura. Deprecare
  gradualmente `partnerTechRole`.
  [SDK](../../../libs/selfcare-user-sdk-model/src/main/java/it.pagopa.selfcare.user.model/OnboardedProduct.java#L17)
- **Operazioni utente:** usare prodotto e tipo in assegnazione, controlli multiruolo, cambio stato, cancellazione,
  verifica utente, conteggi e `/authorize`. I ruoli ammessi devono venire dal mapping del tipo. Le API legacy
  senza tipo usano l'ultimo onboarding.
  [Assegnazione](../../../apps/user-ms/src/main/java/it/pagopa/selfcare/user/service/UserServiceImpl.java#L944)
  · [Stato e cancellazione](../../../apps/user-ms/src/main/java/it/pagopa/selfcare/user/service/UserInstitutionServiceDefault.java#L208)
  · [Dashboard](../../../apps/dashboard-bff/src/main/java/it/pagopa/selfcare/dashboard/service/UserV2ServiceImpl.java#L236)
- **Sincronizzazione degli amministratori:** limitarla al tipo, senza revocare gli amministratori dell'altra
  adesione.
  [Sincronizzazione](../../../apps/user-ms/src/main/java/it/pagopa/selfcare/user/service/UserServiceImpl.java#L385)
- **Eventi e aggregati:** in `user-cdc`, raggruppare i ruoli per prodotto, tipo e ruolo. Aggiungere
  `institutionType` agli eventi `sc-users`, FD e webhook, includendolo anche nell'ID evento per evitare collisioni.
  I ruoli propagati agli aggregati devono usare il tipo dell'aggregato già presente nella delega.
  [ID eventi](../../../libs/selfcare-user-sdk-event/src/main/java/it/pagopa/selfcare/user/UserUtils.java#L31)
  · [Aggregati](../../../apps/user-cdc/src/main/java/it/pagopa/selfcare/user/event/UserInstitutionCdcService.java#L416)

**Done:** MANAGER PSP e DELEGATE PT dello stesso ente restano distinti in scritture, autorizzazioni ed eventi.

## T3 — UserGroup: gruppi separati per `institutionType`

**Componenti:** `user-group-ms`, `user-group-cdc`, gestione gruppi in `dashboard-bff`

- **Modello e filtri:** aggiungere `institutionType` a entity, DTO e filtri. Le letture legacy senza tipo usano
  l'ultimo onboarding.
- **Nome del gruppo:** controllarne l'unicità per tenant, ente, prodotto, tipo e nome, solo tra gruppi attivi o
  sospesi. Il controllo resta applicativo.
  [Servizio](../../../apps/user-group-ms/src/main/java/it/pagopa/selfcare/user_group/service/UserGroupServiceImpl.java#L266)
- **Membri:** accettare solo utenti con ruolo valido sullo stesso tipo. Anche le rimozioni massive e i gruppi degli
  aggregatori devono distinguere il tipo.
  [Membri](../../../apps/dashboard-bff/src/main/java/it/pagopa/selfcare/dashboard/service/UserGroupV2ServiceImpl.java#L60)
- **Eventi:** aggiungere `institutionType` agli eventi `sc-usergroups`.

**Done:** i gruppi PSP e PT dello stesso ente non si mescolano.

## T4 — Token Exchange e controllo permessi

**Componenti:** `dashboard-bff`, `iam`

- **Token v2:** per i prodotti con `exchangeVersion: "v2"`, emettere `organization.institutionTypes[]` con ruoli e
  gruppi separati per tipo.
  [Token](../../../apps/dashboard-bff/src/main/java/it/pagopa/selfcare/dashboard/security/ExchangeTokenServiceV2.java#L279)
- **Token v1 e billing:** mantenere il formato attuale e includere solo ruoli e gruppi dell'ultimo onboarding del
  prodotto. Oggi ruoli e gruppi di tipi diversi verrebbero mescolati.
- **Permessi nel dashboard:** inoltrare `institutionType` nei controlli sui permessi e nella vista dell'ente; senza
  tipo, usare l'ultimo onboarding.
  [Permessi](../../../apps/dashboard-bff/src/main/java/it/pagopa/selfcare/dashboard/security/SelfCarePermissionEvaluatorV2.java#L152)
- **IAM e rilascio:** distinguere le adesioni in `getInstitutionProducts`. Concordare con il backoffice di
  `prod-pagopa` il passaggio al token v2.
  [IAM](../../../apps/iam/src/main/java/it/pagopa/selfcare/iam/service/IamServiceImpl.java#L520)

**Done:** il token v2 separa i tipi; il token v1 contiene solo l'ultimo onboarding; i controlli server negano
operazioni sul tipo sbagliato.

## T5 — External APIs

**Componenti:** `external-api` e API di `institution-ms`, `user-ms` e `user-group-ms` esposte tramite APIM

- **Contratto:** aggiungere a `GET /v2/institutions/{id}/contract` il parametro opzionale `institutionType`. Se
  manca, si usa l'ultimo onboarding. L'adesione viene scelta in `institution-ms`; il contratto si recupera con le
  API documentali esistenti tramite il suo ID.
  [Contratto](../../../apps/external-api/src/main/java/it/pagopa/selfcare/external_api/service/ContractServiceImpl.java#L58)
- **Utenti:** introdurre `POST /v3/users` (`getUserInfo` v3) con tutte le associazioni, ciascuna con
  `institutionType`. Le versioni esistenti restituiscono solo l'ultimo onboarding. Estendere `users/{id}` con i
  filtri opzionali `institutionId`, `productId` e `institutionType`.
- **Altre letture:** aggiungere `institutionType` opzionale, in ingresso e in risposta, a prodotti utente, prodotti
  dell'ente, adesioni, deleghe e gruppi. Senza tipo, mantengono la regola dell'ultimo onboarding.
  [Utenti](../../../apps/external-api/src/main/java/it/pagopa/selfcare/external_api/service/UserServiceImpl.java#L158)
- **Pubblicazione:** usare modifiche solo additive o nuove versioni, come richiesto da Optic. Aggiornare merge
  OpenAPI e APIM per la v3, Bruno e documentazione per i prodotti, senza cambiare le chiavi APIM.

**Done:** i client esistenti ricevono l'ultimo onboarding e i client aggiornati possono selezionare o leggere ogni
tipo.

## T6 — Aggiornamento massivo di `institutionType`

- **Collection da aggiornare:** completare `Institution.onboarding[].institutionType` dove manca, poi
  `userInstitutions.products[].institutionType` e `UserGroups.institutionType`. `Delegations` non va aggiornata.
- **Fonte del tipo:** usare l'adesione dell'ente per lo stesso prodotto. Prima del rilascio esiste al massimo
  un'adesione per prodotto, quindi l'attribuzione è univoca. Se manca, usare la pratica di onboarding collegata.
  Segnalare i casi senza una fonte certa per una correzione manuale, senza assegnare valori predefiniti.
- **Esecuzione sicura:** separare per tenant AR e PNPG, lavorare a lotti, rendere il batch ripetibile e partire con
  una prova senza scritture. Riutilizzare l'impostazione dello
  [script di migrazione `tenantId`](../MULTITENANT/Step_1/scripts/README.md). Prima di procedere, servono un report
  di verifica, il ripristino puntuale di Cosmos DB e un controllo dei consumi RU.
- **Eventi:** evitare che l'aggiornamento generi una massa di notifiche ai prodotti. Ad esempio, i servizi CDC
  possono ignorare le modifiche che toccano solo `institutionType`; la migrazione non deve aggiornare `updatedAt`.

**Done:** il report finale mostra zero documenti senza `institutionType` nelle tre collection.

## T7 — Rilascio in fasi e verifica

- **Fase 1 — Scrittura del tipo:** rilasciare T1–T5 in modalità compatibile: i servizi scrivono ed espongono
  `institutionType`, ma non filtrano ancora per tipo e non accettano la seconda adesione. Ordine: librerie,
  `institution-ms`, `user-ms`, `user-group-ms`, servizi CDC, `dashboard-bff`, `external-api`, APIM.
- **Fase 2 — Aggiornamento massivo:** eseguire T6 per ogni ambiente e tenant e verificare l'assenza di documenti
  senza tipo.
- **Fase 3 — Attivazione:** attivare i filtri per tipo, la seconda adesione su `prod-pagopa` e il token v2 per
  prodotto e ambiente, con rollback tramite flag.
- **Monitoraggio:** risposte `409` e `400`, duplicati rilevati in scrittura, metriche dei servizi CDC, RU di
  Cosmos DB, latenza p95 sotto 500 ms e disponibilità di almeno 99,9% sugli endpoint coinvolti.
- **Test e documentazione:** scenari Cucumber PSP + PT sullo stesso ente, compresa la regola dell'ultimo
  onboarding e la creazione concorrente. Aggiungere test Bruno per v2 e v3, aggiornare `docs/apps/*` e la Privacy
  Review.

**Done:** ruoli, gruppi, deleghe e token dei due tipi restano isolati in AR e PNPG.

## Dipendenze

**T1–T5, solo Fase 1 → T6 → attivazione in Fase 3 → T7**

T2 e T3 possono procedere in parallelo. Nessun filtro per tipo va attivato prima che T6 confermi l'assenza di
documenti senza `institutionType`.

## Decisioni ancora aperte

- **Definizione di «ultimo onboarding»:** scegliere se ordinare per `createdAt` o `activatedAt` e confermare che si
  considerano solo le adesioni attive. Oggi il dashboard usa l'adesione attiva più recente per `createdAt`.
- **Scritture legacy senza `institutionType`:** confermare se cancellazioni e modifiche devono applicare la stessa
  regola dell'ultimo onboarding. La SPEC la definisce solo per il risultato restituito.
- **`getUserInfo` v3:** confermare il path `/v3/users`.
- **Gruppi esistenti di `prod-pagopa`:** dopo T6 ricevono il tipo dell'unica adesione attuale; resta da decidere se
  l'UI debba filtrarli per tipo.
- **Backoffice IAM:** decidere se gli operatori PagoPA restano autorizzati per prodotto o per tipo.
- **Consolidamento degli enti duplicati:** è previsto nella DR, ma non nella SPEC. Va confermato se è fuori ambito
  o se richiede un task dedicato.
