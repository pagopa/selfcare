# it.pagopa.selfcare.utils:selc-commons-crypto

This module contains utilities to perform cryptographic operation, such digital signatures.

See [Confluence page](https://pagopa.atlassian.net/wiki/spaces/SCP/pages/616857618/Firma+digitale+per+mezzo+dei+servizi+di+Aruba)
for integration and documentation details

## Configuration

The following environment variable allows to select the source to use in order to build pkcs7 hash signature used when
building PAdES and CAdES signatures.

| ENV                  | Description                                                                     | Default |
|----------------------|---------------------------------------------------------------------------------|---------|
| CRYPTO_PKCS7_SOURCE  | The source to use in order to build CAdES and PAdES signatures.                 | local   |
| CRYPTO_PRIVATE_KEY   | The private key (PEM) used when the pkcs7 hash signature source is <i>local</i> |         |
| CRYPTO_CERT          | The certificate (PEM) used when the pkcs7 hash signature source is <i>local</i> |         |

### Alternative pkcs7 hash signature sources

Through the environment variable <b><i>CRYPTO_PKCS7_SOURCE</i></b> it is possible to configure an alternative pkcs7 hash
signature source.

The sources available inside this repository are:

* local
    * It will use the provided private key and certificate
* aruba
    * It will use Aruba services
    * It will require the following
      dependency [it.pagopa.selfcare.soap:selc-commons-connector-soap-aruba-sign](../../connector/soap/aruba-sign/README.md)

## Services

This module register the following Spring's bean

| Service                                                              | Description                                                                |
|----------------------------------------------------------------------|----------------------------------------------------------------------------|
| it.pagopa.selfcare.commons.utils.crypto.service.CadesSignService     | It allows to build CAdES digital signature on a single file                |
| it.pagopa.selfcare.commons.utils.crypto.service.PadesSignService     | It allows to build PAdES digital signature on a single pdf                 |
| it.pagopa.selfcare.commons.utils.crypto.service.Pkcs7HashSignService | Conditionally registered when the source for pkcs7 hash signature is local |