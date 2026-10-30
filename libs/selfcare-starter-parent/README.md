# Selfcare Starter Parent

Parent Maven Spring Boot condiviso: `it.pagopa.selfcare:selc-starter-parent`.
Il codice proviene da [pagopa/selfcare-starter-parent](https://github.com/pagopa/selfcare-starter-parent),
branch `main`, commit `101a09f1d284adda68855b3288aa169e3045cb6a`.
L'importazione con Git subtree senza squash conserva tutti i 24 commit originali.
La PR di migrazione deve essere integrata con **merge commit**, non squash o rebase,
per mantenere lo storico originale nel branch `main`.
Al momento dell'importazione `pagopa/selfcare` consente solo squash merge:
prima di integrare la PR un amministratore deve abilitare il merge commit
e verificare eventuali regole di storia lineare sul branch. Non usare squash
come alternativa, perche' eliminerebbe i commit importati dallo storico di `main`.

## Build e rilascio

La versione `0.3.3` mantiene Spring Boot, dipendenze e plugin del parent originale.
Le versioni precedenti restano sul feed Azure DevOps; non vengono ripubblicate.

Comando dalla root del monorepo (Java 17, Maven 3.9.x):

```sh
mvn -B -ntp -f libs/selfcare-starter-parent/pom.xml clean verify
```

Il modulo contiene solo un POM, quindi non ha test Java.
Il workflow `.github/workflows/pr_starter_parent.yml` esegue questa verifica.
Il workflow `.github/workflows/release_starter_parent.yml` pubblica su
`https://maven.pkg.github.com/pagopa/selfcare` quando il POM cambia su `main`,
oppure tramite `workflow_dispatch` su `main`. Usa `GITHUB_TOKEN` con permesso
`packages: write` e server Maven `selfcare`; non richiede credenziali Azure.
Le pipeline Azure DevOps sono sostituite dai workflow e restano nello storico Git.

Ogni nuovo rilascio richiede una nuova versione nel POM: una release gia'
pubblicata non va sovrascritta. Le modifiche al solo README non rilasciano
l'artefatto. La prima pubblicazione richiede il merge della migrazione su `main`.

## Consumer da migrare

Inventario dei riferimenti diretti al parent nel monorepo al momento
dell'importazione; i loro POM non sono modificati da questa migrazione.

| Microservizio | POM | Versione attuale |
|---|---|---|
| dashboard-bff | `apps/dashboard-bff/pom.xml` | `0.3.2-SNAPSHOT` |
| external-api | `apps/external-api/pom.xml` | `0.3.2-SNAPSHOT` |
| institution-ms | `apps/institution-ms/pom.xml` | `0.3.2-SNAPSHOT` |
| onboarding-bff | `apps/onboarding-bff/pom.xml` | `0.3.1-SNAPSHOT` |
| registry-proxy | `apps/registry-proxy/pom.xml` | `0.3.1-SNAPSHOT` |
| user-group-ms | `apps/user-group-ms/pom.xml` | `0.3.1-SNAPSHOT` |

Anche la libreria `libs/selfcare-commons/pom.xml` e i suoi moduli ereditano il
parent `0.3.2-SNAPSHOT` e vanno migrati.

Dopo la pubblicazione su GitHub Packages:

1. Aggiornare la versione del parent a `0.3.3`, mantenendo le coordinate
   `it.pagopa.selfcare:selc-starter-parent`.
2. Per i servizi del monorepo usare
   `<relativePath>../../libs/selfcare-starter-parent/pom.xml</relativePath>`;
   per `selfcare-commons` usare
   `<relativePath>../selfcare-starter-parent/pom.xml</relativePath>`.
   I consumer esterni possono usare `<relativePath/>` e il repository Maven
   `selfcare` con URL `https://maven.pkg.github.com/pagopa/selfcare`.
3. Configurare il server Maven `selfcare` con credenziali di lettura dei package
   (in GitHub Actions, `GITHUB_TOKEN` con `packages: read` e accesso al package).
   Non rimuovere Azure DevOps finche' altre dipendenze ne hanno bisogno.
4. Nei microservizi impostare `<sonar.skip>false</sonar.skip>`: il parent
   imposta `true` per essere escluso, come gli altri moduli `libs`, dall'analisi
   Sonar del monorepo; le applicazioni devono continuare a essere analizzate.
5. Eseguire build, test unitari e suite Cucumber del consumer. Per chi parte da
   `0.3.1-SNAPSHOT`, verificare anche gli aggiornamenti di Spring Cloud e
   Springdoc presenti nella linea `0.3.2-SNAPSHOT` importata.