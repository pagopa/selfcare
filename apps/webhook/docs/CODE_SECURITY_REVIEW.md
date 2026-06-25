# Webhook Service - Code and Security Review

## Scope

This review covers the current `apps/webhook` implementation against:

- `requirements.md`
- `ARCHITECTURE.md`
- `SECURITY.md`
- the application code under `src/main/java`
- the available tests under `src/test`

## Critical findings

| Severity | Area | Finding | Evidence | Recommendation |
| --- | --- | --- | --- | --- |
| High | Authentication and authorization | REST endpoints do not enforce application-level authentication or authorization. JWT is configured, but controllers do not use `@Authenticated`, `@RolesAllowed`, or equivalent checks. | `WebhookController.java`, `InfoController.java` | Define endpoint access rules, then enforce them in the service using Quarkus security annotations or a central authorization layer. Fail closed for create, list, update, delete, and notification submission. |
| High | SSRF / outbound HTTP | Webhook URLs are user-configurable and used directly for outbound HTTP calls without validating scheme, host, port, resolved IP, redirects, or private-network targets. | `WebhookNotificationService.sendHttpRequest`, `URI.create(webhook.getUrl())` | Add a webhook target validator. Restrict schemes, ports, hosts, redirects, DNS behavior, and private/link-local/metadata IP ranges before sending. Prefer allowlists where possible. |
| High | Sensitive data exposure | Webhook headers are decrypted and returned in API responses. If headers contain `Authorization`, API keys, or tokens, they are exposed to API readers. | `WebhookService.toResponse`, `response.setHeaders(DataEncryptionConfig.decrypt(webhook.getHeaders()))` | Do not return decrypted secret header values. Return redacted values or header names only, and expose secrets only through explicit rotation/update flows. |

## Security findings

| Severity | Area | Finding | Evidence | Recommendation |
| --- | --- | --- | --- | --- |
| Medium/High | Input validation | `httpMethod` is accepted as an arbitrary string and later passed to `HttpMethod.valueOf(...)`. Invalid or unintended methods can fail at runtime or enable unwanted behavior. | `WebhookRequest.httpMethod`, `WebhookNotificationService.sendHttpRequest` | Validate `httpMethod` against an explicit allowlist such as `POST`, `PUT`, or other approved methods. |
| Medium/High | Input validation | URL, headers, payload, and retry policy values have only minimal validation. There are no strong limits on size, format, header names, header values, retry bounds, or URL length. | `WebhookRequest`, `NotificationRequest`, `RetryPolicyRequest` | Add Bean Validation constraints and service-level validation for size, format, and allowed ranges. Reject negative, zero, extreme, or nonsensical retry values. |
| Medium | Logging and persistence | Target endpoint response bodies are stored in `lastError` and can be logged on permanent failure. The target response is attacker-controlled and may contain sensitive data or log injection payloads. | `WebhookNotificationService.handleHttpResponse`, `markNotificationAsFailed` | Store bounded, sanitized error summaries. Avoid persisting or logging full remote response bodies by default. |
| Medium | Secret defaults | Encryption key and IV have static default values in `application.properties`. If secrets are missing in an environment, the service can start with known cryptographic material. | `application.properties` | Remove production-like defaults for encryption material and fail fast when required secrets are missing outside test profiles. |
| Medium | Authorization design gap | `SECURITY.md` requires fail-closed behavior for privileged endpoints, but `ARCHITECTURE.md` still marks endpoint authorization policy as `TO BE DECIDED`, and the code exposes sensitive operations. | `ARCHITECTURE.md`, `SECURITY.md`, controllers | Resolve authorization requirements before production use. Define caller types, scopes/roles, product ownership checks, and internal-only endpoints. |

## Functional and reliability findings

| Severity | Area | Finding | Evidence | Recommendation |
| --- | --- | --- | --- | --- |
| Medium | Retry semantics | `initialDelayMs`, `maxDelayMs`, and `backoffMultiplier` are persisted but not used to schedule retries. The scheduler processes retryable records every 10 seconds regardless of the policy. | `RetryPolicy`, `WebhookNotificationService.processFailedNotifications` | Either implement policy-based retry timing or remove/defer these fields from the active contract until supported. |
| Medium | Notification lifecycle | Notifications created by `sendNotification` start as `SENDING`, while the retry scheduler only claims `PENDING` and `RETRY`. If the initial processing fails before setting `RETRY`, records can remain stuck in `SENDING`. | `WebhookService.sendNotification`, `WebhookNotificationRepository.findAndLockPendingNotifications` | Persist new notifications as `PENDING`, then transition to `SENDING` only when processing starts, or add a recovery path for stale `SENDING` records. |
| Medium | Error handling | `sendHttpRequest` catches broad exceptions and turns them into retry failures, but several invalid configuration cases are not rejected at creation/update time. | `WebhookNotificationService.sendHttpRequest` | Validate webhook configuration before persistence so delivery-time failures are reserved for runtime/network errors. |
| Low/Medium | Delete behavior | Delete service methods exist and are unit-tested, but the REST endpoint always returns `501 Not Implemented`. | `WebhookController.deleteWebhook`, `WebhookService.deleteWebhookByProductId`, `WebhookServiceTest` | Either remove unused delete service paths/tests or complete the API behavior with authorization, retention, and audit semantics. |

## Code structure findings

| Severity | Area | Finding | Evidence | Recommendation |
| --- | --- | --- | --- | --- |
| Low/Medium | Service responsibilities | `WebhookService` mixes DTO mapping, encryption/decryption, business logic, persistence orchestration, and notification creation. | `WebhookService` | Extract validators and mappers. Keep encryption policy isolated and make response redaction explicit. |
| Low/Medium | Model clarity | `WebhookInternalRequest` extends `WebhookRequest` and redeclares `productId`, creating an ambiguous model. | `WebhookInternalRequest` | Avoid field shadowing. Prefer composition or a separate DTO with explicit fields. |
| Low | Sanitization semantics | `Sanitizer.sanitizeString` removes all characters outside `[A-Za-z0-9_-]`. This is safe for logs but can corrupt legitimate identifiers if used before business queries. | `Sanitizer`, `WebhookController.getWebhook`, `WebhookService.createWebhook` | Separate validation from log sanitization. Validate identifiers before persistence/querying and sanitize only at output/logging boundaries. |
| Low | Test quality | Controller tests include unused query parameters and do not assert authentication or validation behavior. | `WebhookControllerTest` | Add tests for auth, invalid input, redaction, SSRF prevention, and error handling. |

## Missing security tests

Add tests for:

- unauthenticated access to every non-public endpoint;
- authorization failures for cross-product access;
- invalid and disallowed webhook URLs, including localhost, link-local, private ranges, metadata IPs, unsupported schemes, and redirect-to-private targets;
- invalid HTTP methods;
- oversized payloads and headers;
- retry policy bounds;
- redaction of webhook headers in responses;
- sanitized and bounded `lastError` values;
- stale `SENDING` notification recovery.

## Recommended fix order

1. Define and enforce authentication and authorization for all endpoints.
2. Add webhook URL validation and SSRF protections before outbound delivery.
3. Stop returning decrypted webhook header values in API responses.
4. Add strict validation for HTTP method, URL, headers, payload size, and retry policy bounds.
5. Fix notification lifecycle and retry semantics.
6. Refactor service responsibilities into validators, mappers, and focused business services.
