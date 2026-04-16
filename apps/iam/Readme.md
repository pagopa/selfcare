# IAM Module

## Overview

The **IAM (Identity and Access Management)** module is responsible for managing internal users with SSO (Single Sign-On) access and handling roles and permissions for all users, including both SSO and SPID (Public Digital Identity System) authenticated users.

This module provides centralized identity management, role-based access control (RBAC), and permission management across different products and services within the platform.

## Key Features

- **User Management**: Create, update, and retrieve user information
- **SSO Integration**: Support for internal users with Single Sign-On authentication
- **SPID Support**: Management of users authenticated via SPID (Italian public identity system)
- **Role-Based Access Control (RBAC)**: Assign and manage roles per product
- **Permission Management**: Fine-grained permission control through role-to-permission mapping
- **Multi-Product Support**: Users can have different roles across multiple products
- **Reactive Architecture**: Built with Quarkus and Mutiny for non-blocking operations

## Architecture

The module is built using:
- **Quarkus**: Reactive framework
- **MongoDB**: NoSQL database for flexible schema and embedded documents
- **Panache MongoDB**: Simplified MongoDB operations with reactive support
- **Mutiny**: Reactive programming library

## Data Model

### Entity: UserClaims

**MongoDB Collection**: `userClaims`

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `_id` | String | PK, Required | Unique identifier (UUID)  |
| `email` | String | Unique, Required | User's email (primary key) |
| `name` | String | Optional | User's first name |
| `familyName` | String | Optional | User's last name |
| `productRoles` | Array<ProductRoles> | Optional | List of roles per product |

**Indexes:**
- Primary: `_id` (userId)
- Secondary: `email` (unique)

**Sample Document:**
```json
{
  "_id": "56cf116a-0cd7-4f0c-8ace-1acd33f81751",
  "email": "user@example.com",
  "name": "Mario",
  "familyName": "Rossi",
  "productRoles": [
    {
      "productId": "product1",
      "roles": ["admin", "operator"]
    },
    {
      "productId": "product2",
      "roles": ["viewer"]
    }
  ]
}
```

---

### Entity: Roles

**MongoDB Collection**: `roles`

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `_id` | String | PK, Required | Role name (primary key) |
| `permissions` | Array<String> | Required | List of permissions associated with the role |

**Sample Document:**
```json
{
  "_id": "admin",
  "permissions": [
    "read:users",
    "write:users",
    "delete:users",
    "manage:roles"
  ]
}
```

---

### Embedded Model: ProductRoles

**Embedded in UserClaims**

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `productId` | String | Required | Product identifier |
| `roles` | Array<String> | Required | List of role names for this product |

**Sample:**
```json
{
  "productId": "product1",
  "roles": ["admin", "operator"]
}
```

---

## Relationships

```
UserClaims (1) ──contains──> (N) ProductRoles [embedded]
ProductRoles (N) ──references──> (M) Roles [by name]
```

### 1. UserClaims → ProductRoles
- **Type**: 1:N (One-to-Many)
- **Implementation**: Embedded documents
- **Description**: A user can have zero or more ProductRoles. ProductRoles are embedded within the UserClaims document for performance and atomic updates.

### 2. ProductRoles → Roles
- **Type**: N:M (Many-to-Many)
- **Implementation**: Reference by ID (role name)
- **Description**: `ProductRoles.roles[]` contains role names (foreign keys) that reference `Roles._id`. The relationship is resolved via MongoDB aggregation using `$lookup`.

---

## Aggregation Queries

### Extract User Permissions

The following aggregation pipeline extracts all permissions for a specific user and product by joining `UserClaims` with `Roles`:

```json
[
  {
    "$match": {
      "_id": "user-uid"
    }
  },
  {
    "$unwind": "$productRoles"
  },
  {
    "$match": {
      "productRoles.productId": "product1"
    }
  },
  {
    "$unwind": "$productRoles.roles"
  },
  {
    "$lookup": {
      "from": "roles",
      "localField": "productRoles.roles",
      "foreignField": "_id",
      "as": "roleDetails"
    }
  },
  {
    "$unwind": "$roleDetails"
  },
  {
    "$unwind": "$roleDetails.permissions"
  },
  {
    "$match": {
      "roleDetails.permissions": "read:users"
    }
  },
  {
    "$group": {
      "_id": {
        "uid": "$_id",
        "email": "$email",
        "productId": "$productRoles.productId"
      },
      "permissions": {
        "$addToSet": "$roleDetails.permissions"
      }
    }
  },
  {
    "$project": {
      "_id": 0,
      "email": "$_id.email",
      "uid": "$_id.uid",
      "productId": "$_id.productId",
      "permissions": "$permissions"
    }
  }
]
```

**Result:**
```json
{
  "email": "user@example.com",
  "uid": "56cf116a-0cd7-4f0c-8ace-1acd33f81751",
  "productId": "product1",
  "permissions": [
    "read:users"
  ]
}
```

---

## API Endpoints

### User Management

#### Save/Update User
```http
PATCH /iam/users?productId={productId}
Content-Type: application/json

{
  "email": "user@example.com",
  "name": "Mario",
  "familyName": "Rossi",
  "productRoles": ["admin", "operator"]
}
```

#### Get User
```http
GET /iam/users/{uid}?productId={productId}
```

### Permission Management

#### Get User Permissions
```http
GET /iam/users/{uid}/permissions?productId={productId}&permission={permission}
```

**Response:**
```json
{
  "email": "user@example.com",
  "uid": "56cf116a-0cd7-4f0c-8ace-1acd33f81751",
  "productId": "product1",
  "permissions": ["read:users", "write:users"]
}
```

---

## Domain Model Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        UserClaims                           │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ _id: String (email) [PK]                               │ │
│  │ uid: String [Unique]                                   │ │
│  │ name: String                                           │ │
│  │ familyName: String                                     │ │
│  │ productRoles: [                                        │ │
│  │   {                                                    │ │
│  │     productId: String                                  │ │
│  │     roles: ["admin", "operator"]   ──┐                 │ │
│  │   }                                  │                 │ │
│  │ ]                                    │                 │ │
│  └──────────────────────────────────────┼─────────────────┘ │
└───────────────────────────────────────┬─┼───────────────────┘
                                        │ │ references by name
                          embedded      │ │
                                        │ │
                                        │ └──────────────────┐
                                        │                    │
┌───────────────────────────────────────▼────────────────────▼──┐
│                           Roles                               │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │ _id: String (role name) [PK]                             │ │
│  │ permissions: [                                           │ │
│  │   "read:users",                                          │ │
│  │   "write:users",                                         │ │
│  │   "delete:users"                                         │ │
│  │ ]                                                        │ │
│  └──────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────┘
```

---

## Authentication Flow

### SSO Users (Internal)
1. User authenticates via SSO provider
2. IAM module creates/updates UserClaims with email as identifier
3. Roles and permissions are assigned per product
4. User accesses resources based on assigned permissions

### SPID Users (External)
1. User authenticates via SPID
2. IAM module creates/updates UserClaims with SPID identifier
3. Same role and permission management as SSO users
4. Cross-platform identity management

---

## Business Rules

1. **UID as Primary Key**: UUID (uid) generated on first save serves as the primary identifier (_id) in MongoDB
2. **Unique Email**: Each user has a unique email 
3. **Product Isolation**: Roles are scoped per product, allowing different permissions across products
4. **Role Inheritance**: Permissions are resolved through role-to-permission mapping
5. **Atomic Updates**: ProductRoles updates for a specific product don't affect other products
6. **Permission Aggregation**: Multiple roles result in a union of all associated permissions

---

## Usage Examples

### Create User with Roles
```java
SaveUserRequest request = new SaveUserRequest();
request.setEmail("user@example.com");
request.setName("Mario");
request.setFamilyName("Rossi");
request.setProductRoles(List.of("admin", "operator"));

iamService.saveUser(request, "product1")
  .subscribe().with(
    user -> log.info("User created: {}", user.getUid()),
    failure -> log.error("Failed to create user", failure)
  );
```

### Get User Permissions
```java
userPermissionsRepository.getUserPermissions(uid, "read:users", "product1")
  .subscribe().with(
    permissions -> log.info("Permissions: {}", permissions.getPermissions()),
    failure -> log.error("Failed to get permissions", failure)
  );
```

### Update User Roles for a Product
```java
// Adds or updates roles for product2 without affecting product1
SaveUserRequest request = new SaveUserRequest();
request.setEmail("user@example.com");
request.setProductRoles(List.of("viewer"));

iamService.saveUser(request, "product2")
  .subscribe().with(
    user -> log.info("Roles updated for product2"),
    failure -> log.error("Failed to update roles", failure)
  );
```

---

## Configuration

### MongoDB Connection
```properties
quarkus.mongodb.connection-string=${MONGODB_CONNECTION_STRING:mongodb://localhost:27017}
quarkus.mongodb.database=selcIam
```

### Indexes (MongoDB Shell)
```javascript
db.userClaims.createIndex({ "email": 1 }, { unique: true })
db.userClaims.createIndex({ "productRoles.productId": 1 })
db.roles.createIndex({ "_id": 1 }, { unique: true })
```

---

## Sample data

### userClaims
```bash
[{
  "_id": "c9b96232-9e1b-4744-bc45-256355d40020",
  "email": "test1@mail.xyz"
},{
  "_id": "56cf116a-0cd7-4f0c-8ace-1acd33f81751",
  "email": "test2@mail.xyz",
  "name": "Test",
  "productRoles": [
    {
      "productId": "product2",
      "roles": [
        "role1",
        "role4"
      ]
    },
    {
      "productId": "product1",
      "roles": [
        "role1",
        "role2"
      ]
    }
  ]
}]
```

### roles
```bash
[{
  "_id": "role1",
  "permissions": [
    "permission1",
    "permission2"
  ]
},{
  "_id": "role2",
  "permissions": [
    "permission1",
    "permission2",
    "permission3"
  ]
},{
  "_id": "role4",
  "permissions": [
    "permission1",
    "permission3",
    "permission4"
  ]
}]
```
---

## Future Enhancements

- [ ] Permission caching for improved performance
- [ ] Audit logging for role/permission changes
- [ ] Role hierarchy support (role inheritance)
- [ ] Multi-tenancy support

---
