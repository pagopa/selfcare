Feature: Update User Group

  @FirstUpdateScenario
  Scenario: Successfully suspend a group with a valid ID
    Given I have a valid group ID to update: "6759f8df78b6af202b222d29"
    When I send a POST request to "/v1/user-groups/{groupId}/suspend" with authentication "true"
    Then [UPDATE] the response status should be 204
    And the retrieved group should be changed status to "SUSPENDED"

  Scenario: Attempt to suspend a group with a non-existent ID
    Given I have a non-existent group ID "99999"
    When I send a POST request to "/v1/user-groups/{groupId}/suspend" with authentication "true"
    Then [UPDATE] the response status should be 404
    And [UPDATE] the response should contain an error message "Not Found"

  Scenario: Successfully activate a group with a valid ID
    Given I have a valid group ID to update: "6759f8df78b6af202b222d29"
    When I send a POST request to "/v1/user-groups/{groupId}/activate" with authentication "true"
    Then [UPDATE] the response status should be 204
    And the retrieved group should be changed status to "ACTIVE"

  Scenario: Attempt to activate a group with a non-existent ID
    Given I have a non-existent group ID "99999"
    When I send a POST request to "/v1/user-groups/{groupId}/activate" with authentication "true"
    Then [UPDATE] the response status should be 404
    And [UPDATE] the response should contain an error message "Not Found"

  Scenario: Successfully delete a group with a valid ID
    Given I have a valid group ID to update: "6759f8df78b6af202b222d29"
    When I send a DELETE request to "/v1/user-groups/{groupId}" with authentication "true"
    Then [UPDATE] the response status should be 204
    And the retrieved group should be changed status to "DELETED"

  Scenario: Attempt to delete a group with a non-existent ID
    Given I have a non-existent group ID "99999"
    When I send a DELETE request to "/v1/user-groups/{groupId}" with authentication "true"
    Then [UPDATE] the response status should be 404
    And [UPDATE] the response should contain an error message "Not Found"

  Scenario: Successfully update a group with a valid ID and valid data
    Given I have a valid group ID to update: "6759f8df78b6af202b222d29"
    And I have data to update:
      | name          | description          | members                                                                    |
      | updated Name  | updatedDescription   | 525db33f-967f-4a82-8984-c606225e714a,a1b7c86b-d195-41d8-8291-7c3467abfd30  |
    When I send a PUT request to "/v1/user-groups/{groupId}" with authentication "true"
    Then [UPDATE] the response status should be 200
    And the retrieved group should be updated

  Scenario: Attempt to update a non-existent group
    Given I have a non-existent group ID "99999"
    And I have data to update:
      | name        | description  | members                                                                   |
      | Group Name  | TestGroup    | 525db33f-967f-4a82-8984-c606225e714a,a1b7c86b-d195-41d8-8291-7c3467abfd30 |
    When I send a PUT request to "/v1/user-groups/{groupId}" with authentication "true"
    Then [UPDATE] the response status should be 404
    And [UPDATE] the response should contain an error message "Not Found"

  @LastUpdateScenario
  Scenario: Attempt to update a suspended group
    Given I have a valid group ID to update: "6759f8df78b6af202b222d2a"
    And I have data to update:
      | name        | description  | members                                                                   |
      | Group Name  | TestGroup    | 525db33f-967f-4a82-8984-c606225e714a,a1b7c86b-d195-41d8-8291-7c3467abfd30 |
    When I send a PUT request to "/v1/user-groups/{groupId}" with authentication "true"
    Then [UPDATE] the response status should be 400
    And [UPDATE] the response should contain an error message "Trying to modify suspended group"



