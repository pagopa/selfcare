output "identity_client_id" {
  value = module.identity_ci.identity_client_id
}

output "identity_principal_id" {
  value = module.identity_ci.identity_principal_id
}

output "identity_fe_client_id" {
  value = module.identity_ci_fe.identity_client_id
}

output "identity_ms_client_id" {
  value = module.identity_ci_ms.identity_client_id
}
