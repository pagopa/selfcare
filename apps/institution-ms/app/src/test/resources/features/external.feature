@FeatureExternal
Feature: External

# GET /external/institutions

  Scenario: Successfully retrieve institutions data by ids
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | ids | b4705659-3a01-430a-a19b-7bdb4e340223,fc5466e5-df00-4800-9ad5-aa2e7d9344f9,c9a50656-f345-4c81-84be-5b2474470544,000,c7a9a8e2-36e3-4ad5-9e63-6d482b74d1d7 |
    When I send a GET request to "/external/institutions"
    Then The status code is 200
    And The response body contains the list "" of size 4
    And The response body contains at path "" the following list of objects in any order:
      | id                                   | externalId                           | origin | originId | description                             | digitalAddress               | address            | zipCode | taxCode          | imported | supportEmail | delegation | subunitCode | subunitType |
      | b4705659-3a01-430a-a19b-7bdb4e340223 | 94076720658#UF5D7W                   | IPA    | UF5D7W   | Uff_eFatturaPA                          | saic8bu00x@pec.istruzione.it | Via San Pietro, 10 | 84014   | 94076720658      | false    |              | false      | UF5D7W      | UO          |
      | c9a50656-f345-4c81-84be-5b2474470544 | 00310810825                          | IPA    | c_c067   | Comune di Castelbuono                   | comune.castelbuono@pec.it    | Via Sant' Anna, 25 | 90013   | 00310810825      | false    | a@l.it       | false      |             |             |
      | fc5466e5-df00-4800-9ad5-aa2e7d9344f9 | 94076720658                          | IPA    | isticom  | 3 Istituto Comprensivo Nocera Inferiore | saic8bu00x@pec.istruzione.it | Via San Pietro, 10 | 84014   | 94076720658      | false    |              | false      |             |             |
      | c7a9a8e2-36e3-4ad5-9e63-6d482b74d1d7 | PRVTNT80A41H401T                     | IPA    | isticom  | Privato CF 1                            | test@test.com                | Via Roma, 1        | 00000   | PRVTNT80A41H401T | false    |              | false      |             |             |

  Scenario: Successfully retrieve institutions onboarding data by ids
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | ids | fc5466e5-df00-4800-9ad5-aa2e7d9344f9 |
    When I send a GET request to "/external/institutions"
    Then The status code is 200
    And The response body contains the list "[0].onboarding" of size 2
    And The response body contains at path "[0].onboarding" the following list of objects in any order:
      | productId   | tokenId                              | status | origin | originId | institutionType |
      | prod-pagopa | afb6c11d-67f8-44f9-b04f-88d47e0d2fc3 | ACTIVE | IPA    | isticom  | PA              |
      | prod-io     | 24534011-a806-4137-855c-059ea9b943cd | ACTIVE | SELC   | 123X     | PT              |

  Scenario: Bad request while while getting institutions data by ids
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "/external/institutions"
    Then The status code is 400
    And The response body contains:
      | status | 400               |
      | detail | MISSING PARAMETER |

  Scenario: Not found institutions while getting institutions data by ids
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | ids | 123456 |
    When I send a GET request to "/external/institutions"
    Then The status code is 200
    And The response body contains the list "" of size 0

# GET /external/institutions/{externalId}

  Scenario: Successfully get institution by externalId
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | externalId | 01726610056_AVO36EJ |
    When I send a GET request to "/external/institutions/{externalId}"
    Then The status code is 200
    And The response body contains:
      | id             | f94c0589-b07e-4ee7-a509-fda5fe91faa2  |
      | externalId     | 01726610056_AVO36EJ                   |
      | origin         | IPA                                   |
      | originId       | AVO36EJ                               |
      | description    | COMUNE DI MORANSENGO-TONENGO          |
      | address        | Via VIA AIRALI 9                      |
      | zipCode        | 14023                                 |
      | taxCode        | 01726610056                           |
      | imported       | false                                 |
      | digitalAddress | comune.moransengotonengo@legalmail.it |
      | subunitCode    | AVO36EJ                               |
      | subunitType    | AOO                                   |
    And The response body contains the list "onboarding" of size 4
    And The response body contains at path "onboarding" the following list of objects in any order:
      | productId    | tokenId                              | status  | institutionType | origin | originId |
      | prod-pn      | 5cf41352-de89-4bc1-b42a-3c86ba30c9bb | PENDING |                 | IPA    | AVO36EJ  |
      | prod-interop | 7a7bcae7-13f6-4b4c-84f0-adbcfca283b4 | PENDING |                 | IPA    | AVO36EJ  |
      | prod-idpay   | 65f7b3f2-9351-4c87-ada6-3205c5660a3d | PENDING |                 | IPA    | AVO36EJ  |
      | prod-io      |                                      | ACTIVE  |                 | IPA    | AVO36EJ  |

  Scenario: Not found institutions while getting institution by externalId
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | externalId | 000 |
    When I send a GET request to "/external/institutions/{externalId}"
    Then The status code is 404
    And The response body contains:
      | status | 404                                                                                 |
      | detail | Cannot find Institution using institutionId UNDEFINED and externalInstitutionId 000 |

# GET /external/institutions/{externalId}/geotaxonomies

  Scenario: Successfully retrieve geotaxonomies by externalId
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | externalId | 15555555555 |
    When I send a GET request to "/external/institutions/{externalId}/geotaxonomies"
    Then The status code is 200
    And The response body contains the list "" of size 2
    And The response body contains at path "" the following list of objects in any order:
      | code   | desc             | istat_code | province_id | province_abbreviation | region_id | country | country_abbreviation | enabled |
      | 058091 | ROMA - COMUNE    | 058091     | 058         | RM                    | 12        | 100     | IT                   | false   |
      | 038008 | FERRARA - COMUNE | 038008     | 038         | FE                    | 08        | 100     | IT                   | false   |

  Scenario: Error retrieving geotaxonomies by externalId
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | externalId | 99000870064 |
    When I send a GET request to "/external/institutions/{externalId}/geotaxonomies"
    Then The status code is 500
    And The response body contains:
      | status | 500                                             |
      | detail | Error on retrieve geographic taxonomy code: 123 |

  Scenario: Not found geotaxonomies by externalId with non existing institution
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | externalId | 000 |
    When I send a GET request to "/external/institutions/{externalId}/geotaxonomies"
    Then The status code is 404
    And The response body contains:
      | status | 404                                                                                 |
      | detail | Cannot find Institution using institutionId UNDEFINED and externalInstitutionId 000 |

# GET /external/institutions/{externalId}/products

  Scenario: Successfully get onboarded products by externalId
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | externalId | 85000870064 |
    When I send a GET request to "/external/institutions/{externalId}/products"
    Then The status code is 200
    And The response body contains the list "products" of size 4
    And The response body contains at path "products" the following list of objects in any order:
      | id           | state   |
      | prod-io      | ACTIVE  |
      | prod-pagopa  | DELETED |
      | prod-pagopa  | ACTIVE  |
      | prod-interop | ACTIVE  |

  Scenario: Successfully get onboarded products by externalId and states
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | externalId | 85000870064 |
    And The following query params:
      | states | ACTIVE,SUSPENDED |
    When I send a GET request to "/external/institutions/{externalId}/products"
    Then The status code is 200
    And The response body contains the list "products" of size 3
    And The response body contains at path "products" the following list of objects in any order:
      | id           | state  |
      | prod-pagopa  | ACTIVE |
      | prod-io      | ACTIVE |
      | prod-interop | ACTIVE |

  Scenario: Not found institution while retrieving onboarded products by externalId
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | externalId | 000 |
    When I send a GET request to "/external/institutions/{externalId}/products"
    Then The status code is 404
    And The response body contains:
      | status | 404                                                                                 |
      | detail | Cannot find Institution using institutionId UNDEFINED and externalInstitutionId 000 |

  Scenario: Not found onboarded products by externalId and states
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | externalId | 85000870064 |
    And The following query params:
      | states | SUSPENDED |
    When I send a GET request to "/external/institutions/{externalId}/products"
    Then The status code is 404
    And The response body contains:
      | status | 404                                                                                       |
      | detail | Products not found for institution having internalId 467ac77d-7faa-47bf-a60e-38ea74bd5fd2 |

# GET /external/institutions/{externalId}/products/{productId}/billing

  Scenario: Successfully retrieve billing data by externalId and productId
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | externalId | 01726610056_AVO36EJ |
      | productId  | prod-io             |
    When I send a GET request to "/external/institutions/{externalId}/products/{productId}/billing"
    Then The status code is 200
    And The response body contains:
      | institutionId            | f94c0589-b07e-4ee7-a509-fda5fe91faa2  |
      | externalId               | 01726610056_AVO36EJ                   |
      | origin                   |                                       |
      | originId                 |                                       |
      | description              | COMUNE DI MORANSENGO-TONENGO          |
      | institutionType          |                                       |
      | digitalAddress           | comune.moransengotonengo@legalmail.it |
      | address                  | Via VIA AIRALI 9                      |
      | zipCode                  | 14023                                 |
      | taxCode                  | 01726610056                           |
      | pricingPlan              |                                       |
      | subunitCode              | AVO36EJ                               |
      | subunitType              | AOO                                   |
      | aooParentCode            |                                       |
      | billing.vatNumber        | 01726610056                           |
      | billing.taxCodeInvoicing |                                       |
      | billing.recipientCode    | c_i580                                |
      | billing.publicServices   | false                                 |

  Scenario: Successfully retrieve billing data by externalId as a fiscal code and productId
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | externalId |  35a78332-d038-4bfa-8e85-2cba7f6b7bf7 |
      | productId  | prod-io                               |
    When I send a GET request to "/external/institutions/{externalId}/products/{productId}/billing"
    Then The status code is 200
    And The response body contains:
      | institutionId            | 0c6d2a5e-9c42-4b2f-9c3f-94c5cb2b6d1a  |
      | externalId               | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7  |
      | origin                   |                                       |
      | originId                 |                                       |
      | description              | Privato CF 2                          |
      | institutionType          | PRV_PF                                |
      | digitalAddress           | test@test.com                         |
      | address                  | Via Roma, 1                           |
      | zipCode                  | 00000                                 |
      | taxCode                  | blbrki80A41H401T                      |
      | pricingPlan              |                                       |
      | subunitCode              |                                       |
      | subunitType              |                                       |
      | aooParentCode            |                                       |

  Scenario: Not Found billing data with non existing externalId
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | externalId | 000     |
      | productId  | prod-io |
    When I send a GET request to "/external/institutions/{externalId}/products/{productId}/billing"
    Then The status code is 404
    And The response body contains:
      | status | 404                                                                            |
      | detail | Error while retrieving institution having externalId 000 and productId prod-io |

  Scenario: Not Found billing data with non existing productId
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | externalId | 01726610056_AVO36EJ |
      | productId  | prod-x              |
    When I send a GET request to "/external/institutions/{externalId}/products/{productId}/billing"
    Then The status code is 404
    And The response body contains:
      | status | 404                                                                                           |
      | detail | Error while retrieving institution having externalId 01726610056_AVO36EJ and productId prod-x |
