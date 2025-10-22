Feature: Health Check
  As a monitoring system
  I want to check the health of the IAM service
  So that I can ensure it is operational

  Scenario: Ping endpoint returns OK
    When I ping the IAM service
    Then I should receive a 200 OK response
    And the response body should be "OK"

  Scenario: Service is responsive
    When I send 5 consecutive ping requests
    Then all requests should return 200 OK
    And all responses should be received within 1 second