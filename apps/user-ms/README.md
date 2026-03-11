# Microservice User

Repository that contains backend services synch for selfcare user.

It implements CRUD operations for the 'user' object and the business logic for the user.

## Configuration Properties

Before running you must set these properties as environment variables.


| **Property**                                           | **Environment Variable**                 | **Default** | **Required** |
|--------------------------------------------------------|------------------------------------------|-------------|:------------:|
| quarkus.mongodb.connection-string<br/>                 | MONGODB-CONNECTION-STRING                |             |     yes      |
| mp.jwt.verify.publickey<br/>                           | JWT-PUBLIC-KEY                           |             |     yes      |


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

## Environment Variables

| **Environment Variable**                | **Default** | **Required** |
|-----------------------------------------|-------------|:------------:|
| JWT-PUBLIC-KEY                          |             |     yes      |
| EVENT_HUB_BASE_PATH                     |             |     yes      |
| SHARED_ACCESS_KEY_NAME                  |             |     yes      |
| EVENTHUB-SC-USERS-SELFCARE-WO-KEY-LC    |             |     yes      |
| USER_MS_EVENTHUB_USERS_ENABLED          |             |     yes      |
| MONGODB-CONNECTION-STRING               |             |     yes      |
| STORAGE_CONTAINER_PRODUCT               |             |     yes      |
| BLOB-STORAGE-PRODUCT-CONNECTION-STRING  |             |     yes      |
| STORAGE_CONTAINER_TEMPLATES             |             |     yes      |
| SUSER-REGISTRY-API-KEY                  |             |     yes      |
| USER_REGISTRY_URL                       |             |     yes      |
| MAIL_SUBJECT_PREFIX                     |             |     yes      |
| ENV_TARGET                              |             |     yes      |
| NO_REPLY_MAIL                           |             |     yes      |
| AWS_SES_ACCESS_KEY_ID                   |             |     yes      |
| AWS_SES_SECRET_ACCESS_KEY               |             |     yes      |
| AWS_SES_REGION                          |             |     yes      |
| NO_REPLY_MAIL                           |             |     yes      |
| USER_MS_RETRY_MIN_BACKOFF               | 5           |     yes      |
| USER_MS_RETRY_MAX_BACKOFF               | 60          |     yes      |
| USER_MS_RETRY                           | 3           |     yes      |

## EventHub messages

Messages via EventHub are sent in the following cases:
- Creation of a new user or assignment of a new role to an already registered user
- Modification of data for an already registered user (first name, last name, email)
- Update of the status of a product associated with the user

To perform local tests of functionalities that involve sending messages to the SC-User topic, the following environment variables must be set with the values corresponding to the environment to which you want to send the messages:
- EVENT_HUB_BASE_PATH
- SHARED_ACCESS_KEY_NAME
- EVENTHUB-SC-USERS-SELFCARE-WO-KEY-LC

## Cucumber Tests (Integration Tests)

To run the Cucumber tests locally, execute it.pagopa.selfcare.user.integration_test.CucumberSuite

To run a single test or a specific feature file, open the file and press the play button for the corresponding test (or the file). The first execution will fail; you will then need to modify the configuration by setting the main class to:
it.pagopa.selfcare.user.integration_test.CucumberSuite.
