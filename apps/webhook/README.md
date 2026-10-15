# Webhook Service

Microservizio per la gestione e invio di notifiche via webhook HTTP.

## Funzionalità

- **Gestione Webhook**: CRUD completo per configurazioni webhook
- **Invio Notifiche HTTP**: Invio asincrono di notifiche verso endpoint esterni
- **Retry Logic**: Sistema di retry automatico con backoff esponenziale
- **Monitoring**: Health check e metriche
- **Persistenza**: MongoDB per configurazioni e stato notifiche
- **Security**: Autenticazione JWT tramite selfcare-sdk-security

## Tecnologie

- **Quarkus 3.28.3**: Framework Java reattivo
- **MongoDB**: Database per persistenza
- **Java 17**: Versione Java
- **Maven**: Build tool

## Struttura

```
webhook/
├── src/main/java/it/pagopa/selfcare/webhook/
│   ├── entity/           # Modelli dominio (Webhook, WebhookNotification)
│   ├── repository/       # Repository MongoDB Panache
│   ├── service/          # Business logic e invio HTTP
│   ├── resource/         # REST endpoints
│   └── dto/              # Data Transfer Objects
├── src/main/resources/
│   └── application.properties
├── pom.xml
├── Dockerfile
└── docker-compose.yml
```

## API Endpoints

### Webhook Management

- `POST /webhooks` - Crea nuovo webhook
- `GET /webhooks` - Lista tutti i webhook
- `GET /webhooks/{id}` - Recupera webhook specifico
- `PUT /webhooks/{id}` - Aggiorna webhook
- `DELETE /webhooks/{id}` - Elimina webhook

### Notifiche

- `POST /webhooks/notify` - Invia notifica webhook

### Health & Docs

- `GET /health` - Health check
- `GET /swagger-ui` - Swagger UI
- `GET /openapi` - OpenAPI spec

## Configurazione

Variabili d'ambiente principali:

```bash
MONGODB_CONNECTION_STRING=mongodb://localhost:27017
MONGODB_DATABASE=webhook
JWT_ISSUER=https://selfcare.pagopa.it
WEBHOOK_RETRY_MAX_ATTEMPTS=3
WEBHOOK_RETRY_INITIAL_DELAY=1000
WEBHOOK_RETRY_MAX_DELAY=10000
WEBHOOK_TIMEOUT_CONNECT=5000
WEBHOOK_TIMEOUT_READ=10000
```

## Build & Run

### Tenant e JWT

Il servizio usa `selfcare-sdk-security` 0.8.0. Per
`GET /webhooks/{productId}`, `tenantId` e' opzionale: se assente o vuoto viene
risolto da `X-Tenant-Id`, oppure dal claim JWT `tenant_id`. Valori discordanti
tra query, header e claim causano `400`; anche l'assenza di tutte le fonti causa `400`.
Gli endpoint interni mantengono il comportamento precedente, senza un filtro
globale che imponga il tenant.

Il registro contiene `AR` e `PNPG` e non configura risorse Mongo per tenant:
la persistenza resta sul database condiviso configurato per webhook.
`TENANT_SUPPORTED_TENANTS` controlla i tenant del registro. La verifica JWT
accetta gli issuer `SPID` e `PAGOPA` e usa ancora `JWT_PUBLIC_KEY` se nel registro
non sono configurate chiavi per tenant.

Il filtro SDK per i token `SPID` resta attivo: richiede `X-Tenant-Id` coerente
con il tenant del token, altrimenti risponde `401`. Per i token legacy privi
del claim, l'SDK usa `DEFAULT_TENANT` (default `PNPG`) e valida i tenant rispetto
a `SUPPORTED_TENANTS` (default `AR,PNPG`). Queste variabili sono distinte da
`TENANT_SUPPORTED_TENANTS`. `JWT_TENANT_CLAIM_REQUIRED=true` rifiuta i token SPID
senza claim. Il default legacy non e' una fonte per `EffectiveTenant`.

### Locale con Maven

```bash
# Build
mvn clean package -DskipTests

# Run
java -jar target/quarkus-app/quarkus-run.jar
```

### Docker

```bash
# Build
podman build \
  --build-arg REPO_SELFCARE=selfcare \
  --build-arg REPO_USERNAME=your_username \
  --build-arg REPO_PASSWORD=your_token \
  -t webhook:latest \
  -f apps/webhook/Dockerfile \
  .

# Run
podman run -p 8083:8083 \
  -e MONGODB_CONNECTION_STRING=mongodb://host.docker.internal:27017 \
  webhook:latest
```

### Docker Compose

```bash
cd apps/webhook
export GITHUB_USERNAME=your_username
export GITHUB_TOKEN=your_token
docker-compose up
```

## Esempio Utilizzo

### 1. Crea un webhook

```bash
curl -X POST http://localhost:8083/webhooks \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My Webhook",
    "description": "Notifica per eventi",
    "url": "https://example.com/webhook",
    "httpMethod": "POST",
    "headers": {
      "Authorization": "Bearer token123",
      "X-Custom-Header": "value"
    },
    "retryPolicy": {
      "maxAttempts": 3,
      "initialDelayMs": 1000,
      "maxDelayMs": 10000,
      "backoffMultiplier": 2.0
    }
  }'
```

### 2. Invia una notifica

```bash
curl -X POST http://localhost:8083/webhooks/notify \
  -H "Content-Type: application/json" \
  -d '{
    "webhookId": "507f1f77bcf86cd799439011",
    "payload": "{\"event\":\"user.created\",\"data\":{\"userId\":\"123\"}}"
  }'
```

## Retry Logic

Il sistema implementa un meccanismo di retry automatico:

1. **Primo tentativo**: Immediato
2. **Retry**: Con backoff esponenziale (default: 1s, 2s, 4s...)
3. **Massimo tentativi**: Configurabile (default: 3)
4. **Scheduler**: Processa notifiche fallite ogni 30 secondi

## Monitoring

- Health endpoint: `GET /health`
- Logs strutturati con livelli configurabili
- Tracking dello stato di ogni notifica (PENDING, SENDING, SUCCESS, FAILED, RETRY)

## Sviluppo

### Test locale con MongoDB

```bash
# Start MongoDB
podman run -d -p 27017:27017 --name mongodb mongo:7

# Run application in dev mode
mvn quarkus:dev
```

### Accesso Swagger UI

Una volta avviato il servizio, accedi a:
http://localhost:8083/swagger-ui
