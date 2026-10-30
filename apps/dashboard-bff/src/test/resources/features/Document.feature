@document
Feature: Document

  Scenario: Successfully get document status for the requested products
    Given user login with username "j.doe" and password "test"
    And the institution ID is "067327d3-bdd6-408d-8655-87e8f1960046"
    And the products are "prod-io,prod-pagopa,prod-pn"
    When I send a GET request to "/v1/documents/status" to get document status
    Then the response status should be 200
    And The dashboard response body contains:
      | products[0].productId                | prod-io     |
      | products[0].contractUpdateRequired   | true        |
      | products[0].contractUpdateInProgress | false       |
      | products[1].productId                | prod-pagopa |
      | products[1].contractUpdateRequired   | true        |
      | products[1].contractUpdateInProgress | true        |
      | products[2].productId                | prod-pn     |
      | products[2].contractUpdateRequired   | false       |
      | products[2].contractUpdateInProgress | false       |
    And The dashboard response body contains the list "products" of size 3

  Scenario: Attempt to get document status without productIds
    Given user login with username "j.doe" and password "test"
    And the institution ID is "067327d3-bdd6-408d-8655-87e8f1960046"
    When I send a GET request to "/v1/documents/status" to get document status
    Then the response status should be 400
    And the response should contain an error message "Required request parameter 'productIds' for method parameter type List is not present"

  Scenario: Attempt to get document status without permission
    Given user login with username "r.balboa" and password "test"
    And the institution ID is "c9a50656-f345-4c81-84be-5b2474470544"
    And the products are "prod-io"
    When I send a GET request to "/v1/documents/status" to get document status
    Then the response status should be 403

  Scenario: Successfully get the documents of an institution and product
    Given user login with username "j.doe" and password "test"
    And the institution ID is "067327d3-bdd6-408d-8655-87e8f1960046" and the product ID is "prod-pagopa"
    When I send a GET request to "/v1/documents/institutions/{institutionId}/products/{productId}" to get documents list
    Then the response status should be 200
    And The dashboard response body contains at path "documents" the following list of objects in any order:
      | name                                | type        |
      | Piattaforma_pagoPA_accordo_adesione | INSTITUTION |
      | Addendum                            | ATTACHMENT  |

  Scenario: Attempt to get the documents of a product without an active onboarding
    Given user login with username "j.doe" and password "test"
    And the institution ID is "b4705659-3a01-430a-a19b-7bdb4e340223" and the product ID is "prod-pagopa"
    When I send a GET request to "/v1/documents/institutions/{institutionId}/products/{productId}" to get documents list
    Then the response status should be 404

  Scenario: Attempt to get the documents without permission
    Given user login with username "r.balboa" and password "test"
    And the institution ID is "c9a50656-f345-4c81-84be-5b2474470544" and the product ID is "prod-io"
    When I send a GET request to "/v1/documents/institutions/{institutionId}/products/{productId}" to get documents list
    Then the response status should be 403
