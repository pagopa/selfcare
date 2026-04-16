# Bootstrap

This module sets up the foundational GitHub and Azure resources required for CI/CD pipelines. It provisions:

- **Azure User Assigned Identities** for CI and CD (infra, frontend, microservices domains)
- **Federated identity credentials** (OIDC) to allow GitHub Actions to authenticate with Azure without secrets
- **Azure role assignments** and **Key Vault access policies** for each identity
- **GitHub repository environments** (CI/CD) with branch protection rules
- **GitHub repository secrets** (Azure client IDs, subscription ID, tenant ID)

## Apply

The GitHub Terraform provider requires the `GITHUB_OWNER` environment variable to be set to the `pagopa` organization. Run apply as follows:

```sh
GITHUB_OWNER=pagopa terraform apply
```