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

  private static final String SPRING_MODEL = "it.pagopa.selfcare.onboarding.web.model.";
  private static final String SPRING_QUERY_PARAMETER =
      "@io.swagger.v3.oas.annotations.Parameter @org.springframework.web.bind.annotation.RequestParam ";

  private RequestBindingScenarios() {}

  static List<Scenario> all() {
    List<Scenario> scenarios = new ArrayList<>();
    for (String[] boundary : new String[][] {
        {"absent-defaults", "", "*", null, "0", "50"},
        {"empty-defaults", "?search=&page=&pageSize=&category=", "*", null, "0", "50"},
        {"bare-defaults", "?search&page&pageSize&category", "*", null, "0", "50"},
        {"repeated-integer", "?page=2&page=3", "*", null, "2", "50"},
        {"ignored-second-integer", "?page=2&page=bad", "*", null, "2", "50"},
        {"repeated-search", "?search=alpha&search=beta", "alpha,beta", null, "0", "50"},
        {"repeated-category", "?category=A&category=B", "*", "A,B", "0", "50"},
        {"empty-first-string", "?search=&search=beta", ",beta", null, "0", "50"}
    }) {
      scenarios.add(Scenario.api("binding-boundaries", boundary[0], "/v2/institutions/ipa" + boundary[1])
          .stub(stub -> stub.on(PARTY_REGISTRY_PROXY, "GET", "/institutions/ipa",
              Reply.json(200, "{\"count\":0,\"items\":[]}")))
          .expect(check -> {
            var call = check.status(200).totalCalls(1).propagatesIdentity()
                .call(PARTY_REGISTRY_PROXY, "GET", "/institutions/ipa")
                .query("search", boundary[2]).query("page", boundary[4]).query("pageSize", boundary[5]);
            if (boundary[3] == null) {
              call.queryAbsent("category");
            } else {
              call.query("category", boundary[3]);
            }
          }));
    }
    scenarios.add(Scenario.api("binding-boundaries", "invalid-first-integer", "/v2/institutions/ipa?page=bad&page=2")
        .expect(check -> check.status(400).contentType("application/problem+json").totalCalls(0)));
    scenarios.add(Scenario.api("binding-boundaries", "repeated-enum-uses-first",
            "/v2/tokens/ob1/download?type=CONTRACT_SIGNED&type=bad")
        .stub(Fx::onboardingExists).stub(Fx::iamAdminOnly)
        .stub(stub -> stub.on(MS_DOCUMENT, "GET", "/v1/document-content/ob1/contract-signed", Fx.document("signed.pdf")))
        .expect(check -> check.status(200).bodyBytes(Fx.PDF).totalCalls(3)));
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
    scenarios.addAll(conversionDetails());
    scenarios.addAll(bodyDetails());
    scenarios.addAll(remainingBindingDetails());
    return scenarios;
  }

  private static List<Scenario> remainingBindingDetails() {
    List<Scenario> scenarios = new ArrayList<>();
    String controller = "it.pagopa.selfcare.onboarding.web.controller.";
    for (String[] endpoint : new String[][] {
        {"/v2/tokens/ob1/reject", "void", "TokenV2Controller.rejectOnboarding(java.lang.String,"
            + SPRING_MODEL + "ReasonForRejectDto,java.security.Principal)"},
        {"/v2/institutions/onboarding", "void", "InstitutionV2Controller.onboarding("
            + SPRING_MODEL + "OnboardingProductDto)"},
        {"/v2/institutions/company/verify-manager", SPRING_MODEL + "VerifyManagerResponse",
            "InstitutionV2Controller.verifyManager(" + SPRING_MODEL + "VerifyManagerRequest,java.security.Principal)"},
        {"/v2/institutions/company/onboarding", "void", "InstitutionV2Controller.onboarding("
            + SPRING_MODEL + "CompanyOnboardingDto,java.security.Principal)"},
        {"/v1/users/validate", "void", "UserController.validate(" + SPRING_MODEL + "UserDataValidationDto)"},
        {"/v1/users/search-user", "it.pagopa.selfcare.onboarding.connector.model.user.UserId",
            "UserController.searchUser(" + SPRING_MODEL + "UserTaxCodeDto)"},
        {"/v1/users/onboarding", "void", "UserController.onboarding(" + SPRING_MODEL + "OnboardingUserDto)"},
        {"/v1/users/onboarding/aggregator", "void",
            "UserController.onboardingAggregator(" + SPRING_MODEL + "OnboardingUserDto)"},
        {"/v1/users/check-manager", SPRING_MODEL + "CheckManagerResponse",
            "UserController.checkManager(" + SPRING_MODEL + "CheckManagerDto)"},
        {"/v1/institutions/verification/match", SPRING_MODEL + "MatchInfoResultResource",
            "InstitutionController.postVerificationMatch(" + SPRING_MODEL + "VerificationMatchRequest)"},
        {"/v1/institutions/verification/legal-address", SPRING_MODEL + "InstitutionLegalAddressResource",
            "InstitutionController.postVerificationLegalAddress(" + SPRING_MODEL + "VerificationLegalAddressRequest)"},
        {"/v1/institutions/onboarding", "void",
            "InstitutionController.onboarding(" + SPRING_MODEL + "OnboardingProductDto)"},
        {"/v1/institutions/company/onboarding", "void",
            "InstitutionController.onboarding(" + SPRING_MODEL + "CompanyOnboardingDto)"}
    }) {
      String detail = "Required request body is missing: public " + endpoint[1] + " " + controller + endpoint[2];
      scenarios.add(Scenario.api("binding-final", "empty-body-" + endpoint[0], "POST", endpoint[0])
          .json("").expect(check -> check.problem(400, detail).totalCalls(0)));
      scenarios.add(Scenario.api("binding-final", "chunked-empty-body-" + endpoint[0], "POST", endpoint[0])
          .raw(null, new byte[0]).chunked()
          .expect(check -> check.problem(400, detail).totalCalls(0)));
      scenarios.add(Scenario.api("binding-final", "chunked-body-without-content-type-" + endpoint[0],
              "POST", endpoint[0])
          .raw(null, "{}".getBytes(StandardCharsets.UTF_8)).chunked()
          .expect(check -> check.problem(400, "Content-Type 'application/octet-stream' is not supported").totalCalls(0)));
    }
    scenarios.add(Scenario.api("binding-final", "json-null-required-body", "POST", "/v1/users/search-user")
        .json("null").expect(check -> check.problem(400, "Required request body is missing: public "
            + "it.pagopa.selfcare.onboarding.connector.model.user.UserId " + controller + "UserController.searchUser("
            + SPRING_MODEL + "UserTaxCodeDto)").totalCalls(0)));
    scenarios.add(Scenario.api("binding-final", "empty-optional-pg-body", "POST", "/v2/institutions/onboarding/users/pg")
        .json("").expect(check -> check.problem(500, "Cannot invoke \"" + SPRING_MODEL
            + "CompanyOnboardingUserDto.toString()\" because \"companyOnboardingUserDto\" is null").totalCalls(0)));
    for (String media : List.of("application/vnd.api+json", "application/json-patch+json")) {
      scenarios.add(Scenario.api("binding-final", "vendor-json-" + media, "POST", "/v1/users/search-user")
          .raw(media, "\"scalar\"".getBytes(StandardCharsets.UTF_8))
          .expect(check -> check.problem(400, scalarBody(SPRING_MODEL + "UserTaxCodeDto", "scalar")).totalCalls(0)));
    }
    for (String values : List.of("abc,1", "abc,def")) {
      String[] repeated = values.split(",");
      scenarios.add(Scenario.api("binding-final", "invalid-repeated-integer-" + values,
              "/v2/institutions/ipa?page=" + repeated[0] + "&page=" + repeated[1])
          .expect(check -> check.problem(400,
              "Failed to convert value of type 'java.lang.String[]' to required type 'java.lang.Integer'; "
                  + "For input string: \"" + values + "\"").totalCalls(0)));
    }
    scenarios.add(Scenario.api("binding-final", "invalid-repeated-verify-type",
            "/v1/institutions/onboarding/verify?productId=prod-io&taxCode=00000000000&verifyType=bad&verifyType=INTERNAL")
        .expect(check -> check.problem(400,
            enumMismatch(SPRING_MODEL + "VerifyType", SPRING_MODEL + "VerifyType", "bad")
                .replace("value of type 'java.lang.String'", "value of type 'java.lang.String[]'")).totalCalls(0)));
    scenarios.add(Scenario.api("binding-final", "body-without-content-type", "POST", "/v1/users/search-user")
        .raw(null, "{}".getBytes(StandardCharsets.UTF_8))
        .expect(check -> check.problem(400, "Content-Type 'application/octet-stream' is not supported").totalCalls(0)));
    scenarios.add(Scenario.api("binding-final", "multipart-without-content-type", "POST", "/v2/tokens/ob1/complete")
        .raw(null, "{}".getBytes(StandardCharsets.UTF_8))
        .expect(check -> check.problem(400, "Content-Type is not supported").totalCalls(0)));
    scenarios.add(Scenario.api("binding-final", "chunked-multipart-without-content-type", "POST", "/v2/tokens/ob1/complete")
        .raw(null, "{}".getBytes(StandardCharsets.UTF_8)).chunked()
        .expect(check -> check.problem(400, "Content-Type is not supported").totalCalls(0)));
    scenarios.add(Scenario.api("binding-final", "chunked-empty-multipart-without-content-type", "POST", "/v2/tokens/ob1/complete")
        .raw(null, new byte[0]).chunked()
        .expect(check -> check.problem(400, "Content-Type is not supported").totalCalls(0)));
    scenarios.add(Scenario.api("binding-final", "chunked-malformed-body-without-content-type", "POST", "/v1/users/search-user")
        .raw(null, "not-json".getBytes(StandardCharsets.UTF_8)).chunked()
        .expect(check -> check.problem(400, "Content-Type 'application/octet-stream' is not supported").totalCalls(0)));
    for (String media : List.of("application/json", "application/vnd.api+json")) {
      scenarios.add(Scenario.api("binding-final", "chunked-explicit-json-" + media, "POST", "/v1/users/search-user")
          .raw(media, "\"scalar\"".getBytes(StandardCharsets.UTF_8)).chunked()
          .expect(check -> check.problem(400, scalarBody(SPRING_MODEL + "UserTaxCodeDto", "scalar")).totalCalls(0)));
    }
    scenarios.add(Scenario.api("binding-final", "chunked-empty-optional-pg-body", "POST", "/v2/institutions/onboarding/users/pg")
        .raw(null, new byte[0]).chunked()
        .expect(check -> check.problem(500, "Cannot invoke \"" + SPRING_MODEL
            + "CompanyOnboardingUserDto.toString()\" because \"companyOnboardingUserDto\" is null").totalCalls(0)));
    scenarios.add(Scenario.api("binding-final", "chunked-optional-pg-body-without-content-type",
            "POST", "/v2/institutions/onboarding/users/pg")
        .raw(null, "{}".getBytes(StandardCharsets.UTF_8)).chunked()
        .expect(check -> check.problem(400, "Content-Type 'application/octet-stream' is not supported").totalCalls(0)));
    return scenarios;
  }

  /** Exact conversion failures of the immutable Spring oracle for query parameters (cd0a2751f). */
  private static List<Scenario> conversionDetails() {
    List<Scenario> scenarios = new ArrayList<>();
    for (String value : List.of("+0x10", "+0X10", "+#10")) {
      scenarios.add(Scenario.api("binding", "signed-plus-radix-rejected-" + label(value),
              "/v2/institutions/ipa?page=" + encode(value))
          .expect(check -> check.problem(400, integerMismatch("For input string: \"" + value + "\""))
              .totalCalls(0)));
    }
    for (String[] conversion : new String[][] {{"+16", "16"}, {"-0x10", "-16"}}) {
      String value = conversion[0];
      scenarios.add(Scenario.api("binding", "signed-integer-accepted-" + label(value),
              "/v2/institutions/ipa?page=" + encode(value))
          .stub(stub -> stub.on(PARTY_REGISTRY_PROXY, "GET", "/institutions/ipa",
              Reply.json(200, "{\"count\":0,\"items\":[]}")))
          .expect(check -> check.status(200).totalCalls(1)
              .call(PARTY_REGISTRY_PROXY, "GET", "/institutions/ipa").query("page", conversion[1])));
    }
    for (String[] conversion : new String[][] {
        {"\u20031", "1"}, {"1\u2003", "1"}, {"1\u20032", "12"}, {"0\u2003x10", "16"}, {"\u20281", "1"}, {"\u000B1", "1"}
    }) {
      String value = conversion[0];
      scenarios.add(Scenario.api("binding", "unicode-whitespace-integer-" + label(value),
              "/v2/institutions/ipa?page=" + encode(value))
          .stub(stub -> stub.on(PARTY_REGISTRY_PROXY, "GET", "/institutions/ipa",
              Reply.json(200, "{\"count\":0,\"items\":[]}")))
          .expect(check -> check.status(200).totalCalls(1)
              .call(PARTY_REGISTRY_PROXY, "GET", "/institutions/ipa").query("page", conversion[1])));
    }
    for (String value : List.of("\u00A01", "1\u00A02", "\u00A0", "\u202F1", "\u20071")) {
      scenarios.add(Scenario.api("binding", "non-breaking-space-integer-" + label(value),
              "/v2/institutions/ipa?page=" + encode(value))
          .expect(check -> check.problem(400, integerMismatch("For input string: \"" + value + "\""))
              .totalCalls(0)));
    }
    for (String[] invalid : new String[][] {
        {"1_0", "1_0"}, {" 1_0 ", "1_0"}, {"2147483648", "2147483648"}, {"abc", "abc"}, {"1.5", "1.5"}
    }) {
      scenarios.add(Scenario.api("binding", "invalid-integer-detail-" + label(invalid[0]),
              "/v2/institutions/ipa?page=" + encode(invalid[0]))
          .expect(check -> check.problem(400, integerMismatch("For input string: \"" + invalid[1] + "\""))
              .totalCalls(0)));
    }
    scenarios.add(Scenario.api("binding", "invalid-integer-detail-0x", "/v2/institutions/ipa?page=0x")
        .expect(check -> check.problem(400, integerMismatch("For input string: \"\" under radix 16"))
            .totalCalls(0)));
    scenarios.add(Scenario.api("binding", "invalid-integer-detail--0x", "/v2/institutions/ipa?page=-0x")
        .expect(check -> check.problem(400, integerMismatch("For input string: \"-\" under radix 16"))
            .totalCalls(0)));

    for (String value : List.of("bad", " bad ", "contract_signed", "CONTRACT SIGNED", "\u2003")) {
      scenarios.add(Scenario.api("binding", "invalid-required-enum-detail-" + label(value),
              "/v2/tokens/ob1/download?type=" + encode(value))
          .expect(check -> check.problem(400, enumMismatch(SPRING_MODEL + "DownloadDocumentType",
              SPRING_MODEL + "DownloadDocumentType", value)).totalCalls(0)));
    }
    for (String value : List.of(" ", "  ", "\t")) {
      scenarios.add(Scenario.api("binding", "blank-required-enum-detail-" + label(value),
              "/v2/tokens/ob1/download?type=" + encode(value))
          .expect(check -> check.problem(400,
              "Required request parameter 'type' for method parameter type DownloadDocumentType is present but converted to null")
              .totalCalls(0)));
    }
    for (String value : List.of("bad", "internal", "\u2003")) {
      scenarios.add(Scenario.api("binding", "invalid-verify-type-detail-" + label(value),
              "/v1/institutions/onboarding/verify?productId=prod-io&taxCode=00000000000&verifyType=" + encode(value))
          .expect(check -> check.problem(400, enumMismatch(SPRING_MODEL + "VerifyType",
              SPRING_MODEL + "VerifyType", value)).totalCalls(0)));
    }
    // Optional<InstitutionType>: only an empty value is absent, a blank one is converted and rejected
    for (String value : List.of("bad", "pa", " ", "\t")) {
      scenarios.add(Scenario.api("binding", "invalid-product-institution-type-detail-" + label(value),
              "/v1/product/prod-io?institutionType=" + encode(value))
          .expect(check -> check.problem(400, enumMismatch("java.util.Optional",
              "it.pagopa.selfcare.onboarding.common.InstitutionType", value)).totalCalls(0)));
    }
    return scenarios;
  }

  /** Exact body and media type failures of the immutable Spring oracle (cd0a2751f). */
  private static List<Scenario> bodyDetails() {
    List<Scenario> scenarios = new ArrayList<>();
    for (String[] endpoint : new String[][] {
        {"/v2/tokens/ob1/reject", "ReasonForRejectDto"},
        {"/v2/institutions/onboarding", "OnboardingProductDto"},
        {"/v2/institutions/onboarding/users/pg", "CompanyOnboardingUserDto"},
        {"/v2/institutions/company/verify-manager", "VerifyManagerRequest"},
        {"/v2/institutions/company/onboarding", "CompanyOnboardingDto"},
        {"/v1/users/validate", "UserDataValidationDto"},
        {"/v1/users/search-user", "UserTaxCodeDto"},
        {"/v1/users/onboarding", "OnboardingUserDto"},
        {"/v1/users/onboarding/aggregator", "OnboardingUserDto"},
        {"/v1/users/check-manager", "CheckManagerDto"},
        {"/v1/institutions/verification/match", "VerificationMatchRequest"},
        {"/v1/institutions/verification/legal-address", "VerificationLegalAddressRequest"},
        {"/v1/institutions/onboarding", "OnboardingProductDto"},
        {"/v1/institutions/company/onboarding", "CompanyOnboardingDto"}
    }) {
      scenarios.add(Scenario.api("binding", "scalar-json-body-" + endpoint[0], "POST", endpoint[0])
          .json("\"scalar\"")
          .expect(check -> check.problem(400, scalarBody(SPRING_MODEL + endpoint[1], "scalar")).totalCalls(0)));
    }
    scenarios.add(Scenario.api("binding", "scalar-nested-gpu-data-type-name", "POST", "/v2/institutions/onboarding")
        .json("{\"gpuData\":\"x\"}")
        .expect(check -> check.problem(400,
            scalarBody("it.pagopa.selfcare.onboarding.connector.model.onboarding.GPUData", "x")).totalCalls(0)));
    scenarios.add(Scenario.api("binding", "scalar-nested-aggregate-type-name", "POST", "/v2/institutions/onboarding")
        .json("{\"aggregates\":[\"x\"]}")
        .expect(check -> check.problem(400, scalarBody(SPRING_MODEL + "AggregateInstitution", "x")).totalCalls(0)));
    scenarios.add(Scenario.api("binding", "scalar-nested-aggregates-generic-type-name", "POST",
            "/v2/institutions/onboarding")
        .json("{\"aggregates\":\"x\"}")
        .expect(check -> check.problem(400, "JSON parse error: Cannot deserialize value of type `java.util.ArrayList<"
            + SPRING_MODEL + "AggregateInstitution>` from String value (token `JsonToken.VALUE_STRING`)")
            .totalCalls(0)));
    scenarios.add(Scenario.api("binding", "scalar-nested-users-generic-type-name", "POST", "/v1/users/onboarding")
        .json("{\"users\":\"x\"}")
        .expect(check -> check.problem(400, "JSON parse error: Cannot deserialize value of type `java.util.ArrayList<"
            + SPRING_MODEL + "UserDto>` from String value (token `JsonToken.VALUE_STRING`)").totalCalls(0)));
    scenarios.add(Scenario.api("binding", "scalar-nested-user-type-name", "POST", "/v1/users/onboarding")
        .json("{\"users\":[\"x\"]}")
        .expect(check -> check.problem(400, scalarBody(SPRING_MODEL + "UserDto", "x")).totalCalls(0)));
    scenarios.add(Scenario.api("binding", "scalar-nested-billing-data-type-name", "POST", "/v2/institutions/onboarding")
        .json("{\"billingData\":\"x\"}")
        .expect(check -> check.problem(400, scalarBody(SPRING_MODEL + "BillingDataDto", "x")).totalCalls(0)));

    for (String[] media : new String[][] {
        {"text/plain", "text/plain;charset=UTF-8"},
        {"application/xml", "application/xml;charset=UTF-8"},
        {"application/x-www-form-urlencoded", "application/x-www-form-urlencoded;charset=UTF-8"},
        {"Text/Plain;Foo=Bar", "text/plain;Foo=Bar;charset=UTF-8"},
        {"text/plain;charset=ISO-8859-1", "text/plain;charset=ISO-8859-1"},
        {"text/plain;foo=bar;charset=UTF-16", "text/plain;foo=bar;charset=UTF-16"}
    }) {
      scenarios.add(Scenario.api("binding", "unsupported-body-media-type-" + label(media[0]), "POST", "/v1/users/search-user")
          .raw(media[0], "taxCode".getBytes(StandardCharsets.UTF_8))
          .expect(check -> check.problem(400, "Content-Type '" + media[1] + "' is not supported").totalCalls(0)));
    }
    scenarios.add(Scenario.api("binding", "unsupported-body-media-type-token-reject", "POST", "/v2/tokens/ob1/reject")
        .raw("text/plain", "reason".getBytes(StandardCharsets.UTF_8))
        .expect(check -> check.problem(400, "Content-Type 'text/plain;charset=UTF-8' is not supported")
            .totalCalls(0)));
    // routes declaring multipart/form-data reject the raw header at mapping time
    for (String media : List.of("application/json", "text/plain")) {
      scenarios.add(Scenario.api("binding", "unsupported-multipart-media-type-" + label(media), "POST",
              "/v2/tokens/ob1/complete")
          .raw(media, "{}".getBytes(StandardCharsets.UTF_8))
          .expect(check -> check.problem(400, "Content-Type '" + media + "' is not supported").totalCalls(0)));
    }
    return scenarios;
  }

  // scenario ids stay printable even for whitespace-only input values
  private static String label(String value) {
    StringBuilder label = new StringBuilder();
    for (char c : value.toCharArray()) {
      label.append(Character.isLetterOrDigit(c) || "+#._-/;=".indexOf(c) >= 0 ? String.valueOf(c) : String.format("u%04x", (int) c));
    }
    return label.toString();
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private static String integerMismatch(String reason) {
    return "Failed to convert value of type 'java.lang.String' to required type 'java.lang.Integer'; " + reason;
  }

  private static String enumMismatch(String requiredType, String enumType, String value) {
    return "Failed to convert value of type 'java.lang.String' to required type '" + requiredType
        + "'; Failed to convert from type [java.lang.String] to type [" + SPRING_QUERY_PARAMETER + enumType
        + "] for value [" + value + "]";
  }

  private static String scalarBody(String type, String value) {
    return "JSON parse error: Cannot construct instance of `" + type + "` (although at least one Creator exists): "
        + "no String-argument constructor/factory method to deserialize from String value ('" + value + "')";
  }
}
