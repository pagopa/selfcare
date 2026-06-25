@delegation
Feature: Delegation

  Scenario: Attempt to createDelegation without permissions
    Given user login with username "r.balboa" and password "test"
    And the following delegation request details:
      | from                                 | to                                    | productId          | type | institutionToName | institutionFromName  |
      | c9a50656-f345-4c81-84be-5b2474470544 | 067327d3-bdd6-408d-8655-87e8f1960046  | prod-io-premium    | PT   | PT test           | Comune di Castelbuono|
    When I send a POST request to "/v1/delegations" to create a delegation
    Then the response status should be 403

  Scenario: Successfully creating a delegation with valid data
    Given user login with username "j.doe" and password "test"
    And the following delegation request details:
      | from                                 | to                                    | productId  | type | institutionToName | institutionFromName  |
      | c9a50656-f345-4c81-84be-5b2474470544 | 067327d3-bdd6-408d-8655-87e8f1960046  | prod-io    | AOO  | AOO test          | Comune di Castelbuono|
    When I send a POST request to "/v1/delegations" to create a delegation
    Then the response status should be 201
    And the response should contain a delegation id

  Scenario: Creating a delegation with a missing "to" field
    Given user login with username "j.doe" and password "test"
    And the following delegation request details:
      | from                                  | productId  | type | institutionToName | institutionFromName  |
      | c9a50656-f345-4c81-84be-5b2474470544  | prod-io    | PT   | PT test           | Comune di Castelbuono|
    When I send a POST request to "/v1/delegations" to create a delegation
    Then the response status should be 400
    And the response should contain an error message "delegationRequestDto.to,must not be blank"

  Scenario: Creating a delegation with a missing "from" field
    Given user login with username "j.doe" and password "test"
    And the following delegation request details:
       | to                                    | productId  | type | institutionToName | institutionFromName  |
       | 067327d3-bdd6-408d-8655-87e8f1960046  | prod-io    | PT   | PT test           | Comune di Castelbuono|
    When I send a POST request to "/v1/delegations" to create a delegation
    Then the response status should be 400
    And the response should contain an error message "delegationRequestDto.from,must not be blank"

  Scenario: Creating a delegation with a missing "fromName" field
    Given user login with username "j.doe" and password "test"
    And the following delegation request details:
      | from                                 | to                                    | productId  | type | institutionToName |
      | c9a50656-f345-4c81-84be-5b2474470544 | 067327d3-bdd6-408d-8655-87e8f1960046  | prod-io    | PT   | PT test           |
    When I send a POST request to "/v1/delegations" to create a delegation
    Then the response status should be 400
    And the response should contain an error message "delegationRequestDto.institutionFromName,must not be blank"


  Scenario: Creating a delegation with a missing "toName" field
    Given user login with username "j.doe" and password "test"
    And the following delegation request details:
      | from                                 | to                                    | productId  | type  | institutionFromName  |
      | c9a50656-f345-4c81-84be-5b2474470544 | 067327d3-bdd6-408d-8655-87e8f1960046  | prod-io    | PT    | Comune di Castelbuono|
    When I send a POST request to "/v1/delegations" to create a delegation
    Then the response status should be 400
    And the response should contain an error message "delegationRequestDto.institutionToName,must not be blank"

  Scenario: Creating a delegation for the product "prod-pagopa" with an invalid "to" field (non-existent tax code)
    Given user login with username "j.doe" and password "test"
    And the following delegation request details:
      | from                                 | to          | productId  | type | institutionToName | institutionFromName  |
      | c9a50656-f345-4c81-84be-5b2474470544 | 00000000000 | prod-io    | PT   | PT test           | Comune di Castelbuono|
    When I send a POST request to "/v1/delegations" to create a delegation
    Then the response status should be 404

  Scenario: Creating a delegation for the product "prod-pagopa" and setting the institution from the tax code
    Given user login with username "j.doe" and password "test"
    And the following delegation request details:
      | from                                 | to           | productId    | type | institutionToName | institutionFromName  |
      | c9a50656-f345-4c81-84be-5b2474470544 | 99000870064  | prod-pagopa  | AOO  | AOO test          | Comune di Castelbuono|
    When I send a POST request to "/v1/delegations" to create a delegation
    Then the response status should be 201
    And the response should contain a delegation id

  Scenario: Creating a delegation with a conflict (duplicate delegation)
    Given user login with username "j.doe" and password "test"
    And the following delegation request details:
      | from                                 | to                                    | productId  | type | institutionToName | institutionFromName  |
      | c9a50656-f345-4c81-84be-5b2474470544 | 067327d3-bdd6-408d-8655-87e8f1960046  | prod-io    | AOO  | AOO test           | Comune di Castelbuono|
    When I send a POST request to "/v1/delegations" to create a delegation
    Then the response status should be 409

  Scenario: Attempt to retrieve delegation with delegator with role Operator
    Given user login with username "r.balboa" and password "test"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    When I send a GET request to "/v1/institutions/{institutionId}/partners" to retrieve delegations
    Then the response status should be 403

  Scenario: Successfully retrieve delegation using delegator without filter for productId
    Given user login with username "j.doe" and password "test"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    When I send a GET request to "/v1/institutions/{institutionId}/partners" to retrieve delegations
    Then the response status should be 200
    And the response should contain a list of delegations for all product

  Scenario: Successfully retrieve delegation using delegator with filter for productId
    Given user login with username "j.doe" and password "test"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    And the productId is "prod-io"
    When I send a GET request to "/v1/institutions/{institutionId}/partners" to retrieve delegations
    Then the response status should be 200
    And the response should contain a list of delegations only for "prod-io"

  Scenario: Attempt to retrieve delegation using delegator with filter for productId nothing found
    Given user login with username "j.doe" and password "test"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    And the productId is "prod-pn"
    When I send a GET request to "/v1/institutions/{institutionId}/partners" to retrieve delegations
    Then the response status should be 200
    And the response should contain an empty list

  Scenario: Successfully retrieve delegation using delegate without filter for productId
    Given user login with username "j.doe" and password "test"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    When I send a GET request to "/v1/institutions/{institutionId}/institutions" to retrieve delegations
    Then the response status should be 200
    And the response should contain a list of delegations for all product

  Scenario: Attempt to retrieve delegation with delegate without permissions
    Given user login with username "r.balboa" and password "test"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    When I send a GET request to "/v1/institutions/{institutionId}/institutions" to retrieve delegations
    Then the response status should be 404

  Scenario: Successfully retrieve delegation using delegate with filter for productId
    Given user login with username "j.doe" and password "test"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    And the productId is "prod-io"
    When I send a GET request to "/v1/institutions/{institutionId}/institutions" to retrieve delegations
    Then the response status should be 200
    And the response should contain a list of delegations only for "prod-io"

  Scenario: Attempt to retrieve delegation using delegate with filter for productId nothing found
    Given user login with username "j.doe" and password "test"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    And the productId is "prod-pn"
    When I send a GET request to "/v1/institutions/{institutionId}/institutions" to retrieve delegations
    Then the response status should be 200
    And the response should contain an empty list

  Scenario: Successfully retrieve paginated delegation using delegate without filter for productId
    Given user login with username "j.doe" and password "test"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    When I send a GET request to "/v2/institutions/{institutionId}/institutions" to retrieve paginated delegations
    Then the response status should be 200
    And the response should contain a list of paginated delegations

  Scenario: Attempt to retrieve paginated delegation with delegate without permissions
    Given user login with username "r.balboa" and password "test"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    When I send a GET request to "/v2/institutions/{institutionId}/institutions" to retrieve paginated delegations
    Then the response status should be 404

  Scenario: Successfully retrieve paginated delegation using delegate with search and pageable filter
    Given user login with username "j.doe" and password "test"
    And I have "Comune di Castelbuono" as search filter
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    And require page 0 and size 1 and order by "ASC"
    When I send a GET request to "/v2/institutions/{institutionId}/institutions" to retrieve paginated delegations
    Then the response status should be 200
    And the response should contain a list of paginated delegations with other pages

  Scenario: Successfully retrieve paginated delegation using delegate with filter for productId
    Given user login with username "j.doe" and password "test"
    And the productId is "prod-io"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    When I send a GET request to "/v2/institutions/{institutionId}/institutions" to retrieve paginated delegations
    Then the response status should be 200
    And the paginated response contains delegation only for "prod-io"

  Scenario: Attempt to retrieve paginated delegation using delegate with filter for productId nothing found
    Given user login with username "j.doe" and password "test"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    And the productId is "prod-pn"
    When I send a GET request to "/v2/institutions/{institutionId}/institutions" to retrieve paginated delegations
    Then the response status should be 200
    And the response should contain an empty list for paginatedApi
