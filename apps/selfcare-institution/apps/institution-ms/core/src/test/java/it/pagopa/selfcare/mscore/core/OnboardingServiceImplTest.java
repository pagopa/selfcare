package it.pagopa.selfcare.mscore.core;

import it.pagopa.selfcare.mscore.api.InstitutionConnector;
import it.pagopa.selfcare.mscore.api.MailNotificationConnector;
import it.pagopa.selfcare.mscore.config.InstitutionSendMailConfig;
import it.pagopa.selfcare.mscore.constant.RelationshipState;
import it.pagopa.selfcare.mscore.core.util.UtilEnumList;
import it.pagopa.selfcare.mscore.exception.InvalidRequestException;
import it.pagopa.selfcare.mscore.exception.ResourceNotFoundException;
import it.pagopa.selfcare.mscore.model.institution.Billing;
import it.pagopa.selfcare.mscore.model.institution.Institution;
import it.pagopa.selfcare.mscore.model.institution.Onboarding;
import it.pagopa.selfcare.mscore.model.onboarding.Token;
import it.pagopa.selfcare.mscore.model.onboarding.VerifyOnboardingFilters;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {OnboardingServiceImpl.class})
@ExtendWith(MockitoExtension.class)
class OnboardingServiceImplTest {

    @Mock
    private OnboardingDao onboardingDao;

    @InjectMocks
    private OnboardingServiceImpl onboardingServiceImpl;

    @Mock
    private InstitutionService institutionService;

    @Mock
    private InstitutionConnector institutionConnector;

    @Mock
    private MailNotificationConnector mailNotificationConnector;

    @Mock
    private InstitutionSendMailConfig institutionSendMailConfig;

    /**
     * Method under test: {@link OnboardingServiceImpl#verifyOnboardingInfo(String, String)}
     */
    @Test
    void testVerifyOnboardingInfo() {
        doNothing().when(institutionService)
                .retrieveInstitutionsWithFilter(any(), any(), any());
        onboardingServiceImpl.verifyOnboardingInfo("42", "42");
        verify(institutionService).retrieveInstitutionsWithFilter(any(), any(),
                any());
    }

    /**
     * Method under test: {@link OnboardingServiceImpl#verifyOnboardingInfoSubunit(String, String, String)}
     */
    @Test
    void shouldNothingWhenVerifyOnboardingInfoSubunit() {
        when(institutionConnector.existsByTaxCodeAndSubunitCodeAndProductAndStatusList(any(), any(), any(), any()))
                .thenReturn(true);
        onboardingServiceImpl.verifyOnboardingInfoSubunit("42", "42", "example");
        verify(institutionConnector).existsByTaxCodeAndSubunitCodeAndProductAndStatusList(any(), any(), any(), any());
    }

    @Test
    void VerifyOnboardingInfoSubunitResourceNotFound() {
        when(institutionConnector.existsByTaxCodeAndSubunitCodeAndProductAndStatusList(any(), any(), any(), any()))
                .thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> onboardingServiceImpl.verifyOnboardingInfoSubunit("42", "42", "example"));

    }

    @Test
    void testVerifyOnboardingInfoByFilter() {
        // Arrange
        when(institutionConnector.existsOnboardingByFilters(Mockito.any())).thenReturn(true);

        VerifyOnboardingFilters verifyOnboardingFilters = new VerifyOnboardingFilters("Product", "", "", "Origin", "OriginId", "");
        // Act
        onboardingServiceImpl.verifyOnboardingInfoByFilters(verifyOnboardingFilters);

        // Assert that nothing has changed
        verify(institutionConnector).existsOnboardingByFilters(Mockito.any());
    }

    @Test
    void testVerifyOnboardingInfoByFilterNotFound() {
        // Arrange
        when(institutionConnector.existsOnboardingByFilters(Mockito.any())).thenReturn(false);

        VerifyOnboardingFilters verifyOnboardingFilters = new VerifyOnboardingFilters("Product", "", "", "Origin", "OriginId", "");
        // Act
        Assertions.assertThrows(ResourceNotFoundException.class, () -> onboardingServiceImpl.verifyOnboardingInfoByFilters(verifyOnboardingFilters));

        // Assert that nothing has changed
        verify(institutionConnector).existsOnboardingByFilters(Mockito.any());
    }

    /**
     * Method under test: {@link OnboardingServiceImpl#verifyOnboardingInfo(String, String)}
     */
    @Test
    void testVerifyOnboardingInfo5() {
        doThrow(new InvalidRequestException("An error occurred", "Code")).when(institutionService)
                .retrieveInstitutionsWithFilter(any(), any(), any());
        assertThrows(InvalidRequestException.class, () -> onboardingServiceImpl.verifyOnboardingInfo("42", "42"));
        verify(institutionService).retrieveInstitutionsWithFilter(any(), any(),
                any());
    }

    @Test
    void persistOnboarding_whenUserExistsOnRegistry() {

        Onboarding onboarding = dummyOnboarding();
        onboarding.setStatus(UtilEnumList.VALID_RELATIONSHIP_STATES.get(0));
        Institution institution = new Institution();
        institution.setId("institutionId");
        institution.setOnboarding(List.of(onboarding, dummyOnboarding()));

        when(mailNotificationConnector.addMailNotification(any(), any(), any(), anyInt())).thenReturn(true);
        when(institutionConnector.findById(institution.getId())).thenReturn(institution);
        when(institutionSendMailConfig.getPecNotificationDisabled()).thenReturn(false);
        when(institutionSendMailConfig.getPecNotificationFrequency()).thenReturn(30);
        when(institutionSendMailConfig.getEpochDatePecNotification()).thenReturn("2024-01-01");

        String institutionId = institution.getId();

        String productId = onboarding.getProductId();
        Onboarding onb = new Onboarding();
        onb.setInstitutionType(InstitutionType.PA);
        
        StringBuilder statusCode = new StringBuilder();

        onboardingServiceImpl.persistOnboarding(institutionId,
                productId, onb, statusCode);
        
        assertEquals(HttpStatus.OK.value(), Integer.parseInt(statusCode.toString()));
    }

    @Test
    void persistOnboarding_whenInstitutionTypeIsPT() {

        Onboarding onboarding = dummyOnboarding();
        onboarding.setStatus(UtilEnumList.VALID_RELATIONSHIP_STATES.get(0));
        onboarding.setInstitutionType(InstitutionType.PT);
        onboarding.setOrigin("SELC");
        onboarding.setOriginId("123");
        Institution institution = new Institution();
        institution.setId("institutionId");
        institution.setOnboarding(List.of(onboarding, dummyOnboarding()));

        when(institutionConnector.findById(institution.getId())).thenReturn(institution);

        String institutionId = institution.getId();
        String productId = onboarding.getProductId();
        Onboarding onb = new Onboarding();
        onb.setInstitutionType(InstitutionType.PT);
        StringBuilder statusCode = new StringBuilder();

        onboardingServiceImpl.persistOnboarding(institutionId, productId, onb, statusCode);

        verify(mailNotificationConnector, never()).addMailNotification(any(), any(), any(), anyInt());
        assertEquals(HttpStatus.OK.value(), Integer.parseInt(statusCode.toString()));
    }

    @Test
    void persistOnboarding_whenPecNotificationIsDisabled() {

        Onboarding onboarding = dummyOnboarding();
        onboarding.setStatus(UtilEnumList.VALID_RELATIONSHIP_STATES.get(0));
        onboarding.setInstitutionType(InstitutionType.PG);
        onboarding.setOriginId("123x");
        onboarding.setOrigin("012341234");
        Institution institution = new Institution();
        institution.setId("institutionId");
        institution.setOnboarding(List.of(onboarding, dummyOnboarding()));

        when(institutionConnector.findById(institution.getId())).thenReturn(institution);
        when(institutionSendMailConfig.getPecNotificationDisabled()).thenReturn(true);

        String institutionId = institution.getId();

        String productId = onboarding.getProductId();
        Onboarding onb = new Onboarding();
        onb.setInstitutionType(InstitutionType.PG);

        StringBuilder statusCode = new StringBuilder();

        onboardingServiceImpl.persistOnboarding(institutionId,
                productId, onb, statusCode);

        verify(mailNotificationConnector, never()).addMailNotification(any(), any(), any(), anyInt());

        assertEquals(HttpStatus.OK.value(), Integer.parseInt(statusCode.toString()));
    }

    @Test
    void persistOnboarding_whenIsAlreadyOnboardingWithAnotherInstitutionType() {

        Onboarding onboarding = dummyOnboarding();
        onboarding.setStatus(UtilEnumList.VALID_RELATIONSHIP_STATES.get(0));
        onboarding.setOrigin("SELC");
        onboarding.setOriginId("123");
        Institution institution = new Institution();
        institution.setId("institutionId");
        institution.setOnboarding(List.of(onboarding, dummyOnboarding()));

        when(institutionConnector.findById(institution.getId())).thenReturn(institution);

        String institutionId = institution.getId();
        String productId = onboarding.getProductId();
        Onboarding onb = new Onboarding();
        onb.setInstitutionType(InstitutionType.PT);
        StringBuilder statusCode = new StringBuilder();

        assertThrows(IllegalStateException.class,
                () -> onboardingServiceImpl.persistOnboarding(institutionId, productId, onb, statusCode));

        verify(mailNotificationConnector, never()).addMailNotification(any(), any(), any(), anyInt());
    }


    /**
     * Method under test: {@link OnboardingServiceImpl#persistOnboarding(String, String, Onboarding, StringBuilder)}
     */
    @Test
    void persistOnboarding_shouldRollback() {

        String pricingPlan = "pricingPlan";
        String productId = "productId";
        Onboarding onboarding = dummyOnboarding();
        onboarding.setStatus(UtilEnumList.VALID_RELATIONSHIP_STATES.get(0));

        Onboarding onboardingToPersist = new Onboarding();
        onboardingToPersist.setPricingPlan(pricingPlan);
        onboardingToPersist.setProductId(productId);
        onboardingToPersist.setBilling(new Billing());

        Institution institution = new Institution();
        institution.setId("institutionId");
        institution.setOnboarding(List.of(onboarding));


        when(institutionConnector.findById(institution.getId())).thenReturn(institution);
        when(institutionConnector.findAndAddOnboarding(any(), any())).thenThrow(new RuntimeException());
        String institutionId = institution.getId();

        Assertions.assertThrows(InvalidRequestException.class, () -> onboardingServiceImpl.persistOnboarding(
        		institutionId, productId, onboardingToPersist, new StringBuilder()));

        verify(onboardingDao, times(1))
                .rollbackPersistOnboarding(any(), any());
    }

    /**
     * Method under test: {@link OnboardingServiceImpl#persistOnboarding(String, String, Onboarding, StringBuilder)}
     */
    @Test
    void persistOnboarding_whenUserNotExistsOnRegistry() {

        String pricingPlan = "pricingPlan";
        String productId = "productId";
        Billing billing = new Billing();
        billing.setVatNumber("vatNumber");
        billing.setPublicServices(false);
        billing.setRecipientCode("recipientCode");
        billing.setTaxCodeInvoicing("taxCodeInvoicing");
        Onboarding onboarding = dummyOnboarding();
        onboarding.setProductId(productId);
        onboarding.setInstitutionType(InstitutionType.PA);
        onboarding.setStatus(UtilEnumList.VALID_RELATIONSHIP_STATES.get(0));

        Onboarding onboardingToPersist = new Onboarding();
        onboardingToPersist.setPricingPlan(pricingPlan);
        onboardingToPersist.setProductId(productId);
        onboardingToPersist.setBilling(billing);
        onboardingToPersist.setIsAggregator(true);
        onboardingToPersist.setCreatedAt(OffsetDateTime.of(
                2025, 3, 13, 0, 0, 0, 0, ZoneOffset.UTC));
        onboardingToPersist.setInstitutionType(InstitutionType.PA);
        onboardingToPersist.setOrigin("IPA");
        onboardingToPersist.setOriginId("123x");

        Institution firstInstitution = new Institution();
        firstInstitution.setId("institutionId");

        Institution institution = new Institution();
        institution.setId("institutionId");
        institution.setOnboarding(List.of(onboarding));
        institution.setDigitalAddress("test@junit.pagopa");

        Token token = new Token();
        token.setId(onboarding.getTokenId());
        token.setInstitutionId("institutionId");
        token.setProductId(productId);
        token.setCreatedAt(onboarding.getCreatedAt());
        token.setUpdatedAt(onboarding.getUpdatedAt());
        token.setStatus(onboarding.getStatus());
        token.setContractSigned(onboarding.getContract());

        when(institutionConnector.findById(institution.getId())).thenReturn(firstInstitution);
        when(institutionConnector.findAndAddOnboarding(any(), any())).thenReturn(institution);
        when(institutionSendMailConfig.getPecNotificationDisabled()).thenReturn(false);
        when(institutionSendMailConfig.getPecNotificationFrequency()).thenReturn(30);
        when(institutionSendMailConfig.getEpochDatePecNotification()).thenReturn("2024-01-01");

        StringBuilder statusCode = new StringBuilder();

        onboardingServiceImpl.persistOnboarding(institution.getId(), productId, onboardingToPersist, statusCode);

        ArgumentCaptor<Onboarding> captor = ArgumentCaptor.forClass(Onboarding.class);
        verify(institutionConnector, times(1))
                .findAndAddOnboarding(any(), captor.capture());
        Onboarding actual = captor.getValue();
        assertEquals(billing, actual.getBilling());
        assertEquals(actual.getCreatedAt().getDayOfYear(), LocalDate.of(2025, 3, 13).getDayOfYear());
        assertEquals(InstitutionType.PA, actual.getInstitutionType());
        assertEquals("IPA", actual.getOrigin());
        assertEquals("123x", actual.getOriginId());
        assertEquals(HttpStatus.CREATED.value(), Integer.parseInt(statusCode.toString()));

        verify(mailNotificationConnector, times(1)).addMailNotification("institutionId", "productId", "test@junit.pagopa", 17);
    }

    private Onboarding dummyOnboarding() {
        Onboarding onboarding = new Onboarding();
        onboarding.setBilling(new Billing());
        onboarding.setTokenId("42");
        onboarding.setPricingPlan("C3");
        onboarding.setProductId("42");
        onboarding.setInstitutionType(InstitutionType.PA);
        return onboarding;
    }

    @Test
    void deleteOnboardedInstitution_success() {
        String institutionId = UUID.randomUUID().toString();
        String productId = UUID.randomUUID().toString();

        Onboarding onboarding = new Onboarding();
        onboarding.setProductId(productId);
        onboarding.setStatus(RelationshipState.DELETED);

        when(mailNotificationConnector.removeMailNotification(institutionId, productId)).thenReturn(true);

        onboardingServiceImpl.deleteOnboardedInstitution(institutionId, productId);

        verify(institutionConnector, times(1)).findAndDeleteOnboarding(institutionId, productId);
        verify(mailNotificationConnector, times(1)).removeMailNotification(institutionId, productId);
    }

    @Test
    void deleteOnboardedInstitution_fail() {
        String institutionId = UUID.randomUUID().toString();
        String productId = UUID.randomUUID().toString();

        Onboarding onboarding = new Onboarding();
        onboarding.setProductId(productId);
        onboarding.setStatus(RelationshipState.DELETED);

        when(mailNotificationConnector.removeMailNotification(institutionId, productId)).thenReturn(false);

        onboardingServiceImpl.deleteOnboardedInstitution(institutionId, productId);

        verify(institutionConnector, times(1)).findAndDeleteOnboarding(institutionId, productId);
        verify(mailNotificationConnector, times(1)).removeMailNotification(institutionId, productId);
    }

    @Test
    void testCalculateModuleDayOfTheEpoch() {
        OffsetDateTime mockCurrentDate = OffsetDateTime.of(2024, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC); // 31 days after epoch

        int result = onboardingServiceImpl.calculateModuleDayOfTheEpoch("2024-01-01", mockCurrentDate, 30);

        LocalDate epochStart = LocalDate.parse("2024-01-01");
        long daysDiff = ChronoUnit.DAYS.between(epochStart, mockCurrentDate);
        int expected = (int) (daysDiff % 30);

        assertEquals(expected, result);
    }
}

