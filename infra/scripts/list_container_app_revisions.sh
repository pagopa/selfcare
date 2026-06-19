#!/usr/bin/env bash
set -euo pipefail

###############################################################################
# List active revisions for all Container Apps in a given environment.
#
# Usage:
#   ./list_container_app_revisions.sh -g <resource-group> -e <environment> [-a <app-name>] [-s <subscription-id>]
#
# Options:
#   -g  Resource group name             (required)
#   -e  Container App Environment name  (required)
#   -a  Container App name              (optional; lists all apps if omitted)
#   -s  Azure subscription ID           (optional; uses current az default)
###############################################################################

usage() {
  grep '^#' "$0" | sed 's/^# \{0,1\}//' | tail -n +3
  exit 1
}

RESOURCE_GROUP=""
ENVIRONMENT=""
APP_NAME=""
SUBSCRIPTION_ARGS=""

while getopts "g:e:a:s:h" opt; do
  case $opt in
    g) RESOURCE_GROUP="$OPTARG" ;;
    e) ENVIRONMENT="$OPTARG" ;;
    a) APP_NAME="$OPTARG" ;;
    s) SUBSCRIPTION_ARGS="--subscription $OPTARG" ;;
    h) usage ;;
    *) usage ;;
  esac
done

if [[ -z "$RESOURCE_GROUP" || -z "$ENVIRONMENT" ]]; then
  echo "❌  -g <resource-group> and -e <environment> are required" >&2
  usage
fi

if [[ -n "$APP_NAME" ]]; then
  echo "🔍  Listing revisions for Container App '$APP_NAME' in environment '$ENVIRONMENT' (RG: '$RESOURCE_GROUP')..."
else
  echo "🔍  Listing Container Apps in environment '$ENVIRONMENT' (RG: '$RESOURCE_GROUP')..."
fi
echo ""

APPS=()
if [[ -n "$APP_NAME" ]]; then
  APPS=("$APP_NAME")
else
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
fi

if [[ ${#APPS[@]} -eq 0 ]]; then
  echo "⚠️   No Container Apps found."
  exit 0
fi

for APP in "${APPS[@]}"; do
  echo "=== $APP ==="
  az containerapp revision list \
    --name "$APP" \
    --resource-group "$RESOURCE_GROUP" \
    ${SUBSCRIPTION_ARGS} \
    --query "[?properties.active==\`true\`].{containerApp:'$APP', revision:name, state:properties.runningState, replicas:properties.replicas, created:properties.createdTime}" \
    --output table
  echo ""
done
