resource "restapi_object" "search_index" {
  provider     = restapi.search
  query_string = "api-version=${var.api_version}"
  id_attribute = "name"
  path         = "/indexes"

  data = jsonencode({
    "name" : "onboarding-index-${var.domain}",
    "analyzers" : [
      {
        "name" : "autocomplete_analyzer",
        "@odata.type" : "#Microsoft.Azure.Search.CustomAnalyzer",
        "tokenizer" : "autocomplete_tokenizer",
        "tokenFilters" : ["lowercase", "asciifolding"]
      },
      {
        "name" : "autocomplete_search_analyzer",
        "@odata.type" : "#Microsoft.Azure.Search.CustomAnalyzer",
        "tokenizer" : "lowercase",
        "tokenFilters" : ["lowercase", "asciifolding"]
      }
    ],
    "tokenizers" : [
      {
        "name" : "autocomplete_tokenizer",
        "@odata.type" : "#Microsoft.Azure.Search.EdgeNGramTokenizer",
        "minGram" : 3,
        "maxGram" : 10,
        "tokenChars" : ["letter", "digit"]
      }
    ],
    "fields" : [
      {
        "name" : "onboardingId",
        "type" : "Edm.String",
        "key" : true,
        "searchable" : false,
        "filterable" : true,
        "sortable" : true,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "institutionId",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : true,
        "sortable" : true,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "description",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : true,
        "filterable" : false,
        "sortable" : true,
        "facetable" : false,
        "retrievable" : true,
        "indexAnalyzer" : "autocomplete_analyzer",
        "searchAnalyzer" : "autocomplete_search_analyzer"
      },
      {
        "name" : "parentDescription",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : true,
        "filterable" : false,
        "sortable" : true,
        "facetable" : false,
        "retrievable" : true,
        "indexAnalyzer" : "autocomplete_analyzer",
        "searchAnalyzer" : "autocomplete_search_analyzer"
      },
      {
        "name" : "taxCode",
        "type" : "Edm.String",
        "searchable" : true,
        "filterable" : true,
        "sortable" : true,
        "facetable" : false,
        "retrievable" : true,
        "analyzer" : "standard.lucene"
      },
      {
        "name" : "subunitCode",
        "type" : "Edm.String",
        "searchable" : false,
        "filterable" : true,
        "sortable" : true,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "subunitType",
        "type" : "Edm.String",
        "searchable" : false,
        "filterable" : true,
        "sortable" : true,
        "facetable" : true,
        "retrievable" : true
      },
      {
        "name" : "productId",
        "type" : "Edm.String",
        "searchable" : false,
        "filterable" : true,
        "sortable" : true,
        "facetable" : true,
        "retrievable" : true
      },
      {
        "name" : "institutionType",
        "type" : "Edm.String",
        "searchable" : false,
        "filterable" : true,
        "sortable" : true,
        "facetable" : true,
        "retrievable" : true
      },
      {
        "name" : "status",
        "type" : "Edm.String",
        "searchable" : false,
        "filterable" : true,
        "sortable" : true,
        "facetable" : true,
        "retrievable" : true
      },
      {
        "name" : "createdAt",
        "type" : "Edm.DateTimeOffset",
        "retrievable" : true,
        "filterable" : true,
        "sortable" : true,
        "searchable" : false
      },
      {
        "name" : "updatedAt",
        "type" : "Edm.DateTimeOffset",
        "retrievable" : true,
        "filterable" : true,
        "sortable" : true,
        "searchable" : false
      },
      {
        "name" : "activatedAt",
        "type" : "Edm.DateTimeOffset",
        "retrievable" : true,
        "filterable" : true,
        "sortable" : true,
        "searchable" : false
      },
      {
        "name" : "expiringDate",
        "type" : "Edm.DateTimeOffset",
        "retrievable" : true,
        "filterable" : true,
        "sortable" : true,
        "searchable" : false
      }
    ]
  })
}
