@TenantIsolation
Feature: Single-process tenant isolation

  Scenario: Same identifiers are routed to independent tenant databases
    Given An authenticated request for tenant "AR"
    When I send a GET request to "institutions/tenant-isolation-institution/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].institutionDescription | AR institution |
    Given An authenticated request for tenant "PNPG"
    When I send a GET request to "institutions/tenant-isolation-institution/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].institutionDescription | PNPG institution |

  Scenario: Writes in one tenant cannot change the matching institution in the other
    Given An authenticated request for tenant "AR"
    And The following request body:
      """
      {"institutionDescription":"AR updated"}
      """
    When I send a PUT request to "institutions/tenant-isolation-institution"
    Then The status code is 204
    Given An authenticated request for tenant "PNPG"
    When I send a GET request to "institutions/tenant-isolation-institution/user-institutions"
    Then The status code is 200
    And The response body contains:
      | [0].institutionDescription | PNPG institution |
    Given An authenticated request for tenant "AR"
    And The following request body:
      """
      {"institutionDescription":"AR institution"}
      """
    When I send a PUT request to "institutions/tenant-isolation-institution"
    Then The status code is 204

  Scenario: Missing tenant is rejected for an authenticated request
    Given An authenticated request for tenant "AR"
    And The tenant header is absent
    When I send a GET request to "institutions/tenant-isolation-institution/user-institutions"
    Then The status code is 400

  Scenario: Unsupported tenant is rejected
    Given An authenticated request for tenant "AR"
    And The tenant header is "UNKNOWN"
    When I send a GET request to "institutions/tenant-isolation-institution/user-institutions"
    Then The status code is 400

  Scenario: JWT and header mismatch is rejected
    Given An authenticated request for tenant "AR"
    And The tenant header is "PNPG"
    When I send a GET request to "institutions/tenant-isolation-institution/user-institutions"
    Then The status code is 401

  Scenario: Legacy records remain readable in their tenant database
    Given An authenticated request for tenant "PNPG"
    When I send a GET request to "institutions/legacy-pnpg-institution/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
