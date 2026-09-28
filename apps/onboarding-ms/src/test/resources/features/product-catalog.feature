@Onboarding
Feature: Product catalog HTTP integration

  Scenario Outline: The same product is read from the requested tenant
    When I request product "prod-io" for tenant "<tenant>"
    Then the response status code should be 200
    And the response should have field "tenantId" with value "<tenant>"
    When I request the expiration of product "prod-io" for tenant "<tenant>"
    Then the response status code should be 200
    And the product expiration is <expiration> days

    Examples:
      | tenant | expiration |
      | AR     | 30         |
      | PNPG   | 45         |

  Scenario Outline: Products are not read from another tenant
    When I request product "<product>" for tenant "<tenant>"
    Then the response status code should be 404
    And the response should have field "exception" with value "it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException"

    Examples:
      | tenant | product     |
      | PNPG   | prod-pagopa |
      | AR     | prod-pn-pg  |
