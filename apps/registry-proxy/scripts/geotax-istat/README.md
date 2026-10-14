# geotax-istat – popolamento indice di fallback geotassonomie

Script Python, da lanciare **a mano**, che popola l'indice Azure AI Search `geotax-istat-index-<domain>`.
`registry-proxy` usa questo indice come fallback di `GET /geotaxonomies/{geotaxId}` quando il datalake (Anagrafe Nazionale) non restituisce la geotassonomia, ad esempio per codici ISTAT già aggiornati da IPA ma non ancora da ANPR (caso Sardegna).

Specifica completa: `docs/specs/GEOTAX_ISTAT_FALLBACK/PLAN.md`.

## Fonti dati (scaricate in autonomia)

| Fonte | Uso |
|---|---|
| IPA open data – enti | insieme dei `Codice_comune_ISTAT` usati dagli enti |
| ISTAT SITUAS – Elenco comuni (report 61, `situas-servizi.istat.it`) | denominazione, codice regione, codice provincia, sigla automobilistica, aggiornati alla data di riferimento (default: oggi) |

IPA contiene solo il codice ISTAT del comune: le altre informazioni arrivano da ISTAT.
I codici IPA assenti in ISTAT vengono segnalati con un `WARNING` e **non** vengono caricati.

> ⚠️ Il vecchio CSV statico `istat.it/storage/codici-unita-amministrative/Elenco-comuni-italiani.csv` è fermo a gennaio 2024 e **non** contiene i nuovi codici sardi (112–119). Resta disponibile con `--istat-source csv`, ma non va usato.

## Setup

```bash
cd apps/registry-proxy/scripts/geotax-istat
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
```

## Utilizzo

```bash
# chiave admin dell'AI Search (non viene mai stampata)
export AZURE_SEARCH_API_KEY=$(az keyvault secret show --vault-name <kv> --name azure-search-api-key --query value -o tsv)

# 1. anteprima senza upload
python upload_geotax_istat.py --dry-run --output preview.json

# 2. validazione della mappatura su codici noti al datalake (consigliata)
export GEOTAXONOMY_API_KEY=...
python upload_geotax_istat.py --dry-run --verify-against-datalake 20

# 3. upload (default: tutti i comuni presenti in IPA)
python upload_geotax_istat.py \
  --search-endpoint https://selc-d-weu-ar-srch.search.windows.net \
  --index-name geotax-istat-index-ar
```

### Opzioni principali

| Opzione | Descrizione |
|---|---|
| `--dry-run` | costruisce i documenti senza caricarli |
| `--output file.json` | salva i documenti generati |
| `--region 20` | carica solo una regione (codice ISTAT, 20 = Sardegna) |
| `--codes file.txt` | carica solo i codici elencati (uno per riga) invece di quelli IPA |
| `--province-source historic\|uts` | colonna ISTAT usata per `provinceId`: `historic` (default, es. Roma = `058`, come il datalake) oppure unità territoriale sovracomunale |
| `--reference-date dd/MM/yyyy` | data di riferimento dell'elenco ISTAT SITUAS (default: oggi) |
| `--istat-source situas\|csv` | fonte ISTAT (default `situas`) |
| `--verify-against-datalake N` | confronta N documenti con il datalake e segnala con `WARNING` i campi diversi |
| `--ipa-file` / `--istat-file` | usa file locali invece del download |

L'upload usa `mergeOrUpload` a batch da 1000, quindi lo script è **idempotente** e si può rilanciare.
Exit code: `0` ok, `1` almeno un documento non indicizzato, `2` errore di configurazione.

### Rimozione di un codice

Quando l'Anagrafe si allinea, il datalake ha comunque la precedenza e il documento non viene più usato.
Per rimuoverlo esplicitamente:

```bash
curl -X POST "https://<srch>.search.windows.net/indexes/geotax-istat-index-ar/docs/index?api-version=2023-11-01" \
  -H "api-key: $AZURE_SEARCH_API_KEY" -H "Content-Type: application/json" \
  -d '{"value":[{"@search.action":"delete","id":"<codice>"}]}'
```

## Test

```bash
pip install pytest
python -m pytest -q tests
```



