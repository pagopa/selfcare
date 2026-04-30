Feature: Events

  Scenario: Successfully resend all user events based on institutionId, userId, and fromDate (with fromDate parameter)
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | fromDate                         | 2020-03-27T14:30:00Z                                             |
    When I send a POST request to "events/sc-users"
    Then The status code is 204

  Scenario: Successfully resend all user events based on institutionId, userId, and fromDate (with fromDate, institutionId and userId parameters)
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | fromDate                         | 2020-03-27T14:30:00Z                                             |
      | institutionId                    | a1b2c3d4-5678-90ab-cdef-1234567890ab                             |
      | userId                           | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                             |
    When I send a POST request to "events/sc-users"
    Then The status code is 204

  Scenario: Successfully resend all user events based on institutionId, userId, and fromDate (with fromDate and institutionId parameters)
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | fromDate                         | 2020-03-27T14:30:00Z                                             |
      | institutionId                    | a1b2c3d4-5678-90ab-cdef-1234567890ab                             |
    When I send a POST request to "events/sc-users"
    Then The status code is 204

  Scenario: Successfully resend all user events based on institutionId, userId, and fromDate (with fromDate and userId parameters)
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | fromDate                         | 2020-03-27T14:30:00Z                                             |
      | userId                           | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7                             |
    When I send a POST request to "events/sc-users"
    Then The status code is 204

  Scenario: Successfully resend all user events based on institutionId, userId, and fromDate (with fromDate parameter and institutionId and userId wrong parameters)
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | fromDate                         | 2020-03-27T14:30:00Z                                             |
      | institutionId                    | wrongInstitutionId                                               |
      | userId                           | wrongUserId                                                      |
    When I send a POST request to "events/sc-users"
    Then The status code is 204

  Scenario: Unsuccessfully resend all user events based on institutionId, userId, and fromDate (without parameters)
    Given User login with username "j.doe" and password "test"
    When I send a POST request to "events/sc-users"
    Then The status code is 500
    And The response body contains string:
      | Something has gone wrong in the server                                                              |

  Scenario: Unsuccessfully resend all user events based on institutionId, userId, and fromDate (with wrong fromDate)
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | fromDate                         | 200-14-27T14:30:00Z                                              |
    When I send a POST request to "events/sc-users"
    Then The status code is 500
    And The response body contains string:
      | Something has gone wrong in the server                                                              |

  Scenario: Bad Token resend all user events based on institutionId, userId, and fromDate
    Given A bad jwt token
    And The following query params:
      | fromDate                         | 2020-03-27T14:30:00Z                                             |
    When I send a POST request to "events/sc-users"
    Then The status code is 401