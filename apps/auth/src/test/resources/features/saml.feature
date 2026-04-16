Feature: SAML Login Flow

  Background:
    Given SAML service is available

#  Scenario: Successful SAML login
#    When I submit a valid SAML response
#    Then a session token is generated
#    And the user is synchronized with IAM

  Scenario: Invalid SAML signature
    When I submit a SAML response with invalid signature
    Then the login fails with signature error

  Scenario: Expired SAML assertion
    When I submit an expired SAML response
    Then the login fails with time interval error