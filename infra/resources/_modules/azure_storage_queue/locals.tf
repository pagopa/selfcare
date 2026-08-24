locals {
  storage_rg        = "${var.environment.prefix}-${var.environment.env_short}-webhook-storage-rg"
  poison_queue_name = coalesce(var.poison_queue_name, "${var.queue_name}-poison")
}
