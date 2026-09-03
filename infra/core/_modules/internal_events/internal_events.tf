
locals {
  topics = {
    "institution-events" = {
      subscriptions = ["user-group-sub", "user-sub"]
    }
  }

  subscriptions = {
    for subscription in flatten([
      for topic_name, topic in local.topics : [
        for subscription_name in topic.subscriptions : {
          topic_name        = topic_name
          subscription_name = subscription_name
        }
      ]
    ]) : "${subscription.topic_name}/${subscription.subscription_name}" => subscription
  }
}

## Namespace ##

resource "azurerm_resource_group" "internal_events_rg" {
  name     = "${var.prefix}-${var.env_short}-${var.domain}-internal-events-rg"
  location = var.location
}

resource "azurerm_servicebus_namespace" "internal_events" {
  name                = "${var.prefix}-${var.env_short}-${var.domain}-internal-events"
  location            = azurerm_resource_group.internal_events_rg.location
  resource_group_name = azurerm_resource_group.internal_events_rg.name
  sku                 = "Standard"
}

## Namespace Managed Identity ##

resource "azurerm_user_assigned_identity" "internal_events_identity" {
  name                = "${var.prefix}-${var.env_short}-${var.domain}-internal-events-managed-identity"
  location            = azurerm_resource_group.internal_events_rg.location
  resource_group_name = azurerm_resource_group.internal_events_rg.name
  tags                = var.tags
}

resource "azurerm_role_assignment" "internal_events_sender_identity" {
  scope                = azurerm_servicebus_namespace.internal_events.id
  role_definition_name = "Azure Service Bus Data Sender"
  principal_id         = azurerm_user_assigned_identity.internal_events_identity.principal_id
}

resource "azurerm_role_assignment" "internal_events_receiver_identity" {
  scope                = azurerm_servicebus_namespace.internal_events.id
  role_definition_name = "Azure Service Bus Data Receiver"
  principal_id         = azurerm_user_assigned_identity.internal_events_identity.principal_id
}

## Topics ##

resource "azurerm_servicebus_topic" "internal_events_topic" {
  for_each     = local.topics

  name         = each.key
  namespace_id = azurerm_servicebus_namespace.internal_events.id
}

## Subscriptions ##

resource "azurerm_servicebus_subscription" "internal_events_sub" {
  for_each           = local.subscriptions

  name               = each.value.subscription_name
  topic_id           = azurerm_servicebus_topic.internal_events_topic[each.value.topic_name].id
  max_delivery_count = 10
  requires_session   = true
}

## Per Topic Managed Identities ##

# resource "azurerm_user_assigned_identity" "internal_events_sender" {
#   for_each = local.topics

#   name                = "${var.prefix}-${var.env_short}-${var.domain}-${each.key}-sender-managed-identity"
#   location            = azurerm_resource_group.internal_events_rg.location
#   resource_group_name = azurerm_resource_group.internal_events_rg.name
#   tags                = var.tags
# }

# resource "azurerm_role_assignment" "internal_events_sender" {
#   for_each = local.topics

#   scope                = azurerm_servicebus_topic.internal_events_topic[each.key].id
#   role_definition_name = "Azure Service Bus Data Sender"
#   principal_id         = azurerm_user_assigned_identity.internal_events_sender[each.key].principal_id
# }

## Per Subscription Managed Identities ##

# resource "azurerm_user_assigned_identity" "internal_events_receiver" {
#   for_each = local.subscriptions

#   name                = "${var.prefix}-${var.env_short}-${var.domain}-${each.value.topic_name}-${each.value.subscription_name}-receiver-managed-identity"
#   location            = azurerm_resource_group.internal_events_rg.location
#   resource_group_name = azurerm_resource_group.internal_events_rg.name
#   tags                = var.tags
# }

# resource "azurerm_role_assignment" "internal_events_receiver" {
#   for_each = local.subscriptions

#   scope                = azurerm_servicebus_subscription.internal_events_sub[each.key].id
#   role_definition_name = "Azure Service Bus Data Receiver"
#   principal_id         = azurerm_user_assigned_identity.internal_events_receiver[each.key].principal_id
# }
