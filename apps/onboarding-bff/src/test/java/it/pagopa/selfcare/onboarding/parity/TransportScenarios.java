package it.pagopa.selfcare.onboarding.parity;

import static it.pagopa.selfcare.onboarding.parity.DownstreamStub.MS_DOCUMENT;
import static it.pagopa.selfcare.onboarding.parity.DownstreamStub.MS_IAM;
import static it.pagopa.selfcare.onboarding.parity.DownstreamStub.MS_ONBOARDING;
import static it.pagopa.selfcare.onboarding.parity.DownstreamStub.MS_PRODUCT;
import static it.pagopa.selfcare.onboarding.parity.DownstreamStub.PARTY_PROCESS;
import static it.pagopa.selfcare.onboarding.parity.DownstreamStub.PARTY_REGISTRY_PROXY;
import static it.pagopa.selfcare.onboarding.parity.DownstreamStub.USER_REGISTRY;

import it.pagopa.selfcare.onboarding.parity.DownstreamStub.Reply;
import java.util.ArrayList;
import java.util.List;

/**
 * Downstream transport failures (the connection is dropped without an answer).
 *
 * <p>Spring answers 500 "An error occurred during a downstream service request". The connectors
 * annotated with resilience4j {@code @Retry(retryTimeout)} (3 attempts, 5 s apart: ms-onboarding,
 * party-registry-proxy, document-ms reads, party-process institutions by tax code) keep retrying
 * and so answer after ~10 s; every other client fails at once. In this disconnect scenario the
 * Spring transport also repeats each bodyless GET/HEAD once: six wire calls for retried reads,
 * two for non-retried reads, and one for the POSTs below. Both call counts and pauses are checked.
 *
 * <p>Read timeouts are not part of the parity gate: the Spring reference does not enforce the
 * timeouts configured in its Feign properties (a 12 s answer is still served with
 * REST_CLIENT_READ_TIMEOUT=2000).
 */
final class TransportScenarios {

  private static final String G = "transport-failures";
  private static final String DETAIL = "An error occurred during a downstream service request";

  private TransportScenarios() {}

  static List<Scenario> all() {
    List<Scenario> s = new ArrayList<>();
    retried(
        s,
        Scenario.api(G, "ms-onboarding-connection-dropped-is-retried", "/v2/tokens/ob1")
            .stub(st -> st.on(MS_ONBOARDING, "GET", "/v1/onboarding/ob1/withUserInfo", Reply.abort())),
        MS_ONBOARDING);
    retried(
        s,
        Scenario.api(G, "ms-document-read-connection-dropped-is-retried", "/v2/tokens/ob1/attachment?name=a.pdf")
            .stub(st -> st.on(MS_DOCUMENT, "GET", "/v1/document-content/ob1/attachment", Reply.abort())),
        MS_DOCUMENT);
    retried(
        s,
        Scenario.api(G, "party-registry-proxy-connection-dropped-is-retried", "/v2/institutions/ipa/00000000000")
            .stub(st -> st.on(PARTY_REGISTRY_PROXY, "GET", "/institutions/ipa/00000000000", Reply.abort())),
        PARTY_REGISTRY_PROXY);
    retried(
        s,
        Scenario.api(G, "party-process-institutions-connection-dropped-is-retried", "/v1/institutions/geographic-taxonomies?taxCode=00000000000")
            .stub(st -> st.on(PARTY_PROCESS, "GET", "/institutions", Reply.abort())),
        PARTY_PROCESS);

    failsFast(
        s,
        Scenario.api(G, "ms-iam-connection-dropped-fails-fast", "/v2/tokens/ob1")
            .stub(
                st -> {
                  Fx.onboardingExists(st);
                  st.on(MS_IAM, "GET", "/iam/users/.*", Reply.abort());
                }),
        MS_IAM, 2, 3);
    failsFast(
        s,
        Scenario.api(G, "ms-product-connection-dropped-fails-fast", "/v1/product/prod-io")
            .stub(st -> st.on(MS_PRODUCT, "GET", "/product/prod-io", Reply.abort())),
        MS_PRODUCT, 2, 2);
    failsFast(
        s,
        Scenario.api(G, "party-process-external-connection-dropped-fails-fast", "/v1/institutions/inst1/geographic-taxonomy")
            .stub(st -> st.on(PARTY_PROCESS, "GET", "/external/institutions/inst1", Reply.abort())),
        PARTY_PROCESS, 2, 2);
    failsFast(
        s,
        Scenario.api(G, "user-registry-connection-dropped-fails-fast", "POST", "/v1/users/search-user")
            .json("{\"taxCode\":\"AAAAAA00A00A000A\"}")
            .stub(st -> st.on(USER_REGISTRY, "POST", "/users/search", Reply.abort())),
        USER_REGISTRY, 1, 1);
    failsFast(
        s,
        Scenario.api(G, "party-registry-proxy-infocamere-connection-dropped-fails-fast", "POST", "/v2/institutions/company/verify-manager")
            .json("{\"companyTaxCode\":\"00000000000\"}")
            .stub(st -> st.on(PARTY_REGISTRY_PROXY, "POST", "/info-camere/institutions", Reply.abort())),
        PARTY_REGISTRY_PROXY, 1, 1);
    s.add(
        Scenario.api(G, "ms-document-head-connection-dropped-fails-fast", "HEAD", "/v2/tokens/ob1/attachment/status?name=a.pdf")
            .stub(st -> st.on(MS_DOCUMENT, "HEAD", "/v1/documents/ob1/attachment/status", Reply.abort()))
            .expect(c -> c.status(500).noBody().contentType("application/problem+json")
                    .callCount(MS_DOCUMENT, 2).totalCalls(2).elapsedBelow(4_000)));
    s.add(
        Scenario.api(G, "ms-product-truncated-body-is-not-replayed", "/v1/product/prod-io")
            .stub(st -> st.on(MS_PRODUCT, "GET", "/product/prod-io", Reply.json(200, "{\"id\":").truncated()))
            .expect(c -> failure(c).callCount(MS_PRODUCT, 1).totalCalls(1).elapsedBelow(4_000)));
    s.add(
        Scenario.api(G, "bodyless-put-dropped-once-is-replayed-immediately", "PUT", "/v2/institutions/ob1")
            .stub(st -> st.on(MS_ONBOARDING, "PUT", "/v1/onboarding/ob1",
                call -> st.callsTo(MS_ONBOARDING).size() == 1 ? Reply.abort() : Reply.status(204)))
            .expect(c -> c.status(204).noBody().callCount(MS_ONBOARDING, 2).totalCalls(2)
                .propagatesIdentity().elapsedBelow(4_000)));
    retried(
        s,
        Scenario.api(G, "bodyless-put-always-dropped-exhausts-service-retries", "PUT", "/v2/institutions/ob1")
            .stub(st -> st.on(MS_ONBOARDING, "PUT", "/v1/onboarding/ob1", Reply.abort())),
        MS_ONBOARDING);
    s.add(userRegistryPatch());
    return s;
  }

  private static Scenario userRegistryPatch() {
    String userId = "44444444-4444-4444-8444-444444444444";
    String product = """
        {"productId":"prod-io","title":"IO","status":"ACTIVE","features":{"enabled":true},
         "contracts":[{"institutionType":"PA","path":"contract.pdf","version":"1"}],
         "roleMappings":[{"role":"MANAGER","backOfficeRoles":[{"code":"admin"}]}]}
        """;
    return Scenario.api(G, "user-registry-patch-keeps-the-legacy-transport-error", "POST", "/v1/institutions/onboarding")
        .json("""
            {"productId":"prod-io","institutionType":"PA","origin":"IPA","taxCode":"00000000000",
             "billingData":{"businessName":"Comune","registeredOffice":"Via Roma","digitalAddress":"pec@test.it"},
             "users":[{"name":"Mario","surname":"Rossi","taxCode":"RSSMRA80A01H501U","role":"MANAGER","email":"m@test.it"}]}
            """)
        .stub(st -> {
          st.on(MS_PRODUCT, "GET", "/product/prod-io", Reply.json(200, product));
          st.on(MS_PRODUCT, "GET", "/product/prod-io/valid", Reply.json(200, product));
          st.on(PARTY_PROCESS, "GET", "/institutions",
              Reply.json(200, "{\"institutions\":[{\"id\":\"inst1\",\"externalId\":\"ext1\"}]}"));
          st.on(USER_REGISTRY, "POST", "/users/search", Reply.json(200,
              "{\"id\":\"" + userId + "\",\"name\":{\"value\":\"Luigi\",\"certification\":\"NONE\"},"
                  + "\"familyName\":{\"value\":\"Rossi\",\"certification\":\"SPID\"}}"));
          st.on(USER_REGISTRY, "PATCH", "/users/" + userId, Reply.status(204));
          st.on(PARTY_PROCESS, "POST", "/onboarding/institution", Reply.status(201));
        })
        .expect(c -> failure(c).exactCalls(
                "ms-product GET /product/prod-io",
                "ms-product GET /product/prod-io/valid",
                "ms-product GET /product/prod-io/valid",
                "party-process GET /institutions",
                "user-registry POST /users/search")
            .propagatesIdentity().elapsedBelow(4_000));
  }

  private static void retried(List<Scenario> s, Scenario scenario, String service) {
    s.add(scenario.expect(c -> failure(c).callCount(service, 6).totalCalls(6)
            .elapsedAtLeast(9_500).elapsedBelow(14_000)));
  }

  private static void failsFast(
      List<Scenario> s, Scenario scenario, String service, int expectedCalls, int expectedTotalCalls) {
    s.add(scenario.expect(c -> failure(c).callCount(service, expectedCalls)
            .totalCalls(expectedTotalCalls).elapsedBelow(4_000)));
  }

  private static Check failure(Check c) {
    return c.problem(500, DETAIL).jsonAbsent("/invalidParams");
  }
}
