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

## Apply

The GitHub Terraform provider requires the `GITHUB_OWNER` environment variable to be set to the `pagopa` organization. Run apply as follows:

```sh
GITHUB_OWNER=pagopa terraform apply
```