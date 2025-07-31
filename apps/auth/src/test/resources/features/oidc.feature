Feature: Oidc

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

  Scenario: Successful OIDC exchange with OTP feature flag set to "BETA", user in beta list, forced OTP disabled and no previous OTP flow found
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
  Scenario: Successful OIDC exchange with OTP feature flag set to "BETA", user in beta list, forced OTP
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

  @RemoveOtpFlow
  Scenario: Successful OIDC exchange with OTP feature flag set to "BETA", user in beta list, forced OTP, previous OTP flow found and new Otp flow required
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

  Scenario: Successful OIDC exchange with OTP feature flag set to "BETA", user in beta list, forced OTP disabled and previous completed OTP flow with sameIdp=true found
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
    And An OTP flow with uuid "239b58f1-9865-4ef5-b45f-b7f574a0c84c" already exists with status "COMPLETED" and attempts 1
    When I send a POST request to "oidc/exchange"
    Then The status code is 200
    And The session token claims contains:
      | fiscal_number    | PRVTNT80A41H401T    |
      | name             | John                |
      | family_name      | Doe                 |
      | iss              | SPID                |

  Scenario: Successful OIDC exchange with OTP feature flag set to "BETA", user in beta list, forced OTP disabled and previous pending OTP flow with sameIdp=false found
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

  Scenario: Successful OIDC exchange with OTP feature flag set to "BETA", user in beta list, forced OTP and previous pending OTP flow found
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


  Scenario: Not found token in one identity create request token
    And The following request body:
    """
    {
        "code": "not_found_auth_code_123",
        "redirectUri": "https://example.com/not_found"
    }
    """
    When I send a POST request to "oidc/exchange"
    Then The status code is 404
    And The response body contains:
      | status | 404       |
      | detail | Not Found:Received: 'Not Found, status code 404' when invoking: Rest Client method: 'org.openapi.quarkus.one_identity_json.api.DefaultApi#createRequestToken' |

  Scenario: Fail extract claims from Jwt token
    And The following request body:
      """
      {
          "code": "auth_code_bad_token_123",
          "redirectUri": "https://example.com/callback"
      }
      """
    When I send a POST request to "oidc/exchange"
    Then The status code is 500
    And The response body contains:
      | status | 500       |
      | detail | Cannot parse idToken from authorization code |

  Scenario: Fail saving user using patch, user not found in user registry
    And The following request body:
      """
      {
          "code": "auth_code_mario_rossi_987",
          "redirectUri": "https://example.com/callback"
      }
      """
    When I send a POST request to "oidc/exchange"
    Then The status code is 500
    And The response body contains:
      | status | 500       |
      | detail | Cannot patch user on Personal Data Vault:it.pagopa.selfcare.auth.exception.ResourceNotFoundException: Not Found:Received: 'Not Found, status code 404' when invoking: Rest Client method: 'org.openapi.quarkus.user_registry_json.api.UserApi#saveUsingPATCH' |

  Scenario: Fail getting user info email on external internal APIs
    And OTP feature flag is set to "BETA"
    And User in the beta user list with the following details:
      | fiscalCode  | VRDMRA22T71F205A                |
      | forcedEmail | m.verdi@regionelazio.forced.it  |
      | forceOtp    | true                            |
    And The following request body:
      """
      {
          "code": "auth_code_mario_verdi_567",
          "redirectUri": "https://example.com/callback"
      }
      """
    When I send a POST request to "oidc/exchange"
    Then The status code is 500
    And The response body contains:
      | status | 500       |
      | detail | Cannot Handle OTP Flow:it.pagopa.selfcare.auth.exception.InternalException: Cannot get User Info Email on External Internal APIs:it.pagopa.selfcare.auth.exception.InternalException: Internal server error:Received: 'Internal Server Error, status code 500' when invoking: Rest Client method: 'org.openapi.quarkus.internal_json.api.UserApi#v2getUserInfoUsingGET' |