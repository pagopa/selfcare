Feature: Document API validation

  Scenario: GET documents by onboardingId not found
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "/v1/documents/onboarding/onb-not-found"
    Then The status code is 404

  Scenario: GET document by id not found
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "/v1/documents/000000000000000000000000"
    Then The status code is 404

  Scenario: PUT update contract signed without existing document
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | contractSigned | path/to/contract-signed.pdf |
    When I send a PUT request to "/v1/documents/onb-not-found/contract-signed"
    Then The status code is 404

  Scenario: HEAD attachment status missing
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | name | missing.pdf |
    When I send a HEAD request to "/v1/documents/onb-not-found/attachment/status"
    Then The status code is 404

  Scenario: POST save document validation error
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
    {
      "productId": "prod-test",
      "documentType": "INSTITUTION"
    }
    """
    When I send a POST request to "/v1/documents"
    Then The status code is 400

  Scenario: POST import document validation error
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
    {
      "productId": "prod-test",
      "templatePath": "contract-templates/prod-test/5ca7f491-5770-40ba-baf0-6fe612bba14e.html"
    }
    """
    When I send a POST request to "/v1/documents/import"
    Then The status code is 400
