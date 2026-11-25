# DEV PNPG technical session issuer

Only workloads in `selc-d-pnpg-cae-cp` should call
`http://selc-d-pnpg-auth-ms-ca:8082/token/service` with `{"tenantId":"PNPG"}`.
The listener uses plaintext HTTP and trusts the CAE, not a caller identity.
No APIM API, login, OTP collection or cross-CAE network access is provisioned.
Port 8080 permits health probes only. The SDK requires explicit HTTP opt-in.

Before deployment, populate `auth-m2m-iam-uid-pnpg` (canonical lowercase UUID)
and `jwt-private-key-pkcs8` (PNPG RSA private key in PKCS#8 PEM format) in
`selc-d-pnpg-kv`. The signing key must match the PNPG verifier's public key and
`jwt-kid`; recipients must also accept the PNPG session audience and authorize
the technical identity. The runtime identity reads versionless vault references.
Use an auth image supporting tenant-only issuance (`d1e71cc43` or later).

If initialized from the AR template, run `terraform init -reconfigure` before
planning. Confirm the state key is `selc.infra.resources.auth.dev-pnpg.tfstate`;
do not migrate or copy the AR state.

<!-- BEGIN_TF_DOCS -->
## Requirements

| Name | Version |
| ---- | ------- |
| <a name="requirement_terraform"></a> [terraform](#requirement\_terraform) | >=1.10.0 |
| <a name="requirement_azurerm"></a> [azurerm](#requirement\_azurerm) | ~> 4.0 |

## Providers

No providers.

## Modules

| Name | Source | Version |
| ---- | ------ | ------- |
| <a name="module_auth_m2m_proxy"></a> [auth\_m2m\_proxy](#module\_auth\_m2m\_proxy) | ../../_modules/auth_m2m_proxy | n/a |
| <a name="module_container_app_auth_ms"></a> [container\_app\_auth\_ms](#module\_container\_app\_auth\_ms) | ../../_modules/container_app_microservice | n/a |
| <a name="module_local"></a> [local](#module\_local) | ../../_modules/local-env | n/a |

## Resources

No resources.

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_image_tag"></a> [image\_tag](#input\_image\_tag) | Image tag to use for auth | `string` | `"latest"` | no |

## Outputs

| Name | Description |
| ---- | ----------- |
| <a name="output_m2m_proxy_endpoint"></a> [m2m\_proxy\_endpoint](#output\_m2m\_proxy\_endpoint) | n/a |
<!-- END_TF_DOCS -->
