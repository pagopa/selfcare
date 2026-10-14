# ai_search_geotax

Creates the Azure AI Search index `geotax-istat-index-<domain>` used by `registry-proxy`
as fallback for `GET /geotaxonomies/{geotaxId}` when the datalake does not return the
geotaxonomy of an ISTAT code (e.g. codes already updated by IPA but not yet by ANPR).

The index is populated manually with `apps/registry-proxy/scripts/geotax-istat/upload_geotax_istat.py`.

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| api_version | API version to use for Azure Search REST API calls | `string` | `"2024-07-01"` | no |
| domain | Domain (e.g. ar) | `string` | n/a | yes |
| srch_service_name | AI Search service name | `string` | n/a | yes |
| srch_service_primary_key | AI Search service primary key | `string` | n/a | yes |

## Outputs

| Name | Description |
| ---- | ----------- |
| geotax_istat_index_name | Name of the geotaxonomy ISTAT fallback AI Search index |

