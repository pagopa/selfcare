# selfcare-user-sdk-event

> Quarkus REST clients and models to publish user and user-group notifications to Azure Event Hubs (main SelfCare hub and the FD hub) over the Event Hubs REST API.

| | |
|---|---|
| Path | `libs/selfcare-user-sdk-event` |
| Artifact | `it.pagopa.selfcare:selfcare-user-sdk-event:1.1.1` (parent [`selfcare-user-sdk-pom:1.1.1`](selfcare-user-sdk-pom.md)) |
| Stack | Quarkus REST Client (`quarkus-resteasy-reactive` 3.5.2, MicroProfile Rest Client), Mutiny, `azure-identity`, Lombok, MapStruct; depends on [`selfcare-user-sdk-model`](selfcare-user-sdk-model.md) |
| Used by | `user-cdc`, `user-group-cdc`, `user-ms` |

## What it provides

| Type | Purpose |
|---|---|
| `client.EventHubRestClient` | `@RegisterRestClient(configKey = "event-hub")`, `POST messages`: `sendMessage(UserNotificationToSend)`, `sendUserGroupMessage(UserGroupNotificationToSend)`, both `Uni<Void>`. Authorized by `EventhubTokenAuthorization`. |
| `client.EventHubFdRestClient` | `@RegisterRestClient(configKey = "event-hub-fd")`, `POST messages`: `sendMessage(FdUserNotificationToSend)`. Authorized by `EventhubFdTokenAuthorization`. |
| `auth.EventhubTokenAuthorization`, `auth.EventhubFdTokenAuthorization` | Client request filters setting `Authorization`: a Managed Identity bearer token (scope `https://eventhubs.azure.net/.default`, cached until expiry) if a client id is configured, otherwise a SAS token built from key name and key. |
| `model.*` | `UserNotificationToSend`, `FdUserNotificationToSend`, `UserGroupNotificationToSend`, `UserToNotify`, `TrackEventInput`, `NotificationUserType` (`ACTIVE_USER`, `SUSPEND_USER`, `DELETE_USER`), `constants.QueueEvent` (`ADD`, `UPDATE`) |
| `UserUtils` | `getSASToken`, `uniqueIdNotification`, `groupingProductAndReturnMinStateProduct`, `groupingProductWithRoleAndReturnMinStateProduct`, `mapPropsForTrackEvent`, `retrieveFdProduct` |

## Usage

```xml
<dependency>
  <groupId>it.pagopa.selfcare</groupId>
  <artifactId>selfcare-user-sdk-event</artifactId>
  <version>1.1.1</version>
</dependency>
```

```java
@RestClient
EventHubRestClient eventHubRestClient;

eventHubRestClient.sendMessage(notification);
```

Configuration read by the filters (values are set by the consuming app, secrets via env/Key Vault):

| Property | Used by |
|---|---|
| `quarkus.rest-client.event-hub.url` | `EventHubRestClient` base URL and SAS resource URI |
| `eventhub.rest-client.keyName`, `eventhub.rest-client.key` | SAS token |
| `eventhub.sender.managed-identity-client-id` (optional) | Managed Identity mode |
| `quarkus.rest-client.event-hub-fd.url` | `EventHubFdRestClient` |
| `eventhubfd.rest-client.keyName`, `eventhubfd.rest-client.key`, `eventhubfd.sender.managed-identity-client-id` (optional) | FD hub |

## Build and release

```bash
mvn --projects :selfcare-user-sdk-pom --also-make-dependents clean package -DskipTests
```

Released with the user SDK by `.github/workflows/release_selfcare_user_sdk_pom.yml`
(`mvn -B deploy -f libs/selfcare-user-sdk-pom/pom.xml`). Coverage profile `selfcare-user-sdk-event` in `test-coverage/pom.xml`.

## References

- [`libs/selfcare-user-sdk-event/README.md`](../../libs/selfcare-user-sdk-event/README.md) (outdated: old class names and artifact `user-sdk-event` 0.0.1)
- [user-cdc app](../apps/user-cdc.md), [user-group-cdc app](../apps/user-group-cdc.md), [user-ms app](../apps/user-ms.md)
