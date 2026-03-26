output "identity_client_id" {
  value = module.identity_cd.identity_client_id
}

output "identity_principal_id" {
  value = module.identity_cd.identity_principal_id
}

output "identity_fe_client_id" {
  value = module.identity_cd_fe.identity_client_id
}

output "identity_ms_client_id" {
  value = module.identity_cd_ms.identity_client_id
}
