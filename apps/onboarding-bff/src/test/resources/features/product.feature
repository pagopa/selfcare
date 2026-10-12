Feature: Product

  Scenario: Success in getProducts
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "/v1/products"
    Then The status code is 200
    And The response body contains the list "status" of size 14

  Scenario: Success in getProducts for admin
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "/v1/products/admin"
    Then The status code is 200
    And The response body contains the list "status" of size 5

  Scenario: Success in getProducts by id
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "/v1/product/prod-pagopa"
    Then The status code is 200
    And The response body contains:
      | id     | prod-pagopa      |
      | title  | Pagamenti pagoPA |
      | status | ACTIVE           |

  Scenario: Failed case in getProducts by wrong id
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "/v1/product/prod-pago"
    Then The status code is 404