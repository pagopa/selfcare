Feature: Webhook Management

  Background:
    Given the database is empty

  Scenario: Create a new webhook
    Given I have a webhook request with name "TestWebhook" and url "http://example.com"
    When I create the webhook
    Then the response status code should be 201
    And the response should contain "id"
    And the response should contain "name" with value "TestWebhook"

  Scenario: List webhooks
    Given I have created a webhook with name "Webhook 1"
    And I have created a webhook with name "Webhook 2"
    When I list all webhooks
    Then the response status code should be 200
    And the response list should contain 2 items

  Scenario: Get webhook by ID
    Given I have created a webhook with name "MyWebhook"
    When I get the webhook by its ID
    Then the response status code should be 200
    And the response should contain "name" with value "MyWebhook"

  Scenario: Update webhook
    Given I have created a webhook with name "Old Name"
    When I update the webhook with name "New Name"
    Then the response status code should be 200
    And the response should contain "name" with value "New Name"

  Scenario: Delete webhook
    Given I have created a webhook with name "To Delete"
    When I delete the webhook by its ID
    Then the response status code should be 204
    And I get the webhook by its ID
    Then the response status code should be 404

  Scenario: Send notification
    Given I have created a webhook for product "prod-io"
    When I send a notification for product "prod-io" with payload "{}"
    Then the response status code should be 202
    