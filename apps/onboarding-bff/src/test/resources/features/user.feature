Feature: User

  Scenario: Success to validate user from PDV
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
      {
        "taxCode": "VRDMRA22T71F205A",
        "name":"Tizio",
        "surname":"Caio"
      }
    """
    When I send a POST request to "/v1/users/validate"
    Then The status code is 204

  Scenario: Failed to validate user from PDV error name
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
      {
        "taxCode": "VRDMRA22T71F205A",
        "name":"Tizioz",
        "surname":"Caio"
      }
    """
    When I send a POST request to "/v1/users/validate"
    Then The status code is 409


  Scenario: Failed to validate user from PDV error surname
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
      {
        "taxCode": "VRDMRA22T71F205A",
        "name":"Tizio",
        "surname":"Caioz"
      }
    """
    When I send a POST request to "/v1/users/validate"
    Then The status code is 409

  Scenario: Failed to validate user from PDV when not exist
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
      {
        "taxCode": "VRDMRA22T71F205X",
        "name":"Tizio",
        "surname":"Caio"
      }
    """
    When I send a POST request to "/v1/users/validate"
    Then The status code is 404


  Scenario: Success to search user from PDV
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
      {
        "taxCode": "VRDMRA22T71F205A"
      }
    """
    When I send a POST request to "/v1/users/search-user"
    Then The status code is 200

  Scenario: Failed to search user from PDV
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
      {
        "taxCode": "VRDMRA22T71F205X"
      }
    """
    When I send a POST request to "/v1/users/search-user"
    Then The status code is 404

  #Scenario: Success to onboarding users
  #  Given User login with username "j.doe" and password "test"
  #  And The following request body:
  #  """
  #   {
  #    "users":[
  #      {
  #         "name":"Tizio",
  #         "role":"MANAGER",
  #         "surname":"Caio",
  #         "taxCode":"VRDMRA22T71F205A",
  #         "email":"prova@test.it"
  #      }
  #    ],
  #    "productId":"prod-interop",
  #    "institutionType":"PA",
  #    "origin":"SELC",
  #    "originId":"11223345661",
  #    "taxCode":"11223345661"
  #   }
  #  """
  #  When I send a POST request to "/v1/users/onboarding"
  #  Then The status code is 200

  #Scenario: Failed to onboarding users
  #  Given User login with username "j.doe" and password "test"
  #  And The following request body:
  #  """
  #    {
  #    "users":[
  #    {
  #       "name":"Tizio",
  #       "role":"MANAGER",
  #       "surname":"Caio",
  #       "taxCode":"VRDMRA22T71F205A",
  #       "email":"prova@test.it"
  #    }
  #    ],
  #    "productId":"prod-interop",
  #     "institutionType":"PA",
  #     "origin":"SELC",
  #     "originId":"00145190",
  #     "taxCode":"16341672901"
  #    }
  #  """
  #  When I send a POST request to "/v1/users/onboarding"
  #  Then The status code is 404


  #Scenario: Success to onboarding aggregator
  #  Given User login with username "j.doe" and password "test"
  #  And The following request body:
  #  """
  #    {
  #    "users":[
  #    {
  #       "name":"Tizio",
  #       "role":"MANAGER",
  #       "surname":"Caio",
  #       "taxCode":"VRDMRA22T71F205A",
  #       "email":"prova@test.it"
  #    }
  #    ],
  #    "productId":"prod-interop",
  #     "institutionType":"PA",
  #     "origin":"SELC",
  #     "originId":"00145190922",
  #     "taxCode":"00145190922"
  #    }
  #  """
  #  When I send a POST request to "/v1/users/onboarding/aggregator"
  #  Then The status code is 200


  Scenario: Success to check manager
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
      {
      "userId":"ec7ca4d5-c537-462a-8c06-e102e0c21c44",
      "productId":"prod-interop",
       "institutionType":"PA",
       "origin":"SELC",
       "originId":"00145190922",
       "taxCode":"00145190922"
      }
    """
    When I send a POST request to "/v1/users/check-manager"
    Then The status code is 200


  Scenario: Failed to check manager
    Given User login with username "j.doe" and password "test"
    And The following request body:
    """
      {
      "userId":"ec7ca4d5-c537-462a-8c06-e102e0c21c40",
      "productId":"prod-interop",
       "institutionType":"PA",
       "origin":"SELC",
       "originId":"00145190922",
       "taxCode":"00145190922"
      }
    """
    When I send a POST request to "/v1/users/check-manager"
    Then The status code is 200

  Scenario: Success to retrive manager infos
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "/v1/users/onboarding/37f7609b-5a4b-4200-82e7-2117756d64aa/manager"
    Then The status code is 200

  Scenario: Failed to retrive manager infos when onboardingId not exist
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "/v1/users/onboarding/ec7ca4d5-c537-462a-8c06-e102e0c21c40/manager"
    Then The status code is 404