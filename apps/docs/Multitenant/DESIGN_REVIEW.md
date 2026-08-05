# Design Review — Multitenant Architecture (Selfcare)

> Basato sul template Confluence [Template DR](https://pagopa.atlassian.net/wiki/spaces/~693876898/pages/2019000457/Template+DR).
> Fonti: `apps/docs/Multitenant/Step_0/{REQUIREMENTS,ARCHITECTURE,SECURITY,EPIC}.md`,
> `apps/docs/Multitenant/Step_1/{REQUIREMENTS,ARCHITECTURE,SECURITY,EPIC}.md`.

| **Team** | TO BE DECIDED — team Selfcare responsabile dei microservizi in `apps/` (non specificato nei documenti sorgente) |
| --- | --- |
| **Prodotto** | Selfcare — microservizi backend condivisi tra `selfcare.pagopa.it` (AR) e `imprese.notifichedigitali.it` (PNPG) |
| **Initiative canvas** | TO BE DECIDED — nessun link fornito nei documenti sorgente |

# L'esigenza

Oggi la piattaforma Selfcare è deployata su due Azure Container App Environment separati, uno per
`selfcare.pagopa.it` e uno per `imprese.notifichedigitali.it`, ciascuno con frontend, routing APIM e
autenticazione dedicati (OneIdentity → `auth` per il primo, `hub-spid-login` per il secondo). Questo duplica
l'infrastruttura, impedisce di identificare il tenant in modo uniforme a runtime e non fornisce oggi un modo
coerente per isolare i dati (Cosmos DB, Azure Storage, vault dati personali, email) quando le due utenze
condividono lo stesso backend (`Step_0/REQUIREMENTS.md` "Project purpose"; `Step_1/REQUIREMENTS.md` "Project
purpose").

# L'esito atteso dell'iniziativa

Un singolo deployment backend per ambiente (dev/uat/prod) servirà entrambi i tenant. Ogni richiesta porterà
un'identità di tenant verificabile sia come header HTTP (`X-Tenant-Id`) sia come claim JWT, riconciliati e mai
derivati in modo indipendente a valle (SELC-1, SELC-2, `Step_0/ARCHITECTURE.md`). A livello dati, ogni
microservizio isolerà esplicitamente le informazioni del tenant in Cosmos DB, Azure Storage, vault dati
personali ed email in uscita, con comportamento fail-closed su ogni tenant non risolto (SELC-8..SELC-11,
`Step_1/ARCHITECTURE.md`). Le due infrastrutture Terraform (`-ar`/`-pnpg`) verranno consolidate tramite una
migrazione parallel-run, senza downtime, con dismissione del legacy solo dopo validazione in produzione
(`Step_0/ARCHITECTURE.md` "Deployment model").

---

# Attori del sistema

1. **USER**: utente finale autenticato tramite OneIdentity (`selfcare.pagopa.it`) o SPID via `hub-spid-login`
   (`imprese.notifichedigitali.it`).
2. **APIM**: Azure API Management, gateway condiviso che risolve il tenant dall'header `Host` e inietta/
   sovrascrive `X-Tenant-Id` prima di inoltrare al backend (SELC-5).
3. **`auth`**: microservizio interno che emette il JWT per il flusso OneIdentity, aggiungendo il claim tenant
   (SELC-4).
4. **`hub-spid-login`**: microservizio esterno "black box" che emette JWT per il flusso SPID; richiede uno
   strato di injection del claim tenant, meccanismo `TO BE DECIDED` (SELC-3).
5. **Backend microservices** (`apps/*`): consumano `X-Tenant-Id` e claim JWT già validati, applicano
   enforcement e isolamento dati (SELC-7, SELC-8..SELC-11).
6. **Cosmos DB / Azure Storage / Personal Data Vault / Email provider**: sistemi a valle su cui viene applicato
   l'isolamento per tenant.

# Casi d'uso

## Must have

1. Come **USER**, quando accedo tramite OneIdentity o SPID, voglio che la mia sessione porti un'identità di
   tenant coerente, in modo che tutte le chiamate successive vengano instradate e autorizzate correttamente
   (SELC-1, SELC-2, SELC-4).
2. Come **APIM**, quando ricevo una richiesta su uno dei due hostname noti, voglio risolvere il tenant
   dall'header `Host` e sovrascrivere `X-Tenant-Id`, in modo che il backend condiviso non si fidi mai di un
   header fornito dal client (SELC-5, anti-spoofing — `Step_0/SECURITY.md`).
3. Come **backend microservice**, quando ricevo una richiesta autenticata, voglio riconciliare l'header
   `X-Tenant-Id` con il claim JWT (default a `PNPG` solo per token `hub-spid-login` privi di claim), in modo da
   rifiutare ogni richiesta inconsistente (SELC-2.3, SELC-3.1, SELC-3.4).
4. Come **backend microservice**, quando accedo a Cosmos DB, voglio applicare il modello di isolamento scelto
   (discriminator field o database-per-tenant) usando solo il tenant già validato, in modo da non restituire mai
   dati di un altro tenant (SELC-8).
5. Come **backend microservice**, quando accedo a Azure Storage, voglio risolvere container/account solo dal
   tenant validato, in modo da non esporre blob di un altro tenant (SELC-9).
6. Come **backend microservice**, quando interrogo il vault dati personali, voglio selezionare l'istanza/tenant
   corretta, in modo da non leggere/scrivere PII di un altro tenant (SELC-10).
7. Come **backend microservice**, quando invio una email, voglio usare il dominio mittente del tenant corretto,
   in modo da non inviare comunicazioni con l'identità sbagliata (SELC-11).
8. Come **piattaforma**, quando eseguo la migrazione, voglio un rollout parallel-run (nuovo stack accanto al
   legacy, cutover solo a livello APIM), in modo da garantire zero downtime e un rollback sicuro
   (`Step_0/ARCHITECTURE.md` "Migration Strategy").

## Nice to have

1. Come **piattaforma**, voglio poter onboardare un nuovo tenant senza modificare il codice backend, in modo da
   estendere la piattaforma con costo marginale (SELC-6.2).
2. Come **operatore**, voglio un audit log esplicito per ogni fallback (`hub-spid-login` → `PNPG`) e per ogni
   rigetto per tenant sconosciuto, in modo da poter investigare anomalie (`Step_0/SECURITY.md`,
   `Step_1/SECURITY.md` — Logging & error handling).

# Vincoli normativi

TO BE DECIDED — `Step_0/REQUIREMENTS.md` e `Step_1/REQUIREMENTS.md` marcano esplicitamente questo punto come
aperto. Possibili aree da verificare: eventuali differenze di trattamento dati tra il flusso SPID
(`imprese.notifichedigitali.it`) e OneIdentity (`selfcare.pagopa.it`), vincoli di data residency per Cosmos DB/
Storage/vault per tenant.

# Caratteristiche del sistema

**Scalabilità** — Il backend condiviso deve assorbire il traffico aggregato delle due utenze legacy senza
degrado prestazionale; i numeri esatti derivano dalle analytics APIM pre-migrazione (`Step_0/ARCHITECTURE.md`
"Scale expectations" — combined baseline). Lo scaling è delegato ad Azure Container Apps (KEDA), con trigger
basati su richieste HTTP concorrenti piuttosto che soglie CPU/memoria, coerentemente con lo stack reattivo
Quarkus/Mutiny.

**Elasticità** — I repliche minime devono garantire alta disponibilità (es. ≥2 in produzione); le repliche
massime devono essere limitate per evitare l'esaurimento del connection pool verso MongoDB/Cosmos DB condiviso
(`Step_0/ARCHITECTURE.md` "Replica Bounds"). Il rate limiting per tenant a livello APIM previene il fenomeno
"noisy neighbor" in cui un tenant monopolizza le risorse condivise (SELC-5, `Step_0/ARCHITECTURE.md` "Tenant
Protection").

**Extensibility** — La strategia di risoluzione tenant deve coprire almeno i due tenant noti (AR, PNPG) ed
essere estendibile a nuovi tenant senza modifiche al codice di risoluzione (SELC-6.2); analogamente,
l'inventario dei modelli di isolamento dati (Cosmos DB, Storage) deve poter accogliere nuovi
tenant/microservizi senza ridisegnare l'architettura (SELC-8.6, SELC-9.6).

**Recoverability** — Ogni componente di risoluzione/isolamento tenant deve fallire in modo esplicito
("fail-closed") quando il tenant non è risolvibile, piuttosto che degradare silenziosamente su un tenant di
default (SELC-1.3, SELC-3.1/3.4, SELC-8.4, SELC-9.4, SELC-10.2, SELC-11.2). La migrazione parallel-run consente
un rollback sicuro mantenendo il legacy attivo fino a validazione in produzione.

## Service Level Objective

TO BE DECIDED — nessuna soglia numerica di disponibilità/latenza è definita nei documenti sorgente. Le baseline
di traffico (SLA per metrica di input) dipendono dalle analytics APIM pre-migrazione, non ancora raccolte
(`Step_0/ARCHITECTURE.md` "Scale expectations").

# Make or buy

TO BE DECIDED — nessuna valutazione di soluzioni "as-is"/"off the shelf" è documentata nei sorgenti (es. per il
vault dati personali, il cui provider è ancora `TO BE DECIDED` in `Step_1/ARCHITECTURE.md`).

---

# Design di alto livello

## Servizi cloud

| Cloud provider | Microsoft Azure (Azure Container Apps, Azure API Management, Azure Cosmos DB, Azure Storage, Azure Key Vault) — grounded in `infra/core/_modules/*`, `infra/resources/_modules/*` |
| --- | --- |
| Data classification | TO BE DECIDED — include PII gestita dal "Personal Data Vault" (riferimenti in `apps/onboarding-ms`, `apps/auth`); livello di classificazione non specificato nei documenti sorgente |
| DNS esposti | `selfcare.pagopa.it`, `imprese.notifichedigitali.it` (frontend); dominio APIM interno `${azurerm_api_management_custom_domain.api_custom_domain...}` (`infra/core/_modules/apim/variables.tf`) |
| Region | `westeurope` (rilevato da `infra/resources/document-ms/dev-ar/main.tf`: `whitemoss-eb7ef327.westeurope.azurecontainerapps.io`) |
| Subscription | TO BE DECIDED — ambienti noti: dev/uat/prod, per ciascuno stack `-ar`/`-pnpg` in consolidamento (`infra/resources/<app>/{dev,uat,prod}-{ar,pnpg}`) |

## Vista statica delle componenti

### Diagramma delle dipendenze (System Context)

Upstream: frontend React di `selfcare.pagopa.it` e `imprese.notifichedigitali.it` (client framework, per
`Step_0/ARCHITECTURE.md`). Downstream: OneIdentity (IdP esterno), `hub-spid-login` (issuer di token SPID
esterno/black-box), Azure Cosmos DB, Azure Storage, Personal Data Vault (provider `TO BE DECIDED`), provider
email/SMTP.

```plantuml
@startuml selfcare-multitenant-system-context
!theme plain

title Selfcare Multitenant - System Context

left to right direction
skinparam shadowing false
skinparam componentStyle rectangle
skinparam wrapWidth 220
skinparam maxMessageSize 120

actor "AR User" as ar_user
actor "PNPG User" as pnpg_user

cloud "AR Frontend\nselfcare.pagopa.it" as ar_frontend
cloud "PNPG Frontend\nimprese.notifichedigitali.it" as pnpg_frontend

cloud "OneIdentity\nExternal Identity Provider" as one_identity
cloud "hub-spid-login\nExternal SPID token issuer" as hub_spid

rectangle "Selfcare Multitenant Platform\n\nShared APIM, auth and backend microservices.\nResolves and validates the tenant identity,\nthen enforces tenant isolation fail-closed." as selfcare

database "Azure Cosmos DB\nTenant-isolated data" as cosmos
storage "Azure Storage\nTenant-isolated objects" as storage
rectangle "Personal Data Vault\nTenant-isolated PII\nProvider: TO BE DECIDED" as vault
cloud "Email / SMTP Provider\nTenant-specific sender domain" as email

ar_user --> ar_frontend : Uses
ar_frontend --> one_identity : Authenticates
one_identity --> selfcare : Returns verified identity\nto the auth flow
selfcare --> ar_frontend : Issues tenant-aware JWT
ar_frontend --> selfcare : HTTPS requests\nwith JWT

pnpg_user --> pnpg_frontend : Uses
pnpg_frontend --> hub_spid : Authenticates via SPID
hub_spid --> pnpg_frontend : Issues SPID JWT
pnpg_frontend --> selfcare : HTTPS requests\nwith JWT

selfcare --> cosmos : Reads and writes data\nusing validated tenant
selfcare --> storage : Reads and writes objects\nusing validated tenant
selfcare --> vault : Reads and writes PII\nusing validated tenant
selfcare --> email : Sends email using\ntenant identity

note bottom of selfcare
  APIM never trusts a client-provided tenant header.
  Unknown tenants and header/claim mismatches are rejected.
  The temporary hub-spid-login fallback to PNPG is audited.
end note

@enduml
```

### Diagramma delle componenti (Container Diagram)

Componenti applicative note:
- **APIM**: risolve tenant da `Host`, inietta `X-Tenant-Id`, applica rate limiting per tenant. API: nessuna
  esposta direttamente, agisce da reverse proxy/gateway.
- **`auth`** (Quarkus): emette JWT con claim tenant per il flusso OneIdentity. Owner dei dati: sessione/claims.
  Attori: USER via OneIdentity.
- **`hub-spid-login`** + strato di injection (da progettare): emette/arricchisce JWT per il flusso SPID. Owner
  dati: sessione/claims. Attori: USER via SPID.
- **Backend microservices** (`apps/*`, Quarkus reattivo): enforcement tenant, isolamento dati Cosmos DB/
  Storage/vault/email. Specifiche OpenAPI: `src/main/docs/` per app (convenzione di repository).
- **Cosmos DB** (Mongo API): datastore primario, isolamento via discriminator field o database-per-tenant
  (SELC-8).
- **Azure Storage**: blob/file, isolamento via container o account per tenant (SELC-9).
- **Personal Data Vault**: PII, selezione istanza per tenant (SELC-10); provider `TO BE DECIDED`.
- **Email/SMTP provider**: invio tenant-aware (SELC-11); servizio noto: `institution-send-mail-scheduler`.

```plantuml
@startuml selfcare-multitenant-container-diagram
!theme plain

title Selfcare Multitenant - Container Diagram

left to right direction
skinparam shadowing false
skinparam componentStyle rectangle
skinparam wrapWidth 220
skinparam maxMessageSize 120

actor "AR User" as ar_user
actor "PNPG User" as pnpg_user

cloud "OneIdentity\nExternal Identity Provider" as one_identity
cloud "hub-spid-login\nExternal SPID token issuer" as hub_spid
rectangle "Personal Data Vault\nProvider: TO BE DECIDED" as vault
cloud "Email / SMTP Provider" as email

node "Frontend Applications" {
  component "AR Frontend\nReact\nselfcare.pagopa.it" as ar_frontend
  component "PNPG Frontend\nReact\nimprese.notifichedigitali.it" as pnpg_frontend
}

rectangle "Selfcare Multitenant Backend" {
  component "Azure API Management\n\nTrusted ingress gateway.\nResolves tenant from request context,\noverwrites X-Tenant-Id and applies\ntenant-aware rate limiting." as apim

  component "auth\nQuarkus\n\nCompletes the OneIdentity flow and\nissues JWTs with the tenant claim." as auth

  component "SPID Tenant Claim Injection\nMechanism: TO BE DECIDED\n\nAdds the trusted PNPG tenant identity\nwithout weakening SPID validation." as spid_injector

  component "Backend Microservices\nQuarkus / Mutiny - apps/*\n\nReconcile JWT claim and X-Tenant-Id,\nexecute business logic and enforce\ntenant-aware data access fail-closed." as backend

  component "institution-send-mail-scheduler\nQuarkus\n\nSelects tenant-specific sender identity\nfor queued notifications." as scheduler

  database "Azure Cosmos DB\nMongo API\n\nDiscriminator field or\ndatabase per tenant" as cosmos

  storage "Azure Storage\n\nContainer or account\nper tenant" as storage
}

ar_user --> ar_frontend : Uses
pnpg_user --> pnpg_frontend : Uses

ar_frontend --> one_identity : Starts authentication
one_identity --> auth : Returns verified identity
auth --> ar_frontend : Returns tenant-aware JWT

pnpg_frontend --> hub_spid : Starts SPID authentication
hub_spid --> pnpg_frontend : Returns SPID JWT

ar_frontend --> apim : HTTPS API requests\nwith JWT
pnpg_frontend --> apim : HTTPS API requests\nwith JWT

apim --> spid_injector : Routes eligible SPID tokens\nfor tenant claim injection
spid_injector --> backend : JWT with trusted tenant claim\nand X-Tenant-Id
apim --> backend : JWT and overwritten\nX-Tenant-Id

backend --> cosmos : Tenant-scoped queries\nand writes
backend --> storage : Tenant-scoped object\naccess
backend --> vault : Tenant-scoped PII\naccess
backend --> scheduler : Queues tenant-aware\nemail notifications
scheduler --> email : Sends with tenant-specific\ndomain and credentials

note bottom of apim
  Unknown hosts, tenants and caller mappings are rejected.
  Client-provided X-Tenant-Id values are never trusted.
end note

note bottom of backend
  The validated tenant context is the only source used
  to select Cosmos DB, Storage, vault and email resources.
end note

@enduml
```

### Diagramma dell'architettura (Deployment Diagram)

Nodi infrastrutturali noti: Azure API Management (gateway), Azure Container App Environment condiviso
(`infra/core/_modules/container_app_environments`), Azure Cosmos DB (Mongo API), Azure Storage Account(s) per
app, Azure Key Vault (secret/cert storage), DNS pubblico/privato (`infra/core/_modules/dns_public`,
`dns_private`). Stato attuale: due stack paralleli `-ar`/`-pnpg` per app/ambiente; stato target: uno stack
condiviso per ambiente, con i due legacy dismessi post-validazione (`Step_0/ARCHITECTURE.md` "Deployment
model").

```plantuml
@startuml selfcare-multitenant-deployment-diagram
!theme plain

title Selfcare Multitenant - Deployment Diagram

left to right direction
skinparam shadowing false
skinparam componentStyle rectangle
skinparam wrapWidth 220
skinparam maxMessageSize 120

actor "AR User" as ar_user
actor "PNPG User" as pnpg_user

cloud "Public Internet" {
  node "Public DNS" as public_dns {
    artifact "selfcare.pagopa.it" as ar_dns
    artifact "imprese.notifichedigitali.it" as pnpg_dns
  }
}

cloud "Microsoft Azure - Environment: dev / uat / prod" as azure {
  node "Frontend Hosting" as frontend_hosting {
    artifact "AR React frontend" as ar_frontend
    artifact "PNPG React frontend" as pnpg_frontend
  }

  node "Azure API Management" as apim {
    component "AR API routes" as ar_routes
    component "PNPG API routes" as pnpg_routes
    component "Tenant resolution policies\nHost / subscription mapping\nX-Tenant-Id overwrite" as policies
  }

  node "Target Shared Stack" as target {
    node "Azure Container Apps Environment" as cae {
      component "auth" as auth
      component "Shared backend microservices\napps/*" as backend
      component "institution-send-mail-scheduler" as scheduler
    }

    node "Azure Key Vault" as key_vault {
      artifact "JWT keys and certificates" as jwt_secrets
      artifact "Cosmos, Storage and email secrets" as service_secrets
    }

    database "Azure Cosmos DB\nMongo API" as cosmos
    storage "Azure Storage Accounts" as storage
    node "Private DNS / Network Integration" as private_network
  }

  frame "Parallel-run migration only" as migration {
    node "Legacy AR Stack\n*-ar" as legacy_ar {
      component "AR Container Apps" as legacy_ar_apps
      database "AR data resources" as legacy_ar_data
    }

    node "Legacy PNPG Stack\n*-pnpg" as legacy_pnpg {
      component "PNPG Container Apps" as legacy_pnpg_apps
      database "PNPG data resources" as legacy_pnpg_data
    }
  }
}

cloud "External Services" {
  component "OneIdentity" as one_identity
  component "hub-spid-login" as hub_spid
  component "Personal Data Vault\nTO BE DECIDED" as vault
  component "Email / SMTP Provider" as email
}

ar_user --> ar_dns : HTTPS
pnpg_user --> pnpg_dns : HTTPS
ar_dns --> ar_frontend : Resolves frontend endpoint
pnpg_dns --> pnpg_frontend : Resolves frontend endpoint
ar_frontend --> ar_routes : HTTPS API requests
pnpg_frontend --> pnpg_routes : HTTPS API requests

ar_routes --> policies
pnpg_routes --> policies
policies --> auth : OneIdentity login flow
policies --> backend : Tenant-aware API traffic

auth --> one_identity : Authentication
pnpg_frontend --> hub_spid : SPID authentication
hub_spid --> pnpg_frontend : Issues SPID JWT

auth --> key_vault : Reads signing material
backend --> key_vault : Reads service credentials
scheduler --> key_vault : Reads tenant email credentials

backend --> cosmos : Tenant-scoped data access
backend --> storage : Tenant-scoped object access
backend --> vault : Tenant-scoped PII access
scheduler --> email : Tenant-specific sender

cae --> private_network : Private connectivity
private_network --> cosmos
private_network --> storage
private_network --> key_vault

policies ..> legacy_ar_apps : Temporary rollback route
policies ..> legacy_pnpg_apps : Temporary rollback route
legacy_ar_apps --> legacy_ar_data
legacy_pnpg_apps --> legacy_pnpg_data

note bottom of migration
  The shared stack runs beside both legacy stacks.
  APIM performs the cutover; legacy resources remain available
  until production validation succeeds, then are decommissioned.
end note

note bottom of target
  One shared deployment exists per environment.
  Tenant isolation is preserved in application policies,
  data access, storage resolution, secrets and email identity.
end note

@enduml
```

## Vista dinamica delle componenti

Flusso principale (successo): USER → login (OneIdentity/`auth` o `hub-spid-login`) → JWT con claim tenant → USER
chiama frontend → richiesta HTTP verso APIM → APIM risolve tenant da `Host`, sovrascrive `X-Tenant-Id` → backend
riconcilia header/claim → backend applica isolamento dati (Cosmos DB/Storage/vault/email) usando il tenant
validato → risposta.

Flusso di fallimento: tenant non risolvibile da APIM (host sconosciuto) → rigetto esplicito, nessun default
silenzioso (SELC-1.3); claim JWT assente per token non `hub-spid-login`, o mismatch header/claim → rigetto
(SELC-2.3); tenant non mappato a Cosmos DB/Storage/vault/dominio email → fail-closed, richiesta/operazione
rigettata (SELC-8.4, SELC-9.4, SELC-10.2, SELC-11.2).

```plantuml
@startuml selfcare-multitenant-dynamic-view
!theme plain

title Selfcare Multitenant - Dynamic Component View

autonumber
hide footbox
skinparam shadowing false
skinparam wrapWidth 220
skinparam maxMessageSize 120

actor User
participant "AR / PNPG\nReact Frontend" as frontend
participant "OneIdentity" as one_identity
participant "auth" as auth
participant "hub-spid-login" as hub_spid
participant "SPID Tenant Claim Injector\nMechanism: TO BE DECIDED" as spid_injector
participant "Azure API Management" as apim
participant "Backend Microservice" as backend
database "Azure Cosmos DB" as cosmos
collections "Azure Storage" as storage
participant "Personal Data Vault" as vault
participant "Email Scheduler" as scheduler
participant "Email / SMTP Provider" as email

group UC-1 - Establish a tenant-aware authenticated session
  User -> frontend : Selects the AR or PNPG service

  alt AR authentication through OneIdentity
    frontend -> auth : Starts login using the AR hostname
    auth -> one_identity : Requests authentication
    one_identity --> auth : Returns verified user identity
    auth -> auth : Resolves AR tenant from trusted context
    auth --> frontend : Returns signed JWT with tenant claim = AR
  else PNPG authentication through SPID
    frontend -> hub_spid : Starts SPID authentication
    hub_spid --> spid_injector : Returns signed SPID JWT
    spid_injector -> spid_injector : Validates the SPID token and\nassociates trusted PNPG identity
    spid_injector --> frontend : Returns tenant-aware JWT
  end
end

group UC-2 - Resolve and propagate tenant identity at ingress
  User -> frontend : Performs an authenticated operation
  frontend -> apim : HTTPS request with JWT\nand optional client X-Tenant-Id
  apim -> apim : Resolves tenant from trusted host,\nsubscription and operation mapping

  alt Tenant cannot be resolved
    break Request rejected before reaching the backend
      apim --> frontend : 403 - unknown tenant
      frontend --> User : Displays access error
    end
  else Tenant resolved
    apim -> apim : Overwrites X-Tenant-Id\nwith the trusted tenant
    apim -> backend : JWT + trusted X-Tenant-Id
  end
end

group UC-3 - Reconcile tenant identities and authorize processing
  backend -> backend : Validates JWT signature and issuer
  backend -> backend : Reconciles JWT tenant claim\nwith X-Tenant-Id

  alt Claim is missing and issuer is hub-spid-login
    backend -> backend : Applies audited temporary fallback\nto PNPG
  else Claim/header mismatch or unsupported missing claim
    break Request rejected before business processing
      backend --> apim : 401/403 - tenant identity rejected
      apim --> frontend : Error response
      frontend --> User : Displays access error
    end
  else Tenant identity is consistent
    backend -> backend : Creates validated tenant context
  end
end

group UC-4 - Execute tenant-isolated business operations
  alt Read or write domain data
    backend -> cosmos : Query/write with tenant discriminator\nor tenant-specific database
    cosmos --> backend : Tenant-scoped result
  else Read or write documents
    backend -> storage : Resolve tenant container/account\nand access object
    storage --> backend : Tenant-scoped object/result
  else Read or write personal data
    backend -> vault : Resolve tenant vault instance\nand access PII
    vault --> backend : Tenant-scoped PII/result
  end

  alt Tenant resource mapping exists
    backend --> apim : Successful business response
    apim --> frontend : Successful response
    frontend --> User : Displays result
  else Tenant resource mapping is missing
    backend --> apim : Fail-closed error
    apim --> frontend : Error response
    frontend --> User : Displays operation error
  end
end

group UC-5 - Send a tenant-aware email when required
  opt Business operation produces an email notification
    backend -> scheduler : Queues notification with validated tenant
    scheduler -> scheduler : Selects tenant sender domain\nand credentials

    alt Email tenant mapping exists
      scheduler -> email : Sends message with tenant identity
      email --> scheduler : Delivery result
    else Email tenant mapping is missing
      scheduler -> scheduler : Rejects delivery and emits audit event
    end
  end
end

@enduml
```

Il diagramma rappresenta questi casi d'uso dinamici:

1. **Sessione autenticata tenant-aware** — l'utente accede tramite OneIdentity per AR oppure SPID per PNPG e
   ottiene un JWT associato al tenant corretto.
2. **Risoluzione tenant all'ingresso** — APIM deriva il tenant solo da informazioni fidate, sovrascrive
   `X-Tenant-Id` e rigetta richieste non attribuibili a un tenant noto.
3. **Enforcement nel backend** — il microservizio valida firma e issuer del JWT, riconcilia claim e header e
   crea il contesto tenant; mismatch e claim mancanti non supportati vengono rifiutati.
4. **Accesso isolato ai dati** — Cosmos DB, Azure Storage e Personal Data Vault sono selezionati o filtrati
   esclusivamente tramite il tenant validato; una configurazione mancante produce un errore fail-closed.
5. **Email tenant-aware** — la notifica mantiene il tenant fino allo scheduler, che seleziona dominio mittente
   e credenziali corretti oppure blocca l'invio e genera un evento di audit.

---

# Data layer

Entità logiche note (dettaglio completo in `Step_0/REQUIREMENTS.md` "Business objects / data entities" e
`Step_1/REQUIREMENTS.md` "Business objects / data entities"):

- **Tenant registry**: elenco canonico dei tenant (`default`/AR, `PNPG`) e mapping host/path → tenant (SELC-6.3);
  meccanismo di storage `TO BE DECIDED`, candidato: APIM Named Values gestite via Terraform
  (`Step_0/ARCHITECTURE.md`).
- **JWT session token**: claim tenant, nome/formato `TO BE DECIDED` (Open Question `Step_0`).
- **`X-Tenant-Id` HTTP header**: propagato su ogni richiesta e chiamata service-to-service (SELC-1.2).
- **Cosmos DB documents**: campo discriminatore `tenantId` (modello logico) oppure database dedicato per
  tenant (modello fisico) — scelta per microservizio ancora `TO BE DECIDED` (SELC-8.6).
- **Azure Storage objects**: container per tenant in account condiviso, oppure account dedicato per tenant —
  scelta per microservizio ancora `TO BE DECIDED` (SELC-9.6).
- **Personal Data Vault tenant mapping**: provider e contratto API `TO BE DECIDED` (SELC-10.3); consumatori noti:
  `onboarding-ms`, `auth`.
- **Email sender domain mapping**: per tenant, consumatore noto `institution-send-mail-scheduler`; elenco
  completo dei microservizi che inviano email `TO BE DECIDED` (SELC-11.3).

Non è ancora presente un formato Data Contract/JSON-Schema versionato per queste entità nel repository; si
raccomanda di completarlo prima dell'implementazione, secondo la convenzione richiamata dal template DR.

## Registro `tenant_data_isolation` e naming delle variabili per-tenant

Il mapping dati per tenant sopra descritto (Cosmos DB, Storage, vault, email) è **proposto** come sorgente
unica di verità in `local.tenant_data_isolation` (`infra/resources/_modules/local-env/locals.tf`,
Step_1/EPIC.md sub-task 9), da consumare nei microservizi tramite un'unica variabile d'ambiente JSON
(`SELFCARE_TENANT_DATA_ISOLATION`) e una classe `TenantDataIsolationRegistry`
(`libs/selfcare-sdk-security`), con lookup fail-closed su un tenant già validato a monte.

> **Stato reale (verificato).** Né `local.tenant_data_isolation` in Terraform né
> `TenantDataIsolationRegistry` / `SELFCARE_TENANT_DATA_ISOLATION` in Java esistono oggi nel repository: un
> grep sull'intero monorepo li trova solo in questi documenti. Vanno letti come **design proposto, non
> implementato**; gli snippet più sotto sono stati riscritti per non dipenderne.

**Gap identificato in fase di review** (`infra/resources/onboarding-ms/dev-pnpg/onboarding.tf`): oggi
`MONGODB-CONNECTION-STRING` e `AZURE_STORAGE_ACCOUNT_NAME` sono dichiarate con lo **stesso nome letterale**
negli stack `-ar` e `-pnpg`, ciascuna puntata a un valore diverso solo perché i due stack sono deployment
separati. Nel momento in cui `infra/resources/<app>/{dev,uat,prod}-ar`/`-pnpg` confluiranno in un unico
stack (Step_0/EPIC.md sub-task 7), una singola container app non può portare due secret/app setting sotto
lo stesso nome: ogni variabile di questo tipo deve diventare **una variabile per tenant** prima che lo
stack che la dichiara venga consolidato.

Pattern raccomandato — generalizzare il registro già costruito per il sub-task 9 invece di introdurne uno
parallelo:

1. **Terraform**: derivare i nomi dei secret/app setting per tenant da `module.local.config.tenant_data_isolation`
   con un'espressione `for`/`merge` (una voce per chiave tenant), anziché un nome scritto a mano per servizio.
   Il registro anticipa già la collisione nel Key Vault: `cosmos_connection_string_secret_name` è
   `mongodb-connection-string` per AR ma `mongodb-connection-string-pnpg` per PNPG, proprio perché entrambi
   dovranno coesistere nello stesso vault post-consolidamento. Il nome dell'account di storage non richiede
   nemmeno un secret: è già derivabile per tenant da `storage_account_infix`.
2. **Codice applicativo** (parte che Terraform da solo non risolve): sostituire la singola proprietà
   `quarkus.mongodb.connection-string` / il singolo account-name di storage con un **client selezionato per
   tenant a runtime dal `TenantId` già risolto** (`TenantContext`), non con un fetch del secret da Key Vault
   ad ogni richiesta (evita di aggiungere una dipendenza/latenza Key Vault nel percorso di richiesta, e i
   tenant noti fail-closed sono solo due):
   - Mongo: `MongoDatabaseResolver` nativo di Panache per il caso *database separati sullo stesso account*
     (assetto target), oppure *named client* Quarkus (`quarkus.mongodb.ar.connection-string` /
     `quarkus.mongodb.pnpg.connection-string`) risolti via `@MongoClientName` per il caso *account
     fisicamente separati* — vedi la sezione di dispatch runtime più sotto per il dettaglio verificato.
   - Storage: una `EnumMap<TenantId, BlobServiceClient>` costruita una volta all'avvio dal nome
     account/credenziali per tenant.

Questo è lavoro applicativo, non solo Terraform, e appartiene alla conversione di consolidamento di ogni
singola app (Step_0/EPIC.md sub-task 7), non al registro condiviso: è tracciato qui come prerequisito
esplicito perché non venga riscoperto app per app.

### Meccanismo di dispatch runtime per i named client Mongo

Il tenant è già risolto **prima** che un repository venga invocato: `TenantValidationFilter` valorizza
`TenantContext` (`@RequestScoped`) all'ingresso della request; ogni repository legge da lì tramite
`CurrentTenantProvider.currentTenantId()`, mai dall'header grezzo (stesso principio già in uso nel modello
discriminator-field di `document-ms`).

> **Correzione (verificata via bytecode).** Una precedente stesura di questa sezione proponeva di
> sovrascrivere `mongoDatabase()` sul repository. **Quel meccanismo non funziona** e non va usato.
> Nell'interfaccia `ReactivePanacheMongoRepositoryBase` i corpi di `find`, `findById`, `mongoCollection` e
> `mongoDatabase` lanciano tutti `implementationInjectionMissing()`: sono segnaposto sostituiti a build-time
> dall'enhancement Panache con chiamate **statiche** a `ReactiveMongoOperations`, che passano `entityClass`,
> non `this`. La catena reale è `find(...)` → `ReactiveMongoOperations.mongoCollection(Class)` →
> `mongoDatabase(Class)` → lettura dell'annotazione `@MongoEntity` → `mongoDatabase(MongoEntity)` →
> `BeanUtils.clientFromArc(...)`. Un override di `mongoDatabase()` sul repository non viene quindi mai
> invocato da `find`/`persist`/`update`: verrebbe eseguito solo se chiamato a mano, dando la falsa
> impressione di isolamento.

Il punto di innesto corretto e supportato è invece `io.quarkus.mongodb.panache.common.MongoDatabaseResolver`
(interfaccia con un solo metodo `String resolve()`, presente sia in Quarkus 3.11.2 sia in 3.31.2). In
`mongoDatabase(MongoEntity)` Panache usa `MongoEntity.database()` se valorizzata, **altrimenti** invoca
`BeanUtils.getDatabaseNameFromResolver()`, che risolve il bean CDI a ogni chiamata. Nel monorepo nessuna
entity valorizza `database=` né `clientName=` (tutte usano solo `collection=`), quindi il resolver è
applicabile senza modificare le entity:

```java
@ApplicationScoped
public class TenantMongoDatabaseResolver implements MongoDatabaseResolver {

  @Inject CurrentTenantProvider currentTenantProvider;

  @Override
  public String resolve() {
    return currentTenantProvider.currentTenantId()
        .flatMap(TenantId::fromValue)
        .map(t -> t == TenantId.PNPG ? "selcOnboardingPnpg" : "selcOnboarding")
        .orElseThrow(UnresolvedTenantException::new); // fail-closed, nessun default silenzioso
  }
}
```

Poiché il resolver è interrogato a ogni operazione, un unico bean `@ApplicationScoped` (nessuna sottoclasse
per tenant, nessuna repository factory) serve correttamente richieste AR/PNPG interlacciate sullo stesso
processo, e **nessun repository va toccato**.

**Limite da conoscere:** il resolver seleziona solo il *nome del database*; il `ReactiveMongoClient` resta
quello risolto da `BeanUtils.clientFromArc(...)` a partire da `@MongoEntity.clientName` (valore statico di
compilazione). Il resolver copre quindi il caso "database separati sullo stesso account Cosmos" — che è
l'assetto target del consolidamento — ma **non** il caso "due account Cosmos fisicamente distinti". Per
quest'ultimo, finché dura la migrazione, `@MongoClientName` va usato su un client iniettato e usato
direttamente (fuori da Panache), oppure va mantenuto un deployment per account.
Verificato che `io.quarkus.mongodb.MongoClientName` esiste in entrambe le versioni Quarkus in gioco ed è un
`@Qualifier` con `@Target({FIELD, METHOD, PARAMETER, TYPE})` — quindi utilizzabile anche su parametri di
costruttore. Nota: `onboarding-ms`, il servizio target principale, è su Quarkus **3.11.2** (non 3.31.2 come
indicato in precedenza); `document-ms` è su 3.31.2.

Configurazione corrispondente (`application.properties`), che sostituisce l'attuale coppia
`quarkus.mongodb.connection-string` / `quarkus.mongodb.database`:

```properties
# database-per-tenant sullo stesso account: un solo client, nome DB dal resolver
quarkus.mongodb.connection-string = ${MONGODB_CONNECTION_STRING}
quarkus.mongodb.database          = ${MONGODB_DATABASE_NAME}   # fallback se il resolver non è presente

# account-per-tenant (solo finestra di migrazione): named client
quarkus.mongodb.ar.connection-string   = ${MONGODB_CONNECTION_STRING_AR}
quarkus.mongodb.pnpg.connection-string = ${MONGODB_CONNECTION_STRING_PNPG}

# storage per tenant
selfcare.tenant.ar.storage-connection-string   = ${STORAGE_CONNECTION_STRING_AR}
selfcare.tenant.pnpg.storage-connection-string = ${STORAGE_CONNECTION_STRING_PNPG}
```

Da non confondere con il modello discriminator-field già consegnato (`document-ms`, filtro `WHERE tenantId
= ...` su un unico database): il named client è la soluzione per i soli servizi che, come `onboarding-ms`
oggi, mantengono Cosmos **fisicamente separato per tenant**; una volta consolidato il database, il servizio
dovrebbe migrare al modello discriminator-field invece di continuare a mantenere due client.

### Equivalente per i microservizi Spring

Tra i 6 servizi "stateful" del sub-task 7 (Cosmos in entrambi gli account AR e PNPG oggi), due sono Spring
anziché Quarkus: `institution-ms` e `user-group-ms`. Entrambi implementano già lato codice il modello
discriminator-field (query `tenantScoped(...)` — vedi `MongoCustomConnectorImpl` in `institution-ms` e
`UserGroupServiceImpl.tenantScoped(...)` in `user-group-ms`), ma girano oggi contro **due Cosmos ancora
separati**, uno per stack (`dev-ar`, `dev-pnpg` dichiarano ciascuno un proprio `module "cosmosdb"`): lo
stesso gap di `onboarding-ms`, solo mascherato dal fatto che ogni pod oggi si connette a un solo account.

Spring Data MongoDB non ha un `@MongoClientName` nativo, ma offre un punto di innesto equivalente e più
centralizzato: `MongoTemplate.doGetDatabase()` delega a `MongoDatabaseUtils.getDatabase(mongoDbFactory, ...)`
**a ogni chiamata** (verificato via bytecode, non è cacheato in un campo), quindi basta un
`MongoDatabaseFactory` custom — lo stesso pattern di `AbstractRoutingDataSource` per JDBC, applicato a Mongo:

```java
public class TenantRoutingMongoDatabaseFactory implements MongoDatabaseFactory {

  private final MongoDatabaseFactory arFactory;
  private final MongoDatabaseFactory pnpgFactory;
  private final CurrentTenantProvider currentTenantProvider;

  public TenantRoutingMongoDatabaseFactory(MongoDatabaseFactory arFactory,
                                           MongoDatabaseFactory pnpgFactory,
                                           CurrentTenantProvider currentTenantProvider) {
    this.arFactory = arFactory;
    this.pnpgFactory = pnpgFactory;
    this.currentTenantProvider = currentTenantProvider;
  }

  @Override
  public MongoDatabase getMongoDatabase() {
    return delegate().getMongoDatabase();
  }

  @Override
  public MongoDatabase getMongoDatabase(String dbName) {
    return delegate().getMongoDatabase(dbName);
  }

  @Override
  public PersistenceExceptionTranslator getExceptionTranslator() {
    return delegate().getExceptionTranslator();
  }

  @Override
  public ClientSession getSession(ClientSessionOptions options) {
    return delegate().getSession(options);
  }

  @Override
  public MongoDatabaseFactory withSession(ClientSession session) {
    return delegate().withSession(session);
  }

  private MongoDatabaseFactory delegate() {
    return currentTenantProvider.currentTenantId()
        .flatMap(TenantId::fromValue)
        .map(t -> t == TenantId.PNPG ? pnpgFactory : arFactory)
        .orElseThrow(UnresolvedTenantException::new); // fail-closed
  }
}
```

I cinque metodi sopra sono **tutti e soli** i metodi astratti di `MongoDatabaseFactory` in
spring-data-mongodb 4.3.0 (la versione portata da Spring Boot 3.3.0 via `selc-starter-parent`): verificato
sul bytecode che `getCodecRegistry()`, `withSession(ClientSessionOptions)` e `isTransactionActive()` sono
già `default` e non vanno implementati, mentre `getExceptionTranslator()`, `getSession(...)` e
`withSession(ClientSession)` — omessi da una precedente stesura di questo snippet — sono obbligatori.

```java
@Configuration
public class TenantAwareMongoConfig {

  @Bean @Primary
  public MongoDatabaseFactory tenantRoutingMongoDatabaseFactory(
      @Qualifier("arDbFactory") MongoDatabaseFactory ar,
      @Qualifier("pnpgDbFactory") MongoDatabaseFactory pnpg,
      CurrentTenantProvider currentTenantProvider) {
    return new TenantRoutingMongoDatabaseFactory(ar, pnpg, currentTenantProvider);
  }

  @Bean("arDbFactory")
  public MongoDatabaseFactory arDbFactory(@Value("${mongodb.ar.uri}") String uri) {
    return new SimpleMongoClientDatabaseFactory(uri);
  }

  @Bean("pnpgDbFactory")
  public MongoDatabaseFactory pnpgDbFactory(@Value("${mongodb.pnpg.uri}") String uri) {
    return new SimpleMongoClientDatabaseFactory(uri);
  }
}
```

`SimpleMongoClientDatabaseFactory` espone i costruttori `(String connectionString)`,
`(ConnectionString)` e `(MongoClient, String databaseName)` — verificati sul jar. I due servizi Spring
interessati configurano oggi `spring.data.mongodb.uri` + `spring.data.mongodb.database`
(`institution-ms/connector/dao/.../dao-config.properties`, `user-group-ms/.../core-config.properties`):
quelle due proprietà vanno sostituite dalla coppia `mongodb.ar.uri` / `mongodb.pnpg.uri` sopra, altrimenti
l'autoconfigurazione di Spring Boot continua a creare il proprio factory (che il `@Primary` scavalca, ma
lasciandolo attivo si mantiene una connessione inutile).

Il vantaggio rispetto a Quarkus/Panache: qui il dispatch è **centralizzato in un solo bean**, non va
ripetuto per ogni repository. Il `MongoTemplate`/`MongoOperations` condiviso (iniettato in
`MongoCustomConnectorImpl` per `institution-ms`, o dietro le `MongoRepository` auto-generate di
`user-group-ms`) resta invariato: nessuna repository, custom o auto-generata da Spring Data, deve essere
toccata. Le query `tenantScoped(...)` restano comunque utili come difesa in profondità durante la finestra
di migrazione, ma diventano ridondanti — non più necessarie per l'isolamento — una volta che ogni pod parla
con un solo Cosmos per tenant tramite questo factory.

### Selezione dello storage account per tenant (Quarkus e Spring, stesso pattern)

Verificato nel codice: ogni connector di storage oggi presente nel repository costruisce **un solo**
`BlobServiceClient`/`BlobServiceAsyncClient`, una volta sola nel costruttore, da un'**unica** proprietà di
connection-string/account-name — lo stesso gap già visto per Mongo, ma replicato indipendentemente in
quattro classi: `AzureBlobClient` (`institution-ms`, Spring), `AzureBlobClient` (`dashboard-bff`, Spring),
`ContractTemplateStorageImpl` (`product`, Quarkus) e la configurazione blob di `onboarding-ms`
(`onboarding-ms.blob-storage.account-name-product`, Quarkus). Nessuna di queste ha mai avuto bisogno di
selezionare un account diverso a runtime, perché finora ogni pod gira in uno stack `-ar` o `-pnpg` isolato.

A differenza di Mongo, qui **non serve un meccanismo specifico per framework**: l'SDK Azure Storage
(`com.azure.storage.blob`) è già usato in modo completamente manuale in tutte e quattro le classi — non c'è
un layer applicativo (Panache, Spring Data) che intercetta le chiamate e va convinto a rieseguire una
lookup. Basta un unico provider, identico in Quarkus e Spring a meno delle annotazioni, che costruisce gli N
client all'avvio (uno per tenant) ed espone un metodo risolto ad ogni chiamata:

```java
@ApplicationScoped // Spring: @Component
public class TenantAwareBlobServiceClientProvider {

  private final EnumMap<TenantId, BlobServiceClient> clientsByTenant = new EnumMap<>(TenantId.class);
  private final CurrentTenantProvider currentTenantProvider;

  @Inject // Spring: costruttore unico, nessuna annotazione richiesta
  public TenantAwareBlobServiceClientProvider(
      Config config,                       // Spring: org.springframework.core.env.Environment
      CurrentTenantProvider currentTenantProvider) {
    this.currentTenantProvider = currentTenantProvider;
    for (TenantId tenant : TenantId.values()) {
      String key = "selfcare.tenant." + tenant.name().toLowerCase() + ".storage-connection-string";
      String connectionString = config.getOptionalValue(key, String.class)
          .orElseThrow(() -> new IllegalStateException("Configurazione storage mancante: " + key));
      clientsByTenant.put(tenant,
          new BlobServiceClientBuilder().connectionString(connectionString).buildClient());
    }
  }

  public BlobServiceClient current() {
    return currentTenantProvider.currentTenantId()
        .flatMap(TenantId::fromValue)
        .map(clientsByTenant::get)
        .orElseThrow(UnresolvedTenantException::new); // fail-closed, nessun default silenzioso
  }
}
```

Il ciclo su `TenantId.values()` con `orElseThrow` fa fallire l'**avvio** del pod se un tenant non ha
configurazione: aggiungere un terzo tenant senza configurarlo rompe al boot, non a runtime sulla prima
richiesta di quel tenant.

**Nota su managed identity.** Tutti i connector oggi in repository usano *connection string*, non managed
identity — lo snippet sopra riflette lo stato attuale ed è quindi un drop-in. La variante con managed
identity sostituisce le due righe di build con:

```java
      .endpoint("https://" + accountName + ".blob.core.windows.net")
      .credential(new DefaultAzureCredentialBuilder()
          .managedIdentityClientId(managedIdentityClientId).build())
```

(`DefaultAzureCredentialBuilder.managedIdentityClientId(String)` e
`BlobServiceClientBuilder.credential(TokenCredential)` verificati sui jar). Richiede però
`com.azure:azure-identity` sul classpath: oggi lo dichiara esplicitamente solo `dashboard-bff`; i servizi
Quarkus passano da `io.quarkiverse.azureservices:quarkus-azure-storage-blob` e la dipendenza va aggiunta.

I quattro connector esistenti cambiano nello stesso modo: sostituire il campo `BlobServiceClient` costruito
nel costruttore con l'iniezione di `TenantAwareBlobServiceClientProvider`, e chiamare `.current()` all'inizio
di ogni metodo (`getTemplateFile`, `uploadInstitutionLogo`, `upload`, ...) invece di leggere il campo fisso —
mai una volta sola, per restare corretti con richieste AR/PNPG interlacciate sullo stesso processo. Per
`ContractTemplateStorageImpl` (client asincrono) l'`EnumMap` contiene `BlobServiceAsyncClient` e la build
termina con `.buildAsyncClient()`; il resto del pattern è identico.

Questo copre solo la selezione dell'**account**; il nome del **container**, dove un servizio condivide un
solo account tra tenant (SELC-9.3), resta un problema diverso e si risolve suffissando il container col
tenant nello stesso punto in cui oggi si legge la proprietà fissa — i due meccanismi non vanno combinati per
lo stesso servizio, sono alternativi a seconda di quale modello di isolamento storage è stato scelto per quel
servizio.

> **Nota trasversale sugli snippet di questa sezione.** `TenantDataIsolationRegistry`,
> `UnresolvedTenantMappingException` e la variabile Terraform `tenant_data_isolation`, citati in una
> precedente stesura come "già implementati", **non esistono** nel codice né in Terraform (verificato con
> grep sull'intero repository: comparivano solo in questi documenti). Gli snippet sono stati riscritti per
> essere autonomi, leggendo la configurazione per tenant da chiavi ordinarie
> (`selfcare.tenant.<ar|pnpg>.*`) e usando `TenantId.fromValue(String) → Optional<TenantId>` (fail-closed,
> preferibile a `valueOf`) e un `UnresolvedTenantException` applicativo. Le firme usate sono verificate sui
> jar effettivamente risolti, ma **non compilate**.

---

# Business continuity & disaster recovery

**Cosa può rompersi**: risoluzione tenant errata in APIM (redirige traffico al tenant sbagliato); mismatch
header/claim non gestito correttamente (autorizzazione errata); modello di isolamento dati misto o incompleto
per un microservizio (data leak cross-tenant); injection del claim `hub-spid-login` che indebolisce la
validazione SPID esistente; cutover APIM che interrompe il traffico durante la migrazione.

**Come si mantiene la continuità operativa**:
- Migrazione **parallel-run**: il nuovo stack condiviso viene distribuito accanto ai legacy `-ar`/`-pnpg`; il
  cutover avviene solo a livello di routing APIM; i legacy vengono dismessi solo dopo validazione in produzione
  (`Step_0/ARCHITECTURE.md` "Migration Strategy") — consente un rollback rapido riportando il routing APIM al
  legacy.
- **Fail-closed by design**: ogni componente (APIM, enforcement backend, Cosmos DB `TenantResolver`, Storage
  resolver, vault, email) rigetta esplicitamente le richieste con tenant non risolvibile, invece di degradare
  silenziosamente su un tenant di default (SELC-1.3, SELC-8.4, SELC-9.4, SELC-10.2, SELC-11.2) — riduce il
  raggio d'azione di un errore di risoluzione a un rifiuto osservabile, non a una corruzione/leak dati.
- **Segregazione dei segreti**: certificati JWT, connection string Cosmos DB, chiavi Storage e credenziali email
  restano in Azure Key Vault anche dopo il consolidamento del deployment, per non ridurre l'isolamento di trust
  tra tenant durante/dopo la migrazione (`Step_0/SECURITY.md`, `Step_1/SECURITY.md` — Secret handling).
- **Audit logging esplicito** per rigetti da mismatch tenant e per il default `hub-spid-login → PNPG`, per
  consentire investigazione post-incidente (`Step_0/SECURITY.md`, `Step_1/SECURITY.md` — Logging & error
  handling).

Piano di rollback dettagliato e RTO/RPO: `TO BE DECIDED` — non specificati nei documenti sorgente.
