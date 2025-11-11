Feature: User Permissions
  As a system
  I want to check user permissions
  So that I can authorize access to resources

  Background:
    Given the IAM service is running

  Scenario: User has required permission
    Given a user with UID "d8880750-906a-4c43-8d48-983693fe24a4" has the following permissions for product "product-A":
      | read:users  |
      | write:users |
      | admin       |
    When I check if user "d8880750-906a-4c43-8d48-983693fe24a4" has permission "read:users" for product "product-A"
    Then the permission check should return true

  Scenario: User does not have required permission
    Given a user with UID "a0530f76-3454-418c-9d65-eb3162075495" has the following permissions for product "product-A":
      | read:users |
    When I check if user "a0530f76-3454-418c-9d65-eb3162075495" has permission "delete:users" for product "product-A"
    Then the permission check should return false

  Scenario: User with empty permissions
    Given a user with UID "042b311a-7b99-4eaa-8c7d-9e5b5f6bb9ae" has no permissions for product "product-A"
    When I check if user "042b311a-7b99-4eaa-8c7d-9e5b5f6bb9ae" has permission "read:users" for product "product-A"
    Then the permission check should return false

  Scenario Outline: Check multiple permissions
    Given a user with UID "a0530f76-3454-418c-9d65-eb3162075495" has the following permissions for product "product-B":
      | read:users  |
      | write:users |
    When I check if user "a0530f76-3454-418c-9d65-eb3162075495" has permission "<permission>" for product "product-B"
    Then the permission check should return <result>

    Examples:
      | permission   | result |
      | read:users   | true   |
      | write:users  | true   |
      | delete:users | false  |
      | admin        | false  |

  Scenario Outline: Check multiple permissions with admin role
    Given a user with UID "d8880750-906a-4c43-8d48-983693fe24a4" has the following permissions for product "product-B":
      | read:users  |
      | write:users |
    When I check if user "d8880750-906a-4c43-8d48-983693fe24a4" has permission "<permission>" for product "product-B"
    Then the permission check should return <result>

    Examples:
      | permission   | result |
      | read:users   | true   |
      | write:users  | true   |
      | delete:users | true  |
      | admin        | true  |

  Scenario: Check permission with institution filter
    Given a user with UID "72d4984f-d2bc-4584-a6a7-dd63068b7f48" has permissions for product "product-A" and institution "inst-001"
    When I check if user "72d4984f-d2bc-4584-a6a7-dd63068b7f48" has permission "read:users" for product "product-A" and institution "inst-001"
    Then the permission check should return true