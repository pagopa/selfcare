###############################################################################
# Common analyzers/tokenizers locals
###############################################################################
locals {
  autocomplete_analyzers = [
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
  ]

  autocomplete_tokenizers = [
    {
      "name" : "autocomplete_tokenizer",
      "@odata.type" : "#Microsoft.Azure.Search.EdgeNGramTokenizer",
      "minGram" : 3,
      "maxGram" : 10,
      "tokenChars" : ["letter", "digit"]
    }
  ]
}

###############################################################################
# IPA Institution index
# Key = taxCode (Codice_fiscale_ente)
# Data_aggiornamento is stored as a filterable/sortable string (yyyy-mm-dd)
# so the scheduler can skip records whose dataAggiornamento has not changed.
###############################################################################
resource "restapi_object" "ipa_institution_index" {
  provider     = restapi.search
  query_string = "api-version=${var.api_version}"
  id_attribute = "name"
  path         = "/indexes"

  lifecycle {
    ignore_changes = [data, api_data, api_response]
  }

  data = jsonencode({
    "name" : "ipa-institution-index-${var.domain}",
    "analyzers" : local.autocomplete_analyzers,
    "tokenizers" : local.autocomplete_tokenizers,
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
        "name" : "originId",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : true,
        "sortable" : true,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "taxCode",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : true,
        "filterable" : true,
        "sortable" : true,
        "facetable" : false,
        "retrievable" : true,
        "analyzer" : "standard.lucene"
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
        "name" : "category",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : true,
        "sortable" : false,
        "facetable" : true,
        "retrievable" : true
      },
      {
        "name" : "digitalAddress",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "address",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "zipCode",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : true,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "istatCode",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : true,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "origin",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : true,
        "sortable" : false,
        "facetable" : true,
        "retrievable" : true
      },
      # Data_aggiornamento (yyyy-mm-dd) — used by the scheduler to detect
      # whether a record needs to be updated instead of blindly re-indexing.
      {
        "name" : "dataAggiornamento",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : true,
        "sortable" : true,
        "facetable" : false,
        "retrievable" : true
      }
    ]
  })
}

###############################################################################
# IPA AOO index (Area Organizzativa Omogenea)
# Key = codiceUniAoo
###############################################################################
resource "restapi_object" "ipa_aoo_index" {
  provider     = restapi.search
  query_string = "api-version=${var.api_version}"
  id_attribute = "name"
  path         = "/indexes"

  lifecycle {
    ignore_changes = [data, api_data, api_response]
  }

  data = jsonencode({
    "name" : "ipa-aoo-index-${var.domain}",
    "analyzers" : local.autocomplete_analyzers,
    "tokenizers" : local.autocomplete_tokenizers,
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
        "name" : "codiceIpa",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : true,
        "sortable" : true,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "denominazioneEnte",
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
        "name" : "codiceFiscaleEnte",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : true,
        "filterable" : true,
        "sortable" : true,
        "facetable" : false,
        "retrievable" : true,
        "analyzer" : "standard.lucene"
      },
      {
        "name" : "codiceUniAoo",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : true,
        "sortable" : true,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "denominazioneAoo",
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
        "name" : "mail1",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "mail2",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "mail3",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "codAoo",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : true,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "dataIstituzione",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : true,
        "sortable" : true,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "nomeResponsabile",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "cognomeResponsabile",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "mailResponsabile",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "telefonoResponsabile",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "codiceComuneISTAT",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : true,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "codiceCatastaleComune",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "cap",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "indirizzo",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "telefono",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "fax",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "tipoMail1",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "tipoMail2",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "tipoMail3",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "protocolloInformatico",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "uriProtocolloInformatico",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      # Data_aggiornamento (yyyy-mm-dd) — scheduler uses this to decide
      # whether each AOO record needs to be updated.
      {
        "name" : "dataAggiornamento",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : true,
        "sortable" : true,
        "facetable" : false,
        "retrievable" : true
      }
    ]
  })
}

###############################################################################
# IPA UO index (Unità Organizzativa)
# Key = codiceUniUo
###############################################################################
resource "restapi_object" "ipa_uo_index" {
  provider     = restapi.search
  query_string = "api-version=${var.api_version}"
  id_attribute = "name"
  path         = "/indexes"

  lifecycle {
    ignore_changes = [data, api_data, api_response]
  }

  data = jsonencode({
    "name" : "ipa-uo-index-${var.domain}",
    "analyzers" : local.autocomplete_analyzers,
    "tokenizers" : local.autocomplete_tokenizers,
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
        "name" : "codiceIpa",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : true,
        "sortable" : true,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "denominazioneEnte",
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
        "name" : "codiceFiscaleEnte",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : true,
        "filterable" : true,
        "sortable" : true,
        "facetable" : false,
        "retrievable" : true,
        "analyzer" : "standard.lucene"
      },
      {
        "name" : "codiceFiscaleSfe",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : true,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "codiceUniUo",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : true,
        "sortable" : true,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "codiceUniUoPadre",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : true,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "codiceUniAoo",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : true,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "descrizioneUo",
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
        "name" : "mail1",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "mail2",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "mail3",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "dataIstituzione",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : true,
        "sortable" : true,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "nomeResponsabile",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "cognomeResponsabile",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "mailResponsabile",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "telefonoResponsabile",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "codiceComuneISTAT",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : true,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "codiceCatastaleComune",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "cap",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "indirizzo",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "telefono",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "fax",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "tipoMail1",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "tipoMail2",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "tipoMail3",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "url",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : false,
        "sortable" : false,
        "facetable" : false,
        "retrievable" : true
      },
      # Data_aggiornamento (yyyy-mm-dd) — scheduler uses this to decide
      # whether each UO record needs to be updated.
      {
        "name" : "dataAggiornamento",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : true,
        "sortable" : true,
        "facetable" : false,
        "retrievable" : true
      }
    ]
  })
}

###############################################################################
# IPA Category index
# Key = code (Codice_categoria)
# Categories do not carry a Data_aggiornamento field in IPA open data.
###############################################################################
resource "restapi_object" "ipa_category_index" {
  provider     = restapi.search
  query_string = "api-version=${var.api_version}"
  id_attribute = "name"
  path         = "/indexes"

  lifecycle {
    ignore_changes = [data, api_data, api_response]
  }

  data = jsonencode({
    "name" : "ipa-category-index-${var.domain}",
    "analyzers" : local.autocomplete_analyzers,
    "tokenizers" : local.autocomplete_tokenizers,
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
        "name" : "code",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : true,
        "sortable" : true,
        "facetable" : false,
        "retrievable" : true
      },
      {
        "name" : "name",
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
        "name" : "kind",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : true,
        "sortable" : false,
        "facetable" : true,
        "retrievable" : true
      },
      {
        "name" : "origin",
        "type" : "Edm.String",
        "key" : false,
        "searchable" : false,
        "filterable" : true,
        "sortable" : false,
        "facetable" : true,
        "retrievable" : true
      }
    ]
  })
}
