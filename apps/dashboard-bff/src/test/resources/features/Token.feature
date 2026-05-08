@token
Feature: Token API

  ##token exchange
  Scenario: Successful token exchange with valid institution and product IDs
    Given user login with username "j.doe" and password "test"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    And the productId is "prod-interop"
    When I send a GET request to "/v2/token/exchange" with the given details to retrieve token
    Then the response status should be 200
    And the response should contain a valid token

  Scenario: Attempt to token exchange without institution ID
    Given user login with username "j.doe" and password "test"
    And the productId is "prod-interop"
    When I send a GET request to "/v2/token/exchange" with the given details to retrieve token
    Then the response status should be 400
    And the response should contain an error message "Required request parameter 'institutionId' for method parameter type String is not present"

  Scenario: Attempt to token exchange without productId
    Given user login with username "j.doe" and password "test"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    When I send a GET request to "/v2/token/exchange" with the given details to retrieve token
    Then the response status should be 400
    And the response should contain an error message "Required request parameter 'productId' for method parameter type String is not present"

  Scenario: Attempt to retrieve token without user permissions on product
    Given user login with username "r.balboa" and password "test"
    And the institutionId is "467ac77d-7faa-47bf-a60e-38ea74bd5fd2"
    And the productId is "prod-interop"
    When I send a GET request to "/v2/token/exchange" with the given details to retrieve token
    Then the response status should be 404

  ## backoffice admin token exchange
  Scenario: Successful backoffice admin token exchange with valid institution and product IDs
    Given user login with username "b.barnes" and password "test"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    And the productId is "prod-interop"
    When I send a GET request to "/v2/token/exchange/back-office/admin" with the given details to retrieve back-office admin URL
    Then the response status should be 200
    And the response should contain a valid backoffice admin token

  Scenario: Successful backoffice admin token exchange with valid institution, product IDs and with lang parameter
    Given user login with username "b.barnes" and password "test"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    And the productId is "prod-interop"
    And the language is "de"
    When I send a GET request to "/v2/token/exchange/back-office/admin" with the given details to retrieve back-office admin URL
    Then the response status should be 200
    And the response should contain a valid backoffice admin token
    And the response should contain a "de" query param for language

  Scenario: Successful backoffice admin token exchange with valid institution, product IDs and with non default group
    Given user login with username "b.king" and password "test"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    And the productId is "prod-interop"
    When I send a GET request to "/v2/token/exchange/back-office/admin" with the given details to retrieve back-office admin URL
    Then the response status should be 200
    And the response should contain a valid backoffice admin token with non default group

  Scenario: Attempt to retrieve backoffice admin token without permission (IAM user permission)
    Given user login with username "b.barnes" and password "test"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    And the productId is "product-A"
    When I send a GET request to "/v2/token/exchange/back-office/admin" with the given details to retrieve back-office admin URL
    Then the response status should be 403

  Scenario: Attempt to backoffice admin token exchange without institution ID
    Given user login with username "j.doe" and password "test"
    And the productId is "prod-interop"
    When I send a GET request to "/v2/token/exchange/back-office/admin" with the given details to retrieve back-office admin URL
    Then the response status should be 400
    And the response should contain an error message "Required request parameter 'institutionId' for method parameter type String is not present"

  Scenario: Attempt to backoffice admin token exchange without productId
    Given user login with username "j.doe" and password "test"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    When I send a GET request to "/v2/token/exchange/back-office/admin" with the given details to retrieve back-office admin URL
    Then the response status should be 400
    And the response should contain an error message "Required request parameter 'productId' for method parameter type String is not present"

  ##billing token exchange
  Scenario: Successful billing token exchange with valid institution ID and environment
    Given user login with username "j.doe" and password "test"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    When I send a GET request to "/v2/token/exchange/fatturazione" with the given details to retrieve billing token
    Then the response status should be 200
    And the response should contain a valid billing token

  Scenario: Attempt to retrieve billing token without institution ID
    Given user login with username "j.doe" and password "test"
    When I send a GET request to "/v2/token/exchange/fatturazione" with the given details to retrieve billing token
    Then the response status should be 400
    And the response should contain an error message "Required request parameter 'institutionId' for method parameter type String is not present"



