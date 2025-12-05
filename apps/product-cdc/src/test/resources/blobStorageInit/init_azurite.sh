sleep 10

mkdir -p /workspace

az storage container create --name products --account-name devstoreaccount1 --account-key Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg== --connection-string "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg==;BlobEndpoint=http://azurite:10000/devstoreaccount1;"
az storage blob upload --container-name products --file /workspace/products.json --name products.json --account-name devstoreaccount1 --account-key Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg== --connection-string "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg==;BlobEndpoint=http://azurite:10000/devstoreaccount1;" --overwrite
az storage blob list --container-name products --account-name devstoreaccount1 --account-key Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg== --connection-string "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg==;BlobEndpoint=http://azurite:10000/devstoreaccount1;"

az storage container create --name resources --account-name devstoreaccount1 --account-key Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg== --connection-string "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg==;BlobEndpoint=http://azurite:10000/devstoreaccount1;"
az storage blob upload-batch --destination resources --source /workspace/resources --account-name devstoreaccount1 --account-key Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg== --connection-string "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg==;BlobEndpoint=http://azurite:10000/devstoreaccount1;"
az storage blob list --container-name resources --account-name devstoreaccount1 --account-key Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg== --connection-string "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg==;BlobEndpoint=http://azurite:10000/devstoreaccount1;"

az storage container create --name contracts-blob --account-name devstoreaccount1 --account-key Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg== --connection-string "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg==;BlobEndpoint=http://azurite:10000/devstoreaccount1;"
az storage blob upload-batch --destination contracts-blob/contracts/template --source /workspace/contracts-blob --account-name devstoreaccount1 --account-key Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg== --connection-string "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg==;BlobEndpoint=http://azurite:10000/devstoreaccount1;"
az storage blob list --container-name contracts-blob --account-name devstoreaccount1 --account-key Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg== --connection-string "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg==;BlobEndpoint=http://azurite:10000/devstoreaccount1;"

az storage blob upload --container-name contracts-blob --file /workspace/dummy-accordo_di_adesione.html --name contracts/dummy-accordo_di_adesione.html --account-name devstoreaccount1 --account-key "Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg==" --connection-string "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCD9I1QhZT4gRjAAHEDPazjFIwtg==;BlobEndpoint=http://azurite:10000/devstoreaccount1;"

echo "BLOBSTORAGE INITIALIZED"

exit 0