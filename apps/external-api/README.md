# selfcare-external-api-backend

Selfcare External API service, integrating with product-ms through REST.

## Product service configuration

Set `MS_PRODUCT_URL` to the base URL of product-ms (default: `http://localhost:8080`). The generated Feign client calls the product endpoints with the optional `tenantId` query parameter omitted.
