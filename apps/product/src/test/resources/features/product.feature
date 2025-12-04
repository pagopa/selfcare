Feature: Product API end-to-end onboarding and lifecycle

  Scenario: POST /product - successfully create product in TESTING state
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
     {
      "productId": "prod-test",
      "alias": "prod-test",
      "title": "Prod TEST",
      "description": "Product description",
      "status": "TESTING",
      "version": 1,
      "consumers": [
        "Standard"
      ],
      "visualConfiguration": {
        "logoUrl": "http://localhost:8080",
        "depictImageUrl": "http://localhost:8080",
        "logoBgColor": "#0B3EE3"
      },
      "features": {
        "allowCompanyOnboarding": true,
        "allowIndividualOnboarding": false,
        "allowedInstitutionTaxCode": [],
        "delegable": false,
        "invoiceable": true,
        "expirationDays": 30,
        "enabled": true
      },

      "roleMappings": [
        {
          "role": "OPERATOR",
          "multiroleAllowed": false,
          "phasesAdditionAllowed": [],
          "skipUserCreation": false,
          "backOfficeRoles": [
            {
              "code": "referente operativo",
              "label": "Operatore",
              "description": "Operatore"
            }
          ]
        }
      ],
      "contracts": [
        {
          "type": "institution",
          "institutionType": "default",
          "path": "contracts/template/io/2.4.6/io-accordo_di_adesione-v.2.4.6.html",
          "version": "2.4.6",
          "order": 10,
          "generated": true,
          "mandatory": true,
          "name": "Accordo di adesione IO",
          "workflowState": "REQUEST",
          "workflowType": []
        }
      ],
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
      "emailTemplates": [
        {
          "type": "IMPORT",
          "institutionType": "default",
          "path": "contracts/template/mail/import-massivo-io/1.0.0.json",
          "version": "1.0.0",
          "status": "PENDING"
        }
      ],
      "backOfficeEnvironmentConfigurations": [
        {
          "env": "Locale",
          "urlPublic": "http://localhost:8080",
          "urlBO": "http://localhost:8080",
          "identityTokenAudience": "locale"
        }
      ],
      "metadata": {
        "createdBy": "user-apim-name"
      }
    }
    """
    And The following query params:
      | productId | prod-test   |
      | createdBy | utente-test |
    When I send a POST request to "/product"
    Then The status code is 201
    And The response body contains:
      | productId | prod-test |
      | status    | TESTING   |

  Scenario: GET /product - successfully retrieve product after creation
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId | prod-test |
    When I send a GET request to "/product"
    Then The status code is 200
    And The response body contains:
      | productId | prod-test |
      | status    | TESTING   |
      | version   | 1         |

  Scenario: GET /product/origins - successfully retrieve origins
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId | prod-test |
    When I send a GET request to "/product/origins"
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
      "productId": "prod-test",
      "alias": "prod-test",
      "title": "Prod TEST 2 - Patched",
      "description": "Description updated via PATCH",
      "status": "ACTIVE",
      "version": 1,
      "consumers": [
        "Standard"
      ],
      "visualConfiguration": {
        "logoUrl": "http://localhost:8080",
        "depictImageUrl": "http://localhost:8080",
        "logoBgColor": "#0B3EE3"
      },
      "features": {
        "allowCompanyOnboarding": true,
        "allowIndividualOnboarding": false,
        "allowedInstitutionTaxCode": [],
        "delegable": false,
        "invoiceable": true,
        "expirationDays": 30,
        "enabled": false
      },
      "roleMappings": [
        {
          "role": "OPERATOR",
          "multiroleAllowed": false,
          "phasesAdditionAllowed": [],
          "skipUserCreation": false,
          "backOfficeRoles": [
            {
              "code": "referente operativo",
              "label": "Operatore",
              "description": "Operatore"
            }
          ]
        }
      ],
      "contracts": [
        {
          "type": "institution",
          "institutionType": "default",
          "path": "contracts/template/io/2.4.6/io-accordo_di_adesione-v.2.4.6.html",
          "version": "2.4.6",
          "order": 10,
          "generated": true,
          "mandatory": true,
          "name": "Accordo di adesione IO",
          "workflowState": "REQUEST",
          "workflowType": []
        }
      ],
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
      "emailTemplates": [
        {
          "type": "IMPORT",
          "institutionType": "default",
          "path": "contracts/template/mail/import-massivo-io/1.0.0.json",
          "version": "1.0.0",
          "status": "PENDING"
        }
      ],
      "backOfficeEnvironmentConfigurations": [
        {
          "env": "Locale",
          "urlPublic": "http://localhost:8080",
          "urlBO": "http://localhost:8080",
          "identityTokenAudience": "locale"
        }
      ],
      "metadata": {
        "createdBy": "user-apim-name"
      }
    }
    """
    And The following query params:
      | productId | prod-test   |
      | createdBy | utente-test |
    When I send a POST request to "/product"
    Then The status code is 201
    And The response body contains:
      | productId | prod-test |
      | status    | ACTIVE    |

  Scenario: GET /product - successfully retrieve product after upsert
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId | prod-test   |
      | createdBy | utente-test |
    When I send a GET request to "/product"
    Then The status code is 200
    And The response body contains:
      | productId | prod-test |
      | status    | ACTIVE    |
      | version   | 2         |

  Scenario: PATCH /product - successfully update product fields
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId | prod-test   |
      | createdBy | utente-test |
    And The following request body:
    """
      {
        "description": "Description updated via PATCH",
        "features[0].enabled": false,
        "title": "Prod TEST 2 - Patched"
      }
    """
    When I send a PATCH request to "/product" with content type "application/json"
    Then The status code is 200
    And The response body contains:
      | productId   | prod-test                     |
      | description | Description updated via PATCH |
      | features.enabled | false                         |
      | title       | Prod TEST 2 - Patched         |

  Scenario: GET /product - successfully retrieve product after patch
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId | prod-test |
    When I send a GET request to "/product"
    Then The status code is 200
    And The response body contains:
      | productId   | prod-test                     |
      | description | Description updated via PATCH |
      | features.enabled | false                         |

  Scenario: DELETE /product - successfully mark product as DELETED
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId | prod-test |
    When I send a DELETE request to "/product"
    Then The status code is 200
    And The response body contains:
      | productId | prod-test |
      | status    | DELETED   |

  Scenario: GET /product - return 404 when product not found
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId | prod-unknown |
    When I send a GET request to "/product"
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
        "title": "Without ID"
      }
    """
    When I send a POST request to "/product"
    Then The status code is 400

  Scenario: PATCH /product - return 400 when payload is invalid
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId | prod-unknown |
    When I send a PATCH request to "/product" with content type "application/json"
    Then The status code is 400
    And The response body contains:
      | title  | Bad Request |
      | status | 400         |

  Scenario: DELETE /product - return 404 when product not found
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId | prod-unknown |
    When I send a DELETE request to "/product"
    Then The status code is 404
    And The response body contains:
      | title  | Product not found |
      | status | 404               |