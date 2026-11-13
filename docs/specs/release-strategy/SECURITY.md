# RELEASE_STRATEGY - Security

Sources of truth: [REQUIREMENTS.md](./REQUIREMENTS.md) (`SELC-x.y`) and [ARCHITECTURE.md](./ARCHITECTURE.md) (C1-C7). These are provisional implementation rules, not a security audit or evidence that controls are already enforced.

**Prompt library status:** no `PROMPT.md` files were found in the repository, and no library location/index is available in this task's context. Existing repository security specifications also report the library as unavailable. No library prompt was selected or read; the mappings below explicitly use public OWASP Cheat Sheet fallbacks. Repository paths are implementation context, not prompt directories. Re-resolve these mappings when the library is supplied.

**Threat profile:** a release control plane using GitHub Actions, Maven, the container registry, Terraform and Azure runtimes, with a read-only HTML overview. This is not a new business API. Prioritize pipeline execution, artifact substitution, privilege escalation, destination isolation, falsified deployment state, secret leakage and unsafe reporting. No AI/agent components or new application sessions are proposed.

## Required Security Inputs

- Known stack: GitHub Actions; Maven; GitHub container registry; Docker images deployed to Azure Container Apps; Azure Functions packages; Terraform; existing Python-generated HTML status page. Existing Quarkus/Spring/Functions application authentication remains unchanged.
- Trust boundaries: reviewed source to publisher; publisher to artifact store; approver to deployer; deployer to Azure destination; observer to runtime; registry writers to versioned records; records to browser rendering.
- Identity model: GitHub access controls and production environment approvals; Azure OIDC for existing deployment automation; managed identities and Key Vault for existing runtime configuration.
- Identity/authorization slots: exact federated trust subjects, Azure role scopes, registry-writer credentials and runner network placement are **UNRESOLVED - TO BE DECIDED**.
- Registry slot: dedicated branch versus separate deployment repository, schema, write-conflict protocol and history retention are **UNRESOLVED - TO BE DECIDED**.
- Functions artifact slot: persistent storage, package deployment and runtime checksum/provenance verification are **UNRESOLVED - TO BE DECIDED**.
- Observer slot: implementation language/framework, query interfaces, schedule, freshness limit and notification channel are **UNRESOLVED - TO BE DECIDED**.
- Reporting slot: final integration, audience and reader authentication are **UNRESOLVED - TO BE DECIDED**. Do not assume the release inventory is public.
- Release policy slots: immutability enforcement, rollback retention, destination-specific UAT evidence and compatibility/smoke-test gates are **UNRESOLVED - TO BE DECIDED**.
- Sensitive data: credentials, secret values and application data are forbidden in release records. Actor identifiers and operational metadata are required for auditability but must not be published to an unapproved audience.

## Provisional Security Rules

Rules derive from the requirements and the named fallback sources. Unresolved slots require a reviewed decision before their boundary is enabled; they are not permission to bypass a control.

### 1. Pipeline execution and approvals

Sources: [OWASP CI/CD Security][cicd]; repository context: `.github/workflows/call_release_docker.yml`, `.github/workflows/release_app.yml`. Traceability: C1/C3/C7; SELC-1.4-1.6, 2.5, 2.7, 4.2.

- **SEC-1.1** Protect version changes, release tags, workflow code and deployment configuration with review and access controls. Validate the selected source commit and artifact against the approved request; approval of a branch name alone does not authorize later changes to that branch.
- **SEC-1.2** Never run untrusted pull-request code, scripts, artifacts or caches in a context holding publication/deployment credentials. Separate untrusted checks from privileged release jobs and isolate their execution environments.
- **SEC-1.3** Grant GitHub permissions per job, not broadly across the workflow. Pin third-party actions and reusable workflows to reviewed immutable commit references; updates require review. Do not extend mutable-reference exceptions.
- **SEC-1.4** Require production approval and successful UAT evidence for the exact artifact and intended destination. Rollbacks retain approval checks; composition dispatch cannot bypass component gates or treat child-run creation as completion.

### 2. Authentication and destination authorization

Sources: [OWASP Authorization][authorization], [OWASP CI/CD Security][cicd]; repository context: `infra/resources/_modules/container_app_microservice/`, `infra/resources/_modules/functions/`. Traceability: C1/C3-C6; SELC-2.5, 3.10, 4.2.

- **SEC-2.1** Use short-lived federated Azure credentials for automation where supported. Scope federation to trusted repository/workflow environments and verify issuer, audience and subject through the identity configuration; do not broaden trust to arbitrary branches or contributors.
- **SEC-2.2** Separate publisher, deployer, observer and registry-writer privileges. The observer reads Azure runtime state but cannot deploy; its permission to write observations does not authorize changing desired state. The reporting process gets no deployment privileges.
- **SEC-2.3** Authorize every operation against the full `(service, environment, destination)` key. Resolve resource IDs and credentials from trusted destination configuration, not caller-supplied subscription IDs, resource groups or arbitrary URLs. A valid input is not proof of authorization.
- **SEC-2.4** Limit Azure and GitHub grants to required resources and operations. Do not grant subscription-wide Owner or broad secret-read access to simplify implementation. Exact role assignments and registry-write isolation remain TO BE DECIDED.

### 3. Artifact integrity, dependencies and rollback

Sources: [OWASP CI/CD Security][cicd], [OWASP Vulnerable Dependency Management][dependencies]; repository context: `apps/product/pom.xml`, `apps/product/Dockerfile`, ARCHITECTURE.md Dependency Rules. Traceability: C1-C3; SELC-1.1-1.9, 2.1-2.4, 4.1-4.6.

- **SEC-3.1** Bind service/version to the full source commit, trusted build run and immutable image digest or package checksum. Compare integrity against protected publication metadata, not a checksum supplied alongside an untrusted artifact. A matching digest alone does not establish a trusted publisher.
- **SEC-3.2** Publish each release once; reject version/tag replacement and snapshot parents/dependencies. Promote the exact UAT artifact without rebuilding. Restrict publish/delete permissions and preserve active and designated rollback artifacts.
- **SEC-3.3** Apply ARCHITECTURE.md dependency rules to direct/transitive libraries and build tooling; use approved repositories, exact versions and integrity checks. Scan dependencies and images before publication and re-evaluate known vulnerabilities before promotion or rollback. Retention does not make an old artifact safe.
- **SEC-3.4** Block unavailable, untrusted or incompatible rollback targets with an explicit reason. Do not roll back secret rotations or destructive data/APIM changes implicitly. Functions storage, integrity verification and retention enforcement must be resolved before enabling package promotion.

### 4. Input validation and command boundaries

Sources: [OWASP Input Validation][validation], [OWASP CI/CD Security][cicd]. Traceability: C1/C3-C5/C7; SELC-1.4, 3.1-3.4, 4.3, 5.1-5.4.

- **SEC-4.1** Allow-list service/environment/destination combinations. Validate versions, full commit IDs, artifact references and composition entries structurally and semantically before any write or deployment. Bound record size, nesting and composition length; reject duplicate destinations and invalid dependency ordering.
- **SEC-4.2** Treat workflow inputs, Git metadata, manifest values and API responses as untrusted. Pass validated values as data through argument arrays or quoted environment variables; never interpolate raw values into shell source, use `eval`, or derive executable code from records.
- **SEC-4.3** Use data-only parsers and serializers, not executable configuration. Constrain paths to approved working directories. If packages are extracted, reject absolute paths, traversal and links escaping the extraction root.
- **SEC-4.4** Restrict outbound queries/downloads and credential-bearing requests to configured trusted endpoints. Validate TLS; reject unexpected redirects or hosts rather than forwarding credentials. Use bounded timeouts/retries and never accept a caller-selected URL as a runtime observation authority.

### 5. Secrets, containers and Terraform

Sources: [OWASP Secrets Management][secrets], [OWASP Docker Security][docker], [OWASP Infrastructure as Code Security][iac]. Repository context: `infra/resources/_modules/container_app_microservice/`, `infra/resources/_modules/functions/`. Traceability: C2/C3/C5; SELC-2.6, 4.7, 4.8.

- **SEC-5.1** Use existing secret-management boundaries and identity-backed runtime references. Never put secrets in source, manifests, inventory, image layers, build arguments or generated HTML. Supply build credentials through ephemeral secret mounts; prevent leakage into caches, artifacts and logs.
- **SEC-5.2** Keep containers patched, use reviewed digest-pinned base images and non-root runtime users, and avoid unnecessary privileges or Docker socket access. Restrict private-network runner access to trusted jobs; do not expose private runtime endpoints merely to simplify verification.
- **SEC-5.3** Review and scan Terraform changes; pin external module revisions and provider selections. Protect state and plan artifacts with access controls and encryption: values read from Key Vault can still enter Terraform state. Never publish raw state or plans in the inventory.
- **SEC-5.4** Bind plan/apply to the reviewed destination and artifact. Maintain one image-reference owner across normal deploy and rollback, so Terraform cannot silently restore a different image. Fail on identity/destination mismatches rather than defaulting to `latest` or another environment.

### 6. Inventory integrity, logging and failures

Sources: [OWASP Logging][logging], [OWASP Authorization][authorization], [OWASP Infrastructure as Code Security][iac]. Traceability: C3-C5/C7; SELC-3.2-3.8, 3.10, 3.11, 5.4, 5.5.

- **SEC-6.1** Preserve separation between approved intent, observations and attempt history. Only verified runtime identity and smoke-test results establish successful deployment. An untrusted workflow payload or version variable cannot assert what is running.
- **SEC-6.2** Serialize deploy/rollback per destination and protect shared-registry writes against lost updates. Bind observations to their target and observation time; delayed jobs must not overwrite newer verified state. Conflict detection/recovery mechanism: TO BE DECIDED.
- **SEC-6.3** Record actor, destination, previous/requested identities, workflow correlation, timestamps and outcome. Restrict history modification/deletion and sanitize untrusted text against log injection. Never log tokens, authorization headers, secret values, raw configuration dumps or application payloads.
- **SEC-6.4** Report query, authorization, validation, smoke-test and persistence failures explicitly. Retain the last verified observation with freshness/unknown indicators; never substitute healthy defaults. A registry failure after rollout is not a rollback: reconcile runtime state and expose partial component outcomes.

### 7. HTTP reporting boundary

Sources: [OWASP Cross-Site Scripting Prevention][xss], [OWASP Authorization][authorization], [OWASP Secrets Management][secrets]; repository context: `.github/status-page/generate.py`. Traceability: C6; SELC-3.9.

- **SEC-7.1** Escape all dynamic values for their HTML/attribute context, including service labels, actors, errors and release metadata. Validate link schemes and destinations separately; HTML escaping alone does not make an unsafe URL safe. Do not render untrusted HTML or scripts from release records.
- **SEC-7.2** Keep the page read-only and free of tokens or privileged browser calls. Resolve audience/authentication before publishing inventory; a private source repository does not by itself prove that generated pages or build artifacts are private.
- **SEC-7.3** No new business API, session implementation or APIM policy is specified. Do not add JWT, password, CORS or WAF machinery to this release feature by assumption. If an inbound API, webhook or interactive deployment UI is introduced, revisit authentication, authorization, CSRF, rate limits and API validation before exposing it.

### 8. Code-quality security baseline

Sources: [OWASP Input Validation][validation], [OWASP Vulnerable Dependency Management][dependencies], [OWASP CI/CD Security][cicd]; repository context: `apps/` service-specific POMs and ARCHITECTURE.md Dependency Rules.

- **SEC-8.1** Preserve each application's existing build/test conventions; do not infer a new Quarkus or Spring control-plane service. Keep validation, serialization and error handling explicit and testable; prefer first-party/standard-library code over new dependencies.
- **SEC-8.2** Cover rejected destinations and malformed manifests, artifact/version mismatches, unauthorized operations, stale/concurrent writes, partial child failures, secret redaction and HTML/URL encoding with focused tests. Integration verification must prove that PROD promotion reuses the UAT artifact and rollback does not rebuild.

## Selected Prompts

No prompt directories are asserted below: every resolved guidance mapping is an **OWASP fallback because the library is unavailable**. The cited repository paths provide stack context only.

- Code quality -> `apps/` service-specific build conventions + [Input Validation][validation] and [Vulnerable Dependency Management][dependencies] (fallback).
- CI/CD and artifact supply chain -> `.github/workflows/` + [CI/CD Security][cicd] (fallback).
- Authentication: GitHub/Azure federation -> `.github/workflows/call_release_docker.yml` + [CI/CD Security][cicd] and [Secrets Management][secrets] (fallback); exact trust configuration **UNRESOLVED - TO BE DECIDED**.
- Authorization: Azure/GitHub permissions -> `infra/resources/_modules/container_app_microservice/`, `infra/resources/_modules/functions/` + [Authorization][authorization] (fallback); exact roles and registry credentials **UNRESOLVED - TO BE DECIDED**.
- Containers / Azure Container Apps -> `infra/resources/_modules/container_app_microservice/` + [Docker Security][docker] (closest fallback; no Azure-specific prompt available).
- Azure Functions packages -> `infra/resources/_modules/functions/` + [CI/CD Security][cicd] (closest fallback); storage/deployment/identity-verification slot **UNRESOLVED - TO BE DECIDED**.
- Terraform -> `infra/resources/` + [Infrastructure as Code Security][iac] (fallback).
- Secret handling / Key Vault -> `infra/resources/_modules/container_app_microservice/`, `infra/resources/_modules/functions/` + [Secrets Management][secrets] (fallback).
- Input validation / outbound integration safety -> [Input Validation][validation] and [CI/CD Security][cicd] (fallback); concrete runtime query interfaces **UNRESOLVED - TO BE DECIDED**.
- Logging, audit and failure handling -> [Logging][logging] + REQUIREMENTS.md SELC-3 (fallback); notification channel and retention **UNRESOLVED - TO BE DECIDED**.
- HTTP reporting / generated HTML -> `.github/status-page/` + [Cross-Site Scripting Prevention][xss] (fallback); reader authentication and final integration **UNRESOLVED - TO BE DECIDED**.
- Backend framework -> existing Quarkus/Spring/Functions workloads retain their own controls; no new HTTP server selected. Observer framework slot **UNRESOLVED - TO BE DECIDED**; no framework prompt can be selected.
- API security / APIM, client JWT, session/password/MFA implementation, webhooks, uploads -> no new surface specified; not selected for this feature. Existing application controls remain unchanged; outbound and HTML boundaries are covered above.
- AI / agent security -> not applicable; no AI components in ARCHITECTURE.md.

[cicd]: https://cheatsheetseries.owasp.org/cheatsheets/CI_CD_Security_Cheat_Sheet.html
[authorization]: https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html
[secrets]: https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html
[validation]: https://cheatsheetseries.owasp.org/cheatsheets/Input_Validation_Cheat_Sheet.html
[xss]: https://cheatsheetseries.owasp.org/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.html
[logging]: https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html
[docker]: https://cheatsheetseries.owasp.org/cheatsheets/Docker_Security_Cheat_Sheet.html
[dependencies]: https://cheatsheetseries.owasp.org/cheatsheets/Vulnerable_Dependency_Management_Cheat_Sheet.html
[iac]: https://cheatsheetseries.owasp.org/cheatsheets/Infrastructure_as_Code_Security_Cheat_Sheet.html
