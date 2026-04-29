@FeatureDelegationV2
Feature: DelegationV2

  Scenario: Successfully get delegations with institutionId,brokerId,productId,search
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | institutionId | bf4dcdb6-f223-4996-bfbc-326b119dd101 |
      | brokerId      | c18d0cd5-e8a5-4f40-894b-f1f4252e1294 |
      | productId     | prod-io                              |
      | search        | Comune di Assisi                     |
    When I send a GET request to "/v2/delegations"
    Then The status code is 200
    And The response body contains the list "delegations" of size 2
    And The response body contains at path "delegations" the following list of objects in any order:
      | id                                   | institutionId                        | institutionName  | institutionRootName | type | productId | taxCode     | institutionType | brokerId                             | brokerTaxCode | brokerType | brokerName           | status |
      | 88d04946-1b34-482c-8218-fda002b163cf | bf4dcdb6-f223-4996-bfbc-326b119dd101 | Comune di Assisi |                     | EA   | prod-io   | 00313820540 | PA              | c18d0cd5-e8a5-4f40-894b-f1f4252e1294 | 12399999890   | PT         | NTT Data             | ACTIVE |
      | 65a4c351-339b-44a2-bb74-cc650ba2413a | bf4dcdb6-f223-4996-bfbc-326b119dd101 | Comune di Assisi |                     | PT   | prod-io   | 00313820540 | PA              | c18d0cd5-e8a5-4f40-894b-f1f4252e1294 | 12399999890   | PT         | test pt senza geotax | ACTIVE |
    And The response body contains:
      | pageInfo.totalElements | 2     |
      | pageInfo.totalPages    | 1     |
      | pageInfo.pageNo        | 0     |
      | pageInfo.pageSize      | 10000 |

  Scenario: Successfully get delegations with institutionId,order and pagination
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | institutionId | bf4dcdb6-f223-4996-bfbc-326b119dd101 |
      | order         | DESC                                 |
      | page          | 1                                    |
      | size          | 10                                   |
    When I send a GET request to "/v2/delegations"
    Then The status code is 200
    And The response body contains the list "delegations" of size 1
    And The response body contains at path "delegations" the following list of objects in any order:
      | institutionId                        | institutionName  | status |
      | bf4dcdb6-f223-4996-bfbc-326b119dd101 | Comune di Assisi | ACTIVE |
    And The response body contains:
        | pageInfo.totalElements | 11    |
        | pageInfo.totalPages    | 2     |
        | pageInfo.pageNo        | 1     |
        | pageInfo.pageSize      | 10    |

  Scenario: Successfully get delegations with institutionId and taxCode as a fiscal code
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | institutionId | c7a9a8e2-36e3-4ad5-9e63-6d482b74d1d7 |
      | taxCode       | BLBRKY67C30H501B                     |
    When I send a GET request to "/v2/delegations"
    Then The status code is 200
    And The response body contains the list "delegations" of size 1
    And The response body contains at path "delegations" the following list of objects in any order:
      | id                                   | institutionId                        | institutionName | type | productId | taxCode          | institutionType | brokerId                             | brokerTaxCode    | brokerType | brokerName           | status |
      | 204c18ce-310f-454e-ae0e-7e87c019ba5c | c7a9a8e2-36e3-4ad5-9e63-6d482b74d1d7 | Privato CF 1    | PT   | prod-io   | PRVTNT80A41H401T | PRV_PF          | 0c6d2a5e-9c42-4b2f-9c3f-94c5cb2b6d1a | blbrki80A41H401T | PRV_PF     | Privato CF 2         | ACTIVE |
    And The response body contains:
      | pageInfo.totalElements | 1     |
      | pageInfo.totalPages    | 1     |
      | pageInfo.pageNo        | 0     |
      | pageInfo.pageSize      | 10000 |

  Scenario: Not found delegations
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | institutionId | 123 |
    When I send a GET request to "/v2/delegations"
    Then The status code is 200
    And The response body contains the list "delegations" of size 0
    And The response body contains:
      | pageInfo.totalElements | 0     |
      | pageInfo.totalPages    | 0     |
      | pageInfo.pageNo        | 0     |
      | pageInfo.pageSize      | 10000 |

  Scenario: Invalid request while getting delegations without institutionId and brokerId
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "/v2/delegations"
    Then The status code is 400
    And The response body contains:
      | status | 400                                          |
      | detail | institutionId or brokerId must not be null!! |
