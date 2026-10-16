@TenantIsolationAcceptance
Feature: Tenant database isolation at request boundaries

  Scenario: Same identifiers resolve to tenant-specific documents and notification changes
    Given tenant isolation acceptance data is prepared
    And I authenticate as "j.doe" for tenant "AR"
    When I retrieve institution "tenant-acceptance-shared-institution" using tenant header "ar"
    Then The status code is 200
    And the tenant acceptance institution description is "AR institution"
    When I retrieve delegations for the tenant acceptance institution
    Then The status code is 200
    And the tenant acceptance delegation product is "ar-product"
    And tenant acceptance mail notifications are isolated
    And I authenticate as "j.doe" for tenant "PNPG"
    When I retrieve institution "tenant-acceptance-shared-institution" using tenant header "PNPG"
    Then The status code is 200
    And the tenant acceptance institution description is "PNPG institution"
    When I retrieve delegations for the tenant acceptance institution
    Then The status code is 200
    And the tenant acceptance delegation product is "pnpg-product"

  Scenario: Invalid tenant headers reject writes before persistence
    Given tenant isolation acceptance data is prepared
    And I authenticate as "j.doe" for tenant "AR"
    When I attempt a delegation write with tenant selection "missing"
    Then tenant selection is rejected before persistence
    When I attempt a delegation write with tenant selection "unsupported"
    Then tenant selection is rejected before persistence
    When I attempt a delegation write with tenant selection "duplicate"
    Then tenant selection is rejected before persistence
    When I attempt a delegation write with tenant selection "mismatch"
    Then tenant selection is rejected before persistence
    When I retrieve institution "tenant-acceptance-shared-institution" using tenant header "AR"
    Then The status code is 200
    And the tenant acceptance institution description is "AR institution"
