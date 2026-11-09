# selfcare-onboarding-sdk-product

> Model and service to load the SelfCare product catalogue (`products.json`) and query/validate products, roles and contract templates.

| | |
|---|---|
| Path | `libs/selfcare-onboarding-sdk-product` |
| Artifact | `it.pagopa.selfcare:selfcare-onboarding-sdk-product:0.18.3` (version from [`selfcare-onboarding-sdk-pom`](selfcare-onboarding-sdk-pom.md)) |
| Stack | Plain Java 17, Jackson (`jdk8`, `jsr310`), `commons-lang3`; depends on [`selfcare-onboarding-sdk-common`](selfcare-onboarding-sdk-common.md) and [`selfcare-onboarding-sdk-azure-storage`](selfcare-onboarding-sdk-azure-storage.md) |
| Used by | `dashboard-bff`, `document-ms`, `institution-ms` (`connector-api`, `core`), `institution-send-mail-scheduler`, `onboarding-cdc`, `product`, `product-cdc`, `user-cdc`, `user-ms` |

## What it provides

| Package | Content |
|---|---|
| `it.pagopa.selfcare.product.entity` | `Product`, `ProductRole`, `ProductRoleInfo`, `ProductStatus`, `ContractTemplate`, `AttachmentTemplate`, `EmailTemplate`, `BackOfficeConfigurations`, `SigningConfiguration`, `SigningStep`, `ManagingInstitution`, `OriginEntry`, `StorageOrigin`, `PHASE_ADDITION_ALLOWED` |
| `it.pagopa.selfcare.product.service` | `ProductService` (`getProduct`, `getProductRaw`, `getProductIsValid`, `validateProductRole`, `validateRoleMappings`, `verifyAllowedByInstitutionTaxCode`, `getProductExpirationDate`, `isProductEnabled`); `ProductServiceDefault` (built from a JSON string); `ProductServiceCacheable` (reads the file through `AzureBlobClient` and reloads it when the blob `LastModified` changes) |
| `it.pagopa.selfcare.product.utils` | `ProductUtils` (`validRoles`, `validRolesByProductRole`, `getProductRole`) |
| `it.pagopa.selfcare.product.exception` | `ProductNotFoundException`, `InvalidRoleMappingException` |

Avro schemas of the product document: `src/main/schema/Product_v1.avsc`, `Product_v2.avsc`, `Product_v3.avsc`.

## Usage

```xml
<dependency>
  <groupId>it.pagopa.selfcare</groupId>
  <artifactId>selfcare-onboarding-sdk-product</artifactId>
  <version>0.18.3</version>
</dependency>
```

```java
AzureBlobClient blob = new AzureBlobClientDefault(connectionString, container);
ProductService productService = new ProductServiceCacheable(blob, "products.json");
Product product = productService.getProductIsValid("prod-pagopa");
```

The consuming app decides where `products.json` lives (storage account, container, path) through its own properties.

## Build and release

```bash
mvn -f libs/selfcare-onboarding-sdk-pom/pom.xml clean package
```

Released with the Onboarding SDK by `.github/workflows/release_onboarding_sdk.yml` (manual `workflow_dispatch`).

## References

- [`libs/selfcare-onboarding-sdk-product/README.md`](../../libs/selfcare-onboarding-sdk-product/README.md)
- [product app](../apps/product.md), [product-cdc app](../apps/product-cdc.md)
