# Microservice Onboarding

Repository that contains backend services synch for selfcare onboarding.

It implements CRUD operations for the 'onboarding' object and the business logic for the onboarding phase. During the onboarding process, the following activities are executed:

1. Check for the presence of users associated with the onboarding and potentially add them to the point of sale (pdv).
2. Validate the requested product's suitability and verify eligible roles.
3. Verify if there is an existing onboarding record for that institution and product.

After the data is saved, it invokes the function implemented by onboarding-functions to trigger asynchronous onboarding activities. 

### Disable starting async onboarding workflow

````properties
env ONBOARDING_ORCHESTRATION_ENABLED=false.
````
### Allowed onboarding product with status TESTING

ONBOARDING_ALLOWED_INSTITUTIONS_PRODUCTS property permits onboarding for product with status TESTING. It can contain a map of entry with product as key and a list of taxcode as value.

````properties
ONBOARDING_ALLOWED_INSTITUTIONS_PRODUCTS={'prod-interop': ['0123456789']}.
````

## Configuration Properties

Before running you must set these properties as environment variables.


| **Property**                                           | **Environment Variable**                 | **Default** | **Required** |
|--------------------------------------------------------|------------------------------------------|-------------|:------------:|
| quarkus.mongodb.connection-string<br/>                 | MONGODB-CONNECTION-STRING                |             |     yes      |
| mp.jwt.verify.publickey<br/>                           | JWT-PUBLIC-KEY                           |             |     yes      |
| quarkus.rest-client."**.UserApi".api-key<br/>          | USER-REGISTRY-API-KEY                    |             |     yes      |
| quarkus.rest-client."**.UserApi".url<br/>              | USER_REGISTRY_URL                        |             |     yes      |
| quarkus.rest-client."**.CoreApi".url<br/>              | MS_CORE_URL                              |             |     yes      |
| quarkus.rest-client."**.AooApi".url<br/>               | MS_PARTY_REGISTRY_URL                    |             |     yes      |
| quarkus.rest-client."**.UoApi".url<br/>                | MS_PARTY_REGISTRY_URL                    |             |     yes      |
| quarkus.rest-client."**.OrchestrationApi".url<br/>     | ONBOARDING_FUNCTIONS_URL                 |             |     yes      |
| quarkus.rest-client."**.OrchestrationApi".api-key<br/> | ONBOARDING-FUNCTIONS-API-KEY             |             |     yes      |
| quarkus.rest-client."**.InstitutionApi".url<br/>       | MS_USER_URL                              |             |     yes      |
| onboarding.institutions-allowed-list<br/>              | ONBOARDING_ALLOWED_INSTITUTIONS_PRODUCTS |             |      no      |

> **_NOTE:_**  properties that contains secret must have the same name of its secret as uppercase.


## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

For some endpoints 

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8083/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Related Guides


### RESTEasy Reactive

Easily start your Reactive RESTful Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)

### OpenAPI Generator

Rest client are generated using a quarkus' extension.

[Related guide section...](hhttps://github.com/quarkiverse/quarkus-openapi-generator)
