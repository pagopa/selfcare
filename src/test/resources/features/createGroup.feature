@FeatureCreate
Feature: Create User Group

  @CreateNewGroup
  Scenario: Successfully create a new user group
    Given the following user group details:
      | name        | description  | productId  | institutionId | status  | members                                                                   |
      | Group Name  | TestGroup    | product123 | inst123       | ACTIVE  | 525db33f-967f-4a82-8984-c606225e714a,a1b7c86b-d195-41d8-8291-7c3467abfd30 |
    When I send a POST request to "/v1/user-groups" with the given details, with authentication "true"
    Then [CREATE] the response status should be 201
    And the response should contain a valid user group resource with name "Group Name"
    And the response should contain the description "TestGroup"
    And the response should contain the productId "product123"
    And the response should contain the institutionId "inst123"
    And the response should contain the status "ACTIVE"
    And the response should contain 2 members
    And the response should contain the createdBy "4ba2832d-9c4c-40f3-9126-e1c72905ef14"
    And the response should contain the createdAt notNull
    And the response should contain the modified data null

  # Scenario negativo: Nome del gruppo già esistente (conflitto)
  @DuplicateGroupName
  Scenario: Attempt to create a user group with a duplicate name
    Given the following user group details:
      | name        | description  | productId   | institutionId                         | status  | members                                                                   |
      | io group    | TestGroup    | prod-test   | 9c8ae123-d990-4400-b043-67a60aff31bc  | ACTIVE  | 525db33f-967f-4a82-8984-c606225e714a,a1b7c86b-d195-41d8-8291-7c3467abfd30 |
    When I send a POST request to "/v1/user-groups" with the given details, with authentication "true"
    Then [CREATE] the response status should be 409
    And [CREATE] the response should contain an error message "A group with the same name already exists in ACTIVE or SUSPENDED state"

  # Scenario negativo: Dettagli del gruppo mancanti (name non fornito)
  Scenario: Attempt to create a user group with missing required fields
    Given the following user group details:
      | description  | productId  | institutionId | status  | members                                                                   |
      | TestGroup    | product123 | inst123       | ACTIVE  | 525db33f-967f-4a82-8984-c606225e714a,a1b7c86b-d195-41d8-8291-7c3467abfd30 |
    When I send a POST request to "/v1/user-groups" with the given details, with authentication "true"
    Then [CREATE] the response status should be 400
    And [CREATE] the response should contain an error message "createUserGroupDto.name,must not be blank"

  # Scenario negativo: Dettagli del gruppo mancanti (institutionId non fornito)
  Scenario: Attempt to create a user group with missing required fields
    Given the following user group details:
      | name        | description  | productId  | status  | members                                                                   |
      | Group Name  | TestGroup    | product123 | ACTIVE  | 525db33f-967f-4a82-8984-c606225e714a,a1b7c86b-d195-41d8-8291-7c3467abfd30 |
    When I send a POST request to "/v1/user-groups" with the given details, with authentication "true"
    Then [CREATE] the response status should be 400
    And [CREATE] the response should contain an error message "createUserGroupDto.institutionId,must not be blank"

  # Scenario negativo: Dettagli del gruppo mancanti (productId non fornito)
  Scenario: Attempt to create a user group with missing required fields
    Given the following user group details:
      | name        | description  | institutionId | status  | members                                                                   |
      | Group Name  | TestGroup    | inst123       | ACTIVE  | 525db33f-967f-4a82-8984-c606225e714a,a1b7c86b-d195-41d8-8291-7c3467abfd30 |
    When I send a POST request to "/v1/user-groups" with the given details, with authentication "true"
    Then [CREATE] the response status should be 400
    And [CREATE] the response should contain an error message "createUserGroupDto.productId,must not be blank"

  # Scenario negativo: Dettagli del gruppo mancanti (description non fornito)
  Scenario: Attempt to create a user group with missing required fields
    Given the following user group details:
      | name         | productId  | institutionId | status  | members                                                                   |
      | Group Name   | product123 | inst123       | ACTIVE  | 525db33f-967f-4a82-8984-c606225e714a,a1b7c86b-d195-41d8-8291-7c3467abfd30 |
    When I send a POST request to "/v1/user-groups" with the given details, with authentication "true"
    Then [CREATE] the response status should be 400
    And [CREATE] the response should contain an error message "createUserGroupDto.description,must not be blank"

  # Scenario negativo: Dettagli del gruppo mancanti (status non fornito)
  Scenario: Attempt to create a user group with missing required fields
    Given the following user group details:
      | name        | description  | productId  | institutionId  | members                                                                   |
      | Group Name  | TestGroup    | product123 | inst123        | 525db33f-967f-4a82-8984-c606225e714a,a1b7c86b-d195-41d8-8291-7c3467abfd30 |
    When I send a POST request to "/v1/user-groups" with the given details, with authentication "true"
    Then [CREATE] the response status should be 400
    And [CREATE] the response should contain an error message "createUserGroupDto.status,must not be null"

  # Scenario negativo: Dettagli del gruppo mancanti (members vuoto)
  Scenario: Attempt to create a user group with missing required fields
    Given the following user group details:
      | name        | description  | productId  | institutionId | status  | members |
      | Group Name  | TestGroup    | product123 | inst123       | ACTIVE  |         |
    When I send a POST request to "/v1/user-groups" with the given details, with authentication "true"
    Then [CREATE] the response status should be 400
    And [CREATE] the response should contain an error message "createUserGroupDto.members,must not be empty"

  # Scenario negativo: Autenticazione mancante (utente non autenticato)
  Scenario: Attempt to create a user group without authentication
    Given the following user group details:
      | name        | description   | productId  | institutionId | status  | members         |
      | Group Name  | Test Group    | product123 | inst123       | ACTIVE  |                 |
    When I send a POST request to "/v1/user-groups" with the given details, with authentication "false"
    Then [CREATE] the response status should be 401
