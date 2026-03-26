Feature: ProductMS

  Scenario: GET /v2/product - successfully retrieve origins
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId | prod-test |
    When I send a GET request to "/v2/product"
    Then The status code is 200
    And The response body contains:
      | origins[0].institutionType | PA  |
      | origins[0].labelKey        | pa  |
      | origins[0].origin          | IPA |