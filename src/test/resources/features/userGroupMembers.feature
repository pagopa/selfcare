@FeatureMembers
Feature: User Group Members

  @FirstGroupMembersScenario
  Scenario: Successfully add a member to a group
    Given [MEMBERS] user login with username "j.doe" and password "test"
    And I have group ID "6759f8df78b6af202b222d29" and member ID "78bb7f07-0464-4ff9-a0ee-82568902b7bf"
    When I send a PUT request to "/v1/user-groups/{id}/members/{memberId}"
    Then [MEMBERS] the response status should be 204

  Scenario: Attempt to add a member to a non-existent group
    Given [MEMBERS] user login with username "j.doe" and password "test"
    And I have group ID "99999" and member ID "78bb7f07-0464-4ff9-a0ee-82568902b7bf"
    When I send a PUT request to "/v1/user-groups/{id}/members/{memberId}"
    Then [MEMBERS] the response status should be 404
    And [MEMBERS] the response should contain an error message "Not Found"

  Scenario: Attempt to add a member to a suspended group
    Given [MEMBERS] user login with username "j.doe" and password "test"
    And I have group ID "6759f8df78b6af202b222d2b" and member ID "78bb7f07-0464-4ff9-a0ee-82568902b7bf"
    When I send a PUT request to "/v1/user-groups/{id}/members/{memberId}"
    Then [MEMBERS] the response status should be 400
    And [MEMBERS] the response should contain an error message "Trying to modify suspended group"

  Scenario: Successfully delete a member from a group
    Given [MEMBERS] user login with username "j.doe" and password "test"
    And I have group ID "6759f8df78b6af202b222d29" and member ID "78bb7f07-0464-4ff9-a0ee-82568902b7bf"
    When [MEMBERS] I send a DELETE request to "/v1/user-groups/{id}/members/{memberId}"
    Then [MEMBERS] the response status should be 204

  Scenario: Attempt to delete a member from a non-existent group
    Given [MEMBERS] user login with username "j.doe" and password "test"
    And I have group ID "99999" and member ID "78bb7f07-0464-4ff9-a0ee-82568902b7bf"
    When [MEMBERS] I send a DELETE request to "/v1/user-groups/{id}/members/{memberId}"
    Then [MEMBERS] the response status should be 404
    And [MEMBERS] the response should contain an error message "Not Found"

  Scenario: Attempt to delete a member from a suspended group
    Given [MEMBERS] user login with username "j.doe" and password "test"
    And I have group ID "6759f8df78b6af202b222d2b" and member ID "78bb7f07-0464-4ff9-a0ee-82568902b7bf"
    When [MEMBERS] I send a DELETE request to "/v1/user-groups/{id}/members/{memberId}"
    Then [MEMBERS] the response status should be 400
    And [MEMBERS] the response should contain an error message "Trying to modify suspended group"

  Scenario: Attempt to delete a member with non-existent member ID
    Given [MEMBERS] user login with username "j.doe" and password "test"
    And I have group ID "6759f8df78b6af202b222d29" and member ID "f71dcad0-3374-4b51-91b8-75a9b36a5696"
    When [MEMBERS] I send a DELETE request to "/v1/user-groups/{id}/members/{memberId}"
    Then [MEMBERS] the response status should be 204

  Scenario: Successfully delete a member from all groups associated with institutionId and productId
    Given [MEMBERS] user login with username "j.doe" and password "test"
    And I have a member id "75003d64-7b8c-4768-b20c-cf66467d44c7", institution id "9c8ae123-d990-4400-b043-67a60aff31bc" and product id "prod-test"
    When I send a DELETE request to "/v1/user-groups/members/{memberId}" with query parameters
    Then [MEMBERS] the response status should be 204

  Scenario: Attempt to delete a member with a missing institution ID
    Given [MEMBERS] user login with username "j.doe" and password "test"
    And I have a member id "75003d64-7b8c-4768-b20c-cf66467d44c7" and product id "prod-test"
    When I send a DELETE request to "/v1/user-groups/members/{memberId}" with query parameters
    Then [MEMBERS] the response status should be 500
    And [MEMBERS] the response should contain an error message "A institution id is required"

  Scenario: Attempt to delete a member with a missing product ID
    Given [MEMBERS] user login with username "j.doe" and password "test"
    And I have a member id "75003d64-7b8c-4768-b20c-cf66467d44c7" and institution id "9c8ae123-d990-4400-b043-67a60aff31bc"
    When I send a DELETE request to "/v1/user-groups/members/{memberId}" with query parameters
    Then [MEMBERS] the response status should be 500
    And [MEMBERS] the response should contain an error message "A product id is required"

  Scenario: Attempt to delete a member who is not in any group associated with the given institutionId and productId
    Given [MEMBERS] user login with username "j.doe" and password "test"
    And I have a member id "19017fde-142a-4e11-9498-a12a746d0f15", institution id "9c8ae123-d990-4400-b043-67a60aff31bc" and product id "prod-test"
    When I send a DELETE request to "/v1/user-groups/members/{memberId}" with query parameters
    Then [MEMBERS] the response status should be 204


  Scenario: Successfully add members to a group
    Given [MEMBERS] user login with username "j.doe" and password "test"
    And the following add members to user group request details:
      | productId | institutionId                        | parentInstitutionId                  | members                                                                   |
      | prod-test | 9c7ae123-d990-4400-b043-67a60aff31bc | 5d1ae124-d870-4400-b043-67a60aff32cb | 525db33f-967f-4a82-8984-c606225e714a,a1b7c86b-d195-41d8-8291-7c3467abfd30 |
    When I send a PUT request to "/v1/user-groups/members" to add members to a group
    Then [MEMBERS] the response status should be 204
    # UPDATE
    Given [RETRIEVE] user login with username "j.doe" and password "test"
    And I have valid filters institutionId "9c7ae123-d990-4400-b043-67a60aff31bc" productId "prod-test" and status "ACTIVE"
    When I send a GET request to "/v1/user-groups" to retrieve userGroups
    Then [RETRIEVE] the response status should be 200
    And the response should contain 1 item
    And the response should contain the following list of members:
      | 525db33f-967f-4a82-8984-c606225e714a |
      | 75003d64-7b8c-4768-b20c-cf66467d44c7 |
      | a1b7c86b-d195-41d8-8291-7c3467abfd30 |

  Scenario: Attempt to add members without institutionId
    Given [MEMBERS] user login with username "j.doe" and password "test"
    And the following add members to user group request details:
      | productId | institutionId | parentInstitutionId                  | members                                                                   |
      | prod-test |               | 5d1ae124-d870-4400-b043-67a60aff32cb | 525db33f-967f-4a82-8984-c606225e714a,a1b7c86b-d195-41d8-8291-7c3467abfd30 |
    When I send a PUT request to "/v1/user-groups/members" to add members to a group
    Then [MEMBERS] the response status should be 400
    And [MEMBERS] the response should contain an error message "addMembersToUserGroupDto.institutionId,must not be blank"

  Scenario: Attempt to add members without parentInstitutionId
    Given [MEMBERS] user login with username "j.doe" and password "test"
    And the following add members to user group request details:
      | productId | institutionId                        | parentInstitutionId | members                                                                   |
      | prod-test | 9c7ae123-d990-4400-b043-67a60aff31bc |                     | 525db33f-967f-4a82-8984-c606225e714a,a1b7c86b-d195-41d8-8291-7c3467abfd30 |
    When I send a PUT request to "/v1/user-groups/members" to add members to a group
    Then [MEMBERS] the response status should be 400
    And [MEMBERS] the response should contain an error message "addMembersToUserGroupDto.parentInstitutionId,must not be blank"

  Scenario: Attempt to add members with empty member list
    Given [MEMBERS] user login with username "j.doe" and password "test"
    And the following add members to user group request details:
      | productId | institutionId                        | parentInstitutionId                  | members |
      | prod-test | 9c7ae123-d990-4400-b043-67a60aff31bc | 5d1ae124-d870-4400-b043-67a60aff32cb |         |
    When I send a PUT request to "/v1/user-groups/members" to add members to a group
    Then [MEMBERS] the response status should be 400
    And [MEMBERS] the response should contain an error message "addMembersToUserGroupDto.members,must not be empty"

  Scenario: Attempt to add members to a non-existent group
    Given [MEMBERS] user login with username "j.doe" and password "test"
    And the following add members to user group request details:
      | productId  | institutionId | parentInstitutionId | members                                                                   |
      | product123 | notfound      | inst456             | 525db33f-967f-4a82-8984-c606225e714a,a1b7c86b-d195-41d8-8291-7c3467abfd30 |
    When I send a PUT request to "/v1/user-groups/members" to add members to a group
    Then [MEMBERS] the response status should be 404
    And [MEMBERS] the response should contain an error message "Not Found"

  Scenario: Successfully delete members from a group
    Given [MEMBERS] user login with username "j.doe" and password "test"
    And the following members to delete from user group request details:
      | productId | institutionId                        | parentInstitutionId                  | members                                                                   |
      | prod-test | 9c7ae123-d990-4400-b043-67a60aff3116 | 5d1ae124-d870-4400-b043-67a60aff32df | 75003d64-7b8c-4768-b20c-cf66467d4124,75003d64-7b8c-4768-b20c-cf66467d4125 |
    When I send a DELETE request to "/v1/user-groups/members" to delete members from a group
    Then [MEMBERS] the response status should be 204
    Given [RETRIEVE] user login with username "j.doe" and password "test"
    And I have valid filters institutionId "9c7ae123-d990-4400-b043-67a60aff3116" productId "prod-test" and status "ACTIVE"
    When I send a GET request to "/v1/user-groups" to retrieve userGroups
    Then [RETRIEVE] the response status should be 200
    And the response should contain 1 item
    And the response should contain the following list of members:
      | 75003d64-7b8c-4768-b20c-cf66467d4123 |

  Scenario: Attempt to delete members without institutionId
    Given [MEMBERS] user login with username "j.doe" and password "test"
    And the following members to delete from user group request details:
      | productId | institutionId | parentInstitutionId                  | members                                                                   |
      | prod-test |               | 5d1ae124-d870-4400-b043-67a60aff32cb | 525db33f-967f-4a82-8984-c606225e714a,a1b7c86b-d195-41d8-8291-7c3467abfd30 |
    When I send a DELETE request to "/v1/user-groups/members" to delete members from a group
    Then [MEMBERS] the response status should be 400
    And [MEMBERS] the response should contain an error message "deleteMembersFromUserGroupDto.institutionId,must not be blank"

  Scenario: Attempt to delete members without parentInstitutionId
    Given [MEMBERS] user login with username "j.doe" and password "test"
    And the following members to delete from user group request details:
      | productId | institutionId                        | parentInstitutionId | members                                                                   |
      | prod-test | 9c7ae123-d990-4400-b043-67a60aff31bc |                     | 525db33f-967f-4a82-8984-c606225e714a,a1b7c86b-d195-41d8-8291-7c3467abfd30 |
    When I send a DELETE request to "/v1/user-groups/members" to delete members from a group
    Then [MEMBERS] the response status should be 400
    And [MEMBERS] the response should contain an error message "deleteMembersFromUserGroupDto.parentInstitutionId,must not be blank"

  Scenario: Attempt to delete members with empty member list
    Given [MEMBERS] user login with username "j.doe" and password "test"
    And the following members to delete from user group request details:
      | productId | institutionId                        | parentInstitutionId                  | members |
      | prod-test | 9c7ae123-d990-4400-b043-67a60aff31bc | 5d1ae124-d870-4400-b043-67a60aff32cb |         |
    When I send a DELETE request to "/v1/user-groups/members" to delete members from a group
    Then [MEMBERS] the response status should be 400
    And [MEMBERS] the response should contain an error message "deleteMembersFromUserGroupDto.members,must not be empty"

  @LastGroupMembersScenario
  Scenario: Attempt to delete members from a non-existent group
    Given [MEMBERS] user login with username "j.doe" and password "test"
    And the following members to delete from user group request details:
      | productId  | institutionId | parentInstitutionId | members                                                                   |
      | product123 | notfound      | inst456             | 525db33f-967f-4a82-8984-c606225e714a,a1b7c86b-d195-41d8-8291-7c3467abfd30 |
    When I send a DELETE request to "/v1/user-groups/members" to delete members from a group
    Then [MEMBERS] the response status should be 404
    And [MEMBERS] the response should contain an error message "Not Found"