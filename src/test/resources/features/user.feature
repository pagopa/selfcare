Feature: User

  ######################### BEGIN GET /users/email #########################

  Scenario: Successfully get user emails giving productId and institutionId with two results
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId                         | prod-io                                             |
      | institutionId                     | a1b2c3d4-5678-90ab-cdef-1234567890ab                |
    When I send a GET request to "users/emails"
    Then The status code is 200
    And The response body contains the list "" of size 2
    And The response body contains at path "" the following list of values in any order:
      | 81956dd1-00fd-4423-888b-f77a48d26ba1@test.it                                            |
      | r.balboa@regionelazio.it                                                                |

  Scenario: Successfully get user emails giving productId and institutionId
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId                         | prod-pagopa                                         |
      | institutionId                     | d0d28367-1695-4c50-a260-6fda526e9aab                |
    When I send a GET request to "users/emails"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0]                               | 81956dd1-00fd-4423-888b-f77a48d26ba1@test.it        |

  Scenario: Successfully get user emails giving productId and institutionId
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId                         | prod-ciban                                          |
      | institutionId                     | d0d28367-1695-4c50-a260-6fda526e9aab                |
    When I send a GET request to "users/emails"
    Then The status code is 200
    And The response body contains the list "" of size 0

  Scenario: Unsuccessfully get user emails without giving productId and institutionId
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "users/emails"
    Then The status code is 400
    And The response body contains the list "violations" of size 2
    And The response body contains:
      | title                                                   | Constraint Violation          |
      | status                                                  | 400                           |
    And The response body contains at path "violations" the following list of objects in any order:
      | field                                                   | message                       |
      | getUsersEmailByInstitutionAndProduct.institutionId      | must not be null              |
      | getUsersEmailByInstitutionAndProduct.productId          | must not be null              |

  Scenario: Unsuccessfully get user emails giving institutionId and without giving productId
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | institutionId                     | d0d28367-1695-4c50-a260-6fda526e9aab                |
    When I send a GET request to "users/emails"
    Then The status code is 400
    And The response body contains the list "violations" of size 1
    And The response body contains:
      | title                             | Constraint Violation                                |
      | status                            | 400                                                 |
      | violations[0].field               | getUsersEmailByInstitutionAndProduct.productId      |
      | violations[0].message             | must not be null                                    |

  Scenario: Unsuccessfully get user emails giving productId and without giving institutionId
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId                         | prod-ciban                                          |
    When I send a GET request to "users/emails"
    Then The status code is 400
    And The response body contains the list "violations" of size 1
    And The response body contains:
      | title                             | Constraint Violation                                |
      | status                            | 400                                                 |
      | violations[0].field               | getUsersEmailByInstitutionAndProduct.institutionId  |
      | violations[0].message             | must not be null                                    |


  Scenario: Bad Token get user emails giving productId and institutionId
    Given A bad jwt token
    And The following query params:
      | productId                         | prod-ciban                                          |
      | institutionId                     | d0d28367-1695-4c50-a260-6fda526e9aab                |
    When I send a GET request to "users/emails"
    Then The status code is 401

  ######################### END GET /users/email #########################

  ######################### BEGIN GET /users/{id} #########################

  Scenario: Successfully get user info given userId
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id                                                                      | 97a511a7-2acc-47b9-afed-2f3c65753b4a          |
    When I send a GET request to "users/{id}"
    Then The status code is 200
    And The response body contains:
      | id                                                                      | 97a511a7-2acc-47b9-afed-2f3c65753b4a          |
      | workContacts.ID_CONTACTS#8370aa38-a2ab-404b-9b8a-10487167332e           | 8370aa38-a2ab-404b-9b8a-10487167332e@test.it  |
      | workContacts.ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1               | 81956dd1-00fd-4423-888b-f77a48d26ba1@test.it  |
      | workContacts.ID_CONTACTS#875eeb28-2c83-4c0b-8d4d-63ac1b599375           | 875eeb28-2c83-4c0b-8d4d-63ac1b599375@test.it  |

  Scenario: Successfully get user info given userId, institutionId and productId
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id                                                                      | 97a511a7-2acc-47b9-afed-2f3c65753b4a          |
    And The following query params:
      | institutionId                                                           | d0d28367-1695-4c50-a260-6fda526e9aab          |
      | productId                                                               | prod-ciban                                    |
    When I send a GET request to "users/{id}"
    Then The status code is 200
    And The response body contains:
      | id                                                                      | 97a511a7-2acc-47b9-afed-2f3c65753b4a          |
      | workContacts.ID_CONTACTS#8370aa38-a2ab-404b-9b8a-10487167332e           | 8370aa38-a2ab-404b-9b8a-10487167332e@test.it  |
      | workContacts.ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1               | 81956dd1-00fd-4423-888b-f77a48d26ba1@test.it  |
      | workContacts.ID_CONTACTS#875eeb28-2c83-4c0b-8d4d-63ac1b599375           | 875eeb28-2c83-4c0b-8d4d-63ac1b599375@test.it  |

  Scenario: Unsuccessfully get user info given userId and wrong institutionId
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id            | 97a511a7-2acc-47b9-afed-2f3c65753b4a                               |
    And The following query params:
      | institutionId | d0d28367-1695-4c50-a260-aasdasda                                   |
    When I send a GET request to "users/{id}"
    Then The status code is 404
    And The response body contains:
      | detail        | User having userId 97a511a7-2acc-47b9-afed-2f3c65753b4a not found  |
      | status        | 404                                                                |
      | title         | User having userId 97a511a7-2acc-47b9-afed-2f3c65753b4a not found  |

  Scenario: Unsuccessfully get user info given userId and wrong productId
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id            | 97a511a7-2acc-47b9-afed-2f3c65753b4a                               |
    And The following query params:
      | productId     | prod-fd                                                            |
    When I send a GET request to "users/{id}"
    Then The status code is 404
    And The response body contains:
      | detail        | User having userId 97a511a7-2acc-47b9-afed-2f3c65753b4a not found  |
      | status        | 404                                                                |
      | title         | User having userId 97a511a7-2acc-47b9-afed-2f3c65753b4a not found  |

  Scenario: Unsuccessfully get user info given wrong userId
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id            | asdasdasdasd                                                       |
    When I send a GET request to "users/{id}"
    Then The status code is 404
    And The response body contains:
      | detail        | User having userId asdasdasdasd not found                          |
      | status        | 404                                                                |
      | title         | User having userId asdasdasdasd not found                          |

  Scenario: Bad Token get user info given userId
    Given A bad jwt token
    And The following path params:
      | id            | 97a511a7-2acc-47b9-afed-2f3c65753b4a                               |
    When I send a GET request to "users/{id}"
    Then The status code is 401

  ######################### END GET /users/{id} #########################



  ######################### BEGIN GET /{userId}/institutions #########################

  Scenario: Successfully get products info and role which the user is enabled with userId
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | userId        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                               |
    When I send a GET request to "users/{userId}/institutions"
    Then The status code is 200
    And The response body contains the list "institutions" of size 2
    And The response body contains:
      | userId        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                               |
    And The response body contains at path "institutions" the following list of objects in any order:
      | institutionId                                   | institutionName                  | role                    | status             |
      | a1b2c3d4-5678-90ab-cdef-1234567890ab            | Regione Lazio                    | SUB_DELEGATE            | ACTIVE             |
      | f2e4d6c8-9876-5432-ba10-abcdef123456            | Università di Bologna            | MANAGER                 | PENDING            |

  Scenario: Successfully get products info and role which the user is enabled with userId and institutionId
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | userId        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                               |
    And The following query params:
      | institutionId | f2e4d6c8-9876-5432-ba10-abcdef123456                               |
    When I send a GET request to "users/{userId}/institutions"
    Then The status code is 200
    And The response body contains the list "institutions" of size 1
    And The response body contains:
      | userId        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                               |
    And The response body contains at path "institutions" the following list of objects in any order:
      | institutionId                                   | institutionName                  | role                    | status             |
      | f2e4d6c8-9876-5432-ba10-abcdef123456            | Università di Bologna            | MANAGER                 | PENDING            |

  Scenario: Successfully get products info and role which the user is enabled with userId and states
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | userId        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                               |
    And The following query params:
      | states        | ACTIVE                                                             |
    When I send a GET request to "users/{userId}/institutions"
    Then The status code is 200
    And The response body contains the list "institutions" of size 1
    And The response body contains:
      | userId        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                               |
    And The response body contains at path "institutions" the following list of objects in any order:
      | institutionId                                   | institutionName                  | role                   | status              |
      | a1b2c3d4-5678-90ab-cdef-1234567890ab            | Regione Lazio                    | SUB_DELEGATE           | ACTIVE              |

  Scenario: Successfully get products info and role which the user is enabled with userId and two states
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | userId        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                               |
    And The following query params:
      | states        | ACTIVE                                                             |
      | states        | PENDING                                                            |
    When I send a GET request to "users/{userId}/institutions"
    Then The status code is 200
    And The response body contains the list "institutions" of size 2
    And The response body contains:
      | userId        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                               |
    And The response body contains at path "institutions" the following list of objects in any order:
      | institutionId                                   | institutionName                  | role                   | status              |
      | a1b2c3d4-5678-90ab-cdef-1234567890ab            | Regione Lazio                    | SUB_DELEGATE           | ACTIVE              |
      | f2e4d6c8-9876-5432-ba10-abcdef123456            | Università di Bologna            | MANAGER                | PENDING             |

  Scenario: Unsuccessfully get products info and role which the user is enabled with userId and not present state
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | userId                                   | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                                                   |
    And The following query params:
      | states                                   | DELETED                                                                                |
    When I send a GET request to "users/{userId}/institutions"
    Then The status code is 404
    And The response body contains:
      | detail                                   | User with id 35a78332-d038-4bfa-8e85-2cba7f6b7bf7 and institution id null not found!   |
      | status                                   | 404                                                                                    |
      | title                                    | User with id 35a78332-d038-4bfa-8e85-2cba7f6b7bf7 and institution id null not found!   |

  Scenario: Unsuccessfully get products info and role which the user is enabled with userId and wrong institution
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | userId                                   | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                                                        |
    And The following query params:
      | institutionId                            | asdfghjkl                                                                                   |
    When I send a GET request to "users/{userId}/institutions"
    Then The status code is 404
    And The response body contains:
      | detail                                   | User with id 35a78332-d038-4bfa-8e85-2cba7f6b7bf7 and institution id asdfghjkl not found!   |
      | status                                   | 404                                                                                         |
      | title                                    | User with id 35a78332-d038-4bfa-8e85-2cba7f6b7bf7 and institution id asdfghjkl not found!   |

  Scenario: Unsuccessfully get products info and role which the user is enabled with userId and states and wrong institution
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | userId                                   | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                                                        |
    And The following query params:
      | institutionId                            | asdfghjkl                                                                                   |
      | states                                   | ACTIVE                                                                                      |
      | states                                   | PENDING                                                                                     |
    When I send a GET request to "users/{userId}/institutions"
    Then The status code is 404
    And The response body contains:
      | detail                                   | User with id 35a78332-d038-4bfa-8e85-2cba7f6b7bf7 and institution id asdfghjkl not found!   |
      | status                                   | 404                                                                                         |
      | title                                    | User with id 35a78332-d038-4bfa-8e85-2cba7f6b7bf7 and institution id asdfghjkl not found!   |

  Scenario: Unsuccessfully get products info and role which the user is enabled with userId and wrong state
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | userId                                   | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7               |
    And The following query params:
      | states                                   | ASDFGH                                             |
    When I send a GET request to "users/{userId}/institutions"
    Then The status code is 500
    And The response body contains string:
      | Something has gone wrong in the server                                                        |

  Scenario: Unsuccessfully get products info and role which the user is enabled with wrong userId
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | userId                                   | asdfghjkl                                          |
    When I send a GET request to "users/{userId}/institutions"
    Then The status code is 404
    And The response body contains:
      | detail                                   | User with id asdfghjkl not found!                  |
      | status                                   | 404                                                |
      | title                                    | User with id asdfghjkl not found!                  |

  Scenario: Bad Token get products info and role which the user is enabled with userId
    Given A bad jwt token
    And The following path params:
      | userId                                   | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7               |
    When I send a GET request to "users/{userId}/institutions"
    Then The status code is 401

  ######################### END GET /{userId}/institutions #########################


  ######################### BEGIN GET /{id}/details #########################

  Scenario: Successfully get user's information from pdv: name, familyName, email, fiscalCode and workContacts
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id                                                                                | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7          |
    When I send a GET request to "users/{id}/details"
    Then The status code is 200
    And The response body contains:
      | id                                                                                | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7          |
      | fiscalCode                                                                        | blbrki80A41H401T                              |
      | name.value                                                                        | rocky                                         |
      | name.certified                                                                    | SPID                                          |
      | familyName.value                                                                  | Balboa                                        |
      | familyName.certified                                                              | SPID                                          |
      | workContacts.ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv.email.value             | r.balboa@regionelazio.it                      |
      | workContacts.ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv.email.certified         | NONE                                          |
      | workContacts.ID_CONTACTS#35a78332-d038-4bfa-8e85-2cba7f6b7bf7.email.value         | 8370aa38-a2ab-404b-9b8a-10487167332e@test.it  |
      | workContacts.ID_CONTACTS#35a78332-d038-4bfa-8e85-2cba7f6b7bf7.email.certified     | NONE                                          |
      | workContacts.ID_MAIL#123123-55555-efaz-12312-apclacpela.email.value               | r.balboa@unibologna.it                        |
      | workContacts.ID_MAIL#123123-55555-efaz-12312-apclacpela.email.certified           | NONE                                          |

  Scenario: Successfully get user's information from pdv with field name
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id                                                                                | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7          |
    And The following query params:
      | field                                                                             | name                                          |
    When I send a GET request to "users/{id}/details"
    Then The status code is 200
    And The response body contains:
      | id                                                                                | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7          |
      | name.value                                                                        | rocky                                         |
      | name.certified                                                                    | SPID                                          |
    And The response body doesn't contain field "familyName.value"
    And The response body doesn't contain field "familyName.certified"
    And The response body doesn't contain field "fiscalCode"
    And The response body doesn't contain field "workContacts.ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv.email.value"
    And The response body doesn't contain field "workContacts.ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv.email.certified"

  Scenario: Successfully get user's information from pdv with field familyName
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id                                                                                | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7          |
    And The following query params:
      | field                                                                             | familyName                                    |
    When I send a GET request to "users/{id}/details"
    Then The status code is 200
    And The response body contains:
      | id                                                                                | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7          |
      | familyName.value                                                                  | Balboa                                        |
      | familyName.certified                                                              | SPID                                          |
    And The response body doesn't contain field "name.value"
    And The response body doesn't contain field "name.certified"
    And The response body doesn't contain field "fiscalCode"
    And The response body doesn't contain field "workContacts.ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv.email.value"
    And The response body doesn't contain field "workContacts.ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv.email.certified"

  Scenario: Successfully get user's information from pdv with field workContracts
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id                                                                                | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7          |
    And The following query params:
      | field                                                                             | workContracts                                 |
    When I send a GET request to "users/{id}/details"
    Then The status code is 200
    And The response body contains:
      | id                                                                                | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7          |
      | workContacts.ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv.email.value             | r.balboa@regionelazio.it                      |
      | workContacts.ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv.email.certified         | NONE                                          |
      | workContacts.ID_CONTACTS#35a78332-d038-4bfa-8e85-2cba7f6b7bf7.email.value         | 8370aa38-a2ab-404b-9b8a-10487167332e@test.it  |
      | workContacts.ID_CONTACTS#35a78332-d038-4bfa-8e85-2cba7f6b7bf7.email.certified     | NONE                                          |
      | workContacts.ID_MAIL#123123-55555-efaz-12312-apclacpela.email.value               | r.balboa@unibologna.it                        |
      | workContacts.ID_MAIL#123123-55555-efaz-12312-apclacpela.email.certified           | NONE                                          |
    And The response body doesn't contain field "name.value"
    And The response body doesn't contain field "name.certified"
    And The response body doesn't contain field "familyName.value"
    And The response body doesn't contain field "familyName.certified"
    And The response body doesn't contain field "fiscalCode"

  Scenario: Successfully get user's information from pdv with field fiscalCode
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id                                                                                | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7          |
    And The following query params:
      | field                                                                             | fiscalCode                                    |
    When I send a GET request to "users/{id}/details"
    Then The status code is 200
    And The response body contains:
      | id                                                                                | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7          |
      | fiscalCode                                                                        | blbrki80A41H401T                              |
    And The response body doesn't contain field "name.value"
    And The response body doesn't contain field "name.certified"
    And The response body doesn't contain field "familyName.value"
    And The response body doesn't contain field "familyName.certified"
    And The response body doesn't contain field "workContacts.ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv.email.value"
    And The response body doesn't contain field "workContacts.ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv.email.certified"

  Scenario: Successfully get user's information from pdv with field fiscalCode and name
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id                                                                                | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7          |
    And The following query params:
      | field                                                                             | fiscalCode,name                               |
    When I send a GET request to "users/{id}/details"
    Then The status code is 200
    And The response body contains:
      | id                                                                                | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7          |
      | name.value                                                                        | rocky                                         |
      | name.certified                                                                    | SPID                                          |
      | fiscalCode                                                                        | blbrki80A41H401T                              |
    And The response body doesn't contain field "familyName.value"
    And The response body doesn't contain field "familyName.certified"
    And The response body doesn't contain field "workContacts.ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv.email.value"
    And The response body doesn't contain field "workContacts.ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv.email.certified"

  Scenario: Successfully get user's information from pdv with field fiscalCode, name and familyName
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id                                                                                | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7          |
    And The following query params:
      | field                                                                             | fiscalCode,name,familyName                    |
    When I send a GET request to "users/{id}/details"
    Then The status code is 200
    And The response body contains:
      | id                                                                                | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7          |
      | name.value                                                                        | rocky                                         |
      | name.certified                                                                    | SPID                                          |
      | fiscalCode                                                                        | blbrki80A41H401T                              |
      | familyName.value                                                                  | Balboa                                        |
      | familyName.certified                                                              | SPID                                          |
    And The response body doesn't contain field "workContacts.ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv.email.value"
    And The response body doesn't contain field "workContacts.ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv.email.certified"

  Scenario: Successfully get user's information from pdv: name, familyName, email, fiscalCode and workContacts with parameter institutionId
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id                                                                                | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7          |
    And The following query params:
      | institutionId                                                                     | f2e4d6c8-9876-5432-ba10-abcdef123456          |
    When I send a GET request to "users/{id}/details"
    Then The status code is 200
    And The response body contains:
      | id                                                                                | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7          |
      | fiscalCode                                                                        | blbrki80A41H401T                              |
      | name.value                                                                        | rocky                                         |
      | name.certified                                                                    | SPID                                          |
      | familyName.value                                                                  | Balboa                                        |
      | familyName.certified                                                              | SPID                                          |
      | workContacts.ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv.email.value             | r.balboa@regionelazio.it                      |
      | workContacts.ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv.email.certified         | NONE                                          |
      | workContacts.ID_CONTACTS#35a78332-d038-4bfa-8e85-2cba7f6b7bf7.email.value         | 8370aa38-a2ab-404b-9b8a-10487167332e@test.it  |
      | workContacts.ID_CONTACTS#35a78332-d038-4bfa-8e85-2cba7f6b7bf7.email.certified     | NONE                                          |
      | workContacts.ID_MAIL#123123-55555-efaz-12312-apclacpela.email.value               | r.balboa@unibologna.it                        |
      | workContacts.ID_MAIL#123123-55555-efaz-12312-apclacpela.email.certified           | NONE                                          |
      | email.value                                                                       | r.balboa@unibologna.it                        |
      | email.certified                                                                   | NONE                                          |

  Scenario: Successfully get user's information from pdv: name, familyName, email, fiscalCode and workContacts with parameter institutionId
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id                                                                                | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7          |
    And The following query params:
      | institutionId                                                                     | a1b2c3d4-5678-90ab-cdef-1234567890ab          |
    When I send a GET request to "users/{id}/details"
    Then The status code is 200
    And The response body contains:
      | id                                                                                | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7          |
      | fiscalCode                                                                        | blbrki80A41H401T                              |
      | name.value                                                                        | rocky                                         |
      | name.certified                                                                    | SPID                                          |
      | familyName.value                                                                  | Balboa                                        |
      | familyName.certified                                                              | SPID                                          |
      | workContacts.ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv.email.value             | r.balboa@regionelazio.it                      |
      | workContacts.ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv.email.certified         | NONE                                          |
      | workContacts.ID_CONTACTS#35a78332-d038-4bfa-8e85-2cba7f6b7bf7.email.value         | 8370aa38-a2ab-404b-9b8a-10487167332e@test.it  |
      | workContacts.ID_CONTACTS#35a78332-d038-4bfa-8e85-2cba7f6b7bf7.email.certified     | NONE                                          |
      | workContacts.ID_MAIL#123123-55555-efaz-12312-apclacpela.email.value               | r.balboa@unibologna.it                        |
      | workContacts.ID_MAIL#123123-55555-efaz-12312-apclacpela.email.certified           | NONE                                          |
      | email.value                                                                       | r.balboa@regionelazio.it                      |
      | email.certified                                                                   | NONE                                          |


  Scenario: Successfully get user's information from pdv: name, familyName, email, fiscalCode and workContacts with wrong parameter institutionId
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id                           | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7               |
    And The following query params:
      | institutionId                | asdasdasdasd                                       |
    When I send a GET request to "users/{id}/details"
    Then The status code is 200
    And The response body contains:
      | id                                                                                | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7          |
      | fiscalCode                                                                        | blbrki80A41H401T                              |
      | name.value                                                                        | rocky                                         |
      | name.certified                                                                    | SPID                                          |
      | familyName.value                                                                  | Balboa                                        |
      | familyName.certified                                                              | SPID                                          |
      | workContacts.ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv.email.value             | r.balboa@regionelazio.it                      |
      | workContacts.ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv.email.certified         | NONE                                          |
      | workContacts.ID_CONTACTS#35a78332-d038-4bfa-8e85-2cba7f6b7bf7.email.value         | 8370aa38-a2ab-404b-9b8a-10487167332e@test.it  |
      | workContacts.ID_CONTACTS#35a78332-d038-4bfa-8e85-2cba7f6b7bf7.email.certified     | NONE                                          |
      | workContacts.ID_MAIL#123123-55555-efaz-12312-apclacpela.email.value               | r.balboa@unibologna.it                        |
      | workContacts.ID_MAIL#123123-55555-efaz-12312-apclacpela.email.certified           | NONE                                          |
    And The response body doesn't contain field "email.value"
    And The response body doesn't contain field "email.certified"

  Scenario: Unsuccessfully get user's information from pdv: name, familyName, email, fiscalCode and workContacts with wrong parameter field
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id                                          | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7      |
    And The following query params:
      | field                                       | wrongfield                                |
    When I send a GET request to "users/{id}/details"
    Then The status code is 500
    And The response body contains string:
      | Something has gone wrong in the server                                                  |

  Scenario: Unsuccessfully get user's information from pdv: name, familyName, email, fiscalCode and workContacts with wrong userId
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id                                          | wronguserid                               |
    When I send a GET request to "users/{id}/details"
    Then The status code is 500
    And The response body contains string:
      | Something has gone wrong in the server      |

  Scenario: Bad Token get user's information from pdv: name, familyName, email, fiscalCode and workContacts
    Given A bad jwt token
    And The following path params:
      | id                                          | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7      |
    When I send a GET request to "users/{id}/details"
    Then The status code is 401


  ######################### END GET /{id}/details #########################


  ######################### BEGIN POST /search #########################

  Scenario: Successfully search user by fiscalCode
    Given User login with username "j.doe" and password "test"
    And The following request body:
      """
      {
          "fiscalCode": "blbrki80A41H401T"
      }
      """
    When I send a POST request to "users/search"
    Then The status code is 200
    And The response body contains:
      | id                                                                                | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7          |
      | name.value                                                                        | rocky                                         |
      | name.certified                                                                    | SPID                                          |
      | familyName.value                                                                  | Balboa                                        |
      | familyName.certified                                                              | SPID                                          |
      | workContacts.ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv.email.value             | r.balboa@regionelazio.it                      |
      | workContacts.ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv.email.certified         | NONE                                          |
      | workContacts.ID_CONTACTS#35a78332-d038-4bfa-8e85-2cba7f6b7bf7.email.value         | 8370aa38-a2ab-404b-9b8a-10487167332e@test.it  |
      | workContacts.ID_CONTACTS#35a78332-d038-4bfa-8e85-2cba7f6b7bf7.email.certified     | NONE                                          |
      | workContacts.ID_MAIL#123123-55555-efaz-12312-apclacpela.email.value               | r.balboa@unibologna.it                        |
      | workContacts.ID_MAIL#123123-55555-efaz-12312-apclacpela.email.certified           | NONE                                          |
    And The response body doesn't contain field "fiscalCode"

  Scenario: Successfully search user by fiscalCode and institutionId
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | institutionId                                                                     | f2e4d6c8-9876-5432-ba10-abcdef123456          |
    And The following request body:
      """
      {
          "fiscalCode": "blbrki80A41H401T"
      }
      """
    When I send a POST request to "users/search"
    Then The status code is 200
    And The response body contains:
      | id                                                                                | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7          |
      | name.value                                                                        | rocky                                         |
      | name.certified                                                                    | SPID                                          |
      | familyName.value                                                                  | Balboa                                        |
      | familyName.certified                                                              | SPID                                          |
      | workContacts.ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv.email.value             | r.balboa@regionelazio.it                      |
      | workContacts.ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv.email.certified         | NONE                                          |
      | workContacts.ID_CONTACTS#35a78332-d038-4bfa-8e85-2cba7f6b7bf7.email.value         | 8370aa38-a2ab-404b-9b8a-10487167332e@test.it  |
      | workContacts.ID_CONTACTS#35a78332-d038-4bfa-8e85-2cba7f6b7bf7.email.certified     | NONE                                          |
      | workContacts.ID_MAIL#123123-55555-efaz-12312-apclacpela.email.value               | r.balboa@unibologna.it                        |
      | workContacts.ID_MAIL#123123-55555-efaz-12312-apclacpela.email.certified           | NONE                                          |
      | email.value                                                                       | r.balboa@unibologna.it                        |
      | email.certified                                                                   | NONE                                          |
    And The response body doesn't contain field "fiscalCode"

  Scenario: Successfully search user by fiscalCode and institutionId
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | institutionId                                                                     | a1b2c3d4-5678-90ab-cdef-1234567890ab          |
    And The following request body:
      """
      {
          "fiscalCode": "blbrki80A41H401T"
      }
      """
    When I send a POST request to "users/search"
    Then The status code is 200
    And The response body contains:
      | id                                                                                | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7          |
      | name.value                                                                        | rocky                                         |
      | name.certified                                                                    | SPID                                          |
      | familyName.value                                                                  | Balboa                                        |
      | familyName.certified                                                              | SPID                                          |
      | workContacts.ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv.email.value             | r.balboa@regionelazio.it                      |
      | workContacts.ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv.email.certified         | NONE                                          |
      | workContacts.ID_CONTACTS#35a78332-d038-4bfa-8e85-2cba7f6b7bf7.email.value         | 8370aa38-a2ab-404b-9b8a-10487167332e@test.it  |
      | workContacts.ID_CONTACTS#35a78332-d038-4bfa-8e85-2cba7f6b7bf7.email.certified     | NONE                                          |
      | workContacts.ID_MAIL#123123-55555-efaz-12312-apclacpela.email.value               | r.balboa@unibologna.it                        |
      | workContacts.ID_MAIL#123123-55555-efaz-12312-apclacpela.email.certified           | NONE                                          |
      | email.value                                                                       | r.balboa@regionelazio.it                      |
      | email.certified                                                                   | NONE                                          |
    And The response body doesn't contain field "fiscalCode"

  Scenario: Successfully search user by fiscalCode and wrong institutionId
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | institutionId                                                                     | asdasdasd                                     |
    And The following request body:
      """
      {
          "fiscalCode": "blbrki80A41H401T"
      }
      """
    When I send a POST request to "users/search"
    Then The status code is 200
    And The response body contains:
      | id                                                                                | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7          |
      | name.value                                                                        | rocky                                         |
      | name.certified                                                                    | SPID                                          |
      | familyName.value                                                                  | Balboa                                        |
      | familyName.certified                                                              | SPID                                          |
      | workContacts.ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv.email.value             | r.balboa@regionelazio.it                      |
      | workContacts.ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv.email.certified         | NONE                                          |
      | workContacts.ID_CONTACTS#35a78332-d038-4bfa-8e85-2cba7f6b7bf7.email.value         | 8370aa38-a2ab-404b-9b8a-10487167332e@test.it  |
      | workContacts.ID_CONTACTS#35a78332-d038-4bfa-8e85-2cba7f6b7bf7.email.certified     | NONE                                          |
      | workContacts.ID_MAIL#123123-55555-efaz-12312-apclacpela.email.value               | r.balboa@unibologna.it                        |
      | workContacts.ID_MAIL#123123-55555-efaz-12312-apclacpela.email.certified           | NONE                                          |
    And The response body doesn't contain field "fiscalCode"
    And The response body doesn't contain field "email.value"
    And The response body doesn't contain field "email.certified"

  Scenario: Unsuccessfully search user by wrong fiscalCode
    Given User login with username "j.doe" and password "test"
    And The following request body:
      """
      {
          "fiscalCode": "wrongfiscalcode"
      }
      """
    When I send a POST request to "users/search"
    Then The status code is 404
    And The response body contains:
      | detail                                            | User not found          |
      | status                                            | 404                     |
      | title                                             | User not found          |

  Scenario: Unsuccessfully search user without body
    Given User login with username "j.doe" and password "test"
    And The following request body:
      """
      {
      }
      """
    When I send a POST request to "users/search"
    Then The status code is 500
    And The response body contains string:
      | Something has gone wrong in the server            |

  Scenario: Unsuccessfully search user without wrong body field
    Given User login with username "j.doe" and password "test"
    And The following request body:
      """
      {
        "wrongfield": "blbrki80A41H401T"
      }
      """
    When I send a POST request to "users/search"
    Then The status code is 500
    And The response body contains string:
      | Something has gone wrong in the server            |

  Scenario: Bad Token search user by fiscalCode
    Given A bad jwt token
    And The following request body:
      """
      {
          "fiscalCode": "blbrki80A41H401T"
      }
      """
    When I send a POST request to "users/search"
    Then The status code is 401

  ######################### END POST /search #########################

  ######################### BEGIN DELETE /{userId}/institutions/{institutionId}/products/{productId} #########################

  # Cancellazione riuscita
  @RemoveUserInstitutionAndUserInfoAfterScenario
  Scenario: Successfully delete logically the association institution and product
    Given User login with username "j.doe" and password "test"
    And A mock userInstitution with id "65a4b6c7d8e9f01234567890" and onboardedProductState "ACTIVE" and role "SUB_DELEGATE" and productId "prod-pagopa"
    And A mock userInfo with id "d0d28367-1695-4c50-a260-6fda526e9aab", institutionName "Comune di Milano", status "ACTIVE", role "SUB_DELEGATE" to userInfo document with id "35a78332-d038-4bfa-8e85-2cba7f6b7bf7"
    And The following path params:
      | institutionId              |  d0d28367-1695-4c50-a260-6fda526e9aab                |
    And The following query params:
      | userId                     | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                 |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].id                     | 65a4b6c7d8e9f01234567890                             |
    And The response body contains at path "[0].products" the following list of objects in any order:
     | status                      |
     | ACTIVE                      |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | userId                     | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                 |
      | institutionId              | d0d28367-1695-4c50-a260-6fda526e9aab                 |
      | productId                  | prod-pagopa                                          |
    When I send a DELETE request to "users/{userId}/institutions/{institutionId}/products/{productId}"
    Then The status code is 204
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId              |  d0d28367-1695-4c50-a260-6fda526e9aab                |
    And The following query params:
      | userId                     | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                 |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].id                     | 65a4b6c7d8e9f01234567890                             |
    And The response body contains at path "[0].products" the following list of objects in any order:
      | status                                                                            |
      | DELETED                                                                           |

  # Cancellazione di prodotto già cancellato
  @RemoveUserInstitutionAndUserInfoAfterScenario
  Scenario: Unsuccessfully delete logically the association institution and product because product is already deleted
    Given User login with username "j.doe" and password "test"
    And A mock userInstitution with id "65a4b6c7d8e9f01234567890" and onboardedProductState "DELETED" and role "SUB_DELEGATE" and productId "prod-pagopa"
    And A mock userInfo with id "d0d28367-1695-4c50-a260-6fda526e9aab", institutionName "Comune di Milano", status "DELETED", role "SUB_DELEGATE" to userInfo document with id "35a78332-d038-4bfa-8e85-2cba7f6b7bf7"
    And The following path params:
      | institutionId              |  d0d28367-1695-4c50-a260-6fda526e9aab               |
    And The following query params:
      | userId                     | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].id                     | 65a4b6c7d8e9f01234567890                            |
    And The response body contains at path "[0].products" the following list of objects in any order:
      | status                     |
      | DELETED                    |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | userId                     | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                |
      | institutionId              | d0d28367-1695-4c50-a260-6fda526e9aab                |
      | productId                  | prod-pagopa                                         |
    When I send a DELETE request to "users/{userId}/institutions/{institutionId}/products/{productId}"
    Then The status code is 404
    And The response body contains:
      | detail                     | USER TO UPDATE NOT FOUND                            |
      | status                     | 404                                                 |
      | title                      | USER TO UPDATE NOT FOUND                            |

  # productid inesistente per utente
  Scenario: Unsuccessfully delete logically the association institution and product because user is not registered with selected product
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | userId                     | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                |
      | institutionId              | f2e4d6c8-9876-5432-ba10-abcdef123456                |
      | productId                  | prod-pagopa                                         |
    When I send a DELETE request to "users/{userId}/institutions/{institutionId}/products/{productId}"
    Then The status code is 404
    And The response body contains:
      | detail                     | USER TO UPDATE NOT FOUND                            |
      | status                     | 404                                                 |
      | title                      | USER TO UPDATE NOT FOUND                            |

  # productid sbagliato
  Scenario: Unsuccessfully delete logically the association institution and product because of wrong product id
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | userId                     | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                |
      | institutionId              | f2e4d6c8-9876-5432-ba10-abcdef123456                |
      | productId                  | prod-wrongproductid                                 |
    When I send a DELETE request to "users/{userId}/institutions/{institutionId}/products/{productId}"
    Then The status code is 404
    And The response body contains:
      | detail                     | USER TO UPDATE NOT FOUND                            |
      | status                     | 404                                                 |
      | title                      | USER TO UPDATE NOT FOUND                            |

  # institutionid inesistente per utente
  Scenario: Unsuccessfully delete logically the association institution and product because of wrong institutionId
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | userId                     | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                |
      | institutionId              | wronginstitutionid                                  |
      | productId                  | prod-io                                             |
    When I send a DELETE request to "users/{userId}/institutions/{institutionId}/products/{productId}"
    Then The status code is 404
    And The response body contains:
      | detail                     | USER TO UPDATE NOT FOUND                            |
      | status                     | 404                                                 |
      | title                      | USER TO UPDATE NOT FOUND                            |

  # userid inesistente
  Scenario: Unsuccessfully delete logically the association institution and product because of wrong userid
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | userId                     | wronguserid                                         |
      | institutionId              | f2e4d6c8-9876-5432-ba10-abcdef123456                |
      | productId                  | prod-pagopa                                         |
    When I send a DELETE request to "users/{userId}/institutions/{institutionId}/products/{productId}"
    Then The status code is 404
    And The response body contains:
      | detail                     | USER TO UPDATE NOT FOUND                            |
      | status                     | 404                                                 |
      | title                      | USER TO UPDATE NOT FOUND                            |

  # senza jwt
  Scenario: Bad Token delete logically the association institution and product
    Given A bad jwt token
    And The following path params:
      | userId                     | wronguserid                                         |
      | institutionId              | f2e4d6c8-9876-5432-ba10-abcdef123456                |
      | productId                  | prod-pagopa                                         |
    When I send a DELETE request to "users/{userId}/institutions/{institutionId}/products/{productId}"
    Then The status code is 401

  ######################### END DELETE /{userId}/institutions/{institutionId}/products/{productId} #########################

  ######################### BEGIN PUT /{id}/status #########################

  # Cambio stato corretto
  @RemoveUserInstitutionAndUserInfoAfterScenarioWithUnusedUser
  Scenario: Successfully update user status with status (from ACTIVE to DELETED with multiple userInstitution)
    Given User login with username "j.doe" and password "test"
    And A mock userInstitution with id "65a4b6c7d8e9f01234567890" and onboardedProductState "ACTIVE" and role "SUB_DELEGATE" and productId "prod-pagopa" and institutionId "d0d28367-1695-4c50-a260-6fda526e9aab" and institutionDescription "Comune di Milano" and unused user
    And A mock userInstitution with id "78a2342c31599e1812b1819a" and onboardedProductState "ACTIVE" and role "MANAGER" and productId "prod-io" and institutionId "e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21" and institutionDescription "Comune di Lucca" and unused user
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | d0d28367-1695-4c50-a260-6fda526e9aab          |
    And The following query params:
      | userId                        | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
      | [0].institutionId             | d0d28367-1695-4c50-a260-6fda526e9aab          |
      | [0].institutionDescription    | Comune di Milano                              |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | role         | status     |
      | prod-pagopa                   | SUB_DELEGATE | ACTIVE     |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21          |
    And The following query params:
      | userId                        | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
      | [0].institutionId             | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21          |
      | [0].institutionDescription    | Comune di Lucca                               |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | role         | status     |
      | prod-io                       | MANAGER      | ACTIVE     |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id              | 12a521c9-3agc-43l5-adee-3d1c95310b1e                        |
    And The following query params:
      | status          | DELETED                                                     |
    When I send a PUT request to "users/{id}/status"
    Then The status code is 204
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | d0d28367-1695-4c50-a260-6fda526e9aab          |
    And The following query params:
      | userId                        | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
      | [0].institutionId             | d0d28367-1695-4c50-a260-6fda526e9aab          |
      | [0].institutionDescription    | Comune di Milano                              |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | role         | status     |
      | prod-pagopa                   | SUB_DELEGATE | DELETED    |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21          |
    And The following query params:
      | userId                        | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
      | [0].institutionId             | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21          |
      | [0].institutionDescription    | Comune di Lucca                               |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | role         | status     |
      | prod-io                       | MANAGER      | DELETED    |

  # Cambio stato con filtro institutionId
  @RemoveUserInstitutionAndUserInfoAfterScenarioWithUnusedUser
  Scenario: Successfully update user status with status (from ACTIVE to DELETED with multiple userInstitution and institutionId filter)
    Given User login with username "j.doe" and password "test"
    And A mock userInstitution with id "65a4b6c7d8e9f01234567890" and onboardedProductState "ACTIVE" and role "SUB_DELEGATE" and productId "prod-pagopa" and institutionId "d0d28367-1695-4c50-a260-6fda526e9aab" and institutionDescription "Comune di Milano" and unused user
    And A mock userInstitution with id "78a2342c31599e1812b1819a" and onboardedProductState "ACTIVE" and role "MANAGER" and productId "prod-io" and institutionId "e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21" and institutionDescription "Comune di Lucca" and unused user
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | d0d28367-1695-4c50-a260-6fda526e9aab          |
    And The following query params:
      | userId                        | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
      | [0].institutionId             | d0d28367-1695-4c50-a260-6fda526e9aab          |
      | [0].institutionDescription    | Comune di Milano                              |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | role         | status     |
      | prod-pagopa                   | SUB_DELEGATE | ACTIVE     |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21          |
    And The following query params:
      | userId                        | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
      | [0].institutionId             | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21          |
      | [0].institutionDescription    | Comune di Lucca                               |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | role         | status     |
      | prod-io                       | MANAGER      | ACTIVE     |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id              | 12a521c9-3agc-43l5-adee-3d1c95310b1e                        |
    And The following query params:
      | status          | DELETED                                                     |
      | institutionId   | d0d28367-1695-4c50-a260-6fda526e9aab                        |
    When I send a PUT request to "users/{id}/status"
    Then The status code is 204
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | d0d28367-1695-4c50-a260-6fda526e9aab          |
    And The following query params:
      | userId                        | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
      | [0].institutionId             | d0d28367-1695-4c50-a260-6fda526e9aab          |
      | [0].institutionDescription    | Comune di Milano                              |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | role         | status     |
      | prod-pagopa                   | SUB_DELEGATE | DELETED    |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21          |
    And The following query params:
      | userId                        | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
      | [0].institutionId             | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21          |
      | [0].institutionDescription    | Comune di Lucca                               |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | role         | status     |
      | prod-io                       | MANAGER      | ACTIVE     |

  # Cambio stato con filtro productId
  @RemoveUserInstitutionAndUserInfoAfterScenarioWithUnusedUser
  Scenario: Successfully update user status with status (from ACTIVE to DELETED with multiple userInstitution and productId filter)
    Given User login with username "j.doe" and password "test"
    And A mock userInstitution with id "65a4b6c7d8e9f01234567890" and onboardedProductState "ACTIVE" and role "SUB_DELEGATE" and productId "prod-pagopa" and institutionId "d0d28367-1695-4c50-a260-6fda526e9aab" and institutionDescription "Comune di Milano" and unused user
    And A mock userInstitution with id "78a2342c31599e1812b1819a" and onboardedProductState "ACTIVE" and role "MANAGER" and productId "prod-io" and institutionId "e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21" and institutionDescription "Comune di Lucca" and unused user
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | d0d28367-1695-4c50-a260-6fda526e9aab          |
    And The following query params:
      | userId                        | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
      | [0].institutionId             | d0d28367-1695-4c50-a260-6fda526e9aab          |
      | [0].institutionDescription    | Comune di Milano                              |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | role         | status     |
      | prod-pagopa                   | SUB_DELEGATE | ACTIVE     |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21          |
    And The following query params:
      | userId                        | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
      | [0].institutionId             | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21          |
      | [0].institutionDescription    | Comune di Lucca                               |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | role         | status     |
      | prod-io                       | MANAGER      | ACTIVE     |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id              | 12a521c9-3agc-43l5-adee-3d1c95310b1e                        |
    And The following query params:
      | status          | DELETED                                                     |
      | productId       | prod-pagopa                                                 |
    When I send a PUT request to "users/{id}/status"
    Then The status code is 204
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | d0d28367-1695-4c50-a260-6fda526e9aab          |
    And The following query params:
      | userId                        | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
      | [0].institutionId             | d0d28367-1695-4c50-a260-6fda526e9aab          |
      | [0].institutionDescription    | Comune di Milano                              |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | role         | status     |
      | prod-pagopa                   | SUB_DELEGATE | DELETED    |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21          |
    And The following query params:
      | userId                        | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
      | [0].institutionId             | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21          |
      | [0].institutionDescription    | Comune di Lucca                               |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | role         | status     |
      | prod-io                       | MANAGER      | ACTIVE     |

  # Cambio stato con filtro role
  @RemoveUserInstitutionAndUserInfoAfterScenarioWithUnusedUser
  Scenario: Successfully update user status with status (from ACTIVE to DELETED with multiple userInstitution and role filter)
    Given User login with username "j.doe" and password "test"
    And A mock userInstitution with id "65a4b6c7d8e9f01234567890" and onboardedProductState "ACTIVE" and role "SUB_DELEGATE" and productId "prod-pagopa" and institutionId "d0d28367-1695-4c50-a260-6fda526e9aab" and institutionDescription "Comune di Milano" and unused user
    And A mock userInstitution with id "78a2342c31599e1812b1819a" and onboardedProductState "ACTIVE" and role "MANAGER" and productId "prod-io" and institutionId "e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21" and institutionDescription "Comune di Lucca" and unused user
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | d0d28367-1695-4c50-a260-6fda526e9aab          |
    And The following query params:
      | userId                        | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
      | [0].institutionId             | d0d28367-1695-4c50-a260-6fda526e9aab          |
      | [0].institutionDescription    | Comune di Milano                              |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | role         | status     |
      | prod-pagopa                   | SUB_DELEGATE | ACTIVE     |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21          |
    And The following query params:
      | userId                        | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
      | [0].institutionId             | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21          |
      | [0].institutionDescription    | Comune di Lucca                               |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | role         | status     |
      | prod-io                       | MANAGER      | ACTIVE     |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id              | 12a521c9-3agc-43l5-adee-3d1c95310b1e                        |
    And The following query params:
      | status          | DELETED                                                     |
      | role            | SUB_DELEGATE                                                |
    When I send a PUT request to "users/{id}/status"
    Then The status code is 204
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | d0d28367-1695-4c50-a260-6fda526e9aab          |
    And The following query params:
      | userId                        | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
      | [0].institutionId             | d0d28367-1695-4c50-a260-6fda526e9aab          |
      | [0].institutionDescription    | Comune di Milano                              |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | role         | status     |
      | prod-pagopa                   | SUB_DELEGATE | DELETED    |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21          |
    And The following query params:
      | userId                        | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
      | [0].institutionId             | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21          |
      | [0].institutionDescription    | Comune di Lucca                               |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | role         | status     |
      | prod-io                       | MANAGER      | ACTIVE     |

  # Cambio stato con filtro productRole
  @RemoveUserInstitutionAndUserInfoAfterScenarioWithUnusedUser
  Scenario: Successfully update user status with status (from ACTIVE to DELETED with multiple userInstitution and productRole filter)
    Given User login with username "j.doe" and password "test"
    And A mock userInstitution with id "65a4b6c7d8e9f01234567890" and onboardedProductState "ACTIVE" and role "SUB_DELEGATE" and productId "prod-pagopa" and institutionId "d0d28367-1695-4c50-a260-6fda526e9aab" and institutionDescription "Comune di Milano" and unused user
    And A mock userInstitution with id "78a2342c31599e1812b1819a" and onboardedProductState "ACTIVE" and role "MANAGER" and productId "prod-io" and institutionId "e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21" and institutionDescription "Comune di Lucca" and unused user
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | d0d28367-1695-4c50-a260-6fda526e9aab          |
    And The following query params:
      | userId                        | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
      | [0].institutionId             | d0d28367-1695-4c50-a260-6fda526e9aab          |
      | [0].institutionDescription    | Comune di Milano                              |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | role         | status     |
      | prod-pagopa                   | SUB_DELEGATE | ACTIVE     |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21          |
    And The following query params:
      | userId                        | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
      | [0].institutionId             | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21          |
      | [0].institutionDescription    | Comune di Lucca                               |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | role         | status     |
      | prod-io                       | MANAGER      | ACTIVE     |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id              | 12a521c9-3agc-43l5-adee-3d1c95310b1e                        |
    And The following query params:
      | status          | DELETED                                                     |
      | productRole     | admin                                                       |
    When I send a PUT request to "users/{id}/status"
    Then The status code is 204
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | d0d28367-1695-4c50-a260-6fda526e9aab          |
    And The following query params:
      | userId                        | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
      | [0].institutionId             | d0d28367-1695-4c50-a260-6fda526e9aab          |
      | [0].institutionDescription    | Comune di Milano                              |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | role         | status     |
      | prod-pagopa                   | SUB_DELEGATE | DELETED    |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21          |
    And The following query params:
      | userId                        | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
      | [0].institutionId             | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21          |
      | [0].institutionDescription    | Comune di Lucca                               |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | role         | status     |
      | prod-io                       | MANAGER      | ACTIVE     |

  @RemoveUserInstitutionAndUserInfoAfterScenarioWithUnusedUser
  Scenario: Successfully update user status with status (from ACTIVE to PENDING)
    Given User login with username "j.doe" and password "test"
    And A mock userInstitution with id "65a4b6c7d8e9f01234567890" and onboardedProductState "ACTIVE" and role "SUB_DELEGATE" and productId "prod-pagopa" and institutionId "d0d28367-1695-4c50-a260-6fda526e9aab" and institutionDescription "Comune di Milano" and unused user
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | d0d28367-1695-4c50-a260-6fda526e9aab          |
    And The following query params:
      | userId                        | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
      | [0].institutionId             | d0d28367-1695-4c50-a260-6fda526e9aab          |
      | [0].institutionDescription    | Comune di Milano                              |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | role         | status     |
      | prod-pagopa                   | SUB_DELEGATE | ACTIVE    |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id              | 12a521c9-3agc-43l5-adee-3d1c95310b1e                        |
    And The following query params:
      | status          | PENDING                                                     |
    When I send a PUT request to "users/{id}/status"
    Then The status code is 204
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | d0d28367-1695-4c50-a260-6fda526e9aab          |
    And The following query params:
      | userId                        | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
      | [0].institutionId             | d0d28367-1695-4c50-a260-6fda526e9aab          |
      | [0].institutionDescription    | Comune di Milano                              |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | role         | status     |
      | prod-pagopa                   | SUB_DELEGATE | PENDING    |

  @RemoveUserInstitutionAndUserInfoAfterScenarioWithUnusedUser
  Scenario: Successfully update user status with status (from SUSPENDED to ACTIVE)
    Given User login with username "j.doe" and password "test"
    And A mock userInstitution with id "65a4b6c7d8e9f01234567890" and onboardedProductState "SUSPENDED" and role "SUB_DELEGATE" and productId "prod-pagopa" and institutionId "d0d28367-1695-4c50-a260-6fda526e9aab" and institutionDescription "Comune di Milano" and unused user
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | d0d28367-1695-4c50-a260-6fda526e9aab          |
    And The following query params:
      | userId                        | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
      | [0].institutionId             | d0d28367-1695-4c50-a260-6fda526e9aab          |
      | [0].institutionDescription    | Comune di Milano                              |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | role         | status     |
      | prod-pagopa                   | SUB_DELEGATE | SUSPENDED    |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id              | 12a521c9-3agc-43l5-adee-3d1c95310b1e                        |
    And The following query params:
      | status          | ACTIVE                                                     |
    When I send a PUT request to "users/{id}/status"
    Then The status code is 204
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | d0d28367-1695-4c50-a260-6fda526e9aab          |
    And The following query params:
      | userId                        | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
      | [0].institutionId             | d0d28367-1695-4c50-a260-6fda526e9aab          |
      | [0].institutionDescription    | Comune di Milano                              |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | role         | status     |
      | prod-pagopa                   | SUB_DELEGATE | ACTIVE    |

  @RemoveUserInstitutionAndUserInfoAfterScenarioWithUnusedUser
  Scenario: Successfully update user status with status (from ACTIVE to SUSPENDED)
    Given User login with username "j.doe" and password "test"
    And A mock userInstitution with id "65a4b6c7d8e9f01234567890" and onboardedProductState "ACTIVE" and role "SUB_DELEGATE" and productId "prod-pagopa" and institutionId "d0d28367-1695-4c50-a260-6fda526e9aab" and institutionDescription "Comune di Milano" and unused user
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | d0d28367-1695-4c50-a260-6fda526e9aab          |
    And The following query params:
      | userId                        | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
      | [0].institutionId             | d0d28367-1695-4c50-a260-6fda526e9aab          |
      | [0].institutionDescription    | Comune di Milano                              |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | role         | status     |
      | prod-pagopa                   | SUB_DELEGATE | ACTIVE    |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id              | 12a521c9-3agc-43l5-adee-3d1c95310b1e                        |
    And The following query params:
      | status          | SUSPENDED                                                     |
    When I send a PUT request to "users/{id}/status"
    Then The status code is 204
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | d0d28367-1695-4c50-a260-6fda526e9aab          |
    And The following query params:
      | userId                        | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
      | [0].institutionId             | d0d28367-1695-4c50-a260-6fda526e9aab          |
      | [0].institutionDescription    | Comune di Milano                              |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | role         | status     |
      | prod-pagopa                   | SUB_DELEGATE | SUSPENDED    |

  @RemoveUserInstitutionAndUserInfoAfterScenarioWithUnusedUser
  Scenario: Successfully update user status with status (from ACTIVE to DELETED)
    Given User login with username "j.doe" and password "test"
    And A mock userInstitution with id "65a4b6c7d8e9f01234567890" and onboardedProductState "ACTIVE" and role "SUB_DELEGATE" and productId "prod-pagopa" and institutionId "d0d28367-1695-4c50-a260-6fda526e9aab" and institutionDescription "Comune di Milano" and unused user
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | d0d28367-1695-4c50-a260-6fda526e9aab          |
    And The following query params:
      | userId                        | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
      | [0].institutionId             | d0d28367-1695-4c50-a260-6fda526e9aab          |
      | [0].institutionDescription    | Comune di Milano                              |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | role         | status     |
      | prod-pagopa                   | SUB_DELEGATE | ACTIVE    |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id              | 12a521c9-3agc-43l5-adee-3d1c95310b1e                        |
    And The following query params:
      | status          | DELETED                                                     |
    When I send a PUT request to "users/{id}/status"
    Then The status code is 204
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | d0d28367-1695-4c50-a260-6fda526e9aab          |
    And The following query params:
      | userId                        | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
      | [0].institutionId             | d0d28367-1695-4c50-a260-6fda526e9aab          |
      | [0].institutionDescription    | Comune di Milano                              |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | role         | status     |
      | prod-pagopa                   | SUB_DELEGATE | DELETED    |

  @RemoveUserInstitutionAndUserInfoAfterScenarioWithUnusedUser
  Scenario: Unsuccessfully update user status with status (from PENDING to ACTIVE)
    Given User login with username "j.doe" and password "test"
    And A mock userInstitution with id "65a4b6c7d8e9f01234567890" and onboardedProductState "PENDING" and role "SUB_DELEGATE" and productId "prod-pagopa" and institutionId "d0d28367-1695-4c50-a260-6fda526e9aab" and institutionDescription "Comune di Milano" and unused user
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | d0d28367-1695-4c50-a260-6fda526e9aab          |
    And The following query params:
      | userId                        | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
      | [0].institutionId             | d0d28367-1695-4c50-a260-6fda526e9aab          |
      | [0].institutionDescription    | Comune di Milano                              |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | role         | status     |
      | prod-pagopa                   | SUB_DELEGATE | PENDING    |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id              | 12a521c9-3agc-43l5-adee-3d1c95310b1e                        |
    And The following query params:
      | status          | ACTIVE                                                     |
    When I send a PUT request to "users/{id}/status"
    Then The status code is 404
    And The response body contains:
      | detail                    | USER TO UPDATE NOT FOUND          |
      | status             | 404          |
      | title    | USER TO UPDATE NOT FOUND                              |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | d0d28367-1695-4c50-a260-6fda526e9aab          |
    And The following query params:
      | userId                        | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
      | [0].institutionId             | d0d28367-1695-4c50-a260-6fda526e9aab          |
      | [0].institutionDescription    | Comune di Milano                              |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | role         | status     |
      | prod-pagopa                   | SUB_DELEGATE | PENDING    |

  @RemoveUserInstitutionAndUserInfoAfterScenarioWithUnusedUser
  Scenario: Unsuccessfully update user status with status (from PENDING to DELETED)
    Given User login with username "j.doe" and password "test"
    And A mock userInstitution with id "65a4b6c7d8e9f01234567890" and onboardedProductState "PENDING" and role "SUB_DELEGATE" and productId "prod-pagopa" and institutionId "d0d28367-1695-4c50-a260-6fda526e9aab" and institutionDescription "Comune di Milano" and unused user
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | d0d28367-1695-4c50-a260-6fda526e9aab          |
    And The following query params:
      | userId                        | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
      | [0].institutionId             | d0d28367-1695-4c50-a260-6fda526e9aab          |
      | [0].institutionDescription    | Comune di Milano                              |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | role         | status     |
      | prod-pagopa                   | SUB_DELEGATE | PENDING    |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id              | 12a521c9-3agc-43l5-adee-3d1c95310b1e                        |
    And The following query params:
      | status          | DELETED                                                     |
    When I send a PUT request to "users/{id}/status"
    Then The status code is 404
    And The response body contains:
      | detail                    | USER TO UPDATE NOT FOUND          |
      | status             | 404          |
      | title    | USER TO UPDATE NOT FOUND                              |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | d0d28367-1695-4c50-a260-6fda526e9aab          |
    And The following query params:
      | userId                        | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
      | [0].institutionId             | d0d28367-1695-4c50-a260-6fda526e9aab          |
      | [0].institutionDescription    | Comune di Milano                              |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | role         | status     |
      | prod-pagopa                   | SUB_DELEGATE | PENDING    |

  @RemoveUserInstitutionAndUserInfoAfterScenarioWithUnusedUser
  Scenario: Unsuccessfully update user status with status (from PENDING to SUSPENDED)
    Given User login with username "j.doe" and password "test"
    And A mock userInstitution with id "65a4b6c7d8e9f01234567890" and onboardedProductState "PENDING" and role "SUB_DELEGATE" and productId "prod-pagopa" and institutionId "d0d28367-1695-4c50-a260-6fda526e9aab" and institutionDescription "Comune di Milano" and unused user
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | d0d28367-1695-4c50-a260-6fda526e9aab          |
    And The following query params:
      | userId                        | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
      | [0].institutionId             | d0d28367-1695-4c50-a260-6fda526e9aab          |
      | [0].institutionDescription    | Comune di Milano                              |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | role         | status     |
      | prod-pagopa                   | SUB_DELEGATE | PENDING    |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id              | 12a521c9-3agc-43l5-adee-3d1c95310b1e                        |
    And The following query params:
      | status          | SUSPENDED                                                     |
    When I send a PUT request to "users/{id}/status"
    Then The status code is 404
    And The response body contains:
      | detail                    | USER TO UPDATE NOT FOUND          |
      | status             | 404          |
      | title    | USER TO UPDATE NOT FOUND                              |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 | d0d28367-1695-4c50-a260-6fda526e9aab          |
    And The following query params:
      | userId                        | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 12a521c9-3agc-43l5-adee-3d1c95310b1e          |
      | [0].institutionId             | d0d28367-1695-4c50-a260-6fda526e9aab          |
      | [0].institutionDescription    | Comune di Milano                              |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | role         | status     |
      | prod-pagopa                   | SUB_DELEGATE | PENDING    |

  Scenario: Unsuccessfully update user status with wrong productId
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id              |  12a521c9-3agc-43l5-adee-3d1c95310b1e                       |
    And The following query params:
      | status          | DELETED                                                     |
      | productId          | wrongProduct                                                     |
    When I send a PUT request to "users/{id}/status"
    Then The status code is 500
    And The response body contains string:
      | Something has gone wrong in the server                                                        |

  Scenario: Unsuccessfully update user status with wrong userId
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id              |  wrongUser                                     |
    And The following query params:
      | status          | DELETED                                                     |
    When I send a PUT request to "users/{id}/status"
    Then The status code is 500
    And The response body contains string:
      | Something has gone wrong in the server                                                        |

  Scenario: Unsuccessfully update user status with wrong role
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id              |  12a521c9-3agc-43l5-adee-3d1c95310b1e                       |
    And The following query params:
      | status          | DELETED                                                     |
      | role            | wrongRole                                                     |
    When I send a PUT request to "users/{id}/status"
    Then The status code is 500
    And The response body contains string:
      | Something has gone wrong in the server                                                        |

  Scenario: Unsuccessfully update user status with wrong institutionId
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id              |  12a521c9-3agc-43l5-adee-3d1c95310b1e                       |
    And The following query params:
      | status          | DELETED                                                     |
      | institutionId            | wrongInstitution                                                     |
    When I send a PUT request to "users/{id}/status"
    Then The status code is 500
    And The response body contains string:
      | Something has gone wrong in the server                                                        |

  Scenario: Unsuccessfully update user status with wrong productRole
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id              |  12a521c9-3agc-43l5-adee-3d1c95310b1e                       |
    And The following query params:
      | status          | DELETED                                                     |
      | productRole            | wrongProductRole                                                     |
    When I send a PUT request to "users/{id}/status"
    Then The status code is 500
    And The response body contains string:
      | Something has gone wrong in the server                                                        |

  # Cambio stato senza parametri
  Scenario: Unsuccessfully update user status without status
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id              |  35a78332-d038-4bfa-8e85-2cba7f6b7bf7                |
    When I send a PUT request to "users/{id}/status"
    Then The status code is 400
    And The response body contains string:
      | STATUS IS MANDATORY                                                        |

  # Senza JWT
  Scenario: Bad Token update user status with optional filter
    Given A bad jwt token
    And The following path params:
      | id              |  35a78332-d038-4bfa-8e85-2cba7f6b7bf7                |
    When I send a PUT request to "users/{id}/status"
    Then The status code is 401
  ######################### END PUT /{id}/status #########################


  ######################### BEGIN GET /ids #########################

  Scenario: Successfully retrieve all users given their userIds with 2 userIds
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | userIds                   | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7,97a511a7-2acc-47b9-afed-2f3c65753b4a                            |
    When I send a GET request to "users/ids"
    Then The status code is 200
    And The response body contains the list "" of size 4
    And The response body contains at path "" the following list of objects in any order:
      | id                        | userId                               | institutionId                        | institutionDescription |
      | 65aea85f85a6a37415221bd6  | 97a511a7-2acc-47b9-afed-2f3c65753b4a | d0d28367-1695-4c50-a260-6fda526e9aab | Comune di Milano       |
      | 65b1234f85a6a37415221ef9  | 97a511a7-2acc-47b9-afed-2f3c65753b4a | a1b2c3d4-5678-90ab-cdef-1234567890ab | Regione Lazio          |
      | 65b1214f85b2a37412421ef6  | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7 | a1b2c3d4-5678-90ab-cdef-1234567890ab | Regione Lazio          |
      | 65b2345a85a6a37415222ff1  | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7 | f2e4d6c8-9876-5432-ba10-abcdef123456 | Università di Bologna  |

  Scenario: Successfully retrieve all users given their userIds with 1 userId
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | userIds                   | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                                                                 |
    When I send a GET request to "users/ids"
    Then The status code is 200
    And The response body contains the list "" of size 2
    And The response body contains at path "" the following list of objects in any order:
      | id                        | userId                               | institutionId                        | institutionDescription |
      | 65b1214f85b2a37412421ef6  | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7 | a1b2c3d4-5678-90ab-cdef-1234567890ab | Regione Lazio          |
      | 65b2345a85a6a37415222ff1  | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7 | f2e4d6c8-9876-5432-ba10-abcdef123456 | Università di Bologna  |

  Scenario: Successfully retrieve all users given their userIds with wrong user id
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | userIds                   | wrongUserId                                                                                          |
    When I send a GET request to "users/ids"
    Then The status code is 200
    And The response body contains the list "" of size 0

  Scenario: Successfully retrieve all users given their userIds without passing userIds
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "users/ids"
    Then The status code is 200
    And The response body contains the list "" of size 4
    And The response body contains at path "" the following list of objects in any order:
      | id                        | userId                               | institutionId                        | institutionDescription |
      | 65aea85f85a6a37415221bd6  | 97a511a7-2acc-47b9-afed-2f3c65753b4a | d0d28367-1695-4c50-a260-6fda526e9aab | Comune di Milano       |
      | 65b1234f85a6a37415221ef9  | 97a511a7-2acc-47b9-afed-2f3c65753b4a | a1b2c3d4-5678-90ab-cdef-1234567890ab | Regione Lazio          |
      | 65b1214f85b2a37412421ef6  | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7 | a1b2c3d4-5678-90ab-cdef-1234567890ab | Regione Lazio          |
      | 65b2345a85a6a37415222ff1  | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7 | f2e4d6c8-9876-5432-ba10-abcdef123456 | Università di Bologna  |

  Scenario: Bad Token retrieve all users given their userIds
    Given A bad jwt token
    And The following query params:
      | userIds                   |  35a78332-d038-4bfa-8e85-2cba7f6b7bf7                                                                |
    When I send a GET request to "users/ids"
    Then The status code is 401

  ######################### END GET /ids #########################

  ######################### BEGIN GET /notification #########################

  Scenario: Successfully retrieve all SC-User for DataLake
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "users/notification"
    Then The status code is 200
    And The response body contains the list "users" of size 5
    And The response body contains at path "users" the following list of objects in any order:
      | id                                                              | institutionId                               | productId      | user.userId                                | user.email                                      | user.role       | user.productRole            | user.relationshipStatus |
      | 65aea85f85a6a37415221bd6_prod-ciban_referente amministrativo    | d0d28367-1695-4c50-a260-6fda526e9aab        | prod-ciban     | 97a511a7-2acc-47b9-afed-2f3c65753b4a       | 81956dd1-00fd-4423-888b-f77a48d26ba1@test.it    | MANAGER         | referente amministrativo    | PENDING                 |
      | 65aea85f85a6a37415221bd6_prod-pagopa_referente amministrativo   | d0d28367-1695-4c50-a260-6fda526e9aab        | prod-pagopa    | 97a511a7-2acc-47b9-afed-2f3c65753b4a       | 81956dd1-00fd-4423-888b-f77a48d26ba1@test.it    | MANAGER         | referente amministrativo    | ACTIVE                  |
      | 65b1234f85a6a37415221ef9_prod-io_admin                          | a1b2c3d4-5678-90ab-cdef-1234567890ab        | prod-io        | 97a511a7-2acc-47b9-afed-2f3c65753b4a       | 81956dd1-00fd-4423-888b-f77a48d26ba1@test.it    | MANAGER         | admin                       | ACTIVE                  |
      | 65b1234f85a6a37415221ef9_prod-interop_referente amministrativo  | a1b2c3d4-5678-90ab-cdef-1234567890ab        | prod-interop   | 97a511a7-2acc-47b9-afed-2f3c65753b4a       | 81956dd1-00fd-4423-888b-f77a48d26ba1@test.it    | MANAGER         | referente amministrativo    | DELETED                 |
      | 65b1214f85b2a37412421ef6_prod-io_admin                          | a1b2c3d4-5678-90ab-cdef-1234567890ab        | prod-io        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7       | r.balboa@regionelazio.it                        | SUB_DELEGATE    | admin                       | ACTIVE                  |

  Scenario: Successfully retrieve all SC-User for DataLake with productId filter
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId                                                       | prod-io                                     |
    When I send a GET request to "users/notification"
    Then The status code is 200
    And The response body contains the list "users" of size 2
    And The response body contains at path "users" the following list of objects in any order:
      | id                                                              | institutionId                               | productId      | user.userId                                | user.email                                      | user.role       | user.productRole            | user.relationshipStatus |
      | 65b1234f85a6a37415221ef9_prod-io_admin                          | a1b2c3d4-5678-90ab-cdef-1234567890ab        | prod-io        | 97a511a7-2acc-47b9-afed-2f3c65753b4a       | 81956dd1-00fd-4423-888b-f77a48d26ba1@test.it    | MANAGER         | admin                       | ACTIVE                  |
      | 65b1214f85b2a37412421ef6_prod-io_admin                          | a1b2c3d4-5678-90ab-cdef-1234567890ab        | prod-io        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7       | r.balboa@regionelazio.it                        | SUB_DELEGATE    | admin                       | ACTIVE                  |

  Scenario: Successfully retrieve all SC-User for DataLake with wrong productId filter
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId                                                       | prod-wrongproduct                           |
    When I send a GET request to "users/notification"
    Then The status code is 200
    And The response body contains the list "users" of size 0

  Scenario: Successfully retrieve all SC-User for DataLake with page number without page size
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | page                                                       | 2                           |
    When I send a GET request to "users/notification"
    Then The status code is 200
    And The response body contains the list "users" of size 0

  Scenario: Unsuccessfully retrieve all SC-User for DataLake with wrong page size
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | size                                                       | -2                           |
    When I send a GET request to "users/notification"
    Then The status code is 500
    And The response body contains string:
      | Something has gone wrong in the server                                                    |

  Scenario: Unsuccessfully retrieve all SC-User for DataLake with wrong page number
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | page                                                       | -2                           |
    When I send a GET request to "users/notification"
    Then The status code is 500
    And The response body contains string:
      | Something has gone wrong in the server                                                    |

  Scenario: Bad Token retrieve all users given their userIds
    Given A bad jwt token
    When I send a GET request to "users/notification"
    Then The status code is 401

  # Mancano test con paginazione a causa del bug aperto

  ######################### END GET /notification #########################


  ######################### BEGIN GET / #########################

  Scenario: Successfully retrieve paged users with optional filters in input as query params
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "users"
    Then The status code is 200
    And The response body contains the list "" of size 4
    And The response body contains at path "" the following list of objects in any order:
      | id                                 | institutionId                               | userId                                    | institutionDescription     | userMailUuid                                      |
      | 65aea85f85a6a37415221bd6           | d0d28367-1695-4c50-a260-6fda526e9aab        | 97a511a7-2acc-47b9-afed-2f3c65753b4a      | Comune di Milano           | ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1      |
      | 65b2345a85a6a37415222ff1           | f2e4d6c8-9876-5432-ba10-abcdef123456        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7      | Università di Bologna      | ID_MAIL#123123-55555-efaz-12312-apclacpela        |
      | 65b1214f85b2a37412421ef6           | a1b2c3d4-5678-90ab-cdef-1234567890ab        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7      | Regione Lazio              | ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv      |
      | 65b1234f85a6a37415221ef9           | a1b2c3d4-5678-90ab-cdef-1234567890ab        | 97a511a7-2acc-47b9-afed-2f3c65753b4a      | Regione Lazio              | ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1      |

  Scenario: Successfully retrieve paged users with optional filters in input as query params with institutionId filter
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | institutionId                      | a1b2c3d4-5678-90ab-cdef-1234567890ab        |
    When I send a GET request to "users"
    Then The status code is 200
    And The response body contains the list "" of size 2
    And The response body contains at path "" the following list of objects in any order:
      | id                                 | institutionId                               | userId                                    | institutionDescription     | userMailUuid                                      |
      | 65b1214f85b2a37412421ef6           | a1b2c3d4-5678-90ab-cdef-1234567890ab        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7      | Regione Lazio              | ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv      |
      | 65b1234f85a6a37415221ef9           | a1b2c3d4-5678-90ab-cdef-1234567890ab        | 97a511a7-2acc-47b9-afed-2f3c65753b4a      | Regione Lazio              | ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1      |

  Scenario: Successfully retrieve paged users with optional filters in input as query params with userId filter
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | userId                             | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7        |
    When I send a GET request to "users"
    Then The status code is 200
    And The response body contains the list "" of size 2
    And The response body contains at path "" the following list of objects in any order:
      | id                                 | institutionId                               | userId                                    | institutionDescription     | userMailUuid                                      |
      | 65b2345a85a6a37415222ff1           | f2e4d6c8-9876-5432-ba10-abcdef123456        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7      | Università di Bologna      | ID_MAIL#123123-55555-efaz-12312-apclacpela        |
      | 65b1214f85b2a37412421ef6           | a1b2c3d4-5678-90ab-cdef-1234567890ab        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7      | Regione Lazio              | ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv      |

  Scenario: Successfully retrieve paged users with optional filters in input as query params with roles filter
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | roles                             | SUB_DELEGATE                                 |
      | roles                             | DELEGATE                                     |
    When I send a GET request to "users"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains at path "" the following list of objects in any order:
      | id                                 | institutionId                               | userId                                    | institutionDescription     | userMailUuid                                      |
      | 65b1214f85b2a37412421ef6           | a1b2c3d4-5678-90ab-cdef-1234567890ab        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7      | Regione Lazio              | ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv      |

  Scenario: Successfully retrieve paged users with optional filters in input as query params with states filter
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | states                             | DELETED                                      |
      | states                             | PENDING                                      |
    When I send a GET request to "users"
    Then The status code is 200
    And The response body contains the list "" of size 3
    And The response body contains at path "" the following list of objects in any order:
      | id                                 | institutionId                               | userId                                    | institutionDescription     | userMailUuid                                      |
      | 65aea85f85a6a37415221bd6           | d0d28367-1695-4c50-a260-6fda526e9aab        | 97a511a7-2acc-47b9-afed-2f3c65753b4a      | Comune di Milano           | ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1      |
      | 65b2345a85a6a37415222ff1           | f2e4d6c8-9876-5432-ba10-abcdef123456        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7      | Università di Bologna      | ID_MAIL#123123-55555-efaz-12312-apclacpela        |
      | 65b1234f85a6a37415221ef9           | a1b2c3d4-5678-90ab-cdef-1234567890ab        | 97a511a7-2acc-47b9-afed-2f3c65753b4a      | Regione Lazio              | ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1      |

  Scenario: Successfully retrieve paged users with optional filters in input as query params with productRoles (2) filter
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productRoles                        | referente amministrativo                   |
      | productRoles                        | admin                                      |
    When I send a GET request to "users"
    Then The status code is 200
    And The response body contains the list "" of size 4
    And The response body contains at path "" the following list of objects in any order:
      | id                                 | institutionId                               | userId                                    | institutionDescription     | userMailUuid                                      |
      | 65aea85f85a6a37415221bd6           | d0d28367-1695-4c50-a260-6fda526e9aab        | 97a511a7-2acc-47b9-afed-2f3c65753b4a      | Comune di Milano           | ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1      |
      | 65b2345a85a6a37415222ff1           | f2e4d6c8-9876-5432-ba10-abcdef123456        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7      | Università di Bologna      | ID_MAIL#123123-55555-efaz-12312-apclacpela        |
      | 65b1234f85a6a37415221ef9           | a1b2c3d4-5678-90ab-cdef-1234567890ab        | 97a511a7-2acc-47b9-afed-2f3c65753b4a      | Regione Lazio              | ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1      |
      | 65b1214f85b2a37412421ef6           | a1b2c3d4-5678-90ab-cdef-1234567890ab        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7      | Regione Lazio              | ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv      |

  Scenario: Successfully retrieve paged users with optional filters in input as query params with productRoles (1) filter
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productRoles                       | admin                                       |
    When I send a GET request to "users"
    Then The status code is 200
    And The response body contains the list "" of size 2
    And The response body contains at path "" the following list of objects in any order:
      | id                                 | institutionId                               | userId                                    | institutionDescription     | userMailUuid                                      |
      | 65b1234f85a6a37415221ef9           | a1b2c3d4-5678-90ab-cdef-1234567890ab        | 97a511a7-2acc-47b9-afed-2f3c65753b4a      | Regione Lazio              | ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1      |
      | 65b1214f85b2a37412421ef6           | a1b2c3d4-5678-90ab-cdef-1234567890ab        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7      | Regione Lazio              | ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv      |

  Scenario: Successfully retrieve paged users with optional filters in input as query params with paging
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | page                               | 0                                           |
      | size                               | 2                                           |
    When I send a GET request to "users"
    Then The status code is 200
    And The response body contains the list "" of size 2

  Scenario: Successfully retrieve paged users with optional filters in input as query params with paging and wrong page
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | page                               | 4                                           |
      | size                               | 2                                           |
    When I send a GET request to "users"
    Then The status code is 200
    And The response body contains the list "" of size 0

  Scenario: Successfully retrieve paged users with optional filters in input as query params with productRoles and userId filters
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | userId                             | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7        |
      | productRoles                       | admin                                       |
    When I send a GET request to "users"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains at path "" the following list of objects in any order:
      | id                                 | institutionId                               | userId                                    | institutionDescription     | userMailUuid                                      |
      | 65b1214f85b2a37412421ef6           | a1b2c3d4-5678-90ab-cdef-1234567890ab        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7      | Regione Lazio              | ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv      |

  Scenario: Successfully retrieve paged users with optional filters in input as query params with wrong productRoles
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productRoles                       | wrongProductRole                            |
    When I send a GET request to "users"
    Then The status code is 200
    And The response body contains the list "" of size 0

  Scenario: Successfully retrieve paged users with optional filters in input as query params with wrong userId
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | userId                             | wrongUserId                                 |
    When I send a GET request to "users"
    Then The status code is 200
    And The response body contains the list "" of size 0

  Scenario: Successfully retrieve paged users with optional filters in input as query params with wrong product
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | products                           | wrongProduct                                |
    When I send a GET request to "users"
    Then The status code is 200
    And The response body contains the list "" of size 0

  Scenario: Successfully retrieve paged users with optional filters in input as query params with wrong institutionId
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | institutionId                      | wrongInstitutionId                          |
    When I send a GET request to "users"
    Then The status code is 200
    And The response body contains the list "" of size 0

  Scenario: Successfully retrieve paged users with optional filters in input as query params with wrong state
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | states                             | wrongState                                  |
    When I send a GET request to "users"
    Then The status code is 200
    And The response body contains the list "" of size 0

  Scenario: Unsuccessfully retrieve paged users with optional filters in input as query params with wrong role
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | roles                              | wrongRole                                   |
    When I send a GET request to "users"
    Then The status code is 400
    And The response body contains string:
      | Invalid role value: wrongRole                                                    |

  Scenario: Unsuccessfully retrieve paged users with optional filters in input as query params with wrong page
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | page                               | -1                                          |
    When I send a GET request to "users"
    Then The status code is 500
    And The response body contains string:
      | Something has gone wrong in the server                                           |

  Scenario: Unsuccessfully retrieve paged users with optional filters in input as query params with wrong page
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | size                               | -1                                          |
    When I send a GET request to "users"
    Then The status code is 500
    And The response body contains string:
      | Something has gone wrong in the server                                           |

  Scenario: Bad Token retrieve paged users with optional filters in input as query params
    Given A bad jwt token
    When I send a GET request to "users"
    Then The status code is 401

  ######################### END GET / #########################

  ######################### BEGIN PUT /{id}/institution/{institutionId}/product/{productId}/status #########################

  @RemoveUserInstitutionAndUserInfoAfterScenario
  Scenario: Successfully update user product status from ACTIVE to DELETED
    Given User login with username "j.doe" and password "test"
    And A mock userInstitution with id "65a4b6c7d8e9f01234567890" and onboardedProductState "ACTIVE" and role "SUB_DELEGATE" and productId "prod-pagopa"
    And A mock userInfo with id "d0d28367-1695-4c50-a260-6fda526e9aab", institutionName "Comune di Milano", status "ACTIVE", role "SUB_DELEGATE" to userInfo document with id "35a78332-d038-4bfa-8e85-2cba7f6b7bf7"
    And The following path params:
      | institutionId              |  d0d28367-1695-4c50-a260-6fda526e9aab                |
    And The following query params:
      | userId                     | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                 |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].id                     | 65a4b6c7d8e9f01234567890                             |
    And The response body contains at path "[0].products" the following list of objects in any order:
      | status                     |
      | ACTIVE                    |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id                         | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                 |
      | institutionId              | d0d28367-1695-4c50-a260-6fda526e9aab                 |
      | productId                  | prod-pagopa                                          |
    And The following query params:
      | status                     | DELETED                                              |
    When I send a PUT request to "users/{id}/institution/{institutionId}/product/{productId}/status"
    Then The status code is 204
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId              |  d0d28367-1695-4c50-a260-6fda526e9aab                |
    And The following query params:
      | userId                     | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                 |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].id                     | 65a4b6c7d8e9f01234567890                             |
    And The response body contains at path "[0].products" the following list of objects in any order:
      | status                     |
      | DELETED                    |

  @RemoveUserInstitutionAndUserInfoAfterScenario
  Scenario: Successfully update user product status from SUSPENDED to ACTIVE
    Given User login with username "j.doe" and password "test"
    And A mock userInstitution with id "65a4b6c7d8e9f01234567890" and onboardedProductState "SUSPENDED" and role "SUB_DELEGATE" and productId "prod-pagopa"
    And A mock userInfo with id "d0d28367-1695-4c50-a260-6fda526e9aab", institutionName "Comune di Milano", status "SUSPENDED", role "SUB_DELEGATE" to userInfo document with id "35a78332-d038-4bfa-8e85-2cba7f6b7bf7"
    And The following path params:
      | institutionId              |  d0d28367-1695-4c50-a260-6fda526e9aab                |
    And The following query params:
      | userId                     | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                 |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].id                     | 65a4b6c7d8e9f01234567890                             |
    And The response body contains at path "[0].products" the following list of objects in any order:
      | status                     |
      | SUSPENDED                  |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id                         | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                 |
      | institutionId              | d0d28367-1695-4c50-a260-6fda526e9aab                 |
      | productId                  | prod-pagopa                                          |
    And The following query params:
      | status                     | ACTIVE                                               |
    When I send a PUT request to "users/{id}/institution/{institutionId}/product/{productId}/status"
    Then The status code is 204
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId              |  d0d28367-1695-4c50-a260-6fda526e9aab                |
    And The following query params:
      | userId                     | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                 |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].id                     | 65a4b6c7d8e9f01234567890                             |
    And The response body contains at path "[0].products" the following list of objects in any order:
      | status                     |
      | ACTIVE                     |

  @RemoveUserInstitutionAndUserInfoAfterScenario
  Scenario: Successfully update user product status from ACTIVE to SUSPENDED
    Given User login with username "j.doe" and password "test"
    And A mock userInstitution with id "65a4b6c7d8e9f01234567890" and onboardedProductState "ACTIVE" and role "SUB_DELEGATE" and productId "prod-pagopa"
    And A mock userInfo with id "d0d28367-1695-4c50-a260-6fda526e9aab", institutionName "Comune di Milano", status "ACTIVE", role "SUB_DELEGATE" to userInfo document with id "35a78332-d038-4bfa-8e85-2cba7f6b7bf7"
    And The following path params:
      | institutionId              |  d0d28367-1695-4c50-a260-6fda526e9aab                |
    And The following query params:
      | userId                     | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                 |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].id                     | 65a4b6c7d8e9f01234567890                             |
    And The response body contains at path "[0].products" the following list of objects in any order:
      | status                     |
      | ACTIVE                     |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id                         | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                 |
      | institutionId              | d0d28367-1695-4c50-a260-6fda526e9aab                 |
      | productId                  | prod-pagopa                                          |
    And The following query params:
      | status                     | SUSPENDED                                            |
    When I send a PUT request to "users/{id}/institution/{institutionId}/product/{productId}/status"
    Then The status code is 204
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId              |  d0d28367-1695-4c50-a260-6fda526e9aab                |
    And The following query params:
      | userId                     | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                 |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].id                     | 65a4b6c7d8e9f01234567890                             |
    And The response body contains at path "[0].products" the following list of objects in any order:
      | status                     |
      | SUSPENDED                  |

  @RemoveUserInstitutionAndUserInfoAfterScenario
  Scenario: Successfully update user product status with productRole filter
    Given User login with username "j.doe" and password "test"
    And A mock userInstitution with id "65a4b6c7d8e9f01234567890" and onboardedProductState "ACTIVE" and role "SUB_DELEGATE" and productId "prod-pagopa"
    And A mock userInfo with id "d0d28367-1695-4c50-a260-6fda526e9aab", institutionName "Comune di Milano", status "ACTIVE", role "SUB_DELEGATE" to userInfo document with id "35a78332-d038-4bfa-8e85-2cba7f6b7bf7"
    And The following path params:
      | institutionId              |  d0d28367-1695-4c50-a260-6fda526e9aab                |
    And The following query params:
      | userId                     | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                 |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].id                     | 65a4b6c7d8e9f01234567890                             |
    And The response body contains at path "[0].products" the following list of objects in any order:
      | status                     |
      | ACTIVE                    |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id                         | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                 |
      | institutionId              | d0d28367-1695-4c50-a260-6fda526e9aab                 |
      | productId                  | prod-pagopa                                          |
    And The following query params:
      | status                     | DELETED                                              |
      | productRole                | admin                                                |
    When I send a PUT request to "users/{id}/institution/{institutionId}/product/{productId}/status"
    Then The status code is 204
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId              |  d0d28367-1695-4c50-a260-6fda526e9aab                |
    And The following query params:
      | userId                     | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                 |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].id                     | 65a4b6c7d8e9f01234567890                             |
    And The response body contains at path "[0].products" the following list of objects in any order:
      | status                     |
      | DELETED                    |

  @RemoveUserInstitutionAndUserInfoAfterScenario
  Scenario: Unsuccessfully update user product status from DELETED to ACTIVE
    Given User login with username "j.doe" and password "test"
    And A mock userInstitution with id "65a4b6c7d8e9f01234567890" and onboardedProductState "DELETED" and role "SUB_DELEGATE" and productId "prod-pagopa"
    And A mock userInfo with id "d0d28367-1695-4c50-a260-6fda526e9aab", institutionName "Comune di Milano", status "DELETED", role "SUB_DELEGATE" to userInfo document with id "35a78332-d038-4bfa-8e85-2cba7f6b7bf7"
    And The following path params:
      | institutionId              |  d0d28367-1695-4c50-a260-6fda526e9aab                |
    And The following query params:
      | userId                     | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                 |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].id                     | 65a4b6c7d8e9f01234567890                             |
    And The response body contains at path "[0].products" the following list of objects in any order:
      | status                     |
      | DELETED                    |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id                         | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                 |
      | institutionId              | d0d28367-1695-4c50-a260-6fda526e9aab                 |
      | productId                  | prod-pagopa                                          |
    And The following query params:
      | status                     | ACTIVE                                               |
    When I send a PUT request to "users/{id}/institution/{institutionId}/product/{productId}/status"
    Then The status code is 404
    And The response body contains:
      | detail                     | USER TO UPDATE NOT FOUND                             |
      | status                     | 404                                                  |
      | title                      | USER TO UPDATE NOT FOUND                             |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId              |  d0d28367-1695-4c50-a260-6fda526e9aab                |
    And The following query params:
      | userId                     | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                 |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].id                     | 65a4b6c7d8e9f01234567890                             |
    And The response body contains at path "[0].products" the following list of objects in any order:
      | status                     |
      | DELETED                    |

  @RemoveUserInstitutionAndUserInfoAfterScenario
  Scenario: Unsuccessfully update user product status from TOBEVALIDATED to ACTIVE
    Given User login with username "j.doe" and password "test"
    And A mock userInstitution with id "65a4b6c7d8e9f01234567890" and onboardedProductState "TOBEVALIDATED" and role "SUB_DELEGATE" and productId "prod-pagopa"
    And A mock userInfo with id "d0d28367-1695-4c50-a260-6fda526e9aab", institutionName "Comune di Milano", status "TOBEVALIDATED", role "SUB_DELEGATE" to userInfo document with id "35a78332-d038-4bfa-8e85-2cba7f6b7bf7"
    And The following path params:
      | institutionId              |  d0d28367-1695-4c50-a260-6fda526e9aab                |
    And The following query params:
      | userId                     | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                 |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].id                     | 65a4b6c7d8e9f01234567890                             |
    And The response body contains at path "[0].products" the following list of objects in any order:
      | status                     |
      | TOBEVALIDATED              |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id                         | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                 |
      | institutionId              | d0d28367-1695-4c50-a260-6fda526e9aab                 |
      | productId                  | prod-pagopa                                          |
    And The following query params:
      | status                     | ACTIVE                                               |
    When I send a PUT request to "users/{id}/institution/{institutionId}/product/{productId}/status"
    Then The status code is 404
    And The response body contains:
      | detail                     | USER TO UPDATE NOT FOUND                             |
      | status                     | 404                                                  |
      | title                      | USER TO UPDATE NOT FOUND                             |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId              |  d0d28367-1695-4c50-a260-6fda526e9aab                |
    And The following query params:
      | userId                     | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                 |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].id                     | 65a4b6c7d8e9f01234567890                             |
    And The response body contains at path "[0].products" the following list of objects in any order:
      | status                     |
      | TOBEVALIDATED              |

  @RemoveUserInstitutionAndUserInfoAfterScenario
  Scenario: Unsuccessfully update user product status from PENDING to ACTIVE
    Given User login with username "j.doe" and password "test"
    And A mock userInstitution with id "65a4b6c7d8e9f01234567890" and onboardedProductState "PENDING" and role "SUB_DELEGATE" and productId "prod-pagopa"
    And A mock userInfo with id "d0d28367-1695-4c50-a260-6fda526e9aab", institutionName "Comune di Milano", status "PENDING", role "SUB_DELEGATE" to userInfo document with id "35a78332-d038-4bfa-8e85-2cba7f6b7bf7"
    And The following path params:
      | institutionId              |  d0d28367-1695-4c50-a260-6fda526e9aab                |
    And The following query params:
      | userId                     | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                 |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].id                     | 65a4b6c7d8e9f01234567890                             |
    And The response body contains at path "[0].products" the following list of objects in any order:
      | status                     |
      | PENDING                    |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id                         | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                 |
      | institutionId              | d0d28367-1695-4c50-a260-6fda526e9aab                 |
      | productId                  | prod-pagopa                                          |
    And The following query params:
      | status                     | ACTIVE                                               |
    When I send a PUT request to "users/{id}/institution/{institutionId}/product/{productId}/status"
    Then The status code is 404
    And The response body contains:
      | detail                     | USER TO UPDATE NOT FOUND                             |
      | status                     | 404                                                  |
      | title                      | USER TO UPDATE NOT FOUND                             |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId              |  d0d28367-1695-4c50-a260-6fda526e9aab                |
    And The following query params:
      | userId                     | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                 |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].id                     | 65a4b6c7d8e9f01234567890                             |
    And The response body contains at path "[0].products" the following list of objects in any order:
      | status                     |
      | PENDING                    |

  @RemoveUserInstitutionAndUserInfoAfterScenario
  Scenario: Unsuccessfully update user product status with wrong productRole filter
    Given User login with username "j.doe" and password "test"
    And A mock userInstitution with id "65a4b6c7d8e9f01234567890" and onboardedProductState "ACTIVE" and role "SUB_DELEGATE" and productId "prod-pagopa"
    And A mock userInfo with id "d0d28367-1695-4c50-a260-6fda526e9aab", institutionName "Comune di Milano", status "ACTIVE", role "SUB_DELEGATE" to userInfo document with id "35a78332-d038-4bfa-8e85-2cba7f6b7bf7"
    And The following path params:
      | institutionId              |  d0d28367-1695-4c50-a260-6fda526e9aab                |
    And The following query params:
      | userId                     | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                 |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].id                     | 65a4b6c7d8e9f01234567890                             |
    And The response body contains at path "[0].products" the following list of objects in any order:
      | status                     |
      | ACTIVE                     |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id                         | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                 |
      | institutionId              | d0d28367-1695-4c50-a260-6fda526e9aab                 |
      | productId                  | prod-pagopa                                          |
    And The following query params:
      | status                     | DELETED                                              |
      | productRole                | referente amministrativo                             |
    When I send a PUT request to "users/{id}/institution/{institutionId}/product/{productId}/status"
    Then The status code is 500
    And The response body contains string:
      | Something has gone wrong in the server                                           |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId              |  d0d28367-1695-4c50-a260-6fda526e9aab                |
    And The following query params:
      | userId                     | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                 |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].id                     | 65a4b6c7d8e9f01234567890                             |
    And The response body contains at path "[0].products" the following list of objects in any order:
      | status                     |
      | ACTIVE                     |

  @RemoveUserInstitutionAndUserInfoAfterScenario
  Scenario: Unsuccessfully update user product status with wrong institutionId
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id                         | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                 |
      | institutionId              | d0d28367-1695-4c50-a260-6fda526e9aab                 |
      | productId                  | prod-wrongProd                                       |
    And The following query params:
      | status                     | DELETED                                              |
    When I send a PUT request to "users/{id}/institution/{institutionId}/product/{productId}/status"
    Then The status code is 500
    And The response body contains string:
      | Something has gone wrong in the server                                           |

  @RemoveUserInstitutionAndUserInfoAfterScenario
  Scenario: Unsuccessfully update user product status with wrong institutionId
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id                         | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                 |
      | institutionId              | wrongInstitution                                     |
      | productId                  | prod-pagopa                                          |
    And The following query params:
      | status                     | DELETED                                              |
    When I send a PUT request to "users/{id}/institution/{institutionId}/product/{productId}/status"
    Then The status code is 500
    And The response body contains string:
      | Something has gone wrong in the server                                           |

  @RemoveUserInstitutionAndUserInfoAfterScenario
  Scenario: Unsuccessfully update user product status with wrong user
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id                         | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                 |
      | institutionId              | d0d28367-1695-4c50-a260-6fda526e9aab                 |
      | productId                  | prod-pagopa                                          |
    And The following query params:
      | status                     | DELETED                                              |
    When I send a PUT request to "users/{id}/institution/{institutionId}/product/{productId}/status"
    Then The status code is 500
    And The response body contains string:
      | Something has gone wrong in the server                                           |

  @RemoveUserInstitutionAndUserInfoAfterScenario
  Scenario: Unsuccessfully update user product status with wrong user
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id                         | wrongUser                                            |
      | institutionId              | d0d28367-1695-4c50-a260-6fda526e9aab                 |
      | productId                  | prod-pagopa                                          |
    And The following query params:
      | status                     | WRONGSTATUS                                          |
    When I send a PUT request to "users/{id}/institution/{institutionId}/product/{productId}/status"
    Then The status code is 500
    And The response body contains string:
      | Something has gone wrong in the server                                           |

  @RemoveUserInstitutionAndUserInfoAfterScenario
  Scenario: Unsuccessfully update user product status without status
    Given User login with username "j.doe" and password "test"
    And A mock userInstitution with id "65a4b6c7d8e9f01234567890" and onboardedProductState "PENDING" and role "SUB_DELEGATE" and productId "prod-pagopa"
    And A mock userInfo with id "d0d28367-1695-4c50-a260-6fda526e9aab", institutionName "Comune di Milano", status "PENDING", role "SUB_DELEGATE" to userInfo document with id "35a78332-d038-4bfa-8e85-2cba7f6b7bf7"
    And The following path params:
      | institutionId              |  d0d28367-1695-4c50-a260-6fda526e9aab                |
    And The following query params:
      | userId                     | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                 |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].id                     | 65a4b6c7d8e9f01234567890                             |
    And The response body contains at path "[0].products" the following list of objects in any order:
      | status                     |
      | PENDING                    |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id                         | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                 |
      | institutionId              | d0d28367-1695-4c50-a260-6fda526e9aab                 |
      | productId                  | prod-pagopa                                          |
    When I send a PUT request to "users/{id}/institution/{institutionId}/product/{productId}/status"
    Then The status code is 400
    And The response body contains:
      | title                      | Constraint Violation                                 |
      | status                     | 400                                                  |
      | violations[0].field        | updateUserProductStatus.status                       |
      | violations[0].message      | must not be null                                     |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId              |  d0d28367-1695-4c50-a260-6fda526e9aab                |
    And The following query params:
      | userId                     | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                 |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].id                     | 65a4b6c7d8e9f01234567890                             |
    And The response body contains at path "[0].products" the following list of objects in any order:
      | status                     |
      | PENDING                    |

  Scenario: Bad Token update user product status
    Given A bad jwt token
    And The following path params:
      | id            | 97a511a7-2acc-47b9-afed-2f3c65753b4a |
      | institutionId | d0d28367-1695-4c50-a260-6fda526e9aab |
      | productId     | prod-pagopa                          |
    And The following query params:
      | status        | DELETED                              |
    When I send a PUT request to "users/{id}/institution/{institutionId}/product/{productId}/status"
    Then The status code is 401

  ######################### END PUT /{id}/institution/{institutionId}/product/{productId}/status #########################


  ######################### BEGIN POST /{userId} #########################

  @RemoveUserInstitutionAfterCreateFromAPI
  Scenario: Successfully update or create a user by userId with a new role
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
    And The following path params:
      | institutionId                 |  e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21         |
    And The following query params:
      | userId                        | 97a511a7-2acc-47b9-afed-2f3c65753b4a          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 97a511a7-2acc-47b9-afed-2f3c65753b4a          |
      | [0].institutionId             | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21          |
      | [0].institutionDescription    | Comune di Bergamo                             |
      | [0].userMailUuid              | ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1  |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | tokenId                                       | role         | productRole               | status     |
      | prod-io                       | 7a3df825-8317-4601-9fea-12283b7ed97f          | DELEGATE     | referente amministrativo  | ACTIVE     |

  @RemoveUserInstitutionAfterCreateFromAPI
  Scenario: Successfully update or create a user by userId with a new role (2 times with different products for same institution)
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
    And The following path params:
      | institutionId                 |  e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21         |
    And The following query params:
      | userId                        | 97a511a7-2acc-47b9-afed-2f3c65753b4a          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 97a511a7-2acc-47b9-afed-2f3c65753b4a          |
      | [0].institutionId             | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21          |
      | [0].institutionDescription    | Comune di Bergamo                             |
      | [0].userMailUuid              | ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1  |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | tokenId                                       | role         | productRole               | status     |
      | prod-io                       | 7a3df825-8317-4601-9fea-12283b7ed97f          | DELEGATE     | referente amministrativo  | ACTIVE     |
    Given User login with username "j.doe" and password "test"
    And The following request body:
      """
      {
          "institutionId": "e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21",
          "product": {
              "productId": "prod-pagopa",
              "role": "MANAGER",
              "tokenId": "5b2cfa14-9246-4d03-bfcb-8a9d1e6e3f45",
              "productRoles": [
                  "admin"
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
    And The following path params:
      | institutionId                 |  e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21         |
    And The following query params:
      | userId                        | 97a511a7-2acc-47b9-afed-2f3c65753b4a          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 97a511a7-2acc-47b9-afed-2f3c65753b4a          |
      | [0].institutionId             | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21          |
      | [0].institutionDescription    | Comune di Bergamo                             |
      | [0].userMailUuid              | ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1  |
    And The response body contains the list "[0].products" of size 2
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | tokenId                                       | role         | productRole               | status     |
      | prod-io                       | 7a3df825-8317-4601-9fea-12283b7ed97f          | DELEGATE     | referente amministrativo  | ACTIVE     |
      | prod-pagopa                   | 5b2cfa14-9246-4d03-bfcb-8a9d1e6e3f45          | MANAGER      | admin                     | ACTIVE     |

  @RemoveUserInstitutionAfterCreateFromAPI
  Scenario: Unsuccessfully update or create a user by userId with a new role (2 times with same product for same institution)
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
    And The following path params:
      | institutionId                 |  e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21         |
    And The following query params:
      | userId                        | 97a511a7-2acc-47b9-afed-2f3c65753b4a          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 97a511a7-2acc-47b9-afed-2f3c65753b4a          |
      | [0].institutionId             | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21          |
      | [0].institutionDescription    | Comune di Bergamo                             |
      | [0].userMailUuid              | ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1  |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | tokenId                                       | role         | productRole               | status     |
      | prod-io                       | 7a3df825-8317-4601-9fea-12283b7ed97f          | DELEGATE     | referente amministrativo  | ACTIVE     |
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
    Then The status code is 400
    And The response body contains string:
      | User already has roles on Product prod-io                                     |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 |  e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21         |
    And The following query params:
      | userId                        | 97a511a7-2acc-47b9-afed-2f3c65753b4a          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 97a511a7-2acc-47b9-afed-2f3c65753b4a          |
      | [0].institutionId             | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21          |
      | [0].institutionDescription    | Comune di Bergamo                             |
      | [0].userMailUuid              | ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1  |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | tokenId                                       | role         | productRole               | status     |
      | prod-io                       | 7a3df825-8317-4601-9fea-12283b7ed97f          | DELEGATE     | referente amministrativo  | ACTIVE     |

  Scenario: Unsuccessfully update or create a user by userId with a new role (with wrong user)
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
      | userId                        | wrongUser                                                                |
    When I send a POST request to "users/{userId}"
    Then The status code is 404
    And The response body contains:
      | detail | Received: 'Not Found, status code 404' when invoking: Rest Client method: 'org.openapi.quarkus.user_registry_json.api.UserApi#findByIdUsingGET' |
      | status | 404                                                                                                                                             |
      | title  | Received: 'Not Found, status code 404' when invoking: Rest Client method: 'org.openapi.quarkus.user_registry_json.api.UserApi#findByIdUsingGET' |


  Scenario: Bad Token update or create a user by userId with a new role
    Given A bad jwt token
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
    Then The status code is 401
  ######################### END POST /{userId} #########################

  ######################### BEGIN POST /{userId}/onboarding #########################

  @RemoveUserInstitutionAfterCreateFromAPI
  Scenario: Successfully update or create a user by userId after check if user is a manager for the specified product
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
    When I send a POST request to "users/{userId}/onboarding"
    Then The status code is 201
    And The response body contains string:
      | 97a511a7-2acc-47b9-afed-2f3c65753b4a                                          |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 |  e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21         |
    And The following query params:
      | userId                        | 97a511a7-2acc-47b9-afed-2f3c65753b4a          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 97a511a7-2acc-47b9-afed-2f3c65753b4a          |
      | [0].institutionId             | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21          |
      | [0].institutionDescription    | Comune di Bergamo                             |
      | [0].userMailUuid              | ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1  |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | tokenId                                       | role         | productRole               | status     |
      | prod-io                       | 7a3df825-8317-4601-9fea-12283b7ed97f          | DELEGATE     | referente amministrativo  | ACTIVE     |
    Given User login with username "j.doe" and password "test"
    And The following request body:
      """
      {
          "institutionId": "e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21",
          "product": {
              "productId": "prod-io",
              "role": "MANAGER",
              "tokenId": "7a3df825-8317-4601-9fea-12283b7ed97f",
              "productRoles": [
                  "admin"
              ]
          },
          "institutionDescription": "Comune di Bergamo",
          "userMailUuid": "ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1"
      }
      """
    And The following path params:
      | userId                        | 97a511a7-2acc-47b9-afed-2f3c65753b4a          |
    When I send a POST request to "users/{userId}/onboarding"
    Then The status code is 201
    And The response body contains string:
      | 97a511a7-2acc-47b9-afed-2f3c65753b4a                                          |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 |  e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21         |
    And The following query params:
      | userId                        | 97a511a7-2acc-47b9-afed-2f3c65753b4a          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 97a511a7-2acc-47b9-afed-2f3c65753b4a          |
      | [0].institutionId             | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21          |
      | [0].institutionDescription    | Comune di Bergamo                             |
      | [0].userMailUuid              | ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1  |
    And The response body contains the list "[0].products" of size 2
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | tokenId                                       | role         | productRole               | status     |
      | prod-io                       | 7a3df825-8317-4601-9fea-12283b7ed97f          | DELEGATE     | referente amministrativo  | DELETED    |
      | prod-io                       | 7a3df825-8317-4601-9fea-12283b7ed97f          | MANAGER      | admin                     | ACTIVE     |
    Given User login with username "j.doe" and password "test"
    And The following request body:
      """
      {
          "institutionId": "e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21",
          "product": {
              "productId": "prod-io",
              "role": "MANAGER",
              "tokenId": "7a3df825-8317-4601-9fea-12283b7ed97f",
              "productRoles": [
                  "admin"
              ]
          },
          "institutionDescription": "Comune di Bergamo",
          "userMailUuid": "ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1"
      }
      """
    And The following path params:
      | userId                        | 97a511a7-2acc-47b9-afed-2f3c65753b4a          |
    When I send a POST request to "users/{userId}/onboarding"
    Then The status code is 200
    And The response body contains string:
      | 97a511a7-2acc-47b9-afed-2f3c65753b4a                                          |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 |  e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21         |
    And The following query params:
      | userId                        | 97a511a7-2acc-47b9-afed-2f3c65753b4a          |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 97a511a7-2acc-47b9-afed-2f3c65753b4a          |
      | [0].institutionId             | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21          |
      | [0].institutionDescription    | Comune di Bergamo                             |
      | [0].userMailUuid              | ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1  |
    And The response body contains the list "[0].products" of size 2
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | tokenId                                       | role         | productRole               | status     |
      | prod-io                       | 7a3df825-8317-4601-9fea-12283b7ed97f          | DELEGATE     | referente amministrativo  | DELETED    |
      | prod-io                       | 7a3df825-8317-4601-9fea-12283b7ed97f          | MANAGER      | admin                     | ACTIVE     |

  Scenario: Unsuccessfully update or create a user by userId after check if user is a manager for the specified product (with wrong role and user not created)
    Given User login with username "j.doe" and password "test"
    And The following request body:
      """
      {
          "institutionId": "e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21",
          "product": {
              "productId": "prod-io",
              "role": "WRONGROLE",
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
    When I send a POST request to "users/{userId}/onboarding"
    Then The status code is 500
    And The response body contains string:
      | Something has gone wrong in the server                                        |

  @RemoveUserInstitutionAfterCreateFromAPI
  Scenario: Unsuccessfully update or create a user by userId after check if user is a manager for the specified product (with wrong role and user created)
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
      | userId                        | 97a511a7-2acc-47b9-afed-2f3c65753b4a                                   |
    When I send a POST request to "users/{userId}/onboarding"
    Then The status code is 201
    And The response body contains string:
      | 97a511a7-2acc-47b9-afed-2f3c65753b4a                                                                   |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId                 |  e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21                                  |
    And The following query params:
      | userId                        | 97a511a7-2acc-47b9-afed-2f3c65753b4a                                   |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].userId                    | 97a511a7-2acc-47b9-afed-2f3c65753b4a                                   |
      | [0].institutionId             | e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21                                   |
      | [0].institutionDescription    | Comune di Bergamo                                                      |
      | [0].userMailUuid              | ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1                           |
    And The response body contains the list "[0].products" of size 1
    And The response body contains at path "[0].products" the following list of objects in any order:
      | productId                     | tokenId                                       | role                   | productRole               | status     |
      | prod-io                       | 7a3df825-8317-4601-9fea-12283b7ed97f          | DELEGATE               | referente amministrativo  | ACTIVE     |
    Given User login with username "j.doe" and password "test"
    And The following request body:
      """
      {
          "institutionId": "e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21",
          "product": {
              "productId": "prod-io",
              "role": "WRONGROLE",
              "tokenId": "7a3df825-8317-4601-9fea-12283b7ed97f",
              "productRoles": [
                  "admin"
              ]
          },
          "institutionDescription": "Comune di Bergamo",
          "userMailUuid": "ID_MAIL#81956dd1-00fd-4423-888b-f77a48d26ba1"
      }
      """
    And The following path params:
      | userId                        | 97a511a7-2acc-47b9-afed-2f3c65753b4a                                     |
    When I send a POST request to "users/{userId}/onboarding"
    Then The status code is 400
    And The response body contains string:
      | Invalid role: WRONGROLE. Allowed value are: [MANAGER, DELEGATE, SUB_DELEGATE, OPERATOR, ADMIN_EA]       |

  Scenario: Unsuccessfully update or create a user by userId after check if user is a manager for the specified product (with wrong userId)
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
      | userId                        | wrongUser                                                                |
    When I send a POST request to "users/{userId}/onboarding"
    Then The status code is 404
    And The response body contains:
      | detail | Received: 'Not Found, status code 404' when invoking: Rest Client method: 'org.openapi.quarkus.user_registry_json.api.UserApi#findByIdUsingGET' |
      | status | 404                                                                                                                                             |
      | title  | Received: 'Not Found, status code 404' when invoking: Rest Client method: 'org.openapi.quarkus.user_registry_json.api.UserApi#findByIdUsingGET' |

  Scenario: Bad Token update or create a user by userId after check if user is a manager for the specified product
    Given A bad jwt token
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
    When I send a POST request to "users/{userId}/onboarding"
    Then The status code is 401

  ######################### END POST /{userId}/onboarding #########################

  ######################### BEGIN POST / #########################

  Scenario: Bad Token create a new user or update an existing one
    Given A bad jwt token
    And The following request body:
      """
      {
          "institutionId": "e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21",
          "user": {
            "fiscalCode": "MRYWLM80A01H501H",
            "institutionEmail": "prova@email.com"
          },
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
    When I send a POST request to "users/"
    Then The status code is 401

  ######################### END POST / #########################

  ######################### BEGIN GET /{userId}/institutions/{institutionId} #########################

  Scenario: Successfully retrieves userInstitution data with list of actions permitted for each user's product (with role MANAGER)
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | userId                            | 97a511a7-2acc-47b9-afed-2f3c65753b4a        |
      | institutionId                     | d0d28367-1695-4c50-a260-6fda526e9aab        |
    When I send a GET request to "users/{userId}/institutions/{institutionId}"
    Then The status code is 200
    And The response body contains:
      | userId                            | 97a511a7-2acc-47b9-afed-2f3c65753b4a        |
      | institutionId                     | d0d28367-1695-4c50-a260-6fda526e9aab        |
      | institutionDescription            | Comune di Milano                            |
      | products[0].productId             | prod-pagopa                                 |
      | products[0].tokenId               | f9a23bcd-6b2a-4f08-a7f3-1e6d5c9e8b74        |
      | products[0].status                | ACTIVE                                      |
      | products[0].productRole           | referente amministrativo                    |
      | products[0].role                  | MANAGER                                     |
      | products[0].env                   | ROOT                                        |
    And The response body contains the list "products" of size 1
    And The response body contains at path "products[0].userProductActions" the following list of values in any order:
      | Selc:UploadLogo                   |
      | Selc:ViewBilling                  |
      | Selc:RequestProductAccess         |
      | Selc:ListAvailableProducts        |
      | Selc:ListActiveProducts           |
      | Selc:AccessProductBackoffice      |
      | Selc:ViewManagedInstitutions      |
      | Selc:ViewDelegations              |
      | Selc:ManageProductUsers           |
      | Selc:ListProductUsers             |
      | Selc:ManageProductGroups          |
      | Selc:CreateDelegation             |
      | Selc:ViewInstitutionData          |
      | Selc:UpdateInstitutionData        |

  @RemoveUserInstitutionAndUserInfoAfterScenario
  Scenario: Successfully retrieves userInstitution data with list of actions permitted for each user's product (with role OPERATOR)
    Given User login with username "j.doe" and password "test"
    And A mock userInstitution with id "65a4b6c7d8e9f01234567890" and onboardedProductState "ACTIVE" and role "OPERATOR" and productId "prod-pagopa"
    And The following path params:
      | userId                            | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7        |
      | institutionId                     | d0d28367-1695-4c50-a260-6fda526e9aab        |
    When I send a GET request to "users/{userId}/institutions/{institutionId}"
    Then The status code is 200
    And The response body contains:
      | userId                            | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7        |
      | institutionId                     | d0d28367-1695-4c50-a260-6fda526e9aab        |
      | institutionDescription            | Comune di Milano                            |
      | products[0].productId             | prod-pagopa                                 |
      | products[0].tokenId               | asda8312-3311-5642-gsds-gfr2252341          |
      | products[0].status                | ACTIVE                                      |
      | products[0].role                  | OPERATOR                                    |
      | products[0].env                   | ROOT                                        |
    And The response body contains the list "products" of size 1
    And The response body contains at path "products[0].userProductActions" the following list of values in any order:
      | Selc:ViewBilling                  |
      | Selc:AccessProductBackoffice      |
      | Selc:ViewInstitutionData          |
      | Selc:ListActiveProducts           |

  @RemoveUserInstitutionAndUserInfoAfterScenario
  Scenario: Successfully retrieves userInstitution data with list of actions permitted for each user's product (with role DELEGATE)
    Given User login with username "j.doe" and password "test"
    And A mock userInstitution with id "65a4b6c7d8e9f01234567890" and onboardedProductState "ACTIVE" and role "DELEGATE" and productId "prod-pagopa"
    And The following path params:
      | userId                            | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7        |
      | institutionId                     | d0d28367-1695-4c50-a260-6fda526e9aab        |
    When I send a GET request to "users/{userId}/institutions/{institutionId}"
    Then The status code is 200
    And The response body contains:
      | userId                            | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7        |
      | institutionId                     | d0d28367-1695-4c50-a260-6fda526e9aab        |
      | institutionDescription            | Comune di Milano                            |
      | products[0].productId             | prod-pagopa                                 |
      | products[0].tokenId               | asda8312-3311-5642-gsds-gfr2252341          |
      | products[0].status                | ACTIVE                                      |
      | products[0].role                  | DELEGATE                                    |
      | products[0].env                   | ROOT                                        |
    And The response body contains the list "products" of size 1
    And The response body contains at path "products[0].userProductActions" the following list of values in any order:
      | Selc:UploadLogo                   |
      | Selc:ViewBilling                  |
      | Selc:RequestProductAccess         |
      | Selc:ListAvailableProducts        |
      | Selc:ListActiveProducts           |
      | Selc:AccessProductBackoffice      |
      | Selc:ViewManagedInstitutions      |
      | Selc:ViewDelegations              |
      | Selc:ManageProductUsers           |
      | Selc:ListProductUsers             |
      | Selc:ManageProductGroups          |
      | Selc:CreateDelegation             |
      | Selc:ViewInstitutionData          |
      | Selc:UpdateInstitutionData        |

  @RemoveUserInstitutionAndUserInfoAfterScenario
  Scenario: Successfully retrieves userInstitution data with list of actions permitted for each user's product (with role SUB_DELEGATE)
    Given User login with username "j.doe" and password "test"
    And A mock userInstitution with id "65a4b6c7d8e9f01234567890" and onboardedProductState "ACTIVE" and role "SUB_DELEGATE" and productId "prod-pagopa"
    And The following path params:
      | userId                            | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7        |
      | institutionId                     | d0d28367-1695-4c50-a260-6fda526e9aab        |
    When I send a GET request to "users/{userId}/institutions/{institutionId}"
    Then The status code is 200
    And The response body contains:
      | userId                            | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7        |
      | institutionId                     | d0d28367-1695-4c50-a260-6fda526e9aab        |
      | institutionDescription            | Comune di Milano                            |
      | products[0].productId             | prod-pagopa                                 |
      | products[0].tokenId               | asda8312-3311-5642-gsds-gfr2252341          |
      | products[0].status                | ACTIVE                                      |
      | products[0].role                  | SUB_DELEGATE                                |
      | products[0].env                   | ROOT                                        |
    And The response body contains the list "products" of size 1
    And The response body contains at path "products[0].userProductActions" the following list of values in any order:
      | Selc:UploadLogo                   |
      | Selc:ViewBilling                  |
      | Selc:RequestProductAccess         |
      | Selc:ListAvailableProducts        |
      | Selc:ListActiveProducts           |
      | Selc:AccessProductBackoffice      |
      | Selc:ViewManagedInstitutions      |
      | Selc:ViewDelegations              |
      | Selc:ManageProductUsers           |
      | Selc:ListProductUsers             |
      | Selc:ManageProductGroups          |
      | Selc:CreateDelegation             |
      | Selc:ViewInstitutionData          |
      | Selc:UpdateInstitutionData        |

  @RemoveUserInstitutionAndUserInfoAfterScenario
  Scenario: Successfully retrieves userInstitution data with list of actions permitted for each user's product (with role ADMIN_EA and prod pago-pa)
    Given User login with username "j.doe" and password "test"
    And A mock userInstitution with id "65a4b6c7d8e9f01234567890" and onboardedProductState "ACTIVE" and role "ADMIN_EA" and productId "prod-pagopa"
    And The following path params:
      | userId                            | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7        |
      | institutionId                     | d0d28367-1695-4c50-a260-6fda526e9aab        |
    When I send a GET request to "users/{userId}/institutions/{institutionId}"
    Then The status code is 200
    And The response body contains:
      | userId                            | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7        |
      | institutionId                     | d0d28367-1695-4c50-a260-6fda526e9aab        |
      | institutionDescription            | Comune di Milano                            |
      | products[0].productId             | prod-pagopa                                 |
      | products[0].tokenId               | asda8312-3311-5642-gsds-gfr2252341          |
      | products[0].status                | ACTIVE                                      |
      | products[0].role                  | ADMIN_EA                                    |
      | products[0].env                   | ROOT                                        |
    And The response body contains the list "products" of size 1
    And The response body contains at path "products[0].userProductActions" the following list of values in any order:
      | Selc:UploadLogo                   |
      | Selc:ViewBilling                  |
      | Selc:RequestProductAccess         |
      | Selc:ListAvailableProducts        |
      | Selc:ListActiveProducts           |
      | Selc:AccessProductBackoffice      |
      | Selc:ViewManagedInstitutions      |
      | Selc:ViewDelegations              |
      | Selc:ListProductUsers             |
      | Selc:ManageProductGroups          |
      | Selc:ViewInstitutionData          |
      | Selc:UpdateInstitutionData        |

  @RemoveUserInstitutionAndUserInfoAfterScenario
  Scenario: Successfully retrieves userInstitution data with list of actions permitted for each user's product (with role ADMIN_EA and prod io)
    Given User login with username "j.doe" and password "test"
    And A mock userInstitution with id "65a4b6c7d8e9f01234567890" and onboardedProductState "ACTIVE" and role "ADMIN_EA" and productId "prod-io"
    And The following path params:
      | userId                            | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7        |
      | institutionId                     | d0d28367-1695-4c50-a260-6fda526e9aab        |
    When I send a GET request to "users/{userId}/institutions/{institutionId}"
    Then The status code is 200
    And The response body contains:
      | userId                            | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7        |
      | institutionId                     | d0d28367-1695-4c50-a260-6fda526e9aab        |
      | institutionDescription            | Comune di Milano                            |
      | products[0].productId             | prod-io                                 |
      | products[0].tokenId               | asda8312-3311-5642-gsds-gfr2252341          |
      | products[0].status                | ACTIVE                                      |
      | products[0].role                  | ADMIN_EA                                    |
      | products[0].env                   | ROOT                                        |
    And The response body contains the list "products" of size 1
    And The response body contains at path "products[0].userProductActions" the following list of values in any order:
      | Selc:UploadLogo                   |
      | Selc:ViewBilling                  |
      | Selc:ListActiveProducts           |
      | Selc:AccessProductBackoffice      |
      | Selc:ViewManagedInstitutions      |
      | Selc:ViewDelegations              |
      | Selc:ManageProductUsers           |
      | Selc:ListProductUsers             |
      | Selc:ManageProductGroups          |
      | Selc:CreateDelegation             |
      | Selc:ViewInstitutionData          |
      | Selc:UpdateInstitutionData        |

  @RemoveUserInstitutionAndUserInfoAfterScenario
  Scenario: Successfully retrieves userInstitution data with list of actions permitted for each user's product (with role ADMIN_EA and prod ciban)
    Given User login with username "j.doe" and password "test"
    And A mock userInstitution with id "65a4b6c7d8e9f01234567890" and onboardedProductState "ACTIVE" and role "ADMIN_EA" and productId "prod-ciban"
    And The following path params:
      | userId                            | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7        |
      | institutionId                     | d0d28367-1695-4c50-a260-6fda526e9aab        |
    When I send a GET request to "users/{userId}/institutions/{institutionId}"
    Then The status code is 200
    And The response body contains:
      | userId                            | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7        |
      | institutionId                     | d0d28367-1695-4c50-a260-6fda526e9aab        |
      | institutionDescription            | Comune di Milano                            |
      | products[0].productId             | prod-ciban                                  |
      | products[0].tokenId               | asda8312-3311-5642-gsds-gfr2252341          |
      | products[0].status                | ACTIVE                                      |
      | products[0].role                  | ADMIN_EA                                    |
      | products[0].env                   | ROOT                                        |
    And The response body contains the list "products" of size 1
    And The response body contains at path "products[0].userProductActions" the following list of values in any order:
      | Selc:UploadLogo                   |
      | Selc:ViewBilling                  |
      | Selc:RequestProductAccess         |
      | Selc:ListAvailableProducts        |
      | Selc:ListActiveProducts           |
      | Selc:AccessProductBackoffice      |
      | Selc:ViewManagedInstitutions      |
      | Selc:ViewDelegations              |
      | Selc:ListProductUsers             |
      | Selc:ManageProductGroups          |
      | Selc:ViewInstitutionData          |
      | Selc:UpdateInstitutionData        |

  @RemoveUserInstitutionAndUserInfoAfterScenario
  Scenario: Successfully retrieves userInstitution data with list of actions permitted for each user's product (with role ADMIN_EA and prod ciban and correct productId filter)
    Given User login with username "j.doe" and password "test"
    And A mock userInstitution with id "65a4b6c7d8e9f01234567890" and onboardedProductState "ACTIVE" and role "ADMIN_EA" and productId "prod-ciban"
    And The following path params:
      | userId                            | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7        |
      | institutionId                     | d0d28367-1695-4c50-a260-6fda526e9aab        |
    And The following query params:
      | productId                         | prod-ciban                                  |
    When I send a GET request to "users/{userId}/institutions/{institutionId}"
    Then The status code is 200
    And The response body contains:
      | userId                            | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7        |
      | institutionId                     | d0d28367-1695-4c50-a260-6fda526e9aab        |
      | institutionDescription            | Comune di Milano                            |
      | products[0].productId             | prod-ciban                                  |
      | products[0].tokenId               | asda8312-3311-5642-gsds-gfr2252341          |
      | products[0].status                | ACTIVE                                      |
      | products[0].role                  | ADMIN_EA                                    |
      | products[0].env                   | ROOT                                        |
    And The response body contains the list "products" of size 1
    And The response body contains at path "products[0].userProductActions" the following list of values in any order:
      | Selc:UploadLogo                   |
      | Selc:ViewBilling                  |
      | Selc:RequestProductAccess         |
      | Selc:ListAvailableProducts        |
      | Selc:ListActiveProducts           |
      | Selc:AccessProductBackoffice      |
      | Selc:ViewManagedInstitutions      |
      | Selc:ViewDelegations              |
      | Selc:ListProductUsers             |
      | Selc:ManageProductGroups          |
      | Selc:ViewInstitutionData          |
      | Selc:UpdateInstitutionData        |

  @RemoveUserInstitutionAndUserInfoAfterScenario
  Scenario: Unsuccessfully retrieves userInstitution data with list of actions permitted for each user's product (with role ADMIN_EA and prod ciban and wrong productId filter)
    Given User login with username "j.doe" and password "test"
    And A mock userInstitution with id "65a4b6c7d8e9f01234567890" and onboardedProductState "ACTIVE" and role "ADMIN_EA" and productId "prod-ciban"
    And The following path params:
      | userId                            | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                                                                                            |
      | institutionId                     | d0d28367-1695-4c50-a260-6fda526e9aab                                                                                            |
    And The following query params:
      | productId                         | prod-io                                                                                                                         |
    When I send a GET request to "users/{userId}/institutions/{institutionId}"
    Then The status code is 404
    And The response body contains:
      | detail                            | User having userId [35a78332-d038-4bfa-8e85-2cba7f6b7bf7] and institutionId [d0d28367-1695-4c50-a260-6fda526e9aab] not found    |
      | status                            | 404                                                                                                                             |
      | title                             | User having userId [35a78332-d038-4bfa-8e85-2cba7f6b7bf7] and institutionId [d0d28367-1695-4c50-a260-6fda526e9aab] not found    |

  Scenario: Unsuccessfully retrieves userInstitution data with list of actions permitted for each user's product (with wrong institutionId)
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | userId                            | 97a511a7-2acc-47b9-afed-2f3c65753b4a                                                                        |
      | institutionId                     | wrongInstitution                                                                                            |
    When I send a GET request to "users/{userId}/institutions/{institutionId}"
    Then The status code is 404
    And The response body contains:
      | detail                            | User having userId [97a511a7-2acc-47b9-afed-2f3c65753b4a] and institutionId [wrongInstitution] not found    |
      | status                            | 404                                                                                                         |
      | title                             | User having userId [97a511a7-2acc-47b9-afed-2f3c65753b4a] and institutionId [wrongInstitution] not found    |

  Scenario: Unsuccessfully retrieves userInstitution data with list of actions permitted for each user's product (with wrong userId)
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | userId                            | wrongUser                                                                                                   |
      | institutionId                     | d0d28367-1695-4c50-a260-6fda526e9aab                                                                        |
    When I send a GET request to "users/{userId}/institutions/{institutionId}"
    Then The status code is 404
    And The response body contains:
      | detail                            | User having userId [wrongUser] and institutionId [d0d28367-1695-4c50-a260-6fda526e9aab] not found           |
      | status                            | 404                                                                                                         |
      | title                             | User having userId [wrongUser] and institutionId [d0d28367-1695-4c50-a260-6fda526e9aab] not found           |


  Scenario: Bad Token retrieves userInstitution data with list of actions permitted for each user's product
    Given A bad jwt token
    And The following path params:
      | userId                            | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7        |
      | institutionId                     | d0d28367-1695-4c50-a260-6fda526e9aab        |
    When I send a GET request to "users/{userId}/institutions/{institutionId}"
    Then The status code is 401

  ######################### END GET /{userId}/institutions/{institutionId} #########################