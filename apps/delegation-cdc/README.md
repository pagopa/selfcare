# Microservice Institution

Our dedicated microservice is crafted to expertly manage all events related to operations, such as insertion, update, and deletion, 
within the MongoDB collections residing in the delegations domain, to align delegations collection properly.
This specialized solution has been meticulously designed to mitigate potential concurrency issues arising from the presence of multiple active instances 
on the main microservices.

## Configuration Properties

Before running you have to set these properties as environment variables.

| **Property**                                  | **Environment Variable**                     | **Default** | **Required** |
|-----------------------------------------------|----------------------------------------------|-------------|:------------:|
| quarkus.mongodb.connection-string             | MONGODB-CONNECTION-STRING                    |             |     yes      |
| delegation-cdc.app-insights.connection-string | DELEGATION-CDC-APPINSIGHTS-CONNECTION-STRING |             |     yes      |   
| delegation-cdc.storage.connection-string      | STORAGE_CONNECTION_STRING                    |             |     yes      |


> **_NOTE:_**  properties that contains secret must have the same name of its secret as uppercase.

