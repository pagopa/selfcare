variable "enable_function_app_public_network_access" {
  type        = bool
  description = "Temporarily keep public ingress enabled while verifying private endpoint access; set false for the final state"
  default     = false
}
