# Onboarding SDK Crypto

This module contains utilities to perform cryptographic operation, such digital signatures. See [Confluence page](https://pagopa.atlassian.net/wiki/spaces/SCP/pages/616857618/Firma+digitale+per+mezzo+dei+servizi+di+Aruba)
for integration and documentation details


### Hash signature sources

It is possible to configure different hash signature sources.

The sources available inside this repository are:

* Pkcs7HashSignService
* ArubaPkcs7HashSignService


### Pkcs7HashSignService

It will use the provided private key and certificate, you must set these env variables.

| Properties         | Description                                                                     | Default |
|--------------------|---------------------------------------------------------------------------------|---------|
| CRYPTO_PRIVATE_KEY | The private key (PEM) used when the pkcs7 hash signature source is <i>local</i> |         |
| CRYPTO_CERT        | The certificate (PEM) used when the pkcs7 hash signature source is <i>local</i> |         |



### ArubaPkcs7HashSignService: Aruba integration

It integrates the ARSS (Aruba Remote Sign Service) Soap service in order to build the automatic digital
signature of the hash of a single file through the certificates stored inside Aruba.

See [Confluence page](https://pagopa.atlassian.net/wiki/spaces/SCP/pages/616857618/Firma+digitale+per+mezzo+dei+servizi+di+Aruba)
for integration and documentation details


The integration towards Aruba is configurable through the following environment variables:

| ENV                                            | Description                                                                                                                                                                                                     | Default                                                                     |
|------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------|
| ARUBA_SIGN_SERVICE_BASE_URL                    | The URL of the webService                                                                                                                                                                                     | https://arss.demo.firma-automatica.it:443/ArubaSignService/ArubaSignService |
| ARUBA_SIGN_SERVICE_CONNECT_TIMEOUT_MS          | The timeout configured to establish the connection. If 0, no timeout will be configured                                                                                                                       | 0                                                                           |
| ARUBA_SIGN_SERVICE_REQUEST_TIMEOUT_MS          | The timeout configured for the request. If 0, no timeout will be configured                                                                                                                                   | 0                                                                           |
| ARUBA_SIGN_SERVICE_IDENTITY_TYPE_OTP_AUTH      | The string identifying the automatic signature domain indicated when ARSS is installed                                                                                                                        | typeOtpAuth                                                                 |
| ARUBA_SIGN_SERVICE_IDENTITY_OTP_PWD            | The string identifying the automatic signature transactions defined when the ARSS server is installed (it is normally known by the administrator of the IT infrastructure network on which users are working) | otpPwd                                                                      |
| ARUBA_SIGN_SERVICE_IDENTITY_USER               | The string containing the signature user's username                                                                                                                                                           | user                                                                        |
| ARUBA_SIGN_SERVICE_IDENTITY_DELEGATED_USER     | The string containing the username for the delegated user                                                                                                                                                     | delegatedUser                                                               |
| ARUBA_SIGN_SERVICE_IDENTITY_DELEGATED_PASSWORD | The String containing the delegated user's password                                                                                                                                                           | delegatedPassword                                                           |
| ARUBA_SIGN_SERVICE_IDENTITY_DELEGATED_DOMAIN   | The delegated user's domain                                                                                                                                                                                   | delegatedDomain                                                             |


## Installation

To use this library in your projects, you can add the dependency to your pom.xml if you're using Maven:

```shell script
<dependency>
    <groupId>it.pagopa.selfcare</groupId>
    <artifactId>onboarding-sdk-crypto</artifactId>
    <version>0.1.0</version>
</dependency>
```
If you are using Gradle, you can add the dependency to your build.gradle file:

```shell script
dependencies {
    implementation 'it.pagopa.selfcare:onboarding-sdk-crypto:0.1.0'
}
```

## Usage

You can inject the service in the context of Quarkus or Spring (replace @ApplicationScoped with @Bean).

### Pkcs7HashSignService

```java script
    @ApplicationScoped
    public Pkcs7HashSignService pkcs7HashSignService(){
       return new Pkcs7HashSignServiceImpl();
    }
    
    @ApplicationScoped
    public PadesSignService padesSignService(Pkcs7HashSignService pkcs7HashSignService){
        return new PadesSignServiceImpl(pkcs7HashSignService);
    }
 ```

### ArubaPkcs7HashSignService

```java script
    @ApplicationScoped
    public Pkcs7HashSignService pkcs7HashSignService(){
       return new ArubaPkcs7HashSignServiceImpl(new ArubaSignServiceImpl());
    }
    
    @ApplicationScoped
    public PadesSignService padesSignService(Pkcs7HashSignService pkcs7HashSignService){
        return new PadesSignServiceImpl(pkcs7HashSignService);
    }
 ```