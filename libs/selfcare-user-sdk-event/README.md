# Onboarding SDK Azure Storage

This library has been developed to provide a set of Java utility classes to simplify the work of interact with azure eventhub. It allows to send messages to different topics in the eventhub using SAS token exchange.

## Installation

To use this library in your projects, you can add the dependency to your pom.xml if you're using Maven:

```shell script
<dependency>
  <groupId>it.pagopa.selfcare</groupId>
  <artifactId>user-sdk-event</artifactId>
  <version>0.0.1</version>
</dependency>
```

If you are using Gradle, you can add the dependency to your build.gradle file:

```shell script
dependencies {
    implementation 'it.pagopa.selfcare:user-sdk-event:0.0.1'
}
```

## Usage

To use the EventHubRestClient you need to create a Config Class in your project to instantiate a bean for initializing the SaSTokenAuth class with the required properties:

```java script

public class UserMsConfig {
    @ConfigProperty(name = "eventhub.rest-client.keyName")
    String eventHubKeyName;
    @ConfigProperty(name = "eventhub.rest-client.key")
    String eventHubKey;
    @ConfigProperty(name = "rest-client.event-hub.uri")
    URI eventHubBaseUri;
    
    @ApplicationScoped
    public EventhubSasTokenAuthorization eventhubSasTokenAuthorization(){
        return new EventhubSasTokenAuthorization(eventHubBaseUri, eventHubKeyName, eventHubKey);

    }
}
```
In your application.properties file you will need to add 

```properties
quarkus.rest-client."it.pagopa.selfcare.user.client.eventhub.EventHubRestClient".url=${EVENT_HUB_BASE_PATH:test}
rest-client.event-hub.uri=${EVENT_HUB_BASE_PATH:test}
eventhub.rest-client.keyName=${SHARED_ACCESS_KEY_NAME:test}
eventhub.rest-client.key=${EVENTHUB-SC-USERS-SELFCARE-WO-KEY-LC:test}

```
Note: Every tokenAuthorization bean is specific to a eventhub topic, so if the application need to send messages to different topics,
you will need to instantiate as many SasTokenAuth beans with the uri/keyName/key triple specific to the topic