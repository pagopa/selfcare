package it.pagopa.selfcare.onboarding.runtime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import it.pagopa.selfcare.onboarding.parity.DownstreamStub;
import it.pagopa.selfcare.onboarding.parity.DownstreamStub.Reply;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * A failure of ms-iam while authorizing a request is reported like any other downstream failure:
 * the REST client of IAM must go through the same error mapping of every other client.
 */
@QuarkusTest
@TestProfile(RuntimeTestProfile.class)
class RuntimeIamErrorsHttpTest {

    private static final String GENERIC_DETAIL = "An error occurred during a downstream service request";
    private static final String PERMISSIONS_PATH = "/runtime-test/permissions/onb-1";

    @InjectStub
    DownstreamStub stub;

    @BeforeEach
    void stubOnboarding() {
        stub.reset();
        stub.on(DownstreamStub.MS_ONBOARDING, "GET", "/v1/onboarding/onb-1/withUserInfo",
                Reply.json(200, "{\"id\":\"onb-1\",\"productId\":\"prod-io\",\"users\":[]}"));
    }

    @ParameterizedTest(name = "IAM answers {0} -> {1}")
    @CsvSource({"400,400", "404,404", "409,409", "401,401", "403,403", "422,422", "429,429", "500,502", "503,502"})
    void iamErrorStatus_followsTheDownstreamErrorFamilies(int iamStatus, int expectedStatus) {
        stub.on(DownstreamStub.MS_IAM, "GET", "/iam/users/.+", Reply.status(iamStatus));

        given().header("Authorization", "Bearer " + RuntimeJwt.spidToken(null)).header("X-Tenant-Id", "PNPG")
                .queryParam("permission", "Selc:ManageAccountPage")
                .get(PERMISSIONS_PATH).then().statusCode(expectedStatus);
    }

    @ParameterizedTest(name = "IAM answers {0} with a body")
    @CsvSource({"401", "403", "422", "429"})
    void iamClientError_hasTheGenericDetail(int iamStatus) {
        stub.on(DownstreamStub.MS_IAM, "GET", "/iam/users/.+", Reply.json(iamStatus, "{\"message\":\"secret\"}"));

        given().header("Authorization", "Bearer " + RuntimeJwt.spidToken(null)).header("X-Tenant-Id", "PNPG")
                .queryParam("permission", "Selc:ManageAccountPage")
                .get(PERMISSIONS_PATH).then().statusCode(iamStatus)
                .body("detail", is(GENERIC_DETAIL));
    }

    @ParameterizedTest(name = "IAM answers {0} with a body")
    @CsvSource({"500", "503"})
    void iamServerError_isABadGatewayWithoutDetail(int iamStatus) {
        stub.on(DownstreamStub.MS_IAM, "GET", "/iam/users/.+", Reply.json(iamStatus, "{\"message\":\"secret\"}"));

        given().header("Authorization", "Bearer " + RuntimeJwt.spidToken(null)).header("X-Tenant-Id", "PNPG")
                .queryParam("permission", "Selc:ManageAccountPage")
                .get(PERMISSIONS_PATH).then().statusCode(502)
                .body("title", is("Bad Gateway"))
                .body("detail", nullValue());
    }
}
