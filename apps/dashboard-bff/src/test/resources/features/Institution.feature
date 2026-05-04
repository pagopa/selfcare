@institution
Feature: Institution

  Scenario: Successfully save institution logo by institutionId
    Given user login with username "j.doe" and password "test"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    When I send a PUT request to "/v1/institutions/{institutionId}/logo" to save institutions logo
    Then the response status should be 200

  Scenario: Attempt to save institution logo by institutionId without permissions (not onboarded)
    Given user login with username "r.balboa" and password "test"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    When I send a PUT request to "/v1/institutions/{institutionId}/logo" to save institutions logo
    Then the response status should be 404

  Scenario: Successfully retrieve institution by institutionId
    Given user login with username "j.doe" and password "test"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    When I send a GET request to "/v1/institutions/{institutionId}" to retrieve institution
    Then the response status should be 200
    And The response body contains:
      | id                          | 067327d3-bdd6-408d-8655-87e8f1960046  |
      | externalId                  | 99000870064                           |
      | originId                    | c_d277                                |
      | origin                      | IPA                                   |
      | name                        | comune di dernice                     |
      | category                    | Comuni e loro Consorzi e Associazioni |
      | categoryCode                | L6                                    |
      | fiscalCode                  | 99000870064                           |
      | mailAddress                 | protocollo@pec.comune.dernice.al.it   |
      | address                     | Via Roma N.17                         |
      | zipCode                     | 15056                                 |
      | products[0].origin          | IPA                                   |
      | products[0].originId        | c_d277                                |
      | products[0].productId       | prod-io                               |
      | products[0].institutionType | PSP                                   |
      | products[0].createdAt       | 2022-06-10T13:29:10.462Z              |
    And The response body contains the list "products" of size 4
    # Field institutionType not present if productId not specified in request
    And The response body doesn't contain field "institutionType"

  Scenario: Attempt to retrieve institution by institutionId without permissions (not onboarded)
    Given user login with username "r.balboa" and password "test"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    When I send a GET request to "/v1/institutions/{institutionId}" to retrieve institution
    Then the response status should be 404

  Scenario: Successfully retrieve institution by institutionId v2
    Given user login with username "j.doe" and password "test"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    When I send a GET request to "/v2/institutions/{institutionId}" to retrieve institution
    Then the response status should be 200
    And The response body contains:
      | id                          | 067327d3-bdd6-408d-8655-87e8f1960046  |
      | externalId                  | 99000870064                           |
      | originId                    | c_d277                                |
      | origin                      | IPA                                   |
      | name                        | comune di dernice                     |
      | category                    | Comuni e loro Consorzi e Associazioni |
      | categoryCode                | L6                                    |
      | fiscalCode                  | 99000870064                           |
      | mailAddress                 | protocollo@pec.comune.dernice.al.it   |
      | address                     | Via Roma N.17                         |
      | zipCode                     | 15056                                 |
      | products[0].origin          | IPA                                   |
      | products[0].originId        | c_d277                                |
      | products[0].productId       | prod-io                               |
      | products[0].institutionType | PSP                                   |
      | products[0].tokenId         | string                                |
      | products[0].createdAt       | 2022-06-10T13:29:10.462Z              |
    And The response body contains the list "products" of size 4
    # Field institutionType not present if productId not specified in request
    And The response body doesn't contain field "institutionType"

  Scenario: Unsuccessfully retrieve institution by institutionId v2 with PAGOPA issuer
    Given user login with username "b.king" and password "test"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    When I send a GET request to "/v2/institutions/{institutionId}" to retrieve institution
    Then the response status should be 403

  Scenario: Successfully retrieve all institution by institutionId v2 with PAGOPA issuer with interop permission
    Given user login with username "b.barnes" and password "test"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    When I send a GET request to "/v2/institutions/all/{institutionId}" to retrieve institution
    Then the response status should be 200
    And The response body contains:
      | id                             | 067327d3-bdd6-408d-8655-87e8f1960046                                                                             |
      | externalId                     | 99000870064                                                                                                      |
      | originId                       | c_d277                                                                                                           |
      | origin                         | IPA                                                                                                              |
      | name                           | comune di dernice                                                                                                |
      | category                       | Comuni e loro Consorzi e Associazioni                                                                            |
      | categoryCode                   | L6                                                                                                               |
      | fiscalCode                     | 99000870064                                                                                                      |
      | mailAddress                    | protocollo@pec.comune.dernice.al.it                                                                              |
      | address                        | Via Roma N.17                                                                                                    |
      | zipCode                        | 15056                                                                                                            |
      | products[0].productId          | prod-io                                                                                                          |
      | products[0].authorized         | false                                                                                                            |
      | products[1].productId          | prod-pagopa                                                                                                      |
      | products[1].authorized         | false                                                                                                            |
      | products[2].productId          | prod-pagopa                                                                                                      |
      | products[2].authorized         | false                                                                                                            |
      | products[3].origin             | IPA                                                                                                              |
      | products[3].originId           | c_d277                                                                                                           |
      | products[3].productId          | prod-interop                                                                                                     |
      | products[3].institutionType    | PT                                                                                                               |
      | products[3].userRole           | SUPPORT                                                                                                          |
      | products[3].userProductActions | [read:users, write:users, Selc:AccessProductBackofficeAdmin, Selc:ListAllProductUsers]                           |
    And The response body contains the list "products" of size 4
    # Field institutionType not present if productId not specified in request
    And The response body doesn't contain field "institutionType"
    And The response body doesn't contain field "products[0].userProductActions"
    And The response body doesn't contain field "products[1].userProductActions"
    And The response body doesn't contain field "products[2].userProductActions"

  Scenario: Successfully retrieve all institution by institutionId v2 with PAGOPA issuer with both interop and ALL permissions
    Given user login with username "b.king" and password "test"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    When I send a GET request to "/v2/institutions/all/{institutionId}" to retrieve institution
    Then the response status should be 200
    And The response body contains:
      | id                             | 067327d3-bdd6-408d-8655-87e8f1960046                                                                                |
      | externalId                     | 99000870064                                                                                                         |
      | originId                       | c_d277                                                                                                              |
      | origin                         | IPA                                                                                                                 |
      | name                           | comune di dernice                                                                                                   |
      | category                       | Comuni e loro Consorzi e Associazioni                                                                               |
      | categoryCode                   | L6                                                                                                                  |
      | fiscalCode                     | 99000870064                                                                                                         |
      | mailAddress                    | protocollo@pec.comune.dernice.al.it                                                                                 |
      | address                        | Via Roma N.17                                                                                                       |
      | zipCode                        | 15056                                                                                                               |
      | products[0].productId          | prod-io                                                                                                             |
      | products[0].userRole           | SUPPORT                                                                                                             |
      | products[0].tokenId            | string                                                                                                              |
      | products[0].authorized         | true                                                                                                                |
      | products[0].userProductActions | [read:users, write:users, Selc:AccessProductBackofficeAdmin, Selc:ListAllProductUsers]    |
      | products[1].productId          | prod-pagopa                                                                                                         |
      | products[1].authorized         | false                                                                                                               |
      | products[1].tokenId            | string                                                                                                              |
      | products[2].productId          | prod-pagopa                                                                                                         |
      | products[2].authorized         | true                                                                                                                |
      | products[2].userProductActions | [read:users, write:users, Selc:AccessProductBackofficeAdmin, Selc:ListAllProductUsers]    |
      | products[2].tokenId            | 21f73488-d0df-4a3d-9b6f-9adf634780b5                                                                                |
      | products[3].productId          | prod-interop                                                                                                        |
      | products[3].userRole           | OPERATOR                                                                                                            |
      | products[3].authorized         | true                                                                                                                |
      | products[3].userProductActions | [read:users, Selc:AccessProductBackofficeAdmin]                                                                     |
      | products[3].tokenId            | string                                                                                                              |
    And The response body contains the list "products" of size 4
    # Field institutionType not present if productId not specified in request
    And The response body doesn't contain field "institutionType"
    And The response body doesn't contain field "products[1].userProductActions"

  Scenario: Attempt to retrieve institution by institutionId v2 without permission (not onboarded)
    Given user login with username "r.balboa" and password "test"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    When I send a GET request to "/v2/institutions/{institutionId}" to retrieve institution
    Then the response status should be 404

  Scenario: Attempt to retrieve institution by institutionId v2 without permission (IAM user permission)
    Given user login with username "r.balboa" and password "test"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    When I send a GET request to "/v2/institutions/{institutionId}" to retrieve institution
    Then the response status should be 404

  Scenario: Successfully update institution geo-taxonomy by institutionId
    Given user login with username "j.doe" and password "test"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    And the following geo-taxonomy request details:
      | code   | desc |
      | 058091 | ROMA |
    When I send a PUT request to "/v1/institutions/{institutionId}/geographic-taxonomy" to update institutions geo-taxonomy
    Then the response status should be 200
    And I send a GET request to "/v1/institutions/{institutionId}" to retrieve institution
    And The response body contains:
      | geographicTaxonomies | [[code:058091, desc:ROMA - COMUNE]] |

  Scenario: Attempt to update institution geo-taxonomy by institutionId with invalid request body
    Given user login with username "j.doe" and password "test"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    And the following geo-taxonomy request details:
      | code   | desc |
    When I send a PUT request to "/v1/institutions/{institutionId}/geographic-taxonomy" to update institutions geo-taxonomy
    Then the response status should be 400

  Scenario: Attempt to update institution geo-taxonomy by institutionId without permissions (not onboarded)
    Given user login with username "r.balboa" and password "test"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    And the following geo-taxonomy request details:
      | code   | desc          |
      | 058091 | ROMA - COMUNE |
    When I send a PUT request to "/v1/institutions/{institutionId}/geographic-taxonomy" to update institutions geo-taxonomy
    Then the response status should be 404

  Scenario: Attempt to update institution geo-taxonomy by institutionId without permissions (PAGOPA issuer)
    Given user login with username "b.barnes" and password "test"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    And the following geo-taxonomy request details:
      | code   | desc          |
      | 058091 | ROMA - COMUNE |
    When I send a PUT request to "/v1/institutions/{institutionId}/geographic-taxonomy" to update institutions geo-taxonomy
    Then the response status should be 403

  Scenario: Successfully update institution description by institutionId
    Given user login with username "s.froid" and password "test"
    And the institutionId is "f94c0589-b07e-4ee7-a509-fda5fe91faa2"
    And the following institution description request details:
      | description                   | digitalAddress |
      | COMUNE DI MORANSENGO-TONENGO2 | test@test.it   |
    When I send a PUT request to "/v1/institutions/{institutionId}" to update institution description
    Then the response status should be 200
    And the Institution response should contain an institution id
    And I send a GET request to "/v1/institutions/{institutionId}" to retrieve institution
    And The response body contains:
      | name        | COMUNE DI MORANSENGO-TONENGO2 |
      | mailAddress | test@test.it                  |

  Scenario: Attempt to update institution description by institutionId without permissions (prod different from prod-pn-pg)
    Given user login with username "j.doe" and password "test"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    And the following institution description request details:
      | description        | digitalAddress |
      | comune di dernice2 | test@test.it   |
    When I send a PUT request to "/v1/institutions/{institutionId}" to update institution description
    Then the response status should be 403

  Scenario: Attempt to update institution description by institutionId without permissions (not onboarded)
    Given user login with username "r.balboa" and password "test"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    And the following institution description request details:
      | description        | digitalAddress |
      | comune di dernice2 | test@test.it   |
    When I send a PUT request to "/v1/institutions/{institutionId}" to update institution description
    Then the response status should be 404

  Scenario: Successfully retrieve institution user by institutionId
    Given user login with username "j.doe" and password "test"
    And the userId is "97a511a7-2acc-47b9-afed-2f3c65753b4a"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    When I send a GET request to "/v2/institutions/{institutionId}/users/{userId}" to retrieve institution user
    Then the response status should be 200
    And The response body contains the list "products" of size 4
    And The response body contains:
      | id                                 | 97a511a7-2acc-47b9-afed-2f3c65753b4a |
      | name                               | john                                 |
      | surname                            | Doe                                  |
      | role                               | ADMIN                                |
      | status                             | ACTIVE                               |
      | fiscalCode                         | PRVTNT80A41H401T                     |
      | products[0].id                     | prod-io                              |
      | products[0].roleInfos[0].role      | admin                                |
      | products[0].roleInfos[0].status    | ACTIVE                               |
      | products[0].roleInfos[0].selcRole  | ADMIN                                |
      | products[0].roleInfos[0].partyRole | DELEGATE                             |
      | products[0].roleInfos[0].createdAt | 2024-10-28T12:25:53.228              |
      | products[0].roleInfos[0].updatedAt | 2024-10-28T12:25:53.228              |
      | products[1].id                     | prod-pagopa                          |
      | products[1].roleInfos[0].role      | admin                                |
      | products[1].roleInfos[0].status    | ACTIVE                               |
      | products[1].roleInfos[0].selcRole  | ADMIN                                |
      | products[1].roleInfos[0].partyRole | DELEGATE                             |
      | products[1].roleInfos[0].createdAt | 2024-10-28T12:25:53.228              |
      | products[1].roleInfos[0].updatedAt | 2024-10-28T12:25:53.228              |
      | products[2].id                     | prod-pn                              |
      | products[2].roleInfos[0].role      | admin                                |
      | products[2].roleInfos[0].status    | ACTIVE                               |
      | products[2].roleInfos[0].selcRole  | ADMIN                                |
      | products[2].roleInfos[0].partyRole | DELEGATE                             |
      | products[2].roleInfos[0].createdAt | 2024-10-28T12:25:53.228              |
      | products[2].roleInfos[0].updatedAt | 2024-10-28T12:25:53.228              |
      | products[3].id                     | prod-interop                         |
      | products[3].roleInfos[0].role      | admin                                |
      | products[3].roleInfos[0].status    | ACTIVE                               |
      | products[3].roleInfos[0].selcRole  | ADMIN                                |
      | products[3].roleInfos[0].partyRole | MANAGER                              |
      | products[3].roleInfos[0].createdAt | 2024-10-28T12:25:53.228              |
      | products[3].roleInfos[0].updatedAt | 2024-10-28T12:25:53.228              |

  Scenario: Attempt to retrieve institution user by institutionId without permissions (not onboarded)
    Given user login with username "j.doe" and password "test"
    And the userId is "17a511a7-2acc-47b9-afed-2f3c65853b4a"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    When I send a GET request to "/v2/institutions/{institutionId}/users/{userId}" to retrieve institution user
    Then the response status should be 404

  Scenario: Attempt to retrieve institution user by institutionId without permissions (OPERATOR)
    Given user login with username "j.doe" and password "test"
    And the userId is "35a78332-d038-4bfa-8e85-2cba7f6b7bf7"
    And the institutionId is "467ac77d-7faa-47bf-a60e-38ea74bd5fd2"
    When I send a GET request to "/v2/institutions/{institutionId}/users/{userId}" to retrieve institution user
    Then the response status should be 403

  Scenario: Successfully retrieve institutions
    Given user login with username "j.doe" and password "test"
    When I send a GET request to "/v2/institutions" to retrieve institutions list
    Then the response status should be 200
    And the response should contain institutions list

  Scenario: Successfully retrieve empty institutions list
    Given user login with username "s.froid" and password "test"
    When I send a GET request to "/v2/institutions" to retrieve institutions list
    Then the response status should be 200
    And the response should contain an empty institutions list

  Scenario: Successfully create user product by a user by institutionId
    Given user login with username "j.doe" and password "test"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    And the productId is "prod-pagopa"
    And the following user data request details:
      | name  | surname | taxCode          | email      | role     | productRoles | toAddOnAggregates |
      | Rocky | Balboa  | blbrki80A41H401T | rb@test.it | OPERATOR | operator     | true              |
    When I send a POST request to "/v2/institutions/{institutionId}/products/{productId}/users" to create a new user related to a product for institutions
    Then the response status should be 201

  Scenario: Attempt to create user product by a user by institutionId and with an existent different role
    Given user login with username "j.doe" and password "test"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    And the productId is "prod-io"
    And the following user data request details:
      | name | surname | taxCode          | email      | role     | productRoles        |
      | john | Doe     | PRVTNT80A41H401T | jd@test.it | OPERATOR | referente operativo |
    When I send a POST request to "/v2/institutions/{institutionId}/products/{productId}/users" to create a new user related to a product for institutions
    Then the response status should be 400

  Scenario: Attempt to create user product by a user by institutionId and not allowed productId
    Given user login with username "j.doe" and password "test"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    And the productId is "prod-io"
    And the following user data request details:
      | name | surname | taxCode          | email      | role         | productRoles |
      | john | Doe     | PRVTNT80A41H401T | jd@test.it | SUB_DELEGATE | admin        |
    When I send a POST request to "/v2/institutions/{institutionId}/products/{productId}/users" to create a new user related to a product for institutions
    Then the response status should be 400

  Scenario: Attempt to create user product by an institutionId and productId without permission (not onboarded)
    Given user login with username "j.doe" and password "test"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1970046"
    And the productId is "prod-io"
    And the following user data request details:
      | name | surname | taxCode          | email      | role         | productRoles |
      | john | Doe     | PRVTNT80A41H401T | jd@test.it | SUB_DELEGATE | admin        |
    When I send a POST request to "/v2/institutions/{institutionId}/products/{productId}/users" to create a new user related to a product for institutions
    Then the response status should be 404

  Scenario: Attempt to create user product by institutionId and productId without permissions (OPERATOR)
    Given user login with username "j.doe" and password "test"
    And the institutionId is "467ac77d-7faa-47bf-a60e-38ea74bd5fd2"
    And the productId is "prod-interop"
    And the following user data request details:
      | name | surname | taxCode          | email      | role     | productRoles        |
      | john | Doe     | PRVTNT80A41H401T | jd@test.it | OPERATOR | referente operativo |
    When I send a POST request to "/v2/institutions/{institutionId}/products/{productId}/users" to create a new user related to a product for institutions
    Then the response status should be 403

  Scenario: Attempt to create user product by institutionId and productId with invalid role
    Given user login with username "j.doe" and password "test"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    And the productId is "prod-io"
    And the following user data request details:
      | name | surname | taxCode          | email      | role | productRoles |
      | john | Doe     | PRVTNT80A41H401T | jd@test.it | test | admin        |
    When I send a POST request to "/v2/institutions/{institutionId}/products/{productId}/users" to create a new user related to a product for institutions
    Then the response status should be 400
    And the response should contain an error message "Invalid role: test. Allowed values are: [MANAGER, DELEGATE, SUB_DELEGATE, OPERATOR, ADMIN_EA]"

  Scenario: Attempt to create user product by institutionId and productId with invalid request body
    Given user login with username "j.doe" and password "test"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    And the productId is "prod-io"
    And the following user data request details:
      | name | surname | taxCode | email      | role         | productRoles |
      | john | Doe     |         | jd@test.it | SUB_DELEGATE | admin        |
    When I send a POST request to "/v2/institutions/{institutionId}/products/{productId}/users" to create a new user related to a product for institutions
    Then the response status should be 400
    And the response should contain an error message "taxCode,must not be blank"

  Scenario: Successfully add user by institutionId, productId and userId
    Given user login with username "j.doe" and password "test"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    And the productId is "prod-interop"
    And the userId is "35a78332-d038-4bfa-8e85-2cba7f6b7bf7"
    And the following user product request details:
      | role     | productRoles | toAddOnAggregates |
      | OPERATOR | security     | true              |
    When I send a PUT request to "/v2/institutions/{institutionId}/products/{productId}/users/{userId}" to add a new user related to a product for institutions
    Then the response status should be 201
    And I send a GET request to "/v2/institutions/{institutionId}/users/{userId}" to retrieve institution user
    Then the response status should be 200
    And The response body contains:
      | id         | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7 |
    And The response body contains the list "products" of size 3

  Scenario: Attempt to add user by institutionId, productId and userId without permission
    Given user login with username "j.doe" and password "test"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    And the productId is "prod-interop"
    And the userId is "35a78332-d038-4bfa-8e85-2cba7f6b7bf7"
    And the following user product request details:
      | role     | productRoles |
      | DELEGATE | admin        |
    When I send a PUT request to "/v2/institutions/{institutionId}/products/{productId}/users/{userId}" to add a new user related to a product for institutions
    Then the response status should be 403

  Scenario: Successfully add user by institutionId, productId and userId not allowed product
    Given user login with username "j.doe" and password "test"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    And the productId is "prod-io"
    And the userId is "35a78332-d038-4bfa-8e85-2cba7f6b7bf7"
    And the following user product request details:
      | role     | productRoles |
      | DELEGATE | admin        |
    When I send a PUT request to "/v2/institutions/{institutionId}/products/{productId}/users/{userId}" to add a new user related to a product for institutions
    Then the response status should be 400
    
  Scenario: Attempt to add user with an nonexistent institutionId
    Given user login with username "j.doe" and password "test"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1970046"
    And the productId is "prod-io"
    And the userId is "35a78332-d038-4bfa-8e85-2cba7f6b7bf7"
    And the following user product request details:
      | role     | productRoles |
      | DELEGATE | admin        |
    When I send a PUT request to "/v2/institutions/{institutionId}/products/{productId}/users/{userId}" to add a new user related to a product for institutions
    Then the response status should be 404

  Scenario: Attempt to add user by institutionId, productId and userId with invalid request body
    Given user login with username "j.doe" and password "test"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    And the productId is "prod-io"
    And the userId is "35a78332-d038-4bfa-8e85-2cba7f6b7bf7"
    And the following user product request details:
      | role     | productRoles |
      | DELEGATE |              |
    When I send a PUT request to "/v2/institutions/{institutionId}/products/{productId}/users/{userId}" to add a new user related to a product for institutions
    Then the response status should be 400

  Scenario: Successfully retrieve institution onboardings PENDING
    Given user login with username "j.doe" and password "test"
    And the productId is "prod-interop"
    And the fiscalCode is "08875230016"
    When I send a GET request to "/v2/institutions/onboardings/{productId}/pending" to pending onboardings
    Then the response status should be 200

  Scenario: Attempt to retrieve institution onboardings PENDING not found
    Given user login with username "j.doe" and password "test"
    And the productId is "prod-io"
    And the fiscalCode is "08875230016"
    When I send a GET request to "/v2/institutions/onboardings/{productId}/pending" to pending onboardings
    Then the response status should be 204

  Scenario: Successfully get User Count
    Given user login with username "j.doe" and password "test"
    And the institution ID is "c9a50656-f345-4c81-84be-5b2474470544" and the product ID is "prod-pagopa"
    When I send a GET request to "/v2/institutions/{institutionId}/products/{productId}/users/count" to get users count
    Then the response status should be 200
    And The response body contains:
      | count | 3 |

  Scenario: Attempt to get User Count without permission (OPERATOR)
    Given user login with username "j.doe" and password "test"
    And the institution ID is "467ac77d-7faa-47bf-a60e-38ea74bd5fd2" and the product ID is "prod-interop"
    When I send a GET request to "/v2/institutions/{institutionId}/products/{productId}/users/count" to get users count
    Then the response status should be 403

  Scenario: Attempt to get User Count institution without permission (not onboarded)
    Given user login with username "r.balboa" and password "test"
    And the institution ID is "067327d3-bdd6-408d-8655-87e8f1960046" and the product ID is "prod-interop"
    When I send a GET request to "/v2/institutions/{institutionId}/products/{productId}/users/count" to get users count
    Then the response status should be 404

  Scenario: Successfully get Onboardings Info without products filter
    Given user login with username "j.doe" and password "test"
    And the institution ID is "067327d3-bdd6-408d-8655-87e8f1960046"
    When I send a GET request to "/v2/institutions/{institutionId}/onboardings-info" to get onboardings info
    Then the response status should be 200
    And The response body contains:
      | [0].productId         | prod-io      |
      | [0].status            | ACTIVE       |
      | [0].contractAvailable | true         |
      | [0].institutionType   | PSP          |
      | [0].origin            | IPA          |
      | [0].originId          | c_d277       |
      | [1].productId         | prod-pagopa  |
      | [1].status            | ACTIVE       |
      | [1].contractAvailable | true         |
      | [1].institutionType   | PSP          |
      | [1].origin            | IPA          |
      | [1].originId          | c_d277       |
      | [2].productId         | prod-interop |
      | [2].status            | ACTIVE       |
      | [2].contractAvailable | true         |
      | [2].institutionType   | PT           |
      | [2].origin            | IPA          |
      | [2].originId          | c_d277       |
    And The response body is a list of size 3

  Scenario: Successfully get Onboardings Info with products filter
    Given user login with username "j.doe" and password "test"
    And the institution ID is "067327d3-bdd6-408d-8655-87e8f1960046"
    And the products are "prod-io,prod-pagopa"
    When I send a GET request to "/v2/institutions/{institutionId}/onboardings-info" to get onboardings info
    Then the response status should be 200
    And The response body contains:
    | [0].productId         | prod-io     |
    | [0].status            | ACTIVE      |
    | [0].contractAvailable | true        |
    | [0].institutionType   | PSP         |
    | [0].origin            | IPA         |
    | [0].originId          | c_d277      |
    | [1].productId         | prod-pagopa |
    | [1].status            | ACTIVE      |
    | [1].contractAvailable | true        |
    | [1].institutionType   | PSP         |
    | [1].origin            | IPA         |
    | [1].originId          | c_d277      |
    And The response body is a list of size 2

  Scenario: Attempt to get Onboardings Info without permission
    Given user login with username "r.balboa" and password "test"
    And the institution ID is "c9a50656-f345-4c81-84be-5b2474470544"
    When I send a GET request to "/v2/institutions/{institutionId}/onboardings-info" to get onboardings info
    Then the response status should be 403

  Scenario: Successfully get Contract
    Given user login with username "j.doe" and password "test"
    And the institution ID is "067327d3-bdd6-408d-8655-87e8f1960046" and the product ID is "prod-pagopa"
    When I send a GET request to "/v2/institutions/{institutionId}/contract" to get contract
    Then the response status should be 200

  Scenario: Attempt to get Contract without permission
    Given user login with username "r.balboa" and password "test"
    And the institution ID is "c9a50656-f345-4c81-84be-5b2474470544" and the product ID is "prod-io"
    When I send a GET request to "/v2/institutions/{institutionId}/contract" to get contract
    Then the response status should be 403

  Scenario: Attempt to get a non-available contract
    Given user login with username "r.balboa" and password "test"
    And the institution ID is "467ac77d-7faa-47bf-a60e-38ea74bd5fd2" and the product ID is "prod-pagopa"
    When I send a GET request to "/v2/institutions/{institutionId}/contract" to get contract
    Then the response status should be 404

  Scenario: Successfully check user when user is already onboarded to this institution and product with status ACTIVE
    Given user login with username "j.doe" and password "test"
    And the institution ID is "c9a50656-f345-4c81-84be-5b2474470544" and the product ID is "prod-io"
    And the fiscalCode is "blbrki80A41H401T"
    When I send a POST request to "v2/institutions/{institutionId}/product/{productId}/check-user" to check user from taxCode
    Then the response status should be 200
    And the response of check-user should be "true"

  Scenario: Successfully check user when user is already onboarded to this institution and product with status SUSPENDED
    Given user login with username "j.doe" and password "test"
    And the institution ID is "2a4c94f1-5d11-41f1-89e9-9fef0de4fbfe" and the product ID is "prod-io"
    And the fiscalCode is "blbrki80A41H401T"
    When I send a POST request to "v2/institutions/{institutionId}/product/{productId}/check-user" to check user from taxCode
    Then the response status should be 200
    And the response of check-user should be "true"

  Scenario: Successfully check user when user is not onboarded to this institution and product
    Given user login with username "j.doe" and password "test"
    And the institution ID is "2a4c94f1-5d11-41f1-89e9-9fef0de4fbfe" and the product ID is "prod-interop"
    And the fiscalCode is "blbrki80A41H401T"
    When I send a POST request to "v2/institutions/{institutionId}/product/{productId}/check-user" to check user from taxCode
    Then the response status should be 200
    And the response of check-user should be "false"

  Scenario: Successfully check user when user is not present on pdv
    Given user login with username "j.doe" and password "test"
    And the institution ID is "c9a50656-f345-4c81-84be-5b2474470544" and the product ID is "prod-interop"
    And the fiscalCode is "CCCVTNT80A41H401C"
    When I send a POST request to "v2/institutions/{institutionId}/product/{productId}/check-user" to check user from taxCode
    Then the response status should be 200
    And the response of check-user should be "false"

  Scenario: Attempt to check user without permission
    Given user login with username "r.balboa" and password "test"
    And the institution ID is "c9a50656-f345-4c81-84be-5b2474470544" and the product ID is "prod-io"
    And the fiscalCode is "blbrki80A41H401T"
    When I send a POST request to "v2/institutions/{institutionId}/product/{productId}/check-user" to check user from taxCode
    Then the response status should be 403

  Scenario: Successfully check attachment status when attachment is already uploaded
    Given user login with username "j.doe" and password "test"
    And the institution ID is "067327d3-bdd6-408d-8655-87e8f1960046" and the product ID is "prod-pagopa"
    And the attachmentName is "Dichiarazione_sostitutiva_certificazione"
    When I send a GET request to "v2/institutions/{institutionId}/product/{productId}/attachment/status" to check attachment status from attachment name
    Then the response status should be 200
    And the response of check-attachment should be "true"

  Scenario: Successfully check attachment status when attachment is not uploaded
    Given user login with username "j.doe" and password "test"
    And the institution ID is "c9a50656-f345-4c81-84be-5b2474470544" and the product ID is "prod-pagopa"
    And the attachmentName is "Dichiarazione_sostitutiva_certificazione"
    When I send a GET request to "v2/institutions/{institutionId}/product/{productId}/attachment/status" to check attachment status from attachment name
    Then the response status should be 200
    And the response of check-attachment should be "false"

  Scenario: Attempt to attachment status without permission
    Given user login with username "r.balboa" and password "test"
    And the institution ID is "c9a50656-f345-4c81-84be-5b2474470544" and the product ID is "prod-io"
    And the attachmentName is "Dichiarazione_sostitutiva_certificazione"
    When I send a GET request to "v2/institutions/{institutionId}/product/{productId}/attachment/status" to check attachment status from attachment name
    Then the response status should be 403