package it.pagopa.selfcare.onboarding.parity;

import static it.pagopa.selfcare.onboarding.parity.DownstreamStub.MS_DOCUMENT;
import static it.pagopa.selfcare.onboarding.parity.DownstreamStub.MS_IAM;
import static it.pagopa.selfcare.onboarding.parity.DownstreamStub.MS_ONBOARDING;
import static it.pagopa.selfcare.onboarding.parity.DownstreamStub.MS_PRODUCT;

import it.pagopa.selfcare.onboarding.parity.DownstreamStub.Reply;
import it.pagopa.selfcare.onboarding.parity.ParityJwt.User;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** /v2/tokens/** : documents, approvals, authorization (IAM + requester fallback) and uploads. */
final class TokenScenarios {

  private static final String G = "tokens";
  private static final String WUI = "ms-onboarding GET /v1/onboarding/ob1/withUserInfo";
  private static final String BASE = "/v2/tokens/" + Fx.OB;

  private TokenScenarios() {}

  private static String iam(User user, String permission) {
    return "ms-iam GET /iam/users/" + user.uid + "/permissions/Selc:" + permission;
  }

  private static void documentStubs(DownstreamStub stub) {
    stub.on(MS_DOCUMENT, "GET", "/v1/document-content/ob1/contract", Fx.document("contract.pdf"));
    stub.on(
        MS_DOCUMENT, "GET", "/v1/document-content/ob1/contract-signed", Fx.document("signed.pdf"));
    stub.on(MS_DOCUMENT, "GET", "/v1/document-content/ob1/attachment", Fx.document("att.pdf"));
  }

  static List<Scenario> all() {
    List<Scenario> s = new ArrayList<>();
    retrieve(s);
    documents(s);
    download(s);
    approvals(s);
    uploads(s);
    templates(s);
    return s;
  }

  private static void retrieve(List<Scenario> s) {
    s.add(
        Scenario.api(G, "retrieve-as-admin", BASE)
            .stub(Fx::onboardingExists)
            .stub(Fx::iamAdminOnly)
            .expect(
                c ->
                    c.status(200)
                        .contentType("application/json")
                        .json("/status", "PENDING")
                        .json("/productId", Fx.PRODUCT)
                        .jsonPresent("/institutionInfo")
                        .jsonPresent("/manager")
                        .exactCalls(WUI, iam(User.ADMIN, "ViewAccountPage"), WUI)
                        .call("ms-iam", "GET", "/iam/users/" + User.ADMIN.uid + "/permissions/Selc:ViewAccountPage")
                        .query("productId", Fx.PRODUCT)));
    s.add(
        Scenario.api(G, "retrieve-propagates-identity", BASE)
            .stub(Fx::onboardingExists)
            .stub(Fx::iamAdminOnly)
            .expect(c -> c.status(200).propagatesIdentity()));
    s.add(
        Scenario.api(G, "retrieve-requester-allowed-by-onboarding-user", BASE)
            .as(User.REQUESTER)
            .stub(st -> Fx.onboardingExists(st, User.REQUESTER.uid))
            .stub(Fx::iamAdminOnly)
            .expect(c -> c.status(200).json("/status", "PENDING")));
    s.add(
        Scenario.api(G, "retrieve-denied-for-unrelated-user", BASE)
            .as(User.NO_PERMISSION)
            .stub(Fx::onboardingExists)
            .stub(Fx::iamAdminOnly)
            .expect(
                c ->
                    c.problem(403, "Access Denied")
                        .exactCalls(WUI, iam(User.NO_PERMISSION, "ViewAccountPage"))));
    s.add(
        Scenario.api(G, "retrieve-onboarding-not-found", BASE)
            .stub(st -> st.on(MS_ONBOARDING, "GET", "/v1/onboarding/ob1/withUserInfo", Reply.status(404)))
            .stub(Fx::iamAllowAll)
            .expect(c -> c.status(404).contentType("application/problem+json").callCount(MS_IAM, 0)));
    s.add(
        Scenario.api(G, "retrieve-iam-failure-is-bad-gateway", BASE)
            .stub(Fx::onboardingExists)
            .stub(st -> st.on(MS_IAM, "GET", "/iam/users/.*", Reply.status(500)))
            .expect(c -> c.problem(502, null)));
    s.add(
        Scenario.api(G, "verify-returns-onboarding", "POST", "/v2/tokens/ob1/verify")
            .stub(
                st ->
                    st.on(
                        MS_ONBOARDING,
                        "GET",
                        "/v1/onboarding/ob1",
                        Reply.json(200, Fx.onboarding(Fx.OB))))
            .expect(
                c ->
                    c.status(200)
                        .contentType("application/json")
                        .exactCalls("ms-onboarding GET /v1/onboarding/ob1")));
    s.add(
        Scenario.api(G, "verify-not-found", "POST", "/v2/tokens/ob1/verify")
            .stub(st -> st.on(MS_ONBOARDING, "GET", "/v1/onboarding/ob1", Reply.status(404)))
            .expect(c -> c.status(404).contentType("application/problem+json")));
  }

  private static void documents(List<Scenario> s) {
    // contract
    s.add(
        Scenario.api(G, "contract-as-admin", BASE + "/contract")
            .stub(Fx::onboardingExists)
            .stub(Fx::iamAdminOnly)
            .stub(TokenScenarios::documentStubs)
            .expect(
                c ->
                    c.status(200)
                        .contentType("application/octet-stream")
                        .header("Content-Disposition", "attachment; filename=contract.pdf")
                        .header("Access-Control-Expose-Headers", "Content-Disposition")
                        .bodyBytes(Fx.PDF)
                        .exactCalls(
                            WUI,
                            iam(User.ADMIN, "ViewAccountDocuments"),
                            "ms-document GET /v1/document-content/ob1/contract")
                        .propagatesIdentity()));
    s.add(
        Scenario.api(G, "contract-requester-fallback", BASE + "/contract")
            .as(User.REQUESTER)
            .stub(st -> Fx.onboardingExists(st, User.REQUESTER.uid))
            .stub(Fx::iamAdminOnly)
            .stub(TokenScenarios::documentStubs)
            .expect(
                c ->
                    c.status(200)
                        .header("Content-Disposition", "attachment; filename=contract.pdf")
                        .bodyBytes(Fx.PDF)));
    s.add(
        Scenario.api(G, "contract-denied-no-document-call", BASE + "/contract")
            .as(User.NO_PERMISSION)
            .stub(Fx::onboardingExists)
            .stub(Fx::iamAdminOnly)
            .stub(TokenScenarios::documentStubs)
            .expect(c -> c.problem(403, "Access Denied").callCount(MS_DOCUMENT, 0)));
    s.add(
        Scenario.api(G, "contract-document-not-found", BASE + "/contract")
            .stub(Fx::onboardingExists)
            .stub(Fx::iamAdminOnly)
            .stub(st -> st.on(MS_DOCUMENT, "GET", "/v1/document-content/ob1/contract", Reply.status(404)))
            .expect(c -> c.status(404).contentType("application/problem+json")));

    // available documents
    String available = "{\"attachments\":[\"a.pdf\",\"b.pdf\"],\"contractFilename\":\"contract.pdf\"}";
    s.add(
        Scenario.api(G, "available-documents-as-admin", BASE + "/available-documents")
            .stub(Fx::onboardingExists)
            .stub(Fx::iamAdminOnly)
            .stub(
                st ->
                    st.on(
                        MS_DOCUMENT,
                        "GET",
                        "/v1/documents/ob1/available-documents",
                        Reply.json(200, available)))
            .expect(
                c ->
                    c.status(200)
                        .contentType("application/json")
                        .json("/contractFilename", "contract.pdf")
                        .jsonSize("/attachments", 2)
                        .json("/attachments/0", "a.pdf")
                        .exactCalls(
                            WUI,
                            iam(User.ADMIN, "ViewAccountDocuments"),
                            "ms-document GET /v1/documents/ob1/available-documents")));
    s.add(
        Scenario.api(G, "available-documents-requester-fallback", BASE + "/available-documents")
            .as(User.REQUESTER)
            .stub(st -> Fx.onboardingExists(st, User.REQUESTER.uid))
            .stub(Fx::iamAdminOnly)
            .stub(
                st ->
                    st.on(
                        MS_DOCUMENT,
                        "GET",
                        "/v1/documents/ob1/available-documents",
                        Reply.json(200, available)))
            .expect(c -> c.status(200).json("/contractFilename", "contract.pdf")));
    s.add(
        Scenario.api(G, "available-documents-denied", BASE + "/available-documents")
            .as(User.NO_PERMISSION)
            .stub(Fx::onboardingExists)
            .stub(Fx::iamAdminOnly)
            .expect(c -> c.problem(403, "Access Denied").callCount(MS_DOCUMENT, 0)));

    // attachment: no authorization, document-ms only
    s.add(
        Scenario.api(G, "attachment-download-needs-no-iam", BASE + "/attachment?name=att.pdf")
            .as(User.NO_PERMISSION)
            .stub(TokenScenarios::documentStubs)
            .expect(
                c ->
                    c.status(200)
                        .contentType("application/octet-stream")
                        .header("Content-Disposition", "attachment; filename=att.pdf")
                        .header("Access-Control-Expose-Headers", "Content-Disposition")
                        .bodyBytes(Fx.PDF)
                        .exactCalls("ms-document GET /v1/document-content/ob1/attachment")
                        .call(MS_DOCUMENT, "GET", "/v1/document-content/ob1/attachment")
                        .query("name", "att.pdf")));
    s.add(
        Scenario.api(G, "attachment-download-missing-name", BASE + "/attachment")
            .stub(TokenScenarios::documentStubs)
            .expect(c -> c.status(400).contentType("application/problem+json").totalCalls(0)));
    s.add(
        Scenario.api(G, "attachment-download-not-found", BASE + "/attachment?name=x.pdf")
            .stub(st -> st.on(MS_DOCUMENT, "GET", "/v1/document-content/ob1/attachment", Reply.status(404)))
            .expect(c -> c.status(404).contentType("application/problem+json")));
    for (String method : new String[] {"HEAD", "GET"}) {
      s.add(
          Scenario.api(G, "attachment-status-" + method + "-found", method, BASE + "/attachment/status?name=att.pdf")
              .as(User.NO_PERMISSION)
              .stub(st -> st.on(MS_DOCUMENT, "HEAD", "/v1/documents/ob1/attachment/status", Reply.status(200)))
              .expect(
                  c ->
                      c.status(204)
                          .exactCalls("ms-document HEAD /v1/documents/ob1/attachment/status")
                          .call(MS_DOCUMENT, "HEAD", "/v1/documents/ob1/attachment/status")
                          .query("name", "att.pdf")));
      s.add(
          Scenario.api(G, "attachment-status-" + method + "-absent", method, BASE + "/attachment/status?name=att.pdf")
              .stub(st -> st.on(MS_DOCUMENT, "HEAD", "/v1/documents/ob1/attachment/status", Reply.status(404)))
              .expect(c -> c.status(404)));
      s.add(
          Scenario.api(G, "attachment-status-" + method + "-missing-name", method, BASE + "/attachment/status")
              .expect(c -> c.status(400).totalCalls(0)));
    }
  }

  private static void download(List<Scenario> s) {
    String path = BASE + "/download";
    s.add(
        Scenario.api(G, "download-contract-signed", path + "?type=CONTRACT_SIGNED")
            .stub(Fx::onboardingExists)
            .stub(Fx::iamAdminOnly)
            .stub(TokenScenarios::documentStubs)
            .expect(
                c ->
                    c.status(200)
                        .contentType("application/octet-stream")
                        .header("Content-Disposition", "attachment; filename=signed.pdf")
                        .header("Access-Control-Expose-Headers", "Content-Disposition")
                        .bodyBytes(Fx.PDF)
                        .exactCalls(
                            WUI,
                            iam(User.ADMIN, "ViewAccountDocuments"),
                            "ms-document GET /v1/document-content/ob1/contract-signed")));
    s.add(
        Scenario.api(G, "download-attachment-by-name", path + "?type=ATTACHMENT&name=att.pdf")
            .stub(Fx::onboardingExists)
            .stub(Fx::iamAdminOnly)
            .stub(TokenScenarios::documentStubs)
            .expect(
                c ->
                    c.status(200)
                        .contentType("application/octet-stream")
                        .header("Content-Disposition", "attachment; filename=att.pdf")
                        .bodyBytes(Fx.PDF)
                        .exactCalls(
                            WUI,
                            iam(User.ADMIN, "ViewAccountDocuments"),
                            "ms-document GET /v1/document-content/ob1/attachment")
                        .call(MS_DOCUMENT, "GET", "/v1/document-content/ob1/attachment")
                        .query("name", "att.pdf")));
    s.add(
        Scenario.api(G, "download-requester-fallback", path + "?type=CONTRACT_SIGNED")
            .as(User.REQUESTER)
            .stub(st -> Fx.onboardingExists(st, User.REQUESTER.uid))
            .stub(Fx::iamAdminOnly)
            .stub(TokenScenarios::documentStubs)
            .expect(c -> c.status(200).bodyBytes(Fx.PDF)));
    s.add(
        Scenario.api(G, "download-denied", path + "?type=CONTRACT_SIGNED")
            .as(User.NO_PERMISSION)
            .stub(Fx::onboardingExists)
            .stub(Fx::iamAdminOnly)
            .stub(TokenScenarios::documentStubs)
            .expect(c -> c.problem(403, "Access Denied").callCount(MS_DOCUMENT, 0)));
    for (String name : new String[] {"", "&name=", "&name=%20%20"}) {
      s.add(
          Scenario.api(G, "download-attachment-name-required" + (name.isEmpty() ? "-absent" : name), path + "?type=ATTACHMENT" + name)
              .stub(Fx::onboardingExists)
              .stub(Fx::iamAdminOnly)
              .stub(TokenScenarios::documentStubs)
              .expect(
                  c ->
                      c.problem(400, "Query parameter 'name' is required when type=ATTACHMENT")
                          .callCount(MS_DOCUMENT, 0)));
    }
    s.add(
        Scenario.api(G, "download-missing-type", path)
            .stub(Fx::onboardingExists)
            .stub(Fx::iamAdminOnly)
            .expect(c -> c.status(400).contentType("application/problem+json").totalCalls(0)));
    s.add(
        Scenario.api(G, "download-invalid-type", path + "?type=NOT_A_TYPE")
            .stub(Fx::onboardingExists)
            .stub(Fx::iamAdminOnly)
            .expect(c -> c.status(400).contentType("application/problem+json").totalCalls(0)));
    s.add(
        Scenario.api(G, "download-type-is-case-sensitive", path + "?type=contract_signed")
            .stub(Fx::onboardingExists)
            .stub(Fx::iamAdminOnly)
            .expect(c -> c.status(400).totalCalls(0)));
  }

  private static void approvals(List<Scenario> s) {
    s.add(
        Scenario.api(G, "approve-as-admin", "POST", BASE + "/approve")
            .stub(Fx::onboardingExists)
            .stub(Fx::iamAdminOnly)
            .stub(st -> st.on(MS_ONBOARDING, "PUT", "/v1/onboarding/ob1/approve", Reply.status(200)))
            .expect(
                c ->
                    c.status(200)
                        .exactCalls(
                            WUI,
                            iam(User.ADMIN, "ManageAccountPage"),
                            "ms-onboarding PUT /v1/onboarding/ob1/approve")
                        .call(MS_ONBOARDING, "PUT", "/v1/onboarding/ob1/approve")
                        .jsonBody("/userUid", User.ADMIN.uid)
                        .header("content-type", "application/json")));
    s.add(
        Scenario.api(G, "approve-denied-even-for-requester", "POST", BASE + "/approve")
            .as(User.REQUESTER)
            .stub(st -> Fx.onboardingExists(st, User.REQUESTER.uid))
            .stub(Fx::iamAdminOnly)
            .stub(st -> st.on(MS_ONBOARDING, "PUT", "/v1/onboarding/ob1/approve", Reply.status(200)))
            .expect(
                c ->
                    c.problem(403, "Access Denied")
                        .exactCalls(WUI, iam(User.REQUESTER, "ManageAccountPage"))));
    s.add(
        Scenario.api(G, "approve-onboarding-conflict", "POST", BASE + "/approve")
            .stub(Fx::onboardingExists)
            .stub(Fx::iamAdminOnly)
            .stub(st -> st.on(MS_ONBOARDING, "PUT", "/v1/onboarding/ob1/approve", Reply.status(409)))
            .expect(c -> c.status(409).contentType("application/problem+json")));
    s.add(
        Scenario.api(G, "reject-with-reason", "POST", BASE + "/reject")
            .json("{\"reason\":\"no good\"}")
            .stub(Fx::onboardingExists)
            .stub(Fx::iamAdminOnly)
            .stub(st -> st.on(MS_ONBOARDING, "PUT", "/v1/onboarding/ob1/reject", Reply.status(200)))
            .expect(
                c ->
                    c.status(200)
                        .exactCalls(
                            WUI,
                            iam(User.ADMIN, "ManageAccountPage"),
                            "ms-onboarding PUT /v1/onboarding/ob1/reject")
                        .call(MS_ONBOARDING, "PUT", "/v1/onboarding/ob1/reject")
                        .jsonBody("/reasonForReject", "no good")
                        .jsonBody("/userUid", User.ADMIN.uid)));
    s.add(
        Scenario.api(G, "reject-without-body", "POST", BASE + "/reject")
            .stub(Fx::onboardingExists)
            .stub(Fx::iamAdminOnly)
            .expect(c -> c.status(400).contentType("application/problem+json").totalCalls(0)));
    s.add(
        Scenario.api(G, "reject-denied", "POST", BASE + "/reject")
            .as(User.NO_PERMISSION)
            .json("{\"reason\":\"no good\"}")
            .stub(Fx::onboardingExists)
            .stub(Fx::iamAdminOnly)
            .expect(c -> c.problem(403, "Access Denied").callCount(MS_ONBOARDING, 1)));
    s.add(
        Scenario.api(G, "delete-complete-rejects-as-user", "DELETE", BASE + "/complete")
            .stub(st -> st.on(MS_ONBOARDING, "PUT", "/v1/onboarding/ob1/reject", Reply.status(204)))
            .expect(
                c ->
                    c.status(204)
                        .noBody()
                        .exactCalls("ms-onboarding PUT /v1/onboarding/ob1/reject")
                        .call(MS_ONBOARDING, "PUT", "/v1/onboarding/ob1/reject")
                        .jsonBody("/reasonForReject", "REJECTED_BY_USER")
                        .jsonBody("/userUid", User.ADMIN.uid)));
    s.add(
        Scenario.api(G, "delete-complete-not-found", "DELETE", BASE + "/complete")
            .stub(st -> st.on(MS_ONBOARDING, "PUT", "/v1/onboarding/ob1/reject", Reply.status(404)))
            .expect(c -> c.status(404).contentType("application/problem+json")));
  }

  private static void uploads(List<Scenario> s) {
    byte[] pdf = "%PDF-signed".getBytes(StandardCharsets.UTF_8);
    for (String[] file :
        new String[][] {
          {"signed.pdf", "application/pdf"},
          {"signed.p7m", "application/octet-stream"},
          {"signed.pdf.p7m", "application/pkcs7-mime"},
          {"SIGNED.PDF", "application/pdf"},
        }) {
      s.add(
          Scenario.api(G, "complete-accepts-" + file[0], "POST", BASE + "/complete")
              .multipart(Multipart.body().file("contract", file[0], file[1], pdf))
              .stub(st -> st.on(MS_ONBOARDING, "PUT", "/v1/onboarding/ob1/complete", Reply.status(204)))
              .expect(
                  c ->
                      c.status(204)
                          .noBody()
                          .call(MS_ONBOARDING, "PUT", "/v1/onboarding/ob1/complete")
                          .multipartParts("contract")
                          .multipartPart("contract", file[0], null, pdf)));
      s.add(
          Scenario.api(G, "complete-onboarding-users-accepts-" + file[0], "POST", BASE + "/complete-onboarding-users")
              .multipart(Multipart.body().file("contract", file[0], file[1], pdf))
              .stub(
                  st ->
                      st.on(
                          MS_ONBOARDING,
                          "PUT",
                          "/v1/onboarding/ob1/completeOnboardingUsers",
                          Reply.status(204)))
              .expect(
                  c ->
                      c.status(204)
                          .call(MS_ONBOARDING, "PUT", "/v1/onboarding/ob1/completeOnboardingUsers")
                          .multipartPart("contract", file[0], null, pdf)));
    }
    for (String path : new String[] {BASE + "/complete", BASE + "/complete-onboarding-users"}) {
      s.add(
          Scenario.api(G, "upload-rejects-unsupported-format" + path.substring(BASE.length()), "POST", path)
              .multipart(Multipart.body().file("contract", "contract.txt", "text/plain", pdf))
              .expect(
                  c ->
                      c.problem(400, "Formato file non supportato. Ammessi: [.pdf, .p7m]")
                          .totalCalls(0)));
      s.add(
          Scenario.api(G, "upload-rejects-wrong-part-name" + path.substring(BASE.length()), "POST", path)
              .multipart(Multipart.body().file("file", "contract.pdf", "application/pdf", pdf))
              .expect(c -> c.status(400).contentType("application/problem+json").totalCalls(0)));
      s.add(
          Scenario.api(G, "upload-rejects-json-body" + path.substring(BASE.length()), "POST", path)
              .json("{}")
              .expect(c -> c.status(400).contentType("application/problem+json").totalCalls(0)));
    }
    s.add(
        Scenario.api(G, "complete-onboarding-not-found", "POST", BASE + "/complete")
            .multipart(Multipart.body().file("contract", "c.pdf", "application/pdf", pdf))
            .stub(st -> st.on(MS_ONBOARDING, "PUT", "/v1/onboarding/ob1/complete", Reply.status(404)))
            .expect(c -> c.status(404).contentType("application/problem+json")));

    // attachment upload (tenant aware, product-ms required documents)
    String requiredUser =
        "[{\"id\":\"doc1\",\"name\":\"Doc 1\",\"required\":true,\"maxDocumentsRequired\":2,\"storageOrigin\":\"USER\"}]";
    String requiredSystem =
        "[{\"id\":\"doc1\",\"name\":\"Doc 1\",\"required\":true,\"maxDocumentsRequired\":2,\"storageOrigin\":\"SYSTEM\"}]";
    for (String tenant : new String[] {"PNPG", "AR"}) {
      s.add(
          Scenario.api(G, "upload-attachment-user-storage-" + tenant, "POST", BASE + "/attachment?attachmentName=Doc%201")
              .tenant(tenant)
              .multipart(
                  Multipart.body()
                      .field("attachmentId", "doc1")
                      .field("attachmentDescription", "my description")
                      .file("attachment", "doc.pdf", "application/pdf", pdf))
              .stub(
                  st -> {
                    st.on(MS_ONBOARDING, "GET", "/v1/onboarding/ob1", Reply.json(200, Fx.onboarding(Fx.OB)));
                    st.on(
                        MS_PRODUCT,
                        "GET",
                        "/product/prod-io/required-documents",
                        Reply.json(200, requiredUser));
                    st.on(
                        MS_DOCUMENT,
                        "POST",
                        "/v1/document-content/upload-user-attachment",
                        Reply.status(204));
                  })
              .expect(
                  c ->
                      c.status(204)
                          .noBody()
                          .exactCalls(
                              "ms-onboarding GET /v1/onboarding/ob1",
                              "ms-product GET /product/prod-io/required-documents",
                              "ms-document POST /v1/document-content/upload-user-attachment")
                          .propagatesIdentity()
                          .call(MS_PRODUCT, "GET", "/product/prod-io/required-documents")
                          .query("tenantId", tenant)
                          .query("origin", "IPA")
                          .query("institutionType", "PA")
                          .call(MS_DOCUMENT, "POST", "/v1/document-content/upload-user-attachment")
                          .multipartParts("file", "request")
                          .multipartPart("file", "doc.pdf", "application/pdf", pdf)
                          .multipartPart("request", null, "application/json", null)));
    }
    s.add(
        Scenario.api(G, "upload-attachment-system-storage", "POST", BASE + "/attachment?attachmentName=Doc%201")
            .multipart(
                Multipart.body()
                    .field("attachmentId", "doc1")
                    .field("attachmentDescription", "d")
                    .file("attachment", "doc.pdf", "application/pdf", pdf))
            .stub(
                st -> {
                  st.on(MS_ONBOARDING, "GET", "/v1/onboarding/ob1", Reply.json(200, Fx.onboarding(Fx.OB)));
                  st.on(
                      MS_PRODUCT,
                      "GET",
                      "/product/prod-io/required-documents",
                      Reply.json(200, requiredSystem));
                  st.on(MS_PRODUCT, "GET", "/product/prod-io/valid", Reply.json(200, withAttachmentContract("Doc 1")));
                  st.on(
                      MS_DOCUMENT,
                      "POST",
                      "/v1/document-content/upload-attachment",
                      Reply.status(204));
                })
            .expect(
                c ->
                    c.status(204)
                        .exactCalls(
                            "ms-onboarding GET /v1/onboarding/ob1",
                            "ms-product GET /product/prod-io/required-documents",
                            "ms-product GET /product/prod-io/valid",
                            "ms-document POST /v1/document-content/upload-attachment")
                        .call(MS_DOCUMENT, "POST", "/v1/document-content/upload-attachment")
                        .multipartParts("file", "request")));
    s.add(
        Scenario.of(G, "upload-attachment-requires-tenant-header", "POST", BASE + "/attachment?attachmentName=Doc%201")
            .as(User.ADMIN)
            .multipart(
                Multipart.body()
                    .field("attachmentId", "doc1")
                    .field("attachmentDescription", "d")
                    .file("attachment", "doc.pdf", "application/pdf", pdf))
            .expect(c -> c.problem(400, "Invalid tenant context").totalCalls(0)));
    s.add(
        Scenario.api(G, "upload-attachment-requires-name", "POST", BASE + "/attachment")
            .multipart(
                Multipart.body()
                    .field("attachmentId", "doc1")
                    .field("attachmentDescription", "d")
                    .file("attachment", "doc.pdf", "application/pdf", pdf))
            .expect(c -> c.status(400).contentType("application/problem+json").totalCalls(0)));
    s.add(
        Scenario.api(G, "upload-attachment-rejects-unsupported-format", "POST", BASE + "/attachment?attachmentName=Doc%201")
            .multipart(
                Multipart.body()
                    .field("attachmentId", "doc1")
                    .field("attachmentDescription", "d")
                    .file("attachment", "doc.txt", "text/plain", pdf))
            .expect(
                c ->
                    c.problem(400, "Formato file non supportato. Ammessi: [.pdf, .p7m]")
                        .totalCalls(0)));
    s.add(
        Scenario.api(G, "upload-attachment-requires-file-part", "POST", BASE + "/attachment?attachmentName=Doc%201")
            .multipart(Multipart.body().field("attachmentId", "doc1").field("attachmentDescription", "d"))
            .expect(c -> c.status(400).contentType("application/problem+json").totalCalls(0)));
  }

  private static String withAttachmentContract(String name) {
    return "{\"productId\":\"prod-io\",\"tenantId\":\"PNPG\",\"title\":\"IO\",\"status\":\"ACTIVE\","
        + "\"features\":{\"enabled\":true},\"contracts\":[{\"onboardingType\":\"INSTITUTION\","
        + "\"institutionType\":\"PA\",\"contractType\":\"ATTACHMENT\",\"name\":\""
        + name
        + "\","
        + "\"path\":\"tpl/path.html\",\"version\":\"v1\",\"enabled\":true}]}";
  }

  private static void templates(List<Scenario> s) {
    String valid = withAttachmentContract("tmpl");
    s.add(
        Scenario.api(G, "template-attachment", BASE + "/template-attachment?attachmentName=tmpl")
            .stub(
                st -> {
                  st.on(MS_ONBOARDING, "GET", "/v1/onboarding/ob1", Reply.json(200, Fx.onboarding(Fx.OB)));
                  st.on(MS_PRODUCT, "GET", "/product/prod-io/valid", Reply.json(200, valid));
                  st.on(MS_DOCUMENT, "GET", "/v1/document-content/ob1/template-attachment", Fx.document("template.pdf"));
                })
            .expect(
                c ->
                    c.status(200)
                        .contentType("application/octet-stream")
                        .header("Content-Disposition", "attachment; filename=template.pdf")
                        .header("Access-Control-Expose-Headers", "Content-Disposition")
                        .bodyBytes(Fx.PDF)
                        .exactCalls(
                            "ms-onboarding GET /v1/onboarding/ob1",
                            "ms-product GET /product/prod-io/valid",
                            "ms-document GET /v1/document-content/ob1/template-attachment")
                        .call(MS_DOCUMENT, "GET", "/v1/document-content/ob1/template-attachment")
                        .query("name", "tmpl")
                        .query("templatePath", "tpl/path.html")
                        .query("productId", Fx.PRODUCT)
                        .query("institutionDescription", "Comune di Test")));
    s.add(
        Scenario.api(G, "template-attachment-unknown-name", BASE + "/template-attachment?attachmentName=other")
            .stub(
                st -> {
                  st.on(MS_ONBOARDING, "GET", "/v1/onboarding/ob1", Reply.json(200, Fx.onboarding(Fx.OB)));
                  st.on(MS_PRODUCT, "GET", "/product/prod-io/valid", Reply.json(200, valid));
                })
            .expect(c -> c.status(404).contentType("application/problem+json").callCount(MS_DOCUMENT, 0)));
    s.add(
        Scenario.api(G, "template-attachment-requires-name", BASE + "/template-attachment")
            .expect(c -> c.status(400).totalCalls(0)));
    s.add(
        Scenario.api(G, "aggregates-csv-as-admin", BASE.replace(Fx.OB, Fx.OB) + "/products/prod-io/aggregates-csv")
            .stub(Fx::onboardingExists)
            .stub(Fx::iamAdminOnly)
            .stub(
                st ->
                    st.on(
                        MS_DOCUMENT,
                        "GET",
                        "/v1/document-content/aggregates-csv/ob1/products/prod-io",
                        Fx.document("aggregates.csv")))
            .expect(
                c ->
                    c.status(200)
                        .contentType("application/octet-stream")
                        .header("Content-Disposition", "attachment; filename=aggregates.csv")
                        .bodyBytes(Fx.PDF)
                        .exactCalls(
                            WUI,
                            iam(User.ADMIN, "ViewAccountDocuments"),
                            "ms-document GET /v1/document-content/aggregates-csv/ob1/products/prod-io")));
    s.add(
        Scenario.api(G, "aggregates-csv-denied", BASE + "/products/prod-io/aggregates-csv")
            .as(User.NO_PERMISSION)
            .stub(Fx::onboardingExists)
            .stub(Fx::iamAdminOnly)
            .expect(c -> c.problem(403, "Access Denied").callCount(MS_DOCUMENT, 0)));
  }
}
