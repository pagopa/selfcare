# selfcare-onboarding-sdk-azure-storage

> Small Azure Blob Storage client (`AzureBlobClient`) to read, upload, list and delete files in a container, with connection-string or Managed Identity authentication.

| | |
|---|---|
| Path | `libs/selfcare-onboarding-sdk-azure-storage` |
| Artifact | `it.pagopa.selfcare:selfcare-onboarding-sdk-azure-storage:0.18.3` (version from [`selfcare-onboarding-sdk-pom`](selfcare-onboarding-sdk-pom.md)) |
| Stack | Plain Java 17, `azure-storage-blob` 12.24.0, `azure-identity` 1.18.1, `commons-lang3`, OWASP encoder |
| Used by | `institution-send-mail-scheduler`, `onboarding-functions`, `product-cdc`, `user-cdc`; lib [`selfcare-onboarding-sdk-product`](selfcare-onboarding-sdk-product.md) |

## What it provides

Package `it.pagopa.selfcare.azurestorage`:

| Class | Purpose |
|---|---|
| `AzureBlobClient` | Interface: `retrieveFile`, `getFile`, `getFileAsText`, `getFileAsPdf`, `uploadFile`, `uploadFilePath`, `removeFile`, `getProperties`, `getFiles()` / `getFiles(path)`. |
| `AzureBlobClientDefault` | Implementation. Constructors: `(connectionString, containerName)` and `(containerName, accountName, managedIdentityClientId)` (account + optional user-assigned Managed Identity). |
| `error.SelfcareAzureStorageException`, `error.SelfcareAzureStorageNotFoundException`, `error.SelfcareAzureStorageError` | Error types and codes. |

## Usage

```xml
<dependency>
  <groupId>it.pagopa.selfcare</groupId>
  <artifactId>selfcare-onboarding-sdk-azure-storage</artifactId>
  <version>0.18.3</version>
</dependency>
```

```java
AzureBlobClient client = new AzureBlobClientDefault(connectionString, containerName);
String json = client.getFileAsText("products.json");
```

The library reads no configuration itself: the consuming app passes connection string / account / container
from its own properties.

## Build and release

```bash
mvn -f libs/selfcare-onboarding-sdk-pom/pom.xml clean package
```

Released with the Onboarding SDK by `.github/workflows/release_onboarding_sdk.yml` (manual `workflow_dispatch`).

## References

- [`libs/selfcare-onboarding-sdk-azure-storage/README.md`](../../libs/selfcare-onboarding-sdk-azure-storage/README.md)
- [selfcare-onboarding-sdk-product](selfcare-onboarding-sdk-product.md)
- Tenant-aware alternative for Spring apps: `TenantBlobClientProvider` in [selfcare-commons](selfcare-commons.md)
