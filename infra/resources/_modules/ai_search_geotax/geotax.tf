###############################################################################
# Geotaxonomy ISTAT fallback index
# Key = ISTAT municipality code (equals the datalake geotaxId).
# Used by registry-proxy as fallback when the datalake (Anagrafe Nazionale)
# does not return a geotaxonomy for an ISTAT code already updated by IPA.
# Populated manually by apps/registry-proxy/scripts/geotax-istat.
###############################################################################
resource "restapi_object" "geotax_istat_index" {
  provider     = restapi.search
  query_string = "api-version=${var.api_version}"
  id_attribute = "name"
  path         = "/indexes"

  lifecycle {
    ignore_changes = [data, api_data, api_response]
  }

  data = jsonencode({
    "name" : "geotax-istat-index-${var.domain}",
    "fields" : [
      {
        "name" : "id",
        "type" : "Edm.String",
        "key" : true,
        "searchable" : false,
        "filterable" : true,
        "sortable" : true,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "geotaxId",
        "type" : "Edm.String",
        "searchable" : false,
        "filterable" : true,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "istatCode",
        "type" : "Edm.String",
        "searchable" : false,
        "filterable" : true,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "description",
        "type" : "Edm.String",
        "searchable" : true,
        "filterable" : false,
        "sortable" : true,
        "facetable" : false,
        "retrievable" : true,
        "analyzer" : "standard.lucene"
      },
      {
        "name" : "provinceId",
        "type" : "Edm.String",
        "searchable" : false,
        "filterable" : true,
        "sortable" : false,
        "facetable" : true,
        "retrievable" : true
      },
      {
        "name" : "provinceAbbreviation",
        "type" : "Edm.String",
        "searchable" : false,
        "filterable" : true,
        "sortable" : false,
        "facetable" : true,
        "retrievable" : true
      },
      {
        "name" : "regionId",
        "type" : "Edm.String",
        "searchable" : false,
        "filterable" : true,
        "sortable" : false,
        "facetable" : true,
        "retrievable" : true
      },
      {
        "name" : "country",
        "type" : "Edm.String",
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "countryAbbreviation",
        "type" : "Edm.String",
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "enabled",
        "type" : "Edm.Boolean",
        "searchable" : false,
        "filterable" : true,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "updateDate",
        "type" : "Edm.String",
        "searchable" : false,
        "filterable" : true,
        "sortable" : true,
        "facetable" : false,
        "retrievable" : true
      }
    ]
  })
}

