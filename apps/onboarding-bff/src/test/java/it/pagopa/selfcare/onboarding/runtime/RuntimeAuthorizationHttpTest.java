package it.pagopa.selfcare.onboarding.runtime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.specification.RequestSpecification;
import it.pagopa.selfcare.onboarding.parity.DownstreamStub;
import it.pagopa.selfcare.onboarding.parity.DownstreamStub.Call;
import it.pagopa.selfcare.onboarding.parity.DownstreamStub.Reply;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Authorization of a request end to end: signed token, real services and REST clients, controlled
 * ms-onboarding and ms-iam. Checks the user id, the requester fallback and what reaches the downstream services.
 */
@QuarkusTest
@TestProfile(RuntimeTestProfile.class)
class RuntimeAuthorizationHttpTest {

    private static final String ONBOARDING_ID = "onb-1";
    private static final String PRODUCT_ID = "prod-io";
    private static final String VIEW_PAGE = "Selc:ViewAccountPage";
    private static final String VIEW_DOCUMENTS = "Selc:ViewAccountDocuments";
    private static final String MANAGE_PAGE = "Selc:ManageAccountPage";
    private static final String PERMISSIONS_PATH = "/runtime-test/permissions/" + ONBOARDING_ID;

    @InjectStub
    DownstreamStub stub;

    @BeforeEach
    void resetStub() {
        stub.reset();
    }

    @AfterEach
    void downstreamApiHeadersMatchSpring() {
        for (Call call : stub.calls()) {
            assertEquals(List.of(RuntimeTestEnvironment.USER_REGISTRY_API_KEY), call.headers().get("x-api-key"),
                    "configured API key on " + call);
            assertNull(call.header("x-functions-key"), "unexpected Functions key on " + call);
        }
    }

    @Test
    void iamIsAskedWithTheUidClaimOfTheToken() {
        stubOnboarding(PRODUCT_ID);
        stubIam("{\"hasPermission\":true}");
        String token = RuntimeJwt.sign(RuntimeJwt.spid(RuntimeJwt.UID).withSubject("not-the-user-id"));

        request(token, "PNPG").queryParam("permission", MANAGE_PAGE).get(PERMISSIONS_PATH).then()
                .statusCode(200).body("granted", is(true));

        Call iamCall = onlyCall(DownstreamStub.MS_IAM);
        assertEquals("/iam/users/" + RuntimeJwt.UID + "/permissions/" + MANAGE_PAGE, decoded(iamCall.path()));
        assertEquals(List.of(PRODUCT_ID), iamCall.queryParams().get("productId"));
    }

    @Test
    void tokenWithoutUid_iamIsAskedWithUidNotProvided() {
        stubOnboarding(PRODUCT_ID);
        stubIam("{\"hasPermission\":true}");

        request(RuntimeJwt.sign(RuntimeJwt.spid(null)), "PNPG").queryParam("permission", VIEW_PAGE)
                .get(PERMISSIONS_PATH).then().statusCode(200).body("granted", is(true));

        assertEquals("/iam/users/uid_not_provided/permissions/" + VIEW_PAGE, decoded(onlyCall(DownstreamStub.MS_IAM).path()));
    }

    @Test
    void tokenWithoutUid_isMatchedAsUidNotProvidedByTheRequesterFallback() {
        stubOnboarding(PRODUCT_ID, "uid_not_provided");
        stubIam("{\"hasPermission\":false}");

        request(RuntimeJwt.sign(RuntimeJwt.spid(null)), "PNPG").queryParam("permission", VIEW_PAGE)
                .get(PERMISSIONS_PATH).then().statusCode(200).body("granted", is(true));
    }

    @Test
    void tokenWithEmptyUid_iamIsAskedWithAnEmptyUserSegment() {
        stubOnboarding(PRODUCT_ID);
        stub.on(DownstreamStub.MS_IAM, "GET", "/iam/users/.*/permissions/.+", Reply.json(200, "{\"hasPermission\":true}"));

        request(RuntimeJwt.sign(RuntimeJwt.spid("")), "PNPG").queryParam("permission", VIEW_PAGE)
                .get(PERMISSIONS_PATH).then().statusCode(200).body("granted", is(true));

        assertEquals("/iam/users//permissions/" + VIEW_PAGE, decoded(onlyCall(DownstreamStub.MS_IAM).path()));
    }

    @Test
    void iamGranted_doesNotNeedTheRequesterFallback() {
        stubOnboarding(PRODUCT_ID, "someone-else");
        stubIam("{\"hasPermission\":true}");

        request(RuntimeJwt.spidToken(null), "PNPG").queryParam("permission", VIEW_DOCUMENTS)
                .get(PERMISSIONS_PATH).then().statusCode(200).body("granted", is(true));
    }

    @Test
    void viewDocuments_iamDenied_onboardingRequester_isGrantedIgnoringCase() {
        stubOnboarding(PRODUCT_ID, RuntimeJwt.UID.toUpperCase());
        stubIam("{\"hasPermission\":false}");

        request(RuntimeJwt.spidToken(null), "PNPG").queryParam("permission", VIEW_DOCUMENTS)
                .get(PERMISSIONS_PATH).then().statusCode(200).body("granted", is(true));
    }

    @Test
    void viewPage_iamDenied_onboardingRequester_isGranted() {
        stubOnboarding(PRODUCT_ID, "someone-else", RuntimeJwt.UID);
        stubIam("{\"hasPermission\":false}");

        request(RuntimeJwt.spidToken(null), "PNPG").queryParam("permission", VIEW_PAGE)
                .get(PERMISSIONS_PATH).then().statusCode(200).body("granted", is(true));
    }

    @Test
    void viewPermission_iamDenied_notAnOnboardingUser_isDenied() {
        stubOnboarding(PRODUCT_ID, "someone-else");
        stubIam("{\"hasPermission\":false}");

        request(RuntimeJwt.spidToken(null), "PNPG").queryParam("permission", VIEW_DOCUMENTS)
                .get(PERMISSIONS_PATH).then().statusCode(200).body("granted", is(false));
    }

    @Test
    void managePermission_iamDenied_isNeverGrantedToOnboardingUsers() {
        stubOnboarding(PRODUCT_ID, RuntimeJwt.UID);
        stubIam("{\"hasPermission\":false}");

        request(RuntimeJwt.spidToken(null), "PNPG").queryParam("permission", MANAGE_PAGE)
                .get(PERMISSIONS_PATH).then().statusCode(200).body("granted", is(false));
    }

    @Test
    void iamAnswerWithoutFlag_isADenial_thatTheRequesterFallbackCanStillCover() {
        stubOnboarding(PRODUCT_ID, RuntimeJwt.UID);
        stubIam("{}");

        request(RuntimeJwt.spidToken(null), "PNPG").queryParam("permission", MANAGE_PAGE)
                .get(PERMISSIONS_PATH).then().statusCode(200).body("granted", is(false));
        request(RuntimeJwt.spidToken(null), "PNPG").queryParam("permission", VIEW_PAGE)
                .get(PERMISSIONS_PATH).then().statusCode(200).body("granted", is(true));
    }

    @Test
    void iamFailure_isNotTurnedIntoAGrant_evenForOnboardingRequester() {
        stubOnboarding(PRODUCT_ID, RuntimeJwt.UID);
        stub.on(DownstreamStub.MS_IAM, "GET", "/iam/users/.+", Reply.status(500));

        request(RuntimeJwt.spidToken(null), "PNPG").queryParam("permission", VIEW_DOCUMENTS)
                .get(PERMISSIONS_PATH).then().statusCode(greaterThanOrEqualTo(400));
    }

    @Test
    void iamForbidden_isNotTurnedIntoAGrant() {
        stubOnboarding(PRODUCT_ID, RuntimeJwt.UID);
        stub.on(DownstreamStub.MS_IAM, "GET", "/iam/users/.+", Reply.status(403));

        request(RuntimeJwt.spidToken(null), "PNPG").queryParam("permission", VIEW_DOCUMENTS)
                .get(PERMISSIONS_PATH).then().statusCode(greaterThanOrEqualTo(400));
    }

    @Test
    void iamTimeout_usesTheIamSpecificTimeout() {
        stubOnboarding(PRODUCT_ID, RuntimeJwt.UID);
        stub.on(DownstreamStub.MS_IAM, "GET", "/iam/users/.+",
                Reply.json(200, "{\"hasPermission\":true}").delay(RuntimeTestEnvironment.IAM_READ_TIMEOUT_MS * 3L));

        long start = System.nanoTime();
        request(RuntimeJwt.spidToken(null), "PNPG").queryParam("permission", MANAGE_PAGE)
                .get(PERMISSIONS_PATH).then().statusCode(greaterThanOrEqualTo(400));
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertTrue(elapsedMs < RuntimeTestEnvironment.IAM_READ_TIMEOUT_MS * 3L, "waited " + elapsedMs + " ms");
    }

    @Test
    void callerCredentialsAndTenantAreForwardedToEveryDownstream() {
        stubOnboarding(PRODUCT_ID);
        stubIam("{\"hasPermission\":true}");
        String token = RuntimeJwt.spidToken("AR");

        request(token, "AR").queryParam("permission", MANAGE_PAGE).get(PERMISSIONS_PATH).then().statusCode(200);

        Stream.of(DownstreamStub.MS_ONBOARDING, DownstreamStub.MS_IAM).forEach(service -> {
            Call call = onlyCall(service);
            assertEquals("Bearer " + token, call.header("authorization"), service);
            assertEquals("AR", call.header("x-tenant-id"), service);
        });
    }

    @Test
    void onlyTheFirstTenantHeaderIsForwarded() {
        stubOnboarding(PRODUCT_ID);
        stubIam("{\"hasPermission\":true}");

        given().header("Authorization", "Bearer " + RuntimeJwt.spidToken(null))
                .header("X-Tenant-Id", "PNPG").header("X-Tenant-Id", "AR")
                .queryParam("permission", MANAGE_PAGE)
                .get(PERMISSIONS_PATH).then().statusCode(200);

        Call call = onlyCall(DownstreamStub.MS_IAM);
        assertEquals(List.of("PNPG"), call.headers().get("x-tenant-id"));
    }

    @Test
    void pagopaToken_withoutTenantHeader_forwardsNoTenant() {
        stubOnboarding(PRODUCT_ID);
        stubIam("{\"hasPermission\":true}");
        String token = RuntimeJwt.sign(RuntimeJwt.pagopa(RuntimeJwt.UID));

        given().header("Authorization", "Bearer " + token).queryParam("permission", MANAGE_PAGE)
                .get(PERMISSIONS_PATH).then().statusCode(200).body("granted", is(true));

        stub.calls().forEach(call -> {
            assertEquals("Bearer " + token, call.header("authorization"), call.service());
            assertNull(call.header("x-tenant-id"), call.service());
        });
    }

    @Test
    void pagopaToken_forwardsTheTenantHeaderAsReceived() {
        stubOnboarding(PRODUCT_ID);
        stubIam("{\"hasPermission\":true}");
        String token = RuntimeJwt.sign(RuntimeJwt.pagopa(RuntimeJwt.UID));

        given().header("Authorization", "Bearer " + token).header("X-Tenant-Id", "AR")
                .queryParam("permission", MANAGE_PAGE)
                .get(PERMISSIONS_PATH).then().statusCode(200);

        assertEquals("AR", onlyCall(DownstreamStub.MS_IAM).header("x-tenant-id"));
    }

    @Test
    void noDownstreamCallIsMadeForRejectedRequests() {
        given().header("Authorization", "Bearer garbage").header("X-Tenant-Id", "PNPG")
                .queryParam("permission", MANAGE_PAGE)
                .get(PERMISSIONS_PATH).then().statusCode(401);
        given().header("Authorization", "Bearer " + RuntimeJwt.spidToken("AR")).header("X-Tenant-Id", "PNPG")
                .queryParam("permission", MANAGE_PAGE)
                .get(PERMISSIONS_PATH).then().statusCode(400);

        assertTrue(stub.calls().isEmpty(), () -> "unexpected calls " + stub.calls());
    }

    private static RequestSpecification request(String token, String tenant) {
        return given().header("Authorization", "Bearer " + token).header("X-Tenant-Id", tenant);
    }

    private void stubIam(String body) {
        stub.on(DownstreamStub.MS_IAM, "GET", "/iam/users/.+/permissions/.+", Reply.json(200, body));
    }

    private void stubOnboarding(String productId, String... userIds) {
        String users = Stream.of(userIds)
                .map(id -> "{\"id\":\"" + id + "\",\"role\":\"MANAGER\"}")
                .collect(Collectors.joining(","));
        stub.on(DownstreamStub.MS_ONBOARDING, "GET", "/v1/onboarding/" + ONBOARDING_ID + "/withUserInfo",
                Reply.json(200, "{\"id\":\"" + ONBOARDING_ID + "\",\"productId\":\"" + productId
                        + "\",\"users\":[" + users + "]}"));
    }

    private Call onlyCall(String service) {
        List<Call> calls = stub.callsTo(service);
        assertEquals(1, calls.size(), () -> service + " calls: " + calls);
        return calls.get(0);
    }

    private static String decoded(String path) {
        return URLDecoder.decode(path, StandardCharsets.UTF_8);
    }
}
