Feature: User Management
  As an ADMINistrator
  I want to manage users in the IAM system
  So that I can control access to products

  Background:
    Given the IAM service is running
    And the database is clean

  Scenario: Create a new user
    When I create a user with email "john.doe@example.com" and name "John Doe"
    Then the user should be created successfully
    And the user should have a unique UID
    And the user email should be "john.doe@example.com"

  Scenario: Create a user with product roles
    When I create a user with the following details:
      | email       | john.doe@example.com |
      | name        | John                 |
      | familyName  | Doe                  |
      | productId   | product-A            |
      | roles       | ADMIN,OPERATOR       |
    Then the user should be created successfully
    And the user should have 1 product role
    And the user should have role "ADMIN" for product "product-A"

  Scenario: Update existing user
    Given a user exists with email "jane.smith@example.com"
    When I update the user with new name "Jane Updated"
    Then the user should be updated successfully
    And the user name should be "Jane Updated"
    And the user UID should remain the same

  Scenario: Merge product roles for existing user
    Given a user exists with email "bob@example.com" and product "product-A" with roles "ADMIN"
    When I add product "product-B" with roles "SUPPORT" to the user
    And I request the user without product
    Then the user should have 2 product roles
    And the user should have role "ADMIN" for product "product-A"
    And the user should have role "SUPPORT" for product "product-B"

  Scenario: Replace product roles for existing user
    Given a user exists with email "alice@example.com" and product "product-A" with roles "ADMIN,OPERATOR"
    When I update product "product-A" with roles "ADMIN" for the user
    Then the user should have 1 product role
    And the user should not have role "OPERATOR" for product "product-A"

#  Scenario: Get user by UID
#    Given a user exists with UID "user-123" and email "test@example.com"
#    When I request the user with UID "user-123" for product "product-A"
#    Then the user should be retrieved successfully
#    And the response should contain the user details

  Scenario: Get user by UID - Not found
    When I request the user with UID "non-existing-uid" for product "product-A"
    Then I should receive a 404 Not Found response
    And the error message should contain "User not found"

  Scenario Outline: Create user validation
    When I try to create a user with email "<email>" and name "<name>"
    Then I should receive a <statusCode> response
    And the error message should contain "<errorMessage>"

    Examples:
      | email                 | name | statusCode | errorMessage          |
      |                       | John | 400        | User cannot be null  |
      | invalid-email         | John | 400        | Invalid email format  |
      | valid@example.com     |      | 200        |                       |

  Scenario: Filter user product roles by productId
    Given a user exists with the following product roles:
      | productId | roles          |
      | product-A | ADMIN,OPERATOR |
      | product-B | SUPPORT        |
      | product-C | SUPPORT         |
    When I request the user filtered by product "product-B"
    Then the response should contain only 1 product role
    And the product role should be for product "product-B"