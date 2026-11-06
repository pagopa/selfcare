# selfcare-onboarding-sdk-crypto

> Digital signature utilities for onboarding contracts: PAdES signing of PDFs with a pluggable PKCS#7 hash-sign source (local key, Aruba ARSS SOAP, Namirial HTTP), plus helpers for data encryption.

| | |
|---|---|
| Path | `libs/selfcare-onboarding-sdk-crypto` |
| Artifact | `it.pagopa.selfcare:selfcare-onboarding-sdk-crypto:0.18.3` (version from [`selfcare-onboarding-sdk-pom`](selfcare-onboarding-sdk-pom.md)) |
| Stack | Plain Java 17, PDFBox, BouncyCastle (`bcprov`, `bcpkix`), JAX-WS/JAXB/SAAJ (Aruba SOAP), Google HTTP client (Namirial) |
| Used by | `document-ms`, `iam`, `onboarding-functions`, `onboarding-ms`, `product`, `product-cdc`, `registry-proxy` (root and `web`), `webhook` |

## What it provides

Package `it.pagopa.selfcare.onboarding.crypto`:

| Class | Purpose |
|---|---|
| `PadesSignService` / `PadesSignServiceImpl` | `padesSign(pdfFile, signedPdfFile, signatureInformation)` using a `Pkcs7HashSignService`. |
| `Pkcs7HashSignService` | PDFBox `SignatureInterface` + `returnsFullPdf()`. |
| `Pkcs7HashSignServiceImpl` | Local source: key and certificate from `CRYPTO_PRIVATE_KEY`, `CRYPTO_CERT`. |
| `ArubaPkcs7HashSignServiceImpl`, `ArubaSignService(Impl)` | Aruba Remote Sign Service (SOAP; WSDL/XSD in `src/main/resources/docs/aruba/`). |
| `NamirialPkcs7HashSignServiceImpl`, `NamirialSignService`, `NamiralSignServiceImpl`, `client.NamirialHttpClient` | Namirial remote signature over HTTP. |
| `config.*` | `ArubaSignConfig`/`ArubaInitializer`, `LocalCryptoConfig`/`LocalCryptoInitializer`. |
| `utils.CryptoUtils`, `utils.DataEncryptionUtils`, `utils.SoapLoggingHandler` | Certificate/key parsing, symmetric encrypt/decrypt helpers, SOAP logging. |

## Usage

```xml
<dependency>
  <groupId>it.pagopa.selfcare</groupId>
  <artifactId>selfcare-onboarding-sdk-crypto</artifactId>
  <version>0.18.3</version>
</dependency>
```

```java
Pkcs7HashSignService hashSign = new ArubaPkcs7HashSignServiceImpl(new ArubaSignServiceImpl());
PadesSignService pades = new PadesSignServiceImpl(hashSign);
```

Environment variables read by the default constructors (names only):

| Source | Variables |
|---|---|
| Local | `CRYPTO_PRIVATE_KEY`, `CRYPTO_CERT` |
| Aruba | `ARUBA_SIGN_SERVICE_BASE_URL`, `ARUBA_SIGN_SERVICE_CONNECT_TIMEOUT_MS`, `ARUBA_SIGN_SERVICE_REQUEST_TIMEOUT_MS`, `ARUBA_SIGN_SERVICE_IDENTITY_TYPE_OTP_AUTH`, `ARUBA_SIGN_SERVICE_IDENTITY_OTP_PWD`, `ARUBA_SIGN_SERVICE_IDENTITY_USER`, `ARUBA_SIGN_SERVICE_IDENTITY_DELEGATED_USER`, `ARUBA_SIGN_SERVICE_IDENTITY_DELEGATED_PASSWORD`, `ARUBA_SIGN_SERVICE_IDENTITY_DELEGATED_DOMAIN` |
| Namirial | `NAMIRIAL_BASE_URL`, `NAMIRIAL_SIGN_SERVICE_IDENTITY_USER`, `NAMIRIAL_SIGN_SERVICE_IDENTITY_PASSWORD` |

Tenant-aware apps can instead take signature settings per tenant from the `signature` section of
`tenant.registry.json` (see [selfcare-sdk-tenant](selfcare-sdk-tenant.md)); `NamiralSignServiceImpl` also has explicit-credential constructors.

## Build and release

```bash
mvn -f libs/selfcare-onboarding-sdk-pom/pom.xml clean package
```

Released with the Onboarding SDK by `.github/workflows/release_onboarding_sdk.yml` (manual `workflow_dispatch`).

## References

- [`libs/selfcare-onboarding-sdk-crypto/README.md`](../../libs/selfcare-onboarding-sdk-crypto/README.md)
- Spring counterpart: `selc-commons-crypto` / `selc-commons-connector-soap-aruba-sign` in [selfcare-commons](selfcare-commons.md)
