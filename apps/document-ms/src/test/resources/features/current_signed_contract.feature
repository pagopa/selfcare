Feature: Current signed contract of an onboarding dossier

  Scenario: The latest signed and activated update is the current contract
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "/v1/documents/onboarding/curr-root-onb"
    Then The status code is 200
    And The response body contains:
      | onboardingId     | curr-update-completed-onb |
      | rootOnboardingId  | curr-root-onb             |

  Scenario: A signed but unactivated update does not replace the adhesion contract
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "/v1/documents/onboarding/curr-failed-root-onb"
    Then The status code is 200
    And The response body contains:
      | onboardingId | curr-failed-root-onb |

  Scenario: Activation is idempotent and makes the update current
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "/v1/documents/onboarding/curr-activate-root-onb"
    Then The status code is 200
    And The response body contains:
      | onboardingId | curr-activate-root-onb |
    When I send a PUT request to "/v1/documents/curr-activate-update-onb/activate"
    Then The status code is 204
    When I send a PUT request to "/v1/documents/curr-activate-update-onb/activate"
    Then The status code is 204
    When I send a GET request to "/v1/documents/onboarding/curr-activate-root-onb"
    Then The status code is 200
    And The response body contains:
      | onboardingId    | curr-activate-update-onb |
    And The response body contains field "activatedAt"

  Scenario: Activating an onboarding without a signed contract returns not found
    Given User login with username "j.doe" and password "test"
    When I send a PUT request to "/v1/documents/curr-unsigned-onb/activate"
    Then The status code is 404

  Scenario: The downloaded contract comes from the latest activated update
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "/v1/document-content/curr-root-onb/contract-signed"
    Then The status code is 200
    And The response header contains:
      | Content-Disposition | attachment;filename=signed_contratto_aggiornamento.pdf |


