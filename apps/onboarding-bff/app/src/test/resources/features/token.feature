Feature: Token

  Scenario: Success to complete onboarding
    Given User login with username "j.doe" and password "test"
    And A mock-file of type "application/pdf" with key "contract" and document path "mock/mock-template.pdf" used to perform request
    When I send a POST request to "/v2/tokens/89ad7142-24bb-48ad-8504-9c9231137i103/complete" with multi-part file
    Then The status code is 204

  Scenario: Failed to complete onboarding when onboarding is missing
    Given User login with username "j.doe" and password "test"
    And A mock-file of type "application/pdf" with key "contract" and document path "mock/mock-template.pdf" used to perform request
    When I send a POST request to "/v2/tokens/89ad7142-24bb-48ad-8504-9c9231137i104/complete" with multi-part file
    Then The status code is 400
    And The response body contains:
      | errors[0].detail | Onboarding with id 89ad7142-24bb-48ad-8504-9c9231137i104 not found or it is expired! |

  Scenario: Success to complete onboarding-users
    Given User login with username "j.doe" and password "test"
    And A mock-file of type "application/pdf" with key "contract" and document path "mock/mock-template.pdf" used to perform request
    When I send a POST request to "/v2/tokens/89ad7142-24bb-48ad-8504-9c9231137i105/complete-onboarding-users" with multi-part file
    Then The status code is 204

  Scenario: Failed to complete onboarding-users when onboarding is missing
    Given User login with username "j.doe" and password "test"
    And A mock-file of type "application/pdf" with key "contract" and document path "mock/mock-template.pdf" used to perform request
    When I send a POST request to "/v2/tokens/89ad7142-24bb-48ad-8504-9c9231137i106/complete-onboarding-users" with multi-part file
    Then The status code is 400
    And The response body contains:
      | errors[0].detail | Onboarding with id 89ad7142-24bb-48ad-8504-9c9231137i106 not found or it is expired! |

  Scenario: Success to verify onboarding
    Given User login with username "j.doe" and password "test"
    When I send a POST request to "/v2/tokens/89ad7142-24bb-48ad-8504-9c9231137i103/verify"
    Then The status code is 200

  Scenario: Failed to verify onboarding when not found
    Given User login with username "j.doe" and password "test"
    When I send a POST request to "/v2/tokens/89ad7142-24bb-48ad-8504-9c9231137i106/verify"
    Then The status code is 404

  Scenario: Success to retrive onboarding
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "/v2/tokens/37f7609b-5a4b-4200-82e7-2117756d64aa"
    Then The status code is 200

  Scenario: Failed to retrive onboarding
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "/v2/tokens/89ad7142-24bb-48ad-8504-9c9231137i1019"
    Then The status code is 404

  Scenario: Success to approve onboarding
    Given User login with username "j.doe" and password "test"
    When I send a POST request to "/v2/tokens/89ad7142-24bb-48ad-8504-9c9231137i10222/approve"
    Then The status code is 200

  Scenario: Failed to approve onboarding when given invalid id
    Given User login with username "j.doe" and password "test"
    When I send a POST request to "/v2/tokens/89ad7142-24bb-48ad-8504-9c9231137i101/approve"
    Then The status code is 400

  Scenario: Failed to approve onboarding when is not found
    Given User login with username "j.doe" and password "test"
    When I send a POST request to "/v2/tokens/89ad7142-24bb-48ad-8504-9c9231137i101/approve"
    Then The status code is 400

  Scenario: Failed to approve onboarding when is already consumed
    Given User login with username "j.doe" and password "test"
    When I send a POST request to "/v2/tokens/89ad7142-24bb-48ad-8504-9c9231137i1022/approve"
    Then The status code is 409

  Scenario: Success to reject onboarding
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
      {
        "reason": "test reject"
      }
    """
    When I send a POST request to "/v2/tokens/89ad7142-24bb-48ad-8504-9c9231137i123/reject"
    Then The status code is 200

  Scenario: Failed to reject onboarding
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
      {
        "reason": "test reject"
      }
    """
    When I send a POST request to "/v2/tokens/89ad7142-24bb-48ad-8504-9c9231137i1000/reject"
    Then The status code is 400

  Scenario: Success to delete onboarding
    Given User login with username "j.doe" and password "test"
    When I send a DELETE request to "/v2/tokens/89ad7142-24bb-48ad-8504-9c9231137i103/complete"
    Then The status code is 204

  Scenario: Failed to delete onboarding when status is COMPLETED
    Given User login with username "j.doe" and password "test"
    When I send a DELETE request to "/v2/tokens/89ad7142-24bb-48ad-8504-9c9231137i1000/complete"
    Then The status code is 400

  #Scenario: Success to get Contract
  #  Given User login with username "j.doe" and password "test"
  #  When I send a GET request to "/v2/tokens/89ad7142-24bb-48ad-8504-9c9231137i103/contract"
  #  Then The status code is 200

  Scenario: Failed to get Contract
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "/v2/tokens/89ad7142-24bb-48ad-8504-9c9231137i10001/contract"
    Then The status code is 502
