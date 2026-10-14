#!/usr/bin/env python3
"""
Populate the Azure AI Search index ``geotax-istat-index-<domain>`` used by registry-proxy
as fallback for ``GET /geotaxonomies/{geotaxId}``.

Data sources (both public, downloaded automatically unless local files are provided):
  * IPA open data (enti): distinct ``Codice_comune_ISTAT`` actually used by institutions
  * ISTAT "Elenco comuni italiani": name, region code, province code, car plate abbreviation

Each document has the same shape returned by the datalake, e.g.
  {"id": "058091", "geotaxId": "058091", "istatCode": "058091", "description": "ROMA - COMUNE",
   "provinceId": "058", "provinceAbbreviation": "RM", "regionId": "12",
   "country": "100", "countryAbbreviation": "IT", "enabled": true, "updateDate": "2026-10-13"}

Usage examples:
  export AZURE_SEARCH_API_KEY=...            # admin key of the AI Search service (never printed)
  python upload_geotax_istat.py --search-endpoint https://selc-d-weu-ar-srch.search.windows.net \
      --index-name geotax-istat-index-ar --dry-run --output preview.json
  python upload_geotax_istat.py --search-endpoint ... --index-name ... --region 20
"""
from __future__ import annotations

import argparse
import csv
import datetime
import io
import json
import logging
import os
import re
import sys
from dataclasses import dataclass
from typing import Dict, Iterable, Iterator, List, Optional, Sequence, Set, Tuple

import requests

LOG = logging.getLogger("geotax-istat")

DEFAULT_IPA_URL = "https://indicepa.gov.it/ipa-dati/datastore/dump/d09adf99-dc10-4349-8c53-27b1e5aa97b6"
# ISTAT SITUAS "Elenco comuni" (report 61), always up to date at the requested reference date (dd/MM/yyyy)
DEFAULT_SITUAS_URL = "https://situas-servizi.istat.it/publish/reportspooljson?pfun=61&pdata={date}"
# Legacy static CSV: may be outdated (e.g. it does not contain the 2025 Sardinian codes), kept as alternative
DEFAULT_ISTAT_URL = "https://www.istat.it/storage/codici-unita-amministrative/Elenco-comuni-italiani.csv"
DEFAULT_DATALAKE_URL = "https://api.pdnd.pagopa.it/geo-tax"
DEFAULT_API_VERSION = "2023-11-01"
BATCH_SIZE = 1000
HTTP_TIMEOUT = 120

IPA_ISTAT_COLUMN = "Codice_comune_ISTAT"
COUNTRY = "100"
COUNTRY_ABBREVIATION = "IT"
VALID_CODE = re.compile(r"^[0-9A-Za-z]{1,20}$")

# Normalized header prefixes of the ISTAT CSV (headers contain new lines and footnotes)
ISTAT_COL_REGION = "codice regione"
ISTAT_COL_PROVINCE_HISTORIC = "codice provincia (storico)"
ISTAT_COL_PROVINCE_UTS = "codice dell'unità territoriale sovracomunale"
ISTAT_COL_MUNICIPALITY_CODE = "codice comune formato alfanumerico"
ISTAT_COL_NAME_IT = "denominazione in italiano"
ISTAT_COL_PLATE = "sigla automobilistica"


@dataclass(frozen=True)
class IstatMunicipality:
    code: str
    name: str
    region_id: str
    province_id_historic: str
    province_id_uts: str
    province_abbreviation: str


# --------------------------------------------------------------------------------------
# Parsing
# --------------------------------------------------------------------------------------
def normalize_header(header: str) -> str:
    return re.sub(r"\s+", " ", (header or "").replace("\ufeff", "")).strip().lower()


def normalize_istat_code(code: Optional[str]) -> Optional[str]:
    """Normalize an ISTAT municipality code to 6 digits (leading zeros may be lost in IPA)."""
    if code is None:
        return None
    value = code.strip()
    if not value:
        return None
    if value.isdigit() and len(value) < 6:
        value = value.zfill(6)
    return value if VALID_CODE.match(value) else None


def _find_column(headers: Sequence[str], prefix: str) -> str:
    for header in headers:
        if normalize_header(header).startswith(prefix):
            return header
    raise ValueError(f"Column starting with '{prefix}' not found in ISTAT CSV headers")


def parse_istat_csv(content: str) -> Dict[str, IstatMunicipality]:
    reader = csv.DictReader(io.StringIO(content), delimiter=";")
    headers = reader.fieldnames or []
    col_region = _find_column(headers, ISTAT_COL_REGION)
    col_prov_hist = _find_column(headers, ISTAT_COL_PROVINCE_HISTORIC)
    col_prov_uts = _find_column(headers, ISTAT_COL_PROVINCE_UTS)
    col_code = _find_column(headers, ISTAT_COL_MUNICIPALITY_CODE)
    col_name = _find_column(headers, ISTAT_COL_NAME_IT)
    col_plate = _find_column(headers, ISTAT_COL_PLATE)

    result: Dict[str, IstatMunicipality] = {}
    for row in reader:
        code = normalize_istat_code(row.get(col_code))
        if not code:
            continue
        result[code] = IstatMunicipality(
            code=code,
            name=(row.get(col_name) or "").strip(),
            region_id=(row.get(col_region) or "").strip().zfill(2),
            province_id_historic=(row.get(col_prov_hist) or "").strip().zfill(3),
            province_id_uts=(row.get(col_prov_uts) or "").strip().zfill(3),
            province_abbreviation=(row.get(col_plate) or "").strip(),
        )
    LOG.info("Parsed %d municipalities from ISTAT list", len(result))
    return result


def parse_situas_json(payload: dict) -> Dict[str, IstatMunicipality]:
    """Parses the ISTAT SITUAS report 61 (Elenco comuni) JSON payload."""
    rows = payload.get("resultset")
    if rows is None:
        raise ValueError("'resultset' not found in SITUAS response")
    result: Dict[str, IstatMunicipality] = {}
    for row in rows:
        code = normalize_istat_code(str(row.get("PRO_COM_T") or ""))
        if not code:
            continue
        result[code] = IstatMunicipality(
            code=code,
            name=str(row.get("COMUNE_IT") or row.get("COMUNE") or "").strip(),
            region_id=str(row.get("COD_REG") or "").strip().zfill(2),
            province_id_historic=str(row.get("COD_PROV_STORICO") or "").strip().zfill(3),
            province_id_uts=str(row.get("COD_UTS") or "").strip().zfill(3),
            province_abbreviation=str(row.get("SIGLA_AUTOMOBILISTICA") or "").strip(),
        )
    LOG.info("Parsed %d municipalities from ISTAT SITUAS", len(result))
    return result


def parse_ipa_codes(content: str, delimiter: str = ",") -> Set[str]:
    reader = csv.DictReader(io.StringIO(content), delimiter=delimiter)
    headers = [h.replace("\ufeff", "").strip() for h in (reader.fieldnames or [])]
    reader.fieldnames = headers
    if IPA_ISTAT_COLUMN not in headers:
        raise ValueError(f"Column '{IPA_ISTAT_COLUMN}' not found in IPA CSV (check --ipa-delimiter)")
    codes: Set[str] = set()
    invalid = 0
    for row in reader:
        raw = row.get(IPA_ISTAT_COLUMN)
        code = normalize_istat_code(raw)
        if code:
            codes.add(code)
        elif raw and raw.strip():
            invalid += 1
    if invalid:
        LOG.warning("Skipped %d IPA rows with invalid %s", invalid, IPA_ISTAT_COLUMN)
    LOG.info("Found %d distinct ISTAT municipality codes in IPA", len(codes))
    return codes


# --------------------------------------------------------------------------------------
# Documents
# --------------------------------------------------------------------------------------
def build_document(municipality: IstatMunicipality, province_source: str, update_date: str) -> dict:
    province_id = (municipality.province_id_uts if province_source == "uts"
                   else municipality.province_id_historic)
    return {
        "@search.action": "mergeOrUpload",
        "id": municipality.code,
        "geotaxId": municipality.code,
        "istatCode": municipality.code,
        "description": f"{municipality.name.upper()} - COMUNE",
        "provinceId": province_id,
        "provinceAbbreviation": municipality.province_abbreviation,
        "regionId": municipality.region_id,
        "country": COUNTRY,
        "countryAbbreviation": COUNTRY_ABBREVIATION,
        "enabled": True,
        "updateDate": update_date,
    }


def build_documents(codes: Iterable[str],
                    istat: Dict[str, IstatMunicipality],
                    province_source: str = "historic",
                    region: Optional[str] = None,
                    update_date: Optional[str] = None) -> Tuple[List[dict], List[str]]:
    """Returns (documents, codes missing from the ISTAT list)."""
    update_date = update_date or datetime.date.today().isoformat()
    region = region.zfill(2) if region else None
    documents: List[dict] = []
    missing: List[str] = []
    for code in sorted(set(codes)):
        municipality = istat.get(code)
        if municipality is None:
            missing.append(code)
            continue
        if region and municipality.region_id != region:
            continue
        documents.append(build_document(municipality, province_source, update_date))
    if missing:
        LOG.warning("%d codes not found in ISTAT list and NOT loaded: %s", len(missing), ", ".join(missing))
    LOG.info("Built %d documents (region filter: %s, province source: %s)",
             len(documents), region or "none", province_source)
    return documents, missing


def chunks(items: Sequence[dict], size: int) -> Iterator[Sequence[dict]]:
    for i in range(0, len(items), size):
        yield items[i:i + size]


# --------------------------------------------------------------------------------------
# HTTP
# --------------------------------------------------------------------------------------
def download_text(session: requests.Session, url: str, encoding: str) -> str:
    LOG.info("Downloading %s", url)
    response = session.get(url, timeout=HTTP_TIMEOUT)
    response.raise_for_status()
    return response.content.decode(encoding)


def read_text(path: str, encoding: str) -> str:
    LOG.info("Reading local file %s", path)
    with open(path, "r", encoding=encoding) as f:
        return f.read()


def upload_documents(session: requests.Session, endpoint: str, index_name: str, api_key: str,
                     documents: Sequence[dict], api_version: str = DEFAULT_API_VERSION,
                     batch_size: int = BATCH_SIZE) -> Tuple[int, int]:
    """Uploads documents in batches. Returns (succeeded, failed)."""
    url = f"{endpoint.rstrip('/')}/indexes/{index_name}/docs/index"
    headers = {"api-key": api_key, "Content-Type": "application/json"}
    succeeded = failed = 0
    for n, batch in enumerate(chunks(documents, batch_size), start=1):
        response = session.post(url, params={"api-version": api_version}, headers=headers,
                                data=json.dumps({"value": list(batch)}), timeout=HTTP_TIMEOUT)
        if response.status_code not in (200, 207):
            LOG.error("Batch %d failed with HTTP %d: %s", n, response.status_code, response.text[:500])
            failed += len(batch)
            continue
        for item in response.json().get("value", []):
            if item.get("status"):
                succeeded += 1
            else:
                failed += 1
                LOG.warning("Document %s not indexed (status %s): %s",
                            item.get("key"), item.get("statusCode"), item.get("errorMessage"))
        LOG.info("Batch %d uploaded (%d documents)", n, len(batch))
    return succeeded, failed


def verify_against_datalake(session: requests.Session, datalake_url: str, api_key: str,
                            documents: Sequence[dict], sample_size: int) -> int:
    """Compares generated documents with the datalake for codes the datalake knows.
    Returns the number of mismatches found."""
    fields = {
        "description": "description",
        "provinceId": "province_id",
        "provinceAbbreviation": "province_abbreviation",
        "regionId": "region_id",
        "country": "country",
        "countryAbbreviation": "country_abbreviation",
    }
    compared = mismatches = 0
    for doc in documents:
        if compared >= sample_size:
            break
        response = session.get(f"{datalake_url.rstrip('/')}/geotaxonomy/{doc['id']}",
                               headers={"x-api-key": api_key}, timeout=HTTP_TIMEOUT)
        if response.status_code == 404:
            LOG.info("Verify: code %s not on datalake (expected for updated codes), skipped", doc["id"])
            continue
        if response.status_code != 200:
            LOG.warning("Verify: datalake returned HTTP %d for code %s", response.status_code, doc["id"])
            continue
        compared += 1
        remote = response.json()
        for local_field, remote_field in fields.items():
            local_value = str(doc.get(local_field) or "").upper()
            remote_value = str(remote.get(remote_field) or "").upper()
            if local_value != remote_value:
                mismatches += 1
                LOG.warning("Verify: code %s field %s mismatch: generated='%s' datalake='%s'",
                            doc["id"], local_field, doc.get(local_field), remote.get(remote_field))
    LOG.info("Verify: compared %d documents with datalake, %d field mismatches", compared, mismatches)
    return mismatches


# --------------------------------------------------------------------------------------
# CLI
# --------------------------------------------------------------------------------------
def parse_args(argv: Optional[Sequence[str]] = None) -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Populate the geotaxonomy ISTAT fallback AI Search index")
    p.add_argument("--search-endpoint", help="https://<service>.search.windows.net (required unless --dry-run)")
    p.add_argument("--index-name", default="geotax-istat-index-ar")
    p.add_argument("--api-version", default=DEFAULT_API_VERSION)
    p.add_argument("--ipa-url", default=DEFAULT_IPA_URL)
    p.add_argument("--ipa-file", help="Local IPA CSV (instead of downloading)")
    p.add_argument("--ipa-delimiter", default=",")
    p.add_argument("--istat-source", choices=["situas", "csv"], default="situas",
                   help="situas (default, always up to date) or csv (legacy static list, may be outdated)")
    p.add_argument("--situas-url", default=DEFAULT_SITUAS_URL, help="SITUAS URL template with {date} placeholder")
    p.add_argument("--reference-date", default=None,
                   help="Reference date for SITUAS, dd/MM/yyyy (default: today)")
    p.add_argument("--istat-url", default=DEFAULT_ISTAT_URL)
    p.add_argument("--istat-file", help="Local ISTAT file (CSV, or SITUAS JSON with --istat-source situas)")
    p.add_argument("--istat-encoding", default="cp1252")
    p.add_argument("--codes", help="File with explicit ISTAT codes (one per line) instead of IPA")
    p.add_argument("--region", help="Load only municipalities of this ISTAT region code (e.g. 20 = Sardegna)")
    p.add_argument("--province-source", choices=["historic", "uts"], default="historic",
                   help="ISTAT column used for provinceId (default: historic, e.g. Roma=058)")
    p.add_argument("--verify-against-datalake", type=int, default=0, metavar="N",
                   help="Compare N generated documents with the datalake (needs GEOTAXONOMY_API_KEY)")
    p.add_argument("--datalake-url", default=os.environ.get("GEO_TAXONOMY_URL", DEFAULT_DATALAKE_URL))
    p.add_argument("--dry-run", action="store_true", help="Do not upload, only build documents")
    p.add_argument("--output", help="Write generated documents to this JSON file")
    p.add_argument("--verbose", action="store_true")
    return p.parse_args(argv)


def load_istat(session: requests.Session, args: argparse.Namespace) -> Dict[str, IstatMunicipality]:
    if args.istat_source == "situas":
        if args.istat_file:
            return parse_situas_json(json.loads(read_text(args.istat_file, "utf-8")))
        date = args.reference_date or datetime.date.today().strftime("%d/%m/%Y")
        return parse_situas_json(json.loads(download_text(session, args.situas_url.format(date=date), "utf-8")))
    LOG.warning("Using legacy ISTAT CSV: it may not contain the most recent ISTAT codes")
    istat_text = (read_text(args.istat_file, args.istat_encoding) if args.istat_file
                  else download_text(session, args.istat_url, args.istat_encoding))
    return parse_istat_csv(istat_text)


def main(argv: Optional[Sequence[str]] = None) -> int:
    args = parse_args(argv)
    logging.basicConfig(level=logging.DEBUG if args.verbose else logging.INFO,
                        format="%(asctime)s %(levelname)s %(message)s")
    session = requests.Session()

    istat = load_istat(session, args)

    if args.codes:
        with open(args.codes, encoding="utf-8") as f:
            codes = {c for c in (normalize_istat_code(line) for line in f) if c}
        LOG.info("Loaded %d codes from %s", len(codes), args.codes)
    else:
        ipa_text = (read_text(args.ipa_file, "utf-8-sig") if args.ipa_file
                    else download_text(session, args.ipa_url, "utf-8-sig"))
        codes = parse_ipa_codes(ipa_text, args.ipa_delimiter)

    documents, _ = build_documents(codes, istat, args.province_source, args.region)

    if args.output:
        with open(args.output, "w", encoding="utf-8") as f:
            json.dump(documents, f, ensure_ascii=False, indent=2)
        LOG.info("Documents written to %s", args.output)

    if args.verify_against_datalake > 0:
        datalake_key = os.environ.get("GEOTAXONOMY_API_KEY")
        if not datalake_key:
            LOG.error("GEOTAXONOMY_API_KEY env variable is required for --verify-against-datalake")
            return 2
        if verify_against_datalake(session, args.datalake_url, datalake_key, documents,
                                   args.verify_against_datalake) > 0:
            LOG.warning("Mapping mismatches found: check --province-source before uploading")

    if args.dry_run:
        LOG.info("Dry run: %d documents NOT uploaded", len(documents))
        return 0

    if not args.search_endpoint:
        LOG.error("--search-endpoint is required when not in --dry-run")
        return 2
    api_key = os.environ.get("AZURE_SEARCH_API_KEY")
    if not api_key:
        LOG.error("AZURE_SEARCH_API_KEY env variable is required")
        return 2
    if not documents:
        LOG.warning("No documents to upload")
        return 0

    succeeded, failed = upload_documents(session, args.search_endpoint, args.index_name, api_key,
                                         documents, args.api_version)
    LOG.info("Upload completed on index %s: %d succeeded, %d failed", args.index_name, succeeded, failed)
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())






