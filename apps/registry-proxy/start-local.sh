#!/bin/bash

# Colori per output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

# Verifica che Dapr sia installato
if ! command -v dapr &> /dev/null; then
    echo "Dapr CLI non trovato. Installalo prima di continuare."
    exit 1
fi

# Verifica che Java e Maven siano installati
if ! command -v java &> /dev/null || ! command -v mvn &> /dev/null; then
    echo "Java o Maven non trovati. Assicurati che siano installati."
    exit 1
fi

log "Avvio dell'applicazione con Dapr locale..."

# Avvia l'applicazione Spring Boot con profilo locale
log "Build dell'applicazione..."
./mvnw clean compile

log "Avvio dell'applicazione Spring Boot con Dapr sidecar..."

# Avvia con Dapr
dapr run \
    --app-id party-reg-proxy \
    --app-port 8080 \
    --app-protocol http \
    --dapr-http-port 3500 \
    --dapr-grpc-port 50001 \
    --resources-path ./.dapr/components \
    --config ./.dapr/config/config.yaml \
    --log-level debug
#    -- ./mvn spring-boot:run -Dspring-boot.run.profiles=local