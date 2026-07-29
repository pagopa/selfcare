output "namespace_name" {
  value       = local.namespace.name
  description = "Service Bus namespace name."
}

output "queue_name" {
  value       = azurerm_servicebus_queue.this.name
  description = "Webhook delivery queue name."
}
