@FeatureFake
Feature: Fake

Scenario: Successfully execute fake test
When I send a request to "/fake"
Then [FAKE] the response status should be 200