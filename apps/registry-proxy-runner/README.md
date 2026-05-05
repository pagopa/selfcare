# Registry Proxy Runner

Quarkus microservice that runs as a scheduled job (4 times a day, every 6 hours) to feed Azure AI
Search indexes with IPA institution data from IndicePA open data.

## What it does

1. Fetches IPA institution CSV data from `indicepa.gov.it`
2. Compares each institution's `updateDate` with the value stored in the AI Search index
3. Indexes only new or updated institutions in batches of 1000

## Build & Run

```bash
# Build
mvn -f apps/registry-proxy-runner/pom.xml clean package

# Dev mode
mvn -f apps/registry-proxy-runner/pom.xml quarkus:dev

# Run
java -jar apps/registry-proxy-runner/target/quarkus-app/quarkus-run.jar
```

## Configuration

| Property                                  | Env Variable                         | Default                                           |
|-------------------------------------------|--------------------------------------|---------------------------------------------------|
| `scheduler.ipa-index.cron`                | -                                    | `0 0 0/6 * * ?`                                   |
| `quarkus.rest-client.ipa-open-data.url`   | `IPA_OPEN_DATA_URL`                  | `https://indicepa.gov.it/ipa-dati/datastore/dump` |
| `quarkus.rest-client.azure-ai-search.url` | `AZURE_SEARCH_BASE_URL`              | -                                                 |
| `azure-ai-search.api-key`                 | `AZURE_SEARCH_API_KEY`               | -                                                 |
| `azure-ai-search.index-name`              | `AZURE_SEARCH_IPA_INSTITUTION_INDEX` | `ipa-institution-index-ar`                        |
| `azure-ai-search.api-version`             | `AZURE_SEARCH_API_VERSION`           | `2023-11-01`                                      |
