output "action_group_error_id" {
  value = length(azurerm_monitor_action_group.error_action_group) > 0 ? azurerm_monitor_action_group.error_action_group[0].id : null
}

output "action_group_slack_id" {
  value = azurerm_monitor_action_group.slack.id
}

output "action_group_email_id" {
  value = azurerm_monitor_action_group.email.id
}

output "action_group_selfcare_status_dev_id" {
  value = length(azurerm_monitor_action_group.selfcare_status_dev) > 0 ? azurerm_monitor_action_group.selfcare_status_dev[0].id : null
}

output "action_group_selfcare_status_uat_id" {
  value = length(azurerm_monitor_action_group.selfcare_status_uat) > 0 ? azurerm_monitor_action_group.selfcare_status_uat[0].id : null
}
