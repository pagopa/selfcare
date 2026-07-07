# Storage — User Attachments

Terraform module that provisions a **dedicated Azure Storage Account** used by
`document-ms` to receive end-user uploaded documents

It is a sibling of `_modules/storage_accounts` (which manages
`sc-<env>-documents-blob`, used for system contracts / templates).

## Why a separate storage account

Per Confluence DR ["Flusso di adesione per GSP con origin NON IPA"](https://pagopa.atlassian.net/wiki/spaces/SCP/pages/3035136239) and the internal
Security team analysis:

1. **Blast-radius isolation** — user-uploaded content is untrusted and must live
   in a "sandbox" account, separate from system-managed documents.
2. **Origin routing** — the application persists `documentOrigin = storage
   account name` on each document reference, so retrieval can target the correct
   account. Two storage accounts ⇒ two possible `documentOrigin` values.
3. **Defender for Storage cost model** — Defender is priced per storage account
   (+ per-GB scanned). Enabling it only on the sandbox keeps the cost bounded
   and applies malware scanning exactly where user-controlled content lands.
4. **Public access hardened** — the sandbox account denies public network access
   (`network_rules.default_action = "Deny"`) and is only reachable through the
   private endpoint on the dedicated subnet, preventing race conditions on
   malicious blobs.

## What it creates

| Resource | Name / value |
|---|---|
| Dedicated subnet | `selc-<env>-user-attachments-snet` (from `cidr_subnet_user_attachments_storage`) |
| Storage account | Provisioned by `pagopa-dx/azure-storage-account/azurerm` v1.x, tier `l`, blob-only, PEP on the subnet above |
| Blob container | `sc-<env>-user-attachments-blob` (override with `container_name`) |
| KV secret | `user-attachments-storage-connection-string` (override with `kv_secret_name`) |
| Management locks | `CanNotDelete` on storage account and container |
| Lifecycle policy | Tier-down + delete on blobs matching `lifecycle_prefix_match` |
| Defender for Storage | `azurerm_security_center_storage_defender` with on-upload malware scanning enabled |

## Defender configuration

Defender for Storage is **enabled at storage-account scope** (as recommended
by ProdSec), overriding subscription-level settings:

- **Activity Monitoring** — automatic once Defender is on.
- **On-upload Malware Scanning** — enabled by default (`defender_malware_scanning_enabled`).
- **Malware scanning monthly cap** — default `5000` GB/month (`defender_malware_scanning_cap_gb_per_month`).
- **Soft-delete of malicious blobs** — this is configured through the storage
  account's blob soft-delete retention. To actively block a malicious upload,
  set `blob_features.delete_retention_days >= 1` on the module invocation. The
  module enforces this with a precondition when
  `defender_soft_delete_malicious_blobs = true`.

> ℹ️ At the time of writing the `azurerm` provider exposes the malware scanning
> outcome as blob index tags, but does not model the "soft delete malicious
> blobs" toggle as a first-class attribute. Where required, that toggle should
> be confirmed manually in the Azure portal after apply
> (Storage → Microsoft Defender for Cloud → Settings).

## Naming conventions

Azure limits **storage account names to 24 lowercase alphanumeric characters**.
The underlying `pagopa-dx/azure-storage-account/azurerm` module builds the name
as `<prefix_short><env_short><location_short><domain><app_name>st<instance_number>`,
so `app_name` MUST stay short (≤ 9 chars). The module defaults to `"usrattach"`
(same length as `"documents"` used by the sibling module) which yields:

| Env  | Storage account name        | Chars |
|------|-----------------------------|-------|
| dev  | `scdweuarusrattachst01`     | 21    |
| uat  | `scuweuarusrattachst01`     | 21    |
| prod | `scpweuarusrattachst01`     | 21    |

The blob **container** name is not subject to the 24-char limit and defaults to
`${prefix}-${env_short}-${app_name}-blob` (e.g. `sc-d-usrattach-blob`).
Callers who want a more readable container name (e.g. matching the value the
application already reads from config) should pass `container_name` explicitly.
The `dev-ar/commons.tf` invocation for instance sets it to
`sc-d-user-attachments-blob`.

## Networking

A dedicated `/24` subnet per environment is required. Suggested allocation
(coordinate with the Networking team before apply):

| Env  | CIDR |
|------|------|
| dev  | `10.1.137.0/24` (adjacent to `cidr_subnet_document_storage = 10.1.136.0/24`) |
| uat  | TBD |
| prod | TBD |

## Consumers

The `document-ms` container app (see `infra/resources/document-ms/<env>-ar/main.tf`)
must be updated to inject:

- `STORAGE_CONTAINER_USER` = value of `storage_container_name` output
- Secret `BLOB_STORAGE_USER_CONNECTION_STRING` mapped to the KV secret
  `user-attachments-storage-connection-string`
