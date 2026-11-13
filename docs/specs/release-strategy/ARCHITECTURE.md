# RELEASE_STRATEGY - Architecture

Primary source of truth: [REQUIREMENTS.md](./REQUIREMENTS.md). All requirement IDs below refer to that document. This is an intended architecture, not a description of an implemented migration.

## Required Architecture Inputs

- Requirements source: REQUIREMENTS.md
- System purpose: independent Maven service versions, immutable artifact promotion and a trustworthy deployment inventory supporting individual and coordinated releases and rollbacks without splitting the monorepo.
- Primary use cases: reviewed version updates; artifact publication; UAT validation and production promotion; runtime reconciliation and drift reporting; rollback to retained artifacts; coordinated onboarding releases.
- Target users / actors: developers, release approvers, deployment operators, GitHub Actions workflows and the runtime reconciliation process.
- Runtime environment: existing Azure Container Apps workloads and Azure Functions across DEV/UAT/PROD and applicable AR/PNPG destinations. The shared [Container App module](../../../infra/resources/_modules/container_app_microservice/container_app.tf) currently uses single-revision mode. The [Functions module](../../../infra/resources/_modules/functions/function.tf) provisions Linux hosting, Java 17, Functions runtime `~4`, VNet integration and a private endpoint. CI/CD uses GitHub Actions; reconciliation execution placement and runner network access are TO BE DECIDED.
- Server framework: no new HTTP server is required by the release requirements. Existing deployable applications retain their own Quarkus, Spring and Azure Functions stacks; this strategy does not standardize them. Provisional control plane: GitHub Actions workflows and first-party automation, not a new application service. Reconciliation implementation language/framework: TO BE DECIDED.
- Client framework: extend the existing status page; its [generator](../../../.github/status-page/generate.py) produces HTML using Python. No new SPA framework is selected. Final reporting integration and page access policy: TO BE DECIDED; infrastructure does not establish a new client framework for this feature.
- API style and integration model: workflow-driven release operations, Maven dependency resolution, container registry publication/pull, Azure deployment and runtime observation, and versioned deployment records. No new public business API is required. Runtime query interfaces and Functions package deployment/identity verification mechanism: TO BE DECIDED.
- Authentication and session model: operators retain GitHub access controls and protected-environment approvals; no new application login or session store is proposed. Existing deployment workflows use GitHub credentials and Azure OIDC configuration; application infrastructure uses managed identities and Key Vault references. Exact publisher/deployer/observer/registry-writer permissions and status-page reader authentication: TO BE DECIDED. Runtime application authentication remains unchanged.
- Data model expectations: immutable service release identity `(service, Maven version, source commit, artifact digest/checksum)`; destination key `(service, environment, ar|pnpg)`; separate desired state, timestamped observed state and deployment history; composition manifests with independent component versions. Records include configuration/infrastructure/APIM references, actors, outcomes and workflow links; observations support multiple revisions and traffic weights. Registry location, schema and concurrency mechanism: TO BE DECIDED.
- Deployment model: publish once, validate in UAT, approve and promote the same artifact to PROD. Terraform remains the existing infrastructure mechanism; its image-reference interface must accept digests. Functions use the same retained package across promotions. Exact Functions artifact storage and deployment mechanism: TO BE DECIDED. No new hosting platform is selected.
- Scale expectations: one inventory entry per deployed service/environment/destination, potentially containing several active revisions, plus one history entry per deployment attempt. Existing infrastructure configures application capacity per service; those replica settings do not size the release control plane. Deployment rate, history size/retention, reconciliation interval, freshness threshold and control-plane capacity targets: TO BE DECIDED.
- Security expectations: preserve production approvals and access controls; prevent release/tag replacement; verify artifact identity; reject snapshot release graphs; isolate destination writes; retain audit history and rollback artifacts; keep credentials, secret values and application data out of records and reporting. Separate publication, deployment and observation permissions; exact role assignments and reporting visibility: TO BE DECIDED.
- AI / agent components: none required or proposed. CI runners and workflow automation are not AI agents.

## Initial Architecture (Provisional)

### Boundaries and flow

1. **C1 Release preparation and publisher.** Review a service-scoped Maven version change, validate its effective dependency graph and `<service>/v<version>` tag, then run build/quality checks and publish an immutable release. Shared libraries and parents keep separate lifecycles. Publication records provenance but never declares deployment success.
2. **C2 Artifact retention boundary.** Store container images in the existing GitHub container registry and identify deployments by full digest-pinned references. Retain Functions packages with checksums in persistent storage, whose location is TO BE DECIDED. Protect active and designated rollback artifacts from cleanup; retention duration and enforcement are TO BE DECIDED.
3. **C3 Promotion and rollback executor.** Accept a destination and existing release identity, verify eligibility, serialize operations on that destination, and apply through its supported deployment path. PROD requires UAT evidence and approval. Rollback uses this same executor with a compatible retained artifact; it does not rebuild or automatically reverse data/configuration/APIM changes. Proposed image owner: Terraform consumes the selected digest for both promotion and rollback, avoiding an independent out-of-band image writer. Exact configuration-revision handoff and Functions path are TO BE DECIDED.
4. **C4 Versioned deployment registry.** Keep desired state, observations and historical attempts logically separate. Desired state records intent; only runtime evidence updates observations. Each attempt records previous observed identity, requested identity, actor, references, timestamps and outcome. Storage choice remains dedicated branch versus separate deployment repository, TO BE DECIDED; no database is selected. Write coordination must prevent lost updates across services as well as races within one destination.
5. **C5 Runtime verifier and reconciler.** After deployment, verify artifact identity and smoke-test results before marking success. Periodically observe Azure independently of workflow outcomes; compare active identities to desired state and record drift, health, freshness and traffic allocation. Query failures retain the last verified observation with an explicit stale/unknown status. A failed smoke test or registry write does not imply runtime rollback: record the failure and reconcile actual state. Scheduling, permissions, queries and service-specific checks are TO BE DECIDED.
6. **C6 Read-only release overview.** Extend the existing status page to project C4's desired state, observed versions, health, drift, latest attempts and history. Show observation timestamps and all traffic-serving revisions. Workflow links provide evidence, not a substitute for runtime identity. Page rendering must not trigger deployment or obtain deployment credentials.
7. **C7 Composition coordinator.** Resolve an optional versioned manifest into individual C3 operations, including separate onboarding MS, CDC and Functions entries and explicit destinations. Respect declared ordering/compatibility and await child workflow completion. Persist per-component outcomes; neither release nor rollback is atomic across services. Manifest schema and compatibility policy are TO BE DECIDED.

```text
Reviewed Maven version + service tag
  -> C1 publisher -> C2 immutable artifact
  -> C3 UAT deployment -> C5 verification
  -> approval -> C3 PROD deployment of the same artifact -> C5 verification

C3 attempts + C5 observations -> C4 registry -> C6 overview
C4 desired state + Azure runtime -> C5 periodic reconciliation -> C4
C7 composition -> ordered C3 operations -> per-component outcomes in C4
Rollback -> C3 selecting a compatible retained C2 artifact, without rebuilding
```

### Assumptions and unresolved decisions

- **A1 Control plane:** reuse GitHub Actions and the existing status page rather than introduce a new release-management service. This is provisional; reconciliation placement and reporting access remain TO BE DECIDED.
- **A2 Authority:** published artifacts identify releases, desired state identifies approved intent, and timestamped runtime observations identify what was seen running. No one of these alone proves a healthy current deployment.
- **A3 Revision compatibility:** the inspected Container App module is currently single-revision. The inventory still supports multiple traffic-serving revisions as required; this proposal does not change the runtime revision mode.
- **A4 Recovery:** deploy, registry persistence and runtime observation are not one transaction. Explicit failure records and reconciliation recover visibility; missing observations are never interpreted as an empty or healthy environment.
- **Open implementation gates:** registry schema/location and write coordination; artifact/tag immutability enforcement; Functions storage and package identity verification; retention/rollback windows; reconciliation frequency and notification channel; smoke tests and destination-specific promotion evidence; compatibility rules; least-privilege permissions and page audience. All are TO BE DECIDED.

### Migration boundaries

- **Phase 1 - Baseline:** introduce C4/C5 to observe existing production deployments. Preserve legacy version labels only as history; represent provenance gaps explicitly.
- **Phase 2 - Product pilot:** introduce C1/C2/C3 and C6 for `product`, including the Terraform digest interface. Demonstrate UAT promotion/rollback without rebuilding before approved production promotion.
- **Phase 3 - Extension:** migrate remaining services, multimodule applications and Functions; add C7 where coordinated releases are needed. Retire branch-derived operational versions only after equivalent capabilities are verified, retaining one authoritative deployment path per migrated destination.

## Requirement Traceability

| Component / boundary | Requirements | Needs more architecture input |
|---|---|---|
| C1 service-scoped publisher | SELC-1.1-1.9; SELC-2.1, 2.7, 2.8 | Initial service versions, snapshot remediation, tag/version immutability enforcement: TO BE DECIDED |
| C2 immutable artifacts and retention | SELC-2.2-2.4; SELC-4.1, 4.6 | Functions storage, retention duration and cleanup protection: TO BE DECIDED |
| C3 promotion/rollback and Terraform ownership | SELC-2.4-2.6; SELC-3.10; SELC-4.1-4.5, 4.7, 4.8 | UAT evidence policy, compatibility gates, configuration handoff, Functions deployment, destination locking: TO BE DECIDED |
| C4 registry and history | SELC-3.1-3.6, 3.10, 3.11 | Location, schema, conflict handling and history retention: TO BE DECIDED |
| C5 verification and reconciliation | SELC-3.4-3.8, 3.11 | Execution placement, runtime identity queries, smoke tests, scheduling, freshness and alerting: TO BE DECIDED |
| C6 overview and reader boundary | SELC-3.9; required inputs: privacy/access controls | Reporting integration, audience and reader authentication: TO BE DECIDED |
| C7 composition coordinator | SELC-5.1-5.5 | Manifest schema, ordering/compatibility policy and child-run correlation: TO BE DECIDED |
| Publisher/deployer/observer identities | Required inputs: authentication/security; SELC-2.5, 2.6; SELC-4.2 | Exact credential scopes, role assignments and runner network reachability: TO BE DECIDED |
| Incremental migration | Migration Phases 1-3 and Acceptance Criteria | Legacy mappings, observation gaps and per-service cutover readiness: TO BE DECIDED |

## Dependency Rules

- Do not add a dependency when the standard library or a few lines of first-party code
will do.
- Prefer zero new dependencies. If a library is required, justify it in the PR
description.
- Only use libraries that are actively maintained (commit or release within the last 12
months).
- Only use the latest stable major version. No deprecated, abandoned, or pre-release
packages.
- Reject any library with known unpatched CVEs. Check before adding and on every update.
- Audit transitive dependencies, not just direct ones. A small direct dep with a large or
unvetted tree is a rejection.
- Pin exact versions with a committed lockfile. No floating ranges in production.
- Prefer libraries with a narrow scope, minimal dependencies of their own, and a clear
security track record.
