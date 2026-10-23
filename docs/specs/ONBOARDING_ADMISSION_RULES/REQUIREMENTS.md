# ONBOARDING_ADMISSION_RULES — Requirements

This spec covers how Selfcare decides **whether** an institution with a given `(institutionType, origin)` pair can join a product, and **which workflow** it uses. product-ms is the single source of truth. onboarding-ms only asks it for a decision.

Jira: SELC-9538 (product-ms: SELC-9539, onboarding-ms: SELC-9540).

## Problem

Before this change, the product field `institutionOrigins` was used for two different purposes:

1. **FE**: `GET /product/origins` told the frontend which institution types to show in the self-service onboarding.
2. **BE**: onboarding-ms rejected every pair missing from `institutionOrigins`, through `InstitutionTypeValidator`, in every flow.

So a pair could not be hidden from the FE and still be accepted in the flows operated by PagoPA. The concrete case: GPU institutions must not join `prod-pagopa` from the FE, but PagoPA must still be able to onboard them through CONFIRMATION, IMPORT, aggregation and INCREMENT.

Other rules were hard-coded or duplicated in onboarding-ms:
- the origin → institution type matrix;
- the workflow priorities;
- the workflows allowed for the SELC origin.

There was also a bug: when a pair had no workflow rule, the self-service flow returned **500** instead of 400.

## Functional Requirements

### 1. Admission rules

- **SELC-1.1** product-ms MUST be the only service that evaluates admission rules. onboarding-ms MUST NOT contain origin/type matrices, workflow priorities or workflow whitelists.
- **SELC-1.2** A pair is admitted only if it appears in the product `institutionOrigins`.
- **SELC-1.3** Each `institutionOrigins` entry MAY declare `selfOnboarding`:
  - absent or `true`: the pair can join through both self-service and managed flows (current behaviour);
  - `false`: the pair can join only through managed flows.
- **SELC-1.4** The admitted institution types per origin are platform-wide and set in product-ms configuration (`product-ms.onboarding-rules.allowed-institution-types.<ORIGIN>`). An incoherent pair (e.g. `SA/IPA`) MUST be denied with a message listing the allowed types.
- **SELC-1.5** The workflows admitted per origin are set in product-ms configuration (`product-ms.onboarding-rules.allowed-workflow-types.<ORIGIN>`). Products with a parent are not checked.
- **SELC-1.6** Changing products, pairs, origins or workflows MUST NOT require code changes.

### 2. Channels

- **SELC-2.1** The channel MUST be declared explicitly by each onboarding-ms entry point. It is never derived from the workflow type or from the caller.
  - `SELF_SERVICE`: `onboarding()` (`/`, `/pa`, `/psp`, `/pa/aggregation`) and `onboardingPgCompletion` (`/pg/completion`, called by the PNPG FE).
  - `MANAGED`: `onboardingCompletion`, `onboardingAggregationCompletion`, `onboardingImport`, `onboardingAggregationImport`, `onboardingIncrement`.
- **SELC-2.2** Managed flows impose their own workflow (CONFIRMATION, CONFIRMATION_AGGREGATOR, IMPORT, IMPORT_AGGREGATION, INCREMENT_REGISTRATION_AGGREGATOR). `/pg/completion` imposes CONFIRMATION. Every other self-service request lets product-ms resolve the workflow.
- **SELC-2.3** Flows that refer to an existing onboarding (`onboardingUsers`, `onboardingContractUpdate`, `onboardingUserPg`) are not subject to admission.

### 3. Decision

- **SELC-3.1** product-ms MUST expose `POST /product/{productId}/onboarding-decision` (internal, `tenantId` query parameter), with request `{institutionType, origin, channel, aggregator, workflowType?}`.
- **SELC-3.2** The rules MUST be evaluated in this order, and the first failing rule decides:
  1. origin coherence → `ORIGIN_INSTITUTION_TYPE_MISMATCH`;
  2. pair present in `institutionOrigins` → `INSTITUTION_TYPE_NOT_ALLOWED`;
  3. `SELF_SERVICE` with `selfOnboarding=false` → `SELF_ONBOARDING_NOT_ALLOWED`;
  4. workflow resolvable (imposed, or resolved as in SELC-3.3) → `WORKFLOW_NOT_RESOLVABLE`;
  5. workflow admitted for the origin → `WORKFLOW_NOT_ALLOWED_FOR_ORIGIN`.
- **SELC-3.3** Workflow resolution when not imposed (first match wins):
  1. PT → `FOR_APPROVE_PT`;
  2. aggregator → `CONTRACT_REGISTRATION_AGGREGATOR`;
  3. `requiredSignatures > 1` → `CONTRACT_WITH_COUNTERSIGNATURE`;
  4. product with parent → `CONTRACT_REGISTRATION`;
  5. otherwise the matching `workflowRules` entry.
- **SELC-3.4** An allowed decision MUST return:
  - `workflowType`;
  - the `requiredDocuments` filtered for the pair;
  - `expirationDays` (default 30);
  - the valid product.

  This replaces the separate product-ms calls for workflow, required documents and expiration.
- **SELC-3.5** A missing or invalid product MUST return 404.

### 4. onboarding-ms behaviour

- **SELC-4.1** Admission MUST run before duplicate checks, registry calls and persistence.
- **SELC-4.2** A denied decision MUST be a **400**, never a 500:
  - `INSTITUTION_TYPE_NOT_ALLOWED` and `SELF_ONBOARDING_NOT_ALLOWED` → `InstitutionTypeNotAllowedException` (code `002-1010`, unchanged);
  - other reasons → `InvalidRequestException`;
  - product not found → `OnboardingNotAllowedException`.
- **SELC-4.3** If the registry changes the institution type (IPA import), the decision MUST be re-evaluated for the new type.
- **SELC-4.4** onboarding-ms MUST NOT fall back to local rules when product-ms fails.

### 5. FE and external consumers

- **SELC-5.1** `GET /product/origins` MUST keep its response shape and MUST omit pairs with `selfOnboarding=false`.
- **SELC-5.2** All changes to product-ms APIs MUST be additive. No consumer (FE, BFFs, external-api, onboarding-functions, APIM) needs to change.

### 6. Configuration safety

- **SELC-6.1** product-ms MUST validate the onboarding configuration on create and patch. It reports:
  - duplicated pairs;
  - incoherent origin/type pairs;
  - self-onboardable pairs without a resolvable workflow, or with a workflow not admitted for the origin;
  - `workflowRules` without a matching `institutionOrigins` entry.
- **SELC-6.2** Validation mode is set by `product-ms.onboarding-rules.validation-mode` (`ONBOARDING_RULES_VALIDATION_MODE`):
  - `STRICT` (default): rejects the product with 400;
  - `WARN`: only logs the problems. Use it temporarily in an environment whose configurations are not aligned yet.

  `STRICT` checks only products being created or updated: products already stored are not re-validated.

## Acceptance Criteria

- GPU/SELC on prod-pagopa with `selfOnboarding=false`:
  - self-service → 400 "... can onboard product prod-pagopa only through the flows managed by PagoPA";
  - `/completion` → 200 CONFIRMATION;
  - the other managed flows (aggregation, import, increment) are admitted with their imposed workflow;
  - not returned by `/origins`.
- SA/IPA → 400 "InstitutionType 'SA' is not allowed for origin 'IPA'. Allowed institution types: [GSP, PA, SCEC]".
- A pair without a workflow rule → 400 "No workflow configured for product ..., institutionType ..., origin ...".
- A self-service onboarding calls product-ms twice: the decision, plus `/valid` in `validateAllowedProductList`. A denied onboarding calls it once. Before this change it took 6–8 calls.
