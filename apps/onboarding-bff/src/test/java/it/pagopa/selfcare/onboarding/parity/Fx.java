package it.pagopa.selfcare.onboarding.parity;

import static it.pagopa.selfcare.onboarding.parity.DownstreamStub.MS_DOCUMENT;
import static it.pagopa.selfcare.onboarding.parity.DownstreamStub.MS_IAM;
import static it.pagopa.selfcare.onboarding.parity.DownstreamStub.MS_ONBOARDING;

import it.pagopa.selfcare.onboarding.parity.DownstreamStub.Reply;
import java.nio.charset.StandardCharsets;

/** Shared downstream fixtures: payloads shaped like the real microservices answer. */
final class Fx {

  static final String OB = "ob1";
  static final String PRODUCT = "prod-io";
  static final String INSTITUTION = "inst1";
  static final byte[] PDF = "%PDF-1.4 parity-document".getBytes(StandardCharsets.UTF_8);

  private Fx() {}

  /** Onboarding as ms-onboarding returns it; {@code userId} is listed as onboarding user. */
  static String onboarding(String id, String userId) {
    return "{\"id\":\""
        + id
        + "\",\"productId\":\""
        + PRODUCT
        + "\",\"status\":\"PENDING\",\"institutionType\":\"PA\","
        + "\"institution\":{\"id\":\""
        + INSTITUTION
        + "\",\"origin\":\"IPA\",\"originId\":\"o1\",\"description\":\"Comune di Test\","
        + "\"institutionType\":\"PA\",\"taxCode\":\"00000000000\",\"digitalAddress\":\"pec@test.it\"},"
        + "\"users\":[{\"id\":\""
        + userId
        + "\",\"role\":\"MANAGER\",\"name\":\"Mario\",\"surname\":\"Rossi\","
        + "\"email\":\"mario@test.it\",\"taxCode\":\"RSSMRA80A01H501U\"}],"
        + "\"billing\":{\"vatNumber\":\"00000000000\"},\"signContract\":false}";
  }

  static String onboarding(String id) {
    return onboarding(id, ParityJwt.User.ADMIN.uid);
  }

  /** ms-onboarding answers withUserInfo for {@link #OB}; the onboarding user is {@code userId}. */
  static void onboardingExists(DownstreamStub stub, String userId) {
    stub.on(
        MS_ONBOARDING,
        "GET",
        "/v1/onboarding/" + OB + "/withUserInfo",
        Reply.json(200, onboarding(OB, userId)));
  }

  static void onboardingExists(DownstreamStub stub) {
    onboardingExists(stub, ParityJwt.User.ADMIN.uid);
  }

  static void onboardingSubmittedBy(DownstreamStub stub, String requesterUid) {
    stub.on(
        MS_ONBOARDING,
        "GET",
        "/v1/onboarding/" + OB + "/withUserInfo",
        Reply.json(200, "{\"id\":\"" + OB + "\",\"productId\":\"" + PRODUCT
            + "\",\"status\":\"PENDING\",\"users\":[],\"userRequester\":{\"userRequestUid\":\""
            + requesterUid + "\"}}"));
  }

  /** IAM grants every permission to ADMIN and none to anybody else. */
  static void iamAdminOnly(DownstreamStub stub) {
    stub.on(
        MS_IAM,
        "GET",
        "/iam/users/" + ParityJwt.User.ADMIN.uid + "/permissions/.*",
        Reply.json(200, "{\"hasPermission\":true}"));
    stub.on(MS_IAM, "GET", "/iam/users/.*/permissions/.*", Reply.json(200, "{\"hasPermission\":false}"));
  }

  static void iamAllowAll(DownstreamStub stub) {
    stub.on(MS_IAM, "GET", "/iam/users/.*/permissions/.*", Reply.json(200, "{\"hasPermission\":true}"));
  }

  static void iamDenyAll(DownstreamStub stub) {
    stub.on(MS_IAM, "GET", "/iam/users/.*/permissions/.*", Reply.json(200, "{\"hasPermission\":false}"));
  }

  static Reply document(String filename) {
    return Reply.bytes(200, "application/octet-stream", PDF)
        .header("Content-Disposition", "attachment; filename=\"" + filename + "\"");
  }

  static String product(String productId, String status, boolean enabled) {
    return "{\"productId\":\""
        + productId
        + "\",\"tenantId\":\"PNPG\",\"title\":\"Title of "
        + productId
        + "\",\"parentId\":null,\"status\":\""
        + status
        + "\",\"features\":{\"enabled\":"
        + enabled
        + ",\"delegable\":false},\"visualConfiguration\":{\"logoUrl\":\"https://cdn.test/"
        + productId
        + ".png\",\"depictImageUrl\":\"https://cdn.test/"
        + productId
        + "-depict.png\",\"logoBgColor\":\"#0066CC\"},\"contracts\":[]}";
  }
}
