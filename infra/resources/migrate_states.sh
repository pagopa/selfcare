#!/usr/bin/env bash
# shellcheck disable=SC2016
set -euo pipefail

# =============================================================================
# State Migration Script
# Migrates Terraform state from flat env structure to category-based structure.
#
# Old backend keys:
#   selc.infra.resources.dev.tfstate       (all dev-ar resources)
#   selc.infra.resources.dev-pnpg.tfstate  (all dev-pnpg resources)
#   selc.infra.resources.uat.tfstate       (uat-ar + uat-pnpg — SHARED)
#   selc.infra.resources.prod.tfstate      (prod-ar + prod-pnpg — SHARED)
#
# New backend keys:
#   selc.infra.resources.<category>.<env>.tfstate
#
# Storage backends:
#   dev  → selcdstinfraterraform / azurermstate
#   uat  → selcustinfraterraform / azurermstate
#   prod → selcpstinfraterraform / azurermstate
#
# Requirements:
#   - Azure CLI (az) authenticated with appropriate permissions
#   - jq >= 1.6
#
# IMPORTANT:
#   - Run ONCE before applying any new category/<env> configuration.
#   - Old state blobs are NOT deleted by this script.
#   - For uat and prod the old state is shared between -ar and -pnpg envs.
#     Resources with the same module name in both (e.g. module.apim_api_registry_proxy,
#     module.cosmosdb) will be duplicated across the extracted states.
#     Review with `terraform plan` after migration and adjust manually if needed.
# =============================================================================

RESOURCE_GROUP="io-infra-rg"
CONTAINER="azurermstate"

DEV_STORAGE="selcdstinfraterraform"
UAT_STORAGE="selcustinfraterraform"
PROD_STORAGE="selcpstinfraterraform"

WORK_DIR=$(mktemp -d)
trap 'rm -rf "$WORK_DIR"' EXIT

# ---- Colours ---------------------------------------------------------------
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
log()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC}  $*"; }
err()  { echo -e "${RED}[ERROR]${NC} $*" >&2; }
sep()  { echo -e "${BLUE}------------------------------------------------------------------------${NC}"; }

# ---- Helpers ---------------------------------------------------------------

# Download a state blob to a local file. Returns 1 if blob doesn't exist.
download_state() {
  local storage="$1" key="$2" out="$3"
  log "Downloading: $key"
  if ! az storage blob download \
        --account-name  "$storage" \
        --container-name "$CONTAINER" \
        --name           "$key" \
        --file           "$out" \
        --auth-mode      login \
        --overwrite \
        --output none 2>/dev/null; then
    warn "  Blob '$key' not found in '$storage' — skipping"
    return 1
  fi
}

# Filter a state file by a jq boolean selector; write result to a new file.
extract_state() {
  local src="$1" dst="$2" jq_select="$3"
  jq ".resources |= map(select($jq_select)) | .serial += 1" "$src" > "$dst"
  local n
  n=$(jq '.resources | length' "$dst")
  log "  → $(basename "$dst")  ($n resource(s))"
}

# Upload a state file. Skips upload if the file has 0 resources.
upload_state() {
  local storage="$1" key="$2" src="$3"
  local n
  n=$(jq '.resources | length' "$src" 2>/dev/null || echo 0)
  if [[ "$n" -eq 0 ]]; then
    warn "  Skipping upload of '$key' — 0 resources matched"
    return 0
  fi
  log "Uploading: $key  ($n resource(s))"
  az storage blob upload \
    --account-name   "$storage" \
    --container-name "$CONTAINER" \
    --name           "$key" \
    --file           "$src" \
    --auth-mode      login \
    --overwrite \
    --output none
}

# Download (once), filter, and upload for a single category/env combination.
migrate() {
  local storage="$1" old_key="$2" new_key="$3" jq_select="$4"

  # Cache the downloaded blob so the same source is not downloaded repeatedly.
  local old_state="$WORK_DIR/$(echo "$old_key" | tr '.' '_' | tr '-' '_').json"
  local new_state="$WORK_DIR/$(echo "$new_key" | tr '.' '_' | tr '-' '_').json"

  if [[ ! -f "$old_state" ]]; then
    download_state "$storage" "$old_key" "$old_state" || return 0
  fi

  extract_state  "$old_state" "$new_state" "$jq_select"
  upload_state   "$storage"  "$new_key"   "$new_state"
}

# =============================================================================
# JQ selectors
# Each selector is a jq boolean expression used inside:
#   .resources |= map(select(<EXPR>))
#
# Logic:
#   - Module resources: match on .module path prefix
#   - Root resources:   match on (.module // "") == "" and resource name/type
#
# Regex anchors:
#   ^module\\.NAME(\\.|\\[|$)  — matches the module itself and any children,
#                                 but NOT modules whose name starts with NAME_
#                                 (e.g. "onboarding_backend" won't match
#                                       "onboarding_backend_pnpg")
# =============================================================================

# ---- auth (ar only) ---------------------------------------------------------
SEL_AUTH='
  (.module // "" | test(
    "^module\\.(apim_api_auth|cosmosdb_auth|collection_auth_otp_flows|container_app_auth_ms)(\\.|\\[|$)"
  ))
'

# ---- document (ar only) -----------------------------------------------------
SEL_DOCUMENT='
  ((.module // "" | test("^module\\.(storage_documents|upload_file_logo)(\\.|\\[|$)"))
  or
  ((.module // "") == "" and (.name | test("^(documents_sa_rg|documents_identity)$"))))
'

# ---- iam (ar only) ----------------------------------------------------------
SEL_IAM='
  (.module // "" | test("^module\\.container_app_iam_ms(\\.|\\[|$)"))
'

# ---- onboarding — ar side ---------------------------------------------------
# Distinguisher: module.container_app_onboarding_backend (no _pnpg suffix)
#                module.apim_api_bff_onboarding           (no _pnpg suffix)
SEL_ONBOARDING_AR='
  ((.module // "" | test(
    "^module\\.(cosmosdb|collection_onboardings|collection_tokens|container_app_onboarding_ms|container_app_onboarding_cdc|container_app_onboarding_backend|apim_api_bff_onboarding)(\\.|\\[|$)"
  ))
  or
  ((.module // "") == "" and (.name | test("(onboarding_fn|encryption_key|encryption_iv)"))))
'

# ---- onboarding — pnpg side -------------------------------------------------
# Distinguisher: module.container_app_onboarding_backend_pnpg
#                module.apim_api_bff_onboarding_pnpg
SEL_ONBOARDING_PNPG='
  ((.module // "" | test(
    "^module\\.(cosmosdb|collection_onboardings|collection_tokens|container_app_onboarding_ms|container_app_onboarding_cdc|container_app_onboarding_backend_pnpg|apim_api_bff_onboarding_pnpg)(\\.|\\[|$)"
  ))
  or
  ((.module // "") == "" and (.name | test("(onboarding_fn|encryption_key|encryption_iv)"))))
'

# ---- product (ar only) ------------------------------------------------------
SEL_PRODUCT='
  (.module // "" | test("^module\\.container_app_product_ms(\\.|\\[|$)"))
'

# ---- product-cdc (ar only) --------------------------------------------------
SEL_PRODUCT_CDC='
  (.module // "" | test("^module\\.container_app_product_cdc(\\.|\\[|$)"))
'

# ---- registry-proxy — ar side -----------------------------------------------
# Includes namirial-sign resources (namirial_sws_*, namirial_sign_*) and
# the dapr-related blob store root resources (visura, blob_state*)
SEL_REGISTRY_PROXY_AR='
  ((.module // "" | test("^module\\.(apim_api_registry_proxy|container_app_registry_proxy_ms)(\\.|\\[|$)"))
  or
  ((.module // "") == "" and (.name | test("^(visura|blob_state|namirial_sws_|namirial_sign_)"))))
'

# ---- registry-proxy — pnpg side ---------------------------------------------
SEL_REGISTRY_PROXY_PNPG='
  ((.module // "" | test("^module\\.(apim_api_registry_proxy|container_app_registry_proxy_ms)(\\.|\\[|$)"))
  or
  ((.module // "") == "" and .name == "blob_state_access"))
'

# ---- search (ar only) -------------------------------------------------------
SEL_SEARCH='
  (.module // "" | test("^module\\.(ai_search|ai_search_institution|dapr)(\\.|\\[|$)"))
'

# ---- spid-login (dev-pnpg only) ---------------------------------------------
SEL_SPID_LOGIN='
  (.module // "" | test("^module\\.container_app_hub_spid_login(\\.|\\[|$)"))
'

# ---- webhook (ar only) ------------------------------------------------------
SEL_WEBHOOK='
  ((.module // "" | test(
    "^module\\.(cosmosdb_webhook|collection_webhooks|collection_webhook_notifications|container_app_webhook_ms|apim_api_webhook_ms)(\\.|\\[|$)"
  ))
  or
  ((.module // "") == "" and .type == "azurerm_api_management_api_version_set" and .name == "apim_api_webhook"))
'

# =============================================================================
# DEV-AR  →  selc.infra.resources.dev.tfstate
# =============================================================================
sep
log "=== Migrating DEV-AR state ==="
OLD="selc.infra.resources.dev.tfstate"

migrate "$DEV_STORAGE" "$OLD" "selc.infra.resources.auth.dev-ar.tfstate"             "$SEL_AUTH"
migrate "$DEV_STORAGE" "$OLD" "selc.infra.resources.document.dev-ar.tfstate"         "$SEL_DOCUMENT"
migrate "$DEV_STORAGE" "$OLD" "selc.infra.resources.iam.dev-ar.tfstate"              "$SEL_IAM"
migrate "$DEV_STORAGE" "$OLD" "selc.infra.resources.onboarding.dev-ar.tfstate"       "$SEL_ONBOARDING_AR"
migrate "$DEV_STORAGE" "$OLD" "selc.infra.resources.product.dev-ar.tfstate"          "$SEL_PRODUCT"
migrate "$DEV_STORAGE" "$OLD" "selc.infra.resources.product-cdc.dev-ar.tfstate"      "$SEL_PRODUCT_CDC"
migrate "$DEV_STORAGE" "$OLD" "selc.infra.resources.registry-proxy.dev-ar.tfstate"   "$SEL_REGISTRY_PROXY_AR"
migrate "$DEV_STORAGE" "$OLD" "selc.infra.resources.search.dev-ar.tfstate"           "$SEL_SEARCH"
migrate "$DEV_STORAGE" "$OLD" "selc.infra.resources.webhook.dev-ar.tfstate"          "$SEL_WEBHOOK"

# =============================================================================
# DEV-PNPG  →  selc.infra.resources.dev-pnpg.tfstate
# =============================================================================
sep
log "=== Migrating DEV-PNPG state ==="
OLD="selc.infra.resources.dev-pnpg.tfstate"

migrate "$DEV_STORAGE" "$OLD" "selc.infra.resources.onboarding.dev-pnpg.tfstate"      "$SEL_ONBOARDING_PNPG"
migrate "$DEV_STORAGE" "$OLD" "selc.infra.resources.registry-proxy.dev-pnpg.tfstate"  "$SEL_REGISTRY_PROXY_PNPG"
migrate "$DEV_STORAGE" "$OLD" "selc.infra.resources.spid-login.dev-pnpg.tfstate"      "$SEL_SPID_LOGIN"

# =============================================================================
# UAT  →  selc.infra.resources.uat.tfstate  (SHARED: uat-ar + uat-pnpg)
#
# WARNING: uat-ar and uat-pnpg were both configured to use the same backend
# key.  As a result, the single state file may contain resources from both
# environments with identical module names (e.g. module.cosmosdb,
# module.apim_api_registry_proxy).  Such resources will appear in BOTH the
# extracted uat-ar and uat-pnpg category states.
# After migration, run `terraform plan` against each new directory to detect
# any duplicates and remove them from the appropriate state manually via
# `terraform state rm`.
# =============================================================================
sep
log "=== Migrating UAT state ==="
warn "uat-ar and uat-pnpg share selc.infra.resources.uat.tfstate."
warn "Resources with identical module names will be duplicated — review with"
warn "'terraform plan' after migration and resolve with 'terraform state rm'."

OLD="selc.infra.resources.uat.tfstate"

migrate "$UAT_STORAGE" "$OLD" "selc.infra.resources.auth.uat-ar.tfstate"             "$SEL_AUTH"
migrate "$UAT_STORAGE" "$OLD" "selc.infra.resources.document.uat-ar.tfstate"         "$SEL_DOCUMENT"
migrate "$UAT_STORAGE" "$OLD" "selc.infra.resources.iam.uat-ar.tfstate"              "$SEL_IAM"
migrate "$UAT_STORAGE" "$OLD" "selc.infra.resources.onboarding.uat-ar.tfstate"       "$SEL_ONBOARDING_AR"
migrate "$UAT_STORAGE" "$OLD" "selc.infra.resources.onboarding.uat-pnpg.tfstate"     "$SEL_ONBOARDING_PNPG"
migrate "$UAT_STORAGE" "$OLD" "selc.infra.resources.product.uat-ar.tfstate"          "$SEL_PRODUCT"
migrate "$UAT_STORAGE" "$OLD" "selc.infra.resources.product-cdc.uat-ar.tfstate"      "$SEL_PRODUCT_CDC"
migrate "$UAT_STORAGE" "$OLD" "selc.infra.resources.registry-proxy.uat-ar.tfstate"   "$SEL_REGISTRY_PROXY_AR"
migrate "$UAT_STORAGE" "$OLD" "selc.infra.resources.registry-proxy.uat-pnpg.tfstate" "$SEL_REGISTRY_PROXY_PNPG"
migrate "$UAT_STORAGE" "$OLD" "selc.infra.resources.search.uat-ar.tfstate"           "$SEL_SEARCH"
migrate "$UAT_STORAGE" "$OLD" "selc.infra.resources.webhook.uat-ar.tfstate"          "$SEL_WEBHOOK"

# =============================================================================
# PROD  →  selc.infra.resources.prod.tfstate  (SHARED: prod-ar + prod-pnpg)
# Same caveats as UAT above.
# =============================================================================
sep
log "=== Migrating PROD state ==="
warn "prod-ar and prod-pnpg share selc.infra.resources.prod.tfstate."
warn "Same duplicate-resource caveat applies — review with 'terraform plan'."

OLD="selc.infra.resources.prod.tfstate"

migrate "$PROD_STORAGE" "$OLD" "selc.infra.resources.auth.prod-ar.tfstate"             "$SEL_AUTH"
migrate "$PROD_STORAGE" "$OLD" "selc.infra.resources.document.prod-ar.tfstate"         "$SEL_DOCUMENT"
migrate "$PROD_STORAGE" "$OLD" "selc.infra.resources.iam.prod-ar.tfstate"              "$SEL_IAM"
migrate "$PROD_STORAGE" "$OLD" "selc.infra.resources.onboarding.prod-ar.tfstate"       "$SEL_ONBOARDING_AR"
migrate "$PROD_STORAGE" "$OLD" "selc.infra.resources.onboarding.prod-pnpg.tfstate"     "$SEL_ONBOARDING_PNPG"
migrate "$PROD_STORAGE" "$OLD" "selc.infra.resources.product.prod-ar.tfstate"          "$SEL_PRODUCT"
migrate "$PROD_STORAGE" "$OLD" "selc.infra.resources.product-cdc.prod-ar.tfstate"      "$SEL_PRODUCT_CDC"
migrate "$PROD_STORAGE" "$OLD" "selc.infra.resources.registry-proxy.prod-ar.tfstate"   "$SEL_REGISTRY_PROXY_AR"
migrate "$PROD_STORAGE" "$OLD" "selc.infra.resources.registry-proxy.prod-pnpg.tfstate" "$SEL_REGISTRY_PROXY_PNPG"
migrate "$PROD_STORAGE" "$OLD" "selc.infra.resources.search.prod-ar.tfstate"           "$SEL_SEARCH"
migrate "$PROD_STORAGE" "$OLD" "selc.infra.resources.webhook.prod-ar.tfstate"          "$SEL_WEBHOOK"

# =============================================================================
sep
log "Migration complete."
log ""
log "Next steps for each new category/<env> directory:"
log "  1.  cd infra/resources/<category>/<env>"
log "  2.  terraform init"
log "  3.  terraform plan   # should show no unexpected changes"
log "  4.  Once verified, delete old state blobs manually."
