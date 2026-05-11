@support
Feature: Support

  Scenario: Successfully sending a support request with valid data
    Given user login with username "j.doe" and password "test"
    And the following support request details:
      | email        | institutionId | productId  | userId                              |
      | test@mail.it | institutionId | product123 | 97a511a7-2acc-47b9-afed-2f3c65753b4a |
    When I send a POST request to "/v1/support" with the given details to send a support request
    Then the response status should be 200
    And the response should contain a JWT token
    And the response should contain a redirect URL with institutionId and productId
    And the response should contain an actionURL

  Scenario: Successfully sending a support request without productId
    Given user login with username "j.doe" and password "test"
    And the following support request details:
      | email        | institutionId | userId                              |
      | test@mail.it | institutionId | 97a511a7-2acc-47b9-afed-2f3c65753b4a|
    When I send a POST request to "/v1/support" with the given details to send a support request
    Then the response status should be 200
    And the response should contain a JWT token
    And the response should contain a redirect URL without productId but with institutionId

  Scenario: Successfully sending a support request without institutionId
    Given user login with username "j.doe" and password "test"
    And the following support request details:
      | email        | productId  | userId                              |
      | test@mail.it | product123 | 97a511a7-2acc-47b9-afed-2f3c65753b4a|
    When I send a POST request to "/v1/support" with the given details to send a support request
    Then the response status should be 200
    And the response should contain a JWT token
    And the response should contain a redirect URL without institutionId but with productId

  Scenario: Successfully sending a support request without productId and institutionId
    Given user login with username "j.doe" and password "test"
    And the following support request details:
      | email        | userId                               |
      | test@mail.it | 97a511a7-2acc-47b9-afed-2f3c65753b4a |
    Then the response status should be 200
    When I send a POST request to "/v1/support" with the given details to send a support request
    And the response should contain a JWT token
    And the response should contain a redirect URL without productId and institutionId

  Scenario: Successfully sending a support request with data field
    Given user login with username "j.doe" and password "test"
    And the following support request details:
      | email        | productId  | data                                                                                               |
      | test@mail.it | product123 | %7B%0A%20%20%20%20%22key1%22%3A%20%22value1%22%2C%0A%20%20%20%20%22key2%22%3A%20%22value2%22%0A%7D |
    Then the response status should be 200
    When I send a POST request to "/v1/support" with the given details to send a support request
    And the response should contain a JWT token
    And the response should contain a redirect URL with productId and data

  Scenario: Successfully sending a support request with data field
    Given user login with username "j.doe" and password "test"
    And the following support request details:
      | email        | data              |
      | test@mail.it | hello+big%20world |
    Then the response should contain a JWT token
    When I send a POST request to "/v1/support" with the given details to send a support request
    And the response status should be 200
    And the response should contain a JWT token
    And the response should contain a redirect URL with data

  Scenario: Bad request with data not url encoded
    Given user login with username "j.doe" and password "test"
    And the following support request details:
      | email        | data               |
      | test@mail.it | {"hello": "world"} |
    When I send a POST request to "/v1/support" with the given details to send a support request
    Then the response status should be 400
    And the response should contain an error message "The string must be URL-encoded"
