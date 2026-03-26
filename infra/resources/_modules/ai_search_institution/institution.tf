resource "azapi_resource" "search_index" {
  type                    = "Microsoft.Search/searchServices/indexes@2025-05-01"
  name                    = "institution-index-${var.domain}"
  parent_id               = var.search_service_id
  schema_validation_enabled = false

  body = {
    properties = {
      analyzers = [
        {
          name            = "autocomplete_analyzer"
          "@odata.type"   = "#Microsoft.Azure.Search.CustomAnalyzer"
          tokenizer       = "autocomplete_tokenizer"
          tokenFilters    = ["lowercase", "asciifolding"]
        },
        {
          name            = "autocomplete_search_analyzer"
          "@odata.type"   = "#Microsoft.Azure.Search.CustomAnalyzer"
          tokenizer       = "lowercase"
          tokenFilters    = ["lowercase", "asciifolding"]
        }
      ]
      tokenizers = [
        {
          name        = "autocomplete_tokenizer"
          "@odata.type" = "#Microsoft.Azure.Search.EdgeNGramTokenizer"
          minGram     = 3
          maxGram     = 10
          tokenChars  = ["letter", "digit"]
        }
      ]
      fields = [
        {
          name        = "id"
          type        = "Edm.String"
          key         = true
          searchable  = false
          filterable  = true
          sortable    = true
          facetable   = false
          retrievable = true
        },
        {
          name           = "description"
          type           = "Edm.String"
          key            = false
          searchable     = true
          filterable     = false
          sortable       = true
          facetable      = false
          retrievable    = true
          indexAnalyzer  = "autocomplete_analyzer"
          searchAnalyzer = "autocomplete_search_analyzer"
        },
        {
          name           = "parentDescription"
          type           = "Edm.String"
          key            = false
          searchable     = true
          filterable     = false
          sortable       = true
          facetable      = false
          retrievable    = true
          indexAnalyzer  = "autocomplete_analyzer"
          searchAnalyzer = "autocomplete_search_analyzer"
        },
        {
          name       = "taxCode"
          type       = "Edm.String"
          searchable = true
          filterable = true
          sortable   = true
          facetable  = false
          analyzer   = "standard.lucene"
        },
        {
          name        = "products"
          type        = "Collection(Edm.String)"
          retrievable = true
          searchable  = true
          filterable  = true
          sortable    = false
          facetable   = true
        },
        {
          name        = "institutionTypes"
          type        = "Collection(Edm.String)"
          retrievable = true
          searchable  = true
          filterable  = true
          sortable    = false
          facetable   = true
        },
        {
          name        = "lastModified"
          type        = "Edm.DateTimeOffset"
          retrievable = true
          filterable  = true
          sortable    = true
          searchable  = false
        }
      ]
    }
  }
}
