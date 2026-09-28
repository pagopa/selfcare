package it.pagopa.selfcare.product;

import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.product.service.ProductService;

public record TenantProductCdcResources(
    String tenantId,
    ReactiveMongoClient mongoClient,
    String database,
    ProductService productService,
    AzureBlobClient azureBlobClient,
    String productsFilePath) {}
