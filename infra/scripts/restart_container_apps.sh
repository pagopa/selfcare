#!/usr/bin/env bash
set -euo pipefail

###############################################################################
# Restart Azure Container Apps by injecting a reloadAt env var.
#
# Usage:
#   ./restart_container_apps.sh -g <resource-group> [-e <environment>] [-s <subscription-id>]
#   ./restart_container_apps.sh -g <resource-group> -a <app1,app2,...>
#
# Options:
#   -g  Resource group name                  (required)
#   -e  Container App Environment name       (optional; filters apps by environment)
#   -a  Comma-separated list of apps         (optional; defaults to all apps in RG/env)
#   -s  Azure subscription ID                (optional; uses current az default)
###############################################################################

usage() {
  grep '^#' "$0" | sed 's/^# \{0,1\}//' | tail -n +3
  exit 1
}

RESOURCE_GROUP=""
APP_NAMES=""
ENVIRONMENT=""
SUBSCRIPTION_ARGS=""

while getopts "g:a:e:s:h" opt; do
  case $opt in
    g) RESOURCE_GROUP="$OPTARG" ;;
    a) APP_NAMES="$OPTARG" ;;
    e) ENVIRONMENT="$OPTARG" ;;
    s) SUBSCRIPTION_ARGS="--subscription $OPTARG" ;;
    h) usage ;;
    *) usage ;;
  esac
done

if [[ -z "$RESOURCE_GROUP" ]]; then
  echo "❌  -g <resource-group> is required" >&2
  usage
fi

RELOAD_AT="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"

if [[ -z "$APP_NAMES" ]]; then
  if [[ -n "$ENVIRONMENT" ]]; then
    echo "🔍  Listing Container Apps in environment '$ENVIRONMENT' (RG: '$RESOURCE_GROUP')..."
    APPS=()
    while IFS= read -r line; do
      [[ -n "$line" ]] && APPS+=("$line")
    done < <(
      az containerapp list \
        --resource-group "$RESOURCE_GROUP" \
        --environment "$ENVIRONMENT" \
        ${SUBSCRIPTION_ARGS} \
        --query "[].name" \
        --output tsv
    )
  else
    echo "🔍  Listing Container Apps in resource group '$RESOURCE_GROUP'..."
    APPS=()
    while IFS= read -r line; do
      [[ -n "$line" ]] && APPS+=("$line")
    done < <(
      az containerapp list \
        --resource-group "$RESOURCE_GROUP" \
        ${SUBSCRIPTION_ARGS} \
        --query "[].name" \
        --output tsv
    )
  fi
else
  IFS=',' read -ra APPS <<< "$APP_NAMES"
fi

if [[ ${#APPS[@]} -eq 0 ]]; then
  echo "⚠️   No Container Apps found in '$RESOURCE_GROUP'."
  exit 0
fi

echo "🕐  reloadAt = $RELOAD_AT"
echo "📋  Apps to restart: ${APPS[*]}"
echo ""

for APP in "${APPS[@]}"; do
  echo "🔄  Restarting '$APP'..."
  az containerapp update \
    --name "$APP" \
    --resource-group "$RESOURCE_GROUP" \
    ${SUBSCRIPTION_ARGS} \
    --set-env-vars "reloadAt=${RELOAD_AT}" \
    --output none
  echo "✅  '$APP' updated"
done

echo ""
echo "🎉  Done."
