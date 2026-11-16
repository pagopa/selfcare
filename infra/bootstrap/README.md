# Bootstrap

This module sets up the foundational GitHub and Azure resources required for CI/CD pipelines. It provisions:

- **Azure User Assigned Identities** for CI and CD (infra, frontend, microservices domains)
- **Federated identity credentials** (OIDC) to allow GitHub Actions to authenticate with Azure without secrets
- **Azure role assignments** and **Key Vault access policies** for each identity
- **GitHub repository environments** (CI/CD) with branch protection rules
- **GitHub repository secrets** (Azure client IDs, subscription ID, tenant ID)

## GitHub Actions runners

Runner container resources are configured through the `github_runner` module's
`container` input. DEV uses 2 CPUs and 4 GiB of memory. Functions release jobs
select both `self-hosted` and their environment label (`dev`, `uat`, or `prod`)
so a release cannot be assigned to a runner from another environment.

The DEV Functions release sets `MAVEN_OPTS=-Xmx2g` for both Maven build and deploy
steps, leaving memory for the runner and JVM native allocations. `MAVEN_OPTS`
limits the Java heap; it does not increase the container's memory allocation.

Runner jobs belong to `infra/bootstrap/<environment>-ar`; the project RBAC Key
Vault and its private networking belong to `infra/core/<environment>-ar`. Deploying
`infra/resources/onboarding-functions/` does not apply changes to these resources.

### GitHub App authentication

The runner job uses the DX `github-selfhosted-runner-on-container-app-jobs`
module (`~> 3.0`, requiring Terraform >= 1.15). Both the KEDA scaler and the
ephemeral runner authenticate using a GitHub App, not a user PAT.

Before applying, install the App on `pagopa/selfcare` with repository permissions
**Actions: read** (queue discovery) and **Administration: write** (runner
registration), and approve any permission changes on the organization's App
installation. Populate these secrets outside Terraform in the new AR project RBAC
Key Vault for each environment:

| Environment | Project Key Vault | Resource group |
| ----------- | ---------------- | -------------- |
| DEV | `selc-d-weu-ar-kv-01` | `selc-d-sec-rg` |
| UAT | `selc-u-weu-ar-kv-01` | `selc-u-sec-rg` |
| PROD | `selc-p-weu-ar-kv-01` | `selc-p-sec-rg` |

The `-01` suffix follows DX naming. These vaults are project resources, not
runner-specific vaults. The runners are their first consumers; migrating other
application secrets and granting access to additional identities is a separate
change. The legacy `selc-<d|u|p>-kv` vaults are retained.

| Secret | Value |
| ------ | ----- |
| `github-runner-app-id` | Numeric GitHub App ID |
| `github-runner-app-installation-id` | Installation ID for the App on `pagopa` |
| `github-runner-app-key` | Private key in PEM format, including line breaks |

The DX module uses versionless Key Vault references and grants the job's
system-assigned identity **Key Vault Secrets User** at the project vault scope.
That role can read all secrets in the vault; review its scope before migrating
unrelated application credentials into this shared vault.
Secret values must not be added to Terraform or committed to the repository.
The container receives `GITHUB_APP_*` secret references, without `GITHUB_PAT`.
Labels (`dev`, `uat`, `prod`), CPU/memory, the existing Container App Environment
and networking are preserved. Scaling remains at 0-5 executions, polling every
30 seconds; the DX scaler enables ETags to reduce GitHub API quota consumption.

### Project RBAC vault rollout

The `infra/core/_modules/key_vault_rbac` module creates an RBAC vault
in the existing core security resource group, with public access disabled,
purge protection and 90-day soft-delete retention. Its private endpoint uses the
existing `selc-<d|u|p>-private-endpoints-snet`; the endpoint and its
`privatelink.vaultcore.azure.net` zone belong to the core VNet resource group.
The zone is linked to the runner VNet. Bootstrap only looks up the existing vault
and manages the runner's RBAC reader assignment through the DX module.
The application vaults retain their authorization modes and shared-identity policies.

These resources are local because the DX core-infrastructure vault submodule
hardcodes the `common` name and does not expose public-network access controls.
No new VNet or subnet is created. If a Key Vault private DNS zone is introduced
elsewhere before this rollout, reuse that zone instead of linking a second zone
with the same name to the VNet.

Do not apply bootstrap against an empty project vault. Use two phases,
starting in DEV and repeating separately in UAT and PROD. Run from a trusted
machine with access to the backend and the VNet's private DNS/network, not from
the runner being migrated.

1. Create only the vault and its private networking:

   ```sh
   cd infra/core/dev-ar
   export ARM_SUBSCRIPTION_ID=1ab5e788-3b98-4c63-bd05-de0c7388c853
   terraform init
   terraform plan -target=module.key_vault_rbac
   terraform apply -target=module.key_vault_rbac
   ```

   Review both plans before approval. Targeting is only for this one-time
   provisioning; review existing core dependencies as well as the four new
   resources. A full core plan must follow to detect any outstanding changes.
   The runner job, CAE and existing runner identities belong to the separate
   bootstrap state and are not managed by this core apply.

2. Populate the three secrets outside Terraform. The operator needs `Backup`
   on the source vault and a temporary **Key Vault Secrets Officer** role on the
   destination vault, or equivalent approved permissions. Subscription
   Contributor/Owner alone does not grant secret data-plane access. The Terraform
   deployment identity also needs permission to create role assignments.

   When the App secrets already exist in the application vault, use encrypted
   Key Vault backup/restore. The source and destination must be in the same
   subscription and Azure geography. This preserves all versions and attributes,
   including PEM line breaks, without displaying plaintext values:

   ```bash
   (
     set -euo pipefail
     umask 077
     subscription=DEV-SelfCare
     source_vault=selc-d-kv
     destination_vault=selc-d-weu-ar-kv-01
     backup_dir=$(mktemp -d)
     trap 'rm -f "$backup_dir/github-runner-app-id.backup" "$backup_dir/github-runner-app-installation-id.backup" "$backup_dir/github-runner-app-key.backup"; rmdir "$backup_dir"' EXIT

     for secret in github-runner-app-id github-runner-app-installation-id github-runner-app-key; do
       az keyvault secret backup --subscription "$subscription" \
         --vault-name "$source_vault" --name "$secret" \
         --file "$backup_dir/$secret.backup" --output none --only-show-errors
       az keyvault secret restore --subscription "$subscription" \
         --vault-name "$destination_vault" --file "$backup_dir/$secret.backup" \
         --output none --only-show-errors
     done
   )
   ```

   Restore fails if the destination secret already exists, including a
   soft-deleted secret. For a partial retry, verify the existing secrets and
   restore only the missing ones; do not delete or purge them automatically.
   If the source secrets do not exist, upload the App ID, installation ID and
   PEM from securely held files using `az keyvault secret set --file ...`,
   with `--encoding utf-8 --output none`.

   Confirm that all three destination secrets exist and their current versions
   are enabled and unexpired. Remove the operator's temporary role after the
   transfer. Do not delete the source secrets during migration.

3. Only after core provisioning and secret transfer, switch to
   `infra/bootstrap/<environment>-ar`, set `GITHUB_OWNER=pagopa`, and run
   `terraform init`. Drain running runner executions, then run a **full**
   `terraform plan` and apply the reviewed changes.
   Bootstrap planning requires the core-managed vault to exist.
   The job references switch to the project vault
   and the DX module manages its RBAC reader assignment. For an already-migrated
   DX job, verify that its name and system identity are preserved. The old
   runner-specific access policy on the application vault is removed; unrelated
   application policies remain unchanged.

4. Verify a workflow requiring the environment's runner label completes.
   Check both secret resolution and GitHub registration, not just Azure's
   execution status. RBAC does **not** remove the job/identity creation ordering
   or permission-propagation delay: if secret references fail initially, verify
   the reader assignment, allow propagation, and resynchronize the same Key Vault
   references. GitHub App permissions are a separate prerequisite.

The new vault has not been applied during this change. If an earlier
runner-specific configuration was independently applied, stop before applying:
renaming an Azure Key Vault requires replacement, not an in-place rename.
Preserve its secrets and review resource names, resource groups and state
ownership before migrating. A Terraform `moved` block cannot transfer ownership
between core and bootstrap states.

For rollback, keep the core-managed vault and its secrets, restore the previous runner
vault references and reader permissions, and review the plan before applying.
Do not destroy the project vault as part of rollback; purge protection prevents
immediate reuse of a deleted vault's name.

### Migrating existing PAT runners

This migration **replaces** `selc-<d|u|p>-infra-ca-job` and its system-assigned
identity with a DX-named job. It is not an in-place authentication update.
No state move is provided: the old and new resources have different names.

1. Complete the project-vault provisioning and App secret transfer above. Pause runner-dependent
   workflows and let running executions finish before applying to an environment.
2. From a machine with access to the Terraform backend and Azure resources, run
   `terraform init` and `terraform plan` in `infra/bootstrap/dev-ar`. Review the
   job and its Key Vault authorization changes; the environment, subnet and shared
   user-assigned identity must not be replaced. Do not rely on the runner being
   replaced to execute this migration.
3. Apply the reviewed plan, then trigger a DEV workflow requiring the `dev`
   label. Verify KEDA starts an execution, the runner registers and completes
   the job, and Key Vault/scaler logs show no authorization errors. Repeat in
   UAT and PROD, one environment at a time.
4. Only after successful verification, retire legacy runner jobs belonging to
   other repositories through their owning infrastructure and revoke unused
   PATs. This module does not manage those jobs or delete Key Vault secrets.

For rollback, retain the previous configuration and PAT until verification is
complete. Reverting and applying recreates the legacy job; this also requires
draining running executions and reviewing the plan.

## Apply

The GitHub Terraform provider requires the `GITHUB_OWNER` environment variable to be set to the `pagopa` organization. Run apply as follows:

```sh
GITHUB_OWNER=pagopa terraform apply
```