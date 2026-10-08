package it.pagopa.selfcare.onboarding.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.pagopa.selfcare.onboarding.exception.CustomVerifyException;
import it.pagopa.selfcare.onboarding.exception.DownstreamServiceException;
import it.pagopa.selfcare.onboarding.exception.InternalGatewayErrorException;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.ResourceConflictException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.junit.jupiter.api.Test;

/**
 * Spring retries (resilience4j retryTimeout: 3 attempts, fixed 5 s wait, only transport errors) exactly
 * the connector operations listed here; every other downstream call fails at once.
 */
class RetryPolicyTest {

    private static final String PACKAGE = "it.pagopa.selfcare.onboarding.service.";

    private static Set<String> retriedMethods(String className) throws ClassNotFoundException {
        Class<?> type = Class.forName(PACKAGE + className);
        assertFalse(type.isAnnotationPresent(Retry.class), className + " must not retry every operation");
        return Arrays.stream(type.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Retry.class))
                .map(Method::getName)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static Set<String> names(String... names) {
        return new TreeSet<>(Arrays.asList(names));
    }

    @Test
    void onboardingMsOperationsAreRetried() throws ClassNotFoundException {
        assertEquals(
                names(
                        "onboarding",
                        "onboardingUsers",
                        "onboardingUsersAggregator",
                        "onboardingCompany",
                        "onboardingTokenComplete",
                        "onboardingUsersComplete",
                        "onboardingPending",
                        "approveOnboarding",
                        "rejectOnboarding",
                        "getOnboarding",
                        "getOnboardingWithUserInfo",
                        "onboardingPaAggregation",
                        "checkManager",
                        "triggerOnboardingRequest"),
                retriedMethods("impl.OnboardingServiceImpl"));
    }

    @Test
    void partyRegistryProxyOperationsAreRetried() throws ClassNotFoundException {
        assertEquals(
                names(
                        "matchInstitutionAndUser",
                        "getInstitutionLegalAddress",
                        "getAooById",
                        "getUoById",
                        "getExtById",
                        "getInstitutionProxyById",
                        "findIpaInstitutionByTaxCode",
                        "searchIpaInstitutions"),
                retriedMethods("impl.PartyRegistryProxyService"));
    }

    @Test
    void onlyTheInstitutionsByTaxCodeOperationOfPartyProcessIsRetried() throws ClassNotFoundException {
        assertEquals(names("getInstitutionsByTaxCodeAndSubunitCode"), retriedMethods("impl.PartyService"));
    }

    @Test
    void documentReadsAreRetriedButNotStatusChecksOrUploads() throws ClassNotFoundException {
        assertEquals(
                names(
                        "getContract",
                        "getContractSigned",
                        "getTemplateAttachment",
                        "getAttachment",
                        "getAvailableDocuments",
                        "getAggregatesCsv"),
                retriedMethods("impl.DocumentService"));
    }

    @Test
    void otherDownstreamCallsAreNeverRetried() throws ClassNotFoundException {
        for (String className :
                new String[] {
                    "impl.IamServiceImpl",
                    "impl.ProductServiceImpl",
                    "impl.TokenServiceImpl",
                    "impl.UserServiceImpl",
                    "impl.UserInstitutionServiceImpl",
                    "impl.InstitutionServiceImpl",
                    "impl.UserRegistryService",
                    "ProductService"
                }) {
            assertTrue(retriedMethods(className).isEmpty(), className + " must not retry");
        }
    }

    @Test
    void everyRetryIsThreeAttemptsFiveSecondsApartWithoutJitterOnTransportErrorsOnly()
            throws ClassNotFoundException {
        for (String className :
                new String[] {
                    "impl.OnboardingServiceImpl", "impl.PartyRegistryProxyService", "impl.PartyService", "impl.DocumentService"
                }) {
            for (Method method : Class.forName(PACKAGE + className).getDeclaredMethods()) {
                Retry retry = method.getAnnotation(Retry.class);
                if (retry == null) {
                    continue;
                }
                String where = className + "." + method.getName();
                assertEquals(2, retry.maxRetries(), where + " maxRetries (3 attempts overall)");
                assertEquals(5000, retry.delay(), where + " delay");
                assertEquals(ChronoUnit.MILLIS, retry.delayUnit(), where + " delayUnit");
                assertEquals(0, retry.jitter(), where + " jitter");
                assertEquals(0, retry.abortOn().length, where + " abortOn");
                assertArrayEquals(
                        new Class<?>[] {ProcessingException.class, IOException.class},
                        retry.retryOn(),
                        where + " retryOn");
            }
        }
    }

    @Test
    void downstreamHttpErrorsAreNotRetryable() {
        Class<?>[] retryOn = {ProcessingException.class, IOException.class};
        for (Class<?> failure :
                new Class<?>[] {
                    InternalGatewayErrorException.class,
                    DownstreamServiceException.class,
                    ResourceNotFoundException.class,
                    InvalidRequestException.class,
                    ResourceConflictException.class,
                    CustomVerifyException.class,
                    WebApplicationException.class
                }) {
            for (Class<?> retryable : retryOn) {
                assertFalse(retryable.isAssignableFrom(failure), failure.getSimpleName() + " must not be retried");
            }
        }
        assertTrue(IOException.class.isAssignableFrom(ConnectException.class));
    }
}
