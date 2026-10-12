package it.pagopa.selfcare.onboarding.parity;

import static it.pagopa.selfcare.onboarding.parity.DownstreamStub.MS_ONBOARDING;
import static it.pagopa.selfcare.onboarding.parity.DownstreamStub.MS_PRODUCT;
import static it.pagopa.selfcare.onboarding.parity.DownstreamStub.PARTY_PROCESS;
import static it.pagopa.selfcare.onboarding.parity.DownstreamStub.PARTY_REGISTRY_PROXY;

import it.pagopa.selfcare.onboarding.parity.DownstreamStub.Reply;
import java.util.ArrayList;
import java.util.List;

/** /v1/institutions/** and /v2/institutions/** (IPA search lives with the product scenarios). */
final class InstitutionScenarios {

  private static final String G = "institutions";
  private static final String TAX = "00000000000";
  private static final String UUID_INSTITUTION = "55555555-5555-4555-8555-555555555555";
  private static final String MANAGER =
      "{\"name\":\"Mario\",\"surname\":\"Rossi\",\"taxCode\":\"RSSMRA80A01H501U\",\"role\":\"MANAGER\",\"email\":\"m@t.it\"}";

  private InstitutionScenarios() {}

  static List<Scenario> all() {
    List<Scenario> s = new ArrayList<>();
    v2Queries(s);
    v2Onboarding(s);
    v1Queries(s);
    return s;
  }

  private static void productValid(DownstreamStub st) {
    st.on(MS_PRODUCT, "GET", "/product/prod-io/valid", Reply.json(200, Fx.product("prod-io", "ACTIVE", true)));
  }

  private static String onboardingGet(String status) {
    return "{\"id\":\"ob1\",\"productId\":\"prod-io\",\"status\":\""
        + status
        + "\",\"institutionType\":\"PA\",\"origin\":\"IPA\",\"originId\":\"o1\","
        + "\"institution\":{\"id\":\""
        + UUID_INSTITUTION
        + "\",\"origin\":\"IPA\",\"originId\":\"o1\",\"description\":\"Comune\",\"institutionType\":\"PA\","
        + "\"taxCode\":\""
        + TAX
        + "\",\"digitalAddress\":\"pec@t.it\",\"city\":\"Roma\",\"county\":\"RM\",\"country\":\"IT\"},"
        + "\"users\":[],\"billing\":{\"vatNumber\":\"1\",\"recipientCode\":\"RC1\"}}";
  }

  private static void v2Queries(List<Scenario> s) {
    s.add(
        Scenario.api(G, "v2-by-filters", "/v2/institutions?productId=prod-io&taxCode=" + TAX + "&origin=IPA&originId=o1&subunitCode=U1")
            .stub(
                st ->
                    st.on(
                        MS_ONBOARDING,
                        "GET",
                        "/v1/onboarding/institutionOnboardings",
                        Reply.json(200, "[" + onboardingGet("COMPLETED") + "]")))
            .expect(
                c ->
                    c.status(200)
                        .contentType("application/json")
                        .jsonSize("", 1)
                        .json("/0/id", UUID_INSTITUTION)
                        .json("/0/description", "Comune")
                        .json("/0/originId", "o1")
                        .json("/0/origin", "IPA")
                        .json("/0/institutionType", "PA")
                        .json("/0/taxCode", TAX)
                        .json("/0/digitalAddress", "pec@t.it")
                        .exactCalls("ms-onboarding GET /v1/onboarding/institutionOnboardings")
                        .propagatesIdentity()
                        .call(MS_ONBOARDING, "GET", "/v1/onboarding/institutionOnboardings")
                        .queryAbsent("productId")
                        .query("taxCode", TAX)
                        .query("origin", "IPA")
                        .query("originId", "o1")
                        .query("subunitCode", "U1")
                        .query("status", "COMPLETED")));
    s.add(
        Scenario.api(G, "v2-by-filters-empty-is-not-found", "/v2/institutions?productId=prod-io&taxCode=" + TAX)
            .stub(st -> st.on(MS_ONBOARDING, "GET", "/v1/onboarding/institutionOnboardings", Reply.json(200, "[]")))
            .expect(c -> c.status(404).contentType("application/problem+json")));
    s.add(
        Scenario.api(G, "v2-onboardings-by-status", "/v2/institutions/onboardings?taxCode=" + TAX + "&status=COMPLETED")
            .stub(
                st ->
                    st.on(
                        MS_ONBOARDING,
                        "GET",
                        "/v1/onboarding",
                        Reply.json(200, "{\"items\":[" + onboardingGet("COMPLETED") + "],\"total\":1}")))
            .expect(
                c ->
                    c.status(200)
                        .contentType("application/json")
                        .exactCalls("ms-onboarding GET /v1/onboarding")
                        .call(MS_ONBOARDING, "GET", "/v1/onboarding")
                        .query("taxCode", TAX)
                        .query("status", "COMPLETED")));
    s.add(
        Scenario.api(G, "v2-recipient-code-verification", "/v2/institutions/onboarding/recipient-code/verification?originId=o1&recipientCode=RC1")
            .stub(
                st ->
                    st.on(
                        MS_ONBOARDING,
                        "GET",
                        "/v1/onboarding/checkRecipientCode",
                        Reply.json(200, "\"ACCEPTED\"")))
            .expect(
                c ->
                    c.status(200)
                        .contentType("application/json")
                        .bodyContains("ACCEPTED")
                        .exactCalls("ms-onboarding GET /v1/onboarding/checkRecipientCode")
                        .call(MS_ONBOARDING, "GET", "/v1/onboarding/checkRecipientCode")
                        .query("originId", "o1")
                        .query("recipientCode", "RC1")));
    String institutions =
        "{\"institutions\":[{\"id\":\"inst1\",\"externalId\":\""
            + TAX
            + "\",\"originId\":\"o1\",\"origin\":\"IPA\",\"description\":\"Comune\",\"institutionType\":\"PA\","
            + "\"taxCode\":\""
            + TAX
            + "\",\"digitalAddress\":\"pec@t.it\",\"onboarding\":[{\"productId\":\"prod-io\",\"status\":\"ACTIVE\"}]}]}";
    s.add(
        Scenario.api(G, "v2-active-onboarding", "/v2/institutions/onboarding/active?taxCode=" + TAX + "&productId=prod-io")
            .stub(st -> st.on(PARTY_PROCESS, "GET", "/institutions", Reply.json(200, institutions)))
            .expect(
                c ->
                    c.status(200)
                        .jsonSize("", 1)
                        .json("/0/institutionId", "inst1")
                        .json("/0/businessName", "Comune")
                        .json("/0/onboardings/0/productId", "prod-io")
                        .json("/0/onboardings/0/status", "ACTIVE")
                        .exactCalls("party-process GET /institutions")
                        .call(PARTY_PROCESS, "GET", "/institutions")
                        .query("taxCode", TAX)));
    s.add(
        Scenario.api(G, "v2-active-onboarding-other-product-is-not-found", "/v2/institutions/onboarding/active?taxCode=" + TAX + "&productId=prod-other")
            .stub(st -> st.on(PARTY_PROCESS, "GET", "/institutions", Reply.json(200, institutions)))
            .expect(c -> c.problem(404, "Institution doesn't have active onboarding for the given product")));
    s.add(
        Scenario.api(G, "v2-active-onboarding-blank-is-bad-request", "/v2/institutions/onboarding/active?taxCode=&productId=prod-io")
            .expect(c -> c.status(400).contentType("application/problem+json").totalCalls(0)));
    s.add(
        Scenario.api(G, "v2-trigger-onboarding-request", "PUT", "/v2/institutions/ob1")
            .stub(st -> st.on(MS_ONBOARDING, "PUT", "/v1/onboarding/ob1", Reply.status(204)))
            .expect(c -> c.status(204).noBody().exactCalls("ms-onboarding PUT /v1/onboarding/ob1").propagatesIdentity()));
    s.add(
        Scenario.api(G, "v2-trigger-onboarding-request-not-found", "PUT", "/v2/institutions/ob1")
            .stub(st -> st.on(MS_ONBOARDING, "PUT", "/v1/onboarding/ob1", Reply.status(404)))
            .expect(c -> c.status(404).contentType("application/problem+json")));
    String proxy = "{\"businesses\":[{\"businessTaxId\":\"" + TAX + "\",\"businessName\":\"Acme\"}],\"legalTaxId\":\"AAAAAA00A00A000A\"}";
    s.add(
        Scenario.api(G, "v2-verify-manager-of-company", "POST", "/v2/institutions/company/verify-manager")
            .json("{\"companyTaxCode\":\"" + TAX + "\"}")
            .stub(st -> st.on(PARTY_REGISTRY_PROXY, "POST", "/info-camere/institutions", Reply.json(200, proxy)))
            .expect(
                c ->
                    c.status(200)
                        .json("/origin", "INFOCAMERE")
                        .json("/companyName", "Acme")
                        .exactCalls("party-registry-proxy POST /info-camere/institutions")
                        .call(PARTY_REGISTRY_PROXY, "POST", "/info-camere/institutions")
                        .jsonBody("/filter/legalTaxId", ParityJwt.User.ADMIN.fiscalNumber)));
    String notManager =
        "User with userTaxCode " + ParityJwt.User.ADMIN.fiscalNumber + " is not the legal representative of the institution";
    s.add(
        Scenario.api(G, "v2-verify-manager-falls-back-to-ade", "POST", "/v2/institutions/company/verify-manager")
            .json("{\"companyTaxCode\":\"99999999999\"}")
            .stub(
                st -> {
                  st.on(PARTY_REGISTRY_PROXY, "POST", "/info-camere/institutions", Reply.json(200, proxy));
                  st.on(
                      PARTY_REGISTRY_PROXY,
                      "GET",
                      "/national-registries/verify-legal",
                      Reply.json(200, "{\"verificationResult\":true}"));
                })
            .expect(
                c ->
                    c.status(200)
                        .json("/origin", "ADE")
                        .jsonAbsent("/companyName")
                        .exactCalls(
                            "party-registry-proxy POST /info-camere/institutions",
                            "party-registry-proxy GET /national-registries/verify-legal")
                        .call(PARTY_REGISTRY_PROXY, "GET", "/national-registries/verify-legal")
                        .query("taxId", ParityJwt.User.ADMIN.fiscalNumber)
                        .query("vatNumber", "99999999999")));
    s.add(
        Scenario.api(G, "v2-verify-manager-not-legal-representative", "POST", "/v2/institutions/company/verify-manager")
            .json("{\"companyTaxCode\":\"99999999999\"}")
            .stub(
                st -> {
                  st.on(PARTY_REGISTRY_PROXY, "POST", "/info-camere/institutions", Reply.json(200, proxy));
                  st.on(
                      PARTY_REGISTRY_PROXY,
                      "GET",
                      "/national-registries/verify-legal",
                      Reply.json(200, "{\"verificationResult\":false}"));
                })
            .expect(
                c ->
                    c.problem(404, notManager)
                        .exactCalls(
                            "party-registry-proxy POST /info-camere/institutions",
                            "party-registry-proxy GET /national-registries/verify-legal")));
    s.add(
        Scenario.api(G, "v2-verify-manager-ade-rejection-is-not-legal-representative", "POST", "/v2/institutions/company/verify-manager")
            .json("{\"companyTaxCode\":\"99999999999\"}")
            .stub(
                st -> {
                  st.on(PARTY_REGISTRY_PROXY, "POST", "/info-camere/institutions", Reply.json(200, proxy));
                  st.on(PARTY_REGISTRY_PROXY, "GET", "/national-registries/verify-legal", Reply.status(400));
                })
            .expect(c -> c.problem(404, notManager)));
    s.add(
        Scenario.api(G, "v2-verify-manager-infocamere-failure-is-bad-gateway", "POST", "/v2/institutions/company/verify-manager")
            .json("{\"companyTaxCode\":\"" + TAX + "\"}")
            .stub(st -> st.on(PARTY_REGISTRY_PROXY, "POST", "/info-camere/institutions", Reply.status(503)))
            .expect(c -> c.problem(502, null).exactCalls("party-registry-proxy POST /info-camere/institutions")));
    s.add(
        Scenario.api(G, "v2-verify-manager-requires-company-tax-code", "POST", "/v2/institutions/company/verify-manager")
            .json("{}")
            .expect(
                c ->
                    c.status(400)
                        .json("/detail", "Validation failed")
                        .json("/invalidParams/0/name", "verifyManagerRequest.companyTaxCode")
                        .totalCalls(0)));
    byte[] csv = "taxCode\n00000000000\n".getBytes();
    s.add(
        Scenario.api(G, "v2-aggregation-verification-csv", "POST", "/v2/institutions/onboarding/aggregation/verification?productId=prod-io")
            .multipart(Multipart.body().file("aggregates", "agg.csv", "text/csv", csv))
            .stub(
                st ->
                    st.on(
                        MS_ONBOARDING,
                        "POST",
                        "/v1/aggregates/verification/prod-io",
                        Reply.json(200, "{\"aggregates\":[],\"errors\":[]}")))
            .expect(
                c ->
                    c.status(200)
                        .jsonSize("/aggregates", 0)
                        .jsonSize("/errors", 0)
                        .exactCalls("ms-onboarding POST /v1/aggregates/verification/prod-io")
                        .call(MS_ONBOARDING, "POST", "/v1/aggregates/verification/prod-io")
                        .multipartParts("aggregates")
                        .multipartPart("aggregates", "agg.csv", "text/csv", csv)));
    for (String name : new String[] {"agg.xls", "agg.xlsx"}) {
      s.add(
          Scenario.api(G, "v2-aggregation-verification-accepts-" + name, "POST", "/v2/institutions/onboarding/aggregation/verification?productId=prod-io")
              .multipart(Multipart.body().file("aggregates", name, "application/octet-stream", csv))
              .stub(
                  st ->
                      st.on(
                          MS_ONBOARDING,
                          "POST",
                          "/v1/aggregates/verification/prod-io",
                          Reply.json(200, "{\"aggregates\":[],\"errors\":[]}")))
              .expect(c -> c.status(200).callCount(MS_ONBOARDING, 1)));
    }
    s.add(
        Scenario.api(G, "v2-aggregation-verification-rejects-pdf", "POST", "/v2/institutions/onboarding/aggregation/verification?productId=prod-io")
            .multipart(Multipart.body().file("aggregates", "agg.pdf", "application/pdf", csv))
            .expect(c -> c.problem(400, "Formato file non supportato. Ammessi: [.csv, .xls, .xlsx]").totalCalls(0)));
    s.add(
        Scenario.api(G, "v2-aggregation-verification-requires-product", "POST", "/v2/institutions/onboarding/aggregation/verification")
            .multipart(Multipart.body().file("aggregates", "agg.csv", "text/csv", csv))
            .expect(c -> c.status(400).contentType("application/problem+json").totalCalls(0)));
    s.add(
        Scenario.api(G, "v2-aggregation-verification-requires-file", "POST", "/v2/institutions/onboarding/aggregation/verification?productId=prod-io")
            .multipart(Multipart.body().field("other", "x"))
            .expect(c -> c.status(400).contentType("application/problem+json").totalCalls(0)));
  }

  private static void v2Onboarding(List<Scenario> s) {
    for (boolean certified : new boolean[]{true, false}) {
      String origin = certified ? "infocamere" : "ade";
      String registryMethod = certified ? "POST" : "GET";
      String registryPath = certified ? "/info-camere/institutions" : "/national-registries/verify-legal";
      String owned = certified
          ? "{\"businesses\":[{\"businessTaxId\":\"" + TAX + "\",\"businessName\":\"Acme\"}]}"
          : "{\"verificationResult\":true}";
      String notOwned = certified ? "{\"businesses\":[]}" : "{\"verificationResult\":false}";
      s.add(
          Scenario.api(G, "v2-company-onboarding-" + origin + "-verifies-before-completion", "POST", "/v2/institutions/company/onboarding")
              .json(companyOnboardingRequest(certified))
              .stub(st -> {
                st.on(PARTY_REGISTRY_PROXY, registryMethod, registryPath, Reply.json(200, owned));
                st.on(MS_ONBOARDING, "POST", "/v1/onboarding/pg/completion", Reply.json(200, "{\"id\":\"ob9\"}"));
              })
              .expect(c -> c.status(201).noBody().exactCalls(
                      "party-registry-proxy " + registryMethod + " " + registryPath,
                      "ms-onboarding POST /v1/onboarding/pg/completion")
                  .propagatesIdentity()
                  .call(MS_ONBOARDING, "POST", "/v1/onboarding/pg/completion")
                  .jsonBody("/productId", "prod-io").jsonBody("/institutionType", "PG")
                  .jsonBody("/taxCode", TAX).jsonBody("/businessName", "Acme")
                  .jsonBody("/origin", certified ? "INFOCAMERE" : "ADE")
                  .jsonBody("/users/0/role", "MANAGER")));
      s.add(
          Scenario.api(G, "v2-company-onboarding-" + origin + "-not-owned-never-completes", "POST", "/v2/institutions/company/onboarding")
              .json(companyOnboardingRequest(certified))
              .stub(st -> st.on(PARTY_REGISTRY_PROXY, registryMethod, registryPath, Reply.json(200, notOwned)))
              .expect(c -> c.problem(403, "The selected business does not belong to the user")
                  .exactCalls("party-registry-proxy " + registryMethod + " " + registryPath)
                  .propagatesIdentity()));
    }
    s.add(
        Scenario.api(G, "v2-onboarding-users-pg", "POST", "/v2/institutions/onboarding/users/pg")
            .json("{\"productId\":\"prod-io\",\"taxCode\":\"" + TAX + "\",\"certified\":true,\"users\":[" + MANAGER + "]}")
            .stub(st -> st.on(MS_ONBOARDING, "POST", "/v1/onboarding/users/pg", Reply.json(200, "{\"id\":\"ob9\"}")))
            .expect(
                c ->
                    c.status(200)
                        .exactCalls("ms-onboarding POST /v1/onboarding/users/pg")
                        .propagatesIdentity()
                        .call(MS_ONBOARDING, "POST", "/v1/onboarding/users/pg")
                        .jsonBody("/productId", "prod-io")
                        .jsonBody("/institutionType", "PG")
                        .jsonBody("/origin", "INFOCAMERE")
                        .jsonBody("/taxCode", TAX)
                        .jsonBody("/users/0/taxCode", "RSSMRA80A01H501U")
                        .jsonBody("/users/0/role", "MANAGER")));
    s.add(
        Scenario.api(G, "v2-onboarding-users-pg-requires-users", "POST", "/v2/institutions/onboarding/users/pg")
            .json("{\"productId\":\"prod-io\",\"taxCode\":\"" + TAX + "\",\"certified\":true}")
            .expect(c -> c.status(400).contentType("application/problem+json").totalCalls(0)));
    s.add(
        Scenario.api(G, "v2-onboarding-requires-body-fields", "POST", "/v2/institutions/onboarding")
            .json("{\"productId\":\"prod-io\"}")
            .expect(
                c ->
                    c.status(400)
                        .json("/detail", "Validation failed")
                        .jsonPresent("/invalidParams/0/name")
                        .totalCalls(0)));
    s.add(
        Scenario.api(G, "v2-company-onboarding-requires-body-fields", "POST", "/v2/institutions/company/onboarding")
            .json("{\"productId\":\"prod-io\"}")
            .expect(c -> c.status(400).json("/detail", "Validation failed").totalCalls(0)));
    s.add(
        Scenario.api(G, "v1-onboarding-requires-body-fields", "POST", "/v1/institutions/onboarding")
            .json("{\"productId\":\"prod-io\"}")
            .expect(c -> c.status(400).contentType("application/problem+json").totalCalls(0)));
    s.add(
        Scenario.api(G, "v1-company-onboarding-requires-body-fields", "POST", "/v1/institutions/company/onboarding")
            .json("{\"productId\":\"prod-io\"}")
            .expect(c -> c.status(400).contentType("application/problem+json").totalCalls(0)));
  }

  static String companyOnboardingRequest(boolean certified) {
    return """
        {"productId":"prod-io","institutionType":"PG","taxCode":"%s",
         "billingData":{"businessName":"Acme","taxCode":"%s","certified":%s,"digitalAddress":"pec@t.it"},
         "users":[%s]}
        """.formatted(TAX, TAX, certified, MANAGER);
  }

  private static void v1Queries(List<Scenario> s) {
    s.add(
        Scenario.api(G, "v1-geographic-taxonomy-of-institution", "/v1/institutions/inst1/geographic-taxonomy")
            .stub(
                st ->
                    st.on(
                        PARTY_PROCESS,
                        "GET",
                        "/external/institutions/inst1",
                        Reply.json(
                            200,
                            "{\"id\":\"inst1\",\"externalId\":\"inst1\",\"description\":\"Comune\","
                                + "\"geographicTaxonomies\":[{\"code\":\"058091\",\"desc\":\"Roma\"}]}")))
            .expect(
                c ->
                    c.status(200)
                        .jsonSize("", 1)
                        .json("/0/code", "058091")
                        .json("/0/desc", "Roma")
                        .exactCalls("party-process GET /external/institutions/inst1")));
    s.add(
        Scenario.api(G, "v1-geographic-taxonomy-of-unknown-institution", "/v1/institutions/inst1/geographic-taxonomy")
            .stub(st -> st.on(PARTY_PROCESS, "GET", "/external/institutions/inst1", Reply.status(404)))
            .expect(c -> c.status(404).contentType("application/problem+json")));
    s.add(
        Scenario.api(G, "v1-geographic-taxonomies-by-tax-code", "/v1/institutions/geographic-taxonomies?taxCode=" + TAX + "&subunitCode=U1")
            .stub(
                st ->
                    st.on(
                        PARTY_PROCESS,
                        "GET",
                        "/institutions",
                        Reply.json(
                            200,
                            "{\"institutions\":[{\"id\":\"inst1\",\"externalId\":\"x\",\"description\":\"Comune\","
                                + "\"geographicTaxonomies\":[{\"code\":\"058091\",\"desc\":\"Roma\"}]}]}")))
            .expect(
                c ->
                    c.status(200)
                        .json("/0/code", "058091")
                        .exactCalls("party-process GET /institutions")
                        .call(PARTY_PROCESS, "GET", "/institutions")
                        .query("taxCode", TAX)
                        .query("subunitCode", "U1")));
    s.add(
        Scenario.api(G, "v1-geographic-taxonomies-none-found-is-empty", "/v1/institutions/geographic-taxonomies?taxCode=" + TAX)
            .stub(st -> st.on(PARTY_PROCESS, "GET", "/institutions", Reply.json(200, "{\"institutions\":[]}")))
            .expect(c -> c.status(200).jsonSize("", 0)));
    s.add(
        Scenario.api(G, "v1-onboarding-verify-get", "/v1/institutions/onboarding/verify?productId=prod-io&taxCode=" + TAX)
            .stub(InstitutionScenarios::productValid)
            .stub(st -> st.on(MS_ONBOARDING, "HEAD", "/v1/onboarding/verify", Reply.status(204)))
            .expect(
                c ->
                    c.status(200)
                        .exactCalls(
                            "ms-product GET /product/prod-io/valid",
                            "ms-product GET /product/prod-io/valid",
                            "ms-onboarding HEAD /v1/onboarding/verify")
                        .call(MS_ONBOARDING, "HEAD", "/v1/onboarding/verify")
                        .query("productId", "prod-io")
                        .query("taxCode", TAX)));
    s.add(
        Scenario.api(G, "v1-onboarding-verify-head", "HEAD", "/v1/institutions/onboarding?productId=prod-io&taxCode=" + TAX + "&origin=IPA&originId=o1&subunitCode=U1&institutionType=PA")
            .stub(InstitutionScenarios::productValid)
            .stub(st -> st.on(MS_ONBOARDING, "HEAD", "/v1/onboarding/verify", Reply.status(204)))
            .expect(
                c ->
                    c.status(204)
                        .call(MS_ONBOARDING, "HEAD", "/v1/onboarding/verify")
                        .query("productId", "prod-io")
                        .query("taxCode", TAX)
                        .query("origin", "IPA")
                        .query("originId", "o1")
                        .query("subunitCode", "U1")
                        .query("institutionType", "PA")));
    s.add(
        Scenario.api(G, "v1-onboarding-verify-not-onboarded", "HEAD", "/v1/institutions/onboarding?productId=prod-io&taxCode=" + TAX)
            .stub(InstitutionScenarios::productValid)
            .stub(st -> st.on(MS_ONBOARDING, "HEAD", "/v1/onboarding/verify", Reply.status(404)))
            .expect(c -> c.status(404)));
    s.add(
        Scenario.api(G, "v1-onboarding-verify-without-any-identifier", "HEAD", "/v1/institutions/onboarding?productId=prod-io")
            .stub(InstitutionScenarios::productValid)
            .expect(c -> c.status(400).callCount(MS_ONBOARDING, 0)));
    s.add(
        Scenario.api(G, "v1-institution-product-verify", "HEAD", "/v1/institutions/inst1/products/prod-io")
            .stub(InstitutionScenarios::productValid)
            .stub(st -> st.on(PARTY_PROCESS, "HEAD", "/onboarding/institution/inst1/products/prod-io", Reply.status(204)))
            .expect(
                c ->
                    c.status(204)
                        .exactCalls(
                            "ms-product GET /product/prod-io/valid",
                            "ms-product GET /product/prod-io/valid",
                            "party-process HEAD /onboarding/institution/inst1/products/prod-io")));
    s.add(
        Scenario.api(G, "v1-from-infocamere-with-trailing-slash", "/v1/institutions/from-infocamere/")
            .stub(
                st -> {
                  st.on(
                      PARTY_REGISTRY_PROXY,
                      "POST",
                      "/info-camere/institutions",
                      Reply.json(
                          200,
                          "{\"businesses\":[{\"businessTaxId\":\"" + TAX + "\",\"businessName\":\"Acme\"}],"
                              + "\"legalTaxId\":\"AAAAAA00A00A000A\"}"));
                  st.on(MS_ONBOARDING, "HEAD", "/v1/onboarding/verify", Reply.status(404));
                })
            .expect(
                c ->
                    c.status(200)
                        .contentType("application/json")
                        .json("/businesses/0/businessTaxId", TAX)
                        .json("/businesses/0/businessName", "Acme")
                        .call(PARTY_REGISTRY_PROXY, "POST", "/info-camere/institutions")
                        .jsonBody("/filter/legalTaxId", ParityJwt.User.ADMIN.fiscalNumber)));
    s.add(
        Scenario.api(G, "v1-from-infocamere-without-trailing-slash-is-not-served", "/v1/institutions/from-infocamere")
            .expect(c -> c.status4xx().totalCalls(0)));
    s.add(
        Scenario.api(G, "v1-institutions-of-user-product-unknown", "/v1/institutions?productId=prod-io")
            .stub(st -> st.on(MS_PRODUCT, "GET", "/product/prod-io", Reply.status(404)))
            .expect(c -> c.problem(404, "No product found with id prod-io")));
    s.add(
        Scenario.api(G, "v1-legal-address-verification", "POST", "/v1/institutions/verification/legal-address")
            .json("{\"taxCode\":\"" + TAX + "\"}")
            .stub(
                st ->
                    st.on(
                        PARTY_REGISTRY_PROXY,
                        "GET",
                        "/national-registries/legal-address",
                        Reply.json(200, "{\"address\":\"Via Roma 1\",\"zipCode\":\"00100\"}")))
            .expect(
                c ->
                    c.status(200)
                        .exactCalls("party-registry-proxy GET /national-registries/legal-address")
                        .call(PARTY_REGISTRY_PROXY, "GET", "/national-registries/legal-address")
                        .query("taxId", TAX)));
    s.add(
        Scenario.api(G, "v1-match-verification", "POST", "/v1/institutions/verification/match")
            .json("{\"taxCode\":\"" + TAX + "\",\"userDto\":{\"taxCode\":\"RSSMRA80A01H501U\",\"name\":\"Mario\",\"surname\":\"Rossi\"}}")
            .stub(
                st ->
                    st.on(
                        PARTY_REGISTRY_PROXY,
                        "GET",
                        "/national-registries/verify-legal",
                        Reply.json(200, "{\"verificationResult\":true}")))
            .expect(
                c ->
                    c.status(200)
                        .exactCalls("party-registry-proxy GET /national-registries/verify-legal")
                        .call(PARTY_REGISTRY_PROXY, "GET", "/national-registries/verify-legal")
                        .query("taxId", "RSSMRA80A01H501U")
                        .query("vatNumber", TAX)));
    s.add(
        Scenario.api(G, "v1-match-verification-requires-user", "POST", "/v1/institutions/verification/match")
            .json("{\"taxCode\":\"" + TAX + "\"}")
            .expect(c -> c.status(400).contentType("application/problem+json").totalCalls(0)));
    String billing =
        "{\"institutionId\":\"inst1\",\"externalId\":\"ext1\",\"origin\":\"IPA\",\"originId\":\"o1\",\"description\":\"Comune\","
            + "\"institutionType\":\"PA\",\"digitalAddress\":\"pec@t.it\",\"address\":\"Via Roma 1\",\"zipCode\":\"00100\","
            + "\"taxCode\":\""
            + TAX
            + "\",\"pricingPlan\":\"C1\",\"billing\":{\"vatNumber\":\"1\",\"recipientCode\":\"RC1\",\"publicServices\":true}}";
    String detail =
        "{\"id\":\"inst1\",\"externalId\":\"ext1\",\"origin\":\"IPA\",\"originId\":\"o1\",\"description\":\"Comune\","
            + "\"city\":\"Roma\",\"county\":\"RM\",\"country\":\"IT\",";
    s.add(
        Scenario.api(G, "v1-onboarded-institution-info-hidden-but-served", "/v1/institutions/inst1/products/prod-io/onboarded-institution-info")
            .stub(
                st -> {
                  st.on(PARTY_PROCESS, "GET", "/external/institutions/inst1/products/prod-io/billing", Reply.json(200, billing));
                  st.on(
                      PARTY_PROCESS,
                      "GET",
                      "/external/institutions/inst1",
                      Reply.json(200, detail + "\"geographicTaxonomies\":[{\"code\":\"058091\",\"desc\":\"Roma\"}]}"));
                })
            .expect(
                c ->
                    c.status(200)
                        .contentType("application/json")
                        .json("/institution/id", "inst1")
                        .json("/institution/institutionType", "PA")
                        .json("/institution/origin", "IPA")
                        .json("/institution/city", "Roma")
                        .json("/institution/billingData/businessName", "Comune")
                        .json("/institution/billingData/registeredOffice", "Via Roma 1")
                        .json("/institution/billingData/vatNumber", "1")
                        .json("/institution/billingData/recipientCode", "RC1")
                        .json("/geographicTaxonomies/0/code", "058091")
                        .exactCalls(
                            "party-process GET /external/institutions/inst1/products/prod-io/billing",
                            "party-process GET /external/institutions/inst1")));
    s.add(
        Scenario.api(G, "v1-onboarded-institution-info-without-taxonomies", "/v1/institutions/inst1/products/prod-io/onboarded-institution-info")
            .stub(
                st -> {
                  st.on(PARTY_PROCESS, "GET", "/external/institutions/inst1/products/prod-io/billing", Reply.json(200, billing));
                  st.on(PARTY_PROCESS, "GET", "/external/institutions/inst1", Reply.json(200, detail.substring(0, detail.length() - 1) + "}"));
                })
            .expect(c -> c.problem(400, "The institution inst1 does not have geographic taxonomies.")));
    s.add(
        Scenario.api(G, "v1-onboarded-institution-info-unknown-institution", "/v1/institutions/inst1/products/prod-io/onboarded-institution-info")
            .stub(st -> st.on(PARTY_PROCESS, "GET", "/external/institutions/inst1/products/prod-io/billing", Reply.status(404)))
            .expect(c -> c.status(404).contentType("application/problem+json")));
  }
}
