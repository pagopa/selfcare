Feature: Readiness health endpoint reports downstream dependencies status

  Background:
    Given the readiness endpoint is available at "/q/health/ready"

  @Health
  Scenario: Endpoint reports UP when Azure Blob Storage (Azurite) and MongoDB are reachable
    When I call the readiness endpoint
    Then the readiness HTTP status is 200
    And  the readiness overall status is "UP"
    And  the readiness response contains a check named "blob-storage-contracts" with status "UP"
    And  the readiness response contains a check named "blob-storage-user-attachments" with status "UP"
    And  the readiness response contains a check named "mongodb-document" with status "UP"

  @Health
  Scenario: Blob storage checks expose probe metadata for troubleshooting
    When I call the readiness endpoint
    Then the readiness check "blob-storage-contracts" data contains key "container"
    And  the readiness check "blob-storage-contracts" data contains key "canaryBlob"
    And  the readiness check "blob-storage-contracts" data contains key "latencyMs"
    And  the readiness check "blob-storage-user-attachments" data contains key "container"
    And  the readiness check "blob-storage-user-attachments" data contains key "canaryBlob"
    And  the readiness check "blob-storage-user-attachments" data contains key "latencyMs"

