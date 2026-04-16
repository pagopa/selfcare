@FeatureOnboarding
Feature: Onboarding

  # /onboarding/institution/{externalId}/products/{productId}

  Scenario: Successfully verify onboarding status given productId and externalId
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | externalId | 99000870064 |
      | productId  | prod-io |
    When I send a HEAD request to "/onboarding/institution/{externalId}/products/{productId}"
    Then The status code is 204

  Scenario: Not found onboarding with productId and externalId in status PENDING
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | externalId | 94076720658#UF5D7W |
      | productId  | prod-io            |
    When I send a HEAD request to "/onboarding/institution/{externalId}/products/{productId}"
    Then The status code is 404

  Scenario: Not found onboarding with non existing externalId and productId
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | externalId | 123 |
      | productId  | 456 |
    When I send a HEAD request to "/onboarding/institution/{externalId}/products/{productId}"
    Then The status code is 404

  Scenario: Bad Token onboarding with externalId and productId
    Given A bad jwt token
    And The following path params:
      | externalId | 123 |
      | productId  | 456 |
    When I send a HEAD request to "/onboarding/institution/{externalId}/products/{productId}"
    Then The status code is 401

  # /onboarding

  Scenario: Successfully verify onboarding status given taxcode and productId
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | taxCode   | 99000870064 |
      | productId | prod-pagopa |
    When I send a HEAD request to "/onboarding"
    Then The status code is 204

  Scenario: Successfully verify onboarding status given taxcode (as fiscal code) and productId
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | taxCode   | VRDMRA22T71F205A    |
      | productId | prod-idpay-merchant |
    When I send a HEAD request to "/onboarding"
    Then The status code is 204

  Scenario: Successfully verify onboarding status given taxcode, productId and subunit
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | taxCode     | 01726610056 |
      | productId   | prod-io     |
      | subunitCode | AVO36EJ     |
    When I send a HEAD request to "/onboarding"
    Then The status code is 204

  Scenario: Not found onboarding given taxcode, productId and subunit with status PENDING
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | taxCode     | 01726610056 |
      | productId   | prod-idpay  |
      | subunitCode | AVO36EJ     |
    When I send a HEAD request to "/onboarding"
    Then The status code is 404

  Scenario: Not found onboarding with non existing taxCode and productId
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | taxCode     | 123 |
      | productId   | 456 |
    When I send a HEAD request to "/onboarding"
    Then The status code is 404

  Scenario: Not found onboarding given subunit
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | taxCode     | 01726610056 |
      | productId   | prod-io     |
      | subunitCode | 123         |
    When I send a HEAD request to "/onboarding"
    Then The status code is 404

  Scenario: Bad request onboarding with missing queryParams taxCode and productId
    Given User login with username "j.doe" and password "test"
    When I send a HEAD request to "/onboarding"
    Then The status code is 400

  Scenario: Bad request onboarding with missing queryParam productId
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | taxCode | 01726610056 |
    When I send a HEAD request to "/onboarding"
    Then The status code is 400

  Scenario: Bad request onboarding with missing queryParam taxCode
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId | prod-io |
    When I send a HEAD request to "/onboarding"
    Then The status code is 400

  Scenario: Bad Token onboarding with externalId, productId and subunitCode
    Given A bad jwt token
    And The following query params:
      | taxCode     | 01726610056 |
      | productId   | prod-io     |
      | subunitCode | AVO36EJ     |
    When I send a HEAD request to "/onboarding"
    Then The status code is 401

  # /onboarding/verify

  Scenario: Successfully verify onboarding status with filter productId, externalId
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId  | prod-pagopa |
      | externalId | 85000870064 |
    When I send a HEAD request to "/onboarding/verify"
    Then The status code is 204

  Scenario: Successfully verify onboarding status with filter productId, taxCode (as fiscal code)
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | taxCode     | VRDMRA22T71F205A    |
      | productId   | prod-idpay-merchant |
    When I send a HEAD request to "/onboarding/verify"
    Then The status code is 204

  Scenario: Successfully verify onboarding status with filter productId, subunitCode, origin
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId   | prod-io     |
      | subunitCode | AVO36EJ     |
      | origin      | IPA         |
    When I send a HEAD request to "/onboarding/verify"
    Then The status code is 204

  Scenario: Successfully verify onboarding status with filter productId, origin, originId
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId   | prod-pagopa |
      | origin      | IPA         |
      | originId    | c_d277      |
    When I send a HEAD request to "/onboarding/verify"
    Then The status code is 204

  Scenario: Successfully verify onboarding status with every filter (externalId has priority)
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId   | prod-pagopa |
      | externalId  | 99000870064 |
      | subunitCode | 456         |
      | taxCode     | 123456789   |
      | origin      | XXX         |
      | originId    | 123456      |
    When I send a HEAD request to "/onboarding/verify"
    Then The status code is 204

  Scenario: Not found verifying onboarding status with filter productId, externalId
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId  | prod-pagopa |
      | externalId | 123         |
    When I send a HEAD request to "/onboarding/verify"
    Then The status code is 404

  Scenario: Not found verifying onboarding status with filter productId, taxCode, subunitCode, origin
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | taxCode     | XXX         |
      | productId   | prod-io     |
      | subunitCode | AVO36EJ     |
      | origin      | IPA         |
    When I send a HEAD request to "/onboarding/verify"
    Then The status code is 404

  Scenario: Not found verifying onboarding status with filter productId, origin, originId (without subunitCode)
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId   | prod-io     |
      | origin      | IPA         |
      | originId    | AVO36EJ     |
    When I send a HEAD request to "/onboarding/verify"
    Then The status code is 404

  Scenario: Bad request verify onboarding status with only filter productId
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId | prod-pagopa |
    When I send a HEAD request to "/onboarding/verify"
    Then The status code is 400

  Scenario: Bad request verify onboarding status with filter productId, origin
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId | prod-pagopa |
      | origin    | IPA         |
    When I send a HEAD request to "/onboarding/verify"
    Then The status code is 400

  Scenario: Bad request verify onboarding status with filter productId, originId
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId | prod-pagopa |
      | originId  | c_d277      |
    When I send a HEAD request to "/onboarding/verify"
    Then The status code is 400

  Scenario: Bad Token verify onboarding
    Given A bad jwt token
    And The following query params:
      | productId   | prod-pagopa |
      | origin      | IPA         |
      | originId    | c_d277      |
    When I send a HEAD request to "/onboarding"
    Then The status code is 401
