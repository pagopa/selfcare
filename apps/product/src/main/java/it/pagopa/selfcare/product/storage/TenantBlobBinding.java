package it.pagopa.selfcare.product.storage;

import com.azure.storage.blob.BlobServiceAsyncClient;

public record TenantBlobBinding(
    BlobServiceAsyncClient client, String container, String pathPrefix) {}
