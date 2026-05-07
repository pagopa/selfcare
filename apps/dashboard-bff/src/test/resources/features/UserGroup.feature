@userGroup
Feature: UserGroups

  Scenario: Successfully create a new user group
    Given user login with username "j.doe" and password "test"
    And the following user group details:
      | name       | description | productId  | institutionId                        | members                                                                   |
      | Group Name | TestGroup   | prod-io    | c9a50656-f345-4c81-84be-5b2474470544 | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7,97a511a7-2acc-47b9-afed-2f3c65753b4a |
    When I send a POST request to "/v2/user-groups/" with the given details to create usergroup
    Then the response status should be 201
    And the response should contain a valid user group id

  Scenario: Attempt to create a group without permissions
    Given user login with username "r.balboa" and password "test"
    And the following user group details:
      | name       | description | productId       | institutionId                        | members                                                                   |
      | Group Name | TestGroup   | prod-io-premium | c9a50656-f345-4c81-84be-5b2474470544 | 525db33f-967f-4a82-8984-c606225e714a,a1b7c86b-d195-41d8-8291-7c3467abfd30 |
    When I send a POST request to "/v2/user-groups/" with the given details to create usergroup
    Then the response status should be 403

  Scenario: Attempt to create a group with invalid members:
    Given user login with username "j.doe" and password "test"
    And the following user group details:
      | name       | description | productId  | institutionId | members                             |
      | Group Name | TestGroup   | prod-io    | c9a50656-f345-4c81-84be-5b2474470544       | 97a511a7-2acc-47b9-afed-2f3c65753ccc|
    When I send a POST request to "/v2/user-groups/" with the given details to create usergroup
    Then the response status should be 500
    And the response should contain an error message "Some members in the list aren't allowed for this institution"

  Scenario: Attempt to create a user group with a duplicate name
    Given user login with username "j.doe" and password "test"
    And the following user group details:
      | name       | description | productId  | institutionId                        | members                                                                   |
      | Group Name | TestGroup   | prod-io    | c9a50656-f345-4c81-84be-5b2474470544 | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7,97a511a7-2acc-47b9-afed-2f3c65753b4a |
    When I send a POST request to "/v2/user-groups/" with the given details to create usergroup
    Then the response status should be 409

  Scenario: Attempt to create a user group with missing required fields (name)
    Given user login with username "j.doe" and password "test"
    And the following user group details:
      | description | productId  | institutionId                        | members                                                                   |
      | TestGroup   | prod-io    | c9a50656-f345-4c81-84be-5b2474470544 | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7,97a511a7-2acc-47b9-afed-2f3c65753b4a |
    When I send a POST request to "/v2/user-groups/" with the given details to create usergroup
    Then the response status should be 400
    And the response should contain an error message "createUserGroupDto.name,must not be blank"

  Scenario: Attempt to create a user group with missing required fields (institutionId)
    Given user login with username "j.doe" and password "test"
    And the following user group details:
      | name       | description | productId   | members                                                                   |
      | Group Name | TestGroup   | prod-io     | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7,97a511a7-2acc-47b9-afed-2f3c65753b4a |
    When I send a POST request to "/v2/user-groups/" with the given details to create usergroup
    Then the response status should be 400
    And the response should contain an error message "createUserGroupDto.institutionId,must not be blank"

  Scenario: Attempt to create a user group with missing required fields (productId)
    Given user login with username "j.doe" and password "test"
    And the following user group details:
      | name       | description  | institutionId                        | members                                                                   |
      | Group Name | TestGroup    | c9a50656-f345-4c81-84be-5b2474470544 | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7,97a511a7-2acc-47b9-afed-2f3c65753b4a |
    When I send a POST request to "/v2/user-groups/" with the given details to create usergroup
    Then the response status should be 400
    And the response should contain an error message "createUserGroupDto.productId,must not be blank"

  Scenario: Attempt to create a user group with missing required fields (description)
    Given user login with username "j.doe" and password "test"
    And the following user group details:
      | name        | productId  | institutionId                        | members                                                                   |
      | Group Name  | prod-io    | c9a50656-f345-4c81-84be-5b2474470544 | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7,97a511a7-2acc-47b9-afed-2f3c65753b4a |
    When I send a POST request to "/v2/user-groups/" with the given details to create usergroup
    Then the response status should be 400
    And the response should contain an error message "createUserGroupDto.description,must not be blank"

  Scenario: Attempt to create a user group with missing required fields (members)
    Given user login with username "j.doe" and password "test"
    And the following user group details:
      | name       | description | productId  | institutionId                        |
      | Group Name | TestGroup   | prod-io    | c9a50656-f345-4c81-84be-5b2474470544 |
    When I send a POST request to "/v2/user-groups/" with the given details to create usergroup
    Then the response status should be 400
    And the response should contain an error message "createUserGroupDto.members,must not be empty"

  Scenario: Successfully update a group with a valid ID and valid data
    Given user login with username "j.doe" and password "test"
    And I have groupId "6759f8df78b6af202b222d29"
    And I have data to update:
      | name         | description        | members                                                                   |
      | updated Name | updatedDescription | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7,97a511a7-2acc-47b9-afed-2f3c65753b4a |
    When I send a PUT request to "/v2/user-groups/{groupId}" to update userGroup
    Then the response status should be 204

  Scenario: Attempt to update a group with invalid members:
    Given user login with username "j.doe" and password "test"
    And I have groupId "6759f8df78b6af202b222d29"
    And I have data to update:
      | name         | description        | members                                                                   |
      | updated Name | updatedDescription | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7,97a511a7-2acc-47b9-afed-2f3c65753ccc |
    When I send a PUT request to "/v2/user-groups/{groupId}" to update userGroup
    Then the response status should be 500
    And the response should contain an error message "Some members in the list aren't allowed for this institution"

  Scenario: Attempt to update a non-existent group
    Given user login with username "j.doe" and password "test"
    And I have groupId "non-existent-group-id"
    And I have data to update:
      | name         | description        | members                                                                   |
      | updated Name | updatedDescription | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7,97a511a7-2acc-47b9-afed-2f3c65753ccc |
    When I send a PUT request to "/v2/user-groups/{groupId}" to update userGroup
    Then the response status should be 404


  Scenario: Successfully suspend a group
    Given user login with username "j.doe" and password "test"
    And I have groupId "6759f8df78b6af202b222d29"
    When I send a POST request to "/v2/user-groups/{groupId}/suspend" to update userGroup status
    Then the response status should be 204


  Scenario: Attempt to update a suspended group
    Given user login with username "j.doe" and password "test"
    And I have groupId "6759f8df78b6af202b222d29"
    And I have data to update:
      | name       | description | members                                                                   |
      | Group Name | TestGroup   | 97a511a7-2acc-47b9-afed-2f3c65753b4a|
    When I send a PUT request to "/v2/user-groups/{groupId}" to update userGroup
    Then the response status should be 400


  Scenario: Attempt to suspend a non-existent group
    Given user login with username "j.doe" and password "test"
    And I have groupId "non-existent-group-id"
    When I send a POST request to "/v2/user-groups/{groupId}/suspend" to update userGroup status
    Then the response status should be 404


  Scenario: Successfully activate a group
    Given user login with username "j.doe" and password "test"
    And I have groupId "6759f8df78b6af202b222d29"
    When I send a POST request to "/v2/user-groups/{groupId}/activate" to update userGroup status
    Then the response status should be 204
    And the retrieved group should be changed status to "ACTIVE"


  Scenario: Attempt to activate a non-existent group
    Given user login with username "j.doe" and password "test"
    And I have groupId "non-existent-group-id"
    When I send a POST request to "/v2/user-groups/{groupId}/activate" to update userGroup status
    Then the response status should be 404


  Scenario: Successfully delete a group
    Given user login with username "j.doe" and password "test"
    And I have groupId "6759f8df78b6af202b222d2a"
    When I send a DELETE request to "/v2/user-groups/{groupId}" to delete userGroup
    Then the response status should be 204
    And the retrieved group should be changed status to "DELETED"


  Scenario: Attempt to delete a non-existent group
    Given user login with username "j.doe" and password "test"
    And I have groupId "non-existent-group-id"
    When I send a DELETE request to "/v2/user-groups/{groupId}" to delete userGroup
    Then the response status should be 404


  Scenario: Successfully delete a member from a group
    Given user login with username "j.doe" and password "test"
    And I have groupId "6759f8df78b6af202b222d29"
    And I have memberId "35a78332-d038-4bfa-8e85-2cba7f6b7bf7"
    When I send a DELETE request to "/v2/user-groups/{userGroupId}/members/{userId}" to delete member
    Then the response status should be 204


  Scenario: Attempt to delete a member from a non-existent group
    Given user login with username "j.doe" and password "test"
    And I have groupId "99999"
    And I have memberId "78bb7f07-0464-4ff9-a0ee-82568902b7bf"
    When I send a DELETE request to "/v2/user-groups/{userGroupId}/members/{userId}" to delete member
    Then the response status should be 404


  Scenario: Attempt to delete a member from a suspended group
    Given user login with username "j.doe" and password "test"
    And I have groupId "6759f8df78b6af202b222d2b"
    And I have memberId "78bb7f07-0464-4ff9-a0ee-82568902b7bf"
    When I send a DELETE request to "/v2/user-groups/{userGroupId}/members/{userId}" to delete member
    Then the response status should be 400


  Scenario: Attempt to delete a member with non-existent member ID
    Given user login with username "j.doe" and password "test"
    And I have groupId "6759f8df78b6af202b222d29"
    And I have memberId "f71dcad0-3374-4b51-91b8-75a9b36a5696"
    When I send a DELETE request to "/v2/user-groups/{userGroupId}/members/{userId}" to delete member
    Then the response status should be 204

  Scenario: Attempt to retrieve a non-existent group
    Given user login with username "j.doe" and password "test"
    And I have groupId "non-existent-group-id"
    When I send a GET request to "/v2/user-groups/{id}" to retrieve userGroup
    Then the response status should be 404

  Scenario: Attempt to retrieve a group with a valid ID
    Given user login with username "j.doe" and password "test"
    And I have groupId "6759f8df78b6af202b222d29"
    When I send a GET request to "/v2/user-groups/{id}" to retrieve userGroup
    Then the response status should be 200
    And the response should contain the group details

  Scenario: Attempt to retrieve a group of which you are a member
    Given user login with username "j.doe" and password "test"
    And I have groupId "6759f8df78b6af202b222d29"
    When I send a GET request to "/v2/user-groups/me/{id}" to retrieve userGroup
    Then the response status should be 200
    And the response should contain the group details

  Scenario: Attempt to retrieve a group of which you are NOT a member
    Given user login with username "j.doe" and password "test"
    And I have groupId "68b703f2e027600fe56ceabf"
    When I send a GET request to "/v2/user-groups/me/{id}" to retrieve userGroup
    Then the response status should be 404

  Scenario: Successfully retrieve user groups with valid filters
    Given user login with username "j.doe" and password "test"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    And the productId is "prod-io"
    And I have memberId "35a78332-d038-4bfa-8e85-2cba7f6b7bf7"
    When I send a GET request to "/v2/user-groups" to retrieve userGroups
    Then the response status should be 200
    And the response should contain a paginated list of user groups of 1 items on page 0
    And the response should contain 1 total pages with 1 total elements

  Scenario: Successfully retrieve user groups without memberId
    Given user login with username "j.doe" and password "test"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    And the productId is "prod-io"
    When I send a GET request to "/v2/user-groups" to retrieve userGroups
    Then the response status should be 200
    And the response should contain a paginated list of user groups of 4 items on page 0
    And the response should contain 1 total pages with 4 total elements

  Scenario: Successfully retrieve a paginated list of user groups
    Given user login with username "j.doe" and password "test"
    And I set the page number to 0 and page size to 2
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    And the productId is "prod-io"
    When I send a GET request to "/v2/user-groups" to retrieve userGroups
    Then the response status should be 200
    And the response should contain a paginated list of user groups of 2 items on page 0
    And the response should contain 2 total pages with 4 total elements

  Scenario: Successfully retrieve a paginated list of user groups
    Given user login with username "j.doe" and password "test"
    And I set the page number to 1 and page size to 2
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    And the productId is "prod-io"
    When I send a GET request to "/v2/user-groups" to retrieve userGroups
    Then the response status should be 200
    And the response should contain a paginated list of user groups of 2 items on page 1
    And the response should contain 2 total pages with 4 total elements

  Scenario: Access denied while searching groups with an operator user
    Given user login with username "j.doe" and password "test"
    And the institutionId is "467ac77d-7faa-47bf-a60e-38ea74bd5fd2"
    And the productId is "prod-interop"
    And I have memberId "35a78332-d038-4bfa-8e85-2cba7f6b7bf7"
    When I send a GET request to "/v2/user-groups" to retrieve userGroups
    Then the response status should be 403

  Scenario: No user groups found for the provided filters
    Given user login with username "j.doe" and password "test"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    And the productId is "prod-pagopa"
    When I send a GET request to "/v2/user-groups" to retrieve userGroups
    Then the response status should be 200
    And the response should contain an empty group list

  Scenario: Successfully retrieve groups the user is member of
    Given user login with username "j.doe" and password "test"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    And the productId is "prod-io"
    When I send a GET request to "/v2/user-groups/me" to retrieve userGroups
    Then the response status should be 200
    And the response should contain a paginated list of user groups of 3 items on page 0
    And the response should contain 1 total pages with 3 total elements
    And the response should not contain userGroup "68b703f2e027600fe56ceabf"

  Scenario: Successfully add a member to a group
    Given user login with username "j.doe" and password "test"
    And I have groupId "6759f8df78b6af202b222d29"
    And I have memberId "35a78332-d038-4bfa-8e85-2cba7f6b7bf7"
  When I send a POST request to "/v2/user-groups/{id}/members/{userId}" to add userGroup member
    Then the response status should be 204

  Scenario: Attempt to add a member to a non-existent group
    Given user login with username "j.doe" and password "test"
    And I have groupId "9999"
    And I have memberId "78bb7f07-0464-4ff9-a0ee-82568902b7bf"
  When I send a POST request to "/v2/user-groups/{id}/members/{userId}" to add userGroup member
    Then the response status should be 404

  Scenario: Attempt to add a member to a suspended group
    Given user login with username "j.doe" and password "test"
    And I have groupId "6759f8df78b6af202b222d2a"
    And I have memberId "35a78332-d038-4bfa-8e85-2cba7f6b7bf7"
    When I send a POST request to "/v2/user-groups/{id}/members/{userId}" to add userGroup member
    Then the response status should be 400
