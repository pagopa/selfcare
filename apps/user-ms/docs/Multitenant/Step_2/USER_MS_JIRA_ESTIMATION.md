# User MS Step 2 Multitenancy - Refinement Estimation

## Estimation scale

| Story points | Person effort |
|---:|---:|
| 1 SP | 2 hours |
| 2 SP | 4 hours |
| 4 SP | 8 hours |
| 8 SP | 16 hours |
| 16 SP | 32 hours |
| 32 SP | 64 hours |

## Assumptions

The estimates include:

- technical analysis
- implementation
- automated tests
- configuration updates
- technical release preparation

The estimates do not include:

- waiting time for approvals
- availability of other teams
- operational windows
- configuration propagation time
- post-release observation periods

## Issue estimates

| Issue | SP | Hours | Summary considerations |
|---|---:|---:|---|
| USRMT-01 - SDK and tenant registry | 8 | 16 | Configuration, dependencies, codec, and bootstrap tests for two tenants. |
| USRMT-02 - HTTP tenant resolution | 8 | 16 | Filter, context management, and comprehensive security scenarios. |
| USRMT-03 - JWT per tenant | 16 | 32 | Critical security component with multiple keys, `kid`, issuer, and fallback. |
| USRMT-04 - Propagation and logging | 4 | 8 | Targeted changes with propagation and sanitization tests. |
| USRMT-05 - Tenant in `UserInstitution` | 4 | 8 | Model, fixtures, mapping, and compatibility with existing documents. |
| USRMT-06 - Tenant-aware repository | 16 | 32 | Covers CRUD, count, upsert, bulk update, and two isolation modes. |
| USRMT-07 - MongoDB access refactoring | 16 | 32 | Numerous service paths must be migrated and verified. |
| USRMT-08 - MongoDB indexes | 8 | 16 | Six stacks, query analysis, and `_id` collision checks. |
| USRMT-09 - Tenant-aware product storage | 16 | 32 | Provider, cache, authentication, prefixes, configuration, and tests. |
| USRMT-10 - Tenant-aware health checks | 8 | 16 | MongoDB and storage checks for all configured tenants. |
| USRMT-11 - Tenant in events | 8 | 16 | Shared SDK change, mapping, compatibility, and publishing. |
| USRMT-12 - Tenant-aware webhook | 4 | 8 | Targeted change to the notification flow and removal of the global tenant. |
| USRMT-13 - Context in asynchronous consumers | 8 | 16 | Lifecycle management, errors, retry, and concurrency across tenants. |
| USRMT-14 - Email per tenant | 16 | 32 | Multiple configurations, fail-closed behavior, retry, and observability. |
| USRMT-15 - Infrastructure configuration | 16 | 32 | Shared metadata, six stacks, secrets, identities, and Terraform. |
| SPIKE-01 - Secret ownership | 8 | 16 | Key Vault, RBAC, connectivity, ownership, and rotation analysis. |
| SPIKE-02 - Email configuration | 4 | 8 | Requirements gathering and operational decisions for AR and PNPG. |
| DEP-01 - Actual tenant from `institution-cdc` | 8 | 16 | Event contract, producer, error handling, and contract tests. |
| USRMT-16 - Backfill and data verification | 32 | 64 | Backup, collisions, migration, and verification across multiple environments. |
| USRMT-17 - Strict isolation | 16 | 32 | Progressive rollout, monitoring, and removal of compatibility mode. |
| USRMT-18 - Shared release | 32 | 64 | Full E2E, secret rotation, routing, observation, and rollback. |

## Totals

| Scope | SP | Person-hours |
|---|---:|---:|
| `USRMT` stories | 236 | 472 |
| Spikes | 12 | 24 |
| `institution-cdc` dependency | 8 | 16 |
| **Grand total** | **256** | **512** |

## Refinement considerations

Issues estimated at 16 or 32 SP should be split before they are added to a
sprint:

| Issue | Suggested split |
|---|---|
| USRMT-03 | JWT resolution implementation; key configuration and testing. |
| USRMT-06 | CRUD operations; bulk operations and strict/compatibility management. |
| USRMT-07 | Read refactoring; write and bulk operation refactoring. |
| USRMT-09 | Storage provider and client; service integration and testing. |
| USRMT-14 | Email configuration selection; error handling, retry, and testing. |
| USRMT-15 | Shared infrastructure metadata; application stack updates. |
| USRMT-16 | Backup and collision checks; backfill and verification by environment. |
| USRMT-17 | Enable strict mode by environment; remove compatibility mode. |
| USRMT-18 | End-to-end qualification; rollout, observation, and rollback. |

## Risks and confidence level

The estimates with the greatest uncertainty are:

- **USRMT-03:** depends on JWT runtime compatibility and the availability of
  distinct keys and `kid` values.
- **USRMT-14:** depends on the availability of AR and PNPG email configurations.
- **USRMT-15:** depends on the decision regarding Key Vault, managed identity,
  and Terraform ownership.
- **USRMT-16:** data volume, collisions, and execution times may require
  additional operational effort.
- **USRMT-18:** requires coordination with downstream services, infrastructure,
  and operations teams.

The total estimate represents person effort, not calendar duration. Independent
activities can be performed in parallel, while spikes, migration, and
cross-team dependencies may extend the overall lead time.
