Feature: Onboarding admission rules

  Scenario: POST /product - create a product with a pair onboardable only through managed flows
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
    {
      "tenantId": "AR",
      "productId": "prod-rules",
      "alias": "prod-rules",
      "title": "Prod RULES",
      "description": "Product used to test the onboarding admission rules",
      "status": "ACTIVE",
      "features": {
        "expirationDays": 45
      },
      "institutionOrigins": [
        {
          "institutionType": "PA",
          "origin": "IPA",
          "labelKey": "pa"
        },
        {
          "institutionType": "GPU",
          "origin": "SELC",
          "labelKey": "gpu",
          "selfOnboarding": false
        },
        {
          "institutionType": "PSP",
          "origin": "SELC",
          "labelKey": "psp"
        },
        {
          "institutionType": "PRV",
          "origin": "SELC",
          "labelKey": "prv",
          "selfOnboarding": false
        }
      ],
      "workflowRules": [
        {
          "institutionType": "PA",
          "origin": "IPA",
          "workflowType": "CONTRACT_REGISTRATION"
        },
        {
          "institutionType": "GPU",
          "origin": "SELC",
          "workflowType": "FOR_APPROVE_GPU"
        },
        {
          "institutionType": "PSP",
          "origin": "SELC",
          "workflowType": "FOR_APPROVE"
        }
      ]
    }
    """
    And The following query params:
      | createdBy | user-test |
    When I send a POST request to "/product"
    Then The status code is 201
    And The response body contains:
      | productId | prod-rules |
      | status    | ACTIVE     |

  Scenario: GET /product/{productId} - the full configuration exposes selfOnboarding
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "/product/prod-rules"
    Then The status code is 200
    And The response body contains:
      | institutionOrigins[1].institutionType | GPU   |
      | institutionOrigins[1].selfOnboarding  | false |
    And The response body doesn't contain field "institutionOrigins[0].selfOnboarding"

  Scenario: GET /product/origins - pairs onboardable only through managed flows are not shown
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId | prod-rules |
    When I send a GET request to "/product/origins"
    Then The status code is 200
    And The response body contains the list "origins" of size 2
    And The response body contains:
      | origins[0].institutionType | PA   |
      | origins[0].origin          | IPA  |
      | origins[0].labelKey        | pa   |
      | origins[1].institutionType | PSP  |
      | origins[1].origin          | SELC |
    And The response body doesn't contain field "origins[0].selfOnboarding"

  Scenario: POST /product/{productId}/onboarding-decision - managed-only pair is denied in self-service
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
    { "institutionType": "GPU", "origin": "SELC", "channel": "SELF_SERVICE" }
    """
    When I send a POST request to "/product/prod-rules/onboarding-decision"
    Then The status code is 200
    And The response body contains:
      | allowed | false                       |
      | reason  | SELF_ONBOARDING_NOT_ALLOWED |
    And The response body doesn't contain field "workflowType"

  Scenario: POST /product/{productId}/onboarding-decision - managed-only pair is allowed with the imposed workflow
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
    { "institutionType": "GPU", "origin": "SELC", "channel": "MANAGED", "workflowType": "CONFIRMATION" }
    """
    When I send a POST request to "/product/prod-rules/onboarding-decision"
    Then The status code is 200
    And The response body contains:
      | allowed           | true         |
      | workflowType      | CONFIRMATION |
      | expirationDays    | 45           |
      | product.productId | prod-rules   |

  Scenario: POST /product/{productId}/onboarding-decision - managed-only pair resolves the configured workflow
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
    { "institutionType": "GPU", "origin": "SELC", "channel": "MANAGED" }
    """
    When I send a POST request to "/product/prod-rules/onboarding-decision"
    Then The status code is 200
    And The response body contains:
      | allowed      | true            |
      | workflowType | FOR_APPROVE_GPU |

  Scenario: POST /product/{productId}/onboarding-decision - self-onboardable pair is allowed
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
    { "institutionType": "PA", "origin": "IPA", "channel": "SELF_SERVICE" }
    """
    When I send a POST request to "/product/prod-rules/onboarding-decision"
    Then The status code is 200
    And The response body contains:
      | allowed      | true                  |
      | workflowType | CONTRACT_REGISTRATION |

  Scenario: POST /product/{productId}/onboarding-decision - pair incoherent with the origin is denied
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
    { "institutionType": "SA", "origin": "IPA", "channel": "MANAGED" }
    """
    When I send a POST request to "/product/prod-rules/onboarding-decision"
    Then The status code is 200
    And The response body contains:
      | allowed | false                            |
      | reason  | ORIGIN_INSTITUTION_TYPE_MISMATCH |

  Scenario: POST /product/{productId}/onboarding-decision - pair not listed by the product is denied
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
    { "institutionType": "GSP", "origin": "IPA", "channel": "SELF_SERVICE" }
    """
    When I send a POST request to "/product/prod-rules/onboarding-decision"
    Then The status code is 200
    And The response body contains:
      | allowed | false                        |
      | reason  | INSTITUTION_TYPE_NOT_ALLOWED |

  Scenario: POST /product/{productId}/onboarding-decision - pair without workflow is denied
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
    { "institutionType": "PRV", "origin": "SELC", "channel": "MANAGED" }
    """
    When I send a POST request to "/product/prod-rules/onboarding-decision"
    Then The status code is 200
    And The response body contains:
      | allowed | false                   |
      | reason  | WORKFLOW_NOT_RESOLVABLE |

  Scenario: POST /product/{productId}/onboarding-decision - returns 400 when the channel is missing
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
    { "institutionType": "PA", "origin": "IPA" }
    """
    When I send a POST request to "/product/prod-rules/onboarding-decision"
    Then The status code is 400

  Scenario: POST /product/{productId}/onboarding-decision - returns 404 when the product does not exist
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
    { "institutionType": "PA", "origin": "IPA", "channel": "SELF_SERVICE" }
    """
    When I send a POST request to "/product/prod-unknown/onboarding-decision"
    Then The status code is 404

  Scenario: POST /product - a self-onboardable pair without workflow is rejected
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
    {
      "tenantId": "AR",
      "productId": "prod-rules-invalid",
      "alias": "prod-rules-invalid",
      "title": "Prod RULES invalid",
      "description": "Product with an incoherent onboarding configuration",
      "status": "ACTIVE",
      "institutionOrigins": [
        {
          "institutionType": "PSP",
          "origin": "SELC",
          "labelKey": "psp"
        }
      ]
    }
    """
    And The following query params:
      | createdBy | user-test |
    When I send a POST request to "/product"
    Then The status code is 400
    And The response body contains the string "Invalid onboarding rules for product prod-rules-invalid: institutionOrigins PSP/SELC is self-onboardable but has no workflow"
