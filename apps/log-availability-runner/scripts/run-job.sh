#!/usr/bin/env bash
# Starts a manual execution of the daily availability Container App Job.
#
# Usage: run-job.sh <dev|uat|prod> <yyyy-MM-dd|--backfill>
# Set DRY_RUN=1 to print the az command without starting the job.
set -euo pipefail

usage() {
  echo "Usage: $(basename "$0") <dev|uat|prod> <yyyy-MM-dd|--backfill>" >&2
  exit 2
}

[[ $# -eq 2 ]] || usage

case "$1" in
  dev)  env_short=d; subscription=DEV-SelfCare ;;
  uat)  env_short=u; subscription=UAT-SelfCare ;;
  prod) env_short=p; subscription=PROD-SelfCare ;;
  *)    usage ;;
esac

job_arg="$2"
if [[ "$job_arg" != "--backfill" && ! "$job_arg" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
  usage
fi

resource_group="selc-${env_short}-container-app-002-rg"
job_name="selc-${env_short}-availability-daily-job"

job_query() {
  az containerapp job show --only-show-errors \
    --subscription "$subscription" -n "$job_name" -g "$resource_group" \
    --query "properties.template.containers[0].$1" -o tsv
}

# Overriding --args replaces the job template, so image, resources and env vars must be passed again.
image="$(job_query image)"
container_name="$(job_query name)"
cpu="$(job_query resources.cpu)"
memory="$(job_query resources.memory)"
env_output="$(job_query "env[].join('=',[name,value])")"
env_vars=()
while IFS= read -r env_var; do
  [[ -n "$env_var" ]] && env_vars+=("$env_var")
done <<< "$(printf '%s\n' "$env_output" | tr '\t' '\n')"

if [[ -z "$image" || -z "$container_name" || -z "$cpu" || -z "$memory" || ${#env_vars[@]} -eq 0 ]]; then
  echo "Unable to read the definition of $job_name in $resource_group ($subscription)" >&2
  exit 1
fi

cmd=(az containerapp job start --only-show-errors
  --subscription "$subscription" -n "$job_name" -g "$resource_group"
  --image "$image" --container-name "$container_name"
  --cpu "$cpu" --memory "$memory"
  "--args=$job_arg"
  --env-vars "${env_vars[@]}")

if [[ "${DRY_RUN:-0}" == "1" ]]; then
  printf '%q ' "${cmd[@]}"
  echo
  exit 0
fi

echo "Starting $job_name ($subscription) with argument $job_arg"
"${cmd[@]}" --query "{execution:name, status:properties.status}" -o table
echo "Check status: az containerapp job execution list --subscription $subscription -n $job_name -g $resource_group -o table"
