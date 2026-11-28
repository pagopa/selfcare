# Context
Questo file di specifica è relativo al contesto per l' adozione del multicontratto per prodotto. In particolare tale funzionalità è pensata per consentire ad un ente che si registra su un prodotto con institutionType = "X" (dove X è un tipo generico conenstito dal sistema) può registrarsi sullo stesso prodotto (con il medesimo productId) con un nuovo institutionType in aggiunta al precedente. 

# Scope
Tale documento deve essere utilizzato esclusivamente per analizzare gli impatti sulle seguenti entità di dominio e relativi flussi:
- Institution
- User
- UserGroup
- Delegation
- Token Exchange
- External APIs

# Requirements
- L' unico campo che deve essere propagato ed integrato nella chiave naturale delle entità impattate deve essere l 'intitutionType
- gli indici sul DB non costituiscono vincolo di univocità, se lo sono controlla sul codice terraform che effettivamente siano unique
- Gli impatti su document-ms sono fuori scope
- E' stato già deciso che il pregresso verrà sanato con un update massivo dei dati presenti sul database con la propagazione dell' institutionType corretto
- Per le API che consentono ancora la retrocompatibilità e che quindi non necessitano obbligatoriamente di un rilascio di una nuova versione, adottano la convenzione per la quale il risultato restituito (in assenza di institutionType) deve essere relativo all' ultimo onboarding
- Sull' entità Delegation i campi fromType e toType sono già valorizzati correttamente rispetto all' onboarding per il quale sono create, pertanto le logiche di deduplica sulla componente delegation-cdc sono già implementate

Per ulteriori dettagli si può fare riferimento all Desing Review in draft pubblicata all' indirizzo: https://pagopa.atlassian.net/wiki/spaces/SCP/pages/3495526860/DR+-+Gestione+multi+contratto+per+singolo+prodotto


