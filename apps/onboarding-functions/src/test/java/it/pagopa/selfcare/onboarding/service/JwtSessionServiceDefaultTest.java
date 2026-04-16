package it.pagopa.selfcare.onboarding.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
import org.openapi.quarkus.user_registry_json.model.UserResource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestProfile(JwtSessionServiceDefaultTest.JwtProfile.class)
class JwtSessionServiceDefaultTest {

    @Inject
    JwtSessionServiceDefault tokenService;

    @RestClient
    @InjectMock
    UserApi userRegistryApi;

    public static class JwtProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            InputStream is = getClass().getClassLoader().getResourceAsStream("certs/PKCS1Key.pem");
            String privateKey = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8)).lines()
                    .collect(Collectors.joining("\n"));
            return Map.of("onboarding-functions.jwt.token.signing-key", privateKey,
                    "onboarding-functions.jwt.token.issuer", "SPID",
                    "onboarding-functions.jwt.token.kid", "kid"
            );
        }
    }

    @Test
    void createJwt() {
        final String userId = "userId";
        UserResource userResource = new UserResource();
        userResource.setFiscalCode("fiscalCode");
        CertifiableFieldResourceOfstring certifiedField = new CertifiableFieldResourceOfstring();
        certifiedField.setValue("name");
        userResource.setName(certifiedField);
        userResource.setFamilyName(certifiedField);
        when(userRegistryApi.findByIdUsingGET(any(), any())).thenReturn(userResource);
        String jwt = tokenService.createJwt(userId);
        assertTrue(Objects.nonNull(jwt));
    }

    @Test
    void createNullJwt() {
        final String userId = "userId";
        UserResource userResource = new UserResource();
        userResource.setFiscalCode("fiscalCode");
        CertifiableFieldResourceOfstring certifiedField = new CertifiableFieldResourceOfstring();
        certifiedField.setValue("name");
        userResource.setName(certifiedField);
        userResource.setFamilyName(certifiedField);
        when(userRegistryApi.findByIdUsingGET(any(), any())).thenThrow(new ResourceNotFoundException("An error occurred", "Code"));
        String jwt = tokenService.createJwt(userId);
        assertTrue(Objects.isNull(jwt));
    }

}
