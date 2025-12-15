Feature: Webhook Management

  Background:
    Given the database is empty

  Scenario: Create a new webhook
    Given I have a webhook request with url "http://example.com"
    When I create the webhook
    Then the response status code should be 201
    And the response should contain "productId"
    And the response should contain "url" with value "http://example.com"

  Scenario: List webhooks
    Given I have created a webhook for product "prod-test-1"
    And I have created a webhook for product "prod-test-2"
    When I list all webhooks
    Then the response status code should be 200
    And the response list should contain 2 items

  Scenario: Get webhook by ID
    Given I have created a webhook for product "prod-test"
    When I get the webhook by its ID
    Then the response status code should be 200
    And the response should contain "url" with value "http://example.com"

  Scenario: Update webhook
    Given I have created a webhook with url "http://old-url.com" and productId "prod-update"
    When I update the webhook with url "http://old-url.com"
    Then the response status code should be 200
    And the response should contain "url" with value "http://old-url.com"

  Scenario: Send notification
    Given I have created a webhook for product "prod-io"
    When I send a notification for product "prod-io" with payload "{}"
    Then the response status code should be 202
    