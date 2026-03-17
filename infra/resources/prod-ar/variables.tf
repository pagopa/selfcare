variable "image_tag" {
  type        = string
  description = "Image tag to use for onboarding-* container app"
  default     = "latest"
}

variable "onboarding_image_tag" {
  type        = string
  description = "Image tag to use for onboarding-* container app"
  default     = "latest"
}

variable "auth_image_tag" {
  type        = string
  description = "Image tag to use for auth-ms container app"
  default     = "latest"
}

variable "product_image_tag" {
  type        = string
  description = "Image tag to use for product-ms container app"
  default     = "latest"
}

variable "product_cdc_image_tag" {
  type        = string
  description = "Image tag to use for product-cdc container app"
  default     = "latest"
}

variable "iam_image_tag" {
  type        = string
  description = "Image tag to use for iam-ms container app"
  default     = "latest"
}
