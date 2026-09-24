# Onboarding BFF – Knowledge base operativa

## 2026-09-24 – Integration test: storage tenant-aware

### Contesto

La suite Cucumber di `apps/onboarding-bff/app` avvia `onboarding-ms` come immagine Docker e inizializza Azurite con il container `products`.

### Causa

L'immagine ha il tenant registry di produzione come default; richiede uno storage `products` per AR e PNPG, con nomi e container non presenti nel fixture. L'assenza della configurazione faceva fallire `onboarding-ms` e il BFF restituiva `502` agli scenari.

### Correzione

`test-onboarding-ms.env` imposta un tenant registry test con entrambi i tenant, connessioni Mongo dedicate e connection string Azurite dedicate a `products`.

### Verifica

Eseguire da `apps/onboarding-bff/app`:

```bash
mvn test -Dtest=it.pagopa.selfcare.onboarding.CucumberSuite -Dsurefire.failIfNoSpecifiedTests=false
```

### Prevenzione

Quando si avvia un'immagine di microservizio in test, sovrascrivere tutte le variabili richieste dal tenant registry e mantenerle coerenti con i fixture Docker.
