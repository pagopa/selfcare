sleep 5
mkdir -p /workspace
# Aggiungo products.json
curl -H "Authorization: token $GITHUB_TOKEN" -o ./workspace/products.json https://raw.githubusercontent.com/pagopa/selfcare-infra-private/refs/heads/main/products/env/dev/products.json
ls ./workspace/products.json
az storage container create --name products --account-name devstoreaccount1 --account-key Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg== --connection-string 'DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg==;BlobEndpoint=http://azurite:10000/devstoreaccount1;'
az storage blob upload --container-name products --file ./workspace/products.json --name products.json --account-name devstoreaccount1 --account-key Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg== --connection-string 'DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg==;BlobEndpoint=http://azurite:10000/devstoreaccount1;'
az storage blob list --container-name products --account-name devstoreaccount1 --account-key Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg== --connection-string 'DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg==;BlobEndpoint=http://azurite:10000/devstoreaccount1;'
# Aggiungo i template
az storage container create --name resources --account-name devstoreaccount1 --account-key Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg== --connection-string 'DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg==;BlobEndpoint=http://azurite:10000/devstoreaccount1;'
az storage blob upload-batch --destination resources --source /workspace/resources --account-name devstoreaccount1 --account-key Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg== --connection-string "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg==;BlobEndpoint=http://azurite:10000/devstoreaccount1;"
az storage blob list --container-name resources --account-name devstoreaccount1 --account-key Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg== --connection-string 'DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg==;BlobEndpoint=http://azurite:10000/devstoreaccount1;'

exit 0