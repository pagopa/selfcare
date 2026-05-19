#!/usr/bin/env python3
"""
Generates a GitHub Pages status page for Selfcare deployment workflows.

The previous implementation tried to read workflow_dispatch inputs directly from
workflow run payloads. GitHub's REST API does not expose those inputs on
workflow_runs, so env/target matching was unreliable. This version builds the
matrix from real deployment signals:

- release_app.yml and release_functions.yml display_title values
- top-level job names of infra publish workflows
- direct trigger workflows that call call_release_app.yml
"""

import json
import os
import re
import sys
import urllib.error
import urllib.request
from datetime import datetime, timezone
from html import escape

REPO = "pagopa/selfcare"
TOKEN = os.environ.get("GITHUB_TOKEN", "")

ENVS = ["dev", "uat", "prod"]
TARGETS = ["ar", "pnpg"]
COLUMNS = [(env, target) for env in ENVS for target in TARGETS]
AR_ONLY_COLUMNS = [(env, "ar") for env in ENVS]
PNPG_ONLY_COLUMNS = [(env, "pnpg") for env in ENVS]
MAX_RUN_PAGES = int(os.environ.get("STATUS_PAGE_MAX_PAGES", "5"))
PER_PAGE = 100

RELEASE_TITLE_RE = re.compile(
    r"^Release (?P<app>.+) \((?P<env>dev|uat|prod) - (?P<target>ar|pnpg)\)$"
)
WRAPPER_JOB_RE = re.compile(
    r"(?:^| / )\[(?P<env>Dev|Uat|Prod)\] - (?P<app>.+?) Release(?: /|$)",
    re.IGNORECASE,
)
INFRA_BRACKET_RE = re.compile(
    r"^\[(?P<env>Dev|UAT|Prod)\].*\((?P<target>AR|PNPG)\)$", re.IGNORECASE
)
INFRA_PREFIX_RE = re.compile(
    r"^(?P<env>DEV|UAT|PROD)\s+(?P<target>ar|pnpg)\b", re.IGNORECASE
)

STATUS_META = {
    "success": ("Success", "dot-success"),
    "failure": ("Failed", "dot-failure"),
    "timed_out": ("Timed out", "dot-failure"),
    "action_required": ("Action required", "dot-warning"),
    "in_progress": ("Running", "dot-running"),
    "queued": ("Queued", "dot-running"),
    "pending": ("Pending", "dot-running"),
    "requested": ("Requested", "dot-running"),
    "waiting": ("Waiting", "dot-running"),
    "cancelled": ("Cancelled", "dot-neutral"),
    "neutral": ("Neutral", "dot-neutral"),
    "skipped": ("Skipped", "dot-neutral"),
    None: ("No runs", "dot-empty"),
}

INFRA_ROWS = [
    {
        "label": "Core Infra",
        "definition_workflow": "publish_infra.yml",
        "variables": ["env", "domain"],
        "combos": COLUMNS,
        "source": {"type": "infra_job", "workflow": "publish_infra.yml"},
    },
    {
        "label": "Identity Infra",
        "definition_workflow": "publish_identity_infra.yml",
        "variables": ["env", "target=ar"],
        "combos": AR_ONLY_COLUMNS,
        "source": {"type": "infra_job", "workflow": "publish_identity_infra.yml"},
    },
    {
        "label": "Registry Proxy Infra",
        "definition_workflow": "publish_registry_proxy_infra.yml",
        "variables": ["env", "target=ar,pnpg"],
        "combos": COLUMNS,
        "source": {"type": "infra_job", "workflow": "publish_registry_proxy_infra.yml"},
    },
    {
        "label": "Hub Spid PNPG Infra",
        "definition_workflow": "publish_onboarding_hub_spid_pnpg_infra.yml",
        "variables": ["env", "target=pnpg"],
        "combos": PNPG_ONLY_COLUMNS,
        "source": {
            "type": "infra_job",
            "workflow": "publish_onboarding_hub_spid_pnpg_infra.yml",
        },
    },
    {
        "label": "Namirial Sign Infra",
        "definition_workflow": "publish_onboarding_namirial_sign_infra.yml",
        "variables": ["env", "target=ar"],
        "combos": AR_ONLY_COLUMNS,
        "source": {
            "type": "infra_job",
            "workflow": "publish_onboarding_namirial_sign_infra.yml",
        },
    },
    {
        "label": "Dashboard Events Infra",
        "definition_workflow": "publish_onboarding_dashboard_events_infra.yml",
        "variables": ["env", "target=ar"],
        "combos": AR_ONLY_COLUMNS,
        "source": {
            "type": "infra_job",
            "workflow": "publish_onboarding_dashboard_events_infra.yml",
        },
    },
]

APP_ROWS = [
    {
        "label": "auth",
        "definition_workflow": "trigger_release_auth.yml",
        "variables": ["env", "branch", "app=auth", "target=ar"],
        "combos": AR_ONLY_COLUMNS,
        "source": {
            "type": "wrapper_run",
            "workflow": "trigger_release_auth.yml",
            "app": "auth",
            "target": "ar",
        },
    },
    {
        "label": "iam",
        "definition_workflow": "trigger_release_iam.yml",
        "variables": ["env", "branch", "app=iam", "target=ar"],
        "combos": AR_ONLY_COLUMNS,
        "source": {
            "type": "wrapper_run",
            "workflow": "trigger_release_iam.yml",
            "app": "iam",
            "target": "ar",
        },
    },
    {
        "label": "product",
        "definition_workflow": "trigger_release_product.yml",
        "variables": ["env", "branch", "app=product", "target=ar"],
        "combos": AR_ONLY_COLUMNS,
        "source": {
            "type": "wrapper_run",
            "workflow": "trigger_release_product.yml",
            "app": "product",
            "target": "ar",
        },
    },
    {
        "label": "product-cdc",
        "definition_workflow": "trigger_release_product_cdc.yml",
        "variables": ["env", "branch", "app=product-cdc", "target=ar"],
        "combos": AR_ONLY_COLUMNS,
        "source": {
            "type": "wrapper_run",
            "workflow": "trigger_release_product_cdc.yml",
            "app": "product-cdc",
            "target": "ar",
        },
    },
    {
        "label": "dashboard-bff",
        "definition_workflow": "trigger_release_dashboard_bff.yml",
        "variables": ["env", "branch", "app=dashboard-bff", "targets=ar,pnpg"],
        "combos": COLUMNS,
        "source": {
            "type": "release_dispatch",
            "workflow": "release_app.yml",
            "app": "dashboard-bff",
        },
    },
    {
        "label": "delegation-cdc",
        "definition_workflow": "trigger_release_delegation_cdc.yml",
        "variables": ["env", "branch", "app=delegation-cdc", "target=ar"],
        "combos": AR_ONLY_COLUMNS,
        "source": {
            "type": "release_dispatch",
            "workflow": "release_app.yml",
            "app": "delegation-cdc",
        },
    },
    {
        "label": "document-ms",
        "definition_workflow": "trigger_release_document_ms.yml",
        "variables": ["env", "branch", "app=document-ms", "target=ar"],
        "combos": AR_ONLY_COLUMNS,
        "source": {
            "type": "wrapper_run",
            "workflow": "trigger_release_document_ms.yml",
            "app": "document-ms",
            "target": "ar",
        },
    },
    {
        "label": "external-api",
        "definition_workflow": "release_app.yml",
        "variables": ["env", "target", "app=external-api"],
        "combos": COLUMNS,
        "source": {
            "type": "release_dispatch",
            "workflow": "release_app.yml",
            "app": "external-api",
        },
    },
    {
        "label": "institution-ms",
        "definition_workflow": "trigger_release_institution_ms.yml",
        "variables": ["env", "branch", "app=institution-ms", "targets=ar,pnpg"],
        "combos": COLUMNS,
        "source": {
            "type": "release_dispatch",
            "workflow": "release_app.yml",
            "app": "institution-ms",
        },
    },
    {
        "label": "institution-send-mail-scheduler",
        "definition_workflow": "trigger_release_institution_send_mail_scheduler.yml",
        "variables": ["env", "branch", "app=institution-send-mail-scheduler", "target=ar"],
        "combos": AR_ONLY_COLUMNS,
        "source": {
            "type": "release_dispatch",
            "workflow": "release_app.yml",
            "app": "institution-send-mail-scheduler",
        },
    },
    {
        "label": "onboarding-bff",
        "definition_workflow": "trigger_release_onboarding_bff.yml",
        "variables": ["env", "branch", "app=onboarding-bff", "targets=ar,pnpg"],
        "combos": COLUMNS,
        "source": {
            "type": "release_dispatch",
            "workflow": "release_app.yml",
            "app": "onboarding-bff",
        },
    },
    {
        "label": "onboarding-cdc",
        "definition_workflow": "trigger_release_onboarding.yml",
        "variables": ["env", "branch", "app=onboarding-cdc", "targets=ar,pnpg"],
        "combos": COLUMNS,
        "source": {
            "type": "release_dispatch",
            "workflow": "release_app.yml",
            "app": "onboarding-cdc",
        },
    },
    {
        "label": "onboarding-functions",
        "definition_workflow": "trigger_release_onboarding.yml",
        "variables": ["env", "branch", "app=onboarding-functions", "targets=ar,pnpg"],
        "combos": COLUMNS,
        "source": {
            "type": "release_dispatch",
            "workflow": "release_functions.yml",
            "app": "onboarding-functions",
        },
    },
    {
        "label": "onboarding-ms",
        "definition_workflow": "trigger_release_onboarding.yml",
        "variables": ["env", "branch", "app=onboarding-ms", "targets=ar,pnpg"],
        "combos": COLUMNS,
        "source": {
            "type": "release_dispatch",
            "workflow": "release_app.yml",
            "app": "onboarding-ms",
        },
    },
    {
        "label": "registry-proxy",
        "definition_workflow": "trigger_release_registry_proxy.yml",
        "variables": ["env", "branch", "app=registry-proxy", "targets=ar,pnpg"],
        "combos": COLUMNS,
        "source": {
            "type": "release_dispatch",
            "workflow": "release_app.yml",
            "app": "registry-proxy",
        },
    },
    {
        "label": "registry-proxy-runner",
        "definition_workflow": "release_app.yml",
        "variables": ["env", "target", "app=registry-proxy-runner"],
        "combos": COLUMNS,
        "source": {
            "type": "release_dispatch",
            "workflow": "release_app.yml",
            "app": "registry-proxy-runner",
        },
    },
    {
        "label": "user-cdc",
        "definition_workflow": "trigger_release_user_cdc.yml",
        "variables": ["env", "branch", "app=user-cdc", "targets=ar,pnpg"],
        "combos": COLUMNS,
        "source": {
            "type": "release_dispatch",
            "workflow": "release_app.yml",
            "app": "user-cdc",
        },
    },
    {
        "label": "user-group-cdc",
        "definition_workflow": "trigger_release_user_group_cdc.yml",
        "variables": ["env", "branch", "app=user-group-cdc", "target=ar"],
        "combos": AR_ONLY_COLUMNS,
        "source": {
            "type": "release_dispatch",
            "workflow": "release_app.yml",
            "app": "user-group-cdc",
        },
    },
    {
        "label": "user-group-ms",
        "definition_workflow": "trigger_release_user_group_ms.yml",
        "variables": ["env", "branch", "app=user-group-ms", "targets=ar,pnpg"],
        "combos": COLUMNS,
        "source": {
            "type": "release_dispatch",
            "workflow": "release_app.yml",
            "app": "user-group-ms",
        },
    },
    {
        "label": "user-ms",
        "definition_workflow": "trigger_release_user_ms.yml",
        "variables": ["env", "branch", "app=user-ms", "targets=ar,pnpg"],
        "combos": COLUMNS,
        "source": {
            "type": "release_dispatch",
            "workflow": "release_app.yml",
            "app": "user-ms",
        },
    },
    {
        "label": "webhook",
        "definition_workflow": "trigger_release_webhook.yml",
        "variables": ["env", "branch", "app=webhook", "target=ar"],
        "combos": AR_ONLY_COLUMNS,
        "source": {
            "type": "wrapper_run",
            "workflow": "trigger_release_webhook.yml",
            "app": "webhook",
            "target": "ar",
        },
    },
]

SECTIONS = [
    ("Infrastructure", INFRA_ROWS),
    ("Applications", APP_ROWS),
]

_api_cache = {}


def gh_api(resource):
    url = resource if resource.startswith("http") else f"https://api.github.com/{resource}"
    if url in _api_cache:
        return _api_cache[url]

    req = urllib.request.Request(url)
    req.add_header("Accept", "application/vnd.github+json")
    req.add_header("X-GitHub-Api-Version", "2022-11-28")
    if TOKEN:
        req.add_header("Authorization", f"Bearer {TOKEN}")

    try:
        with urllib.request.urlopen(req, timeout=20) as response:
            payload = json.loads(response.read())
            _api_cache[url] = payload
            return payload
    except urllib.error.HTTPError as exc:
        print(f"HTTP {exc.code} for {url}", file=sys.stderr)
    except Exception as exc:
        print(f"Error {exc} for {url}", file=sys.stderr)
    return None


def workflow_runs(workflow_file):
    for page in range(1, MAX_RUN_PAGES + 1):
        data = gh_api(
            f"repos/{REPO}/actions/workflows/{workflow_file}/runs?per_page={PER_PAGE}&page={page}"
        )
        runs = data.get("workflow_runs", []) if data else []
        if not runs:
            break
        for run in runs:
            yield run
        if len(runs) < PER_PAGE:
            break


def run_or_job_status(entity):
    status = entity.get("status")
    if status and status != "completed":
        return status
    return entity.get("conclusion")


def iso_timestamp(entity):
    return entity.get("completed_at") or entity.get("updated_at") or entity.get("started_at") or ""


def format_timestamp(value):
    if not value:
        return ""
    return value.replace("T", " ").replace("Z", " UTC")


def workflow_url(workflow_file):
    return f"https://github.com/{REPO}/actions/workflows/{workflow_file}"


def run_record(run, title=None):
    return {
        "status": run_or_job_status(run),
        "url": run.get("html_url"),
        "updated_at": iso_timestamp(run),
        "title": title or run.get("display_title") or run.get("name") or "",
        "branch": run.get("head_branch") or "",
    }


def job_record(job, run, title=None):
    return {
        "status": run_or_job_status(job),
        "url": job.get("html_url") or run.get("html_url"),
        "updated_at": iso_timestamp(job) or iso_timestamp(run),
        "title": title or job.get("name") or "",
        "branch": run.get("head_branch") or "",
    }


def render_variables(variables):
    return "".join(f'<span class="var-chip">{escape(variable)}</span>' for variable in variables)


def render_status_cell(record):
    if record is None:
        label, dot_class = STATUS_META[None]
        return (
            '<td class="status-cell">'
            f'<span class="status-link" title="{escape(label)}">'
            f'<span class="status-dot {dot_class}"></span>'
            "</span></td>"
        )

    label, dot_class = STATUS_META.get(record["status"], (record["status"], "dot-neutral"))
    details = [label]
    if record.get("updated_at"):
        details.append(format_timestamp(record["updated_at"]))
    if record.get("branch"):
        details.append(f"branch={record['branch']}")
    if record.get("title"):
        details.append(record["title"])
    title = escape(" | ".join(details))
    url = escape(record.get("url") or "#")
    return (
        '<td class="status-cell">'
        f'<a class="status-link" href="{url}" title="{title}">'
        f'<span class="status-dot {dot_class}"></span>'
        "</a></td>"
    )


def render_na_cell():
    return '<td class="status-cell na-cell">N/A</td>'


def render_column_header(env, target):
    return (
        '<th class="matrix-col">'
        f'<span class="env-name">{escape(env.upper())}</span>'
        f'<span class="target-name">{escape(target)}</span>'
        "</th>"
    )


def collect_release_dispatch_records(workflow_file, wanted_keys):
    records = {}
    remaining = set(wanted_keys)
    for run in workflow_runs(workflow_file):
        match = RELEASE_TITLE_RE.match(run.get("display_title") or "")
        if not match:
            continue
        key = (match.group("app"), match.group("env"), match.group("target"))
        if key not in remaining:
            continue
        records[key] = run_record(run)
        remaining.remove(key)
        if not remaining:
            break
    return records


def infer_wrapper_env(run, app_name):
    jobs = gh_api(run.get("jobs_url") or "") or {}
    for job in jobs.get("jobs", []):
        job_name = job.get("name") or ""
        match = WRAPPER_JOB_RE.search(job_name)
        if not match:
            continue
        if match.group("app").strip().lower() != app_name.lower():
            continue
        if run_or_job_status(job) == "skipped":
            continue
        return match.group("env").lower()
    return None


def collect_wrapper_records(workflow_file, app_name, target, combos):
    records = {}
    remaining_envs = {env for env, _ in combos}
    for run in workflow_runs(workflow_file):
        env = infer_wrapper_env(run, app_name)
        if env is None or env not in remaining_envs:
            continue
        records[(env, target)] = run_record(run)
        remaining_envs.remove(env)
        if not remaining_envs:
            break
    return records


def infer_infra_combo(job_name):
    if " / " in job_name:
        return None

    match = INFRA_BRACKET_RE.match(job_name)
    if match:
        return match.group("env").lower(), match.group("target").lower()

    match = INFRA_PREFIX_RE.match(job_name)
    if match:
        return match.group("env").lower(), match.group("target").lower()

    return None


def collect_infra_records(workflow_file, combos):
    records = {}
    remaining = set(combos)
    for run in workflow_runs(workflow_file):
        jobs = gh_api(run.get("jobs_url") or "") or {}
        for job in jobs.get("jobs", []):
            combo = infer_infra_combo(job.get("name") or "")
            if combo is None or combo not in remaining:
                continue
            if run_or_job_status(job) == "skipped":
                continue
            records[combo] = job_record(job, run)
            remaining.remove(combo)
            if not remaining:
                return records
    return records


def collect_section_statuses(rows):
    release_sources = {}
    infra_sources = {}
    wrapper_sources = {}

    for row in rows:
        source = row["source"]
        if source["type"] == "release_dispatch":
            workflow_file = source["workflow"]
            release_sources.setdefault(workflow_file, set())
            for env, target in row["combos"]:
                release_sources[workflow_file].add((source["app"], env, target))
        elif source["type"] == "infra_job":
            infra_sources[source["workflow"]] = set(row["combos"])
        elif source["type"] == "wrapper_run":
            wrapper_sources[row["label"]] = row

    statuses = {}

    for workflow_file, wanted_keys in release_sources.items():
        statuses[("release_dispatch", workflow_file)] = collect_release_dispatch_records(
            workflow_file, wanted_keys
        )

    for workflow_file, combos in infra_sources.items():
        statuses[("infra_job", workflow_file)] = collect_infra_records(workflow_file, combos)

    for label, row in wrapper_sources.items():
        source = row["source"]
        statuses[("wrapper_run", label)] = collect_wrapper_records(
            source["workflow"], source["app"], source["target"], row["combos"]
        )

    return statuses


def resolve_row_status_map(row, statuses):
    source = row["source"]
    if source["type"] == "release_dispatch":
        records = statuses.get(("release_dispatch", source["workflow"]), {})
        return {
            (env, target): records.get((source["app"], env, target))
            for env, target in row["combos"]
        }

    if source["type"] == "infra_job":
        records = statuses.get(("infra_job", source["workflow"]), {})
        return {(env, target): records.get((env, target)) for env, target in row["combos"]}

    records = statuses.get(("wrapper_run", row["label"]), {})
    return {(env, target): records.get((env, target)) for env, target in row["combos"]}


def section_table(title, rows, statuses):
    html = [f"<section><h2>{escape(title)}</h2>"]
    html.append('<div class="table-wrap"><table>')
    html.append("<thead><tr>")
    html.append('<th class="action-col">Action</th>')
    html.append('<th class="variables-col">Variables</th>')
    for env, target in COLUMNS:
        html.append(render_column_header(env, target))
    html.append("</tr></thead><tbody>")

    for row in rows:
        row_status_map = resolve_row_status_map(row, statuses)
        html.append("<tr>")
        html.append(
            '<td class="action-col">'
            f'<a href="{workflow_url(row["definition_workflow"])}">{escape(row["label"])}</a>'
            "</td>"
        )
        html.append(f'<td class="variables-col">{render_variables(row["variables"])}</td>')
        available = set(row["combos"])
        for combo in COLUMNS:
            if combo not in available:
                html.append(render_na_cell())
                continue
            html.append(render_status_cell(row_status_map.get(combo)))
        html.append("</tr>")

    html.append("</tbody></table></div></section>")
    return "".join(html)


def generate():
    now = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
    all_rows = [row for _, rows in SECTIONS for row in rows]
    statuses = collect_section_statuses(all_rows)

    body = []
    for title, rows in SECTIONS:
        body.append(section_table(title, rows, statuses))

    html = f"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta http-equiv="refresh" content="300">
  <title>Selfcare - Deployment Status</title>
  <style>
    :root {{
      --bg: #f4f7fb;
      --panel: #ffffff;
      --panel-alt: #f9fbfd;
      --border: #d8e1ec;
      --text: #16202a;
      --muted: #5f6f82;
      --ok: #1f8f55;
      --fail: #cf3a35;
      --run: #cf8a07;
      --neutral: #7b8794;
      --empty: #c7d2df;
      --chip-bg: #eef4fb;
      --chip-text: #22415d;
    }}
    * {{ box-sizing: border-box; }}
    body {{
      margin: 0;
      background: linear-gradient(180deg, #f9fbfe 0%, var(--bg) 100%);
      color: var(--text);
      font-family: "Segoe UI", Helvetica, Arial, sans-serif;
    }}
    main {{
      max-width: 1500px;
      margin: 0 auto;
      padding: 32px 20px 48px;
    }}
    h1 {{
      margin: 0 0 12px;
      font-size: 2rem;
      line-height: 1.1;
    }}
    h2 {{
      margin: 0 0 14px;
      font-size: 1rem;
      letter-spacing: 0.06em;
      text-transform: uppercase;
      color: var(--muted);
    }}
    p {{ margin: 0; }}
    a {{ color: #0b5cad; text-decoration: none; }}
    a:hover {{ text-decoration: underline; }}
    .intro {{
      display: grid;
      gap: 12px;
      margin-bottom: 24px;
      padding: 22px 24px;
      border: 1px solid var(--border);
      border-radius: 18px;
      background: rgba(255, 255, 255, 0.92);
      box-shadow: 0 18px 60px rgba(19, 44, 74, 0.08);
    }}
    .updated {{ color: var(--muted); font-size: 0.95rem; }}
    .legend {{
      display: flex;
      flex-wrap: wrap;
      gap: 14px;
      margin-top: 6px;
      color: var(--muted);
      font-size: 0.92rem;
    }}
    .legend-item {{ display: inline-flex; align-items: center; gap: 8px; }}
    section {{ margin-top: 28px; }}
    .table-wrap {{
      overflow-x: auto;
      border: 1px solid var(--border);
      border-radius: 18px;
      background: var(--panel);
      box-shadow: 0 14px 44px rgba(19, 44, 74, 0.06);
    }}
    table {{
      width: 100%;
      min-width: 1120px;
      border-collapse: separate;
      border-spacing: 0;
    }}
    thead th {{
      position: sticky;
      top: 0;
      z-index: 1;
      background: var(--panel-alt);
      color: var(--muted);
      font-size: 0.8rem;
      letter-spacing: 0.04em;
      text-transform: uppercase;
    }}
    th, td {{
      padding: 14px 16px;
      border-bottom: 1px solid var(--border);
      vertical-align: middle;
    }}
    tbody tr:hover {{ background: #fbfdff; }}
    tbody tr:last-child td {{ border-bottom: none; }}
    .action-col {{ min-width: 240px; text-align: left; font-weight: 600; }}
    .variables-col {{ min-width: 280px; text-align: left; }}
    .matrix-col {{ min-width: 92px; text-align: center; }}
    .env-name, .target-name {{ display: block; }}
    .target-name {{ font-size: 0.76rem; text-transform: none; }}
    .status-cell {{ text-align: center; }}
    .na-cell {{ color: var(--empty); font-size: 0.8rem; }}
    .status-link {{
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 28px;
      height: 28px;
      border-radius: 999px;
      border: 1px solid transparent;
    }}
    .status-link:hover {{
      text-decoration: none;
      border-color: var(--border);
      background: #f4f7fb;
    }}
    .status-dot {{
      display: inline-block;
      width: 12px;
      height: 12px;
      border-radius: 999px;
      box-shadow: 0 0 0 4px rgba(22, 32, 42, 0.04);
    }}
    .dot-success {{ background: var(--ok); }}
    .dot-failure {{ background: var(--fail); }}
    .dot-running {{ background: var(--run); }}
    .dot-warning {{ background: var(--run); }}
    .dot-neutral {{ background: var(--neutral); }}
    .dot-empty {{ background: var(--empty); }}
    .var-chip {{
      display: inline-block;
      margin: 0 8px 8px 0;
      padding: 6px 10px;
      border-radius: 999px;
      background: var(--chip-bg);
      color: var(--chip-text);
      font-size: 0.83rem;
      white-space: nowrap;
    }}
    @media (max-width: 820px) {{
      main {{ padding: 24px 14px 36px; }}
      .intro {{ padding: 18px; border-radius: 14px; }}
      .legend {{ gap: 10px; }}
    }}
  </style>
</head>
<body>
  <main>
    <section class="intro">
      <div>
        <h1>Selfcare deployment status</h1>
        <p class="updated">Last updated: {escape(now)} · <a href="https://github.com/{REPO}">pagopa/selfcare</a></p>
      </div>
      <p class="updated">Matrix columns are fixed to dev-ar, dev-pnpg, uat-ar, uat-pnpg, prod-ar and prod-pnpg. Each row links to the workflow definition, while each status dot links to the latest matching run or job.</p>
      <div class="legend">
        <span class="legend-item"><span class="status-dot dot-success"></span>Success</span>
        <span class="legend-item"><span class="status-dot dot-failure"></span>Failed</span>
        <span class="legend-item"><span class="status-dot dot-running"></span>Running</span>
        <span class="legend-item"><span class="status-dot dot-neutral"></span>Cancelled or skipped</span>
        <span class="legend-item"><span class="status-dot dot-empty"></span>No runs</span>
      </div>
    </section>
    {''.join(body)}
  </main>
</body>
</html>"""

    with open("index.html", "w", encoding="utf-8") as output_file:
        output_file.write(html)

    print(f"Generated index.html at {now}")


if __name__ == "__main__":
    generate()
