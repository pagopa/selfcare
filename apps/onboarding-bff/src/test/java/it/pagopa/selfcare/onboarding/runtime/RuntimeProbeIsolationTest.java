package it.pagopa.selfcare.onboarding.runtime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The probe endpoints of the runtime tests must not exist in an application built without enabling them. */
@QuarkusTest
@TestProfile(RuntimeProbeIsolationTest.WithoutProbes.class)
class RuntimeProbeIsolationTest {

    public static class WithoutProbes extends RuntimeTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(RuntimeProbeResource.ENABLED_PROPERTY, "false");
        }
    }

    @Test
    void probeEndpoints_areNotDeployed() {
        given().header("Authorization", "Bearer " + RuntimeJwt.spidToken("PNPG")).header("X-Tenant-Id", "PNPG")
                .when().get("/runtime-test/identity").then()
                .statusCode(allOf(greaterThanOrEqualTo(400), lessThan(500)));
    }
}
