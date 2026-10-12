package it.pagopa.selfcare.onboarding.parity;

import static it.pagopa.selfcare.onboarding.parity.DownstreamStub.MS_PRODUCT;

import it.pagopa.selfcare.onboarding.parity.DownstreamStub.Reply;
import it.pagopa.selfcare.onboarding.parity.ParityJwt.User;
import it.pagopa.selfcare.onboarding.parity.Operations.Operation;
import java.util.ArrayList;
import java.util.List;

/** Authentication, tenant context and framework-level errors, for every operation of the contract. */
final class SecurityScenarios {

  private static final String G = "security";
  private static final String PRODUCTS = "/v1/products";

  private SecurityScenarios() {}

  static List<Scenario> all() {
    List<Scenario> s = new ArrayList<>();
    everyOperation(s);
    authentication(s);
    tokenClaims(s);
    tenant(s);
    frameworkErrors(s);
    return s;
  }

  private static void productsOk(DownstreamStub stub) {
    stub.on(MS_PRODUCT, "GET", "/product", Reply.json(200, "[]"));
  }

  /** Every operation (including the hidden one) is protected: no token, no downstream traffic. */
  private static void everyOperation(List<Scenario> s) {
    List<Operation> operations = new ArrayList<>(Operations.springOperations());
    operations.add(
        new Operation(
            "GET", "/v1/institutions/{externalInstitutionId}/products/{productId}/onboarded-institution-info", "hidden", null));
    for (Operation op : operations) {
      boolean head = op.method().equals("HEAD");
      s.add(
          Scenario.of(G, "unauthenticated " + op.label(), op.method(), op.concretePath())
              .header("X-Tenant-Id", "PNPG")
              .expect(
                  c -> {
                    c.status(401).totalCalls(0).headerContains("WWW-Authenticate", "Bearer");
                    if (!head) {
                      c.problem(401, null);
                    }
                  }));
      s.add(
          Scenario.api(G, "tenant-header-mismatch " + op.label(), op.method(), op.concretePath())
              .setHeader("X-Tenant-Id", "AR")
              .expect(
                  c -> {
                    c.status(400).totalCalls(0);
                    if (!head) {
                      c.problem(400, "Invalid tenant context");
                    }
                  }));
      s.add(
          Scenario.of(G, "tenant-header-missing " + op.label(), op.method(), op.concretePath())
              .as(User.ADMIN)
              .expect(
                  c -> {
                    c.status(400).totalCalls(0);
                    if (!head) {
                      c.problem(400, "Invalid tenant context");
                    }
                  }));
    }
  }

  private static void authentication(List<Scenario> s) {
    s.add(
        Scenario.get(G, "no-authorization-header", PRODUCTS)
            .header("X-Tenant-Id", "PNPG")
            .stub(SecurityScenarios::productsOk)
            .expect(c -> c.problem(401, null).totalCalls(0)));
    s.add(
        Scenario.get(G, "expired-token", PRODUCTS)
            .bearer(ParityJwt.expired(User.ADMIN))
            .header("X-Tenant-Id", "PNPG")
            .stub(SecurityScenarios::productsOk)
            .expect(c -> c.problem(401, null).totalCalls(0)));
    s.add(
        Scenario.get(G, "token-signed-with-untrusted-key", PRODUCTS)
            .bearer(ParityJwt.forged(User.ADMIN))
            .header("X-Tenant-Id", "PNPG")
            .stub(SecurityScenarios::productsOk)
            .expect(c -> c.problem(401, null).totalCalls(0)));
    s.add(
        Scenario.get(G, "garbage-token", PRODUCTS)
            .bearer("not.a.jwt")
            .header("X-Tenant-Id", "PNPG")
            .stub(SecurityScenarios::productsOk)
            .expect(c -> c.problem(401, null).totalCalls(0)));
    s.add(
        Scenario.get(G, "non-bearer-scheme", PRODUCTS)
            .header("Authorization", "Basic dXNlcjpwYXNz")
            .header("X-Tenant-Id", "PNPG")
            .stub(SecurityScenarios::productsOk)
            .expect(c -> c.problem(401, null).totalCalls(0)));
    s.add(
        Scenario.api(G, "valid-token-is-accepted", PRODUCTS)
            .stub(SecurityScenarios::productsOk)
            .expect(c -> c.status(200).exactCalls("ms-product GET /product")));
  }

  /**
   * Token claim variations. Spring's jjwt based authentication is lenient on exp / iat / uid /
   * fiscal_number, rejects a not yet valid or unsigned token with 401 and fails with 500 when the
   * issuer is unknown or absent. All of it is observed on the unchanged reference.
   */
  private static void tokenClaims(List<Scenario> s) {
    java.util.Map<String, java.util.function.Consumer<ParityJwt.Spec>> accepted = new java.util.LinkedHashMap<>();
    accepted.put("without-exp", ParityJwt.Spec::noExpiration);
    accepted.put("without-iat", ParityJwt.Spec::noIssuedAt);
    accepted.put("without-exp-and-iat", sp -> sp.noExpiration().noIssuedAt());
    accepted.put("without-uid", ParityJwt.Spec::noUid);
    accepted.put("empty-uid", sp -> sp.uid(""));
    accepted.put("without-fiscal-number", ParityJwt.Spec::noFiscalNumber);
    accepted.put("pagopa-issuer", sp -> sp.issuer("PAGOPA"));
    accepted.forEach(
        (name, variant) ->
            s.add(
                Scenario.get(G, "token-" + name + "-is-accepted", PRODUCTS)
                    .bearer(ParityJwt.custom(User.ADMIN, variant))
                    .header("X-Tenant-Id", "PNPG")
                    .stub(SecurityScenarios::productsOk)
                    .expect(c -> c.status(200).exactCalls("ms-product GET /product"))));

    s.add(
        Scenario.get(G, "token-not-yet-valid", PRODUCTS)
            .bearer(ParityJwt.custom(User.ADMIN, sp -> sp.notBefore(java.time.Instant.now().plusSeconds(3600))))
            .header("X-Tenant-Id", "PNPG")
            .stub(SecurityScenarios::productsOk)
            .expect(c -> c.problem(401, null).totalCalls(0)));
    s.add(
        Scenario.get(G, "token-unsigned-alg-none", PRODUCTS)
            .bearer(ParityJwt.custom(User.ADMIN, ParityJwt.Spec::unsigned))
            .header("X-Tenant-Id", "PNPG")
            .stub(SecurityScenarios::productsOk)
            .expect(c -> c.problem(401, null).totalCalls(0)));
    s.add(
        Scenario.get(G, "token-unknown-issuer", PRODUCTS)
            .bearer(ParityJwt.custom(User.ADMIN, sp -> sp.issuer("UNKNOWN")))
            .header("X-Tenant-Id", "PNPG")
            .stub(SecurityScenarios::productsOk)
            .expect(c -> c.problem(500, "Unknown issuer").totalCalls(0)));
    s.add(
        Scenario.get(G, "token-without-issuer", PRODUCTS)
            .bearer(ParityJwt.custom(User.ADMIN, sp -> sp.issuer(null)))
            .header("X-Tenant-Id", "PNPG")
            .stub(SecurityScenarios::productsOk)
            .expect(c -> c.status(500).contentType("application/problem+json").totalCalls(0)));

    // The uid claim drives the IAM permission lookup: a token without it is asked as "uid_not_provided".
    s.add(
        Scenario.get(G, "token-without-uid-is-asked-to-iam-as-uid_not_provided", "/v2/tokens/" + Fx.OB)
            .bearer(ParityJwt.custom(User.ADMIN, ParityJwt.Spec::noUid))
            .header("X-Tenant-Id", "PNPG")
            .stub(Fx::onboardingExists)
            .stub(Fx::iamAllowAll)
            .expect(
                c ->
                    c.status(200)
                        .call("ms-iam", "GET", "/iam/users/uid_not_provided/permissions/Selc:ViewAccountPage")
                        .times(1)));
    s.add(
        Scenario.get(G, "token-with-empty-uid-is-asked-to-iam", "/v2/tokens/" + Fx.OB)
            .bearer(ParityJwt.custom(User.ADMIN, sp -> sp.uid("")))
            .header("X-Tenant-Id", "PNPG")
            .stub(Fx::onboardingExists)
            .stub(Fx::iamAllowAll)
            .expect(
                c ->
                    c.status(200)
                        .call("ms-iam", "GET", "/iam/users//permissions/Selc:ViewAccountPage")
                        .times(1)));
  }

  private static void tenant(List<Scenario> s) {
    s.add(
        Scenario.get(G, "tenant-default-is-PNPG-without-claim", PRODUCTS)
            .as(User.ADMIN)
            .header("X-Tenant-Id", "PNPG")
            .stub(SecurityScenarios::productsOk)
            .expect(c -> c.status(200).propagatesIdentity()));
    s.add(
        Scenario.get(G, "tenant-AR-claim-with-AR-header", PRODUCTS)
            .tenant("AR")
            .stub(SecurityScenarios::productsOk)
            .expect(c -> c.status(200).propagatesIdentity()));
    s.add(
        Scenario.get(G, "tenant-AR-claim-with-PNPG-header", PRODUCTS)
            .as(User.ADMIN, "AR")
            .header("X-Tenant-Id", "PNPG")
            .stub(SecurityScenarios::productsOk)
            .expect(c -> c.problem(400, "Invalid tenant context").totalCalls(0)));
    s.add(
        Scenario.get(G, "tenant-PNPG-default-with-AR-header", PRODUCTS)
            .as(User.ADMIN)
            .header("X-Tenant-Id", "AR")
            .stub(SecurityScenarios::productsOk)
            .expect(c -> c.problem(400, "Invalid tenant context").totalCalls(0)));
    s.add(
        Scenario.get(G, "tenant-header-missing", PRODUCTS)
            .as(User.ADMIN)
            .stub(SecurityScenarios::productsOk)
            .expect(c -> c.problem(400, "Invalid tenant context").totalCalls(0)));
    s.add(
        Scenario.get(G, "tenant-header-empty", PRODUCTS)
            .as(User.ADMIN)
            .header("X-Tenant-Id", "")
            .stub(SecurityScenarios::productsOk)
            .expect(c -> c.problem(400, "Invalid tenant context").totalCalls(0)));
    s.add(
        Scenario.get(G, "tenant-unsupported-value", PRODUCTS)
            .tenant("XX")
            .stub(SecurityScenarios::productsOk)
            .expect(c -> c.problem(400, "Invalid tenant context").totalCalls(0)));
    s.add(
        Scenario.get(G, "authentication-comes-before-tenant-check", PRODUCTS)
            .header("X-Tenant-Id", "AR")
            .stub(SecurityScenarios::productsOk)
            .expect(c -> c.status(401).totalCalls(0)));
  }

  private static void frameworkErrors(List<Scenario> s) {
    s.add(
        Scenario.api(G, "method-not-allowed", "DELETE", PRODUCTS)
            .expect(c -> c.problem(405, null).totalCalls(0)));
    s.add(
        Scenario.of(G, "unknown-path-unauthenticated", "GET", "/v1/unknown")
            .expect(c -> c.status(401).totalCalls(0)));
    s.add(
        Scenario.api(G, "malformed-json-body", "POST", "/v1/users/search-user")
            .json("{bad")
            .expect(c -> c.status(400).contentType("application/problem+json").totalCalls(0)));
    s.add(
        Scenario.api(G, "missing-json-body", "POST", "/v1/users/search-user")
            .expect(c -> c.status(400).contentType("application/problem+json").totalCalls(0)));
    s.add(
        Scenario.api(G, "bean-validation-lists-invalid-params", "POST", "/v1/users/search-user")
            .json("{}")
            .expect(
                c ->
                    c.status(400)
                        .contentType("application/problem+json")
                        .json("/title", "Bad Request")
                        .json("/detail", "Validation failed")
                        .json("/invalidParams/0/name", "userTaxCodeDto.taxCode")
                        .jsonPresent("/invalidParams/0/reason")
                        .jsonSize("/invalidParams", 1)
                        .totalCalls(0)));
    s.add(
        Scenario.api(G, "unsupported-media-type", "POST", "/v1/users/search-user")
            .raw("text/plain", "taxCode".getBytes())
            .expect(c -> c.status(400).contentType("application/problem+json").totalCalls(0)));
  }
}
