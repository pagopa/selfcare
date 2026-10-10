package it.pagopa.selfcare.onboarding.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.ParseException;
import it.pagopa.selfcare.onboarding.runtime.RuntimeJwt;
import java.security.KeyPairGenerator;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class BffJwtCallerPrincipalFactoryTest {

    @Test
    void concurrentRequestsCanShareOrReplaceTheVerificationContext() throws Exception {
        BffJwtCallerPrincipalFactory factory = new BffJwtCallerPrincipalFactory();
        JWTAuthContextInfo first = trustedContext();
        JWTAuthContextInfo second = trustedContext();
        String token = RuntimeJwt.spidToken("AR");
        var executor = Executors.newFixedThreadPool(8);
        try {
            var requests = new ArrayList<Callable<String>>();
            for (int i = 0; i < 100; i++) {
                JWTAuthContextInfo context = i % 2 == 0 ? first : second;
                requests.add(() -> factory.parse(token, context).getName());
            }
            for (var result : executor.invokeAll(requests, 10, TimeUnit.SECONDS)) {
                assertEquals(RuntimeJwt.UID, result.get());
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void replacingTheContextNeverReusesThePreviousKey() throws Exception {
        BffJwtCallerPrincipalFactory factory = new BffJwtCallerPrincipalFactory();
        JWTAuthContextInfo trusted = trustedContext();
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        JWTAuthContextInfo other = new JWTAuthContextInfo();
        other.setPublicVerificationKey(generator.generateKeyPair().getPublic());
        String token = RuntimeJwt.spidToken("AR");

        assertEquals(RuntimeJwt.UID, factory.parse(token, trusted).getName());
        assertThrows(ParseException.class, () -> factory.parse(token, other));
        assertEquals(RuntimeJwt.UID, factory.parse(token, trusted).getName());
    }

    private static JWTAuthContextInfo trustedContext() {
        JWTAuthContextInfo context = new JWTAuthContextInfo();
        context.setPublicKeyContent(RuntimeJwt.publicKeyPem());
        return context;
    }
}
