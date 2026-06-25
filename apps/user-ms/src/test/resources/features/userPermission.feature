Feature: UserPermission

  ######################### BEGIN GET /authorize #########################

  Scenario: Successfully get permission for a user in an institution (user with MANAGER role on specified product, permission ANY)
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | institutionId                           | d0d28367-1695-4c50-a260-6fda526e9aab                  |
      | productId                               | prod-pagopa                                           |
      | permission                              | ANY                                                   |
    When I send a GET request to "/authorize"
    Then The status code is 200
    And The response body contains string:
      | true                                    |

  Scenario: Successfully get permission for a user in an institution (user with MANAGER role on specified product, permission ADMIN)
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | institutionId                           | d0d28367-1695-4c50-a260-6fda526e9aab                  |
      | productId                               | prod-pagopa                                           |
      | permission                              | ADMIN                                                 |
    When I send a GET request to "/authorize"
    Then The status code is 200
    And The response body contains string:
      | true                                    |

  Scenario: Successfully get permission for a user in an institution (user without a role on specified product)
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | institutionId                           | d0d28367-1695-4c50-a260-6fda526e9aab                  |
      | productId                               | prod-io                                               |
      | permission                              | ANY                                                   |
    When I send a GET request to "/authorize"
    Then The status code is 200
    And The response body contains string:
      | false                                   |

  Scenario: Unsuccessfully get permission for a user in an institution (user without a role on specified product but wrong permission)
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | institutionId                           | d0d28367-1695-4c50-a260-6fda526e9aab                  |
      | productId                               | prod-io                                               |
      | permission                              | WRONG_PERMISSION                                      |
    When I send a GET request to "/authorize"
    Then The status code is 500
    And The response body contains string:
      | Something has gone wrong in the server                                                          |

  Scenario: Unsuccessfully get permission for a user in an institution (without permission)
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | institutionId                           | d0d28367-1695-4c50-a260-6fda526e9aab                  |
      | productId                               | prod-io                                               |
    When I send a GET request to "/authorize"
    Then The status code is 400
    And The response body contains:
      | title                                   | Constraint Violation                                  |
      | status                                  | 400                                                   |
    And The response body contains the list "violations" of size 1
    And The response body contains at path "violations" the following list of objects in any order:
      | field                                   | message                                               |
      | getPermission.permission                | Permission type is required                           |

  Scenario: Bad Token get permission for a user in an institution
    Given A bad jwt token
    And The following query params:
      | institutionId                           | d0d28367-1695-4c50-a260-6fda526e9aab                  |
      | productId                               | prod-io                                               |
      | permission                              | ANY                                                   |
    When I send a GET request to "/authorize"
    Then The status code is 401

  ######################### END GET /authorize #########################