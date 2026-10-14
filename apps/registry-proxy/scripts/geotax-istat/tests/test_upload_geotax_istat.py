import json
import os
import sys
from unittest.mock import MagicMock

import pytest

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

import upload_geotax_istat as m  # noqa: E402

ISTAT_CSV = (
    "Codice Regione;\"Codice dell'Unità territoriale sovracomunale \n(valida a fini statistici)\";"
    "Codice Provincia (Storico)(1);Progressivo del Comune (2);Codice Comune formato alfanumerico;"
    "Denominazione (Italiana e straniera);Denominazione in italiano;Sigla automobilistica\n"
    "12;258;058;091;058091;Roma;Roma;RM\n"
    "20;111;111;001;111001;Cagliari;Cagliari;CA\n"
    "08;038;038;008;038008;Ferrara;Ferrara;FE\n"
)

IPA_CSV = (
    "\ufeffCodice_IPA,Denominazione_ente,Codice_comune_ISTAT\n"
    "c_h501,Comune di Roma,058091\n"
    "r_sardeg,Regione Sardegna,111001\n"
    "x,Ente senza istat,\n"
    "y,Ente zeri persi,38008\n"
    "z,Ente codice invalido,12'3\n"
    "w,Ente non in ISTAT,999999\n"
)


def test_normalize_istat_code():
    assert m.normalize_istat_code("58091") == "058091"
    assert m.normalize_istat_code(" 111001 ") == "111001"
    assert m.normalize_istat_code("") is None
    assert m.normalize_istat_code(None) is None
    assert m.normalize_istat_code("12'3") is None


def test_parse_istat_csv_with_multiline_headers():
    istat = m.parse_istat_csv(ISTAT_CSV)
    assert set(istat) == {"058091", "111001", "038008"}
    roma = istat["058091"]
    assert roma.name == "Roma"
    assert roma.region_id == "12"
    assert roma.province_id_historic == "058"
    assert roma.province_id_uts == "258"
    assert roma.province_abbreviation == "RM"


def test_parse_istat_csv_missing_column():
    with pytest.raises(ValueError):
        m.parse_istat_csv("Codice Regione;Altro\n12;x\n")


SITUAS_JSON = {"resultset": [
    {"COD_REG": "20", "COD_PROV_STORICO": "112", "COD_UTS": "312", "PRO_COM_T": "112001",
     "COMUNE": "Alghero", "COMUNE_IT": "Alghero", "SIGLA_AUTOMOBILISTICA": "SS", "COD_CATASTO": "A192"},
    {"COD_REG": "12", "COD_PROV_STORICO": "058", "COD_UTS": "258", "PRO_COM_T": "058091",
     "COMUNE": "Roma", "COMUNE_IT": "Roma", "SIGLA_AUTOMOBILISTICA": "RM"},
    {"COD_REG": "12", "PRO_COM_T": ""},
]}


def test_parse_situas_json():
    istat = m.parse_situas_json(SITUAS_JSON)
    assert set(istat) == {"112001", "058091"}
    alghero = istat["112001"]
    assert alghero.name == "Alghero"
    assert alghero.region_id == "20"
    assert alghero.province_id_historic == "112"
    assert alghero.province_id_uts == "312"
    assert alghero.province_abbreviation == "SS"


def test_parse_situas_json_invalid():
    with pytest.raises(ValueError):
        m.parse_situas_json({"foo": []})


def test_main_dry_run_situas_local_file(tmp_path):
    situas_file = tmp_path / "situas.json"
    situas_file.write_text(json.dumps(SITUAS_JSON), encoding="utf-8")
    codes = tmp_path / "codes.txt"
    codes.write_text("112001\n999999\n", encoding="utf-8")
    out = tmp_path / "out.json"
    rc = m.main(["--istat-file", str(situas_file), "--codes", str(codes), "--dry-run", "--output", str(out)])
    assert rc == 0
    docs = json.loads(out.read_text(encoding="utf-8"))
    assert [d["id"] for d in docs] == ["112001"]
    assert docs[0]["description"] == "ALGHERO - COMUNE"
    assert docs[0]["provinceId"] == "112"


def test_main_situas_download_uses_reference_date(monkeypatch, tmp_path):
    captured = {}

    def fake_download(session, url, encoding):
        captured["url"] = url
        return json.dumps(SITUAS_JSON)

    monkeypatch.setattr(m, "download_text", fake_download)
    codes = tmp_path / "codes.txt"
    codes.write_text("058091\n", encoding="utf-8")
    rc = m.main(["--codes", str(codes), "--reference-date", "13/10/2026", "--dry-run"])
    assert rc == 0
    assert captured["url"].endswith("pfun=61&pdata=13/10/2026")


def test_parse_ipa_codes():
    codes = m.parse_ipa_codes(IPA_CSV)
    assert codes == {"058091", "111001", "038008", "999999"}


def test_parse_ipa_codes_missing_column():
    with pytest.raises(ValueError):
        m.parse_ipa_codes("a,b\n1,2\n")


def test_build_documents_and_missing():
    istat = m.parse_istat_csv(ISTAT_CSV)
    docs, missing = m.build_documents({"058091", "111001", "999999"}, istat, update_date="2026-10-13")
    assert missing == ["999999"]
    assert [d["id"] for d in docs] == ["058091", "111001"]
    roma = docs[0]
    assert roma == {
        "@search.action": "mergeOrUpload",
        "id": "058091",
        "geotaxId": "058091",
        "istatCode": "058091",
        "description": "ROMA - COMUNE",
        "provinceId": "058",
        "provinceAbbreviation": "RM",
        "regionId": "12",
        "country": "100",
        "countryAbbreviation": "IT",
        "enabled": True,
        "updateDate": "2026-10-13",
    }


def test_build_documents_region_filter_and_uts():
    istat = m.parse_istat_csv(ISTAT_CSV)
    docs, _ = m.build_documents({"058091", "111001"}, istat, province_source="uts", region="20")
    assert [d["id"] for d in docs] == ["111001"]
    docs, _ = m.build_documents({"058091"}, istat, province_source="uts")
    assert docs[0]["provinceId"] == "258"


def test_chunks():
    items = list(range(2500))
    sizes = [len(c) for c in m.chunks(items, 1000)]
    assert sizes == [1000, 1000, 500]


def _response(status, payload=None, text=""):
    r = MagicMock()
    r.status_code = status
    r.json.return_value = payload or {}
    r.text = text
    return r


def test_upload_documents_counts_success_and_failures():
    session = MagicMock()
    session.post.side_effect = [
        _response(207, {"value": [{"key": "1", "status": True}, {"key": "2", "status": False,
                                                                   "statusCode": 400, "errorMessage": "bad"}]}),
        _response(503, text="unavailable"),
    ]
    docs = [{"id": str(i)} for i in range(3)]
    ok, ko = m.upload_documents(session, "https://srch/", "idx", "key", docs, batch_size=2)
    assert (ok, ko) == (1, 2)
    url = session.post.call_args_list[0].args[0]
    assert url == "https://srch/indexes/idx/docs/index"
    headers = session.post.call_args_list[0].kwargs["headers"]
    assert headers["api-key"] == "key"
    body = json.loads(session.post.call_args_list[0].kwargs["data"])
    assert len(body["value"]) == 2


def test_verify_against_datalake():
    istat = m.parse_istat_csv(ISTAT_CSV)
    docs, _ = m.build_documents({"058091", "111001", "038008"}, istat)
    session = MagicMock()
    # documents are sorted by code: 038008 (Ferrara), 058091 (Roma), 111001 (Cagliari)
    session.get.side_effect = [
        _response(200, {"description": "FERRARA - COMUNE", "province_id": "038", "province_abbreviation": "FE",
                        "region_id": "08", "country": "100", "country_abbreviation": "IT"}),
        _response(404),
        _response(200, {"description": "CAGLIARI - COMUNE", "province_id": "092", "province_abbreviation": "CA",
                        "region_id": "20", "country": "100", "country_abbreviation": "IT"}),
    ]
    mismatches = m.verify_against_datalake(session, "https://dl", "k", docs, sample_size=5)
    assert mismatches == 1
    assert session.get.call_args_list[0].kwargs["headers"] == {"x-api-key": "k"}


def test_main_dry_run_with_local_files(tmp_path, monkeypatch):
    istat_file = tmp_path / "istat.csv"
    istat_file.write_text(ISTAT_CSV, encoding="cp1252")
    ipa_file = tmp_path / "ipa.csv"
    ipa_file.write_text(IPA_CSV, encoding="utf-8")
    out = tmp_path / "out.json"
    rc = m.main(["--istat-source", "csv", "--istat-file", str(istat_file), "--ipa-file", str(ipa_file),
                 "--dry-run", "--output", str(out)])
    assert rc == 0
    assert len(json.loads(out.read_text(encoding="utf-8"))) == 3


def test_main_requires_api_key(tmp_path, monkeypatch):
    monkeypatch.delenv("AZURE_SEARCH_API_KEY", raising=False)
    istat_file = tmp_path / "istat.csv"
    istat_file.write_text(ISTAT_CSV, encoding="cp1252")
    codes = tmp_path / "codes.txt"
    codes.write_text("111001\n", encoding="utf-8")
    rc = m.main(["--istat-source", "csv", "--istat-file", str(istat_file), "--codes", str(codes),
                 "--search-endpoint", "https://srch"])
    assert rc == 2




