# SELC-9328 — Validazione JWT sulle onboarding functions HTTP

## Obiettivo

Rendere ogni Azure Function di `onboarding-functions` attivata tramite HTTP coerente con le API BFF/MS: il chiamante deve propagare il token di sessione e la function deve validare, prima di qualunque effetto applicativo:

- la presenza e il valore supportato dell'header `X-Tenant-Id`;
- la presenza di `Authorization: Bearer <jwt>`;
- algoritmo e firma RS256 del JWT tramite la chiave pubblica del tenant dichiarato nell'header;
- scadenza, issuer e claim obbligatori del JWT;
- coincidenza tra il claim `tenant_id` e `X-Tenant-Id`.

La function key Azure rimane obbligatoria e costituisce un controllo aggiuntivo, non alternativo al JWT.

## Perimetro HTTP

La protezione viene applicata a tutti i trigger HTTP presenti nelle classi:

- `ExternalFunctions`: `TestToken`, `CheckOrganization`, `messageAcknowledgment`, `webhookTest`;
- `InstitutionFunctions`: `TriggerDeleteInstitutionAndUser`;
- `NotificationFunctions`: `Notification`, `ResendNotification`, `CountNotifications`, `ResendNotifications`;
- `OnboardingFunctions`: `StartOnboardingOrchestration`, `TriggerBuildAttachmentsAndSaveTokens`, `TestSendEmail`.

I trigger timer, queue, blob, event hub, orchestration e activity non sono interessati perché non ricevono una richiesta HTTP.

## Decisioni di sicurezza

1. `X-Tenant-Id` è obbligatorio, case-insensitive come nome e normalizzato in maiuscolo come valore.
2. I tenant ammessi sono configurati per ambiente tramite `SUPPORTED_TENANTS`; ogni stack AR/PNPG espone il solo tenant pertinente.
3. La chiave pubblica è distinta per tenant (`JWT_PUBLIC_KEY_AR`, `JWT_PUBLIC_KEY_PNPG`) e proviene da Key Vault.
4. Sono accettati solo token RS256 con `exp`, `iss`, `uid` e `tenant_id`; gli issuer ammessi sono configurabili e, per default, `SPID,PAGOPA`.
5. Chiave mancante o illeggibile, token assente/malformato/scaduto, algoritmo errato, firma non valida, claim assente o tenant discordante producono un rifiuto fail-closed.
6. Header tenant assente/non supportato produce `400 Bad Request`; autenticazione non valida produce `401 Unauthorized` e `WWW-Authenticate: Bearer`.
7. La validazione avviene all'inizio del trigger, prima di parsing del body, logging con dati applicativi, scheduling di orchestrazioni o chiamate a servizi.
8. Non viene introdotto alcun flag di bypass in produzione.

## Propagazione end-to-end

| Chiamante | Flusso | Intervento/verifica |
|---|---|---|
| `onboarding-bff` | sessione utente → `CheckOrganization` | verificare con test di contratto che gli interceptor propaghino insieme `Authorization` e `X-Tenant-Id`, oltre alla function key |
| `onboarding-ms` | sessione utente → orchestration HTTP | propagare il JWT autenticato e il tenant validato; mantenere il token tecnico configurato come fallback per invocazioni prive di contesto HTTP |
| `onboarding-cdc` | evento → orchestration HTTP | propagare il token tecnico e `X-Tenant-Id`, controllando la coerenza del tenant del token |
| APIM external API | APIM → resend/count/acknowledgment | verificare che la policy base generi il JWT RS256 con `tenant_id` e imposti `X-Tenant-Id`, mentre la policy operation aggiunge la function key |
| `onboarding-functions` | function → BFF/MS | confermare che i token tecnici già generati includano `tenant_id` coerente con il contesto |

## Modifiche previste

1. Introdurre un validatore HTTP centralizzato in `onboarding-functions`, con configurazione tenant → chiave pubblica e issuer ammessi.
2. Invocare il validatore in tutti i dodici trigger HTTP e uniformare le risposte di errore.
3. Aggiornare la propagazione nei client REST di `onboarding-ms` e `onboarding-cdc`.
4. Aggiornare i contratti OpenAPI delle functions e dei client dichiarando bearer JWT, function key e `X-Tenant-Id` dove applicabile.
5. Aggiungere le impostazioni Terraform per tenant supportato e chiave pubblica nei sei stack DEV/UAT/PROD AR/PNPG.
6. Aggiornare README e documentazione tecnica dei moduli coinvolti.

## Strategia di test

- Unit test del validatore: token valido AR/PNPG; header case-insensitive; header/token assenti; schema non Bearer; token malformato; algoritmo diverso da RS256; firma errata; token scaduto; issuer non ammesso; claim `exp`, `uid` o `tenant_id` assente; tenant discordante; chiave assente/non valida.
- Test di enforcement su tutti i trigger HTTP: una richiesta rifiutata non deve produrre side effect.
- Test dei client `onboarding-ms`, `onboarding-cdc` e `onboarding-bff`: propagazione congiunta di function key, bearer token e tenant.
- Validazione sintattica dei documenti OpenAPI e formattazione/validazione Terraform.
- Suite Maven dei moduli interessati, eseguite prima in modo mirato e poi complete.

## Rollout e prerequisiti operativi

Prima del deploy delle functions devono essere disponibili in ogni ambiente:

- il secret Key Vault `jwt-public-key` coerente con la chiave usata dai token del relativo tenant;
- token tecnici di `onboarding-ms`, `onboarding-cdc` e APIM firmati con la chiave corrispondente e contenenti `uid`, `exp`, `iss` e `tenant_id`;
- `SUPPORTED_TENANTS` valorizzato con il tenant dello stack.

Ordine raccomandato: predisposizione/verifica secret e token, deploy dei chiamanti che propagano gli header, deploy delle functions con validazione obbligatoria, smoke test per ciascun percorso e tenant.

## Criteri di accettazione

- Nessun trigger HTTP esegue logica applicativa senza function key, tenant valido e JWT valido.
- Un JWT valido per un tenant non può essere usato dichiarando un tenant diverso nell'header.
- Tutti i chiamanti noti propagano i campi richiesti oppure usano un token tecnico tenant-bound.
- Contratti, configurazione infrastrutturale, test e documentazione descrivono lo stesso comportamento.
- Le suite interessate e i controlli statici risultano verdi.
