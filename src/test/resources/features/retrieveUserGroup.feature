@FeatureRetrieve
Feature: Get User Group

  @FirstRetrieveGroupScenario
  Scenario: Successfully retrieve a group with a valid ID
    Given [RETRIEVE] user login with username "j.doe" and password "test"
    And I have a valid group ID to retrieve: "6759f8df78b6af202b222d29"
    When I send a GET request to "/v1/user-groups/{id}"
    Then [RETRIEVE] the response status should be 200
    And the response should contain the group details

  Scenario: Attempt to retrieve a non-existent group
    Given [RETRIEVE] user login with username "j.doe" and password "test"
    And I have a non-existent group ID to retrieve "99999"
    When I send a GET request to "/v1/user-groups/{id}"
    Then [RETRIEVE] the response status should be 404
    And [RETRIEVE] the response should contain an error message "Not Found"

  Scenario: Successfully retrieve user groups with valid filters
    Given [RETRIEVE] user login with username "j.doe" and password "test"
    And I have valid filters institutionId "9c8ae123-d990-4400-b043-67a60aff31bc" productId "prod-test" and status "ACTIVE"
    When I send a GET request to "/v1/user-groups" to retrieve userGroups
    Then [RETRIEVE] the response status should be 200
    And the response should contain 1 item
    And the response should contain the group details

  Scenario: Successfully retrieve user groups without any filters
    Given [RETRIEVE] user login with username "j.doe" and password "test"
    And I have no filters
    When I send a GET request to "/v1/user-groups" to retrieve userGroups
    Then [RETRIEVE] the response status should be 200
    And the response should contain 3 item
    And the response should contains groupIds "6759f8df78b6af202b222d29,6759f8df78b6af202b222d2a,6759f8df78b6af202b222d2b"

  Scenario: Attempt to retrieve user groups with a sorting parameter but no productId or institutionId
    Given [RETRIEVE] user login with username "j.doe" and password "test"
    And I have a filter with sorting by "name" but no filter
    When I send a GET request to "/v1/user-groups" to retrieve userGroups
    Then [RETRIEVE] the response status should be 400
    And [RETRIEVE] the response should contain an error message "Given sort parameters aren't valid"

  Scenario: Attempt to retrieve user groups with a sorting parameter but no productId or institutionId
    Given [RETRIEVE] user login with username "j.doe" and password "test"
    And I have a filter with sorting by "invalidSort" but no filter
    When I send a GET request to "/v1/user-groups" to retrieve userGroups
    Then [RETRIEVE] the response status should be 400
    And [RETRIEVE] the response should contain an error message "Given sort parameters aren't valid"

  Scenario: Attempt to retrieve user groups with status filter but no productId, institutionId, or userId
    Given [RETRIEVE] user login with username "j.doe" and password "test"
    And I have a filter with status "ACTIVE" but no productId, institutionId or userId
    When I send a GET request to "/v1/user-groups" to retrieve userGroups
    Then [RETRIEVE] the response status should be 400
    And [RETRIEVE] the response should contain an error message "At least one of productId, institutionId and userId must be provided with status filter"

  Scenario: Successfully retrieve a paginated list of user groups
    Given [RETRIEVE] user login with username "j.doe" and password "test"
    And I have no filters
    And I set the page number to 0 and page size to 2
    When I send a GET request to "/v1/user-groups" to retrieve userGroups
    Then [RETRIEVE] the response status should be 200
    And the response should contain a paginated list of user groups of 2 items on page 0

  Scenario: Successfully retrieve a paginated list of user groups
    Given [RETRIEVE] user login with username "j.doe" and password "test"
    And I have no filters
    And I set the page number to 1 and page size to 2
    When I send a GET request to "/v1/user-groups" to retrieve userGroups
    Then [RETRIEVE] the response status should be 200
    And the response should contain a paginated list of user groups of 1 items on page 1

  @LastRetrieveGroupScenario
  Scenario: No user groups found for the provided filters
    Given [RETRIEVE] user login with username "j.doe" and password "test"
    And I have valid filters institutionId "9c8ae123-d990-4400-b043-67a60affabcd" productId "prod-test" and status "ACTIVE"
    When I send a GET request to "/v1/user-groups" to retrieve userGroups
    Then [RETRIEVE] the response status should be 200
    And the response should contain an empty list

