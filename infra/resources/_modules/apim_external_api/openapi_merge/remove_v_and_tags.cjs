const fs = require('fs');
const path = require('path');

// Recupera i percorsi dei file dal terminale
const inputFilePath = process.argv[2];
const outputFilePath = process.argv[3];

// Verifica che i percorsi siano stati forniti
if (!inputFilePath || !outputFilePath) {
  console.error('Usage: node script.js <inputFilePath> <outputFilePath>');
  process.exit(1);
}

// Specifica il percorso del file openapi.json
const inputFilePathComplete = path.join(inputFilePath);
const outputFilePathComplete = path.join(outputFilePath);


// Lista dei tag da mantenere (puoi modificare questi tag o passare dinamicamente come argomento del comando)
const tagsToRemove = ["support", "external-v2", "contract", "internal-v1","support-pnpg", "external-pnpg"];

// Leggi il file openapi.json
fs.readFile(inputFilePathComplete, 'utf8', (err, data) => {
    if (err) {
        console.error(`Errore durante la lettura del file: ${err}`);
        return;
    }

    let openapi;
    try {
        openapi = JSON.parse(data);
    } catch (err) {
        console.error(`Errore durante il parsing del JSON: ${err}`);
        return;
    }

    // Filtra i tag nel file openapi
    if (openapi.tags && Array.isArray(openapi.tags)) {
        openapi.tags = openapi.tags.filter(tag => !tagsToRemove.includes(tag.name));
    }

    // Rimuovi v* dal path
    if (openapi.paths && typeof openapi.paths === 'object') {
        Object.keys(openapi.paths).forEach(pathKey => {
            if(pathKey.startsWith('/v')) {
                const pathKeyWithoutVersion = pathKey.substring(3); // Remove '/vX' (es. '/v1' -> '/endpoint')
                const pathItem = openapi.paths[pathKey];
                
                delete openapi.paths[pathKey];
                if (openapi.paths[pathKeyWithoutVersion]) {
                    console.warn(`Il path ${pathKeyWithoutVersion} esiste già. Saltando la sovrascrittura.`);
                } else {
                    openapi.paths[pathKeyWithoutVersion] = pathItem;
                }
            }
        });
    }

    // Edit securitySchemes: replace bearerAuth or others with apiKeyHeader
    if (openapi.components && openapi.components.securitySchemes) {
        if (openapi.components.securitySchemes.bearerAuth) {
            // Rimuove bearerAuth
            delete openapi.components.securitySchemes.bearerAuth;

            // Added apiKeyHeader
            openapi.components.securitySchemes.apiKeyHeader = {
                "type": "apiKey",
                "name": "Ocp-Apim-Subscription-Key",
                "in": "header"
            };
        } else {
            console.warn('bearerAuth non trovato nei securitySchemes.');
        }
    } else {
        console.warn('components.securitySchemes non trovato nel file OpenAPI.');
    }

    // Aggiorna le security requirements nelle operazioni: sostituisce bearerAuth con apiKeyHeader
    if (openapi.paths && typeof openapi.paths === 'object') {
        Object.keys(openapi.paths).forEach(path => {
            const pathItem = openapi.paths[path];
            ['get', 'post', 'put', 'delete', 'options', 'head', 'patch', 'trace'].forEach(method => {
                if (pathItem[method] && pathItem[method].security) {
                    pathItem[method].security = pathItem[method].security.map(securityReq => {
                        const newSecurityReq = {};
                        Object.keys(securityReq).forEach(scheme => {
                            newSecurityReq['apiKeyHeader'] = ["global"];
                        });
                        return newSecurityReq;
                    });
                }
            });
        });
    }

    // Filtra i tag nei paths
    if (openapi.paths && typeof openapi.paths === 'object') {
        Object.keys(openapi.paths).forEach(path => {
            const pathItem = openapi.paths[path];
            ['get', 'post', 'put', 'delete', 'options', 'head', 'patch', 'trace'].forEach(method => {
                if (pathItem[method] && Array.isArray(pathItem[method].tags)) {
                    pathItem[method].tags = pathItem[method].tags.filter(tag => !tagsToRemove.includes(tag));
                }
            });
        });
    }

    // Salva il file modificato
    fs.writeFile(outputFilePath, JSON.stringify(openapi, null, 2), 'utf8', err => {
        if (err) {
            console.error(`Errore durante il salvataggio del file: ${err}`);
            return;
        }

        console.log(`File salvato con successo in ${outputFilePathComplete}`);
    });

     // Elimina il file
     fs.unlink(inputFilePathComplete, err => {
        if (err) {
            console.error(`Errore durante l'eliminazione del file: ${err}`);
            return;
        }

        console.log(`File eliminato con successo: ${inputFilePathComplete}`);
    });
});
