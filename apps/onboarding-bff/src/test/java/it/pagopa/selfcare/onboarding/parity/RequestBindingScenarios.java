package it.pagopa.selfcare.onboarding.parity;

import static it.pagopa.selfcare.onboarding.parity.DownstreamStub.PARTY_REGISTRY_PROXY;
import static it.pagopa.selfcare.onboarding.parity.DownstreamStub.MS_DOCUMENT;
import static it.pagopa.selfcare.onboarding.parity.DownstreamStub.MS_ONBOARDING;
import static it.pagopa.selfcare.onboarding.parity.DownstreamStub.MS_PRODUCT;

import it.pagopa.selfcare.onboarding.parity.DownstreamStub.Reply;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class RequestBindingScenarios {

  private RequestBindingScenarios() {}

  static List<Scenario> all() {
    List<Scenario> scenarios = new ArrayList<>();
    for (String[] conversion : new String[][] {
        {"0x10", "16"}, {"#10", "16"}, {"-0X10", "-16"}, {"010", "10"}, {"+010", "10"}, {"1 2", "12"},
        {" ", "0"}, {"\t", "0"}, {"\u2003", "0"}
    }) {
      String value = conversion[0];
      scenarios.add(Scenario.api("binding", "integer-" + value,
              "/v2/institutions/ipa?page=" + URLEncoder.encode(value, StandardCharsets.UTF_8))
          .stub(stub -> stub.on(PARTY_REGISTRY_PROXY, "GET", "/institutions/ipa",
              Reply.json(200, "{\"count\":0,\"items\":[]}")))
          .expect(check -> check.status(200).totalCalls(1)
              .call(PARTY_REGISTRY_PROXY, "GET", "/institutions/ipa").query("page", conversion[1])));
    }
    for (String value : List.of("1_0", "2147483648")) {
      scenarios.add(Scenario.api("binding", "invalid-integer-" + value,
              "/v2/institutions/ipa?page=" + URLEncoder.encode(value, StandardCharsets.UTF_8))
          .expect(check -> check.status(400).contentType("application/problem+json").totalCalls(0)));
    }
    scenarios.add(Scenario.api("binding", "required-enum-missing", "/v2/tokens/ob1/download")
        .expect(check -> check.problem(400,
            "Required request parameter 'type' for method parameter type DownloadDocumentType is not present")
            .totalCalls(0)));
    scenarios.add(Scenario.api("binding", "required-enum-empty", "/v2/tokens/ob1/download?type=")
        .expect(check -> check.problem(400,
            "Required request parameter 'type' for method parameter type DownloadDocumentType is present but converted to null")
            .totalCalls(0)));
    for (String value : List.of(" ", "\t", "\u2003", "CONTRACT SIGNED")) {
      scenarios.add(Scenario.api("binding", "invalid-required-enum-" + value,
              "/v2/tokens/ob1/download?type=" + URLEncoder.encode(value, StandardCharsets.UTF_8))
          .expect(check -> check.status(400).contentType("application/problem+json").totalCalls(0)));
    }
    scenarios.add(Scenario.api("binding", "required-enum-trimmed", "/v2/tokens/ob1/download?type=%20CONTRACT_SIGNED%20")
        .stub(Fx::onboardingExists)
        .stub(Fx::iamAdminOnly)
        .stub(stub -> stub.on(MS_DOCUMENT, "GET", "/v1/document-content/ob1/contract-signed", Fx.document("signed.pdf")))
        .expect(check -> check.status(200).bodyBytes(Fx.PDF).totalCalls(3)));
    for (String value : List.of("", " ", "\t", " INTERNAL ")) {
      scenarios.add(Scenario.api("binding", "optional-enum-" + value, "HEAD",
              "/v1/institutions/onboarding?productId=prod-io&taxCode=00000000000&verifyType="
                  + URLEncoder.encode(value, StandardCharsets.UTF_8))
          .stub(stub -> {
            stub.on(MS_PRODUCT, "GET", "/product/prod-io/valid", Reply.json(200, Fx.product("prod-io", "ACTIVE", true)));
            stub.on(MS_ONBOARDING, "HEAD", "/v1/onboarding/verify", Reply.status(204));
          })
          .expect(check -> check.status(204).callCount(MS_ONBOARDING, 1)));
    }
    for (String value : List.of("\u2003", "internal")) {
      scenarios.add(Scenario.api("binding", "invalid-optional-enum-" + value, "HEAD",
              "/v1/institutions/onboarding?productId=prod-io&taxCode=00000000000&verifyType="
                  + URLEncoder.encode(value, StandardCharsets.UTF_8))
          .expect(check -> check.status(400).totalCalls(0)));
    }
    return scenarios;
  }
}
