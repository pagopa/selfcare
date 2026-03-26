Feature: Institution

  Scenario: Success to complete onboarding
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
       {
        "assistanceContacts": {
          "supportEmail": "supporto@esempio.it"
        },
        "billingData": {
          "businessName": "Azienda Fittizia S.p.A.",
          "digitalAddress": "azienda.fittizia@pec.it",
          "publicServices": false,
          "recipientCode": "ABC1234",
          "registeredOffice": "Via Roma 123, 84100 Salerno SA",
          "taxCode": "35843432474",
          "taxCodeInvoicing": "12345678901",
          "vatNumber": "IT01234567890",
          "zipCode": "84100"
        },
        "geographicTaxonomies": [
          {
            "code": "084",
            "desc": "Salerno"
          }
        ],
        "institutionLocationData": {
          "city": "Salerno",
          "country": "Italia",
          "county": "Salerno"
        },
        "institutionType": "PRV",
        "isAggregator": false,
        "origin": "SELC",
        "originId": "35843432474",
        "productId": "prod-pagopa",
        "taxCode": "35843432474",
        "users": [
          {
            "email": "mario.rossi@esempio.it",
            "name": "Mario",
            "role": "ADMIN_EA",
            "surname": "Rossi",
            "taxCode": "RSSMRA80A01H703Z"
          }
        ]
      }
    """
    When I send a POST request to "/v1/institutions/onboarding"
    Then The status code is 201

  Scenario: Failed to complete onboarding
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
       {
        "assistanceContacts": {
          "supportEmail": "supporto@esempio.it"
        },
        "billingData": {
          "businessName": "Azienda Fittizia S.p.A.",
          "digitalAddress": "azienda.fittizia@pec.it",
          "publicServices": false,
          "recipientCode": "ABC1234",
          "registeredOffice": "Via Roma 123, 84100 Salerno SA",
          "taxCode": "35843432474",
          "taxCodeInvoicing": "12345678901",
          "vatNumber": "IT01234567890",
          "zipCode": "84100"
        },
        "geographicTaxonomies": [
          {
            "code": "084",
            "desc": "Salerno"
          }
        ],
        "institutionLocationData": {
          "city": "Salerno",
          "country": "Italia",
          "county": "Salerno"
        },
        "institutionType": "PRV",
        "isAggregator": false,
        "origin": "SELC",
        "originId": "35843432474",
        "productId": "prod-pagopa",
        "taxCode": "35843432474",
        "users": [
          {
            "email": "mario.rossi@esempio.it",
            "name": "Example",
            "role": "ADMIN_EA",
            "surname": "Exampleee",
            "taxCode": "RSSMRA80A01H703Z"
          }
        ]
      }
    """
    When I send a POST request to "/v1/institutions/onboarding"
    Then The status code is 409

  Scenario: Success to retrieve geotaxonomy
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId | test1235 |
    When I send a GET request to "/v1/institutions/{institutionId}/geographic-taxonomy"
    Then The status code is 200

  Scenario: Failed to retrieve geotaxonomy
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId | 123 |
    When I send a GET request to "/v1/institutions/{institutionId}/geographic-taxonomy"
    Then The status code is 404

  Scenario: Success to retrieve geotaxonomy by filters
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | taxCode | 35843432474 |
    When I send a GET request to "/v1/institutions/geographic-taxonomies"
    Then The status code is 200

  Scenario: Failed to retrieve geotaxonomy by filters
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | taxCode | test1235 |
    When I send a GET request to "/v1/institutions/geographic-taxonomies"
    Then The status code is 404

  Scenario: Success to retrieve institutions
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId | prod-io |
    When I send a GET request to "/v1/institutions"
    Then The status code is 200

  Scenario: Failed to retrieve institutions
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId | prod-test |
    When I send a GET request to "/v1/institutions"
    Then The status code is 404

  Scenario: Success to verify onboarding
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | externalInstitutionId | test-io |
      | productId             | prod-io |
    When I send a HEAD request to "/v1/institutions/{externalInstitutionId}/products/{productId}"
    Then The status code is 204

  Scenario: Failed to verify onboarding
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | externalInstitutionId | test-io   |
      | productId             | prod-test-1 |
    When I send a HEAD request to "/v1/institutions/{externalInstitutionId}/products/{productId}"
    Then The status code is 404

  Scenario: Failed to verify onboarding when product is not enabled for institution
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | externalInstitutionId | test-io   |
      | productId             | test-product-2 |
    When I send a HEAD request to "/v1/institutions/{externalInstitutionId}/products/{productId}"
    Then The status code is 403

  Scenario: Success to verify onboarding
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | taxCode   | 11223345667 |
      | origin    | INFOCAMERE  |
      | originId  | 11223345667 |
      | productId | prod-io     |
    When I send a HEAD request to "/v1/institutions/onboarding"
    Then The status code is 204


  Scenario: Failed to verify onboarding when missing
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | taxCode   | 11223345669 |
      | origin    | INFOCAMERE  |
      | originId  | 11223345667 |
      | productId | prod-io     |
    When I send a HEAD request to "/v1/institutions/onboarding"
    Then The status code is 404

  Scenario: Success to get institution from infocamere
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "/v1/institutions/from-infocamere/"
    Then The status code is 200

  Scenario: Failed to get institution from infocamere
    Given User login with username "r.balboa" and password "test"
    When I send a GET request to "/v1/institutions/from-infocamere/"
    Then The status code is 404

  Scenario: Success to get verify match by institutionTaxCode and user info
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
      {
       "taxCode": "11223345665",
        "userDto" : {
              "email": "mario.rossi@esempio.it",
              "name": "Mario",
              "role": "ADMIN_EA",
              "surname": "Rossi",
              "taxCode": "RSSMRA80A01H703Z"
         }
       }
     """
    When I send a POST request to "/v1/institutions/verification/match"
    Then The status code is 200


  Scenario: Failed to get verify match by institutionTaxCode and user info
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
      {
       "taxCode": "11223345666",
        "userDto" : {
              "email": "mario.rossi@esempio.it",
              "name": "Mario",
              "role": "ADMIN_EA",
              "surname": "Rossi",
              "taxCode": "RSSMRA80A01H703Z"
         }
       }
     """
    When I send a POST request to "/v1/institutions/verification/match"
    Then The status code is 404

  Scenario: Success to get verify legal-address
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
      {
       "taxCode": "11223345687"
       }
    """
    When I send a POST request to "/v1/institutions/verification/legal-address"
    Then The status code is 200


  Scenario: Failed to get verify legal-address
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
      {
       "taxCode": "11223345686"
       }
    """
    When I send a POST request to "/v1/institutions/verification/legal-address"
    Then The status code is 404