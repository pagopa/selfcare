Feature: Institution

  ######################### BEGIN GET /institutions/{institutionId}/users #########################

  Scenario: Successfully fetches detailed information about users associated with a specific institution
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                           | a1b2c3d4-5678-90ab-cdef-1234567890ab                  |
    When I send a GET request to "/institutions/{institutionId}/users"
    Then The status code is 200
    And The response body contains the list "" of size 2
    And The response body contains at path "" the following list of objects in any order:
      | id                                      | taxCode                  | name        | surname      | email                                               |
      | 97a511a7-2acc-47b9-afed-2f3c65753b4a    | PRVTNT80A41H401T         | John        | Doe          | 81956dd1-00fd-4423-888b-f77a48d26ba1@test.it        |
      | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7    | blbrki80A41H401T         | rocky       | Balboa       | r.balboa@regionelazio.it                            |

  Scenario: Successfully fetches detailed information about users associated with a wrong institution id
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                           | wrongInstitutionId                     |
    When I send a GET request to "/institutions/{institutionId}/users"
    Then The status code is 200
    And The response body contains the list "" of size 0

  Scenario: Bad Token fetches detailed information about users associated with a specific institution
    Given A bad jwt token
    And The following path params:
      | institutionId                           | a1b2c3d4-5678-90ab-cdef-1234567890ab                  |
    When I send a GET request to "/institutions/{institutionId}/users"
    Then The status code is 401

  ######################### END GET /institutions/{institutionId}/users #########################

  ######################### BEGIN GET /institutions/{institutionId}/user-institutions #########################

  Scenario: Successfully retrieve users with optional filters
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                           | a1b2c3d4-5678-90ab-cdef-1234567890ab                  |
    When I send a GET request to "/institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 2
    And The response body contains at path "" the following list of objects in any order:
      | id                                      | userId                                                | institutionId                             | institutionDescription      | userMailUuid                                        |
      | 65b1234f85a6a37415221ef9                | 97a511a7-2acc-47b9-afed-2f3c65753b4a                  | a1b2c3d4-5678-90ab-cdef-1234567890ab      | Regione Lazio               | ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1        |
      | 65b1214f85b2a37412421ef6                | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                  | a1b2c3d4-5678-90ab-cdef-1234567890ab      | Regione Lazio               | ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv        |

  Scenario: Successfully retrieve users with optional filters (userId filter)
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                           | a1b2c3d4-5678-90ab-cdef-1234567890ab                  |
    And The following query params:
      | userId                                  | 97a511a7-2acc-47b9-afed-2f3c65753b4a                  |
    When I send a GET request to "/institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains at path "" the following list of objects in any order:
      | id                                      | userId                                                | institutionId                             | institutionDescription      | userMailUuid                                        |
      | 65b1234f85a6a37415221ef9                | 97a511a7-2acc-47b9-afed-2f3c65753b4a                  | a1b2c3d4-5678-90ab-cdef-1234567890ab      | Regione Lazio               | ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1        |
    And The response body contains the list "[0].products" of size 2
    And The response body contains at path "[0].products" the following list of objects in any order:
      | roleId                               | productId    | tokenId                              | status  | productRole              | role    | env  | createdAt                |
      | d9f8e7c6-1234-45a6-b789-0c1d2e3f4a5b | prod-io      | abc12345-6789-4def-b012-3456789abcd  | ACTIVE  | admin                    | MANAGER | ROOT | 2023-06-15T14:30:00Z     |
      | f1e2d3c4-b567-890a-bcde-1234567890ff | prod-interop | def67890-1234-4abc-5678-90abcdef1234 | DELETED | referente amministrativo | MANAGER | ROOT | 2024-01-20T09:45:10.567Z |

  Scenario: Successfully retrieve users with optional filters (userId filter)
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                           | a1b2c3d4-5678-90ab-cdef-1234567890ab                  |
    And The following query params:
      | userId                                  | 97a511a7-2acc-47b9-afed-2f3c65753b4a                  |
    When I send a GET request to "/institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains at path "" the following list of objects in any order:
      | id                                      | userId                                                | institutionId                             | institutionDescription      | userMailUuid                                        |
      | 65b1234f85a6a37415221ef9                | 97a511a7-2acc-47b9-afed-2f3c65753b4a                  | a1b2c3d4-5678-90ab-cdef-1234567890ab      | Regione Lazio               | ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1        |
    And The response body contains the list "[0].products" of size 2
    And The response body contains at path "[0].products" the following list of objects in any order:
      | roleId                               | productId     | tokenId                               | status    | productRole                 | role      | env      | createdAt                    |
      | d9f8e7c6-1234-45a6-b789-0c1d2e3f4a5b | prod-io       | abc12345-6789-4def-b012-3456789abcd   | ACTIVE    | admin                       | MANAGER   | ROOT     | 2023-06-15T14:30:00Z         |
      | f1e2d3c4-b567-890a-bcde-1234567890ff | prod-interop  | def67890-1234-4abc-5678-90abcdef1234  | DELETED   | referente amministrativo    | MANAGER   | ROOT     | 2024-01-20T09:45:10.567Z     |

  Scenario: Successfully retrieve users with optional filters (products filter)
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                           | a1b2c3d4-5678-90ab-cdef-1234567890ab                  |
    And The following query params:
      | products                                | prod-interop                                          |
    When I send a GET request to "/institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains at path "" the following list of objects in any order:
      | id                                      | userId                                                | institutionId                             | institutionDescription      | userMailUuid                                        |
      | 65b1234f85a6a37415221ef9                | 97a511a7-2acc-47b9-afed-2f3c65753b4a                  | a1b2c3d4-5678-90ab-cdef-1234567890ab      | Regione Lazio               | ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1        |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | roleId                               | productId     | tokenId                               | status    | productRole                 | role      | env      | createdAt                    |
      | f1e2d3c4-b567-890a-bcde-1234567890ff | prod-interop  | def67890-1234-4abc-5678-90abcdef1234  | DELETED   | referente amministrativo    | MANAGER   | ROOT     | 2024-01-20T09:45:10.567Z     |

  Scenario: Successfully retrieve users with optional filters (products filter with two products)
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                           | a1b2c3d4-5678-90ab-cdef-1234567890ab                  |
    And The following query params:
      | products                                | prod-interop                                          |
      | products                                | prod-io                                               |
    When I send a GET request to "/institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 2
    And The response body contains at path "" the following list of objects in any order:
      | id                                      | userId                                                | institutionId                             | institutionDescription      | userMailUuid                                        |
      | 65b1234f85a6a37415221ef9                | 97a511a7-2acc-47b9-afed-2f3c65753b4a                  | a1b2c3d4-5678-90ab-cdef-1234567890ab      | Regione Lazio               | ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1        |
      | 65b1214f85b2a37412421ef6                | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                  | a1b2c3d4-5678-90ab-cdef-1234567890ab      | Regione Lazio               | ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv        |

  Scenario: Successfully retrieve users with optional filters (roles filter)
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                           | a1b2c3d4-5678-90ab-cdef-1234567890ab                  |
    And The following query params:
      | roles                                   | SUB_DELEGATE                                          |
    When I send a GET request to "/institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains at path "" the following list of objects in any order:
      | id                                      | userId                                                | institutionId                             | institutionDescription      | userMailUuid                                        |
      | 65b1214f85b2a37412421ef6                | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                  | a1b2c3d4-5678-90ab-cdef-1234567890ab      | Regione Lazio               | ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv        |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | roleId                               | productId     | tokenId                               | status    | productRole                 | role            | env      | createdAt                    |
      | d9f8e7c6-1234-45a6-b789-0c1d2e3f4a5b | prod-io       | abc12345-6789-4def-b012-3456789abcd   | ACTIVE    | admin                       | SUB_DELEGATE    | ROOT     | 2023-06-15T14:30:00Z         |


  Scenario: Successfully retrieve users with optional filters (roles filter with two roles)
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                           | a1b2c3d4-5678-90ab-cdef-1234567890ab                  |
    And The following query params:
      | roles                                   | SUB_DELEGATE                                          |
      | roles                                   | MANAGER                                               |
    When I send a GET request to "/institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 2
    And The response body contains at path "" the following list of objects in any order:
      | id                                      | userId                                                | institutionId                             | institutionDescription      | userMailUuid                                        |
      | 65b1234f85a6a37415221ef9                | 97a511a7-2acc-47b9-afed-2f3c65753b4a                  | a1b2c3d4-5678-90ab-cdef-1234567890ab      | Regione Lazio               | ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1        |
      | 65b1214f85b2a37412421ef6                | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                  | a1b2c3d4-5678-90ab-cdef-1234567890ab      | Regione Lazio               | ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv        |

  Scenario: Successfully retrieve users with optional filters (states filter)
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                           | a1b2c3d4-5678-90ab-cdef-1234567890ab                  |
    And The following query params:
      | states                                  | DELETED                                               |
    When I send a GET request to "/institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains at path "" the following list of objects in any order:
      | id                                      | userId                                                | institutionId                             | institutionDescription      | userMailUuid                                        |
      | 65b1234f85a6a37415221ef9                | 97a511a7-2acc-47b9-afed-2f3c65753b4a                  | a1b2c3d4-5678-90ab-cdef-1234567890ab      | Regione Lazio               | ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1        |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | roleId                               | productId    | tokenId                              | status  | productRole              | role    | env  | createdAt                |
      | f1e2d3c4-b567-890a-bcde-1234567890ff | prod-interop | def67890-1234-4abc-5678-90abcdef1234 | DELETED | referente amministrativo | MANAGER | ROOT | 2024-01-20T09:45:10.567Z |

  Scenario: Successfully retrieve users with optional filters (states filter with two states)
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                           | a1b2c3d4-5678-90ab-cdef-1234567890ab                  |
    And The following query params:
      | states                                  | DELETED                                               |
      | states                                  | ACTIVE                                               |
    When I send a GET request to "/institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 2
    And The response body contains at path "" the following list of objects in any order:
      | id                                      | userId                                                | institutionId                             | institutionDescription      | userMailUuid                                        |
      | 65b1234f85a6a37415221ef9                | 97a511a7-2acc-47b9-afed-2f3c65753b4a                  | a1b2c3d4-5678-90ab-cdef-1234567890ab      | Regione Lazio               | ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1        |
      | 65b1214f85b2a37412421ef6                | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                  | a1b2c3d4-5678-90ab-cdef-1234567890ab      | Regione Lazio               | ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv        |

  Scenario: Successfully retrieve users with optional filters (productRoles filter)
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                           | a1b2c3d4-5678-90ab-cdef-1234567890ab                  |
    And The following query params:
      | productRoles                            | referente amministrativo                              |
    When I send a GET request to "/institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains at path "" the following list of objects in any order:
      | id                                      | userId                                                | institutionId                             | institutionDescription      | userMailUuid                                        |
      | 65b1234f85a6a37415221ef9                | 97a511a7-2acc-47b9-afed-2f3c65753b4a                  | a1b2c3d4-5678-90ab-cdef-1234567890ab      | Regione Lazio               | ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1        |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | roleId                               | productId    | tokenId                              | status  | productRole              | role    | env  | createdAt                |
      | f1e2d3c4-b567-890a-bcde-1234567890ff | prod-interop | def67890-1234-4abc-5678-90abcdef1234 | DELETED | referente amministrativo | MANAGER | ROOT | 2024-01-20T09:45:10.567Z |

  Scenario: Successfully retrieve users with optional filters (productRoles filter with two productRoles)
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                           | a1b2c3d4-5678-90ab-cdef-1234567890ab                  |
    And The following query params:
      | productRoles                            | referente amministrativo                              |
      | productRoles                            | admin                                                 |
    When I send a GET request to "/institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 2
    And The response body contains at path "" the following list of objects in any order:
      | id                                      | userId                                                | institutionId                             | institutionDescription      | userMailUuid                                        |
      | 65b1234f85a6a37415221ef9                | 97a511a7-2acc-47b9-afed-2f3c65753b4a                  | a1b2c3d4-5678-90ab-cdef-1234567890ab      | Regione Lazio               | ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1        |
      | 65b1214f85b2a37412421ef6                | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                  | a1b2c3d4-5678-90ab-cdef-1234567890ab      | Regione Lazio               | ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv        |

  Scenario: Successfully retrieve users with optional filters (userId and products filters)
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                           | a1b2c3d4-5678-90ab-cdef-1234567890ab                  |
    And The following query params:
      | userId                                  | 97a511a7-2acc-47b9-afed-2f3c65753b4a                  |
      | products                                | prod-interop                                          |
    When I send a GET request to "/institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains at path "" the following list of objects in any order:
      | id                                      | userId                                                | institutionId                             | institutionDescription      | userMailUuid                                        |
      | 65b1234f85a6a37415221ef9                | 97a511a7-2acc-47b9-afed-2f3c65753b4a                  | a1b2c3d4-5678-90ab-cdef-1234567890ab      | Regione Lazio               | ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1        |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | roleId                               | productId    | tokenId                              | status  | productRole              | role    | env  | createdAt                |
      | f1e2d3c4-b567-890a-bcde-1234567890ff | prod-interop | def67890-1234-4abc-5678-90abcdef1234 | DELETED | referente amministrativo | MANAGER | ROOT | 2024-01-20T09:45:10.567Z |

  Scenario: Successfully retrieve users with optional filters (with wrong institutionId)
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                           | wrongInstitution                                      |
    When I send a GET request to "/institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 0

  Scenario: Successfully retrieve users with optional filters (userId filter with wrong userId)
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                           | a1b2c3d4-5678-90ab-cdef-1234567890ab                  |
    And The following query params:
      | userId                                  | wrongUser                                             |
    When I send a GET request to "/institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 0

  Scenario: Successfully retrieve users with optional filters (roles filter with wrong role)
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                           | a1b2c3d4-5678-90ab-cdef-1234567890ab                  |
    And The following query params:
      | roles                                   | wrongRole                                             |
    When I send a GET request to "/institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 0

  Scenario: Successfully retrieve users with optional filters (states filter with wrong state)
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                           | a1b2c3d4-5678-90ab-cdef-1234567890ab                  |
    And The following query params:
      | states                                  | wrongState                                            |
    When I send a GET request to "/institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 0

  Scenario: Successfully retrieve users with optional filters (products filter with wrong product)
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                           | a1b2c3d4-5678-90ab-cdef-1234567890ab                  |
    And The following query params:
      | products                                | wrongProduct                                          |
    When I send a GET request to "/institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 0

  Scenario: Successfully retrieve users with optional filters (productRoles filter with wrong productRole)
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                           | a1b2c3d4-5678-90ab-cdef-1234567890ab                  |
    And The following query params:
      | productRoles                            | wrongProductRole                                      |
    When I send a GET request to "/institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 0

  Scenario: Bad Token retrieve users with optional filters
    Given A bad jwt token
    And The following path params:
      | institutionId                           | a1b2c3d4-5678-90ab-cdef-1234567890ab                  |
    When I send a GET request to "/institutions/{institutionId}/user-institutions"
    Then The status code is 401

  ######################### END GET /institutions/{institutionId}/user-institutions #########################

  ######################### BEGIN GET /institutions/{institutionId}/products/{productId}/users/count #########################

  Scenario: Successfully get the number of users for a certain product of an institution with a certain role and status
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                           | a1b2c3d4-5678-90ab-cdef-1234567890ab                  |
      | productId                               | prod-io                                               |
    When I send a GET request to "/institutions/{institutionId}/products/{productId}/users/count"
    Then The status code is 200
    And The response body contains:
      | institutionId                           | a1b2c3d4-5678-90ab-cdef-1234567890ab                  |
      | productId                               | prod-io                                               |
      | count                                   | 2                                                     |
    And The response body contains at path "roles" the following list of values in any order:
      | MANAGER                                 |
      | DELEGATE                                |
      | SUB_DELEGATE                            |
      | OPERATOR                                |
      | ADMIN_EA                                |
    And The response body contains at path "status" the following list of values in any order:
      | ACTIVE                                  |

  Scenario: Successfully get the number of users for a certain product of an institution with a certain role and status (with status parameter)
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                           | a1b2c3d4-5678-90ab-cdef-1234567890ab                  |
      | productId                               | prod-interop                                          |
    And The following query params:
      | status                                  | DELETED                                               |
    When I send a GET request to "/institutions/{institutionId}/products/{productId}/users/count"
    Then The status code is 200
    And The response body contains:
      | institutionId                           | a1b2c3d4-5678-90ab-cdef-1234567890ab                  |
      | productId                               | prod-interop                                          |
      | count                                   | 1                                                     |
    And The response body contains at path "roles" the following list of values in any order:
      | MANAGER                                 |
      | DELEGATE                                |
      | SUB_DELEGATE                            |
      | OPERATOR                                |
      | ADMIN_EA                                |
    And The response body contains at path "status" the following list of values in any order:
      | DELETED                                 |

  Scenario: Successfully get the number of users for a certain product of an institution with a certain role and status (with roles parameter)
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                           | a1b2c3d4-5678-90ab-cdef-1234567890ab                  |
      | productId                               | prod-io                                               |
    And The following query params:
      | roles                                   | MANAGER                                               |
    When I send a GET request to "/institutions/{institutionId}/products/{productId}/users/count"
    Then The status code is 200
    And The response body contains:
      | institutionId                           | a1b2c3d4-5678-90ab-cdef-1234567890ab                  |
      | productId                               | prod-io                                               |
      | count                                   | 1                                                     |
    And The response body contains at path "roles" the following list of values in any order:
      | MANAGER                                 |
    And The response body contains at path "status" the following list of values in any order:
      | ACTIVE                                 |

  Scenario: Successfully get the number of users for a certain product of an institution with a certain role and status (with roles and status parameters)
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                           | a1b2c3d4-5678-90ab-cdef-1234567890ab                  |
      | productId                               | prod-io                                               |
    And The following query params:
      | roles                                   | MANAGER                                               |
      | status                                  | ACTIVE                                                |
    When I send a GET request to "/institutions/{institutionId}/products/{productId}/users/count"
    Then The status code is 200
    And The response body contains:
      | institutionId                           | a1b2c3d4-5678-90ab-cdef-1234567890ab                  |
      | productId                               | prod-io                                               |
      | count                                   | 1                                                     |
    And The response body contains at path "roles" the following list of values in any order:
      | MANAGER                                 |
    And The response body contains at path "status" the following list of values in any order:
      | ACTIVE                                 |

  Scenario: Unsuccessfully get the number of users for a certain product of an institution with a certain role and status (with wrong role)
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                           | a1b2c3d4-5678-90ab-cdef-1234567890ab                  |
      | productId                               | prod-io                                               |
    And The following query params:
      | roles                                   | WRONG_ROLE                                            |
    When I send a GET request to "/institutions/{institutionId}/products/{productId}/users/count"
    Then The status code is 400
    And The response body contains string:
      | Invalid value WRONG_ROLE for PartyRole       |

  Scenario: Unsuccessfully get the number of users for a certain product of an institution with a certain role and status (with wrong status)
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                           | a1b2c3d4-5678-90ab-cdef-1234567890ab                  |
      | productId                               | prod-io                                               |
    And The following query params:
      | status                                  | WRONG_STATUS                                          |
    When I send a GET request to "/institutions/{institutionId}/products/{productId}/users/count"
    Then The status code is 400
    And The response body contains string:
      | Invalid value WRONG_STATUS for OnboardedProductState                                            |

  Scenario: Bad Token get the number of users for a certain product of an institution with a certain role and status
    Given A bad jwt token
    And The following path params:
      | institutionId                           | a1b2c3d4-5678-90ab-cdef-1234567890ab                  |
      | productId                               | prod-io                                               |
    When I send a GET request to "/institutions/{institutionId}/products/{productId}/users/count"
    Then The status code is 401

  ######################### END GET /institutions/{institutionId}/products/{productId}/users/count #########################

  ######################### BEGIN PUT /institutions/{institutionId}/products/{productId}/created-at #########################

  @RemoveUserInstitutions
  Scenario: Successfully update user's onboarded product creation date (two userIds)
    Given User login with username "j.doe" and password "test"
    And The following request body:
      """
      {
          "institutionId": "e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21",
          "product": {
              "productId": "prod-io",
              "role": "DELEGATE",
              "tokenId": "7a3df825-8317-4601-9fea-12283b7ed97f",
              "productRoles": [
                  "referente amministrativo"
              ]
          },
          "institutionDescription": "Comune di Bergamo",
          "userMailUuid": "ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1"
      }
      """
    And The following path params:
      | userId                        | 97a511a7-2acc-47b9-afed-2f3c65753b4a          |
    When I send a POST request to "users/{userId}"
    Then The status code is 201
    Given User login with username "j.doe" and password "test"
    And The following request body:
      """
      {
          "institutionId": "e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21",
          "product": {
              "productId": "prod-io",
              "role": "DELEGATE",
              "tokenId": "7a3df825-8317-4601-9fea-12283b7ed97f",
              "productRoles": [
                  "referente amministrativo"
              ]
          },
          "institutionDescription": "Comune di Bergamo",
          "userMailUuid": "ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1"
      }
      """
    And The following path params:
      | userId                        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7          |
    When I send a POST request to "users/{userId}"
    Then The status code is 201
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21           |
      | productId                     | prod-io                                        |
    And The following query params:
      | createdAt                     | 2024-03-18T12:34:56Z                           |
      | userIds                       | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7           |
      | userIds                       | 97a511a7-2acc-47b9-afed-2f3c65753b4a           |
    When I send a PUT request to "/institutions/{institutionId}/products/{productId}/created-at"
    Then The status code is 204
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21           |
    And The following query params:
      | userId                        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7           |
    When I send a GET request to "/institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains at path "" the following list of objects in any order:
      | userId                                         | institutionId                             | institutionDescription          | userMailUuid                                        |
      | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7           | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21      | Comune di Bergamo               | ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1        |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                                      | createdAt                                 |
      | prod-io                                        | 2024-03-18T12:34:56Z                      |
    And The response body contains field "[0].products[0].roleId"
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21           |
    And The following query params:
      | userId                        | 97a511a7-2acc-47b9-afed-2f3c65753b4a           |
    When I send a GET request to "/institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains at path "" the following list of objects in any order:
      | userId                                         | institutionId                             | institutionDescription          | userMailUuid                                        |
      | 97a511a7-2acc-47b9-afed-2f3c65753b4a           | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21      | Comune di Bergamo               | ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1        |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                                      | createdAt                                 |
      | prod-io                                        | 2024-03-18T12:34:56Z                      |
    And The response body contains field "[0].products[0].roleId"

  Scenario: Unsuccessfully update user's onboarded product creation date (wrong userId)
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21           |
      | productId                     | prod-io                                        |
    And The following query params:
      | createdAt                     | 2024-03-18T12:34:56Z                           |
      | userIds                       | wrongUser                                      |
    When I send a PUT request to "/institutions/{institutionId}/products/{productId}/created-at"
    Then The status code is 404
    And The response body contains:
      | detail                        | USERS TO UPDATE NOT FOUND                      |
      | status                        | 404                                            |
      | title                         | USERS TO UPDATE NOT FOUND                      |

  Scenario: Unsuccessfully update user's onboarded product creation date (wrong institutionId)
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | wrongInstitution                               |
      | productId                     | prod-io                                        |
    And The following query params:
      | createdAt                     | 2024-03-18T12:34:56Z                           |
      | userIds                       | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7           |
    When I send a PUT request to "/institutions/{institutionId}/products/{productId}/created-at"
    Then The status code is 404
    And The response body contains:
      | detail                        | USERS TO UPDATE NOT FOUND                      |
      | status                        | 404                                            |
      | title                         | USERS TO UPDATE NOT FOUND                      |

  Scenario: Unsuccessfully update user's onboarded product creation date (wrong productId)
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21           |
      | productId                     | prod-interop-coll                              |
    And The following query params:
      | createdAt                     | 2024-03-18T12:34:56Z                           |
      | userIds                       | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7           |
    When I send a PUT request to "/institutions/{institutionId}/products/{productId}/created-at"
    Then The status code is 404
    And The response body contains:
      | detail                        | USERS TO UPDATE NOT FOUND                      |
      | status                        | 404                                            |
      | title                         | USERS TO UPDATE NOT FOUND                      |

  Scenario: Unsuccessfully update user's onboarded product creation date (wrong createdAt)
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21           |
      | productId                     | prod-io                                        |
    And The following query params:
      | createdAt                     | 2024-03-18T12:34:56A                           |
      | userIds                       | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7           |
    When I send a PUT request to "/institutions/{institutionId}/products/{productId}/created-at"
    Then The status code is 500
    And The response body contains string:
      | Something has gone wrong in the server                                         |

  Scenario: Bad Token update user's onboarded product creation date
    Given A bad jwt token
    And The following path params:
      | institutionId                 | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21           |
      | productId                     | prod-io                                        |
    And The following query params:
      | createdAt                     | 2024-03-18T12:34:56Z                           |
      | userIds                       | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7           |
    When I send a PUT request to "/institutions/{institutionId}/products/{productId}/created-at"
    Then The status code is 401

  ######################### END PUT /institutions/{institutionId}/products/{productId}/created-at #########################

  ######################### BEGIN PUT /institutions/{institutionId} #########################

  @RemoveUserInstitutions
  Scenario: Successfully update institution's description across all userInstitution records
    Given User login with username "j.doe" and password "test"
    And The following request body:
      """
      {
          "institutionId": "e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21",
          "product": {
              "productId": "prod-io",
              "role": "DELEGATE",
              "tokenId": "7a3df825-8317-4601-9fea-12283b7ed97f",
              "productRoles": [
                  "referente amministrativo"
              ]
          },
          "institutionDescription": "Comune di Bergamo",
          "userMailUuid": "ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1"
      }
      """
    And The following path params:
      | userId                        | 97a511a7-2acc-47b9-afed-2f3c65753b4a          |
    When I send a POST request to "users/{userId}"
    Then The status code is 201
    Given User login with username "j.doe" and password "test"
    And The following request body:
      """
      {
          "institutionId": "e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21",
          "product": {
              "productId": "prod-io",
              "role": "DELEGATE",
              "tokenId": "7a3df825-8317-4601-9fea-12283b7ed97f",
              "productRoles": [
                  "referente amministrativo"
              ]
          },
          "institutionDescription": "Comune di Bergamo",
          "userMailUuid": "ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1"
      }
      """
    And The following path params:
      | userId                        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7          |
    When I send a POST request to "users/{userId}"
    Then The status code is 201
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21           |
    When I send a GET request to "/institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 2
    And The response body contains at path "" the following list of objects in any order:
      | userId                                         | institutionId                             | institutionDescription          |
      | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7           | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21      | Comune di Bergamo               |
      | 97a511a7-2acc-47b9-afed-2f3c65753b4a           | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21      | Comune di Bergamo               |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                                  | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21      |
    And The following request body:
      """
      {
          "institutionDescription": "Comune di Pavia",
          "institutionRootName": "Root Name"
      }
      """
    When I send a PUT request to "/institutions/{institutionId}"
    Then The status code is 204
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21           |
    When I send a GET request to "/institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 2
    And The response body contains at path "" the following list of objects in any order:
      | userId                                         | institutionId                             | institutionDescription          | institutionRootName |
      | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7           | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21      | Comune di Pavia                 | Root Name           |
      | 97a511a7-2acc-47b9-afed-2f3c65753b4a           | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21      | Comune di Pavia                 | Root Name           |

  Scenario: Successfully update institution's description across all userInstitution records (with wrong institutionId)
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                                  | wrongInstitution                          |
    And The following request body:
      """
      {
          "institutionDescription": "Comune di Pavia"
      }
      """
    When I send a PUT request to "/institutions/{institutionId}"
    Then The status code is 204

  Scenario: Unsuccessfully update institution's description across all userInstitution records (missing institutionDescription field)
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                                                        | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21       |
    And The following request body:
      """
      {
          "wrongField": "Comune di Pavia"
      }
      """
    When I send a PUT request to "/institutions/{institutionId}"
    Then The status code is 400
    And The response body contains:
      | title                                                                | Constraint Violation                       |
      | status                                                               | 400                                        |
    And The response body contains the list "violations" of size 1
    And The response body contains at path "violations" the following list of objects in any order:
      | field                                                                | message                                    |
      | updateInstitutionDescription.descriptionDto.institutionDescription   | institution's description is required      |

  Scenario: Bad Token update institution's description across all userInstitution records
    Given A bad jwt token
    And The following path params:
      | institutionId                                  | wrongInstitution                          |
    And The following request body:
      """
      {
          "institutionDescription": "Comune di Pavia"
      }
      """
    When I send a PUT request to "/institutions/{institutionId}"
    Then The status code is 401

  ######################### END PUT /institutions/{institutionId} ###########################

  ######## BEGIN POST /institutions/{institutionId}/product/{productId}/check-user ##########

  Scenario: Unsuccessfully retrieve check-user (missing fiscalCode field)
    Given User login with username "r.balboa" and password "test"
    And The following request body:
      """
      {
          "invalidField": "PRVTNT80A41H401T"
      }
      """
    And The following path params:
      | institutionId  | a1b2c3d4-5678-90ab-cdef-1234567890ab  |
      | productId      | prod-io                               |
    When I send a POST request to "institutions/{institutionId}/product/{productId}/check-user"
    Then The status code is 400
    And The response body contains:
      | title                                                                | Constraint Violation                       |
      | status                                                               | 400                                        |
    And The response body contains the list "violations" of size 1
    And The response body contains at path "violations" the following list of objects in any order:
      | field                               | message                  |
      | checkUser.searchUserDto.fiscalCode  | Fiscal code is required  |

  Scenario: Successfully retrieve check user when there's already a user
    Given User login with username "r.balboa" and password "test"
    And The following request body:
      """
      {
          "fiscalCode": "PRVTNT80A41H401T"
      }
      """
    And The following path params:
      | institutionId  | a1b2c3d4-5678-90ab-cdef-1234567890ab  |
      | productId      | prod-io                               |
    When I send a POST request to "institutions/{institutionId}/product/{productId}/check-user"
    Then The status code is 200
    And The response body contains string:
      | true |

  Scenario: Successfully retrieve check user when the user is onboarded but deleted
    Given User login with username "r.balboa" and password "test"
    And The following request body:
      """
      {
          "fiscalCode": "PRVTNT80A41H401T"
      }
      """
    And The following path params:
      | institutionId  | a1b2c3d4-5678-90ab-cdef-1234567890ab  |
      | productId      | prod-interop                          |
    When I send a POST request to "institutions/{institutionId}/product/{productId}/check-user"
    Then The status code is 200
    And The response body contains string:
      | false |

  Scenario: Successfully retrieve check user when the user is not present on pdv
    Given User login with username "r.balboa" and password "test"
    And The following request body:
      """
      {
          "fiscalCode": "CCCVTNT80A41H401C"
      }
      """
    And The following path params:
      | institutionId  | a1b2c3d4-5678-90ab-cdef-1234567890ab  |
      | productId      | prod-interop                          |
    When I send a POST request to "institutions/{institutionId}/product/{productId}/check-user"
    Then The status code is 200
    And The response body contains string:
      | false |

  Scenario: Bad Token while invocking check user
    Given A bad jwt token
    And The following request body:
      """
      {
          "fiscalCode": "PRVTNT80A41H401T",
      }
      """
    And The following path params:
      | institutionId  | a1b2c3d4-5678-90ab-cdef-1234567890ab  |
      | productId      | prod-io                               |
    When I send a POST request to "institutions/{institutionId}/product/{productId}/check-user"
    Then The status code is 401

      ########## END POST /institutions/{institutionId}/product/{productId}/check-user ############