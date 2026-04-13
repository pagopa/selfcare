#!/usr/bin/env python3
"""
Generates a GitHub Pages status page for Selfcare workflows.
Queries the GitHub API to find the last run per workflow + env + target combination.
"""

import json
import os
import sys
import urllib.request
import urllib.error
from datetime import datetime, timezone

REPO = "pagopa/selfcare"
TOKEN = os.environ.get("GITHUB_TOKEN", "")

ENVS = ["dev", "uat", "prod"]
DOMAINS = ["ar", "pnpg"]

# ──────────────────────────────────────────────────────────────────────────────
# Workflow definitions
# Each entry: (label, workflow_file, [(env, target_or_None)])
# ──────────────────────────────────────────────────────────────────────────────

INFRA_CORE = [
    ("Core Infra",          "publish_infra.yml",                          [(e, d) for e in ENVS for d in DOMAINS]),
    ("Identity Infra",      "publish_identity_infra.yml",                 [(e, "ar") for e in ENVS]),
    ("Registry Proxy Infra","publish_registry_proxy_infra.yml",           [(e, d) for e in ENVS for d in DOMAINS]),
    ("Hub Spid PNPG Infra", "publish_onboarding_hub_spid_pnpg_infra.yml", [(e, "pnpg") for e in ENVS]),
    ("Namirial Sign Infra",  "publish_onboarding_namirial_sign_infra.yml", [(e, "ar") for e in ENVS]),
    ("Dashboard Events Infra","publish_onboarding_dashboard_events_infra.yml", [(e, "ar") for e in ENVS]),
]

APPS = [
    ("auth",                "trigger_release_auth.yml",       [(e, "ar")   for e in ENVS]),
    ("iam",                 "trigger_release_iam.yml",         [(e, "ar")   for e in ENVS]),
    ("product",             "trigger_release_product.yml",     [(e, "ar")   for e in ENVS]),
    ("product-cdc",         "trigger_release_product_cdc.yml", [(e, "ar")   for e in ENVS]),
    ("onboarding-ms",       "trigger_release_onboarding.yml",  [(e, d) for e in ENVS for d in DOMAINS]),
    ("onboarding-bff",      "trigger_release_onboarding_bff.yml", [(e, "ar") for e in ENVS]),
    ("onboarding-functions","release_functions.yml",           [(e, d) for e in ENVS for d in DOMAINS]),
    ("document-ms",         "trigger_release_document_ms.yml", [(e, "ar")   for e in ENVS]),
    ("registry-proxy",      "trigger_release_registry_proxy.yml", [(e, d) for e in ENVS for d in DOMAINS]),
    ("webhook",             "trigger_release_webhook.yml",     [(e, "ar")   for e in ENVS]),
]

INFRA_RESOURCES = [
    ("auth",                 "pr_auth_infra.yml",              [(e, "ar")   for e in ENVS]),
    ("iam",                  "pr_iam_infra.yml",               [(e, "ar")   for e in ENVS]),
    ("product",              "pr_product_infra.yml",           [(e, "ar")   for e in ENVS]),
    ("product-cdc",          "pr_product_cdc_infra.yml",       [(e, "ar")   for e in ENVS]),
    ("onboarding-ms",        "pr_onboarding_infra_ms.yml",     [(e, "ar")   for e in ENVS]),
    ("onboarding-bff",       "pr_onboarding_infra_bff.yml",    [(e, "ar")   for e in ENVS]),
    ("onboarding-cdc",       "pr_onboarding_infra_cdc.yml",    [(e, "ar")   for e in ENVS]),
    ("onboarding-functions", "pr_onboarding_infra_functions.yml", [(e, "ar") for e in ENVS]),
    ("document-ms",          "pr_document_ms_infra.yml",       [(e, "ar")   for e in ENVS]),
    ("registry-proxy",       "pr_registry_proxy_ms.yml",       [(e, "ar")   for e in ENVS]),
]


def gh_api(path):
    url = f"https://api.github.com/{path}"
    req = urllib.request.Request(url)
    req.add_header("Accept", "application/vnd.github+json")
    req.add_header("X-GitHub-Api-Version", "2022-11-28")
    if TOKEN:
        req.add_header("Authorization", f"Bearer {TOKEN}")
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            return json.loads(resp.read())
    except urllib.error.HTTPError as e:
        print(f"  HTTP {e.code} for {url}", file=sys.stderr)
        return None
    except Exception as e:
        print(f"  Error {e} for {url}", file=sys.stderr)
        return None


# Cache: workflow_file -> list of runs (up to 100)
_runs_cache = {}

def get_runs(workflow_file):
    if workflow_file in _runs_cache:
        return _runs_cache[workflow_file]
    data = gh_api(f"repos/{REPO}/actions/workflows/{workflow_file}/runs?per_page=100")
    runs = data.get("workflow_runs", []) if data else []
    _runs_cache[workflow_file] = runs
    return runs


def find_last_run(workflow_file, env, target):
    """Return the most recent run matching env+target inputs, or None."""
    runs = get_runs(workflow_file)
    for run in runs:
        inputs = (run.get("inputs") or {})
        run_env = inputs.get("env") or inputs.get("environment") or inputs.get("domain")
        run_target = inputs.get("target") or inputs.get("domain")

        env_match = (run_env == env)
        # For infra core: domain input maps to target
        if not env_match:
            run_env2 = inputs.get("environment")
            env_match = (run_env2 == env)

        if not env_match:
            continue

        if target is None:
            return run

        if run_target == target:
            return run

    return None


STATUS_ICON = {
    "success":     ("✅", "success",     "#2da44e"),
    "failure":     ("❌", "failure",     "#cf222e"),
    "in_progress": ("🔄", "running",     "#bf8700"),
    "cancelled":   ("⚪", "cancelled",   "#57606a"),
    "skipped":     ("⏭️", "skipped",    "#57606a"),
    None:          ("➖", "no runs",     "#57606a"),
}

def run_status(run):
    if run is None:
        return None
    if run.get("status") == "in_progress":
        return "in_progress"
    return run.get("conclusion")  # success, failure, cancelled, skipped


def cell(run, workflow_file):
    status = run_status(run)
    icon, label, color = STATUS_ICON.get(status, STATUS_ICON[None])
    if run:
        url = run["html_url"]
        ts  = run.get("updated_at", "")[:16].replace("T", " ")
        return f'<td style="text-align:center"><a href="{url}" title="{label} · {ts}" style="color:{color};text-decoration:none;font-size:1.2em">{icon}</a></td>'
    wf_url = f"https://github.com/{REPO}/actions/workflows/{workflow_file}"
    return f'<td style="text-align:center"><span title="{label}" style="color:{color};font-size:1.2em">{icon}</span></td>'


def col_header(env, domain):
    label = f"{env.upper()}<br><small>{domain}</small>"
    return f'<th style="text-align:center;padding:6px 12px">{label}</th>'


def section_table(title, rows, columns):
    """
    rows: list of (label, workflow_file, [(env, target)])
    columns: list of (env, target) defining column order
    """
    html = f'<h2 style="margin-top:2rem">{title}</h2>\n'
    html += '<table style="border-collapse:collapse;width:100%">\n<thead><tr>'
    html += '<th style="text-align:left;padding:6px 12px">Pipeline</th>'
    for env, domain in columns:
        html += col_header(env, domain)
    html += '</tr></thead>\n<tbody>\n'

    for label, workflow_file, combos in rows:
        wf_url = f"https://github.com/{REPO}/actions/workflows/{workflow_file}"
        html += f'<tr><td style="padding:6px 12px"><a href="{wf_url}">{label}</a></td>'
        for env, domain in columns:
            # Check if this combo is covered by this workflow
            if (env, domain) in combos or (env, domain.replace("pnpg","pnpg")) in combos:
                print(f"  Fetching {workflow_file} env={env} target={domain}", file=sys.stderr)
                run = find_last_run(workflow_file, env, domain if domain != "ar" else None)
                # Try with explicit "ar" if None didn't match
                if run is None and domain == "ar":
                    run = find_last_run(workflow_file, env, "ar")
                html += cell(run, workflow_file)
            else:
                html += '<td style="text-align:center;color:#ccc">—</td>'
        html += '</tr>\n'

    html += '</tbody>\n</table>\n'
    return html


def generate():
    now = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")

    # Column definitions
    core_cols   = [(e, d) for e in ENVS for d in DOMAINS]
    app_cols    = [(e, d) for e in ENVS for d in DOMAINS]
    res_cols    = [(e, "ar") for e in ENVS]

    body = ""
    body += section_table("Infrastructure — Core", INFRA_CORE, core_cols)
    body += section_table("Apps — Release", APPS, app_cols)
    body += section_table("Infrastructure — Resources (PR)", INFRA_RESOURCES, res_cols)

    html = f"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta http-equiv="refresh" content="300">
  <title>Selfcare — Status</title>
  <style>
    body {{ font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
           margin: 2rem auto; max-width: 1200px; padding: 0 1rem; color: #1f2328; }}
    h1   {{ border-bottom: 1px solid #d0d7de; padding-bottom: .5rem; }}
    h2   {{ color: #57606a; font-size: 1rem; font-weight: 600; text-transform: uppercase;
           letter-spacing: .05em; }}
    table {{ border: 1px solid #d0d7de; border-radius: 6px; margin-bottom: 2rem; }}
    thead {{ background: #f6f8fa; }}
    tbody tr:hover {{ background: #f6f8fa; }}
    td, th {{ border-bottom: 1px solid #d0d7de; }}
    tbody tr:last-child td {{ border-bottom: none; }}
    .updated {{ color: #57606a; font-size: .85rem; }}
  </style>
</head>
<body>
  <h1>Selfcare — Deployment Status</h1>
  <p class="updated">Last updated: {now} · <a href="https://github.com/{REPO}">pagopa/selfcare</a></p>
  {body}
</body>
</html>"""

    with open("index.html", "w") as f:
        f.write(html)

    print(f"Generated index.html at {now}")


if __name__ == "__main__":
    generate()
