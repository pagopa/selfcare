output "container_app_job_name" {
  value = module.scheduled_job.container_app_job_name
}

output "job_principal_id" {
  value = module.scheduled_job.system_assigned_identity_principal_id
}

output "data_collection_rule_id" {
  value = azurerm_monitor_data_collection_rule.availability.id
}
