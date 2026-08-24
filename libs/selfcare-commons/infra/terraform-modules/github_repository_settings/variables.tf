variable "github" {
  type = object({
    repository = string
  })
}

variable "identity_component" {
  type        = string
  default     = "ms"
  validation {
    condition     = contains(["ms", "fe"], var.identity_component)
    error_message = "The identity_component variable must be either 'ms' or 'fe'."
  }
}
