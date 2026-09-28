# Applicazione dello Step 2 ad `apps/auth`

Questo documento descrive il lavoro **da svolgere**, non una migrazione gia' effettuata. Si applicano
`REQUIREMENTS.md`, `ARCHITECTURE.md`, `Database_identification.md` e
`JWT_Key_Resolution.md` di questo step. `auth` gestisce flussi OIDC/OneIdentity, SAML e OTP:
non tutti ricevono un JWT in ingresso. La risoluzione del tenant per questi ingressi deve
rispettare il confine di fiducia definito nello Step 0, senza imporre artificialmente un JWT
al callback o all'exchange. Tutte le scelte di risorse successive usano il tenant gia'
validato, mai un parametro o un header riletto dal client.

## Stato attuale e obiettivo

- `apps/auth/conf/TenantRegistry` legge `tenant.registry.json` dello Step 0, abilita oggi solo
  AR per l'autenticazione e legge gia' `tenant.ar.one-identity.client-id` e
  `tenant.ar.one-identity.client-secret`. `OidcServiceImpl` usa le credenziali di quel tenant.
  `TenantSessionKeyProvider` seleziona gia' chiave privata e `kid` di sessione per tenant.
  PNPG e' configurato con `HUB_SPID_LOGIN` e `auth_enabled=false`: non attivare OIDC o SAML
  per PNPG per effetto della sola unificazione del deployment.
- `auth` usa `selfcare-sdk-tenant` per risolvere le credenziali User Registry e
  `selfcare-sdk-tenant-mongodb` per selezionare client e database Mongo da
  `TenantContext`. Il filtro di ingresso copia il tenant validato in quel contesto.
  Nel deployment corrente la route Mongo e' configurata solo per AR; PNPG resta
  disabilitato e non eredita la connessione AR.
  `OtpFlow` contiene `tenantId`, ma alcune operazioni OTP usano filtri per `uuid` o
  `userId` senza vincolo di tenant; il conteggio giornaliero non e' scoped.
- `UserServiceImpl` usa il client generato `user_registry_json` tramite
  `TenantUserRegistryApi`: `TenantUserRegistryApiKeyFilter` seleziona `x-api-key` dal
  registry in base al tenant validato, per ogni richiesta. La vecchia proprieta'
  `quarkus.openapi-generator.user_registry_json.auth.api_key.api-key` non e' piu' usata.
  Anche `auth-ms.mail-sender`, `one_mail.api.key`, le destinazioni e le chiavi degli altri
  client outbound sono attualmente singole; vanno classificate prima del cutover.
- `infra/resources/auth/{dev,uat,prod}-ar/auth.tf` espone il registry di autenticazione,
  il registry delle credenziali per tenant e i secret AR; le risorse Mongo/PDV per
  entrambi i tenant non sono ancora configurate: la route Mongo AR e' presente,
  quella PNPG e il routing PDV no.
  L'implementazione Step 2 deve mantenere separata l'identita' del tenant dalla topologia
  delle risorse: account condivisi e dedicati passano dalla **stessa** risoluzione.

## Contratto di configurazione proposto

Estendere la definizione condivisa delle risorse per tenant, senza aggiungere valori di
secret nel JSON e senza creare una seconda mappa di routing specifica per `auth`.
`oneIdentity` e `userRegistry` sono dimensioni di primo livello **di ciascun tenant**,
riutilizzabili anche da altri servizi: non vanno annidate sotto `auth`. Questo esempio
indica il contenuto richiesto per un tenant che usa entrambi i servizi:

```json
{
  "AR": {
    "mongo": {
      "account": "cosmos-ar",
      "database": "selcAuth",
      "connectionStringEnvVar": "MONGODB_CONNECTION_STRING_AR"
    },
    "oneIdentity": {
      "clientIdEnvVar": "TENANT_AR_ONE_IDENTITY_CLIENT_ID",
      "clientSecretEnvVar": "TENANT_AR_ONE_IDENTITY_CLIENT_SECRET"
    },
    "userRegistry": {
      "apiKeyEnvVar": "USER_REGISTRY_API_KEY_AR"
    }
  }
}
```

PNPG richiede una voce `mongo` distinta (anche se il database fosse condiviso). Le
dimensioni `oneIdentity` e `userRegistry` sono configurate per tenant e validate come
obbligatorie solo dai servizi/flussi che le usano: non inventare credenziali PNPG per
`auth` mentre `auth_enabled=false`, ma non legare la disponibilita' di `userRegistry`
all'abilitazione di `auth` se altri servizi lo utilizzano. Se il login PNPG verra'
abilitato, definire prima il provider, il contratto PDV e le sue credenziali. Conservare nel registry
Step 0 i campi pubblici (`frontend_uri`, `api_uri`, `allowed_origins`,
`authentication_provider`, `auth_enabled`); riconciliare i due schemi in una sola
definizione per tenant oppure tramite un adapter che legge la medesima configurazione
canonica. `AuthTenantContext` e il `TenantContext` delle librerie devono rappresentare lo
stesso tenant verificato, senza due risoluzioni indipendenti.

Durante l'adozione progressiva, `auth` usa `tenant.resources.registry.json` separato
dal `tenant.registry.json` dello Step 0: entrambi indicizzano gli stessi tenant.
Il primo contiene i riferimenti alle credenziali e alla connessione Mongo AR.
Terraform deriva i riferimenti di credenziale AR da `tenant_credential_resources`
nel modulo `local-env` e aggiunge la route Mongo `selcAuth` nello stack `auth`.
`userRegistry` e' presente anche per PNPG nel registro condiviso, affinche' gli
altri microservizi possano configurarne la propria credenziale. `auth` espone
solo il secret AR e `tenant.supported-tenants=AR` limita l'inizializzazione dei
client all'unico tenant oggi abilitato; configurare e validare i secret e la
route Mongo PNPG prima di includere PNPG nel deployment di `auth`.

I nomi MicroProfile `tenant.ar.one-identity.client-id` e
`tenant.ar.one-identity.client-secret` sono gia' definiti in `application.properties`:
mantenerli come ponte per i secret AR durante la migrazione e generalizzare il lookup
per tenant abilitati, senza spostarne i **valori** nel registry. I riferimenti
`clientIdEnvVar`/`clientSecretEnvVar` puntano alle variabili secret-backed corrispondenti;
stabilire un'unica fonte di lettura per evitare configurazioni divergenti. Per User Registry
la proprieta' esistente `quarkus.openapi-generator.user_registry_json.auth.api_key.api-key`
e' **globale**: non e' sufficiente interpolarvi una nuova variabile. Il client generato
usa ora un filtro per richiesta per impostare `x-api-key` dal tenant validato. La copia
di `src/main/openapi/user_registry.json` usata per generare il client omette la
`security` sulle operazioni: il generatore Quarkiverse 2.16.0 altrimenti registra
automaticamente un provider di autenticazione globale. Lo schema `api_key` rimane
definito nella copia; il contratto del servizio remoto continua a richiedere
`x-api-key`. La proprieta' globale e il fallback `example-api-key` sono rimossi.
Credenziali e API key vanno in Key Vault,
iniettate come configurazione secret-backed distinta per tenant (SELC-17.2/17.4).

## Attivita' ordinate per dipendenza

1. **Completato - librerie - modello e registry (`libs/selfcare-sdk-tenant`).** Estendere
   `TenantDefinition`/`TenantRegistry` con due dimensioni opzionali di primo livello per
   tenant, `oneIdentity` e `userRegistry` (riferimenti a client ID, client secret
   OneIdentity e API key User Registry), senza vincolarle al solo microservizio `auth`.
   Esporre accessor tipizzati che risolvono i valori dalle variabili iniettate e
   falliscono se un mapping richiesto e' assente/vuoto. Validare all'avvio i riferimenti
   obbligatori per ogni servizio/flusso che usa la dimensione; non rendere OneIdentity
   obbligatorio per il login PNPG disabilitato e non cambiare gli altri consumer. Testare entrambi i tenant,
   configurazioni mancanti, duplicati e assenza di fallback.
2. **Completato per AR - librerie - contesto e Mongo (`libs/selfcare-sdk-tenant-mongodb`,
   `libs/selfcare-sdk-security` se necessario).** Riutilizzare il producer Mongo e il
   resolver per selezionare account/database `selcAuth` da `TenantContext`. Definire
   l'integrazione con gli ingressi auth privi di JWT: riconciliare il tenant attendibile
   dello Step 0 prima della chiamata a Panache, senza applicare a OIDC/SAML una validazione
   JWT impossibile. Verificare che il contesto resti isolato tra richieste concorrenti e
   che la verifica dei JWT presenti resti corretta (SELC-12, SELC-13).
3. **Completato per AR - client outbound.** Implementare o riutilizzare un provider/filtro
   request-scoped per client REST che legga il tenant validato e aggiunga `x-api-key` dal
   registry. Usarlo nel client OpenAPI `user_registry_json` al posto dell'autenticazione
   globale generata; controllare ordine/registrazione dei filtri del generatore e impedire
   che una chiave globale sovrascriva quella per tenant. Coprire chiamate AR/PNPG
   intercalate e configurazione mancante, senza loggare la chiave.
4. **Parziale - `apps/auth` - adozione delle librerie e credenziali.** Le dipendenze
   condivise e il bridge tra `AuthTenantContext` e `TenantContext` sono presenti.
   Integrare l'attuale `conf/TenantRegistry`, `AuthTenantContext`,
   `TenantResolutionFilter`, `OidcServiceImpl`, `TenantSessionKeyProvider` e
   `UserServiceImpl` con un'unica definizione dei metadata per tenant, senza duplicare
   la risoluzione dell'identita'. Conservare il comportamento AR
   e il rifiuto dei flussi PNPG non abilitati. Portare OneIdentity e User Registry sulla
   configurazione tipizzata per tenant, senza modificare le firme dei client generati.
   Per la PDV confermare prima se URL/istanza e credenziali differiscono tra tenant:
   in tal caso selezionare anche la destinazione per tenant, non solo l'API key
   (SELC-15.1-15.3).
5. **`apps/auth` - dati OTP e chiamate uscenti.** Il routing Mongo flat e' stato
   sostituito per AR; applicare il filtro `tenantId` a tutte le letture,
   aggiornamenti (incluso `mailRequestId`), conteggi/limiti e operazioni per `uuid` di
   `OtpFlow`. Rimuovere la compatibilita' `tenantId` assente solo dopo backfill e
   verifica in tutti gli ambienti. Propagare il tenant verificato nelle chiamate a IAM,
   user-ms, OneMail e negli eventuali payload persistiti; classificare per tenant o
   globali URL, API key, mittente, template, flag OTP e parametri SAML/redirect prima
   di unificare i deployment (SELC-12.4, SELC-16, SELC-17.1).
6. **Infrastruttura e migrazione.** Aggiornare il registro canonico Terraform e gli
   stack `infra/resources/auth` per esporre entrambe le route Mongo, i secret OneIdentity,
   User Registry e le eventuali chiavi di firma/PDV necessarie con nomi per tenant.
   Non serializzare valori riservati nel registry o nello state; configurare accessi
   Key Vault e connettivita' verso ogni dipendenza. Inventariare/backfillare e verificare
   `otpFlows` per account e tenant; revisionare l'indice unico `uuid` e gli altri indici
   rispetto a `(tenantId, ...)` prima di unire i dati. Trasferire ownership degli
   eventuali resource ID a un solo Terraform state prima del cutover.
7. **Verifica e rilascio.** Pubblicare `selfcare-sdk-tenant` 0.4.0 prima del
   deployment di `auth`, che richiede le nuove dimensioni del registry. Aggiungere
   test del registry/provider, del filtro
   User Registry sul client effettivo e dei flussi OIDC, OTP e SAML, inclusi tenant
   assente/sconosciuto/disabilitato, configurazione incompleta, dati cross-tenant e
   richieste AR/PNPG intercalate. Verificare le query su Mongo reale/containerizzato,
   non solo con mock. Eseguire il cutover DEV, UAT e PROD solo dopo i gate di SELC-18,
   con rollback provato e stack legacy disponibili fino alla validazione di entrambi
   i tenant.

## Decisioni bloccanti prima del cutover

- Confermare se e quando `auth` deve gestire login PNPG: oggi usa `HUB_SPID_LOGIN` ed e'
  disabilitato; il routing Step 2 non ne cambia implicitamente il comportamento.
- Definire per AR/PNPG istanza/tenant della Personal Data Vault, URL User Registry,
  eventuale API key distinta e modello credenziali; bloccare il flusso non configurato
  invece di usare la chiave AR per PNPG.
- Classificare SAML, OneMail/OTP (mittente, template, credenziali), IAM/user-ms, audience
  JWT e flag operativi per tenant/ambiente/globali; definire i contratti APIM delle
  chiamate machine-to-machine e la propagazione del tenant.
- Concordare le route Cosmos e l'ownership delle collection OTP per ciascun ambiente,
  insieme alla strategia di indici, backfill, rotazione dei secret e rollback.
