Feature: Product API end-to-end onboarding and lifecycle

  Scenario: Successfully create product onboarding (initial TESTING state)
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
          "default": {
            "IMPORT": [
              {
                "path": "contracts/template/mail/import-massivo-io/1.0.0.json",
                "status": "PENDING",
                "version": "1.0.0"
              }
            ]
          }
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
        "userContractMappings": {}
      }
    """
    When I send a POST request to "/product"
    Then The status code is 201
    And The response body contains:
      | productId | prod-test |
      | status    | TESTING   |

  @happy-path @get
  Scenario: Successfully get product by id after initial creation
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | productId | prod-test |
    When I send a GET request to "/product/{productId}"
    Then The status code is 200
    And The response body contains:
      | productId | prod-test |
      | status    | TESTING   |
      | version   | 1         |

  # =========================
  # 2) Upsert (override some fields; status=ACTIVE) and verification (version should increment)
  # =========================
  Scenario: Successfully upsert product onboarding (switch to ACTIVE)
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
          "default": {
            "IMPORT": [
              {
                "path": "contracts/template/mail/import-massivo-io/1.0.0.json",
                "status": "PENDING",
                "version": "1.0.0"
              }
            ]
          }
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
        "userContractMappings": {}
      }
    """
    When I send a POST request to "/product"
    Then The status code is 201
    And The response body contains:
      | productId | prod-test |
      | status    | ACTIVE    |

  Scenario: Successfully get product by id after upsert (version should be 2)
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | productId | prod-test |
    When I send a GET request to "/product/{productId}"
    Then The status code is 200
    And The response body contains:
      | productId | prod-test |
      | status    | ACTIVE    |
      | version   | 2         |

  # =========================
  # 3) Partial update via JSON Merge Patch and verification
  # =========================
  Scenario: Successfully patch selected product fields
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
    When I send a PATCH request to "/product/{productId}" with content type "application/merge-patch+json"
    Then The status code is 200
    And The response body contains:
      | productId   | prod-test                     |
      | description | Description updated via PATCH |
      | enabled     | false                         |
      | title       | Prod TEST 2 - Patched         |

  Scenario: Successfully get product by id after patch
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | productId | prod-test |
    When I send a GET request to "/product/{productId}"
    Then The status code is 200
    And The response body contains:
      | productId   | prod-test                     |
      | description | Description updated via PATCH |
      | enabled     | false                         |

  # =========================
  # 4) Delete and verification
  # =========================
  Scenario: Successfully delete product by id (status becomes DELETED)
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | productId | prod-test |
    When I send a DELETE request to "/product/{productId}"
    Then The status code is 200
    And The response body contains:
      | productId | prod-test |
      | status    | DELETED   |

  # =========================
  # Negative paths
  # =========================
  Scenario: Get product by id - product not found
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | productId | prod-unknown |
    When I send a GET request to "/product/{productId}"
    Then The status code is 404
    And The response body contains:
      | title  | Product not found |
      | status | 404               |

  Scenario: Create product - bad request (missing productId)
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

  Scenario: Patch product - bad payload (type mismatch)
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | productId | prod-unknown |
    And The following request body:
    """
      { "enabled": "not-a-boolean" }
    """
    When I send a PATCH request to "/product/{productId}" with content type "application/merge-patch+json"
    Then The status code is 400
    And The response body contains:
      | title  | Bad Request |
      | status | 400         |

  Scenario: Delete product - product not found
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | productId | prod-unknown |
    When I send a DELETE request to "/product/{productId}"
    Then The status code is 404
    And The response body contains:
      | title  | Product not found |
      | status | 404               |
