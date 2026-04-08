Feature: Document content health

  Scenario: Service is up
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "/q/health"
    Then The status code is 200

  Scenario: Create contract PDF successfully (onboarding-sdk payload)
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
    {
      "onboardingId": "onb-123",
      "contractTemplatePath": "contract-templates/prod-test/5ca7f491-5770-40ba-baf0-6fe612bba14e.html",
      "productId": "prod-test",
      "productName": "Prod TEST",
      "pricingPlan": "STANDARD",
      "aggregatesCsvBaseUrl": "http://localhost:8081",
      "institution": {
        "id": "inst-1",
        "institutionType": "PA",
        "taxCode": "12345678901",
        "description": "Comune di Test",
        "digitalAddress": "test@example.it",
        "address": "Via Roma 1",
        "zipCode": "00100",
        "city": "Roma",
        "country": "IT",
        "geographicTaxonomies": [
          {
            "code": "RM001",
            "desc": "Roma"
          }
        ],
        "paymentServiceProvider": {
          "abiCode": "03069",
          "businessRegisterNumber": "RM-123456",
          "legalRegisterName": "Banca Test",
          "legalRegisterNumber": "RM-123456",
          "vatNumberGroup": false
        }
      },
      "manager": {
        "id": "user-1",
        "role": "ADMIN",
        "name": "Mario",
        "surname": "Rossi",
        "taxCode": "RSSMRA80A01F205X",
        "email": "mario.rossi@example.it",
        "userMailUuid": "uuid-mail-1"
      },
      "delegates": [
        {
          "id": "user-2",
          "role": "DELEGATE",
          "name": "Luigi",
          "surname": "Verdi",
          "taxCode": "VRDLGU80A01H501Z",
          "email": "luigi.verdi@example.it",
          "userMailUuid": "uuid-mail-2"
        }
      ],
      "billing": {
        "vatNumber": "12345678901",
        "recipientCode": "AAAAAA1",
        "publicServices": true,
        "registeredOffice": "Via Roma 1"
      },
      "payment": {
        "pspCode": "99999",
        "pspAccountCode": "ACC-001",
        "pspBrokerName": "Broker Test",
        "pspBrokerVatNumber": "99999999999"
      }
    }
    """
    When I send a POST request to "/v1/document-content/contract"
    Then The status code is 200
    And The response body contains field "storagePath"
    And The response body contains field "filename"

  Scenario: Create attachment PDF validation error (missing fields)
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
    {
      "onboardingId": "",
      "productId": "prod-test",
      "productName": "Prod TEST"
    }
    """
    When I send a POST request to "/v1/document-content/attachment"
    Then The status code is 400

  Scenario: Upload aggregates CSV without file
    Given User login with username "j.doe" and password "test"
    And The following form params:
      | onboardingId | onb-123 |
      | productId    | prod-test |
    When I send a POST request to "/v1/document-content/aggregates-csv" with form data only
    Then The status code is 400

  Scenario: Upload signed contract without file
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | onboardingId | onb-123 |
    And The following form params:
      | request | {"onboardingId":"onb-123","productId":"prod-test","documentType":"INSTITUTION"} |
    When I send a POST request to "/v1/document-content/{onboardingId}/upload-signed-contract" with form data only
    Then The status code is 400

  Scenario: Retrieve template attachment successfully
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | onboardingId | onb-template |
    And The following query params:
      | templatePath           | contract-templates/prod-test/76f9cfd4-e9ae-4eeb-8ed3-1285cc5968d6.pdf |
      | name                   | template-attachment.pdf |
      | institutionDescription | Comune di Test |
      | productId              | prod-test |
    When I send a GET request to "/v1/document-content/{onboardingId}/template-attachment"
    Then The status code is 200
    And The response header contains:
      | Content-Type | application/octet-stream |

  Scenario: Bad request when creating contract PDF with missing mandatory fields
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
    {
      "contractTemplatePath": "contract-templates/prod-test/5ca7f491-5770-40ba-baf0-6fe612bba14e.html",
      "productId": "prod-test",
      "productName": "Prod TEST",
      "institution": {},
      "manager": {}
    }
    """
    When I send a POST request to "/v1/document-content/contract"
    Then The status code is 400
