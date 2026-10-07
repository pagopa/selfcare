package it.pagopa.selfcare.onboarding.parity;

import static it.pagopa.selfcare.onboarding.parity.DownstreamStub.MS_DOCUMENT;
import static it.pagopa.selfcare.onboarding.parity.DownstreamStub.MS_ONBOARDING;
import static it.pagopa.selfcare.onboarding.parity.DownstreamStub.MS_PRODUCT;
import static it.pagopa.selfcare.onboarding.parity.DownstreamStub.PARTY_REGISTRY_PROXY;
import static it.pagopa.selfcare.onboarding.parity.DownstreamStub.USER_REGISTRY;

import it.pagopa.selfcare.onboarding.parity.DownstreamStub.Reply;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/** Downstream failures: every status family, through clients of different microservices. */
final class ErrorScenarios {

  private static final String G = "downstream-errors";
  private static final int[] STATUSES = {400, 401, 403, 404, 409, 422, 429, 500, 502, 503};

  private ErrorScenarios() {}

  /** One BFF endpoint plus how to make its decisive downstream call answer {@code status}. */
  private record Target(
      String name, String path, String method, String requestBody, BiConsumer<DownstreamStub, Reply> stub) {

    Target(String name, String path, String method, BiConsumer<DownstreamStub, Reply> stub) {
      this(name, path, method, null, stub);
    }
  }

  private static final String TAX_CODE_BODY = "{\"taxCode\":\"RSSMRA80A01H501U\"}";
  private static final String VALIDATE_BODY =
      "{\"taxCode\":\"RSSMRA80A01H501U\",\"name\":\"Mario\",\"surname\":\"Rossi\"}";

  private static String body(int status) {
    return "{\"title\":\"T\",\"status\":" + status + ",\"detail\":\"downstream detail " + status + "\"}";
  }

  static List<Scenario> all() {
    List<Target> targets =
        List.of(
            new Target(
                "product-ms",
                "/v1/product/p1",
                "GET",
                (st, r) -> st.on(MS_PRODUCT, "GET", "/product/p1", r)),
            new Target(
                "document-ms",
                "/v2/tokens/ob1/attachment?name=a.pdf",
                "GET",
                (st, r) -> st.on(MS_DOCUMENT, "GET", "/v1/document-content/ob1/attachment", r)),
            new Target(
                "onboarding-ms",
                "/v2/tokens/ob1/verify",
                "POST",
                (st, r) -> st.on(MS_ONBOARDING, "GET", "/v1/onboarding/ob1", r)),
            new Target(
                "party-registry-proxy",
                "/v2/institutions/ipa/00000000000",
                "GET",
                (st, r) -> st.on(PARTY_REGISTRY_PROXY, "GET", "/institutions/ipa/00000000000", r)),
            new Target(
                "user-registry-search",
                "/v1/users/search-user",
                "POST",
                TAX_CODE_BODY,
                (st, r) -> st.on(USER_REGISTRY, "POST", "/users/search", r)),
            new Target(
                "user-registry-validate",
                "/v1/users/validate",
                "POST",
                VALIDATE_BODY,
                (st, r) -> st.on(USER_REGISTRY, "POST", "/users/search", r)));
    List<Scenario> s = new ArrayList<>();
    for (Target target : targets) {
      for (int status : STATUSES) {
        Scenario scenario =
            Scenario.api(G, target.name() + " answers " + status, target.method(), target.path())
                .stub(st -> target.stub().accept(st, Reply.json(status, body(status))));
        if (target.requestBody() != null) {
          scenario.json(target.requestBody());
        }
        scenario.expect(
            c -> {
              switch (status) {
                case 400, 409 -> c.problem(status, body(status));
                case 404 ->
                    c.problem(
                        404,
                        target.name().equals("product-ms")
                            ? "No product found with id p1"
                            : body(404));
                case 401, 403, 422, 429 ->
                    c.problem(status, "An error occurred during a downstream service request");
                default -> c.problem(502, null).jsonAbsent("/detail").json("/title", "Bad Gateway");
              }
              c.json("/instance", target.path().replaceAll("\\?.*", ""));
            });
        s.add(scenario);
      }
    }
    return s;
  }
}
