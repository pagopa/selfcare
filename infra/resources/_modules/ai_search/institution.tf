resource "restapi_object" "search_index" {
  provider     = restapi.search
  query_string = "api-version=2023-11-01"
  id_attribute = "name"
  path         = "/indexes"

  data = jsonencode({
    "name" : "institution-index-${var.domain}",
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
        "analyzer" : "standard.lucene"
      },
      {
        "name" : "products",
        "type" : "Collection(Edm.String)",
        "retrievable" : true,
        "searchable" : true,
        "filterable" : true,
        "sortable" : false,
        "facetable" : true
        }, {
        "name" : "institutionTypes",
        "type" : "Collection(Edm.String)",
        "retrievable" : true,
        "searchable" : true,
        "filterable" : true,
        "sortable" : false,
        "facetable" : true
      },
      {
        "name" : "lastModified",
        "type" : "Edm.DateTimeOffset",
        "retrievable" : true,
        "filterable" : true,
        "sortable" : true,
        "searchable" : false
      }
    ]
  })

  depends_on = [
    azurerm_search_service.srch_service,
    azurerm_role_assignment.admins_group_to_ai_search_reader,
    azurerm_role_assignment.developers_group_to_ai_search_reader,
    azurerm_role_assignment.infra_ci_to_ai_search_service_contributor,
    azurerm_role_assignment.infra_cd_to_ai_search_service_contributor
  ]
}

# resource "restapi_object" "search_datasource" {
#   provider = restapi.search
#   path         = "/datasources"
#   query_string = "api-version=2023-11-01"
#   id_attribute = "name"

#   data = jsonencode({
#     name = "${var.project}-cosmosdb-aisearch-datasource"
#     type = "cosmosdb"
#     credentials = {
#       # connectionString = replace(data.azurerm_key_vault_secret.cosmosdb_connection_key.value, "AccountEndpoint=", "mongodb://")
#       # connectionString = "ResourceId=${data.azurerm_cosmosdb_account.cosmosdb.id};Database=${var.database_name};IdentityAuthType=AccessToken;"
#       # connectionString = "AccountEndpoint=https://${var.cosmosdb_prefix}-cosmosdb-mongodb-account.mongo.cosmos.azure.com;AccountKey=${data.azurerm_key_vault_secret.cosmosdb_connection_key.value};Database=${var.database_name}"
#       # connectionString = "mongodb://${data.azurerm_cosmosdb_account.cosmosdb.name}:${data.azurerm_key_vault_secret.cosmosdb_connection_key.value}@${data.azurerm_cosmosdb_account.cosmosdb.name}.mongo.cosmos.azure.com:10255/${var.database_name}?ssl=true&retrywrites=false&maxIdleTimeMS=120000&appName=@${data.azurerm_cosmosdb_account.cosmosdb.name}@"
#       # connectionString = "AccountEndpoint=https://${data.azurerm_cosmosdb_account.cosmosdb.name}.documents.azure.com;AccountKey=${data.azurerm_key_vault_secret.cosmosdb_connection_key.value};Database=${var.database_name}"
#       # connectionString = "mongodb://${data.azurerm_cosmosdb_account.cosmosdb.name}:${data.azurerm_key_vault_secret.cosmosdb_connection_key.value}@${data.azurerm_cosmosdb_account.cosmosdb.name}.mongo.cosmos.azure.com:10255/${var.database_name}?ssl=true&retrywrites=false&maxIdleTimeMS=120000&appName=@${data.azurerm_cosmosdb_account.cosmosdb.name}@"

#       # connectionString = "AccountEndpoint=https://${data.azurerm_cosmosdb_account.cosmosdb.name}.mongo.cosmos.azure.com;AccountKey=${data.azurerm_key_vault_secret.cosmosdb_connection_key.value};Database=${var.database_name};ApiKind=MongoDb"
#       connectionString = "AccountEndpoint=https://${data.azurerm_cosmosdb_account.cosmosdb.name}.documents.azure.com;AccountKey=${data.azurerm_key_vault_secret.cosmosdb_connection_key.value};Database=${var.database_name};ApiKind=MongoDb"
#     }
#     container = {
#       name = var.collection_name
#     }
#     dataChangeDetectionPolicy = {
#       "@odata.type"           = "#Microsoft.Azure.Search.HighWaterMarkChangeDetectionPolicy"
#       highWaterMarkColumnName = "_ts" # Campo timestamp di Cosmos DB per il tracciamento delle modifiche
#     }
#     dataDeletionDetectionPolicy = {
#       "@odata.type"         = "#Microsoft.Azure.Search.SoftDeleteColumnDeletionDetectionPolicy"
#       softDeleteColumnName  = "isDeleted"
#       softDeleteMarkerValue = "true"
#     }
#   })

#   depends_on = [
#     azurerm_search_service.srch_service
#     # ,azurerm_role_assignment.search_to_cosmodb
#   ]
# }

# # 2. Crea l'Indexer
# resource "restapi_object" "search_indexer" {
#   provider = restapi.search

#   path         = "/indexers"
#   query_string = "api-version=2023-11-01"
#   id_attribute = "name"


#   data = jsonencode({
#     name            = "${var.project}-cosmosdb-aisearch-indexer"
#     dataSourceName  = "${var.project}-cosmosdb-aisearch-datasource"
#     targetIndexName = restapi_object.search_index.id
#     schedule = {
#       interval = "PT5M" # Esegui l'indexer ogni ora
#     }
#     parameters = {
#       batchSize              = 50
#       maxFailedItems         = 10
#       maxFailedItemsPerBatch = 5
#     }

#     fieldMappings = [
#       {
#         sourceFieldName = "_id"
#         targetFieldName = "id"
#       },
#       {
#         sourceFieldName = "description"
#         targetFieldName = "description"
#       },
#       {
#         sourceFieldName = "taxCode"
#         targetFieldName = "taxCode"
#       },
#       {
#         sourceFieldName = "_ts"
#         targetFieldName = "systemLastModified"
#       }
#     ]

#     outputFieldMappings = [
#       {
#         sourceFieldName = "/document/onboarding/productId"
#         targetFieldName = "products"
#       },
#       {
#         sourceFieldName = "/document/onboarding/institutionType"
#         targetFieldName = "institutionTypes"
#       }
#     ]
#   })

#   depends_on = [
#     restapi_object.search_index,
#     restapi_object.search_datasource
#   ]
# }
