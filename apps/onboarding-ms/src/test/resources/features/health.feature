Feature: Readiness health endpoint reports downstream dependencies status

  Background:
    Given the readiness endpoint is available at "/q/health/ready"

  @Health
  Scenario: Endpoint reports UP when MongoDB is reachable
    When I call the readiness endpoint
    Then the readiness HTTP status is 200
    And  the readiness overall status is "UP"
    And  the readiness response contains a check named "mongodb-onboarding" with status "UP"

  @Health
  Scenario: Product catalog no longer requires Blob Storage readiness
    When I call the readiness endpoint
    Then the readiness response does not contain a check named "blob-storage-product"
