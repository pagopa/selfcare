package it.pagopa.selfcare.onboarding.runtime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.specification.RequestSpecification;
import it.pagopa.selfcare.onboarding.runtime.RawTransportStub.Failure;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * The wire behaviour of the downstream clients when a connection breaks. The Spring BFF (Feign on
 * {@code HttpURLConnection}) sent a request without a body twice when the connection was lost before
 * the answer began, and sent every other request, failing or not, once: the exact number of calls
 * that reach the downstream is the contract checked here, through the real routes of the BFF.
 */
@QuarkusTest
@TestProfile(TransportTestProfile.class)
class TransportReplayHttpTest {

    private static final String GENERIC_DETAIL = "An error occurred during a downstream service request";
    private static final String PRODUCT = "/v1/product/prod-io";

    @InjectStub
    RawTransportStub stub;

    @BeforeEach
    void reset() {
        stub.reset();
    }

    private static RequestSpecification authenticated() {
        return given().header("Authorization", "Bearer " + RuntimeJwt.spidToken(null)).header("X-Tenant-Id", "PNPG");
    }

    private List<RawTransportStub.Hit> productHits() {
        return stub.hits("ms-product");
    }

    @ParameterizedTest(name = "a GET whose connection is {0} before the answer is sent twice, then fails")
    @EnumSource(value = Failure.class, names = {"CLOSE", "RESET"})
    void aDroppedGet_isSentTwiceAndFails(Failure drop) {
        stub.failAlways("ms-product", drop);

        authenticated().get(PRODUCT).then().statusCode(500).body("detail", is(GENERIC_DETAIL));

        assertEquals(2, productHits().size());
        assertEquals(List.of("GET", "GET"), productHits().stream().map(RawTransportStub.Hit::method).toList());
        assertEquals(2, productHits().stream().map(RawTransportStub.Hit::connection).distinct().count(),
                "the second request travels on a new connection");
    }

    @ParameterizedTest(name = "a GET whose connection is {0} once is served by the second request")
    @EnumSource(value = Failure.class, names = {"CLOSE", "RESET"})
    void aGetDroppedOnce_succeedsWithTheSecondRequest(Failure drop) {
        stub.failNext("ms-product", drop, 1);

        authenticated().get(PRODUCT).then().statusCode(200);

        assertEquals(2, productHits().size());
    }

    @Test
    void aGetThatIsNotDropped_isSentOnce() {
        authenticated().get(PRODUCT).then().statusCode(200);

        assertEquals(1, productHits().size());
    }

    @Test
    void aDroppedHead_isSentTwice() {
        stub.failAlways("ms-document", Failure.CLOSE);

        authenticated().head("/v2/tokens/ob1/attachment/status?name=a.pdf").then().statusCode(500);

        assertEquals(List.of("HEAD", "HEAD"),
                stub.hits("ms-document").stream().map(RawTransportStub.Hit::method).toList());
    }

    @Test
    void aPutWithoutBodyDroppedOnce_isSentTwiceAtOnce() {
        stub.failNext("ms-onboarding", Failure.CLOSE, 1);

        authenticated().put("/v2/institutions/ob1").then().statusCode(204);

        List<RawTransportStub.Hit> hits = stub.hits("ms-onboarding");
        assertEquals(List.of("PUT", "PUT"), hits.stream().map(RawTransportStub.Hit::method).toList());
        long pause = hits.get(1).at() - hits.get(0).at();
        assertTrue(pause < 1_000, "the second request is immediate, not the 5 s retry of the service: " + pause + " ms");
    }

    @Test
    void aDroppedIamPermissionCheck_isSentTwice() {
        stub.failAlways("ms-iam", Failure.CLOSE);

        authenticated().queryParam("permission", "Selc:ManageAccountPage")
                .get("/runtime-test/permissions/onb-1").then().statusCode(500).body("detail", is(GENERIC_DETAIL));

        assertEquals(List.of("GET", "GET"), stub.hits("ms-iam").stream().map(RawTransportStub.Hit::method).toList());
    }

    @ParameterizedTest(name = "a POST with a body whose connection is {0} is sent once")
    @EnumSource(value = Failure.class, names = {"CLOSE", "RESET"})
    void aDroppedPostWithBody_isNeverRepeated(Failure drop) {
        stub.failAlways("user-registry", drop);

        authenticated().contentType("application/json").body("{\"taxCode\":\"AAAAAA00A00A000A\"}")
                .post("/v1/users/search-user").then().statusCode(500).body("detail", is(GENERIC_DETAIL));

        assertEquals(1, stub.hits("user-registry").size());
    }

    @ParameterizedTest(name = "a GET answered with a {0} is sent once")
    @EnumSource(value = Failure.class, names = {"TRUNCATED_HEAD", "TRUNCATED_BODY", "GARBAGE"})
    void aBrokenAnswer_isNeverRepeated(Failure failure) {
        stub.failAlways("ms-product", failure);

        authenticated().get(PRODUCT).then().statusCode(500);

        assertEquals(1, productHits().size());
    }
}
