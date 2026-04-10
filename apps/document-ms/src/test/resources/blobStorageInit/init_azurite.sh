#!/bin/sh
set -e

CONN="DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg==;BlobEndpoint=http://azurite:10000/devstoreaccount1;"

sleep 5

# Containers
az storage container create --name sc-d-documents-blob --connection-string "$CONN" >/dev/null
az storage container create --name products --connection-string "$CONN" >/dev/null

# Contract templates (html/pdf) used by scenarios
az storage blob upload --container-name sc-d-documents-blob --file /workspace/contracts/contract-template.html --name "contract-templates/prod-test/5ca7f491-5770-40ba-baf0-6fe612bba14e.html" --overwrite true --connection-string "$CONN" >/dev/null
az storage blob upload --container-name sc-d-documents-blob --file /workspace/contracts/contract-template.pdf --name "contract-templates/prod-test/76f9cfd4-e9ae-4eeb-8ed3-1285cc5968d6.pdf" --overwrite true --connection-string "$CONN" >/dev/null

# Product definition used by document-ms
az storage blob upload --container-name products --file /workspace/products/product.json --name products.json --overwrite true --connection-string "$CONN" >/dev/null

echo "BLOBSTORAGE INITIALIZED."
