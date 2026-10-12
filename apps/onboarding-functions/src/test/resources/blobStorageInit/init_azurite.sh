set -e

sleep 10

mkdir -p /workspace

PRODUCT_STORAGE_CONNECTION_STRING="DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg==;BlobEndpoint=http://azurite:10000/devstoreaccount1;"
test -s /workspace/products.json
az storage container create --name products --connection-string "$PRODUCT_STORAGE_CONNECTION_STRING"
az storage blob upload --container-name products --file /workspace/products.json --name products.json --connection-string "$PRODUCT_STORAGE_CONNECTION_STRING" --overwrite
az storage blob exists --container-name products --name products.json --connection-string "$PRODUCT_STORAGE_CONNECTION_STRING" --query exists --output tsv | grep -qx true

az storage container create --name resources --account-name devstoreaccount1 --account-key Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg== --connection-string "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg==;BlobEndpoint=http://azurite:10000/devstoreaccount1;"
az storage blob upload-batch --destination resources --source /workspace/resources --account-name devstoreaccount1 --account-key Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg== --connection-string "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg==;BlobEndpoint=http://azurite:10000/devstoreaccount1;" --overwrite
az storage blob list --container-name resources --account-name devstoreaccount1 --account-key Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg== --connection-string "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg==;BlobEndpoint=http://azurite:10000/devstoreaccount1;"

az storage container create --name contracts-blob --account-name devstoreaccount1 --account-key Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg== --connection-string "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg==;BlobEndpoint=http://azurite:10000/devstoreaccount1;"
az storage blob upload-batch --destination contracts-blob/contracts/template --source /workspace/contracts-blob --account-name devstoreaccount1 --account-key Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg== --connection-string "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg==;BlobEndpoint=http://azurite:10000/devstoreaccount1;" --overwrite
az storage blob list --container-name contracts-blob --account-name devstoreaccount1 --account-key Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg== --connection-string "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg==;BlobEndpoint=http://azurite:10000/devstoreaccount1;"

az storage blob upload --container-name contracts-blob --file /workspace/dummy-accordo_di_adesione.html --name contracts/dummy-accordo_di_adesione.html --account-name devstoreaccount1 --account-key "Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg==" --connection-string "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg==;BlobEndpoint=http://azurite:10000/devstoreaccount1;" --overwrite
az storage blob upload --container-name contracts-blob --file /workspace/contracts-blob/io/2.4.6/io-accordo_di_adesione-v.2.4.6.html --name contract-templates/prod-io/c26ed929-7fea-4abc-9212-142d7faf01ac.html --account-name devstoreaccount1 --account-key "Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg==" --connection-string "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg==;BlobEndpoint=http://azurite:10000/devstoreaccount1;" --overwrite

echo "BLOBSTORAGE INITIALIZED"

exit 0
