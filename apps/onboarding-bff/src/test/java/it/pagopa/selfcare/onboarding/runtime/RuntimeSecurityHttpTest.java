package it.pagopa.selfcare.onboarding.runtime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.specification.RequestSpecification;
import java.time.Instant;
import java.util.Date;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The security gate over real HTTP: the application verifies tokens signed with a generated key and
 * answers like the Spring BFF (status, problem+json body, public paths, tenant rules).
 */
@QuarkusTest
@TestProfile(RuntimeTestProfile.class)
class RuntimeSecurityHttpTest {

    private static final String PROTECTED_PATH = "/runtime-test/identity";
    private static final String UNAUTHENTICATED_DETAIL = "An Authentication object was not found in the SecurityContext";

    @Test
    void requestWithoutToken_isUnauthorizedWithTheSpringProblem() {
        given().when().get(PROTECTED_PATH).then()
                .statusCode(401)
                .header("Content-Type", startsWith("application/problem+json"))
                .header("WWW-Authenticate", "Bearer realm=\"selfcare\"")
                .body("title", is("Unauthorized"), "status", is(401), "detail", is(UNAUTHENTICATED_DETAIL),
                        "instance", is(PROTECTED_PATH));
    }

    @Test
    void validToken_isAcceptedAndTheUserIdIsTheUidClaim() {
        withTenant(RuntimeJwt.sign(RuntimeJwt.spid(RuntimeJwt.UID).withSubject("not-the-user-id")), "PNPG")
                .get(PROTECTED_PATH).then()
                .statusCode(200)
                .body("uid", is(RuntimeJwt.UID));
    }

    // The Spring BFF (jjwt) checks exp and nbf only when present and requires no iat, aud or uid.
    @ParameterizedTest(name = "{0}")
    @MethodSource("acceptedTokens")
    void tokensTheSpringBffAccepts_areAccepted(String description, String token) {
        withTenant(token, "PNPG").get(PROTECTED_PATH).then().statusCode(200);
    }

    static Stream<Arguments> acceptedTokens() {
        Date future = Date.from(Instant.now().plusSeconds(3600));
        Date past = Date.from(Instant.now().minusSeconds(3600));
        return Stream.of(
                arguments("without exp", RuntimeJwt.sign(RuntimeJwt.claims("SPID", RuntimeJwt.UID))),
                arguments("without iat", RuntimeJwt.sign(RuntimeJwt.spidWithoutIssuedAt(RuntimeJwt.UID))),
                arguments("without exp and iat",
                        RuntimeJwt.sign(JWT.create().withIssuer("SPID").withClaim("uid", RuntimeJwt.UID))),
                arguments("with a future iat", RuntimeJwt.sign(RuntimeJwt.spid(RuntimeJwt.UID).withIssuedAt(future))),
                arguments("with iat after exp",
                        RuntimeJwt.sign(RuntimeJwt.claims("SPID", RuntimeJwt.UID)
                                .withExpiresAt(Date.from(Instant.now().plusSeconds(60))).withIssuedAt(future))),
                arguments("with nbf in the past", RuntimeJwt.sign(RuntimeJwt.spid(RuntimeJwt.UID).withNotBefore(past))),
                arguments("with an audience", RuntimeJwt.sign(RuntimeJwt.spid(RuntimeJwt.UID).withAudience("someone"))),
                arguments("with a kid unknown to the key", RuntimeJwt.sign(RuntimeJwt.spid(RuntimeJwt.UID).withKeyId("rotated"))),
                arguments("signed RS384", RuntimeJwt.signRs384(RuntimeJwt.spid(RuntimeJwt.UID))),
                arguments("signed RS512", RuntimeJwt.signRs512(RuntimeJwt.spid(RuntimeJwt.UID))),
                arguments("signed PS256", RuntimeJwt.signPs256("SPID", RuntimeJwt.UID)));
    }

    @Test
    void tokenWithoutUid_isAcceptedAndTheUserIsUidNotProvided() {
        withTenant(RuntimeJwt.sign(RuntimeJwt.spid(null)), "PNPG").get(PROTECTED_PATH).then()
                .statusCode(200)
                .body("uid", is("uid_not_provided"));
    }

    @Test
    void tokenWithNullUid_isTheSameAsWithoutUid() {
        withTenant(RuntimeJwt.sign(RuntimeJwt.spid(null).withNullClaim("uid")), "PNPG").get(PROTECTED_PATH).then()
                .statusCode(200)
                .body("uid", is("uid_not_provided"));
    }

    @ParameterizedTest(name = "uid [{0}] is kept as it is")
    @ValueSource(strings = {"", " ", "Mixed-Case-Uid"})
    void uidOfTheToken_isNotNormalized(String uid) {
        withTenant(RuntimeJwt.sign(RuntimeJwt.spid(uid)), "PNPG").get(PROTECTED_PATH).then()
                .statusCode(200)
                .body("uid", is(uid));
    }

    @Test
    void pagopaTokenWithoutUid_isAcceptedAndTheUserIsUidNotProvided() {
        given().header("Authorization", "Bearer " + RuntimeJwt.sign(RuntimeJwt.pagopa(null)))
                .get(PROTECTED_PATH).then()
                .statusCode(200)
                .body("uid", is("uid_not_provided"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("identityClaimOfTheWrongType")
    void identityClaimOfTheWrongType_isUnauthorized(String description, JWTCreator.Builder token) {
        withTenant(RuntimeJwt.sign(token), "PNPG").get(PROTECTED_PATH).then()
                .statusCode(401)
                .body("detail", is(UNAUTHENTICATED_DETAIL));
    }

    static Stream<Arguments> identityClaimOfTheWrongType() {
        return Stream.of(
                arguments("numeric uid", RuntimeJwt.spid(null).withClaim("uid", 7)),
                arguments("boolean uid", RuntimeJwt.spid(null).withClaim("uid", true)),
                arguments("array uid", RuntimeJwt.spid(null).withArrayClaim("uid", new String[] {"a"})),
                arguments("numeric email", RuntimeJwt.spid(RuntimeJwt.UID).withClaim("email", 7)),
                arguments("numeric name", RuntimeJwt.spid(RuntimeJwt.UID).withClaim("name", 7)),
                arguments("numeric family_name", RuntimeJwt.spid(RuntimeJwt.UID).withClaim("family_name", 7)),
                arguments("numeric fiscal_number", RuntimeJwt.spid(RuntimeJwt.UID).withClaim("fiscal_number", 7)));
    }

    @Test
    void numericFiscalNumber_isNotReadForPagopaTokens() {
        given().header("Authorization", "Bearer " + RuntimeJwt.sign(RuntimeJwt.pagopa(RuntimeJwt.UID).withClaim("fiscal_number", 7)))
                .get(PROTECTED_PATH).then().statusCode(200);
    }

    @Test
    void tenantContext_isCheckedBeforeTheTypeOfTheIdentityClaims() {
        withTenant(RuntimeJwt.sign(RuntimeJwt.spid(null).withClaim("uid", 7)), "AR").get(PROTECTED_PATH).then()
                .statusCode(400)
                .body("detail", is("Invalid tenant context"));
    }

    @ParameterizedTest(name = "issuer [{0}]")
    @ValueSource(strings = {"UNKNOWN", "", "spid", "kubernetes/serviceaccount"})
    void validSignatureOfAnUnknownIssuer_isAnInternalServerErrorProblem(String issuer) {
        withTenant(RuntimeJwt.sign(RuntimeJwt.claims(issuer, RuntimeJwt.UID)), "PNPG").get(PROTECTED_PATH).then()
                .statusCode(500)
                .header("Content-Type", startsWith("application/problem+json"))
                .body("title", is("Internal Server Error"), "status", is(500), "detail", is("Unknown issuer"),
                        "instance", is(PROTECTED_PATH));
    }

    @Test
    void validSignatureWithoutIssuer_isAnInternalServerErrorProblem() {
        String token = RuntimeJwt.sign(JWT.create().withClaim("uid", RuntimeJwt.UID));

        withTenant(token, "PNPG").get(PROTECTED_PATH).then()
                .statusCode(500)
                .body("status", is(500), "detail", is("Unknown issuer"));
    }

    @Test
    void unknownIssuer_withAnUntrustedSignature_isUnauthorizedNotInternalError() {
        String token = RuntimeJwt.signWithUntrustedKey(RuntimeJwt.claims("UNKNOWN", RuntimeJwt.UID));

        withTenant(token, "PNPG").get(PROTECTED_PATH).then().statusCode(401);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidAuthorizations")
    void invalidCredentials_areUnauthorizedWithTheSpringProblem(String description, String authorization) {
        given().header("Authorization", authorization).header("X-Tenant-Id", "PNPG")
                .when().get(PROTECTED_PATH).then()
                .statusCode(401)
                .header("Content-Type", startsWith("application/problem+json"))
                .header("WWW-Authenticate", "Bearer realm=\"selfcare\"")
                .body("title", is("Unauthorized"), "status", is(401), "detail", is(UNAUTHENTICATED_DETAIL),
                        "instance", is(PROTECTED_PATH));
    }

    static Stream<Arguments> invalidAuthorizations() {
        Date past = Date.from(Instant.now().minusSeconds(3600));
        Date future = Date.from(Instant.now().plusSeconds(3600));
        String valid = RuntimeJwt.spidToken("PNPG");
        return Stream.of(
                arguments("expired token", "Bearer " + RuntimeJwt.sign(RuntimeJwt.claims("SPID", RuntimeJwt.UID).withExpiresAt(past))),
                arguments("token not yet valid (nbf)", "Bearer " + RuntimeJwt.sign(RuntimeJwt.spid(RuntimeJwt.UID).withNotBefore(future))),
                arguments("token signed with an untrusted key", "Bearer " + RuntimeJwt.signWithUntrustedKey(RuntimeJwt.spid(RuntimeJwt.UID))),
                arguments("HS256 keyed with the public key", "Bearer " + RuntimeJwt.signWithThePublicKeyAsSecret(RuntimeJwt.spid(RuntimeJwt.UID))),
                arguments("unsecured token (alg none)", "Bearer " + RuntimeJwt.unsecured(RuntimeJwt.spid(RuntimeJwt.UID))),
                arguments("garbage token", "Bearer not-a-jwt"),
                arguments("empty bearer", "Bearer "),
                arguments("lower case scheme", "bearer " + valid),
                arguments("basic scheme", "Basic dXNlcjpwYXNz"),
                arguments("token without scheme", valid));
    }

    @Test
    void unknownPath_withoutToken_isUnauthorizedLikeAnyProtectedPath() {
        given().when().get("/v1/unknown").then()
                .statusCode(401)
                .body("detail", is(UNAUTHENTICATED_DETAIL), "instance", is("/v1/unknown"));
    }

    @Test
    void unknownPath_withToken_isAClientError() {
        withTenant(RuntimeJwt.spidToken(null), "PNPG")
                .get("/v1/unknown").then()
                .statusCode(allOf(greaterThanOrEqualTo(400), lessThan(500), not(401)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("spidTenantCases")
    void spidToken_tenantMustMatchTheHeader(String description, String tokenTenant, String headerTenant, int expectedStatus) {
        RequestSpecification request = given().header("Authorization", "Bearer " + RuntimeJwt.spidToken(tokenTenant));
        if (headerTenant != null) {
            request = request.header("X-Tenant-Id", headerTenant);
        }
        var response = request.when().get(PROTECTED_PATH).then().statusCode(expectedStatus);
        if (expectedStatus == 400) {
            response.header("Content-Type", startsWith("application/problem+json"))
                    .body("title", is("Bad Request"), "status", is(400), "detail", is("Invalid tenant context"),
                            "instance", is(PROTECTED_PATH));
        }
    }

    static Stream<Arguments> spidTenantCases() {
        return Stream.of(
                arguments("no claim defaults to PNPG", null, "PNPG", 200),
                arguments("no claim, missing header", null, null, 400),
                arguments("no claim, empty header", null, "", 400),
                arguments("no claim, AR header", null, "AR", 400),
                arguments("AR claim and AR header", "AR", "AR", 200),
                arguments("AR claim and PNPG header", "AR", "PNPG", 400),
                arguments("PNPG claim and PNPG header", "PNPG", "PNPG", 200),
                arguments("PNPG claim and AR header", "PNPG", "AR", 400),
                arguments("PNPG claim, missing header", "PNPG", null, 400),
                arguments("tenants are case sensitive (header)", "PNPG", "pnpg", 400),
                arguments("tenants are case sensitive (claim)", "ar", "ar", 400),
                arguments("claim is not trimmed", " AR", "AR", 400),
                arguments("unsupported tenant", "XX", "XX", 400),
                arguments("empty claim", "", "PNPG", 400));
    }

    @Test
    void spidToken_withNonStringTenantClaim_isRejected() {
        String token = RuntimeJwt.sign(RuntimeJwt.spid(RuntimeJwt.UID).withClaim("tenant_id", 7));

        withTenant(token, "PNPG").get(PROTECTED_PATH).then()
                .statusCode(400)
                .body("detail", is("Invalid tenant context"));
    }

    @Test
    void spidToken_withNullTenantClaim_defaultsToPnpg() {
        String token = RuntimeJwt.sign(RuntimeJwt.spid(RuntimeJwt.UID).withNullClaim("tenant_id"));

        withTenant(token, "PNPG").get(PROTECTED_PATH).then().statusCode(200);
        withTenant(token, "AR").get(PROTECTED_PATH).then().statusCode(400);
    }

    @Test
    void spidToken_withRepeatedTenantHeader_usesTheFirstValue() {
        given().header("Authorization", "Bearer " + RuntimeJwt.spidToken(null))
                .header("X-Tenant-Id", "PNPG").header("X-Tenant-Id", "AR")
                .when().get(PROTECTED_PATH).then()
                .statusCode(200);
    }

    @ParameterizedTest(name = "PAGOPA token with tenant header {0}")
    @MethodSource("pagopaTenantHeaders")
    void pagopaToken_isNotCheckedAgainstTheTenantHeader(String headerTenant) {
        String token = RuntimeJwt.sign(RuntimeJwt.pagopa(RuntimeJwt.UID));
        RequestSpecification request = given().header("Authorization", "Bearer " + token);
        if (headerTenant != null) {
            request = request.header("X-Tenant-Id", headerTenant);
        }
        request.when().get(PROTECTED_PATH).then().statusCode(200).body("uid", is(RuntimeJwt.UID));
    }

    static Stream<String> pagopaTenantHeaders() {
        return Stream.of(null, "AR", "PNPG", "anything");
    }

    @ParameterizedTest(name = "{0} is public")
    @MethodSource("publicPaths")
    void documentationAndHealthPaths_areServedWithoutToken(String path) {
        given().when().get(path).then().statusCode(200);
    }

    static Stream<String> publicPaths() {
        return Stream.of("/actuator/health", "/q/health", "/q/health/live", "/q/health/ready",
                "/v3/api-docs", "/swagger-ui/index.html");
    }

    @Test
    void publicPaths_ignoreInvalidTokens() {
        given().header("Authorization", "Bearer not-a-jwt")
                .when().get("/actuator/health").then().statusCode(200);
    }

    @Test
    void actuatorHealth_reportsUp() {
        given().when().get("/actuator/health").then()
                .statusCode(200)
                .body("status", is("UP"));
    }

    @Test
    void actuatorHealth_subPaths_areNeverAnsweredAsUnauthorized() {
        given().when().get("/actuator/health/liveness").then()
                .statusCode(allOf(greaterThanOrEqualTo(400), lessThan(500), not(401)));
    }

    @Test
    void apiDocs_arePublishedAsJson() {
        given().when().get("/v3/api-docs").then()
                .statusCode(200)
                .header("Content-Type", containsString("json"))
                .body("openapi", startsWith("3."), "paths", not(equalTo(null)));
    }

    private static RequestSpecification withTenant(String token, String tenant) {
        return given().header("Authorization", "Bearer " + token).header("X-Tenant-Id", tenant);
    }
}
