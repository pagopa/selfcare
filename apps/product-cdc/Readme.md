# Product CDC Module

This module is designed to synchronize product configurations between the database and the cloud storage. It leverages a Change Data Capture (CDC) mechanism to detect real-time changes in the product collection and automatically updates the corresponding configuration files on the storage.

## 🚀 Overview

The **Product CDC** service listens for changes in the database (MongoDB) and propagates these updates to the Blob Storage. This ensures that the static configuration files consumed by other applications (like the frontend or other microservices) are always up-to-date with the latest data stored in the database.

### Key Features

*   **Real-time Synchronization**: Automatically detects inserts, updates, and deletes in the `products` collection.
*   **Storage Update**: Regenerates and uploads the product JSON configuration file to the configured Blob Storage container.
*   **Consistency**: Guarantees that the configuration file on storage reflects the current state of the database.

## 🛠 How It Works

1.  **CDC Trigger**: The application connects to the MongoDB Change Stream.
2.  **Event Detection**: When a document in the `products` collection is modified (created, updated, or deleted), an event is fired.
3.  **Processing**: The service processes the event, mapping the database entity to the required JSON format.
4.  **Storage Upload**: The updated JSON file is uploaded to the specific path in the Blob Storage, overwriting the previous version.

## 📦 Configuration

The application requires the following configurations to function correctly:

*   **MongoDB Connection**: Connection string and database name to listen for change streams.
*   **Blob Storage**: Connection string and container name where the product files are stored.

### Environment Variables

| Variable | Description |
| :--- | :--- |
| `MONGODB_CONNECTION_STRING` | Connection string for the MongoDB instance. |
| `MONGODB_DATABASE_NAME` | Name of the database containing the `products` collection. |
| `BLOB_STORAGE_CONN_STRING` | Connection string for the Azure Blob Storage. |
| `BLOB_STORAGE_CONTAINER_PRODUCT` | Container name for product configuration files. |

## 📝 Example Workflow

1.  An administrator updates the "IO" product description via the Backoffice API.
2.  MongoDB records the change and emits a change stream event.
3.  This module consumes the event.
4.  The module fetches the latest product data, converts it to the standard JSON format.
5.  The `products.json` file is regenerated with the latest product data and uploaded to the configured Blob Storage container, replacing the previous version.
6.  Other services and applications can now consume the updated `products.json` file from Blob Storage, ensuring they have access to the most current product information.

## 🧩 Error Handling & Logging

The module includes error handling to manage issues such as failed uploads, connectivity problems, or malformed data. All significant events and errors are logged for monitoring and troubleshooting purposes.

## 📚 Further Reading

For more details on configuration, deployment, and integration, refer to the [docs](../docs/product-cdc.md).
