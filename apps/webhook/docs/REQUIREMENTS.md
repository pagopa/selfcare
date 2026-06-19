# Webhook Service - Requirements

## Goal

The Webhook service allows Selfcare products to configure HTTP endpoints and send asynchronous notifications to those endpoints. The service persists webhook configurations and notification delivery status in MongoDB, exposes REST APIs documented through OpenAPI, and manages retries for undelivered notifications.

## Scope

The service must cover:

- webhook configuration management by product;
- notification delivery to active webhooks associated with a product;
- notification delivery status tracking;
- automatic retry of pending or retryable notifications;
- application information, health check, and OpenAPI documentation exposure.

The current implementation does not cover:

- actual webhook deletion through the API, which is currently not implemented;
- retry scheduling based on `initialDelayMs`, `maxDelayMs`, and `backoffMultiplier`, which are currently stored but not used to calculate retry timing.

## Actors

| Actor | Description |
| --- | --- |
| External client | Authorized system that creates, reads, or updates webhook configurations for a product. |
| Internal service | Selfcare system that requests notification delivery for a product. |
| Target endpoint | External system that receives the configured webhook HTTP call. |
| Application scheduler | Internal process that retrieves and retries pending or retryable notifications. |

## Functional requirements

### FR-01 - Create webhook

The service must allow creating a webhook configuration through `POST /webhooks`.

The request must include:

| Field | Required | Description |
| --- | --- | --- |
| `url` | Yes | Target endpoint URL. |
| `productId` | Yes | Product identifier associated with the webhook. |
| `httpMethod` | Yes | HTTP method used to deliver the notification. |
| `headers` | No | Custom HTTP headers sent to the target endpoint. |
| `retryPolicy` | No | Notification retry parameters. |

On creation, the webhook must be persisted with status `ACTIVE`, an empty `description`, `products` set with the provided `productId`, creation and update timestamps, and the default retry policy when no policy is provided.

Expected outcome: `201 Created` with the created webhook details.

### FR-02 - List webhooks

The service must allow retrieving all webhook configurations through `GET /webhooks`.

Expected outcome: `200 OK` with a list of webhooks.

### FR-03 - Get webhook by product

The service must allow retrieving the webhook configuration associated with a product through `GET /webhooks/{productId}`.

Expected outcomes:

| Condition | Response |
| --- | --- |
| Webhook found | `200 OK` with webhook details. |
| Webhook not found | `404 Not Found`. |

### FR-04 - Update webhook by product

The service must allow updating the webhook configuration associated with a product through `PUT /webhooks/{productId}`.

The request can update URL, HTTP method, headers, and retry policy. The service must update the `updatedAt` timestamp.

Expected outcomes:

| Condition | Response |
| --- | --- |
| Webhook found and updated | `200 OK` with updated details. |
| Webhook not found | `404 Not Found`. |

### FR-05 - Delete webhook by product

The `DELETE /webhooks/{productId}` endpoint is exposed but must return `501 Not Implemented` until deletion is completed.

### FR-06 - Send notification

The service must allow requesting notification delivery through `POST /webhooks/notify`.

The request must include:

| Field | Required | Description |
| --- | --- | --- |
| `productId` | Yes | Product for which the notification must be delivered. |
| `payload` | Yes | Serialized JSON payload to send to the target endpoint. |

The service must find all `ACTIVE` webhooks associated with the product, create one notification for each webhook, and start HTTP delivery.

Expected outcome: `202 Accepted`.

If no active webhooks exist for the product, the request must complete without error and a warning log must be written.

### FR-07 - HTTP delivery to target endpoint

For each notification, the service must:

- use the URL configured in the webhook;
- use the configured HTTP method;
- set `Content-Type: application/json`;
- add configured custom headers;
- add a signed JWT header to authenticate the notification source;
- send the notification payload as request body;
- follow HTTP redirects;
- apply configured timeouts.

An HTTP response with a `2xx` status code must mark the notification as `SUCCESS` and set `completedAt`.

A non-`2xx` response or communication error must increment `attemptCount`, store `lastError`, and decide whether to retry or mark the notification as failed.

### FR-08 - Retry notifications

The service must process notifications in `PENDING` or `RETRY` status through a periodic scheduler.

The job must:

- run every 10 seconds;
- acquire at most 100 notifications per cycle;
- apply a processing lock with a 5-minute duration;
- process only notifications that are not already being processed or whose lock has expired;
- release the lock after processing or on error.

The notification must be marked as `FAILED` when `attemptCount` reaches the webhook retry policy `maxAttempts`, or `3` if the policy is missing.

### FR-09 - Webhook and notification statuses

The webhook must support the following statuses:

| Status | Description |
| --- | --- |
| `ACTIVE` | Webhook enabled for notification delivery. |
| `INACTIVE` | Webhook not active. |
| `SUSPENDED` | Webhook suspended. |

The notification must support the following statuses:

| Status | Description |
| --- | --- |
| `PENDING` | Notification waiting to be processed. |
| `SENDING` | Notification being sent. |
| `SUCCESS` | Notification delivered with a `2xx` response. |
| `FAILED` | Notification permanently failed. |
| `RETRY` | Notification to be retried. |

### FR-10 - Application information

The service must expose `GET /info/version` and return the current application version in the following format:

```json
{
  "version": "<version>"
}
```

## Data requirements

### Webhook

| Field | Description |
| --- | --- |
| `id` | MongoDB identifier. |
| `productId` | Main product associated with the webhook. |
| `description` | Webhook description. |
| `url` | Target endpoint. |
| `httpMethod` | HTTP method used to send notifications. |
| `headers` | Custom headers encrypted at rest and decrypted in responses. |
| `products` | List of products associated with the webhook. |
| `status` | Webhook status. |
| `retryPolicy` | Retry policy. |
| `createdAt` | Creation date. |
| `updatedAt` | Last update date. |
| `createdBy` | Creator user or system, when available. |

### Retry policy

| Field | Default | Description |
| --- | --- | --- |
| `maxAttempts` | `3` | Maximum number of attempts. |
| `initialDelayMs` | `1000` | Intended initial delay. |
| `maxDelayMs` | `10000` | Intended maximum delay. |
| `backoffMultiplier` | `2.0` | Intended backoff multiplier. |

### Webhook notification

| Field | Description |
| --- | --- |
| `id` | MongoDB identifier. |
| `webhookId` | Webhook that owns the notification. |
| `payload` | Payload to send. |
| `status` | Notification status. |
| `attemptCount` | Number of performed attempts. |
| `lastError` | Last encountered error. |
| `createdAt` | Creation date. |
| `lastAttemptAt` | Last attempt date. |
| `completedAt` | Completion date. |
| `processing` | Processing lock flag. |
| `processingUntil` | Processing lock expiration. |

## API

| Method | Path | Tag | Description | Main response |
| --- | --- | --- | --- | --- |
| `POST` | `/webhooks` | `external-v2` | Creates a webhook. | `201 Created` |
| `GET` | `/webhooks` | `internal-v1` | Lists all webhooks. | `200 OK` |
| `GET` | `/webhooks/{productId}` | `external-v2` | Retrieves a webhook by product. | `200 OK`, `404 Not Found` |
| `PUT` | `/webhooks/{productId}` | `external-v2` | Updates a webhook by product. | `200 OK`, `404 Not Found` |
| `DELETE` | `/webhooks/{productId}` | - | Endpoint exposed but not implemented. | `501 Not Implemented` |
| `POST` | `/webhooks/notify` | `internal-v1` | Creates and sends notifications for a product. | `202 Accepted` |
| `GET` | `/info/version` | `System` | Returns the application version. | `200 OK` |

## Non-functional requirements

### Persistence

The service must use MongoDB as persistent storage. The main collections are:

- `webhooks`;
- `webhookNotifications`.

Connection configuration must be read from:

| Property | Default |
| --- | --- |
| `MONGODB_CONNECTION_STRING` | `mongodb://localhost:27017` |
| `MONGODB_DATABASE_NAME` | `selcWebhook` |

### Security

The service includes JWT support through Quarkus SmallRye JWT and `selfcare-sdk-security`. The public key must be configured through `JWT_PUBLIC_KEY`, and the subject must be read from the `uid` claim.

Webhook headers must be encrypted at rest through:

| Property | Description |
| --- | --- |
| `SELFCARE_DATA_ENCRIPTION_KEY` | Data encryption key. |
| `SELFCARE_DATA_ENCRIPTION_IV` | Initialization vector. |

### Validation and sanitization

The service must validate required fields with Bean Validation:

- `WebhookRequest.url`;
- `WebhookRequest.productId`;
- `WebhookRequest.httpMethod`;
- `NotificationRequest.productId`;
- `NotificationRequest.payload`.

Relevant string inputs must be sanitized before use or logging.

### Observability

The service must:

- produce application logs at `INFO` level by default;
- expose health checks under `/q/health`;
- expose OpenAPI under `/openapi`;
- expose Swagger UI under `/swagger-ui`.

### Configurability

| Property | Default | Description |
| --- | --- | --- |
| `WEBHOOK_RETRY_MAX_ATTEMPTS` | `3` | Configurable maximum number of attempts. |
| `WEBHOOK_RETRY_INITIAL_DELAY` | `1000` | Intended initial delay. |
| `WEBHOOK_RETRY_MAX_DELAY` | `10000` | Intended maximum delay. |
| `WEBHOOK_TIMEOUT_CONNECT` | `5000` | HTTP connection timeout in ms. |
| `WEBHOOK_TIMEOUT_READ` | `10000` | HTTP read timeout in ms. |

### Performance and concurrency

Retry processing must avoid duplicate concurrent processing by using atomic MongoDB locks. Each scheduled cycle must limit the workload to 100 notifications.

## Acceptance criteria

| ID | Scenario | Expected outcome |
| --- | --- | --- |
| AC-01 | Create a webhook with a valid request. | Response `201` and body containing `productId` and `url`. |
| AC-02 | List webhooks after creating two configurations. | Response `200` with two items. |
| AC-03 | Retrieve an existing webhook by product. | Response `200` with the expected `url`. |
| AC-04 | Update an existing webhook. | Response `200` with updated data. |
| AC-05 | Send a notification for a product with an active webhook. | Response `202` and creation of an associated notification. |
| AC-06 | Target endpoint responds with `2xx`. | Notification marked as `SUCCESS`. |
| AC-07 | Target endpoint responds with non-`2xx` below retry threshold. | Notification marked as `RETRY` and `attemptCount` incremented. |
| AC-08 | Target endpoint keeps failing beyond `maxAttempts`. | Notification marked as `FAILED`. |
| AC-09 | Delete webhook endpoint is invoked. | Response `501 Not Implemented`. |

## Implementation references

- REST controller: `src/main/java/it/pagopa/selfcare/webhook/controller/WebhookController.java`
- Application logic: `src/main/java/it/pagopa/selfcare/webhook/service/WebhookService.java`
- Notification delivery and retry: `src/main/java/it/pagopa/selfcare/webhook/service/WebhookNotificationService.java`
- MongoDB entities: `src/main/java/it/pagopa/selfcare/webhook/entity/`
- Configuration: `src/main/resources/application.properties`
- OpenAPI: `src/main/docs/openapi.yaml`
- BDD scenarios: `src/test/resources/features/webhook.feature`
