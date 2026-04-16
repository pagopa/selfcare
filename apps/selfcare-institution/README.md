# Selfcare Institution

This repo structure and build monorepo with Apache Maven for selfcare institution domain.
Applications under apps/ depend on shared code under libs/.
test-coverage/ is used to assess the test coverage of the entire project.

```
.

├── apps
│   ├── institution-send-mail-scheduler
│   └── institution-ms
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

| Action                                                       |           in working directory           | with Maven                                                                                                 |
|:-------------------------------------------------------------|:----------------------------------------:|:-----------------------------------------------------------------------------------------------------------|
| Build the world                                              |                   `.`                    | `mvn clean package -DskipTests`                                                                            |
| Run `institution-send-mail-scheduler`                        |                   `.`                    | `java -jar apps/institution-send-mail-scheduler/target/institution-send-mail-scheduler-1.0.0-SNAPSHOT.jar` |
| Build and test the world                                     |               `.`                        | `mvn clean package`                                                                                        |
| Build the world                                              | `./apps/institution-send-mail-scheduler` | `mvn --file ../.. clean package -DskipTests`                                                               |
| Build `institution-send-mail-scheduler` and its dependencies |                   `.`                    | `mvn --projects :institution-send-mail-scheduler  --also-make clean package -DskipTests`                   |
| Build `institution-send-mail-scheduler` and its dependencies | `./apps/institution-send-mail-scheduler` | `mvn --file ../.. --projects :institution-send-mail-scheduler --also-make clean package -DskipTests`       |

