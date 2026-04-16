# SelfCare Security SDK

This SDK provides custom security features for Quarkus applications, specifically enhancing JWT processing to support multiple issuers and conditional role assignment.

## Key Features
This library replaces Quarkus's default JWT validation mechanism to enforce custom business logic regarding token issuers and associated user roles:

Multi-Issuer Validation: It enforces that any incoming JSON Web Token (JWT) must be issued by one of the following recognized authorities: SPID or PAGOPA.

Custom Public Key Verification: Both JWT will use the same pre-configured shared public key for signature verification, simplifying trust management.

Conditional Role Assignment: It conditionally augments the authenticated user's Security Identity by adding the SUPPORT role if the token's issuer is PAGOPA.

## Configuration and Usage

The core functionality is implemented within the custom

```JWTCallerPrincipalFactory```, which utilizes CDI

```@Alternative``` and ```@Priority``` to override the default Quarkus behavior.

### Adding the Dependency to your POM
To use the SDK, add the following dependency to your consuming project's pom.xml file.

```
<dependency>
    <groupId>it.pagopa.selfcare</groupId>
    <artifactId>selfcare-sdk-security</artifactId>
    <version>0.0.1</version> 
</dependency>
```
### Application Properties Setup
The SDK requires a single mandatory configuration property: the public key used to verify the JWT signature.

Add the following property to your src/main/resources/application.properties file:

```mp.jwt.verify.publickey=${JWT-PUBLIC-KEY}```

### How it Works (Technical Details)
The custom logic is primarily executed within the overridden ```parse``` method of ```JWTCallerPrincipalFactory```.

- Issuer Extraction: The token's payload is manually parsed to extract the ```iss``` claim before standard validation begins.
- Issuer Check: The extracted issuer is checked against the internal set of valid issuers (```SPID```, ```PAGOPA```). If the issuer is not allowed, a ```ParseException``` is thrown, failing the authentication.
- Key Setup: The shared public key, injected during the constructor phase, is set on the ```JWTAuthContextInfo``` used for the standard validation process.
- Security Identity Augmentation: The custom logic ensures that tokens issued by ```PAGOPA``` result in a ```SecurityIdentity``` that includes the ```SUPPORT``` role, enabling access to specific privileged endpoints.
  - (Note: While the provided class is a ```JWTCallerPrincipalFactory```, the role assignment is typically handled by a subsequent ```SecurityIdentityAugmentor``` in a complete flow. However, the custom factory sets the foundation by ensuring only valid issuers pass the initial check.)
