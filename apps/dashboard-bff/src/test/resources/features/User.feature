@user
Feature: User
  Scenario: Successfully suspend user with 2 roles on prod-interop without productRole filter
    Given user login with username "j.doe" and password "test"
    And the userId is "4a7be530-1c36-41f4-8967-c479fb2d7fa9"
    And the productId is "prod-interop"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    When I send a POST request to "/v2/users/{userId}/suspend" to update user status
    Then the response status should be 204

  Scenario: Successfully activate user with 2 roles on prod-interop without productRole filter
    Given user login with username "j.doe" and password "test"
    And the userId is "4a7be530-1c36-41f4-8967-c479fb2d7fa9"
    And the productId is "prod-interop"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    When I send a POST request to "/v2/users/{userId}/activate" to update user status
    Then the response status should be 204

  Scenario: Successfully suspend user with 2 roles on prod-interop with productRole filter
    Given user login with username "j.doe" and password "test"
    And the userId is "4a7be530-1c36-41f4-8967-c479fb2d7fa9"
    And the productId is "prod-interop"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    And the productRoles are "api"
    When I send a POST request to "/v2/users/{userId}/suspend" to update user status
    Then the response status should be 204
    And the user product should be "SUSPENDED" only on filtered product roles

  Scenario: Successfully activate user with 2 roles on prod-interop with productRole filter
    Given user login with username "j.doe" and password "test"
    And the userId is "4a7be530-1c36-41f4-8967-c479fb2d7fa9"
    And the productId is "prod-interop"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    And the productRoles are "api"
    When I send a POST request to "/v2/users/{userId}/activate" to update user status
    Then the response status should be 204
    And the user product should be "ACTIVE" only on filtered product roles

  Scenario: Successfully suspend user on prod-io
    Given user login with username "j.doe" and password "test"
    And the userId is "4a7be530-1c36-41f4-8967-c479fb2d7fa9"
    And the productId is "prod-io"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    When I send a POST request to "/v2/users/{userId}/suspend" to update user status
    Then the response status should be 204
    And the user product should be "SUSPENDED"

  Scenario: Try to suspend user with given userId without required filter
    Given user login with username "j.doe" and password "test"
    And the userId is "4a7be530-1c36-41f4-8967-c479fb2d7fa9"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    When I send a POST request to "/v2/users/{userId}/suspend" to update user status
    Then the response status should be 400
    And the response should contain an error message "Required request parameter 'productId' for method parameter type String is not present"

  Scenario: Successfully activate user on prod-io
    Given user login with username "j.doe" and password "test"
    And the userId is "4a7be530-1c36-41f4-8967-c479fb2d7fa9"
    And the productId is "prod-io"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    When I send a POST request to "/v2/users/{userId}/activate" to update user status
    Then the response status should be 204
    And the user product should be "ACTIVE"

  Scenario: Try to activate user with given userId without required filter
    Given user login with username "j.doe" and password "test"
    And the userId is "4a7be530-1c36-41f4-8967-c479fb2d7fa9"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    When I send a POST request to "/v2/users/{userId}/activate" to update user status
    Then the response status should be 400
    And the response should contain an error message "Required request parameter 'productId' for method parameter type String is not present"

  Scenario: Successfully delete user with 2 roles on prod-interop with productRole filter
    Given user login with username "j.doe" and password "test"
    And the userId is "4a7be530-1c36-41f4-8967-c479fb2d7fa9"
    And the productId is "prod-interop"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    And the productRoles are "security"
    When I send a DELETE request to "/v2/users/{userId}" to delete user product
    Then the response status should be 204
    And the user product should not contain the mentioned roles info

  Scenario: Successfully delete user on prod-io when user is member of a group
    Given user login with username "j.doe" and password "test"
    And the userId is "4a7be530-1c36-41f4-8967-c479fb2d7fa9"
    And the productId is "prod-io"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    And I have groupId "6759f8df78b6af202b222d29"
    And I send a POST request to "/v2/user-groups/{id}/members/{userId}" to add userGroup member
    When I send a DELETE request to "/v2/users/{userId}" to delete user product
    Then the response status should be 204
    And the user product should not contain the mentioned product
    And the user is removed from user group

  Scenario: Successfully delete user on prod-pagopa when user is not member of a group
    Given user login with username "j.doe" and password "test"
    And the userId is "4a7be530-1c36-41f4-8967-c479fb2d7fa9"
    And the productId is "prod-pagopa"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    When I send a DELETE request to "/v2/users/{userId}" to delete user product
    Then the response status should be 204
    And the user product should not contain the mentioned product

  Scenario: Try to delete user with given userId without required filter
    Given user login with username "j.doe" and password "test"
    And the userId is "35a78332-d038-4bfa-8e85-2cba7f6b7bf7"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    When I send a DELETE request to "/v2/users/{userId}" to delete user product
    Then the response status should be 400
    And the response should contain an error message "Required request parameter 'productId' for method parameter type String is not present"


  Scenario: Successfully retrieve user data for given userId and institutionId without fields filter
    Given user login with username "j.doe" and password "test"
    And the userId is "97a511a7-2acc-47b9-afed-2f3c65753b4a"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    When I send a GET request to "/v2/users/{id}" to retrieve user data
    Then the response status should be 200
    And the response should contain the user data


  Scenario: Retrieving user data for given userId and institutionId without fields filter, user not found
    Given user login with username "j.doe" and password "test"
    And the userId is "35a78332-d038-4bfa-8e85-2cba7f6b7bf8"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    When I send a GET request to "/v2/users/{id}" to retrieve user data
    Then the response status should be 404


  Scenario: Retrieving user data for given userId without institutionId filter
    Given user login with username "j.doe" and password "test"
    And the userId is "97a511a7-2acc-47b9-afed-2f3c65753b4a"
    When I send a GET request to "/v2/users/{id}" to retrieve user data
    Then the response status should be 400
    And the response should contain an error message "Required request parameter 'institutionId' for method parameter type String is not present"


  Scenario: Successfully retrieve user data for given userId and institutionId with workContacts fields filter
    Given user login with username "j.doe" and password "test"
    And the userId is "97a511a7-2acc-47b9-afed-2f3c65753b4a"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    And the fields are "name,familyName"
    When I send a GET request to "/v2/users/{id}" to retrieve user data
    Then the response status should be 200
    And the response should contain only name and familyName of user


  Scenario: Successfully retrieve user data for given userId and institutionId with name, familyName, email, workContacts fields filter
    Given user login with username "j.doe" and password "test"
    And the userId is "97a511a7-2acc-47b9-afed-2f3c65753b4a"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    And the fields are "name,familyName,email,workContacts"
    When I send a GET request to "/v2/users/{id}" to retrieve user data
    Then the response status should be 200
    And the response should contain the user data with  name, familyName, email, workContacts fields


  Scenario: Successfully retrieve user data for given fiscalCode and institutionId
    Given user login with username "j.doe" and password "test"
    And the fiscalCode is "PRVTNT80A41H401T"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    When I send a POST request to "/v2/users/search" to retrieve user data from taxCode
    Then the response status should be 200
    And the response should contain the user data


  Scenario: Retrieving user data for given fiscalCode without institutionId filter
    Given user login with username "j.doe" and password "test"
    And the fiscalCode is "PRVTNT80A41H401T"
    When I send a POST request to "/v2/users/search" to retrieve user data from taxCode
    Then the response status should be 400
    And the response should contain an error message "Required request parameter 'institutionId' for method parameter type String is not present"


  Scenario: Retrieving user data for given fiscalCode with institutionId filter, not found
    Given user login with username "j.doe" and password "test"
    And the fiscalCode is "CCCCCC80C05C525C"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    When I send a POST request to "/v2/users/search" to retrieve user data from taxCode
    Then the response status should be 404

  Scenario: Successfully retrieve users for given institutionId without filters
    Given user login with username "j.doe" and password "test"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    When I send a GET request to "/v2/users/institution/{institutionId}" to retrieve user product data
    Then the response status should be 200
    And the response should contain 8 items

  Scenario: Successfully retrieve users for given institutionId with productRoles filter
    Given user login with username "j.doe" and password "test"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    And the productRoles are "admin"
    When I send a GET request to "/v2/users/institution/{institutionId}" to retrieve user product data
    Then the response status should be 200
    And the response should contain 5 items

  Scenario: Successfully retrieve users for given institutionId with roles and product filter
    Given user login with username "j.doe" and password "test"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    And the productId is "prod-io"
    And the roles are "DELEGATE"
    When I send a GET request to "/v2/users/institution/{institutionId}" to retrieve user product data
    Then the response status should be 200
    And the response should contain 1 items

  Scenario: Successfully retrieve users for given institutionId with productId filter
    Given user login with username "j.doe" and password "test"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    And the productId is "prod-io"
    When I send a GET request to "/v2/users/institution/{institutionId}" to retrieve user product data
    Then the response status should be 200
    And the response should contain 2 items

  Scenario: Successfully retrieve users for given institutionId with productId, roles and productRoles filters
    Given user login with username "j.doe" and password "test"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    And the productId is "prod-interop"
    And the roles are "OPERATOR"
    And the productRoles are "api"
    When I send a GET request to "/v2/users/institution/{institutionId}" to retrieve user product data
    Then the response status should be 200
    And the response should contain 1 items

  Scenario: Successfully retrieve users for given institutionId with productId, roles and productRoles filters, no users found
    Given user login with username "j.doe" and password "test"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    And the productId is "prod-io"
    And the roles are "OPERATOR"
    And the productRoles are "operator"
    When I send a GET request to "/v2/users/institution/{institutionId}" to retrieve user product data
    Then the response status should be 200
    And the response should contain 0 items

  Scenario: Unsuccessfully retrieve all users for given institutionId without productId filter for AR Backstage
    Given user login with username "b.king" and password "test"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    When I send a GET request to "/v2/users/all/institution/{institutionId}" to retrieve all user product data
    Then the response status should be 400

  Scenario: Successfully retrieve all users for given institutionId with productId filter for AR Backstage (users with 2 roles on same product, returning only last valid status)
    Given user login with username "b.king" and password "test"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    And the productId is "prod-interop"
    When I send a GET request to "/v2/users/all/institution/{institutionId}" to retrieve all user product data
    Then the response status should be 200
    And The response body contains the list "" of size 2
    And The response body contains at path "" the following list of objects in any order:
      | id                                   | name  | surname | fiscalCode       | email                                        | partyRole | status    |
      | 97a511a7-2acc-47b9-afed-2f3c65753b4a | john  | Doe     | PRVTNT80A41H401T | 8370aa38-a2ab-404b-9b8a-10487167332e@test.it | OPERATOR  | ACTIVE    |
      | 97a511a7-2acc-47b9-afed-2f3c65753ccc | Mario | Test    | MRATST80M15F205E | 8370aa38-a2ab-404b-9b8a-10487167332e@test.it | OPERATOR  | SUSPENDED |

  Scenario: Successfully retrieve all users for given institutionId with roles and product filter for AR Backstage
    Given user login with username "b.king" and password "test"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    And the productId is "prod-io"
    And the roles are "DELEGATE"
    When I send a GET request to "/v2/users/all/institution/{institutionId}" to retrieve all user product data
    Then the response status should be 200
    And The response body contains the list "" of size 1
    And The response body contains at path "" the following list of objects in any order:
      | id                                   | name  | surname | fiscalCode       | email                                        | partyRole | status |
      | 97a511a7-2acc-47b9-afed-2f3c65753b4a | john  | Doe     | PRVTNT80A41H401T | 8370aa38-a2ab-404b-9b8a-10487167332e@test.it | DELEGATE  | ACTIVE |

  Scenario: Successfully retrieve all users for given institutionId with productId, roles and states filters for AR Backstage
    Given user login with username "b.king" and password "test"
    And the institutionId is "2a4c94f1-5d11-41f1-89e9-9fef0de4fbfe"
    And the productId is "prod-io"
    And the roles are "OPERATOR,DELEGATE"
    And the states are "SUSPENDED"
    When I send a GET request to "/v2/users/all/institution/{institutionId}" to retrieve all user product data
    Then the response status should be 200
    And The response body contains the list "" of size 1
    And The response body contains at path "" the following list of objects in any order:
      | id                                   | name  | surname | fiscalCode       | email  | partyRole | status    |
      | 35a78332-d038-4bfa-8e85-2cba7f6b7bf7 | rocky | Balboa  | blbrki80A41H401T |        | OPERATOR  | SUSPENDED |

  Scenario: Successfully retrieve all users for given institutionId with productId, roles and productRoles filters, no users found for AR Backstage
    Given user login with username "b.king" and password "test"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    And the productId is "prod-io"
    And the roles are "OPERATOR"
    And the states are "SUSPENDED"
    When I send a GET request to "/v2/users/all/institution/{institutionId}" to retrieve all user product data
    Then the response status should be 200
    And The response body contains the list "" of size 0

  Scenario: Unsuccessfully retrieve all users for given institutionId with productId filter for AR Backstage when user has no permission
    Given user login with username "b.barnes" and password "test"
    And the productId is "prod-io"
    And the institutionId is "067327d3-bdd6-408d-8655-87e8f1960046"
    When I send a GET request to "/v2/users/all/institution/{institutionId}" to retrieve all user product data
    Then the response status should be 403

  Scenario: Successfully update user email for given userId and institutionId
    Given user login with username "j.doe" and password "test"
    And the userId is "97a511a7-2acc-47b9-afed-2f3c65753b4a"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    And the email is "cucumber@test.it"
    And the mobilePhone is "3252525251"
    When I send a PUT request to "/v2/users/{id}" to update user data
    Then the response status should be 204
    And the user email should be updated

  Scenario: Successfully update user mobilePhone for given userId and institutionId
    Given user login with username "j.doe" and password "test"
    And the userId is "97a511a7-2acc-47b9-afed-2f3c65753b4a"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    And the email is "cucumber@test.it"
    And the mobilePhone is "3252525252"
    When I send a PUT request to "/v2/users/{id}" to update user data
    Then the response status should be 204
    And the user mobilePhone should be updated

  Scenario: Successfully update user mobilePhone and email for given userId and institutionId
    Given user login with username "j.doe" and password "test"
    And the userId is "97a511a7-2acc-47b9-afed-2f3c65753b4a"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    And the email is "cucumber2@test.it"
    And the mobilePhone is "3252525253"
    When I send a PUT request to "/v2/users/{id}" to update user data
    Then the response status should be 204
    And the user mobilePhone should be updated
    And the user email should be updated

  Scenario: Updating user email for given userId without institutionId
    Given user login with username "j.doe" and password "test"
    And the userId is "97a511a7-2acc-47b9-afed-2f3c65753b4a"
    And the email is "cucumber@test.it"
    When I send a PUT request to "/v2/users/{id}" to update user data
    Then the response status should be 400
    And the response should contain an error message "Required request parameter 'institutionId' for method parameter type String is not present"

  Scenario: Updating user email for given userId with non existent institutionId
    Given user login with username "j.doe" and password "test"
    And the userId is "97a511a7-2acc-47b9-afed-2f3c65753b4a"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470555"
    And the email is "cucumber@test.it"
    When I send a PUT request to "/v2/users/{id}" to update user data
    Then the response status should be 404

  Scenario: Updating user with mobilePhone with less than 7 characters
    Given user login with username "j.doe" and password "test"
    And the userId is "97a511a7-2acc-47b9-afed-2f3c65753b4a"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    And the mobilePhone is "12345"
    When I send a PUT request to "/v2/users/{id}" to update user data
    Then the response status should be 400
    And the response should contain an error message "Il numero di telefono non è valido"

  Scenario: Updating user with mobilePhone with more than 15 characters
    Given user login with username "j.doe" and password "test"
    And the userId is "97a511a7-2acc-47b9-afed-2f3c65753b4a"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    And the mobilePhone is "12345678910111213"
    When I send a PUT request to "/v2/users/{id}" to update user data
    Then the response status should be 400
    And the response should contain an error message "Il numero di telefono non è valido"

  Scenario: Updating user with mobilePhone with alphanumeric characters
    Given user login with username "j.doe" and password "test"
    And the userId is "97a511a7-2acc-47b9-afed-2f3c65753b4a"
    And the institutionId is "c9a50656-f345-4c81-84be-5b2474470544"
    And the mobilePhone is "num123456"
    When I send a PUT request to "/v2/users/{id}" to update user data
    Then the response status should be 400
    And the response should contain an error message "Il numero di telefono non è valido"

  Scenario: Successfully get user otp info
    Given user login with username "m.polo" and password "test"
    When I send a GET request to "/v2/users/otp-info" to retrieve user otp info
    Then the response status should be 200
    And The response body contains:
      | userId                    | 9b8e2b3f-4c7d-4f91-a6e2-3d1f8a7c5b42         |
      | otpEmail                  | mail2@test.test                              |
      | otpReferenceInstitutionId | 467ac77d-7faa-47bf-a60e-38ea74bd5fd2         |
      | canUserChangeOtpEmail     | false                                        |

  Scenario: Unauthorized when i try to get user otp info with a bad token
    Given A bad jwt token
    When I send a GET request to "/v2/users/otp-info" to retrieve user otp info
    Then the response status should be 401
