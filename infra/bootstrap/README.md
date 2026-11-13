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

Runner infrastructure belongs to `infra/bootstrap/<environment>-ar`. Deploying
`infra/resources/onboarding-functions/` does not apply changes to runner resources.

### GitHub App authentication

The runner job uses the DX `github-selfhosted-runner-on-container-app-jobs`
module (`~> 3.0`, requiring Terraform >= 1.15). Both the KEDA scaler and the
ephemeral runner authenticate using a GitHub App, not a user PAT.

Before applying, install the App on `pagopa/selfcare` with repository permissions
**Actions: read** (queue discovery) and **Administration: write** (runner
registration). Populate these secrets outside Terraform in each environment's
Key Vault (`selc-d-kv`, `selc-u-kv`, `selc-p-kv`):

| Secret | Value |
| ------ | ----- |
| `github-runner-app-id` | Numeric GitHub App ID |
| `github-runner-app-installation-id` | Installation ID for the App on `pagopa` |
| `github-runner-app-key` | Private key in PEM format, including line breaks |

The DX module uses versionless Key Vault references and grants the job's
system-assigned identity secret-read access using the vault's authorization
mode. Secret values must not be added to Terraform or committed to the repository.
The container receives `GITHUB_APP_*` secret references, without `GITHUB_PAT`.
Labels (`dev`, `uat`, `prod`), CPU/memory, the existing Container App Environment
and networking are preserved. Scaling remains at 0-5 executions, polling every
30 seconds; the DX scaler enables ETags to reduce GitHub API quota consumption.

### Migrating existing PAT runners

This migration **replaces** `selc-<d|u|p>-infra-ca-job` and its system-assigned
identity with a DX-named job. It is not an in-place authentication update.
No state move is provided: the old and new resources have different names.

1. Provision the App secrets and permissions above. Pause runner-dependent
   workflows and let running executions finish before applying to an environment.
2. From a machine with access to the Terraform backend and Azure resources, run
   `terraform init` and `terraform plan` in `infra/bootstrap/dev-ar`. Review the
   job and its Key Vault access replacement; the environment, subnet and shared
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