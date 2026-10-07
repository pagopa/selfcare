package it.pagopa.selfcare.onboarding.parity;

import static it.pagopa.selfcare.onboarding.parity.DownstreamStub.MS_PRODUCT;
import static it.pagopa.selfcare.onboarding.parity.DownstreamStub.PARTY_REGISTRY_PROXY;

import it.pagopa.selfcare.onboarding.parity.DownstreamStub.Reply;
import java.util.ArrayList;
import java.util.List;

/** Products (new product-ms model), product v2 (tenant aware) and the IPA registry. */
final class ProductScenarios {

  private static final String G = "products";

  private ProductScenarios() {}

  private static String userContract(boolean withPath) {
    return "{\"onboardingType\":\"USER\",\"institutionType\":\"DEFAULT\",\"contractType\":\"CONTRACT\","
        + (withPath ? "\"path\":\"user/contract.html\"," : "")
        + "\"version\":\"v2\",\"enabled\":true}";
  }

  private static String productWithContract(String id, String status, boolean enabled, boolean withPath) {
    String base = Fx.product(id, status, enabled);
    return base.replace("\"contracts\":[]", "\"contracts\":[" + userContract(withPath) + "]");
  }

  static List<Scenario> all() {
    List<Scenario> s = new ArrayList<>();
    v1(s);
    v2(s);
    ipa(s);
    return s;
  }

  private static void v1(List<Scenario> s) {
    String list =
        "["
            + productWithContract("prod-io", "ACTIVE", true, true)
            + ","
            + productWithContract("prod-old", "INACTIVE", true, true)
            + ","
            + productWithContract("prod-off", "ACTIVE", false, true)
            + ","
            + productWithContract("prod-nopath", "ACTIVE", true, false)
            + "]";
    s.add(
        Scenario.api(G, "list-keeps-only-active-and-enabled", "/v1/products")
            .stub(st -> st.on(MS_PRODUCT, "GET", "/product", Reply.json(200, list)))
            .expect(
                c ->
                    c.status(200)
                        .contentType("application/json")
                        .jsonSize("", 2)
                        .json("/0/id", "prod-io")
                        .json("/0/title", "Title of prod-io")
                        .json("/0/status", "ACTIVE")
                        .json("/0/logo", "https://cdn.test/prod-io.png")
                        .json("/0/logoBgColor", "#0066CC")
                        .json("/1/id", "prod-nopath")
                        .exactCalls("ms-product GET /product")
                        .propagatesIdentity()
                        .call(MS_PRODUCT, "GET", "/product")
                        .query("valid", "true")
                        .query("rootOnly", "false")
                        .queryAbsent("tenantId")));
    s.add(
        Scenario.api(G, "list-empty", "/v1/products")
            .stub(st -> st.on(MS_PRODUCT, "GET", "/product", Reply.json(200, "[]")))
            .expect(c -> c.status(200).jsonSize("", 0)));
    s.add(
        Scenario.api(G, "list-propagates-tenant-AR", "/v1/products")
            .tenant("AR")
            .stub(st -> st.on(MS_PRODUCT, "GET", "/product", Reply.json(200, "[]")))
            .expect(c -> c.status(200).propagatesIdentity()));
    s.add(
        Scenario.api(G, "admin-list-roots-with-user-contract", "/v1/products/admin")
            .stub(st -> st.on(MS_PRODUCT, "GET", "/product", Reply.json(200, list)))
            .expect(
                c ->
                    c.status(200)
                        .jsonSize("", 1)
                        .json("/0/id", "prod-io")
                        .call(MS_PRODUCT, "GET", "/product")
                        .query("rootOnly", "true")
                        .query("valid", "true")));
    s.add(
        Scenario.api(G, "by-id", "/v1/product/prod-io")
            .stub(
                st ->
                    st.on(
                        MS_PRODUCT,
                        "GET",
                        "/product/prod-io",
                        Reply.json(200, Fx.product("prod-io", "ACTIVE", true))))
            .expect(
                c ->
                    c.status(200)
                        .contentType("application/json")
                        .json("/id", "prod-io")
                        .json("/title", "Title of prod-io")
                        .json("/status", "ACTIVE")
                        .exactCalls("ms-product GET /product/prod-io")
                        .propagatesIdentity()));
    s.add(
        Scenario.api(G, "by-id-not-found", "/v1/product/missing")
            .stub(
                st ->
                    st.on(
                        MS_PRODUCT,
                        "GET",
                        "/product/missing",
                        Reply.json(404, "{\"title\":\"Not Found\",\"status\":404,\"detail\":\"x\"}")))
            .expect(c -> c.problem(404, "No product found with id missing")));
  }

  private static void v2(List<Scenario> s) {
    String origins = "{\"origins\":[{\"institutionType\":\"PA\",\"origin\":\"IPA\",\"labelKey\":\"pa.ipa\"}]}";
    for (String tenant : new String[] {"PNPG", "AR"}) {
      s.add(
          Scenario.api(G, "v2-origins-" + tenant, "/v2/product?productId=prod-io")
              .tenant(tenant)
              .stub(st -> st.on(MS_PRODUCT, "GET", "/product/origins", Reply.json(200, origins)))
              .expect(
                  c ->
                      c.status(200)
                          .contentType("application/json")
                          .json("/origins/0/institutionType", "PA")
                          .json("/origins/0/origin", "IPA")
                          .json("/origins/0/labelKey", "pa.ipa")
                          .exactCalls("ms-product GET /product/origins")
                          .propagatesIdentity()
                          .call(MS_PRODUCT, "GET", "/product/origins")
                          .query("productId", "prod-io")
                          .query("tenantId", tenant)));
    }
    s.add(
        Scenario.api(G, "v2-origins-requires-product", "/v2/product")
            .expect(c -> c.status(400).contentType("application/problem+json").totalCalls(0)));
    s.add(
        Scenario.of(G, "v2-origins-requires-tenant-header", "GET", "/v2/product?productId=prod-io")
            .as(ParityJwt.User.ADMIN)
            .expect(c -> c.problem(400, "Invalid tenant context").totalCalls(0)));
    s.add(
        Scenario.api(G, "v2-origins-product-not-found", "/v2/product?productId=prod-io")
            .stub(st -> st.on(MS_PRODUCT, "GET", "/product/origins", Reply.status(404)))
            .expect(c -> c.status(404).contentType("application/problem+json")));

    String docs =
        "[{\"id\":\"doc1\",\"name\":\"Doc 1\",\"labelKey\":\"doc.one\",\"required\":true,"
            + "\"mimeType\":\"application/pdf\",\"maxDocumentsRequired\":2,\"storageOrigin\":\"USER\"},"
            + "{\"id\":\"doc2\",\"name\":\"Doc 2\",\"labelKey\":\"doc.two\",\"required\":false,"
            + "\"mimeType\":\"application/pdf\",\"maxDocumentsRequired\":1,\"storageOrigin\":\"SYSTEM\"}]";
    String query = "?institutionType=PA&origin=IPA";
    for (String tenant : new String[] {"PNPG", "AR"}) {
      s.add(
          Scenario.api(G, "v2-required-documents-" + tenant, "/v2/product/prod-io/required-documents" + query)
              .tenant(tenant)
              .stub(
                  st ->
                      st.on(
                          MS_PRODUCT,
                          "GET",
                          "/product/prod-io/required-documents",
                          Reply.json(200, docs)))
              .expect(
                  c ->
                      c.status(200)
                          .contentType("application/json")
                          .jsonSize("", 2)
                          .json("/0/id", "doc1")
                          .json("/0/name", "Doc 1")
                          .json("/0/labelKey", "doc.one")
                          .json("/0/required", true)
                          .json("/0/mimeType", "application/pdf")
                          .json("/0/maxDocumentsRequired", 2)
                          .json("/0/storageOrigin", "USER")
                          .json("/1/required", false)
                          .exactCalls("ms-product GET /product/prod-io/required-documents")
                          .propagatesIdentity()
                          .call(MS_PRODUCT, "GET", "/product/prod-io/required-documents")
                          .query("tenantId", tenant)
                          .query("origin", "IPA")
                          .query("institutionType", "PA")));
    }
    s.add(
        Scenario.api(G, "v2-required-documents-missing-params", "/v2/product/prod-io/required-documents?origin=IPA")
            .expect(c -> c.status(400).contentType("application/problem+json").totalCalls(0)));
    s.add(
        Scenario.of(G, "v2-required-documents-requires-tenant-header", "GET", "/v2/product/prod-io/required-documents" + query)
            .as(ParityJwt.User.ADMIN)
            .expect(c -> c.problem(400, "Invalid tenant context").totalCalls(0)));
    s.add(
        Scenario.api(G, "v2-required-documents-product-not-found", "/v2/product/prod-io/required-documents" + query)
            .stub(st -> st.on(MS_PRODUCT, "GET", "/product/prod-io/required-documents", Reply.status(404)))
            .expect(c -> c.status(404).contentType("application/problem+json")));

    for (boolean enabled : new boolean[] {true, false}) {
      for (String tenant : new String[] {"PNPG", "AR"}) {
        s.add(
            Scenario.api(
                    G,
                    "v2-required-documents-enabled-" + enabled + "-" + tenant,
                    "/v2/product/prod-io/required-documents/enabled" + query)
                .tenant(tenant)
                .stub(
                    st ->
                        st.on(
                            MS_PRODUCT,
                            "HEAD",
                            "/product/prod-io/required-documents/enabled",
                            Reply.status(200).header("X-Required-Documents-Enabled", String.valueOf(enabled))))
                .expect(
                    c ->
                        c.status(200)
                            .contentType("application/json")
                            .json("/requiredDocumentsEnabled", enabled)
                            .exactCalls("ms-product HEAD /product/prod-io/required-documents/enabled")
                            .propagatesIdentity()
                            .call(MS_PRODUCT, "HEAD", "/product/prod-io/required-documents/enabled")
                            .query("tenantId", tenant)
                            .query("origin", "IPA")
                            .query("institutionType", "PA")));
      }
    }
    s.add(
        Scenario.api(G, "v2-required-documents-enabled-header-absent-means-false", "/v2/product/prod-io/required-documents/enabled" + query)
            .stub(
                st ->
                    st.on(
                        MS_PRODUCT,
                        "HEAD",
                        "/product/prod-io/required-documents/enabled",
                        Reply.status(200)))
            .expect(c -> c.status(200).json("/requiredDocumentsEnabled", false)));
    s.add(
        Scenario.of(G, "v2-required-documents-enabled-requires-tenant-header", "GET", "/v2/product/prod-io/required-documents/enabled" + query)
            .as(ParityJwt.User.ADMIN)
            .expect(c -> c.problem(400, "Invalid tenant context").totalCalls(0)));
  }

  private static void ipa(List<Scenario> s) {
    String page =
        "{\"count\":2,\"items\":["
            + "{\"originId\":\"o1\",\"description\":\"Comune di Test\",\"taxCode\":\"00000000000\","
            + "\"digitalAddress\":\"pec@test.it\",\"address\":\"Via Roma 1\",\"zipCode\":\"00100\","
            + "\"category\":\"L6\",\"origin\":\"IPA\"},"
            + "{\"originId\":\"o2\",\"description\":\"Altro Comune\",\"taxCode\":\"11111111111\","
            + "\"digitalAddress\":\"pec2@test.it\",\"address\":\"Via Po 2\",\"zipCode\":\"10100\","
            + "\"category\":\"L6\",\"origin\":\"IPA\"}]}";
    s.add(
        Scenario.api(G, "ipa-search-defaults", "/v2/institutions/ipa")
            .stub(st -> st.on(PARTY_REGISTRY_PROXY, "GET", "/institutions/ipa", Reply.json(200, page)))
            .expect(
                c ->
                    c.status(200)
                        .contentType("application/json")
                        .json("/count", 2)
                        .jsonSize("/items", 2)
                        .json("/items/0/originId", "o1")
                        .json("/items/0/taxCode", "00000000000")
                        .json("/items/0/description", "Comune di Test")
                        .json("/items/0/digitalAddress", "pec@test.it")
                        .json("/items/0/address", "Via Roma 1")
                        .json("/items/0/zipCode", "00100")
                        .json("/items/0/category", "L6")
                        .json("/items/0/origin", "IPA")
                        .exactCalls("party-registry-proxy GET /institutions/ipa")
                        .propagatesIdentity()
                        .call(PARTY_REGISTRY_PROXY, "GET", "/institutions/ipa")
                        .query("search", "*")
                        .query("page", "0")
                        .query("pageSize", "50")
                        .queryAbsent("category")));
    s.add(
        Scenario.api(G, "ipa-search-pagination-and-category", "/v2/institutions/ipa?search=comune&category=L6&page=3&pageSize=10")
            .stub(st -> st.on(PARTY_REGISTRY_PROXY, "GET", "/institutions/ipa", Reply.json(200, page)))
            .expect(
                c ->
                    c.status(200)
                        .call(PARTY_REGISTRY_PROXY, "GET", "/institutions/ipa")
                        .query("search", "comune")
                        .query("category", "L6")
                        .query("page", "3")
                        .query("pageSize", "10")));
    s.add(
        Scenario.api(G, "ipa-search-empty-page", "/v2/institutions/ipa?search=none")
            .stub(
                st ->
                    st.on(
                        PARTY_REGISTRY_PROXY,
                        "GET",
                        "/institutions/ipa",
                        Reply.json(200, "{\"count\":0,\"items\":[]}")))
            .expect(c -> c.status(200).json("/count", 0).jsonSize("/items", 0)));
    s.add(
        Scenario.api(G, "ipa-search-rejects-bad-page", "/v2/institutions/ipa?page=abc")
            .expect(c -> c.status(400).contentType("application/problem+json").totalCalls(0)));
    s.add(
        Scenario.api(G, "ipa-search-registry-bad-request", "/v2/institutions/ipa")
            .stub(st -> st.on(PARTY_REGISTRY_PROXY, "GET", "/institutions/ipa", Reply.status(400)))
            .expect(c -> c.status(400).contentType("application/problem+json")));

    String one =
        "{\"originId\":\"o1\",\"description\":\"Comune di Test\",\"taxCode\":\"00000000000\","
            + "\"digitalAddress\":\"pec@test.it\",\"address\":\"Via Roma 1\",\"zipCode\":\"00100\","
            + "\"category\":\"L6\",\"origin\":\"IPA\"}";
    s.add(
        Scenario.api(G, "ipa-by-tax-code", "/v2/institutions/ipa/00000000000?category=L6")
            .stub(
                st ->
                    st.on(
                        PARTY_REGISTRY_PROXY,
                        "GET",
                        "/institutions/ipa/00000000000",
                        Reply.json(200, one)))
            .expect(
                c ->
                    c.status(200)
                        .contentType("application/json")
                        .json("/originId", "o1")
                        .json("/taxCode", "00000000000")
                        .json("/description", "Comune di Test")
                        .json("/origin", "IPA")
                        .exactCalls("party-registry-proxy GET /institutions/ipa/00000000000")
                        .propagatesIdentity()
                        .call(PARTY_REGISTRY_PROXY, "GET", "/institutions/ipa/00000000000")
                        .query("category", "L6")));
    s.add(
        Scenario.api(G, "ipa-by-tax-code-without-category", "/v2/institutions/ipa/00000000000")
            .stub(
                st ->
                    st.on(
                        PARTY_REGISTRY_PROXY,
                        "GET",
                        "/institutions/ipa/00000000000",
                        Reply.json(200, one)))
            .expect(
                c ->
                    c.status(200)
                        .call(PARTY_REGISTRY_PROXY, "GET", "/institutions/ipa/00000000000")
                        .queryAbsent("category")));
    s.add(
        Scenario.api(G, "ipa-by-tax-code-not-found", "/v2/institutions/ipa/99999999999")
            .stub(
                st ->
                    st.on(
                        PARTY_REGISTRY_PROXY,
                        "GET",
                        "/institutions/ipa/99999999999",
                        Reply.json(404, "{\"status\":404,\"detail\":\"not found\"}")))
            .expect(c -> c.status(404).contentType("application/problem+json")));
  }
}
