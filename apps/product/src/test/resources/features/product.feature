Feature: Product API end-to-end onboarding and lifecycle

  Scenario: POST /product - successfully create product in TESTING state
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
      {
        "alias": "prod-test",
        "allowedInstitutionTaxCode": [],
        "backOfficeEnvironmentConfigurations": {
          "Locale": {
            "identityTokenAudience": "locale",
            "url": "http://localhost:8080"
          }
        },
        "consumers": [
          "Standard"
        ],
        "createdBy": "user-apim-name",
        "delegable": false,
        "depictImageUrl": "http://localhost:8080",
        "description": "Product description",
        "emailTemplates": {
            "IMPORT": [
              {
                "path": "contracts/template/mail/import-massivo-io/1.0.0.json",
                "status": "PENDING",
                "version": "1.0.0"
              }
            ]
        },
        "enabled": true,
        "expirationDate": 30,
        "identityTokenAudience": "http://localhost:8080",
        "institutionAggregatorContractMappings": {},
        "institutionContractMappings": {
          "default": {
            "attachments": [],
            "contractTemplatePath": "contracts/template/io/2.4.6/io-accordo_di_adesione-v.2.4.6.html",
            "contractTemplateVersion": "2.4.6"
          }
        },
        "invoiceable": true,
        "logo": "http://localhost:8080",
        "logoBgColor": "#0B3EE3",
        "productId": "prod-test",
        "roleMappings": {
          "OPERATOR": {
            "multiroleAllowed": false,
            "phasesAdditionAllowed": [],
            "roles": [
              {
                "code": "referente operativo",
                "description": "Operatore",
                "label": "Operatore"
              }
            ],
            "skipUserCreation": false
          }
        },
        "status": "TESTING",
        "title": "Prod TEST",
        "urlBO": "http://localhost:8080",
        "urlPublic": "http://localhost:8080",
        "userAggregatorContractMappings": {},
        "userContractMappings": {},
        "institutionOrigins": [
          {
            "institutionType": "PA",
            "origin": "IPA",
            "labelKey": "pa"
          },
          {
            "institutionType": "GSP",
            "origin": "IPA",
            "labelKey": "gsp"
          },
          {
            "institutionType": "SCP",
            "origin": "SELC",
            "labelKey": "scp"
          }
        ],
        "allowCompanyOnboarding": true,
        "allowIndividualOnboarding": false
      }
    """
    When I send a POST request to "/product"
    Then The status code is 201
    And The response body contains:
      | productId | prod-test |
      | status    | TESTING   |

  Scenario: GET /product/{productId} - successfully retrieve product after creation
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | productId | prod-test |
    When I send a GET request to "/product/{productId}"
    Then The status code is 200
    And The response body contains:
      | productId | prod-test |
      | status    | TESTING   |
      | version   | 1         |

  Scenario: GET /product/{productId}/origins - successfully retrieve origins
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | productId | prod-test |
    When I send a GET request to "/product/{productId}/origins"
    Then The status code is 200
    And The response body contains:
      | origins[0].institutionType | PA  |
      | origins[0].labelKey        | pa  |
      | origins[0].origin          | IPA |

  Scenario: POST /product - successfully upsert existing product to ACTIVE state
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
      {
        "alias": "prod-test",
        "allowedInstitutionTaxCode": [],
        "backOfficeEnvironmentConfigurations": {
          "Locale": {
            "identityTokenAudience": "locale",
            "url": "http://localhost:8080"
          }
        },
        "consumers": [
          "Standard"
        ],
        "createdBy": "user-apim-name",
        "delegable": false,
        "depictImageUrl": "http://localhost:8080",
        "description": "Product description 2",
        "emailTemplates": {
            "IMPORT": [
              {
                "path": "contracts/template/mail/import-massivo-io/1.0.0.json",
                "status": "PENDING",
                "version": "1.0.0"
              }
            ]
        },
        "enabled": true,
        "expirationDate": 30,
        "identityTokenAudience": "http://localhost:8080",
        "institutionAggregatorContractMappings": {},
        "institutionContractMappings": {
          "default": {
            "attachments": [],
            "contractTemplatePath": "contracts/template/io/2.4.6/io-accordo_di_adesione-v.2.4.6.html",
            "contractTemplateVersion": "2.4.6"
          }
        },
        "invoiceable": true,
        "logo": "http://localhost:8080",
        "logoBgColor": "#0B3EE3",
        "productId": "prod-test",
        "roleMappings": {
          "OPERATOR": {
            "multiroleAllowed": false,
            "phasesAdditionAllowed": [],
            "roles": [
              {
                "code": "referente operativo",
                "description": "Operatore",
                "label": "Operatore"
              }
            ],
            "skipUserCreation": false
          }
        },
        "status": "ACTIVE",
        "title": "Prod TEST 2",
        "urlBO": "http://localhost:8080",
        "urlPublic": "http://localhost:8080",
        "userAggregatorContractMappings": {},
        "userContractMappings": {},
        "institutionOrigins": [
          {
            "institutionType": "PA",
            "origin": "IPA",
            "labelKey": "pa"
          },
          {
            "institutionType": "GSP",
            "origin": "IPA",
            "labelKey": "gsp"
          },
          {
            "institutionType": "SCP",
            "origin": "SELC",
            "labelKey": "scp"
          }
        ],
        "allowCompanyOnboarding": true,
        "allowIndividualOnboarding": false
      }
    """
    When I send a POST request to "/product"
    Then The status code is 201
    And The response body contains:
      | productId | prod-test |
      | status    | ACTIVE    |

  Scenario: GET /product/{productId} - successfully retrieve product after upsert
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | productId | prod-test |
    When I send a GET request to "/product/{productId}"
    Then The status code is 200
    And The response body contains:
      | productId | prod-test |
      | status    | ACTIVE    |
      | version   | 2         |

  Scenario: PATCH /product/{productId} - successfully update product fields
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | productId | prod-test |
    And The following request body:
    """
      {
        "description": "Description updated via PATCH",
        "enabled": false,
        "title": "Prod TEST 2 - Patched"
      }
    """
    When I send a PATCH request to "/product/{productId}" with content type "application/json"
    Then The status code is 200
    And The response body contains:
      | productId   | prod-test                     |
      | description | Description updated via PATCH |
      | enabled     | false                         |
      | title       | Prod TEST 2 - Patched         |

  Scenario: GET /product/{productId} - successfully retrieve product after patch
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | productId | prod-test |
    When I send a GET request to "/product/{productId}"
    Then The status code is 200
    And The response body contains:
      | productId   | prod-test                     |
      | description | Description updated via PATCH |
      | enabled     | false                         |

  Scenario: DELETE /product/{productId} - successfully mark product as DELETED
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | productId | prod-test |
    When I send a DELETE request to "/product/{productId}"
    Then The status code is 200
    And The response body contains:
      | productId | prod-test |
      | status    | DELETED   |

  Scenario: GET /product/{productId} - return 404 when product not found
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | productId | prod-unknown |
    When I send a GET request to "/product/{productId}"
    Then The status code is 404
    And The response body contains:
      | title  | Product not found |
      | status | 404               |

  Scenario: POST /product - return 400 when productId is missing
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
      {
        "alias": "prod-missing-id",
        "status": "TESTING",
        "title": "Without ID",
        "enabled": true
      }
    """
    When I send a POST request to "/product"
    Then The status code is 400

  Scenario: PATCH /product/{productId} - return 400 when payload is invalid
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | productId | prod-unknown |
    When I send a PATCH request to "/product/{productId}" with content type "application/json"
    Then The status code is 400
    And The response body contains:
      | title  | Bad Request |
      | status | 400         |

  Scenario: DELETE /product/{productId} - return 404 when product not found
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | productId | prod-unknown |
    When I send a DELETE request to "/product/{productId}"
    Then The status code is 404
    And The response body contains:
      | title  | Product not found |
      | status | 404               |