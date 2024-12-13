Feature: User Group Members

  @FirstGroupMembersScenario
  Scenario: Successfully add a member to a group
    Given I have group ID "6759f8df78b6af202b222d29" and member ID "78bb7f07-0464-4ff9-a0ee-82568902b7bf"
    When I send a PUT request to "/v1/user-groups/{id}/members/{memberId}"
    Then [MEMBERS] the response status should be 204

  Scenario: Attempt to add a member to a non-existent group
    Given I have group ID "99999" and member ID "78bb7f07-0464-4ff9-a0ee-82568902b7bf"
    When I send a PUT request to "/v1/user-groups/{id}/members/{memberId}"
    Then [MEMBERS] the response status should be 404
    And [MEMBERS] the response should contain an error message "Not Found"

  Scenario: Attempt to add a member to a suspended group
    Given I have group ID "6759f8df78b6af202b222d2b" and member ID "78bb7f07-0464-4ff9-a0ee-82568902b7bf"
    When I send a PUT request to "/v1/user-groups/{id}/members/{memberId}"
    Then [MEMBERS] the response status should be 400
    And [MEMBERS] the response should contain an error message "Trying to modify suspended group"

  Scenario: Successfully delete a member from a group
    Given I have group ID "6759f8df78b6af202b222d29" and member ID "78bb7f07-0464-4ff9-a0ee-82568902b7bf"
    When I send a DELETE request to "/v1/user-groups/{id}/members/{memberId}"
    Then [MEMBERS] the response status should be 204

  Scenario: Attempt to delete a member from a non-existent group
    Given I have group ID "99999" and member ID "78bb7f07-0464-4ff9-a0ee-82568902b7bf"
    When I send a DELETE request to "/v1/user-groups/{id}/members/{memberId}"
    Then [MEMBERS] the response status should be 404
    And [MEMBERS] the response should contain an error message "Not Found"

  Scenario: Attempt to delete a member from a suspended group
    Given I have group ID "6759f8df78b6af202b222d2b" and member ID "78bb7f07-0464-4ff9-a0ee-82568902b7bf"
    When I send a DELETE request to "/v1/user-groups/{id}/members/{memberId}"
    Then [MEMBERS] the response status should be 400
    And [MEMBERS] the response should contain an error message "Trying to modify suspended group"

  Scenario: Attempt to delete a member with non-existent member ID
    Given I have group ID "6759f8df78b6af202b222d29" and member ID "f71dcad0-3374-4b51-91b8-75a9b36a5696"
    When I send a DELETE request to "/v1/user-groups/{id}/members/{memberId}"
    Then [MEMBERS] the response status should be 204

  Scenario: Successfully delete a member from all groups associated with institutionId and productId
    Given I have a member id "75003d64-7b8c-4768-b20c-cf66467d44c7", institution id "9c8ae123-d990-4400-b043-67a60aff31bc" and product id "prod-test"
    When I send a DELETE request to "/v1/user-groups/members/{memberId}" with query parameters
    Then [MEMBERS] the response status should be 204

  Scenario: Attempt to delete a member with a missing institution ID
    Given I have a member id "75003d64-7b8c-4768-b20c-cf66467d44c7" and product id "prod-test"
    When I send a DELETE request to "/v1/user-groups/members/{memberId}" with query parameters
    Then [MEMBERS] the response status should be 500
    And [MEMBERS] the response should contain an error message "A institution id is required"

  Scenario: Attempt to delete a member with a missing product ID
    Given I have a member id "75003d64-7b8c-4768-b20c-cf66467d44c7" and institution id "9c8ae123-d990-4400-b043-67a60aff31bc"
    When I send a DELETE request to "/v1/user-groups/members/{memberId}" with query parameters
    Then [MEMBERS] the response status should be 500
    And [MEMBERS] the response should contain an error message "A product id is required"

  @LastGroupMembersScenario
  Scenario: Attempt to delete a member who is not in any group associated with the given institutionId and productId
    Given I have a member id "19017fde-142a-4e11-9498-a12a746d0f15", institution id "9c8ae123-d990-4400-b043-67a60aff31bc" and product id "prod-test"
    When I send a DELETE request to "/v1/user-groups/members/{memberId}" with query parameters
    Then [MEMBERS] the response status should be 204

