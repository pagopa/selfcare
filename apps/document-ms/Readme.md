# Document MS

## Overview

`document-ms` manages the document lifecycle for SelfCare onboardings. It persists document metadata, creates contracts and attachments, uploads and downloads files from the configured storage, verifies signatures and exposes the complete document snapshot associated with an onboarding.

The generated OpenAPI specifications are the source of truth for request and response schemas:

- [`src/main/docs/openapi.json`](src/main/docs/openapi.json)
- [`src/main/docs/openapi.yaml`](src/main/docs/openapi.yaml)

## Main concepts

- `INSTITUTION`: main contract of an institution onboarding.
- `USER`: document produced by a user onboarding, such as an administrative appointment.
- `ATTACHMENT`: technical or business evidence associated with the institution onboarding.
- `rootOnboardingId`: identifier used to group all documents belonging to the same document dossier.
- `storageOrigin`: identifies the physical storage used internally (`SYSTEM` or `USER`).

## APIs

### Document metadata

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/v1/documents` | Saves the metadata of a contract or attachment. |
| `GET` | `/v1/documents/{id}` | Retrieves a document by its unique document ID. |
| `GET` | `/v1/documents/onboarding/{onboardingId}` | Retrieves the latest main document for an onboarding. |
| `POST` | `/v1/documents/import` | Persists metadata for an imported document. |
| `PUT` | `/v1/documents/contract-files` | Updates the signed contract path and filename. |
| `PUT` | `/v1/documents/{onboardingId}/contract-signed` | Updates the signed contract path for an onboarding. |
| `PUT` | `/v1/documents/{onboardingId}/updated-at` | Updates the document modification timestamp. |
| `GET` | `/v1/documents/{onboardingId}/attachment-list` | Returns the attachment names associated with an onboarding. |
| `HEAD` | `/v1/documents/{onboardingId}/attachment/status` | Checks whether a specific attachment is available. |
| `GET` | `/v1/documents/{onboardingId}/available-documents` | Returns attachment names and the signed contract filename available for download. |
| `GET` | `/v1/documents/{onboardingId}/related-documents` | Returns the complete snapshot of related `ATTACHMENT` and `USER` documents. |
| `GET` | `/v1/documents/contract-report` | Checks whether the signed contract is a CAdES document. |

### Document content

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/v1/document-content/contract` | Generates a contract PDF. |
| `POST` | `/v1/document-content/attachment` | Generates an attachment PDF. |
| `POST` | `/v1/document-content/{onboardingId}/upload-signed-contract` | Verifies and uploads a signed contract. |
| `POST` | `/v1/document-content/upload-attachment` | Verifies, signs and uploads a system-managed attachment. |
| `POST` | `/v1/document-content/upload-user-attachment` | Uploads a PDF attachment to user storage. |
| `GET` | `/v1/document-content/{onboardingId}/contract` | Downloads the unsigned contract. |
| `GET` | `/v1/document-content/{onboardingId}/contract-signed` | Downloads the signed contract. |
| `GET` | `/v1/document-content/{onboardingId}/attachment` | Downloads an attachment by filename. |
| `GET` | `/v1/document-content/{onboardingId}/template-attachment` | Downloads an attachment template. |
| `DELETE` | `/v1/document-content/contract` | Deletes a contract from storage. |
| `POST` | `/v1/document-content/visura` | Stores a Visura document. |
| `POST` | `/v1/document-content/aggregates-csv` | Uploads an aggregates CSV file. |
| `GET` | `/v1/document-content/aggregates-csv/{onboardingId}/products/{productId}` | Downloads the aggregates CSV for an onboarding and product. |

### Signature verification

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/v1/signature/verify` | Verifies the digital signature of a contract. |

## Related documents snapshot

`GET /v1/documents/{onboardingId}/related-documents` reads all `ATTACHMENT` and `USER` records linked through `rootOnboardingId`, ordered by creation time. The main `INSTITUTION` contract remains represented by the existing root fields and is not duplicated in this collection.

The response follows the flat SC-Contracts data contract:

```json
[
  {
    "id": "document-uuid",
    "name": "Visura_Camerale",
    "fileName": "visura.pdf",
    "type": "attachment",
    "mimeType": "application/pdf",
    "createdAt": "2026-07-15T10:00:00",
    "filePath": "parties/docs/onboarding-id/attachments/visura.pdf"
  }
]
```

Paths are resolved from `attachmentPath` for user-uploaded attachments and from `contractSigned`, with a generated-path fallback, for system-managed documents. Storage details remain internal and are not exposed in the SC-Contracts related-document representation.

## Compatibility

The related-documents endpoint is additive and does not alter existing APIs. Queue enrichment is controlled by `STANDARD_NOTIFICATION_RELATED_DOCUMENTS_ENABLED` in `onboarding-functions`; when disabled, the historical SC-Contracts payload remains unchanged.
