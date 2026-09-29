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
  AR per l'autenticazione e delega la risoluzione delle credenziali OneIdentity al registry
  condiviso; `OidcServiceImpl` usa le credenziali di quel tenant.
  `TenantSessionKeyProvider` seleziona gia' chiave privata e `kid` di sessione per tenant.
  PNPG e' configurato con `HUB_SPID_LOGIN` e `auth_enabled=false`: non attivare OIDC o SAML
  per PNPG per effetto della sola unificazione del deployment.
- `auth` usa `selfcare-sdk-tenant` per risolvere le credenziali User Registry e
  `selfcare-sdk-tenant-mongodb` per selezionare client e database Mongo da
  `TenantContext`. Il filtro di ingresso copia il tenant validato in quel contesto.
  Nel deployment corrente la route Mongo e' configurata solo per AR; PNPG resta
  disabilitato e non eredita la connessione AR.
  Le query e i conteggi OTP usano `tenantId`; gli aggiornamenti richiedono il
  tenant AR. In modalita' transitoria le letture AR possono ancora vedere record
  legacy senza `tenantId`, ma verifica e reinvio li rifiutano prima di modificarli:
  eseguire il backfill **prima** di trasferire il traffico OTP.
- `UserServiceImpl` usa il client generato `user_registry_json` tramite
  `TenantUserRegistryApi`: `TenantUserRegistryApiKeyFilter` seleziona `x-api-key` dal
  registry in base al tenant validato, per ogni richiesta. La vecchia proprieta'
  `quarkus.openapi-generator.user_registry_json.auth.api_key.api-key` non e' piu' usata.
- Il mittente e la chiave OneMail sono associati esplicitamente al tenant AR; `auth`
  riceve `TENANT_AR_MAIL_SENDER` e `TENANT_AR_ONE_MAIL_API_KEY` senza fallback.
  Le destinazioni degli altri client outbound
  sono ancora singole e vanno classificate prima di un eventuale cutover.
- `infra/resources/auth/{dev,uat,prod}-ar/auth.tf` espone un solo JSON canonico per
  tenant: metadati auth e riferimenti alle risorse/credenziali. La route Mongo,
  OneIdentity e OneMail in `auth` sono solo AR; il riferimento User Registry PNPG
  resta nel registro condiviso per gli altri microservizi. Il routing PDV non e'
  stato definito.
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

PNPG richiedera' una voce `mongo` distinta **solo quando usera' `auth`**. Le
dimensioni `oneIdentity` e `userRegistry` sono configurate per tenant e validate come
obbligatorie solo dai servizi/flussi che le usano: non inventare credenziali PNPG per
`auth` mentre `auth_enabled=false`, ma non legare la disponibilita' di `userRegistry`
all'abilitazione di `auth` se altri servizi lo utilizzano. Se il login PNPG verra'
abilitato, definire prima il provider, il contratto PDV e le sue credenziali. Conservare nel registry
Step 0 i campi pubblici (`frontend_uri`, `api_uri`, `allowed_origins`,
`authentication_provider`, `auth_enabled`), ora nello stesso JSON canonico insieme
ai riferimenti alle risorse. `AuthTenantContext` e il `TenantContext` delle
librerie rappresentano lo stesso tenant verificato.

`auth` usa un solo `tenant.registry.json`: Terraform unisce i metadata Step 0,
i riferimenti `tenant_credential_resources` nel modulo `local-env` e la route
Mongo `selcAuth` AR nello stack `auth`. Il registry SDK ignora i metadata auth,
mentre il registry auth ignora i campi risorsa; entrambi leggono la stessa definizione.
`userRegistry` e' presente anche per PNPG nel registro condiviso, affinche' gli
altri microservizi possano configurarne la propria credenziale. `auth` espone
solo il secret AR e `tenant.supported-tenants=AR` limita l'inizializzazione dei
client all'unico tenant oggi abilitato; configurare e validare i secret e la
route Mongo PNPG prima di includere PNPG nel deployment di `auth`.

Le proprieta' locali `tenant.ar.one-identity.client-id` e
`tenant.ar.one-identity.client-secret` sono state rimosse: il registry condiviso risolve
`clientIdEnvVar`/`clientSecretEnvVar` dalle variabili secret-backed senza inserirne i valori
nel JSON. Per User Registry
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
4. **Completato per AR - `apps/auth` - registry e credenziali.** Entrambi i registry
   leggono lo stesso JSON; il contesto auth alimenta quello condiviso. Il login PNPG
   resta disabilitato. Per un'eventuale futura attivazione PNPG, confermare prima
   istanza, URL e credenziali PDV e scegliere la destinazione per tenant (SELC-15).
5. **Implementato per AR, backfill obbligatorio - dati OTP e chiamate uscenti.**
   Letture e conteggi OTP includono il tenant; scritture vincolate a `tenantId=AR`
   non modificano record legacy. `selfcare.tenant.strict-data-isolation` elimina
   la compatibilita' di lettura dopo il backfill; negli stack DEV/UAT/PROD il flag
   resta `false` finche' non sono verificati i dati. Non spostare traffico OTP
   legacy non migrato: verifica e reinvio sono rifiutati, non producono un successo
   apparente. Il tenant e' propagato a IAM/user-ms e la chiave e il mittente
   OneMail sono tenant-bound; un errore di invio fallisce il flusso. URL,
   template, flag OTP e parametri SAML/redirect restano singoli per `auth` AR;
   classificarli prima di qualsiasi ingresso PNPG (SELC-12.4, SELC-16).
6. **Parziale - infrastruttura e migrazione AR.** Gli stack espongono il registry
   canonico, la route Mongo e i secret necessari per AR; PNPG ha solo il riferimento
   User Registry riutilizzabile dagli altri servizi, senza route Mongo o login in
   `auth`. Nessuna migrazione dei dati e' stata eseguita. Inventariare,
   backfillare e verificare `otpFlows` per ambiente con lo script Step 1,
   revisionare gli indici `(tenantId, ...)` prima di unire dati e trasferire
   ownership degli eventuali resource ID a un solo Terraform state prima del
   cutover. Non serializzare valori riservati nel registry.
7. **Gate di verifica e rilascio AR.** Pubblicare `selfcare-sdk-tenant` 0.4.0
   prima del deployment di `auth`. Verificare le query su Mongo reale/containerizzato,
   il backfill e i flussi OIDC/OTP/SAML sui dati d'ambiente, oltre ai test unitari.
   Confermare il binding APIM subscription-to-tenant, il contratto PDV e la
   connettivita' Key Vault: il repository non consente di accertarli. Abilitare
   strict mode solo quando **tutti** i servizi tenant-owned dell'ambiente usano
   il medesimo flag e i dati sono verificati; non attribuire ad `auth` un
   isolamento strict dell'intera piattaforma. Cutover DEV/UAT/PROD e rollback
   vanno provati nell'ambiente, mantenendo disponibili gli stack legacy (SELC-18).

## Decisioni bloccanti prima del cutover

- PNPG non usa ancora `auth`: `HUB_SPID_LOGIN` resta disabilitato; un futuro login
  richiedera' un progetto e un cutover separati.
- Definire per AR/PNPG istanza/tenant della Personal Data Vault, URL User Registry,
  eventuale API key distinta e modello credenziali; bloccare il flusso non configurato
  invece di usare la chiave AR per PNPG.
- Classificare SAML, OneMail/OTP (mittente, template, credenziali), IAM/user-ms, audience
  JWT e flag operativi per tenant/ambiente/globali; definire i contratti APIM delle
  chiamate machine-to-machine e la propagazione del tenant.
- Concordare le route Cosmos e l'ownership delle collection OTP per ciascun ambiente,
  insieme alla strategia di indici, backfill, rotazione dei secret e rollback.
