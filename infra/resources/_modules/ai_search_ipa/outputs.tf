output "institution_index_name" {
  description = "Name of the IPA institution AI Search index"
  value       = "ipa-institution-index-${var.domain}"
}

output "aoo_index_name" {
  description = "Name of the IPA AOO AI Search index"
  value       = "ipa-aoo-index-${var.domain}"
}

output "uo_index_name" {
  description = "Name of the IPA UO AI Search index"
  value       = "ipa-uo-index-${var.domain}"
}

output "category_index_name" {
  description = "Name of the IPA category AI Search index"
  value       = "ipa-category-index-${var.domain}"
}
