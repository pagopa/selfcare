# Webhook Service - Security

## Required Security Inputs

- Requirements source: `requirements.md`
- Architecture source: `ARCHITECTURE.md`
- Local prompt library status: no repository `PROMPT.md` files were found; use the public OWASP fallbacks until a local prompt library is added.
- Concrete stack: Java 17, Quarkus REST APIs, SmallRye Mutiny, Quarkus Scheduler, Quarkus MongoDB Panache, SmallRye JWT, Vert.x WebClient, OpenAPI/Swagger UI.
- Runtime and deployment: Terraform-managed Azure Container Apps behind Azure API Management, container image `ghcr.io/pagopa/selfcare-webhook-ms:<image_tag>`, Azure Key Vault-backed secrets, Application Insights Java agent.
- Data stores: Cosmos DB Mongo API database `selcWebhook`, collections `webhooks` and `webhookNotifications`.
- Trust boundaries: APIM-to-service HTTP boundary, JWT-authenticated API callers, outbound webhook calls to externally configured target URLs, MongoDB persistence, Key Vault secrets, scheduler retry processing across replicas.
- Unresolved inputs: endpoint-level authorization policy, webhook target allowlist/denylist and SSRF posture, notification API completion semantics, delete semantics, retry backoff semantics.

## Provisional Security Rules

### HTTP boundary and API contract

- Treat every request body, path parameter, header, and configured webhook URL as untrusted. Validate before persistence, outbound use, or logging.
- Keep OpenAPI in sync with controllers and DTO validation; do not expose undocumented request fields or behavior.
- Require JSON content types for JSON endpoints and reject malformed payloads with safe `4xx` responses.
- Do not return decrypted secrets or sensitive operational details in error responses. Use concise errors and keep details in sanitized logs.
- Keep `DELETE /webhooks/{productId}` fail-closed as `501 Not Implemented` until deletion authorization, retention, and audit semantics are defined.
- Do not rely on APIM alone for application security; service-side validation and authentication checks must still hold.

### Authentication and authorization

- Require valid JWT bearer authentication for non-public APIs unless the endpoint is explicitly classified as public.
- Validate JWTs with configured `JWT_PUBLIC_KEY`; do not accept unsigned tokens, weak algorithms, or caller-provided key material.
- Treat the `uid` claim as identity, not authorization. Roles, scopes, product ownership, and internal-vs-external access rules are TO BE DECIDED.
- Until authorization is defined, fail closed for privileged changes: create, update, delete, list-all, and notification submission must not silently become open endpoints.
- Do not add stateful server sessions; architecture defines stateless bearer-token authentication.

### Webhook target and outbound HTTP security

- Configured webhook URLs are a high-risk SSRF boundary. Before enabling production use, define and enforce allowed schemes, hosts, ports, redirects, DNS behavior, private-network restrictions, and egress policy.
- Do not send webhook requests to local, link-local, metadata, private, or internal service addresses unless an explicit allowlist permits them.
- Prefer HTTPS target URLs. If HTTP is allowed for compatibility, document the risk and restrict it by environment or allowlist.
- Apply connect and read timeouts to all outbound calls and avoid unbounded retries.
- Do not forward caller authentication headers to target endpoints. Only send explicitly configured webhook headers.
- Sanitize target response bodies before storing or logging them as `lastError`; remote endpoints can return attacker-controlled content.

### Input validation and sanitization

- Preserve Bean Validation on required fields: webhook `url`, `productId`, `httpMethod`; notification `productId`, `payload`.
- Validate `httpMethod` against an explicit allowlist instead of passing arbitrary strings to the HTTP client.
- Validate `productId` format and length before using it in queries or logs.
- Limit header names, header values, payload size, URL length, and retry policy bounds. Reject negative, zero, extreme, or nonsensical retry values.
- Sanitize strings before logging, but do not use output encoding as a substitute for validation.

### Secrets, encryption, and sensitive data

- Keep `MONGODB_CONNECTION_STRING`, `JWT_PUBLIC_KEY`, `APPLICATIONINSIGHTS_CONNECTION_STRING`, `SELFCARE_DATA_ENCRIPTION_KEY`, and `SELFCARE_DATA_ENCRIPTION_IV` in Key Vault-backed Container App secrets only.
- Never commit real tokens, keys, connection strings, JWT public/private material, or repository credentials.
- Webhook headers must remain encrypted at rest. Do not log decrypted headers or include them in traces.
- Treat webhook payloads as potentially sensitive. Avoid logging full payloads by default.
- Rotate encryption keys and IVs through secret management; do not hardcode production values in application properties, Dockerfiles, Terraform, or tests.

### Persistence and concurrency

- Use repository APIs and parameterized MongoDB query construction; never build queries by concatenating untrusted strings.
- Preserve unique indexing on `webhooks.productId` unless requirements explicitly allow multiple primary webhooks per product.
- Keep MongoDB processing locks for retry workers; multiple Container App replicas must not process the same notification concurrently.
- Release processing locks on both success and failure paths. Do not swallow lock-release errors silently.
- Maintain TTL on `webhookNotifications` unless retention, audit, and privacy requirements change.

### Logging, monitoring, and error handling

- Logs must be useful for operations without exposing secrets, decrypted headers, bearer tokens, full payloads, or unsanitized target response bodies.
- Include stable identifiers such as product ID, webhook ID, and notification ID only after sanitization.
- Keep Application Insights enabled through the Java agent, but review telemetry capture to avoid leaking request bodies or headers.
- Surface failures explicitly through statuses and logs; avoid broad catch blocks that convert security-relevant failures into success-shaped responses.
- Keep health endpoints operational but do not expose dependency secrets or detailed internals through health responses.

### Deployment, containers, and Terraform

- Keep production ingress `allow_insecure_connections = false`.
- Keep runtime containers non-root (`USER 1001`) and avoid adding shells, package managers, or debug tooling to the runtime image unless justified.
- Keep container images pinned by digest where practical, and scan builder, runtime, MongoDB, MockServer, and Application Insights artifacts for vulnerabilities.
- Do not pass Maven repository credentials or tokens into image layers where they can persist in the final runtime image.
- Use Key Vault-backed secrets for Container App env vars; do not move secrets into plain Terraform variables or app settings.
- Keep Container App probes aligned with Quarkus health endpoints and do not expose debug endpoints through APIM.
- Review APIM policies before publication: authentication, authorization, rate limiting, CORS, request size limits, and logging redaction are currently not defined in architecture.

### Dependency and supply-chain rules

- Follow `ARCHITECTURE.md` dependency rules: prefer zero new dependencies, justify required libraries, use actively maintained stable versions, reject known unpatched CVEs, audit transitive dependencies, and pin versions.
- Do not add security libraries to compensate for missing design decisions. Resolve the design decision first, then choose the smallest suitable dependency if needed.
- Keep Maven, Docker, Terraform, and GitHub Container Registry supply-chain changes reviewable in the PR.

## Selected Prompts

- `Code quality -> Code Quality/00 General Code Quality Prompts (local prompt not found; fallback: OWASP Proactive Controls and project dependency rules in ARCHITECTURE.md)`
- `API security -> Web and API Security/06 Secure API Developer (local prompt not found; fallback: OWASP API Security Top 10 and OWASP REST Security Cheat Sheet)`
- `JWT authentication -> Web and API Security/JWT (local prompt not found; fallback: OWASP JSON Web Token Cheat Sheet)`
- `Authorization -> TO BE DECIDED (ARCHITECTURE.md does not define RBAC, ABAC, ReBAC, OPA, OpenFGA, SpiceDB, Casbin, or Cedar)`
- `Outbound webhooks and SSRF -> Web and API Security/Webhooks + SSRF (local prompt not found; fallback: OWASP SSRF Prevention Cheat Sheet and OWASP API Security Top 10)`
- `Input validation -> Web and API Security/Input Validation (local prompt not found; fallback: OWASP Input Validation Cheat Sheet)`
- `Secret handling -> Deployment and Infrastructure/Secret Management (local prompt not found; fallback: OWASP Secrets Management Cheat Sheet)`
- `Backend framework -> Quarkus (local prompt not found; fallback: Quarkus security best-practice defaults plus OWASP API Security Top 10)`
- `Persistence -> MongoDB / Cosmos DB Mongo API (local prompt not found; fallback: OWASP Injection Prevention Cheat Sheet and MongoDB least-privilege/query-safety defaults)`
- `Deployment / infra -> Azure Container Apps + Terraform + APIM under /infra/resources/webhook (local prompt not found; fallback: Azure Container Apps, APIM, Terraform, and container hardening safe defaults)`
- `Logging and monitoring -> Observability / Application Insights (local prompt not found; fallback: OWASP Logging Cheat Sheet)`
- `CI/CD and supply chain -> Deployment and Infrastructure/CI-CD Security (local prompt not found; fallback: OWASP CI/CD Security Cheat Sheet and dependency rules in ARCHITECTURE.md)`
