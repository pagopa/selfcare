Feature: Otp

  ######################## BEGIN POST /otp/verify #########################

  Scenario: Conflict verifying otp flow, otp is expired
    And The following request body:
      """
      {
          "otpUuid": "239b58f1-9865-4ef5-b45f-b7f574a0c99e",
          "otp": "987654"
      }
      """
    When I send a POST request to "otp/verify"
    Then The status code is 409
    And The response body contains:
     | status | 409            |
     | detail | Otp is expired |

  Scenario: Not found otp flow for otp verify
    And The following request body:
      """
      {
          "otpUuid": "239b58f1-9865-4ef5-b45f-b7f574a0c771",
          "otp": "987654"
      }
      """
    When I send a POST request to "otp/verify"
    Then The status code is 404
    And The response body contains:
      | status | 404                 |
      | detail | Cannot find OtpFlow |

  Scenario: Conflict verifying otp flow, Otp is in a final state
    And The following request body:
      """
      {
          "otpUuid": "239b58f1-9865-4ef5-b45f-b7f574a0c456",
          "otp": "987654"
      }
      """
    And An OTP flow with uuid "239b58f1-9865-4ef5-b45f-b7f574a0c456" already exists with status "COMPLETED" and attempts 1
    When I send a POST request to "otp/verify"
    Then The status code is 409
    And The response body contains:
      | status | 409                     |
      | detail | Otp is in a final state |

  Scenario: Forbidden verifying otp flow, max attempts reached
    And The following request body:
      """
      {
          "otpUuid": "239b58f1-9865-4ef5-b45f-b7f574a0c456",
          "otp": "987654"
      }
      """
    And An OTP flow with uuid "239b58f1-9865-4ef5-b45f-b7f574a0c456" already exists with status "PENDING" and attempts 5
    When I send a POST request to "otp/verify"
    Then The status code is 403
    And The response body contains:
      | otpForbiddenCode  | CODE_002 |
      | remainingAttempts | 0        |
      | otpStatus         | PENDING  |

  Scenario: Forbidden verifying otp flow, max attempts reached on current attempt
    And The following request body:
      """
      {
          "otpUuid": "239b58f1-9865-4ef5-b45f-b7f574a0c456",
          "otp": "987654"
      }
      """
    And An OTP flow with uuid "239b58f1-9865-4ef5-b45f-b7f574a0c456" already exists with status "PENDING" and attempts 4
    When I send a POST request to "otp/verify"
    Then The status code is 403
    And The response body contains:
      | otpForbiddenCode  | CODE_002  |
      | remainingAttempts | 0         |
      | otpStatus         | REJECTED  |

  Scenario: Forbidden verifying otp flow, wrong otp code
    And The following request body:
      """
      {
          "otpUuid": "239b58f1-9865-4ef5-b45f-b7f574a0c456",
          "otp": "987654"
      }
      """
    And An OTP flow with uuid "239b58f1-9865-4ef5-b45f-b7f574a0c456" already exists with status "PENDING" and attempts 3
    When I send a POST request to "otp/verify"
    Then The status code is 403
    And The response body contains:
      | otpForbiddenCode  | CODE_001  |
      | remainingAttempts | 1         |
      | otpStatus         | PENDING   |

  Scenario: Successfully verifying otp flow
    Given User login with username "r.balboa" and password "test"
    And The following request body:
      """
      {
          "otpUuid": "239b58f1-9865-4ef5-b45f-b7f574a0a6f5",
          "otp": "123456"
      }
      """
    And An OTP flow with uuid "239b58f1-9865-4ef5-b45f-b7f574a0a6f5" already exists with status "PENDING" and attempts 3
    When I send a POST request to "otp/verify"
    Then The status code is 200
    And The OTP flow with uuid "239b58f1-9865-4ef5-b45f-b7f574a0a6f5" has been updated to status "COMPLETED"
    And The session token claims contains:
      | fiscal_number    | blbrki80A41H401T    |
      | name             | rocky               |
      | family_name      | Balboa              |
      | iss              | SPID                |

  Scenario: Unsuccessfully get user claims, not found user in user registry for otp verify
    Given User login with username "r.balboa" and password "test"
    And The following request body:
      """
      {
          "otpUuid": "239b58f1-9865-4ef5-b45f-b7f574a0b7v7",
          "otp": "123456"
      }
      """
    And An OTP flow with uuid "239b58f1-9865-4ef5-b45f-b7f574a0b7v7" already exists with status "PENDING" and attempts 3
    When I send a POST request to "otp/verify"
    Then The status code is 404
    And The response body contains:
      | status | 404                     |
      | detail | Not Found:Received: 'Not Found, status code 404' when invoking REST Client method: 'org.openapi.quarkus.user_registry_json.api.UserApi#findByIdUsingGET' |


  ######################## END POST /otp/verify #########################

  ######################## BEGIN POST /otp/resend #########################

  Scenario: Not found otp flow for otp resend
    And The following request body:
      """
      {
          "otpUuid": "239b58f1-9865-4ef5-b45f-b7f574a0c771"
      }
      """
    When I send a POST request to "otp/resend"
    Then The status code is 404
    And The response body contains:
      | status | 404                 |
      | detail | Cannot find OtpFlow |

  Scenario: Conflict resending otp flow, Otp is in a final state
    And The following request body:
      """
      {
          "otpUuid": "239b58f1-9865-4ef5-b45f-b7f574a0c456"
      }
      """
    And An OTP flow with uuid "239b58f1-9865-4ef5-b45f-b7f574a0c456" already exists with status "COMPLETED" and attempts 1
    When I send a POST request to "otp/resend"
    Then The status code is 409
    And The response body contains:
      | status | 409                                |
      | detail | Otp is expired or in a final state |

  Scenario: Unsuccessfully get user claims, not found user in user registry for otp resend
    And The following request body:
      """
      {
          "otpUuid": "239b58f1-9865-4ef5-b45f-b7f574a0b7v7"
      }
      """
    And An OTP flow with uuid "239b58f1-9865-4ef5-b45f-b7f574a0b7v7" already exists with status "PENDING" and attempts 3
    When I send a POST request to "otp/resend"
    Then The status code is 500
    And The response body contains:
      | status | 500                     |
      | detail | Cannot get User from PDVit.pagopa.selfcare.auth.exception.ResourceNotFoundException: Not Found:Received: 'Not Found, status code 404' when invoking REST Client method: 'org.openapi.quarkus.user_registry_json.api.UserApi#findByIdUsingGET' |


  Scenario: Unsuccessfully get user info email on External Internal API
    Given User login with username "r.balboa" and password "test"
    And The following request body:
      """
      {
          "otpUuid": "239b58f1-9865-4ef5-b45f-b7f574a0a6f5"
      }
      """
    And An OTP flow with uuid "239b58f1-9865-4ef5-b45f-b7f574a0a6f5" already exists with status "PENDING" and attempts 3
    When I send a POST request to "otp/resend"
    Then The status code is 500
    And The response body contains:
      | status | 500                     |
      | detail | Cannot get User Info Email on External Internal APIs:it.pagopa.selfcare.auth.exception.InternalException: Internal server error:Received: 'Internal Server Error, status code 500' when invoking REST Client method: 'org.openapi.quarkus.internal_json.api.UserApi#getUserOtpEmailInfo' |

  @RemoveOtpFlow
  Scenario: Successfully resend otp flow
    Given User login with username "r.balboa" and password "test"
    And The following request body:
      """
      {
          "otpUuid": "239b58f1-9865-4ef5-b45f-b7f574a0c84c"
      }
      """
    And An OTP flow with uuid "239b58f1-9865-4ef5-b45f-b7f574a0c84c" already exists with status "PENDING" and attempts 3
    When I send a POST request to "otp/resend"
    Then The status code is 200
    And The response body contains:
     | requiresOtpFlow    | true                      |
     | maskedEmail        | r*.b****a@regionelazio.it |
    And The response body contains field "otpSessionUid"
    And An OTP flow should be created with status "PENDING" and mailRequestId "f1a8d4c3-5b72-4a6d-98ef-2d2fd7d53c4e"
    And The OTP flow with uuid "239b58f1-9865-4ef5-b45f-b7f574a0c84c" has been updated to status "REJECTED"

    ######################## BEGIN GET /otp/mail-info #########################

  Scenario: Successfully get otp mail info
    Given User login with username "r.balboa" and password "test"
    When I send a GET request to "otp/mail-info/f1a8d4c3-5b72-4a6d-98ef-2d2fd7d53c4e"
    Then The status code is 200
    And The response body contains:
      | mailRequestId | 81650171-8048-4efc-8728-d3d54f1ee7ec |
      | status        | Delivered                            |
      | recipient     | test.test@test.it                    |
      | attempts      | 1                                    |
    And The response body contains the list "history" of size 3
    And The response body contains:
      | history[0].status | Delivered  |
      | history[1].status | Dispatched |
      | history[2].status | Queued     |

  Scenario: Mail info not found
    Given User login with username "r.balboa" and password "test"
    When I send a GET request to "otp/mail-info/not-found-request-id"
    Then The status code is 404
    And The response body contains:
      | status | 404                                                        |
      | detail | Mail status not found for requestId not-found-request-id   |

  Scenario: OneMail internal server error retrieving mail info
    Given User login with username "r.balboa" and password "test"
    When I send a GET request to "otp/mail-info/internal-error-request-id"
    Then The status code is 500

######################## END GET /otp/mail-info #########################

######################## BEGIN GET /otp/info #########################

  @NoOtpFlows
  Scenario: Successfully get otp info by userId
    Given The following OTP flows exist:
      | uuid                                 | userId                               | status    | attempts | mailRequestId                        |
      | 8f2c7e91-4b6a-4d8e-9f21-7a3c5e8b1d42 | 97a511a7-2acc-47b9-afed-2f3c65753b4a | PENDING   | 0        | 6e3f8a21-9c54-4b7d-a2f8-1d5e7c9b3a61 |
      | 3a91d5f7-8c42-4e6b-b1f9-5d7a2c8e4f30 | 97a511a7-2acc-47b9-afed-2f3c65753b4a | COMPLETED | 1        | b7c4e921-6d58-4a3f-8e12-9c5b7d2a4f60 |
      | c5e8a214-7b39-4d6f-a821-3f9c5e7d2b48 | 1d8e3f90-6a42-4c71-b859-2e7d4a6f8c31 | REJECTED  | 5        | 9a2f6d81-4c73-5e9b-a214-7d8c3f6a5e20 |
    And The following query params:
      | userId | 97a511a7-2acc-47b9-afed-2f3c65753b4a |
    When I send a GET request to "otp/info"
    Then The status code is 200
    And The response body contains the list "" of size 2
    And The response body contains at path "" the following list of objects in any order:
      | uuid                                 | userId                               | status    | mailRequestId                        |
      | 8f2c7e91-4b6a-4d8e-9f21-7a3c5e8b1d42 | 97a511a7-2acc-47b9-afed-2f3c65753b4a | PENDING   | 6e3f8a21-9c54-4b7d-a2f8-1d5e7c9b3a61 |
      | 3a91d5f7-8c42-4e6b-b1f9-5d7a2c8e4f30 | 97a511a7-2acc-47b9-afed-2f3c65753b4a | COMPLETED | b7c4e921-6d58-4a3f-8e12-9c5b7d2a4f60 |

  @NoOtpFlows
  Scenario: Successfully get otp info filtered by status
    Given The following OTP flows exist:
      | uuid                                 | userId                               | status    | attempts | mailRequestId                        |
      | 8f2c7e91-4b6a-4d8e-9f21-7a3c5e8b1d42 | 97a511a7-2acc-47b9-afed-2f3c65753b4a | PENDING   | 0        | 6e3f8a21-9c54-4b7d-a2f8-1d5e7c9b3a61 |
      | 3a91d5f7-8c42-4e6b-b1f9-5d7a2c8e4f30 | 97a511a7-2acc-47b9-afed-2f3c65753b4a | COMPLETED | 1        | b7c4e921-6d58-4a3f-8e12-9c5b7d2a4f60 |
    And The following query params:
      | userId | 97a511a7-2acc-47b9-afed-2f3c65753b4a |
      | status | PENDING                              |
    When I send a GET request to "otp/info"
    Then The status code is 200
    And The response body contains the list "" of size 1
    And The response body contains:
      | [0].uuid          | 8f2c7e91-4b6a-4d8e-9f21-7a3c5e8b1d42 |
      | [0].userId        | 97a511a7-2acc-47b9-afed-2f3c65753b4a |
      | [0].status        | PENDING                              |
      | [0].attempts      | 0                                    |
      | [0].mailRequestId | 6e3f8a21-9c54-4b7d-a2f8-1d5e7c9b3a61 |

  @NoOtpFlows
  Scenario: No otp info found for user
    Given User login with username "r.balboa" and password "test"
    When I send a GET request to "otp/info?userId=not-existing-user"
    Then The status code is 200
    And The response body contains the list "$" of size 0

  @NoOtpFlows
  Scenario: Bad request when userId is missing
    Given User login with username "r.balboa" and password "test"
    When I send a GET request to "otp/info"
    Then The status code is 400

######################## END GET /otp/info #########################
