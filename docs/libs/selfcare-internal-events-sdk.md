# selfcare-internal-events-sdk

> Framework-agnostic SDK to publish and consume SelfCare internal domain events over Azure Service Bus topics with sessions (ordered per key), including a dead-letter queue handler.

| | |
|---|---|
| Path | `libs/selfcare-internal-events-sdk` |
| Artifact | `it.pagopa.selfcare:selfcare-internal-events-sdk:0.0.2` (parent aggregator `it.pagopa.selfcare:libs:0.0.1`) |
| Stack | Plain Java 17, `azure-messaging-servicebus`, `azure-identity`, Jackson (`jsr310`), SLF4J, Lombok |
| Used by | `institution-cdc` (publisher), `user-ms` and `user-group-ms` (subscribers) |

## What it provides

Package `it.pagopa.selfcare.internalevents`:

| Type | Purpose |
|---|---|
| `model.InternalEvent` | Abstract base event: `eventId` (random UUID), `eventType` (simple class name), `eventTime`, `eventTenant`, `eventSource`. Non-serialized fields `eventTargetStatus` (`COMPLETE` default, `ABANDON`, `DEAD_LETTER`), `eventDeadLetterReason`, `eventDeadLetterDescription` let the handler decide the message outcome. |
| `model.@InternalEventTopic` | Class annotation binding an event type to a Service Bus topic. |
| `model.@InternalEventSessionKey` | Field annotation: the value becomes the Service Bus `sessionId` (ordering key). |
| `model.InstitutionUpdatedEvent` | Topic `institution-events`, session key `institutionId`, plus `description`, `parentDescription`. |
| `service.InternalEventSender` | `publish(InternalEvent)` → `boolean`. |
| `service.InternalEventReceiver` | `subscribe(Class<T>, Consumer<T>)`. |
| `service.InternalEventDLQHandler` | `handle(maxMessages, timeoutSeconds, Consumer<InternalEvent>)` for the subscription dead-letter queue. |
| `service.impl.AzureServiceBusEventSender` | Rejects events whose `@InternalEventTopic` differs from the configured topic; sets `sessionId` and application properties `eventType`, `eventTenant`, `eventSource`. |
| `service.impl.AzureServiceBusEventReceiver` | Session processor, `PEEK_LOCK`, auto-complete disabled, configurable `maxConcurrentSessions` / `maxConcurrentCalls`. Messages with an unsubscribed `eventType` are completed (dropped); handler exceptions abandon the message; otherwise the outcome follows `eventTargetStatus`. |
| `service.impl.AzureServiceBusEventDLQHandler` | Same outcome logic on the dead-letter sub-queue. |
| `util.InternalEventUtils` | Shared `ObjectMapper` (Java time), topic and session-key resolution. |

Each Azure implementation has two constructors: `(namespace, managedIdentityClientId, topic, ...)` for Managed
Identity, or `(connectionString, topic, ...)`.

The receiver resolves the event class as `it.pagopa.selfcare.internalevents.model.<eventType>`, so new event types
must be added to this library.

## Usage

```xml
<dependency>
  <groupId>it.pagopa.selfcare</groupId>
  <artifactId>selfcare-internal-events-sdk</artifactId>
  <version>0.0.2</version>
</dependency>
```

```java
InternalEventReceiver receiver = new AzureServiceBusEventReceiver(
    namespace, clientId, "institution-events", "user-sub", maxSessions, maxCalls);
receiver.subscribe(InstitutionUpdatedEvent.class, event -> { /* handle */ });
```

Consumers wire it in an `internalevents/InternalEvents.java` class, configured by app properties (the SDK reads none):

| App | Role | Properties | Subscription |
|---|---|---|---|
| `institution-cdc` | sender on `institution-events` | `institution-cdc.internalevents.{enabled,connection-string,namespace,client-id}` | — |
| `user-ms` | receiver | `user-ms.internalevents.{enabled,connection-string,namespace,client-id}` | `user-sub` |
| `user-group-ms` | receiver | `user-group-ms.internalevents.{enabled,connection-string,namespace,client-id}` | `user-group-sub` |

`enabled` defaults to `false` in all three apps.

## Build and release

```bash
mvn --projects :selfcare-internal-events-sdk --also-make clean package -DskipTests
```

Release: `.github/workflows/release_selfcare_internal_events_sdk.yml` (push on `main` with path filter on the lib,
or `workflow_dispatch`) runs `mvn -B deploy -f libs/selfcare-internal-events-sdk/pom.xml`; the POM declares
`distributionManagement` to `https://maven.pkg.github.com/pagopa/selfcare`. The parent `libs/pom.xml` sets
`maven.deploy.skip=true` and this POM does not override it; verify that the deploy step actually publishes.

No README in the lib folder.

## References

- [institution-cdc app](../apps/institution-cdc.md), [user-ms app](../apps/user-ms.md), [user-group-ms app](../apps/user-group-ms.md)
- [Architecture](../architecture.md)
