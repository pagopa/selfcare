# Selfcare User

This repo structure and build monorepo with Apache Maven for selfcare user domain. 

Applications under apps/ depend on shared code under libs/.
test-coverage/ is used to assess the test coverage of the entire project.



```
.

├── apps
│   ├── user-cdc
│   └── user-ms
│   └── user-group-ms
└── test-coverage
```

Look at single README module for more information.

## Infrastructure

The [`.container_apps/`] sub folder contains terraform files for deploying infrastructure as container apps in Azure.


## Continous integration

The [`.github/`] sub folder contains a self-contained ci-stack for building the monorepo with Github Actions.

## Usage

### Prerequisites

    Java version: 17
    Maven version: 3.9.*

### Setup GitHub Credentials for selfcare-onboarding-sdk

To use the selfcare-onboarding-sdk, you need to configure your Maven settings to include GitHub credentials. This allows Maven to authenticate and download the required dependencies.

1. Open or create the ~/.m2/settings.xml file on your local machine.
2. Add the following <server> configuration to the <servers> section:



```xml script
<servers>
    <server>
        <id>selfcare-onboarding</id>
        <username>**github_username**</username>
        <password>**ghp_token**</password>
    </server>
</servers>

```

## Running the application

```shell script
mvn clean package install
```

## Maven basic actions for monorep

Maven is really not a monorepo-*native* build tool (e.g. lacks
trustworthy incremental builds, can only build java code natively, is recursive and
struggles with partial repo checkouts) but can be made good use of with some tricks
and usage of a couple of lesser known command line switches.

| Action                                                                                       | in working directory | with Maven                                                                      |
|:---------------------------------------------------------------------------------------------|:--------------------:|:--------------------------------------------------------------------------------|
| Build the world                                                                              |         `.`          | `mvn clean package -DskipTests`                                                 |
| Run `user-ms`                                                                                |         `.`          | `java -jar apps/user-ms/target/user-ms-1.0.0-SNAPSHOT.jar`                      |
| Run `user-group-ms`                                                                          |         `.`          | `java -jar apps/user-group-ms/target/user-group-ms-1.0.0-SNAPSHOT.jar`                      |
| Build and test the world                                                                     |         `.`          | `mvn clean package`                                                             |
| Build the world                                                                              |   `./apps/user-ms`   | `mvn --file ../.. clean package -DskipTests`                                    |
| Build `user-ms` and its dependencies                                                         |         `.`          | `mvn --projects :user-ms --also-make clean package -DskipTests`                 |
| Build `user-ms` and its dependencies                                                         |   `./apps/user-ms`   | `mvn --file ../.. --projects :user-ms --also-make clean package -DskipTests`    |
| Build `user-group-ms` and its dependencies                                                   |         `.`          | `mvn --projects :user-group-ms --also-make-dependents clean package -DskipTests`                 |
| Build `user-group-ms` and its dependencies                                                   |         `.`          | `mvn --projects :user-group-ms-app --also-make clean package -DskipTests`                 |
| Build `user-sdk` and its dependents (aka. reverse dependencies or *rdeps* in Bazel parlance) |         `.`          | `mvn --projects :user-sdk-pom --also-make-dependents clean package -DskipTests` |
| Print dependencies of `user-sdk`                                                             |   `./apps/user-ms`   | `mvn dependency:list`                                                           |
| Change version  of `user-sdk`                                                                |         `.`          | `mvn versions:set -DnewVersion=0.1.2 --projects :user-sdk-pom  `                |
| Persist version  of `user-sdk`                                                               |         `.`          | `mvn versions:commit   `                                                        |
