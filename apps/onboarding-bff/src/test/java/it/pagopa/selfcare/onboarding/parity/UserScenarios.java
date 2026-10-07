package it.pagopa.selfcare.onboarding.parity;

import static it.pagopa.selfcare.onboarding.parity.DownstreamStub.MS_ONBOARDING;
import static it.pagopa.selfcare.onboarding.parity.DownstreamStub.USER_REGISTRY;

import it.pagopa.selfcare.onboarding.parity.DownstreamStub.Reply;
import java.util.ArrayList;
import java.util.List;

/** /v1/users/** */
final class UserScenarios {

  private static final String G = "users";
  private static final String USER_ID = "44444444-4444-4444-8444-444444444444";
  private static final String REGISTRY_USER =
      "{\"id\":\""
          + USER_ID
          + "\",\"name\":{\"value\":\"Mario\",\"certification\":\"SPID\"},"
          + "\"familyName\":{\"value\":\"Rossi\",\"certification\":\"SPID\"}}";

  private UserScenarios() {}

  static List<Scenario> all() {
    List<Scenario> s = new ArrayList<>();
    s.add(
        Scenario.api(G, "search-user", "POST", "/v1/users/search-user")
            .json("{\"taxCode\":\"AAAAAA00A00A000A\"}")
            .stub(st -> st.on(USER_REGISTRY, "POST", "/users/search", Reply.json(200, REGISTRY_USER)))
            .expect(
                c ->
                    c.status(200)
                        .contentType("application/json")
                        .json("/id", USER_ID)
                        .exactCalls("user-registry POST /users/search")
                        .propagatesIdentity()
                        .call(USER_REGISTRY, "POST", "/users/search")
                        .queryList("fl", "fiscalCode", "familyName", "name", "workContacts")
                        .header("x-api-key", ParityTargets.USER_REGISTRY_API_KEY)
                        .jsonBody("/fiscalCode", "AAAAAA00A00A000A")));
    s.add(
        Scenario.api(G, "search-user-not-found", "POST", "/v1/users/search-user")
            .json("{\"taxCode\":\"AAAAAA00A00A000A\"}")
            .stub(st -> st.on(USER_REGISTRY, "POST", "/users/search", Reply.status(404)))
            .expect(c -> c.status(404).contentType("application/problem+json")));
    s.add(
        Scenario.api(G, "search-user-registry-failure", "POST", "/v1/users/search-user")
            .json("{\"taxCode\":\"AAAAAA00A00A000A\"}")
            .stub(st -> st.on(USER_REGISTRY, "POST", "/users/search", Reply.status(500)))
            .expect(c -> c.problem(502, null)));
    s.add(
        Scenario.api(G, "validate-matching-certified-data", "POST", "/v1/users/validate")
            .json("{\"taxCode\":\"RSSMRA80A01H501U\",\"name\":\"mario\",\"surname\":\"ROSSI\"}")
            .stub(st -> st.on(USER_REGISTRY, "POST", "/users/search", Reply.json(200, REGISTRY_USER)))
            .expect(
                c ->
                    c.status(204)
                        .noBody()
                        .call(USER_REGISTRY, "POST", "/users/search")
                        .queryList("fl", "name", "familyName")
                        .jsonBody("/fiscalCode", "RSSMRA80A01H501U")));
    s.add(
        Scenario.api(G, "validate-unknown-user-is-not-found", "POST", "/v1/users/validate")
            .json("{\"taxCode\":\"RSSMRA80A01H501U\",\"name\":\"Luigi\",\"surname\":\"Verdi\"}")
            .stub(st -> st.on(USER_REGISTRY, "POST", "/users/search", Reply.status(404)))
            .expect(c -> c.status(404).contentType("application/problem+json").exactCalls("user-registry POST /users/search")));
    s.add(
        Scenario.api(G, "validate-certified-mismatch", "POST", "/v1/users/validate")
            .json("{\"taxCode\":\"RSSMRA80A01H501U\",\"name\":\"Luigi\",\"surname\":\"Verdi\"}")
            .stub(st -> st.on(USER_REGISTRY, "POST", "/users/search", Reply.json(200, REGISTRY_USER)))
            .expect(
                c ->
                    c.problem(409, "there are values that do not match with the certified data")
                        .jsonSize("/invalidParams", 2)
                        .json("/invalidParams/0/name", "name")
                        .json("/invalidParams/0/reason", "the value does not match with the certified data")
                        .json("/invalidParams/1/name", "surname")));
    s.add(
        Scenario.api(G, "validate-requires-tax-code", "POST", "/v1/users/validate")
            .json("{\"name\":\"Luigi\"}")
            .expect(c -> c.status(400).json("/detail", "Validation failed").totalCalls(0)));
    s.add(
        Scenario.api(G, "check-manager", "POST", "/v1/users/check-manager")
            .json("{\"userId\":\"" + USER_ID + "\",\"productId\":\"prod-io\",\"taxCode\":\"00000000000\"}")
            .stub(st -> st.on(MS_ONBOARDING, "POST", "/v1/onboarding/check-manager", Reply.json(200, "{\"response\":true}")))
            .expect(
                c ->
                    c.status(200)
                        .json("/result", true)
                        .exactCalls("ms-onboarding POST /v1/onboarding/check-manager")
                        .propagatesIdentity()
                        .call(MS_ONBOARDING, "POST", "/v1/onboarding/check-manager")
                        .jsonBody("/userId", USER_ID)
                        .jsonBody("/productId", "prod-io")
                        .jsonBody("/taxCode", "00000000000")));
    s.add(
        Scenario.api(G, "check-manager-false", "POST", "/v1/users/check-manager")
            .json("{\"userId\":\"" + USER_ID + "\",\"productId\":\"prod-io\",\"taxCode\":\"00000000000\"}")
            .stub(st -> st.on(MS_ONBOARDING, "POST", "/v1/onboarding/check-manager", Reply.json(200, "{\"response\":false}")))
            .expect(c -> c.status(200).json("/result", false)));
    s.add(
        Scenario.api(G, "check-manager-rejects-non-uuid-user", "POST", "/v1/users/check-manager")
            .json("{\"userId\":\"not-a-uuid\",\"productId\":\"prod-io\"}")
            .expect(c -> c.status(400).contentType("application/problem+json").totalCalls(0)));
    String user =
        "{\"name\":\"Mario\",\"surname\":\"Rossi\",\"taxCode\":\"RSSMRA80A01H501U\",\"role\":\"MANAGER\",\"email\":\"m@t.it\"}";
    for (String[] route :
        new String[][] {
          {"/v1/users/onboarding", "/v1/onboarding/users", "plain"},
          {"/v1/users/onboarding/aggregator", "/v1/onboarding/users/aggregator", "aggregator"}
        }) {
      String path = route[0];
      s.add(
          Scenario.api(G, "onboarding-users-requires-origin-" + route[2], "POST", path)
              .json("{\"productId\":\"prod-io\",\"institutionType\":\"PA\",\"taxCode\":\"00000000000\",\"users\":[" + user + "]}")
              .expect(c -> c.status(400).contentType("application/problem+json").totalCalls(0)));
      s.add(
          Scenario.api(G, "onboarding-users-forwards-request-" + route[2], "POST", path)
              .json(
                  "{\"productId\":\"prod-io\",\"institutionType\":\"PA\",\"taxCode\":\"00000000000\","
                      + "\"origin\":\"IPA\",\"originId\":\"o1\",\"users\":["
                      + user
                      + "]}")
              .stub(st -> st.on(MS_ONBOARDING, "POST", route[1], Reply.json(200, "{\"id\":\"ob9\"}")))
              .expect(
                  c ->
                      c.status(201)
                          .exactCalls("ms-onboarding POST " + route[1])
                          .propagatesIdentity()
                          .call(MS_ONBOARDING, "POST", route[1])
                          .jsonBody("/productId", "prod-io")
                          .jsonBody("/origin", "IPA")
                          .jsonBody("/originId", "o1")
                          .jsonBody("/users/0/taxCode", "RSSMRA80A01H501U")));
    }
    String withManager =
        "{\"id\":\"ob1\",\"productId\":\"prod-io\",\"status\":\"PENDING\",\"institutionType\":\"PA\","
            + "\"institution\":{\"id\":\"inst1\",\"origin\":\"IPA\",\"originId\":\"o1\",\"description\":\"Comune\","
            + "\"institutionType\":\"PA\",\"taxCode\":\"00000000000\"},\"users\":[{\"id\":\""
            + ParityJwt.User.ADMIN.uid
            + "\",\"role\":\"MANAGER\",\"name\":\"Jane\",\"surname\":\"Admin\",\"email\":\"j@t.it\","
            + "\"taxCode\":\"AAAAAA00A00A000A\"}],\"billing\":{\"vatNumber\":\"1\"}}";
    s.add(
        Scenario.api(G, "manager-info-for-the-manager-himself", "/v1/users/onboarding/ob1/manager")
            .stub(st -> st.on(MS_ONBOARDING, "GET", "/v1/onboarding/ob1/withUserInfo", Reply.json(200, withManager)))
            .expect(
                c ->
                    c.status(200)
                        .json("/name", "Jane")
                        .json("/surname", "Admin")
                        .exactCalls("ms-onboarding GET /v1/onboarding/ob1/withUserInfo")));
    s.add(
        Scenario.api(G, "manager-info-onboarding-not-found", "/v1/users/onboarding/ob1/manager")
            .stub(st -> st.on(MS_ONBOARDING, "GET", "/v1/onboarding/ob1/withUserInfo", Reply.status(404)))
            .expect(c -> c.problem(404, "Onboarding not found")));
    return s;
  }
}
