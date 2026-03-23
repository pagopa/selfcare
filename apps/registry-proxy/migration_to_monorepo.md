# Migrazione `selfcare-ms-party-registry-proxy` → `selfcare/apps/registry-proxy`

## Repositories

- **Monorepo destinazione**: https://github.com/pagopa/selfcare
- **Repo sorgente**: https://github.com/pagopa/selfcare-ms-party-registry-proxy
- **Cartella destinazione**: `apps/registry-proxy`

---

## Step 1 — Clona il monorepo e aggiungi il remote

```bash
git clone https://github.com/pagopa/selfcare.git
cd selfcare

# Aggiungi il repo sorgente come remote temporaneo
git remote add registry-proxy https://github.com/pagopa/selfcare-ms-party-registry-proxy.git

# Scarica i dati
git fetch registry-proxy
```

---

## Step 2 — Importa con git subtree

```bash
git subtree add --prefix=apps/registry-proxy registry-proxy/main
```

> ⚠️ Se il branch principale del repo sorgente si chiama `master` anziché `main`, sostituiscilo.
> Puoi verificarlo su [github.com/pagopa/selfcare-ms-party-registry-proxy](https://github.com/pagopa/selfcare-ms-party-registry-proxy).

---

## Step 3 — Pulizia e push

```bash
# Rimuovi il remote temporaneo
git remote remove registry-proxy
```

---

## Step 4 — OPEX

Spostare i file opex

```bash
mv /apps/registry-proxy/.opex/* /.opex/*
```

---

## Step 4 — GHA

Aggiornare le pipeline
* .github/workflow/create_release_branch.yml
* .github/workflow/opex_api.yml
* .github/workflow/release_app.yml

Verificare se presente la pipeline
* .github/workflow/pr_ms.yml

```bash
mv /apps/registry-proxy/.opex/* /.opex/*
```

---

## Step X — Pulizia e push

```bash
# Push
git push origin main
```

---

## Verifica

```bash
# Controlla i file
ls apps/registry-proxy/

# Vedi lo storico della sottocartella
git log --oneline apps/registry-proxy/
```
