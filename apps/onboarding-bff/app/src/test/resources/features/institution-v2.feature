Feature: Institution-V2

  #Scenario: Success to complete onboarding
  #  Given User login with username "j.doe" and password "test"
  #  And The following request body:
  #  """
  #     {
  #      "assistanceContacts": {
  #        "supportEmail": "supporto@esempio.it"
  #      },
  #      "billingData": {
  #        "businessName": "Azienda Fittizia S.p.A.",
  #        "digitalAddress": "azienda.fittizia@pec.it",
  #        "publicServices": false,
  #        "recipientCode": "ABC1234",
  #        "registeredOffice": "Via Roma 123, 84100 Salerno SA",
  #        "taxCode": "35843432474",
  #        "taxCodeInvoicing": "12345678901",
  #        "vatNumber": "IT01234567890",
  #        "zipCode": "84100"
  #      },
  #      "geographicTaxonomies": [
  #        {
  #          "code": "084",
  #          "desc": "Salerno"
  #        }
  #      ],
  #      "institutionLocationData": {
  #        "city": "Salerno",
  #        "country": "Italia",
  #        "county": "Salerno"
  #      },
  #      "institutionType": "PRV",
  #      "isAggregator": false,
  #      "origin": "SELC",
  #      "originId": "35843432474",
  #      "productId": "prod-pagopa",
  #      "taxCode": "35843432474",
  #      "users": [
  #        {
  #          "email": "mario.rossi@esempio.it",
  #          "name": "Mario",
  #          "role": "ADMIN_EA",
  #          "surname": "Rossi",
  #          "taxCode": "RSSMRA80A01H703Z"
  #        }
  #      ]
  #    }
  #  """
  #  When I send a POST request to "/v2/institutions/onboarding"
  #  Then The status code is 204

  Scenario: Success to retry institution by filter
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "/v2/institutions?productId=prod-io&taxCode=11223345661"
    Then The status code is 200

  Scenario: Failed to retry institution when missing required fields
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "/v2/institutions"
    Then The status code is 400

  Scenario: Failed to retry institution when missing onboarding
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "/v2/institutions?productId=prod-fideiussioni&origin=INFOCAMERE"
    Then The status code is 404


  Scenario: Success to retry an active onboarding
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "/v2/institutions/onboarding/active?productId=prod-io&taxCode=11223345661"
    Then The status code is 200

  Scenario: Failed to retry an active onboarding when not found
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "/v2/institutions/onboarding/active?productId=prod-io&taxCode=11223345662"
    Then The status code is 404

  Scenario: Failed to retry an active onboarding missing param into request
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "/v2/institutions/onboarding/active"
    Then The status code is 400


  Scenario: Success recipient code verification
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | originId      | c_d548 |
      | recipientCode | 3QOOYF |

    When I send a GET request to "/v2/institutions/onboarding/recipient-code/verification"
    Then The status code is 200

  Scenario: Failed recipient code verification
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | originId      | -     |
      | recipientCode | 3QOOY |

    When I send a GET request to "/v2/institutions/onboarding/recipient-code/verification"
    Then The status code is 404

  #Scenario: Success onboarding users pg
  #  Given User login with username "j.doe" and password "test"
  #  And The following request body:
  #  """
  #   {
  #    "users":[
  #      {
  #         "name":"Tizio",
  #         "role":"MANAGER",
  #         "surname":"Caio",
  #         "taxCode":"VRDMRA22T71F205A",
  #         "email":"prova@test.it"
  #      }
  #    ],
  #    "productId":"prod-io",
  #    "taxCode":"11223345777",
  #    "certified":"false"
  #   }
  #  """
  #  When I send a POST request to "/v2/institutions/onboarding/users/pg"
  #  Then The status code is 200
