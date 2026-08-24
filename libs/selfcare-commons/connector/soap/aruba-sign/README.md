# it.pagopa.selfcare.soap:selc-commons-connector-soap-aruba-sign

This module integrates the ARSS (Aruba Remote Sign Service) Soap service in order to build the automatic digital
signature of the hash of a single file through the certificates stored inside Aruba.

Moreover, it extends the
module [<b><i>it.pagopa.selfcare.utils:selc-commons-crypto</i></b>](../../../utils/crypto/README.md) in order to use
such service to
build PAdES and CAdES signatures.

See [Confluence page](https://pagopa.atlassian.net/wiki/spaces/SCP/pages/616857618/Firma+digitale+per+mezzo+dei+servizi+di+Aruba)
for integration and documentation details

## Configuration

### Aruba integration

The integration towards Aruba is configurable through the following environment variables:

| ENV                                            | Description                                                                                                                                                                                                   | Default                                                                     |
|------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------|
| ARUBA_SIGN_SERVICE_BASE_URL                    | The URL of the webService                                                                                                                                                                                     | https://arss.demo.firma-automatica.it:443/ArubaSignService/ArubaSignService |
| ARUBA_SIGN_SERVICE_CONNECT_TIMEOUT_MS          | The timeout configured to establish the connection. If 0, no timeout will be configured                                                                                                                       | 0                                                                           |
| ARUBA_SIGN_SERVICE_REQUEST_TIMEOUT_MS          | The timeout configured for the request. If 0, no timeout will be configured                                                                                                                                   | 0                                                                           |
| ARUBA_SIGN_SERVICE_IDENTITY_TYPE_OTP_AUTH      | The string identifying the automatic signature domain indicated when ARSS is installed                                                                                                                        | typeOtpAuth                                                                 |
| ARUBA_SIGN_SERVICE_IDENTITY_OTP_PWD            | The string identifying the automatic signature transactions defined when the ARSS server is installed (it is normally known by the administrator of the IT infrastructure network on which users are working) | otpPwd                                                                      |
| ARUBA_SIGN_SERVICE_IDENTITY_USER               | The string containing the signature user's username                                                                                                                                                           | user                                                                        |
| ARUBA_SIGN_SERVICE_IDENTITY_DELEGATED_USER     | The string containing the username for the delegated user                                                                                                                                                     | delegatedUser                                                               |
| ARUBA_SIGN_SERVICE_IDENTITY_DELEGATED_PASSWORD | The String containing the delegated user's password                                                                                                                                                           | delegatedPassword                                                           |
| ARUBA_SIGN_SERVICE_IDENTITY_DELEGATED_DOMAIN   | The delegated user's domain                                                                                                                                                                                   | delegatedDomain                                                             |

### PAdES and CAdES

The following environment variable allows to enable/disable Aruba services when building PAdES and CAdES signatures

| ENV                 | Description                                                    |Default|
|---------------------|----------------------------------------------------------------|-------|
| CRYPTO_PKCS7_SOURCE | The source to use in order to build CAdES and PAdES signatures | aruba |

## Services

This module register the following Spring's bean

| Service                                                                                    | Description                                                                                                           |
|--------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|
| it.pagopa.selfcare.commons.connector.soap.aruba.sign.service.ArubaSignService              | It allows to perform signature requests towards Aruba.                                                                |
| it.pagopa.selfcare.commons.connector.soap.aruba.sign.service.ArubaPkcs7HashSignServiceImpl | Conditionally registered when the source for PAdES and CAdES signatures is Aruba in order to enable Aruba integration |
