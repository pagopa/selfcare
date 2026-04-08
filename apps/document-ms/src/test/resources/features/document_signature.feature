Feature: Signature API validation

  Scenario: Verify contract signature without file
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
    {
      "onboardingId": "onb-123",
      "fiscalCodes": ["RSSMRA80A01F205X"],
      "skipSignatureVerification": false
    }
    """
    When I send a POST request to "/v1/signature/verify"
    Then The status code is 400
