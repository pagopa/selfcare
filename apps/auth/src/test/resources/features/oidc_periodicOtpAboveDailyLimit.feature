@OidcAboveLimit
Feature: Oidc with periodic OTP flow above daily limit

  Scenario: Successful OIDC exchange with OTP feature flag set to "NONE"
    And The following request body:
      """
      {
          "code": "auth_code_123456",
          "redirectUri": "https://example.com/callback"
      }
      """
    When I send a POST request to "oidc/exchange"
    Then The status code is 200
    And The session token claims contains:
      | fiscal_number    | blbrki80A41H401T    |
      | name             | rocky               |
      | family_name      | Balboa              |
      | iss              | SPID                |

  Scenario: Successful OIDC exchange with OTP feature flag set to "BETA" and user not in beta list
    And OTP feature flag is set to "BETA"
    And The following request body:
      """
      {
          "code": "auth_code_123456",
          "redirectUri": "https://example.com/callback"
      }
      """
    When I send a POST request to "oidc/exchange"
    Then The status code is 200
    And The session token claims contains:
      | fiscal_number    | blbrki80A41H401T    |
      | name             | rocky               |
      | family_name      | Balboa              |
      | iss              | SPID                |

  Scenario: Successful OIDC exchange with OTP feature flag set to "BETA", user in beta list, forced OTP disabled (sameIdp true) and no previous OTP flow found
    Given User login with username "r.balboa" and password "test"
    And OTP feature flag is set to "BETA"
    And User in the beta user list with the following details:
      | fiscalCode | blbrki80A41H401T |
    And The following request body:
      """
      {
          "code": "auth_code_123456",
          "redirectUri": "https://example.com/callback"
      }
      """
    When I send a POST request to "oidc/exchange"
    Then The status code is 200
    And The session token claims contains:
      | fiscal_number    | blbrki80A41H401T    |
      | name             | rocky               |
      | family_name      | Balboa              |
      | iss              | SPID                |

  @RemoveOtpFlow
  Scenario: Successful OIDC exchange with OTP feature flag set to "BETA", user in beta list, forced OTP (sameIdp false) and no previous OTP flow found
    Given User login with username "r.balboa" and password "test"
    And OTP feature flag is set to "BETA"
    And User in the beta user list with the following details:
      | fiscalCode  | blbrki80A41H401T                |
      | forcedEmail | r.balboa@regionelazio.forced.it |
      | forceOtp    | true                            |
    And The following request body:
      """
      {
          "code": "auth_code_123456",
          "redirectUri": "https://example.com/callback"
      }
      """
    When I send a POST request to "oidc/exchange"
    Then The status code is 200
    And The response body contains:
      | requiresOtpFlow    | true                             |
      | maskedEmail        | r*.b****a@regionelazio.forced.it |
    And The response body contains field "otpSessionUid"
    And An OTP flow should be created with status "PENDING"

  Scenario: Successful OIDC exchange with OTP feature flag set to "BETA", user in beta list, forced OTP (sameIdp false) and expired previous OTP flow found
    Given User login with username "j.doe" and password "test"
    And OTP feature flag is set to "BETA"
    And User in the beta user list with the following details:
      | fiscalCode  | PRVTNT80A41H401T                |
      | forcedEmail | j.doe@regionelazio.forced.it    |
      | forceOtp    | true                            |
    And The following request body:
      """
      {
          "code": "auth_code_flow_present_123",
          "redirectUri": "https://example.com/callback"
      }
      """
    When I send a POST request to "oidc/exchange"
    Then The status code is 200
    And The response body contains:
      | requiresOtpFlow    | true                          |
      | maskedEmail        | j*.d*e@regionelazio.forced.it |
    And The response body contains field "otpSessionUid"
    And An OTP flow should be created with status "PENDING"

  Scenario: Successful OIDC exchange with OTP feature flag set to "BETA", user in beta list, forced OTP (sameIdp true) and expired previous OTP flow found
    Given User login with username "j.doe" and password "test"
    And OTP feature flag is set to "BETA"
    And User in the beta user list with the following details:
      | fiscalCode  | PRVTNT80A41H401T                |
      | forcedEmail | j.doe@regionelazio.forced.it    |
      | forceOtp    | true                            |
      | sameIdp     | true                            |
    And The following request body:
      """
      {
          "code": "auth_code_flow_present_123",
          "redirectUri": "https://example.com/callback"
      }
      """
    When I send a POST request to "oidc/exchange"
    Then The status code is 200
    And The response body contains:
      | requiresOtpFlow    | true                          |
      | maskedEmail        | j*.d*e@regionelazio.forced.it |
    And The response body contains field "otpSessionUid"
    And An OTP flow should be created with status "PENDING"

  @RemoveOtpFlow
  Scenario: Successful OIDC exchange with OTP feature flag set to "BETA", user in beta list, forced OTP (sameIdp false) and REJECTED previous OTP flow found
    Given User login with username "j.doe" and password "test"
    And OTP feature flag is set to "BETA"
    And User in the beta user list with the following details:
      | fiscalCode  | PRVTNT80A41H401T                |
      | forcedEmail | j.doe@regionelazio.forced.it    |
      | forceOtp    | true                            |
    And The following request body:
      """
      {
          "code": "auth_code_flow_present_123",
          "redirectUri": "https://example.com/callback"
      }
      """
    And An OTP flow with uuid "239b58f1-9865-4ef5-b45f-b7f574a0c84c" already exists with status "REJECTED" and attempts 1
    When I send a POST request to "oidc/exchange"
    Then The status code is 200
    And The response body contains:
      | requiresOtpFlow    | true                          |
      | maskedEmail        | j*.d*e@regionelazio.forced.it |
    And The response body contains field "otpSessionUid"
    And An OTP flow should be created with status "PENDING"

  @RemoveOtpFlow
  Scenario: Successful OIDC exchange with OTP feature flag set to "BETA", user in beta list, forced OTP (sameIdp true) and REJECTED previous OTP flow found
    Given User login with username "j.doe" and password "test"
    And OTP feature flag is set to "BETA"
    And User in the beta user list with the following details:
      | fiscalCode  | PRVTNT80A41H401T                |
      | forcedEmail | j.doe@regionelazio.forced.it    |
      | forceOtp    | true                            |
    And The following request body:
      """
      {
          "code": "auth_code_flow_present_123",
          "redirectUri": "https://example.com/callback"
      }
      """
    And An OTP flow with uuid "239b58f1-9865-4ef5-b45f-b7f574a0c84c" already exists with status "REJECTED" and attempts 1
    When I send a POST request to "oidc/exchange"
    Then The status code is 200
    And The response body contains:
      | requiresOtpFlow    | true                          |
      | maskedEmail        | j*.d*e@regionelazio.forced.it |
    And The response body contains field "otpSessionUid"
    And An OTP flow should be created with status "PENDING"

  Scenario: Successful OIDC exchange with OTP feature flag set to "BETA", user in beta list, forced OTP disabled (sameIdp true) and previous OTP flow was completed 3 months ago
    Given User login with username "j.doe" and password "test"
    And OTP feature flag is set to "BETA"
    And User in the beta user list with the following details:
      | fiscalCode  | PRVTNT80A41H401T                |
    And The following request body:
      """
      {
          "code": "auth_code_flow_present_123",
          "redirectUri": "https://example.com/callback"
      }
      """
    And An OTP flow with uuid "239b58f1-9865-4ef5-b45f-b7f574a0c84c" was COMPLETED 3 months ago
    When I send a POST request to "oidc/exchange"
    Then The status code is 200
    And The session token claims contains:
      | fiscal_number    | PRVTNT80A41H401T    |
      | name             | John                |
      | family_name      | Doe                 |
      | iss              | SPID                |

  Scenario: Successful OIDC exchange with OTP feature flag set to "BETA", user in beta list, forced OTP disabled (sameIdp true) and previous OTP flow was completed 7 months ago
    Given User login with username "j.doe" and password "test"
    And OTP feature flag is set to "BETA"
    And User in the beta user list with the following details:
      | fiscalCode  | PRVTNT80A41H401T                |
    And The following request body:
      """
      {
          "code": "auth_code_flow_present_123",
          "redirectUri": "https://example.com/callback"
      }
      """
    And An OTP flow with uuid "239b58f1-9865-4ef5-b45f-b7f574a0c84c" was COMPLETED 7 months ago
    When I send a POST request to "oidc/exchange"
    Then The status code is 200
    And The session token claims contains:
      | fiscal_number    | PRVTNT80A41H401T    |
      | name             | John                |
      | family_name      | Doe                 |
      | iss              | SPID                |

  @RemoveOtpFlow
  Scenario: Successful OIDC exchange with OTP feature flag set to "BETA", user in beta list, forced OTP (sameIdp false) and previous completed OTP flow found
    Given User login with username "j.doe" and password "test"
    And OTP feature flag is set to "BETA"
    And User in the beta user list with the following details:
      | fiscalCode  | PRVTNT80A41H401T                |
      | forcedEmail | j.doe@regionelazio.forced.it    |
      | forceOtp    | true                            |
    And The following request body:
      """
      {
          "code": "auth_code_flow_present_123",
          "redirectUri": "https://example.com/callback"
      }
      """
    And An OTP flow with uuid "239b58f1-9865-4ef5-b45f-b7f574a0c84c" already exists with status "COMPLETED" and attempts 1
    When I send a POST request to "oidc/exchange"
    Then The status code is 200
    And The response body contains:
      | requiresOtpFlow    | true                          |
      | maskedEmail        | j*.d*e@regionelazio.forced.it |
    And The response body contains field "otpSessionUid"
    And An OTP flow should be created with status "PENDING"

  Scenario: Successful OIDC exchange with OTP feature flag set to "BETA", user in beta list, forced OTP disabled (sameIdp true) and previous valid pending OTP flow found
    Given User login with username "j.doe" and password "test"
    And OTP feature flag is set to "BETA"
    And User in the beta user list with the following details:
      | fiscalCode  | PRVTNT80A41H401T                |
    And The following request body:
      """
      {
          "code": "auth_code_flow_present_123",
          "redirectUri": "https://example.com/callback"
      }
      """
    And An OTP flow with uuid "239b58f1-9865-4ef5-b45f-b7f574a0c84c" already exists with status "PENDING" and attempts 1
    When I send a POST request to "oidc/exchange"
    Then The status code is 200
    And The response body contains:
      | requiresOtpFlow    | true                                 |
      | otpSessionUid      | 239b58f1-9865-4ef5-b45f-b7f574a0c84c |
      | maskedEmail        | j*.d*e@regionelazio.it               |

  Scenario: Successful OIDC exchange with OTP feature flag set to "BETA", user in beta list, forced OTP (sameIdp false) and previous valid pending OTP flow found
    Given User login with username "j.doe" and password "test"
    And OTP feature flag is set to "BETA"
    And User in the beta user list with the following details:
      | fiscalCode  | PRVTNT80A41H401T                |
      | forcedEmail | j.doe@regionelazio.forced.it    |
      | forceOtp    | true                            |
    And The following request body:
      """
      {
          "code": "auth_code_flow_present_123",
          "redirectUri": "https://example.com/callback"
      }
      """
    And An OTP flow with uuid "239b58f1-9865-4ef5-b45f-b7f574a0c84c" already exists with status "PENDING" and attempts 1
    When I send a POST request to "oidc/exchange"
    Then The status code is 200
    And The response body contains:
      | requiresOtpFlow    | true                                 |
      | otpSessionUid      | 239b58f1-9865-4ef5-b45f-b7f574a0c84c |
      | maskedEmail        | j*.d*e@regionelazio.forced.it        |

  Scenario: Successful OIDC exchange with OTP feature flag set to "ALL", sameIdp true and no previous OTP flow found
    Given User login with username "r.balboa" and password "test"
    And OTP feature flag is set to "ALL"
    And The following request body:
      """
      {
          "code": "auth_code_123456",
          "redirectUri": "https://example.com/callback"
      }
      """
    When I send a POST request to "oidc/exchange"
    Then The status code is 200
    And The session token claims contains:
      | fiscal_number    | blbrki80A41H401T    |
      | name             | rocky               |
      | family_name      | Balboa              |
      | iss              | SPID                |

  Scenario: Successful OIDC exchange with OTP feature flag set to "ALL", sameIdp true and expired previous OTP flow found
    Given User login with username "j.doe" and password "test"
    And OTP feature flag is set to "ALL"
    And The following request body:
      """
      {
          "code": "auth_code_flow_present_123",
          "redirectUri": "https://example.com/callback"
      }
      """
    When I send a POST request to "oidc/exchange"
    Then The status code is 200
    And The response body contains:
      | requiresOtpFlow    | true                   |
      | maskedEmail        | j*.d*e@regionelazio.it |
    And The response body contains field "otpSessionUid"
    And An OTP flow should be created with status "PENDING"

  @RemoveOtpFlow
  Scenario: Successful OIDC exchange with OTP feature flag set to "ALL", sameIdp true and REJECTED previous OTP flow found
    Given User login with username "j.doe" and password "test"
    And OTP feature flag is set to "ALL"
    And The following request body:
      """
      {
          "code": "auth_code_flow_present_123",
          "redirectUri": "https://example.com/callback"
      }
      """
    And An OTP flow with uuid "239b58f1-9865-4ef5-b45f-b7f574a0c84c" already exists with status "REJECTED" and attempts 1
    When I send a POST request to "oidc/exchange"
    Then The status code is 200
    And The response body contains:
      | requiresOtpFlow    | true                          |
      | maskedEmail        | j*.d*e@regionelazio.it |
    And The response body contains field "otpSessionUid"
    And An OTP flow should be created with status "PENDING"

  Scenario: Successful OIDC exchange with OTP feature flag set to "ALL", sameIdp true and previous OTP flow was completed 3 months ago
    Given User login with username "j.doe" and password "test"
    And OTP feature flag is set to "ALL"
    And The following request body:
      """
      {
          "code": "auth_code_flow_present_123",
          "redirectUri": "https://example.com/callback"
      }
      """
    And An OTP flow with uuid "239b58f1-9865-4ef5-b45f-b7f574a0c84c" was COMPLETED 3 months ago
    When I send a POST request to "oidc/exchange"
    Then The status code is 200
    And The session token claims contains:
      | fiscal_number    | PRVTNT80A41H401T    |
      | name             | John                |
      | family_name      | Doe                 |
      | iss              | SPID                |

  Scenario: Successful OIDC exchange with OTP feature flag set to "ALL", sameIdp true and previous OTP flow was completed 7 months ago
    Given User login with username "j.doe" and password "test"
    And The following request body:
      """
      {
          "code": "auth_code_flow_present_123",
          "redirectUri": "https://example.com/callback"
      }
      """
    And An OTP flow with uuid "239b58f1-9865-4ef5-b45f-b7f574a0c84c" was COMPLETED 7 months ago
    When I send a POST request to "oidc/exchange"
    Then The status code is 200
    And The session token claims contains:
      | fiscal_number    | PRVTNT80A41H401T    |
      | name             | John                |
      | family_name      | Doe                 |
      | iss              | SPID                |

  Scenario: Successful OIDC exchange with OTP feature flag set to "ALL", sameIdp true and previous valid pending OTP flow found
    Given User login with username "j.doe" and password "test"
    And OTP feature flag is set to "ALL"
    And The following request body:
      """
      {
          "code": "auth_code_flow_present_123",
          "redirectUri": "https://example.com/callback"
      }
      """
    And An OTP flow with uuid "239b58f1-9865-4ef5-b45f-b7f574a0c84c" already exists with status "PENDING" and attempts 1
    When I send a POST request to "oidc/exchange"
    Then The status code is 200
    And The response body contains:
      | requiresOtpFlow    | true                                 |
      | otpSessionUid      | 239b58f1-9865-4ef5-b45f-b7f574a0c84c |
      | maskedEmail        | j*.d*e@regionelazio.it               |