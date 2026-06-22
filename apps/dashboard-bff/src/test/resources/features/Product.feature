@product
Feature: Product

  Scenario: Successfully retrieving the back-office URL for a valid product and institution
    Given user login with username "j.doe" and password "test"
    And the productId is "prod-pn"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    When I send a GET request to "/v2/products/{productId}/back-office" to retrieve back-office URL
    Then the response status should be 200
    And the response should contain a back-office URL with selfcare token


  Scenario: Retrieving the back-office URL for a product without institutionId
    Given user login with username "j.doe" and password "test"
    And the productId is "prod-interop"
    When I send a GET request to "/v2/products/{productId}/back-office" to retrieve back-office URL
    Then the response status should be 400
    And the response should contain an error message "Required request parameter 'institutionId' for method parameter type String is not present"

  Scenario: Retrieving the back-office URL for a product with lang parameter
    Given user login with username "j.doe" and password "test"
    And the productId is "prod-pn"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    And the language is "de"
    When I send a GET request to "/v2/products/{productId}/back-office" to retrieve back-office URL
    Then the response status should be 200
    And the response should contain a back-office URL with selfcare token
    And the response should contain a "de" query param for language

  Scenario: Retrieving the back-office URL with an invalid environment parameter
    Given user login with username "j.doe" and password "test"
    And the productId is "prod-interop"
    And the institutionId is "467ac77d-7faa-47bf-a60e-38ea74bd5fd2"
    And the environment is "invalidEnv"
    When I send a GET request to "/v2/products/{productId}/back-office" to retrieve back-office URL
    Then the response status should be 500
    And the response should contain an error message "Invalid Request"

  Scenario: Retrieving the back-office URL for a product and institution the user does not have permission to access
    Given user login with username "r.balboa" and password "test"
    And the productId is "prod-io"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    When I send a GET request to "/v2/products/{productId}/back-office" to retrieve back-office URL
    Then the response status should be 404

  Scenario: Successfully retrieving the product roles for given productId and institutionType
    Given user login with username "j.doe" and password "test"
    And the productId is "prod-pagopa"
    And the institutionType is "PSP"
    When I send a GET request to "/v1/products/{productId}/roles" to retrieve productRoles
    Then the response status should be 200
    And the response should contain a list of product roles
    And The dashboard response body contains the list "" of size 5
    And The dashboard response body contains at path "" the following list of objects in any order:
      | partyRole    | selcRole | phasesAdditionAllowed           |
      | OPERATOR     | LIMITED  | ["dashboard"]                   |
      | MANAGER      | ADMIN    | ["onboarding"]                  |
      | DELEGATE     | ADMIN    | ["onboarding"]                  |
      | SUB_DELEGATE | ADMIN    | ["dashboard-async"]             |
      | ADMIN_EA     | ADMIN    | ["onboarding"]                  |
    And The dashboard response body contains at path "find { it.partyRole == 'OPERATOR' }.productRoles" the following list of objects in any order:
      | code           | label              | description                                                       |
      | operator-psp   | Operatore          | Gestisce l’integrazione tecnologica e/o l'operatività dei servizi |
    And The dashboard response body doesn't contain field "find { it.partyRole == 'OPERATOR' }.multiroleAllowed"
    And The dashboard response body contains at path "find { it.partyRole == 'DELEGATE' }.productRoles" the following list of objects in any order:
      | code           | label              | description                                                       |
      | admin-psp      | Amministratore     | Ha tutti i permessi e gestisce gli utenti                         |
    And The dashboard response body doesn't contain field "find { it.partyRole == 'DELEGATE' }.multiroleAllowed"
    And The dashboard response body contains at path "find { it.partyRole == 'ADMIN_EA' }.productRoles" the following list of objects in any order:
      | code           | label              | description                                                       |
      | admin-psp      | Amministratore     | Ha tutti i permessi e gestisce gli utenti                         |
    And The dashboard response body doesn't contain field "find { it.partyRole == 'ADMIN_EA' }.multiroleAllowed"
    And The dashboard response body contains at path "find { it.partyRole == 'SUB_DELEGATE' }.productRoles" the following list of objects in any order:
      | code           | label              | description                                                             |
      | admin-psp      | Amministratore     | Ha tutti i permessi e gestisce gli utenti. Possono essere al massimo 4. |
    And The dashboard response body doesn't contain field "find { it.partyRole == 'SUB_DELEGATE' }.multiroleAllowed"
    And The dashboard response body contains at path "find { it.partyRole == 'MANAGER' }.productRoles" the following list of objects in any order:
      | code           | label              | description                                                       |
      | admin-psp      | Amministratore     | Stipula il contratto e identifica gli amministratori              |
    And The dashboard response body doesn't contain field "find { it.partyRole == 'MANAGER' }.multiroleAllowed"

  Scenario: Successfully retrieving the product roles for given productId
    Given user login with username "j.doe" and password "test"
    And the productId is "prod-pagopa"
    When I send a GET request to "/v1/products/{productId}/roles" to retrieve productRoles
    Then the response status should be 200
    And the response should contain a list of product roles
    And The dashboard response body contains the list "" of size 5
    And The dashboard response body contains at path "" the following list of objects in any order:
      | partyRole    | selcRole | phasesAdditionAllowed           |
      | OPERATOR     | LIMITED  | ["dashboard"]                   |
      | MANAGER      | ADMIN    | ["onboarding"]                  |
      | DELEGATE     | ADMIN    | ["onboarding"]                  |
      | SUB_DELEGATE | ADMIN    | ["dashboard"]                   |
      | ADMIN_EA     | ADMIN    | ["onboarding"]                  |
    And The dashboard response body contains at path "find { it.partyRole == 'OPERATOR' }.productRoles" the following list of objects in any order:
      | code           | label              | description                                                       |
      | operator       | Operatore          | Gestisce l’integrazione tecnologica e/o l'operatività dei servizi |
    And The dashboard response body doesn't contain field "find { it.partyRole == 'OPERATOR' }.multiroleAllowed"
    And The dashboard response body contains at path "find { it.partyRole == 'DELEGATE' }.productRoles" the following list of objects in any order:
      | code           | label                   | description                                                       |
      | admin          | Referente dei Pagamenti | Ha tutti i permessi e gestisce gli utenti                         |
    And The dashboard response body doesn't contain field "find { it.partyRole == 'DELEGATE' }.multiroleAllowed"
    And The dashboard response body contains at path "find { it.partyRole == 'ADMIN_EA' }.productRoles" the following list of objects in any order:
      | code           | label                   | description                                                       |
      | admin          | Referente dei Pagamenti | Ha tutti i permessi e gestisce gli utenti                         |
    And The dashboard response body doesn't contain field "find { it.partyRole == 'ADMIN_EA' }.multiroleAllowed"
    And The dashboard response body contains at path "find { it.partyRole == 'SUB_DELEGATE' }.productRoles" the following list of objects in any order:
      | code           | label                   | description                                                       |
      | admin          | Referente dei Pagamenti | Ha tutti i permessi e gestisce gli utenti                         |
    And The dashboard response body doesn't contain field "find { it.partyRole == 'SUB_DELEGATE' }.multiroleAllowed"
    And The dashboard response body contains at path "find { it.partyRole == 'MANAGER' }.productRoles" the following list of objects in any order:
      | code           | label                   | description                                                       |
      | admin          | Referente dei Pagamenti | Stipula il contratto e identifica gli amministratori              |
    And The dashboard response body doesn't contain field "find { it.partyRole == 'MANAGER' }.multiroleAllowed"

  Scenario: Successfully retrieving the product roles for given productId for prod-interop
    Given user login with username "j.doe" and password "test"
    And the productId is "prod-interop"
    When I send a GET request to "/v1/products/{productId}/roles" to retrieve productRoles
    Then the response status should be 200
    And The dashboard response body contains the list "" of size 4
    And The dashboard response body contains at path "" the following list of objects in any order:
      | partyRole    | selcRole | phasesAdditionAllowed           |
      | OPERATOR     | LIMITED  | ["dashboard"]                   |
      | MANAGER      | ADMIN    | ["onboarding"]                  |
      | DELEGATE     | ADMIN    | ["onboarding"]                  |
      | SUB_DELEGATE | ADMIN    | ["dashboard-async"]             |
    And The dashboard response body contains at path "find { it.partyRole == 'OPERATOR' }.productRoles" the following list of objects in any order:
      | code          | label               | description                                              | multiroleGroups |
      | api           | Operatore API       | Gestisce il ciclo di vita degli e-service                | ["group1"]      |
      | security      | Operatore Sicurezza | Gestisce il ciclo di vita dei client di connessione      | ["group1"]      |
    And The dashboard response body doesn't contain field "find { it.partyRole == 'OPERATOR' }.multiroleAllowed"
    And The dashboard response body contains at path "find { it.partyRole == 'DELEGATE' }.productRoles" the following list of objects in any order:
      | code           | label              | description                                                       |
      | admin          | Amministratore     | Ha tutti i permessi e gestisce gli utenti                         |
    And The dashboard response body doesn't contain field "find { it.partyRole == 'DELEGATE' }.multiroleAllowed"
    And The dashboard response body contains at path "find { it.partyRole == 'SUB_DELEGATE' }.productRoles" the following list of objects in any order:
      | code           | label              | description                                                       |
      | admin          | Amministratore     | Ha tutti i permessi e gestisce gli utenti                         |
    And The dashboard response body doesn't contain field "find { it.partyRole == 'SUB_DELEGATE' }.multiroleAllowed"
    And The dashboard response body contains at path "find { it.partyRole == 'MANAGER' }.productRoles" the following list of objects in any order:
      | code           | label              | description                                                       |
      | admin          | Amministratore     | Stipula il contratto e identifica gli amministratori              |
    And The dashboard response body doesn't contain field "find { it.partyRole == 'MANAGER' }.multiroleAllowed"

  Scenario: Successfully retrieving the product brokers for product PagoPA and institutionType PSP found
    Given user login with username "j.doe" and password "test"
    And the productId is "prod-pagopa"
    And the institutionType is "PSP"
    When I send a GET request to "/v1/products/{productId}/brokers/{institutionType}" to retrieve product brokers
    Then the response status should be 200
    And the response should contain a list of pagopa psp product brokers

  Scenario: Successfully retrieving the product brokers for product PagoPA and institutionType PA found
    Given user login with username "j.doe" and password "test"
    And the productId is "prod-pagopa"
    And the institutionType is "PA"
    When I send a GET request to "/v1/products/{productId}/brokers/{institutionType}" to retrieve product brokers
    Then the response status should be 200
    And the response should contain a list of pagopa product brokers

  Scenario: Successfully retrieving the product brokers for product PagoPA and institutionType PSP not found
    Given user login with username "j.doe" and password "test"
    And the productId is "prod-pagopa"
    And the institutionType is "PSP"
    When I send a GET request to "/v1/products/{productId}/brokers/{institutionType}" to retrieve product brokers
    Then the response status should be 200
    And the response should not contain any product brokers

  Scenario: Successfully retrieving the product brokers for product PagoPA and institutionType PA not found
    Given user login with username "j.doe" and password "test"
    And the productId is "prod-pagopa"
    And the institutionType is "PA"
    When I send a GET request to "/v1/products/{productId}/brokers/{institutionType}" to retrieve product brokers
    Then the response status should be 200
    And the response should not contain any product brokers

  Scenario: Successfully retrieving the product brokers for product interop and given institutionType found
    Given user login with username "j.doe" and password "test"
    And the productId is "prod-interop"
    And the institutionType is "PSP"
    When I send a GET request to "/v1/products/{productId}/brokers/{institutionType}" to retrieve product brokers
    Then the response status should be 200
    And the response should contain a list of product brokers

  Scenario: Successfully retrieving the product brokers for product interop and given institutionType not found
    Given user login with username "j.doe" and password "test"
    And the productId is "prod-io-premium"
    And the institutionType is "PA"
    When I send a GET request to "/v1/products/{productId}/brokers/{institutionType}" to retrieve product brokers
    Then the response status should be 200
    And the response should not contain any product brokers

  Scenario: Successfully retrieving products tree
    Given user login with username "j.doe" and password "test"
    When I send a GET request to "/v1/institutions/products" to retrieve products tree
    Then the response status should be 200

  Scenario: Successfully retrieving my permissions
    Given user login with username "b.king" and password "test"
    When I send a GET request to "/v1/products/my-permissions" to retrieve my permissions
    Then the response status should be 200
    And The dashboard response body contains the list "items" of size 2
    And The dashboard response body contains:
      | items[0].productId   | prod-interop                                                                                                      |
      | items[0].role        | OPERATOR                                                                                                          |
      | items[0].group       | operator                                                                                                          |
      | items[0].permissions | [read:users, Selc:AccessProductBackofficeAdmin]                                                                   |
      | items[1].productId   | ALL                                                                                                               |
      | items[1].role        | SUPPORT                                                                                                           |
      | items[1].permissions | [read:users, write:users, Selc:AccessProductBackofficeAdmin, Selc:ListAllProductUsers, Selc:ListAllProductGroups] |

  Scenario: Unsuccessfully retrieving my permissions without ARB permission
    Given user login with username "j.doe" and password "test"
    When I send a GET request to "/v1/products/my-permissions" to retrieve my permissions
    Then the response status should be 403