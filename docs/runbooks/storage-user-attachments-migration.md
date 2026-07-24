# Runbook — Migration of `storage_user_attachments` to shared `storage_accounts` module

**Scope**: PR that generalizes the dedicated `storage_user_attachments` module and
merges it into the shared `_modules/storage_accounts`. Adds caller-facing
Defender-for-Storage inputs, but the actual enforcement at the ARM level is
deferred until the upstream `pagopa-dx/azure-storage-account` module exposes
the corresponding variables (agreed on review, see follow-up in section 6).

**Applies to**: `infra/core/{dev-ar, uat-ar, prod-ar}`.

---

## 0. Prerequisites

- Terraform (any version compatible with the rest of the repo).
- Azure CLI logged in on the correct subscription (`az account show`).
- Access to the correct Terraform state backend (env vars / `backend.ini`).
- **Environment variables must be exported before every `terraform` call.**
  In DEV the required ones are (adjust for UAT/PROD):
  - `ARM_CLIENT_ID`, `ARM_SUBSCRIPTION_ID`, `ARM_TENANT_ID`, `ARM_USE_OIDC=true`
    (or the OIDC token from GitHub, see repo's `README.md`).

Do NOT run this from Windows PowerShell against the full apply — the
`null_resource.upload_*` resources use bash-only `provisioner "local-exec"`
and will fail. Use `-target` as shown below, or run from a Linux/CI agent.

---

## 1. Status per environment

| Env      | Applied? | Notes |
|----------|----------|-------|
| DEV-AR   | ⏳ TODO   | State cleanup (removes previous `azapi` workaround) + apply of new module design; see section 3.a. |
| UAT-AR   | ⏳ TODO   | Follow section 3.b. |
| PROD-AR  | ⏳ TODO   | Follow section 3.c, requires manual approval. |

> DEV was previously applied with the `azapi_update_resource` workaround (now
> removed). Section 3.a below covers the extra state cleanup that only DEV
> needs.

---

## 2. What this PR changes (recap)

1. **Consolidated module**: `_modules/storage_accounts` gains generic naming
   variables (`naming_config`, `kv_secret_name`, `lifecycle_prefix_match`) and
   Defender caller-facing variables (`defender_enabled`,
   `defender_malware_scanning_enabled`,
   `defender_malware_scanning_cap_gb_per_month`,
   `defender_sensitive_data_discovery_enabled`,
   `defender_soft_delete_malicious_blobs`).
2. **Deleted module**: `_modules/storage_user_attachments/` (replaced by the
   shared module above).
3. **Defender wiring is DEFERRED**: the `defender_*` inputs are accepted by
   the module and validated (soft-delete precondition), but at the ARM level
   Defender for Storage remains configured with the upstream defaults from
   `pagopa-dx/azure-storage-account` v1.x — that is:
   - `malware_scanning_on_upload_enabled = false`
   - `override_subscription_settings_enabled = false`
   - `sensitive_data_discovery_enabled = false`
   As agreed on review, we asked the pagopa-dx team to release a new version
   exposing these inputs. When it lands, `storage_account.tf` will start
   wiring the caller-facing variables to the upstream module — callers won't
   need any change (see section 6).
4. **Callers** `commons.tf` in `dev-ar / uat-ar / prod-ar` migrated from
   `../_modules/storage_user_attachments` to `../_modules/storage_accounts`
   with `naming_config = "user-attachments"` and
   `kv_secret_name = "user-attachments-storage-connection-string"`.
5. **`document-ms`** `resources/document-ms/{dev,uat,prod}-ar/main.tf` already
   consumes the KV secret `user-attachments-storage-connection-string`
   — no additional changes required at the caller side.

---

## 3. Migration procedure (per environment)

### 3.a. DEV-AR — includes cleanup of the previous `azapi` workaround

```powershell
az account set --subscription "<DEV_SUBSCRIPTION_ID_OR_NAME>"

cd C:\Users\fsalamon\Documents\PagoPa\SelfCare\selfcare\infra\core\dev-ar

# 1. State backup (rollback safety net).
terraform state pull > "state.backup.$(Get-Date -Format yyyyMMddHHmm).json"

# 2. Init.
terraform init -upgrade

# 3. Clean up the state entries from the previous azapi-based workaround.
#    `state rm` does NOT touch Azure; the ARM object remains as-is.
#    In DEV both entries should exist; in UAT/PROD they do NOT (skip in 3.b/3.c).
terraform state rm 'module.storage_user_attachments.terraform_data.defender_apply_tick[0]'      2>$null
terraform state rm 'module.storage_user_attachments.azapi_update_resource.defender_override[0]' 2>$null

# 4. Plan — expected on the storage_user_attachments module scope:
#      ~ module.storage_user_attachments.module.storage_account.....this[0]
#        (upstream pagopa-dx wants to reset malware_scanning to false; this
#        will effectively DISABLE malware scanning on the storage account
#        until the new pagopa-dx version is released — see section 5).
#    NO destroys must appear on subnet / container / storage account / lock /
#    KV secret. If they do, STOP and rollback using the state backup.
terraform plan

# 5. Apply targeted (skip pre-existing bash-only null_resource.upload_* which
#    would fail on Windows PowerShell).
terraform apply -target='module.storage_user_attachments'
```

**Post-apply verification (DEV)**:

```powershell
$saId = (az storage account show `
  --name "scdweuarusrattachst01" `
  --resource-group "selc-d-user-attachments-storage-rg" `
  --query id -o tsv)

az rest --method GET `
  --url "https://management.azure.com$saId/providers/Microsoft.Security/defenderForStorageSettings/current?api-version=2025-06-01" `
| ConvertFrom-Json | Select-Object -ExpandProperty properties | ConvertTo-Json -Depth 5
```

Expected JSON (until pagopa-dx exposes the inputs — see section 6):

```json
{
  "isEnabled": true,
  "overrideSubscriptionLevelSettings": false,
  "malwareScanning": {
    "onUpload": { "isEnabled": false, "capGBPerMonth": -1 }
  },
  "sensitiveDataDiscovery": { "isEnabled": false }
}
```

That is: Defender enabled with "activity monitoring" only. Malware scanning
DISABLED. This is the temporary state agreed on review; malware scanning
comes back on automatically as soon as we bump the upstream module version
and uncomment the wiring in `_modules/storage_accounts/storage_account.tf`
(section 6).

### 3.b. UAT-AR

```powershell
az account set --subscription "<UAT_SUBSCRIPTION_ID_OR_NAME>"

cd C:\Users\fsalamon\Documents\PagoPa\SelfCare\selfcare\infra\core\uat-ar

terraform state pull > "state.backup.$(Get-Date -Format yyyyMMddHHmm).json"
terraform init -upgrade

# UAT was never applied with the previous variants; the `state rm` calls
# below are harmless no-ops if the entries don't exist.
terraform state rm 'module.storage_user_attachments.terraform_data.defender_apply_tick[0]'                2>$null
terraform state rm 'module.storage_user_attachments.azapi_update_resource.defender_override[0]'           2>$null
terraform state rm 'module.storage_user_attachments.azurerm_security_center_storage_defender.defender[0]' 2>$null

# State moves — rename addresses from the old `storage_user_attachments`
# module to the shared `storage_accounts` module. NONE of these touch Azure.
# (Only applies if UAT still holds the old module in state; otherwise they
# are no-ops.)
terraform state mv `
  'module.storage_user_attachments.azurerm_subnet.user_attachments_snet' `
  'module.storage_user_attachments.azurerm_subnet.documents_snet'                                             2>$null
terraform state mv `
  'module.storage_user_attachments.azurerm_storage_container.selc_user_attachments_blob' `
  'module.storage_user_attachments.azurerm_storage_container.selc_documents_blob'                             2>$null
terraform state mv `
  'module.storage_user_attachments.azurerm_management_lock.selc_user_attachments_blob_management_lock' `
  'module.storage_user_attachments.azurerm_management_lock.selc_documents_blob_management_lock'               2>$null
terraform state mv `
  'module.storage_user_attachments.azurerm_key_vault_secret.selc_user_attachments_storage_connection_string' `
  'module.storage_user_attachments.azurerm_key_vault_secret.selc_documents_storage_connection_string'         2>$null
terraform state mv `
  'module.storage_user_attachments.azurerm_management_lock.selc_user_attachments_storage_management_lock' `
  'module.storage_user_attachments.azurerm_management_lock.selc_documents_storage_management_lock'            2>$null

terraform plan   # verify: NO destroys on infrastructure

terraform apply -target='module.storage_user_attachments'
```

**Post-apply verification (UAT)**:

```powershell
$saId = (az storage account show `
  --name "scuweuarusrattachst01" `
  --resource-group "selc-u-user-attachments-storage-rg" `
  --query id -o tsv)

az rest --method GET `
  --url "https://management.azure.com$saId/providers/Microsoft.Security/defenderForStorageSettings/current?api-version=2025-06-01" `
| ConvertFrom-Json | Select-Object -ExpandProperty properties | ConvertTo-Json -Depth 5
```

Expected: same as DEV — `isEnabled: true`, `malwareScanning.onUpload.isEnabled: false`
until section 6 is executed.

### 3.c. PROD-AR

Identical to UAT. PROD deployments require **manual approval** per repo
policy — coordinate with the platform team.

```powershell
az account set --subscription "<PROD_SUBSCRIPTION_ID_OR_NAME>"

cd C:\Users\fsalamon\Documents\PagoPa\SelfCare\selfcare\infra\core\prod-ar

terraform state pull > "state.backup.$(Get-Date -Format yyyyMMddHHmm).json"
terraform init -upgrade

terraform state rm 'module.storage_user_attachments.terraform_data.defender_apply_tick[0]'                2>$null
terraform state rm 'module.storage_user_attachments.azapi_update_resource.defender_override[0]'           2>$null
terraform state rm 'module.storage_user_attachments.azurerm_security_center_storage_defender.defender[0]' 2>$null

terraform state mv `
  'module.storage_user_attachments.azurerm_subnet.user_attachments_snet' `
  'module.storage_user_attachments.azurerm_subnet.documents_snet'                                             2>$null
terraform state mv `
  'module.storage_user_attachments.azurerm_storage_container.selc_user_attachments_blob' `
  'module.storage_user_attachments.azurerm_storage_container.selc_documents_blob'                             2>$null
terraform state mv `
  'module.storage_user_attachments.azurerm_management_lock.selc_user_attachments_blob_management_lock' `
  'module.storage_user_attachments.azurerm_management_lock.selc_documents_blob_management_lock'               2>$null
terraform state mv `
  'module.storage_user_attachments.azurerm_key_vault_secret.selc_user_attachments_storage_connection_string' `
  'module.storage_user_attachments.azurerm_key_vault_secret.selc_documents_storage_connection_string'         2>$null
terraform state mv `
  'module.storage_user_attachments.azurerm_management_lock.selc_user_attachments_storage_management_lock' `
  'module.storage_user_attachments.azurerm_management_lock.selc_documents_storage_management_lock'            2>$null

terraform plan   # DOUBLE-CHECK: no destroys on subnet/container/SA/lock/KV secret

terraform apply -target='module.storage_user_attachments'
```

**Post-apply verification (PROD)**:

```powershell
$saId = (az storage account show `
  --name "scpweuarusrattachst01" `
  --resource-group "selc-p-user-attachments-storage-rg" `
  --query id -o tsv)

az rest --method GET `
  --url "https://management.azure.com$saId/providers/Microsoft.Security/defenderForStorageSettings/current?api-version=2025-06-01" `
| ConvertFrom-Json | Select-Object -ExpandProperty properties | ConvertTo-Json -Depth 5
```

---

## 4. Rollback

If anything looks wrong before you approve `apply`, or the apply completes
with unexpected side effects, restore the state file and revert the code:

```powershell
# 1. Restore the state snapshot.
terraform state push .\state.backup.<timestamp>.json

# 2. Revert this branch's changes.
git checkout -- `
  infra/core/_modules/storage_accounts/ `
  infra/core/_modules/storage_user_attachments/ `
  infra/core/dev-ar/commons.tf `
  infra/core/uat-ar/commons.tf `
  infra/core/prod-ar/commons.tf `
  infra/core/dev-ar/locals.tf `
  infra/core/uat-ar/locals.tf `
  infra/core/prod-ar/locals.tf

# 3. Verify no drift.
terraform plan
```

Azure resources are not deleted by `state rm` or `state mv`, only the
Terraform state is remapped; a rollback restores the previous state and
Terraform re-adopts the resources exactly as before.

---

## 5. Known trade-offs / open items

- **Malware scanning temporarily OFF**. Until the pagopa-dx upstream module
  is upgraded (section 6), the `user-attachments` storage account has
  Defender for Storage enabled with **activity monitoring only**. On-upload
  malware scanning is NOT active. The caller-facing `defender_*` inputs are
  still accepted by the module for API stability but do not take effect at
  the ARM level.

- **Private_link_access for `StorageDataScanner`**. As long as the above is
  true, this rule is NOT automatically added by Azure (it is only added
  when `override_subscription_settings_enabled = true` +
  `malware_scanning_on_upload_enabled = true`). Once section 6 is complete
  and malware scanning is on, Azure will re-add the rule and Terraform
  plans may show it as "will be removed" (the upstream module doesn't
  manage it). Safe to ignore; Azure re-adds it within minutes.

---

## 6. Follow-up (post upstream release)

Upstream issue: request `pagopa-dx/azure-storage-account` to expose
Defender for Storage attributes as inputs. Suggested variables (with
backward-compatible defaults):

| Requested variable | Maps to attribute | Default |
|---|---|---|
| `defender_malware_scanning_on_upload_enabled` | `malware_scanning_on_upload_enabled` | `false` |
| `defender_malware_scanning_on_upload_cap_gb_per_month` | `malware_scanning_on_upload_cap_gb_per_month` | `-1` |
| `defender_override_subscription_settings_enabled` | `override_subscription_settings_enabled` | `false` |
| `defender_sensitive_data_discovery_enabled` | `sensitive_data_discovery_enabled` | `false` |
| `defender_scan_results_event_grid_topic_id` | `scan_results_event_grid_topic_id` | `null` |

Once the upstream module is released, in
`_modules/storage_accounts/storage_account.tf`:

1. Bump `version = "~> X.Y"` on the upstream `module "storage_account"` block.
2. **Uncomment** the wiring block already prepared inside `module "storage_account"`
   (see the `# defender_malware_scanning_on_upload_enabled = ...` commented
   lines). Adapt names if the upstream final variables differ — check the
   pagopa-dx CHANGELOG.
3. Callers (`commons.tf` in DEV/UAT/PROD) do NOT need any change: they
   already pass the `defender_*` values via `module "storage_user_attachments"`.
4. `terraform plan` will show an in-place update on the upstream
   `azurerm_security_center_storage_defender.this[0]` from defaults to our
   values. Apply and re-run the verification of section 3 — you should now
   see `malwareScanning.onUpload.isEnabled: true` and
   `overrideSubscriptionLevelSettings: true`.

This closes the temporary trade-off in section 5.

