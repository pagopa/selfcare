# RELEASE_STRATEGY - Requirements

Independent Maven versioning for Selfcare microservices, immutable artifact promotion and a traceable inventory of deployed versions, supporting both individual and coordinated releases and rollbacks.

Status: proposed target strategy. The migration phases below are planned, not implemented. Current-state observations refer to repository configuration, not a live production assessment.

## Problem

The current release identity is derived from `releases/<domain>_vX.Y.Z` branches rather than the effective Maven version of each service. Application POMs can retain snapshot versions while release branches advance independently.

The Docker release workflow builds an image for each deployment and deploys a short Git SHA tag. Rebuilding the same commit does not guarantee that UAT and PROD receive identical artifacts.

Repository variables named `<DOMAIN>_CURRENT_<ENV>_VERSION` do not distinguish AR and PNPG. The release-branch creation workflow updates the UAT variable before deployment. These variables therefore cannot be treated as authoritative evidence of the running version.

The onboarding release groups several independently deployed components under one version. A domain-level version cannot represent partial deployments or differences between component versions.

The existing status page derives deployment information from workflow runs. Operators also need an inventory of artifacts actually running, distinct from the latest deployment attempt.

## Required Requirement Inputs

- Project purpose: migrate Selfcare release management to independent Maven service versions without splitting the monorepo.
- Primary users / actors: developers preparing releases; release approvers; operators deploying, inspecting production and performing rollbacks; CI/CD workflows.
- Core workflows: service version update, immutable artifact publication, UAT validation, production promotion, runtime reconciliation, rollback and coordinated releases.
- Business objects / data entities: service release, artifact digest or checksum, deployment destination, desired state, observed state, deployment history and composition manifest.
- Scope: deployable applications under `apps/`, including multimodule Spring applications and Azure Functions, across DEV, UAT and PROD and the AR/PNPG destinations where each application exists.
- External integrations: Maven repositories, GitHub Actions, GitHub releases and container registry, Azure Container Apps, Azure Functions, Terraform and the existing status page.
- Authentication / roles: production deployment and rollback MUST retain protected-environment approvals and existing access controls.
- Privacy and configuration constraints: release records MUST NOT contain credentials, tokens, secret values or application data. Configuration references MUST NOT expose secret contents.

## Functional Requirements

### 1. Independent Maven versions

- **SELC-1.1** Each deployable service MUST own its Maven version independently of other services and of shared parent POM versions.
- **SELC-1.2** Modules belonging to one deployable application MUST remain version-aligned within that application. Updating one application MUST NOT require a version bump of unrelated services.
- **SELC-1.3** Production release versions MUST use `MAJOR.MINOR.PATCH`. Development versions MAY use the corresponding `-SNAPSHOT` suffix.
- **SELC-1.4** A release MUST originate from a reviewed version-update commit. CI MUST read the effective Maven version and verify that it matches the service-specific Git tag and artifact metadata.
- **SELC-1.5** Release tags MUST identify both service and version, using `<service>/v<version>`, for example `product/v1.4.0`. The tag identifies a repository commit but does not release other services from that commit.
- **SELC-1.6** A published service version and its release tag MUST NOT be overwritten or reassigned. Fixes after publication MUST produce a new version.
- **SELC-1.7** Release builds MUST reject snapshot application versions, snapshot parents and snapshot dependencies, including transitive dependencies. Existing snapshot dependencies MUST be resolved before a service adopts the new release flow.
- **SELC-1.8** Version-update automation SHOULD use Maven tooling scoped to the service and its modules. It MUST NOT impose a single release version on the entire reactor.
- **SELC-1.9** Shared libraries and parent POMs MUST retain their own version lifecycle. Services MUST explicitly select their dependency versions; a library change MUST NOT silently alter an already published service artifact.

### 2. Build once and promote

- **SELC-2.1** A service release MUST produce an immutable deployable artifact after the required build and quality checks. Publication MUST record service name, Maven version, full source commit, build workflow and artifact identity.
- **SELC-2.2** Container images MUST expose a readable version tag, but deployments MUST use the full image reference pinned by digest. `latest`, branch tags and short SHA tags MUST NOT be the identity used to promote or roll back a release.
- **SELC-2.3** Azure Functions releases MUST publish a versioned deployment package with a checksum to persistent artifact storage. Promotion and rollback MUST reuse that package rather than rebuild it.
- **SELC-2.4** UAT and PROD MUST receive the same published artifact for a service release. Promotion between environments or AR/PNPG destinations MUST NOT rebuild the artifact.
- **SELC-2.5** Production promotion MUST require successful UAT validation of the selected artifact and the configured production approval. A source change after validation MUST produce a new release and repeat validation.
- **SELC-2.6** Environment configuration and secrets MUST remain external to the deployable artifact. Deployment records MUST identify the configuration revision and relevant infrastructure/APIM revisions without storing secret values.
- **SELC-2.7** Development auto-deployment MAY continue independently, but development builds MUST NOT overwrite immutable release artifacts or bypass promotion controls.
- **SELC-2.8** Publishing an artifact MUST NOT mark it as deployed. Artifact creation, deployment requests and verified deployment outcomes MUST be distinct events.

Target release flow:

```text
Reviewed service version update
  -> service-specific Git tag
  -> build and quality checks
  -> immutable artifact publication
  -> UAT deployment and validation
  -> production approval
  -> PROD deployment of the same artifact
  -> runtime verification and inventory update
```

### 3. Deployment inventory and history

- **SELC-3.1** The system MUST maintain a versioned deployment registry keyed by service, environment and destination (`ar` or `pnpg`). A dedicated branch in the existing repository MAY host the initial registry.
- **SELC-3.2** The registry MUST distinguish desired state, observed runtime state and deployment history. A merged deployment request MUST update desired state, not assert successful deployment.
- **SELC-3.3** Each deployment record MUST include the destination, requested version and artifact identity, source commit, previous observed artifact, configuration references, initiating actor, timestamps, workflow link and outcome.
- **SELC-3.4** Observed state MUST include the artifact actually active, its Maven version when verifiable, Azure revision or package identity, health and last observation timestamp. Unknown values MUST be explicit rather than inferred from the latest release branch.
- **SELC-3.5** A deployment MUST be marked successful only after runtime identity verification and smoke tests. Failed, cancelled, pending and partially completed operations MUST remain distinguishable from success.
- **SELC-3.6** Failed verification MUST NOT report the requested version as successfully deployed. The registry MUST preserve the last verified observation, mark its freshness and record any verified partial runtime changes.
- **SELC-3.7** Periodic reconciliation MUST compare the registry with Azure runtime state, detect out-of-band changes and expose drift, stale observations and unavailable runtime queries. A query failure MUST NOT appear as a healthy or empty environment.
- **SELC-3.8** Where several Container App revisions receive traffic, observed state MUST list their artifact identities and traffic allocation rather than report a single version.
- **SELC-3.9** The existing status page SHOULD be extended to display desired and observed versions, artifact identities, health, observation freshness, drift and deployment history for each service/environment/destination. The latest attempted deployment MUST remain distinct from the active release.
- **SELC-3.10** Deployment and rollback operations targeting the same service/environment/destination MUST be serialized. Concurrent registry updates for different services MUST NOT overwrite each other's changes.
- **SELC-3.11** Failure to persist a deployment outcome MUST be reported explicitly. Reconciliation MUST allow recovery of the actual runtime state without falsely asserting that a successful runtime change was undone.

### 4. Rollback and infrastructure consistency

- **SELC-4.1** Operators MUST be able to select a previously published and retained artifact for a specific service/environment/destination. Rollback MUST NOT rebuild source code or depend on a mutable branch.
- **SELC-4.2** Rollback MUST use the deployment approval, serialization, runtime verification and history mechanisms used for normal releases. It MUST create a new history entry and MUST NOT erase the original deployment.
- **SELC-4.3** Before execution, rollback MUST verify artifact availability and compatibility with current data, configuration, dependent services and APIM contracts. Unsupported rollback MUST be rejected with an explicit reason.
- **SELC-4.4** Application rollback MUST NOT imply automatic reversal of database migrations, secret rotations, infrastructure or APIM changes. Destructive or incompatible changes MUST include a separate recovery plan.
- **SELC-4.5** Release changes SHOULD preserve backward compatibility through expand-and-contract migrations so that the previous application version remains usable during the rollback window.
- **SELC-4.6** Artifact retention MUST cover the agreed rollback window. Artifacts used by active deployments or designated rollback targets MUST be protected from cleanup.
- **SELC-4.7** The Terraform Container App module MUST support a full digest-pinned image reference rather than constructing every image reference from a tag.
- **SELC-4.8** Application deployment and Terraform MUST have one consistent ownership model for the image reference. A rollback MUST NOT leave an unmanaged image change that a subsequent Terraform apply silently reverses.

### 5. Coordinated releases

- **SELC-5.1** A coordinated release MAY group independently versioned services in a versioned composition manifest. Each entry MUST identify the service release and immutable artifact; the composition identifier MUST NOT replace service Maven versions.
- **SELC-5.2** Onboarding compositions MUST represent `onboarding-ms`, `onboarding-cdc` and `onboarding-functions` separately, with explicit deployment destinations.
- **SELC-5.3** A composition MUST identify required deployment ordering and compatibility constraints where relevant. It MUST NOT assume that changing several services is atomic.
- **SELC-5.4** The release overview MUST show the outcome of every component deployment, including Functions workflows. Dispatching a child workflow MUST NOT count as successful completion.
- **SELC-5.5** A coordinated rollback MUST select a compatible composition and record each component outcome. Partial success MUST remain visible and MUST NOT be collapsed into a single successful domain version.

## Migration Phases

### Phase 1. Baseline inventory

Create a trustworthy starting point before changing release mechanics.

- Inventory artifacts actually active in PROD for every deployed service and AR/PNPG destination, including active revisions and Functions packages.
- Record legacy branch/domain versions as historical references only. Do not invent a correspondence between legacy versions and Maven versions.
- Identify missing artifact provenance, snapshot parents/dependencies and artifacts that cannot currently be recovered for rollback.
- Establish the registry's desired/observed/history model and runtime observation mechanism while existing deployment workflows remain operational.
- Exit criteria: every production deployment has a recorded observation or an explicit unresolved gap; operators can distinguish legacy metadata from verified runtime identity.

### Phase 2. Pilot on product

Validate the end-to-end strategy on one service before extending it.

- Make the `product` Maven version authoritative and introduce reviewed version updates and service-specific release tags.
- Separate artifact publication from deployment; deploy the same image digest without rebuilding.
- Adapt the Terraform image reference, record deployment outcomes and expose them in the status page.
- Exercise promotion and rollback in UAT, including failure handling, drift detection and inventory reconciliation.
- Promote the validated artifact to PROD through the approval process and verify the resulting inventory for each applicable destination.
- Exit criteria: the pilot satisfies the applicable acceptance criteria below, including a demonstrated UAT rollback using a retained artifact and no rebuild.

### Phase 3. Extend and retire the legacy strategy

Migrate remaining services incrementally, retaining one authoritative deployment path per migrated destination.

- Extend the pilot flow to other Quarkus applications, multimodule Spring applications and Azure Functions.
- Resolve each service's snapshot dependency blockers and preserve independent library/parent versioning.
- Introduce composition manifests for coordinated releases such as onboarding without forcing shared service versions.
- Preserve historical mappings and keep non-migrated services observable during coexistence.
- After equivalent promotion, inventory and rollback capabilities are verified, retire version derivation from release branches and use the registry instead of `CURRENT_<ENV>_VERSION` variables as the operational reference.
- Exit criteria: all in-scope services use immutable artifact promotion, have a verified deployment inventory and a documented rollback procedure; legacy release mechanisms no longer control migrated deployments.

## Acceptance Criteria

- Releasing `product:1.4.0` changes neither another service's version nor the version of a shared parent. Its Git tag, effective Maven version and artifact metadata agree.
- CI rejects a tag/POM mismatch, an already published service version and a release graph containing snapshot parents or dependencies.
- UAT and PROD deployment records reference the same image digest or Functions package checksum; promotion executes no artifact build.
- AR and PNPG can run different releases of the same service, and the inventory displays both accurately.
- A deployment request, failed deployment or dispatched-but-unfinished child workflow does not appear as a successfully active release.
- If rollout changes the runtime but a smoke test or registry write fails, the operation reports failure and reconciliation exposes the actual runtime state.
- An out-of-band runtime change is detected as drift. Failed observations retain the last verified state with an explicit stale or unknown status.
- If multiple revisions receive traffic, the overview displays each revision, artifact and traffic allocation.
- A UAT rollback restores a retained previous artifact without rebuilding and creates a new deployment-history entry.
- Missing rollback artifacts or incompatible migration/configuration requirements block rollback with an actionable explanation.
- Concurrent operations cannot race on the same destination or lose unrelated registry updates.
- A partially deployed onboarding composition shows individual component outcomes rather than a misleading successful domain version.

## Open Questions

- Registry location and storage layout: dedicated branch in this repository or a separate deployment repository.
- Artifact retention duration, deployment-history retention and the guaranteed rollback window.
- Runtime reconciliation frequency, observation freshness threshold and operational notification channel.
- Service-specific smoke tests and compatibility gates, including the validation required before promotion to each destination.
- Historical version mapping and initial Maven release version for each migrated service.
- Permanent artifact storage and deployment mechanism for Functions packages that allow environment-independent promotion.

## References

- [Release branch creation](../../../.github/workflows/create_release_branch.yml)
- [Application release entry point](../../../.github/workflows/release_app.yml)
- [Reusable application release](../../../.github/workflows/call_release_app.yml)
- [Docker build and deployment](../../../.github/workflows/call_release_docker.yml)
- [Release tracking variables](../../../.github/workflows/call_update_gh_variable.yml)
- [Onboarding release orchestration](../../../.github/workflows/trigger_release_onboarding.yml)
- [Functions release entry point](../../../.github/workflows/release_functions.yml)
- [Status page generator](../../../.github/status-page/generate.py)
- [Container App Terraform module](../../../infra/resources/_modules/container_app_microservice/container_app.tf)
