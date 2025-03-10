Feature: User

  ######################### BEGIN /users/email #########################

  Scenario: Successfully get user emails giving productId and institutionId with two results
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId                         | prod-io                                             |
      | institutionId                     | a1b2c3d4-5678-90ab-cdef-1234567890ab                |
    When I send a GET request to "users/emails"
    Then The status code is 200
    And The response body contains the list "" of size 2
    And The response body contains at path "" the following list of values in any order:
      | 81956dd1-00fd-4423-888b-f77a48d26ba1@test.it        |
      | r.balboa@regionelazio.it        |

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
      | title                                                   | Constraint Violation      |
      | status                                                  | 400                       |
    And The response body contains at path "violations" the following list of objects in any order:
      | field                                                   | message                   |
      | getUsersEmailByInstitutionAndProduct.institutionId      | non deve essere null      |
      | getUsersEmailByInstitutionAndProduct.productId          | non deve essere null      |

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
      | violations[0].message             | non deve essere null                                |

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
      | violations[0].message             | non deve essere null                                |


  Scenario: Bad Token get user emails giving productId and institutionId
    Given A bad jwt token
    And The following query params:
      | productId                         | prod-ciban                                          |
      | institutionId                     | d0d28367-1695-4c50-a260-6fda526e9aab                |
    When I send a GET request to "users/emails"
    Then The status code is 401

  ######################### END /users/email #########################

  ######################### BEGIN /users/{id} #########################

  Scenario: Successfully get user info given userId
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id | 97a511a7-2acc-47b9-afed-2f3c65753b4a |
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
      | id | 97a511a7-2acc-47b9-afed-2f3c65753b4a |
    And The following query params:
      | institutionId | d0d28367-1695-4c50-a260-6fda526e9aab |
      | productId | prod-ciban |
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

  ######################### END /users/{id} #########################



  ######################### BEGIN /{userId}/institutions #########################

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
      | institutionId                            | institutionName        | role            | status  |
      | a1b2c3d4-5678-90ab-cdef-1234567890ab     | Regione Lazio          | SUB_DELEGATE    | ACTIVE  |
      | f2e4d6c8-9876-5432-ba10-abcdef123456     | Università di Bologna  | MANAGER         | PENDING |

  Scenario: Successfully get products info and role which the user is enabled with userId and institutionId
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | userId        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                               |
    And The following query params:
      | institutionId        | f2e4d6c8-9876-5432-ba10-abcdef123456                               |
    When I send a GET request to "users/{userId}/institutions"
    Then The status code is 200
    And The response body contains the list "institutions" of size 1
    And The response body contains:
      | userId        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                               |
    And The response body contains at path "institutions" the following list of objects in any order:
      | institutionId                            | institutionName        | role            | status  |
      | f2e4d6c8-9876-5432-ba10-abcdef123456     | Università di Bologna  | MANAGER         | PENDING |

  Scenario: Successfully get products info and role which the user is enabled with userId and states
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | userId        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                               |
    And The following query params:
      | states        | ACTIVE                               |
    When I send a GET request to "users/{userId}/institutions"
    Then The status code is 200
    And The response body contains the list "institutions" of size 1
    And The response body contains:
      | userId        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                               |
    And The response body contains at path "institutions" the following list of objects in any order:
      | institutionId                            | institutionName        | role            | status  |
      | a1b2c3d4-5678-90ab-cdef-1234567890ab     | Regione Lazio          | SUB_DELEGATE    | ACTIVE  |

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
      | institutionId                            | institutionName        | role            | status  |
      | a1b2c3d4-5678-90ab-cdef-1234567890ab     | Regione Lazio          | SUB_DELEGATE    | ACTIVE  |
      | f2e4d6c8-9876-5432-ba10-abcdef123456     | Università di Bologna  | MANAGER         | PENDING |

  Scenario: Unsuccessfully get products info and role which the user is enabled with userId and not present state
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | userId        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                               |
    And The following query params:
      | states        | DELETED                                                            |
    When I send a GET request to "users/{userId}/institutions"
    Then The status code is 404
    And The response body contains:
      | detail                                   | User with id 35a78332-d038-4bfa-8e85-2cba7f6b7bf7 and institution id null not found!   |
      | status                                   | 404                                                                                    |
      | title                                    | User with id 35a78332-d038-4bfa-8e85-2cba7f6b7bf7 and institution id null not found!   |

  Scenario: Unsuccessfully get products info and role which the user is enabled with userId and wrong institution
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | userId        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                               |
    And The following query params:
      | institutionId | asdfghjkl                                                          |
    When I send a GET request to "users/{userId}/institutions"
    Then The status code is 404
    And The response body contains:
      | detail                                   | User with id 35a78332-d038-4bfa-8e85-2cba7f6b7bf7 and institution id asdfghjkl not found!   |
      | status                                   | 404                                                                                         |
      | title                                    | User with id 35a78332-d038-4bfa-8e85-2cba7f6b7bf7 and institution id asdfghjkl not found!   |

  Scenario: Unsuccessfully get products info and role which the user is enabled with userId and states and wrong institution
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | userId        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                               |
    And The following query params:
      | institutionId | asdfghjkl                                                          |
      | states        | ACTIVE                                                             |
      | states        | PENDING                                                            |
    When I send a GET request to "users/{userId}/institutions"
    Then The status code is 404
    And The response body contains:
      | detail                                   | User with id 35a78332-d038-4bfa-8e85-2cba7f6b7bf7 and institution id asdfghjkl not found!   |
      | status                                   | 404                                                                                         |
      | title                                    | User with id 35a78332-d038-4bfa-8e85-2cba7f6b7bf7 and institution id asdfghjkl not found!   |

  Scenario: Unsuccessfully get products info and role which the user is enabled with userId and wrong state
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | userId        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                               |
    And The following query params:
      | states        | ASDFGH                                                             |
    When I send a GET request to "users/{userId}/institutions"
    Then The status code is 500
    And The response body contains error string:
      | Something has gone wrong in the server                                             |

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
      | userId            | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                               |
    When I send a GET request to "users/{userId}/institutions"
    Then The status code is 401

  ######################### END /{userId}/institutions #########################


  ######################### BEGIN /{id}/details #########################

  Scenario: Successfully get user's information from pdv: name, familyName, email, fiscalCode and workContacts
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                               |
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
      | id        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                               |
    And The following query params:
      | field     | name                                                               |
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
      | id        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                               |
    And The following query params:
      | field     | familyName                                                         |
    When I send a GET request to "users/{id}/details"
    Then The status code is 200
    And The response body contains:
      | id                                                                                | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7          |
      | familyName.value                                                                        | Balboa                                         |
      | familyName.certified                                                                    | SPID                                          |
    And The response body doesn't contain field "name.value"
    And The response body doesn't contain field "name.certified"
    And The response body doesn't contain field "fiscalCode"
    And The response body doesn't contain field "workContacts.ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv.email.value"
    And The response body doesn't contain field "workContacts.ID_MAIL#1234abcd-5678-ef90-ghij-klmnopqrstuv.email.certified"

  Scenario: Successfully get user's information from pdv with field workContracts
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                               |
    And The following query params:
      | field     | workContracts                                                         |
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
      | id        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                               |
    And The following query params:
      | field     | fiscalCode                                                         |
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
      | id        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                               |
    And The following query params:
      | field     | fiscalCode,name                                                    |
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
      | id        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                               |
    And The following query params:
      | field     | fiscalCode,name,familyName                                         |
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
      | id        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                               |
    And The following query params:
      | institutionId     | f2e4d6c8-9876-5432-ba10-abcdef123456                                         |
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
      | email.value           | r.balboa@unibologna.it                                          |
      | email.certified           | NONE                                          |

  Scenario: Successfully get user's information from pdv: name, familyName, email, fiscalCode and workContacts with parameter institutionId
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                               |
    And The following query params:
      | institutionId     | a1b2c3d4-5678-90ab-cdef-1234567890ab                                       |
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
      | email.value           | r.balboa@regionelazio.it                                          |
      | email.certified           | NONE                                          |


  Scenario: Successfully get user's information from pdv: name, familyName, email, fiscalCode and workContacts with wrong parameter institutionId
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                               |
    And The following query params:
      | institutionId     | asdasdasdasd                                       |
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
      | id        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                               |
    And The following query params:
      | field     | wrongfield                                       |
    When I send a GET request to "users/{id}/details"
    Then The status code is 500
    And The response body contains error string:
      | Something has gone wrong in the server                                             |

  Scenario: Unsuccessfully get user's information from pdv: name, familyName, email, fiscalCode and workContacts with wrong userId
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | id        | wronguserid                               |
    When I send a GET request to "users/{id}/details"
    Then The status code is 500
    And The response body contains error string:
      | Something has gone wrong in the server                                             |

  Scenario: Bad Token get user's information from pdv: name, familyName, email, fiscalCode and workContacts
    Given A bad jwt token
    And The following path params:
      | id            | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                               |
    When I send a GET request to "users/{id}/details"
    Then The status code is 401


  ######################### END /{id}/details #########################



  ######################### BEGIN /search #########################

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
      | institutionId                     | f2e4d6c8-9876-5432-ba10-abcdef123456                |
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
      | email.value           | r.balboa@unibologna.it                                          |
      | email.certified           | NONE                                          |
    And The response body doesn't contain field "fiscalCode"

  Scenario: Successfully search user by fiscalCode and institutionId
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | institutionId                     | a1b2c3d4-5678-90ab-cdef-1234567890ab                |
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
      | email.value           | r.balboa@regionelazio.it                                          |
      | email.certified           | NONE                                          |
    And The response body doesn't contain field "fiscalCode"

  Scenario: Successfully search user by fiscalCode and wrong institutionId
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | institutionId                     | asdasdasd                |
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
      | detail                                                  | User not found          |
      | status                                                  | 404                     |
      | title                                                   | User not found          |

  Scenario: Unsuccessfully search user without body
    Given User login with username "j.doe" and password "test"
    And The following request body:
      """
      {
      }
      """
    When I send a POST request to "users/search"
    Then The status code is 500
    And The response body contains error string:
      | Something has gone wrong in the server                                             |

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
    And The response body contains error string:
      | Something has gone wrong in the server                                             |

  Scenario: Bad Token search user by fiscalCode
    Given A bad jwt token
    And The following request body:
      """
      {
          "fiscalCode": "blbrki80A41H401T"
      }
      """                            |
    When I send a POST request to "users/search"
    Then The status code is 401

  ######################### END /search #########################

  ######################### BEGIN /{userId}/institutions/{institutionId}/products/{productId} #########################

  # Cancellazione riuscita
  @RemoveUserInstitutionAndUserInfoAfterScenario
  Scenario: Successfully delete logically the association institution and product
    Given User login with username "j.doe" and password "test"
    And A mock userInstitution with id "65a4b6c7d8e9f01234567890" and onboardedProductState "ACTIVE"
    And A mock institution with id "d0d28367-1695-4c50-a260-6fda526e9aab", institutionName "Comune di Milano", status "ACTIVE", role "SUB_DELEGATE" to userInfo document with id "35a78332-d038-4bfa-8e85-2cba7f6b7bf7"
    And The following path params:
      | institutionId        |  d0d28367-1695-4c50-a260-6fda526e9aab                             |
    And The following query params:
      | userId        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                               |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].id        | 65a4b6c7d8e9f01234567890                               |
    And The response body contains at path "[0].products" the following list of objects in any order:
     | status  |
     | ACTIVE  |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | userId                     | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                |
      | institutionId                     | d0d28367-1695-4c50-a260-6fda526e9aab                |
      | productId                     | prod-pagopa                |
    When I send a DELETE request to "users/{userId}/institutions/{institutionId}/products/{productId}"
    Then The status code is 204
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | institutionId        |  d0d28367-1695-4c50-a260-6fda526e9aab                             |
    And The following query params:
      | userId        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                               |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].id        | 65a4b6c7d8e9f01234567890                               |
    And The response body contains at path "[0].products" the following list of objects in any order:
      | status  |
      | DELETED  |

  # Cancellazione di prodotto già cancellato
  @RemoveUserInstitutionAndUserInfoAfterScenario
  Scenario: Unsuccessfully delete logically the association institution and product because product is already deleted
    Given User login with username "j.doe" and password "test"
    And A mock userInstitution with id "65a4b6c7d8e9f01234567890" and onboardedProductState "DELETED"
    And A mock institution with id "d0d28367-1695-4c50-a260-6fda526e9aab", institutionName "Comune di Milano", status "DELETED", role "SUB_DELEGATE" to userInfo document with id "35a78332-d038-4bfa-8e85-2cba7f6b7bf7"
    And The following path params:
      | institutionId        |  d0d28367-1695-4c50-a260-6fda526e9aab                             |
    And The following query params:
      | userId        | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                               |
    When I send a GET request to "institutions/{institutionId}/user-institutions"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].id        | 65a4b6c7d8e9f01234567890                               |
    And The response body contains at path "[0].products" the following list of objects in any order:
      | status  |
      | DELETED  |
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | userId                     | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                |
      | institutionId                     | d0d28367-1695-4c50-a260-6fda526e9aab                |
      | productId                     | prod-pagopa                |
    When I send a DELETE request to "users/{userId}/institutions/{institutionId}/products/{productId}"
    Then The status code is 404
    And The response body contains:
      | detail        | USER TO UPDATE NOT FOUND                               |
      | status        | 404                                                     |
      | title        | USER TO UPDATE NOT FOUND                                 |

  # productid inesistente per utente
  Scenario: Unsuccessfully delete logically the association institution and product because user is not registered with selected product
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | userId                     | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                |
      | institutionId                     | f2e4d6c8-9876-5432-ba10-abcdef123456                |
      | productId                     | prod-pagopa                |
    When I send a DELETE request to "users/{userId}/institutions/{institutionId}/products/{productId}"
    Then The status code is 404
    And The response body contains:
      | detail        | USER TO UPDATE NOT FOUND                               |
      | status        | 404                                                     |
      | title        | USER TO UPDATE NOT FOUND                                 |

  # productid sbagliato
  Scenario: Unsuccessfully delete logically the association institution and product because of wrong product id
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | userId                     | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                |
      | institutionId                     | f2e4d6c8-9876-5432-ba10-abcdef123456                |
      | productId                     | prod-wrongproductid                |
    When I send a DELETE request to "users/{userId}/institutions/{institutionId}/products/{productId}"
    Then The status code is 404
    And The response body contains:
      | detail        | USER TO UPDATE NOT FOUND                               |
      | status        | 404                                                     |
      | title        | USER TO UPDATE NOT FOUND                                 |

  # institutionid inesistente per utente
  Scenario: Unsuccessfully delete logically the association institution and product because of wrong institutionId
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | userId                     | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                |
      | institutionId                     | wronginstitutionid                |
      | productId                     | prod-io                |
    When I send a DELETE request to "users/{userId}/institutions/{institutionId}/products/{productId}"
    Then The status code is 404
    And The response body contains:
      | detail        | USER TO UPDATE NOT FOUND                               |
      | status        | 404                                                     |
      | title        | USER TO UPDATE NOT FOUND                                 |

  # userid inesistente
  Scenario: Unsuccessfully delete logically the association institution and product because of wrong userid
    Given User login with username "j.doe" and password "test"
    And The following path params:
      | userId                     | wronguserid                |
      | institutionId                     | f2e4d6c8-9876-5432-ba10-abcdef123456                |
      | productId                     | prod-pagopa                |
    When I send a DELETE request to "users/{userId}/institutions/{institutionId}/products/{productId}"
    Then The status code is 404
    And The response body contains:
      | detail        | USER TO UPDATE NOT FOUND                               |
      | status        | 404                                                     |
      | title        | USER TO UPDATE NOT FOUND                                 |

  # senza jwt
  Scenario: Bad Token delete logically the association institution and product
    Given A bad jwt token
    And The following path params:
      | userId                     | wronguserid                |
      | institutionId                     | f2e4d6c8-9876-5432-ba10-abcdef123456                |
      | productId                     | prod-pagopa                |
    When I send a DELETE request to "users/{userId}/institutions/{institutionId}/products/{productId}"
    Then The status code is 401

  ######################### END /{userId}/institutions/{institutionId}/products/{productId} #########################