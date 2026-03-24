package it.pagopa.selfcare.document.util;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.document.exception.InvalidRequestException;
import it.pagopa.selfcare.document.model.dto.request.*;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.common.PricingPlan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class PdfMapperDataTest {

    // ---- setUpCommonData ----

    @Test
    void setUpCommonData_shouldMapAllFieldsCorrectly() {
        // Given
        GeographicTaxonomyPdfData geoTax = GeographicTaxonomyPdfData.builder()
                .code("code1")
                .desc("Description 1")
                .build();

        InstitutionPdfData institution = InstitutionPdfData.builder()
                .description("Test Institution")
                .address("Via Roma 1")
                .taxCode("12345678901")
                .zipCode("00100")
                .city("Rome")
                .country("IT")
                .county("RM")
                .digitalAddress("test@pec.it")
                .institutionType(InstitutionType.PA)
                .originId("IPA_CODE_123")
                .geographicTaxonomies(List.of(geoTax))
                .parentDescription("Parent Org")
                .build();

        UserPdfData manager = UserPdfData.builder()
                .id("manager-id")
                .name("Mario")
                .surname("Rossi")
                .taxCode("RSSMRA80A01H501U")
                .email("mario.rossi@test.it")
                .build();

        UserPdfData delegate1 = UserPdfData.builder()
                .id("delegate-1")
                .name("Luigi")
                .surname("Verdi")
                .taxCode("VRDLGU85B02H501V")
                .email("luigi.verdi@test.it")
                .build();

        BillingPdfData billing = BillingPdfData.builder()
                .vatNumber("IT12345678901")
                .taxCodeInvoicing("12345678901")
                .recipientCode("ABC123")
                .build();

        ContractPdfRequest request = ContractPdfRequest.builder()
                .onboardingId("onb-123")
                .productId("prod-pagopa")
                .productName("PagoPA")
                .contractTemplatePath("/templates/contract.html")
                
                .institution(institution)
                .manager(manager)
                .delegates(List.of(delegate1))
                .billing(billing)
                .isAggregator(false)
                .build();

        // When
        Map<String, Object> result = PdfMapperData.setUpCommonData(request);

        // Then
        assertNotNull(result);
        assertEquals("Test Institution", result.get(PdfMapperData.INSTITUTION_NAME));
        assertEquals("Via Roma 1", result.get("address"));
        assertEquals("12345678901", result.get("institutionTaxCode"));
        assertEquals("00100", result.get("zipCode"));
        assertEquals("Rome", result.get("institutionCity"));
        assertEquals("IT", result.get("institutionCountry"));
        assertEquals("RM", result.get("institutionCounty"));
        assertEquals("test@pec.it", result.get("institutionMail"));
        assertEquals("Pubblica Amministrazione", result.get("institutionType"));
        assertEquals("IPA_CODE_123", result.get("originId"));
        assertEquals("", result.get("extCountry"));
        assertEquals(" ente centrale Parent Org", result.get("parentInfo"));

        assertEquals("Mario", result.get("managerName"));
        assertEquals("Rossi", result.get("managerSurname"));
        assertEquals("RSSMRA80A01H501U", result.get("managerTaxCode"));
        assertEquals("mario.rossi@test.it", result.get("managerEmail"));
        assertEquals("_______________", result.get("managerPhone"));

        assertEquals("IT12345678901", result.get("institutionVatNumber"));
        assertEquals("12345678901", result.get("taxCodeInvoicing"));

        assertTrue(result.containsKey("delegates"));
        assertTrue(result.containsKey("delegatesSend"));
        assertTrue(result.containsKey("institutionGeoTaxonomies"));

        @SuppressWarnings("unchecked")
        List<String> geoTaxonomies = (List<String>) result.get("institutionGeoTaxonomies");
        assertEquals(1, geoTaxonomies.size());
        assertEquals("Description 1", geoTaxonomies.get(0));
    }

    @Test
    void setUpCommonData_shouldHandleNullOptionalFields() {
        // Given
        InstitutionPdfData institution = InstitutionPdfData.builder()
                .description("Test Institution")
                .address("Via Roma 1")
                .digitalAddress("test@pec.it")
                .institutionType(InstitutionType.PA)
                .build();

        UserPdfData manager = UserPdfData.builder()
                .id("manager-id")
                .taxCode("RSSMRA80A01H501U")
                .email("mario.rossi@test.it")
                .build();

        ContractPdfRequest request = ContractPdfRequest.builder()
                .onboardingId("onb-123")
                .productId("prod-pagopa")
                .productName("PagoPA")
                .contractTemplatePath("/templates/contract.html")
                
                .institution(institution)
                .manager(manager)
                .build();

        // When
        Map<String, Object> result = PdfMapperData.setUpCommonData(request);

        // Then
        assertEquals("_______________", result.get("institutionTaxCode"));
        assertEquals("", result.get("zipCode"));
        assertEquals("__", result.get("institutionCity"));
        assertEquals("__", result.get("institutionCountry"));
        assertEquals("__", result.get("institutionCounty"));
        assertEquals("_______________", result.get("originId"));
        assertEquals("", result.get("parentInfo"));
        assertEquals("", result.get("managerName"));
        assertEquals("", result.get("managerSurname"));
        assertEquals("_______________", result.get("institutionVatNumber"));
        assertEquals("_______________", result.get("taxCodeInvoicing"));
    }

    @Test
    void setUpCommonData_shouldHandleForeignCountry() {
        // Given
        InstitutionPdfData institution = InstitutionPdfData.builder()
                .description("Foreign Institution")
                .address("Main Street 1")
                .digitalAddress("test@example.com")
                .city("Paris")
                .country("FR")
                .institutionType(InstitutionType.PA)
                .build();

        UserPdfData manager = UserPdfData.builder()
                .id("manager-id")
                .taxCode("TAXCODE")
                .email("test@test.it")
                .build();

        ContractPdfRequest request = ContractPdfRequest.builder()
                .onboardingId("onb-123")
                .productId("prod-pagopa")
                .productName("PagoPA")
                .contractTemplatePath("/templates/contract.html")
                
                .institution(institution)
                .manager(manager)
                .build();

        // When
        Map<String, Object> result = PdfMapperData.setUpCommonData(request);

        // Then
        assertEquals("Paris (FR)", result.get("extCountry"));
    }

    @Test
    void setUpCommonData_shouldHandleOriginIdEqualToTaxCode() {
        // Given
        InstitutionPdfData institution = InstitutionPdfData.builder()
                .description("Test Institution")
                .address("Via Roma 1")
                .digitalAddress("test@pec.it")
                .taxCode("12345678901")
                .originId("12345678901") // Same as taxCode
                .institutionType(InstitutionType.PA)
                .build();

        UserPdfData manager = UserPdfData.builder()
                .id("manager-id")
                .taxCode("RSSMRA80A01H501U")
                .email("test@test.it")
                .build();

        ContractPdfRequest request = ContractPdfRequest.builder()
                .onboardingId("onb-123")
                .productId("prod-pagopa")
                .productName("PagoPA")
                .contractTemplatePath("/templates/contract.html")
                
                .institution(institution)
                .manager(manager)
                .build();

        // When
        Map<String, Object> result = PdfMapperData.setUpCommonData(request);

        // Then
        assertEquals("_______________", result.get("originId"));
    }

    @Test
    void setUpCommonData_shouldThrowInvalidRequestException_whenManagerEmailIsNull() {
        // Given
        InstitutionPdfData institution = InstitutionPdfData.builder()
                .description("Test Institution")
                .address("Via Roma 1")
                .digitalAddress("test@pec.it")
                .institutionType(InstitutionType.PA)
                .build();

        UserPdfData manager = UserPdfData.builder()
                .id("manager-id")
                .taxCode("RSSMRA80A01H501U")
                .email(null) // Missing email
                .build();

        ContractPdfRequest request = ContractPdfRequest.builder()
                .onboardingId("onb-123")
                .productId("prod-pagopa")
                .productName("PagoPA")
                .contractTemplatePath("/templates/contract.html")
                
                .institution(institution)
                .manager(manager)
                .build();

        // When & Then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> PdfMapperData.setUpCommonData(request));
        assertEquals("Manager email not found", exception.getMessage());
        assertEquals("0024", exception.getCode());
    }

    @Test
    void setUpCommonData_shouldHandleEmptyDelegatesList() {
        // Given
        InstitutionPdfData institution = InstitutionPdfData.builder()
                .description("Test Institution")
                .address("Via Roma 1")
                .digitalAddress("test@pec.it")
                .institutionType(InstitutionType.PA)
                .build();

        UserPdfData manager = UserPdfData.builder()
                .id("manager-id")
                .taxCode("RSSMRA80A01H501U")
                .email("test@test.it")
                .build();

        ContractPdfRequest request = ContractPdfRequest.builder()
                .onboardingId("onb-123")
                .productId("prod-pagopa")
                .productName("PagoPA")
                .contractTemplatePath("/templates/contract.html")
                
                .institution(institution)
                .manager(manager)
                .delegates(List.of())
                .build();

        // When
        Map<String, Object> result = PdfMapperData.setUpCommonData(request);

        // Then
        assertEquals("", result.get("delegates"));
        assertEquals("", result.get("delegatesSend"));
    }

    @Test
    void setUpCommonData_shouldHandleEmptyGeographicTaxonomies() {
        // Given
        InstitutionPdfData institution = InstitutionPdfData.builder()
                .description("Test Institution")
                .address("Via Roma 1")
                .digitalAddress("test@pec.it")
                .institutionType(InstitutionType.PA)
                .geographicTaxonomies(List.of())
                .build();

        UserPdfData manager = UserPdfData.builder()
                .id("manager-id")
                .taxCode("RSSMRA80A01H501U")
                .email("test@test.it")
                .build();

        ContractPdfRequest request = ContractPdfRequest.builder()
                .onboardingId("onb-123")
                .productId("prod-pagopa")
                .productName("PagoPA")
                .contractTemplatePath("/templates/contract.html")
                
                .institution(institution)
                .manager(manager)
                .build();

        // When
        Map<String, Object> result = PdfMapperData.setUpCommonData(request);

        // Then
        assertFalse(result.containsKey("institutionGeoTaxonomies"));
    }

    // ---- setUpAttachmentData ----

    @Test
    void setUpAttachmentData_shouldMapAllFieldsCorrectly() {
        // Given
        GpuDataPdfData gpuData = GpuDataPdfData.builder()
                .businessRegisterNumber("BRN123")
                .legalRegisterNumber("LRN456")
                .legalRegisterName("Legal Register")
                .longTermPayments(true)
                .build();

        InstitutionPdfData institution = InstitutionPdfData.builder()
                .description("GPU Institution")
                .taxCode("12345678901")
                .digitalAddress("gpu@pec.it")
                .gpuData(gpuData)
                .build();

        UserPdfData manager = UserPdfData.builder()
                .id("manager-id")
                .name("Mario")
                .surname("Rossi")
                .taxCode("RSSMRA80A01H501U")
                .build();

        AttachmentPdfRequest request = AttachmentPdfRequest.builder()
                .onboardingId("onb-123")
                .productId("prod-pagopa")
                .productName("PagoPA")
                .attachmentTemplatePath("/templates/attachment.html")
                .attachmentName("Allegato A")
                .institution(institution)
                .manager(manager)
                .build();

        // When
        Map<String, Object> result = PdfMapperData.setUpAttachmentData(request);

        // Then
        assertEquals("GPU Institution", result.get(PdfMapperData.INSTITUTION_NAME));
        assertEquals("12345678901", result.get("institutionTaxCode"));
        assertEquals("gpu@pec.it", result.get("institutionMail"));
        assertEquals("Mario", result.get("managerName"));
        assertEquals("Rossi", result.get("managerSurname"));
        assertEquals("BRN123", result.get("businessRegisterNumber"));
        assertEquals("LRN456", result.get("legalRegisterNumber"));
        assertEquals("Legal Register", result.get("legalRegisterName"));
        assertEquals("X", result.get("businessRegisterCheckbox1"));
        assertEquals("", result.get("businessRegisterCheckbox2"));
        assertEquals("X", result.get("publicServicesCheckbox1"));
        assertEquals("", result.get("publicServicesCheckbox2"));
        assertEquals("X", result.get("longTermPaymentsCheckbox1"));
        assertEquals("", result.get("longTermPaymentsCheckbox2"));
    }

    @Test
    void setUpAttachmentData_shouldHandleNullGpuData() {
        // Given
        InstitutionPdfData institution = InstitutionPdfData.builder()
                .description("Institution")
                .digitalAddress("test@pec.it")
                .build();

        UserPdfData manager = UserPdfData.builder()
                .id("manager-id")
                .taxCode("RSSMRA80A01H501U")
                .build();

        AttachmentPdfRequest request = AttachmentPdfRequest.builder()
                .onboardingId("onb-123")
                .productId("prod-pagopa")
                .productName("PagoPA")
                .attachmentTemplatePath("/templates/attachment.html")
                .attachmentName("Allegato A")
                .institution(institution)
                .manager(manager)
                .build();

        // When
        Map<String, Object> result = PdfMapperData.setUpAttachmentData(request);

        // Then
        assertFalse(result.containsKey("businessRegisterNumber"));
        assertFalse(result.containsKey("legalRegisterNumber"));
    }

    @Test
    void setUpAttachmentData_shouldHandleEmptyBusinessRegisterNumber() {
        // Given
        GpuDataPdfData gpuData = GpuDataPdfData.builder()
                .businessRegisterNumber("")
                .legalRegisterNumber("LRN456")
                .legalRegisterName("Legal Register")
                .longTermPayments(false)
                .build();

        InstitutionPdfData institution = InstitutionPdfData.builder()
                .description("Institution")
                .digitalAddress("test@pec.it")
                .gpuData(gpuData)
                .build();

        UserPdfData manager = UserPdfData.builder()
                .id("manager-id")
                .taxCode("RSSMRA80A01H501U")
                .build();

        AttachmentPdfRequest request = AttachmentPdfRequest.builder()
                .onboardingId("onb-123")
                .productId("prod-pagopa")
                .productName("PagoPA")
                .attachmentTemplatePath("/templates/attachment.html")
                .attachmentName("Allegato A")
                .institution(institution)
                .manager(manager)
                .build();

        // When
        Map<String, Object> result = PdfMapperData.setUpAttachmentData(request);

        // Then
        assertEquals("", result.get("businessRegisterCheckbox1"));
        assertEquals("X", result.get("businessRegisterCheckbox2"));
        assertEquals("", result.get("longTermPaymentsCheckbox1"));
        assertEquals("X", result.get("longTermPaymentsCheckbox2"));
    }

    // ---- setupPSPData ----

    @Test
    void setupPSPData_shouldMapAllPSPFields() {
        // Given
        PaymentServiceProviderPdfData psp = PaymentServiceProviderPdfData.builder()
                .legalRegisterNumber("PSP123")
                .legalRegisterName("PSP Register")
                .vatNumberGroup(true)
                .businessRegisterNumber("BRN999")
                .abiCode("12345")
                .build();

        DataProtectionOfficerPdfData dpo = DataProtectionOfficerPdfData.builder()
                .address("Via DPO 1")
                .email("dpo@test.it")
                .pec("dpo@pec.it")
                .build();

        InstitutionPdfData institution = InstitutionPdfData.builder()
                .description("PSP Institution")
                .digitalAddress("psp@pec.it")
                .paymentServiceProvider(psp)
                .dataProtectionOfficer(dpo)
                .build();

        BillingPdfData billing = BillingPdfData.builder()
                .recipientCode("RCP123")
                .build();

        UserPdfData manager = UserPdfData.builder()
                .id("manager-id")
                .taxCode("RSSMRA80A01H501U")
                .email("manager@pec.it")
                .build();

        ContractPdfRequest request = ContractPdfRequest.builder()
                .onboardingId("onb-123")
                .productId("prod-pagopa")
                .productName("PagoPA")
                .contractTemplatePath("/templates/contract.html")
                
                .institution(institution)
                .manager(manager)
                .billing(billing)
                .build();

        Map<String, Object> map = PdfMapperData.setUpCommonData(request);

        // When
        PdfMapperData.setupPSPData(map, manager, request);

        // Then
        assertEquals("PSP123", map.get("legalRegisterNumber"));
        assertEquals("PSP Register", map.get("legalRegisterName"));
        assertEquals("partita iva di gruppo", map.get("vatNumberGroup"));
        assertEquals("X", map.get("vatNumberGroupCheckbox1"));
        assertEquals("", map.get("vatNumberGroupCheckbox2"));
        assertEquals("BRN999", map.get("institutionRegister"));
        assertEquals("12345", map.get("institutionAbi"));
        assertEquals("Via DPO 1", map.get("dataProtectionOfficerAddress"));
        assertEquals("dpo@test.it", map.get("dataProtectionOfficerEmail"));
        assertEquals("dpo@pec.it", map.get("dataProtectionOfficerPec"));
        assertEquals("RCP123", map.get(PdfMapperData.INSTITUTION_RECIPIENT_CODE));
        assertEquals("manager@pec.it", map.get("managerPEC"));
    }

    @Test
    void setupPSPData_shouldHandleVatNumberGroupFalse() {
        // Given
        PaymentServiceProviderPdfData psp = PaymentServiceProviderPdfData.builder()
                .vatNumberGroup(false)
                .build();

        InstitutionPdfData institution = InstitutionPdfData.builder()
                .description("PSP Institution")
                .digitalAddress("psp@pec.it")
                .paymentServiceProvider(psp)
                .build();

        UserPdfData manager = UserPdfData.builder()
                .id("manager-id")
                .taxCode("RSSMRA80A01H501U")
                .email("manager@test.it")
                .build();

        ContractPdfRequest request = ContractPdfRequest.builder()
                .onboardingId("onb-123")
                .productId("prod-pagopa")
                .productName("PagoPA")
                .contractTemplatePath("/templates/contract.html")
                
                .institution(institution)
                .manager(manager)
                .build();

        Map<String, Object> map = PdfMapperData.setUpCommonData(request);

        // When
        PdfMapperData.setupPSPData(map, manager, request);

        // Then
        assertEquals("", map.get("vatNumberGroup"));
        assertEquals("", map.get("vatNumberGroupCheckbox1"));
        assertEquals("X", map.get("vatNumberGroupCheckbox2"));
    }

    @Test
    void setupPSPData_shouldHandleNullPSP() {
        // Given
        InstitutionPdfData institution = InstitutionPdfData.builder()
                .description("Institution")
                .digitalAddress("test@pec.it")
                .build();

        UserPdfData manager = UserPdfData.builder()
                .id("manager-id")
                .taxCode("RSSMRA80A01H501U")
                .email("manager@test.it")
                .build();

        ContractPdfRequest request = ContractPdfRequest.builder()
                .onboardingId("onb-123")
                .productId("prod-pagopa")
                .productName("PagoPA")
                .contractTemplatePath("/templates/contract.html")
                
                .institution(institution)
                .manager(manager)
                .build();

        Map<String, Object> map = PdfMapperData.setUpCommonData(request);

        // When & Then (should not throw)
        assertDoesNotThrow(() -> PdfMapperData.setupPSPData(map, manager, request));
    }

    // ---- setECData ----

    @Test
    void setECData_shouldMapAllECFields() {
        // Given
        InstitutionPdfData institution = InstitutionPdfData.builder()
                .rea("RM-123456")
                .shareCapital("100000")
                .businessRegisterPlace("Roma")
                .build();

        Map<String, Object> map = Map.of();
        Map<String, Object> mutableMap = new java.util.HashMap<>(map);

        // When
        PdfMapperData.setECData(mutableMap, institution);

        // Then
        assertEquals("RM-123456", mutableMap.get(PdfMapperData.INSTITUTION_REA));
        assertEquals("100000", mutableMap.get(PdfMapperData.INSTITUTION_SHARE_CAPITAL));
        assertEquals("Roma", mutableMap.get(PdfMapperData.INSTITUTION_BUSINESS_REGISTER_PLACE));
    }

    @Test
    void setECData_shouldHandleNullFields() {
        // Given
        InstitutionPdfData institution = InstitutionPdfData.builder().build();
        Map<String, Object> map = new java.util.HashMap<>();

        // When
        PdfMapperData.setECData(map, institution);

        // Then
        assertEquals("_______________", map.get(PdfMapperData.INSTITUTION_REA));
        assertEquals("_______________", map.get(PdfMapperData.INSTITUTION_SHARE_CAPITAL));
        assertEquals("_______________", map.get(PdfMapperData.INSTITUTION_BUSINESS_REGISTER_PLACE));
    }

    // ---- setupPRVData ----

    @Test
    void setupPRVData_shouldMapAllPRVFields() {
        // Given
        PaymentServiceProviderPdfData psp = PaymentServiceProviderPdfData.builder()
                .businessRegisterNumber("BRN789")
                .build();

        InstitutionPdfData institution = InstitutionPdfData.builder()
                .description("PRV Institution")
                .digitalAddress("prv@pec.it")
                .rea("RM-999999")
                .shareCapital("50000")
                .businessRegisterPlace("Milano")
                .paymentServiceProvider(psp)
                .build();

        BillingPdfData billing = BillingPdfData.builder()
                .recipientCode("PRV123")
                .build();

        UserPdfData delegate = UserPdfData.builder()
                .id("delegate-id")
                .name("Luigi")
                .surname("Verdi")
                .taxCode("VRDLGU85B02H501V")
                .email("luigi@test.it")
                .build();

        UserPdfData manager = UserPdfData.builder()
                .id("manager-id")
                .taxCode("RSSMRA80A01H501U")
                .email("manager@test.it")
                .build();

        ContractPdfRequest request = ContractPdfRequest.builder()
                .onboardingId("onb-123")
                .productId("prod-pagopa")
                .productName("PagoPA")
                .contractTemplatePath("/templates/contract.html")
                
                .institution(institution)
                .manager(manager)
                .delegates(List.of(delegate))
                .billing(billing)
                .isAggregator(true)
                .build();

        Map<String, Object> map = PdfMapperData.setUpCommonData(request);

        // When
        PdfMapperData.setupPRVData(map, request);

        // Then
        assertTrue(((String) map.get("delegatesPrv")).contains("Luigi"));
        assertTrue(((String) map.get("delegatesPrv")).contains("Verdi"));
        assertEquals("PRV123", map.get(PdfMapperData.INSTITUTION_RECIPIENT_CODE));
        assertEquals("X", map.get("isAggregatorCheckbox"));
        assertEquals("RM-999999", map.get(PdfMapperData.INSTITUTION_REA));
    }

    @Test
    void setupPRVData_shouldHandleIsAggregatorFalse() {
        // Given
        InstitutionPdfData institution = InstitutionPdfData.builder()
                .description("Institution")
                .digitalAddress("test@pec.it")
                .build();

        UserPdfData manager = UserPdfData.builder()
                .id("manager-id")
                .taxCode("RSSMRA80A01H501U")
                .email("manager@test.it")
                .build();

        ContractPdfRequest request = ContractPdfRequest.builder()
                .onboardingId("onb-123")
                .productId("prod-pagopa")
                .productName("PagoPA")
                .contractTemplatePath("/templates/contract.html")
                
                .institution(institution)
                .manager(manager)
                .isAggregator(false)
                .build();

        Map<String, Object> map = PdfMapperData.setUpCommonData(request);

        // When
        PdfMapperData.setupPRVData(map, request);

        // Then
        assertEquals("", map.get("isAggregatorCheckbox"));
    }

    // ---- setupProdIOData ----

    @Test
    void setupProdIOData_shouldMapAllIOFieldsForGSP() {
        // Given
        InstitutionPdfData institution = InstitutionPdfData.builder()
                .description("GSP Institution")
                .digitalAddress("gsp@pec.it")
                .institutionType(InstitutionType.GSP)
                .origin(Origin.IPA)
                .originId("IPA123")
                .rea("RM-111111")
                .shareCapital("200000")
                .businessRegisterPlace("Torino")
                .build();

        BillingPdfData billing = BillingPdfData.builder()
                .recipientCode("IO123")
                .build();

        UserPdfData manager = UserPdfData.builder()
                .id("manager-id")
                .name("Giovanni")
                .surname("Bianchi")
                .taxCode("BNCGNN70A01H501W")
                .email("giovanni@test.it")
                .build();

        ContractPdfRequest request = ContractPdfRequest.builder()
                .onboardingId("onb-123")
                .productId("prod-io")
                .productName("IO")
                .contractTemplatePath("/templates/contract.html")
                
                .institution(institution)
                .manager(manager)
                .billing(billing)
                .pricingPlan("C3")
                .build();

        Map<String, Object> map = PdfMapperData.setUpCommonData(request);

        // When
        PdfMapperData.setupProdIOData(request, map, manager);

        // Then
        assertEquals(InstitutionType.GSP, map.get("institutionTypeCode"));
        assertEquals("GSP Institution", map.get("GPSinstitutionName"));
        assertEquals("Giovanni", map.get("GPSmanagerName"));
        assertEquals("Bianchi", map.get("GPSmanagerSurname"));
        assertEquals("BNCGNN70A01H501W", map.get("GPSmanagerTaxCode"));
        assertEquals("3", map.get(PdfMapperData.PRICING_PLAN_PREMIUM));
        assertTrue(((String) map.get("originIdLabelValue")).contains("${originId}"));
    }

    @Test
    void setupProdIOData_shouldHandleNonGSPInstitution() {
        // Given
        InstitutionPdfData institution = InstitutionPdfData.builder()
                .description("PA Institution")
                .digitalAddress("pa@pec.it")
                .institutionType(InstitutionType.PA)
                .origin(Origin.SELC)
                .build();

        UserPdfData manager = UserPdfData.builder()
                .id("manager-id")
                .name("Mario")
                .surname("Rossi")
                .taxCode("RSSMRA80A01H501U")
                .email("mario@test.it")
                .build();

        ContractPdfRequest request = ContractPdfRequest.builder()
                .onboardingId("onb-123")
                .productId("prod-io")
                .productName("IO")
                .contractTemplatePath("/templates/contract.html")
                
                .institution(institution)
                .manager(manager)
                .build();

        Map<String, Object> map = PdfMapperData.setUpCommonData(request);

        // When
        PdfMapperData.setupProdIOData(request, map, manager);

        // Then
        assertEquals("_______________", map.get("GPSinstitutionName"));
        assertEquals("_______________", map.get("GPSmanagerName"));
        assertEquals("_______________", map.get("GPSmanagerSurname"));
        assertEquals("_______________", map.get("GPSmanagerTaxCode"));
        assertEquals("", map.get("originIdLabelValue"));
    }

    @Test
    void setupProdIOData_shouldHandlePricingPlanFA() {
        // Given
        InstitutionPdfData institution = InstitutionPdfData.builder()
                .description("Institution")
                .digitalAddress("test@pec.it")
                .institutionType(InstitutionType.PA)
                .build();

        UserPdfData manager = UserPdfData.builder()
                .id("manager-id")
                .taxCode("RSSMRA80A01H501U")
                .email("manager@test.it")
                .build();

        ContractPdfRequest request = ContractPdfRequest.builder()
                .onboardingId("onb-123")
                .productId("prod-io")
                .productName("IO")
                .contractTemplatePath("/templates/contract.html")
                
                .institution(institution)
                .manager(manager)
                .pricingPlan(PricingPlan.FA.name())
                .build();

        Map<String, Object> map = PdfMapperData.setUpCommonData(request);

        // When
        PdfMapperData.setupProdIOData(request, map, manager);

        // Then
        assertEquals("X", map.get(PdfMapperData.PRICING_PLAN_FAST_CHECKBOX));
        assertEquals("", map.get(PdfMapperData.PRICING_PLAN_BASE_CHECKBOX));
        assertEquals("", map.get(PdfMapperData.PRICING_PLAN_PREMIUM_CHECKBOX));
        assertEquals(PricingPlan.FA.getValue(), map.get(PdfMapperData.PRICING_PLAN));
    }

    @Test
    void setupProdIOData_shouldHandleBasePricingPlanForProdIO() {
        // Given
        InstitutionPdfData institution = InstitutionPdfData.builder()
                .description("Institution")
                .digitalAddress("test@pec.it")
                .institutionType(InstitutionType.PA)
                .build();

        UserPdfData manager = UserPdfData.builder()
                .id("manager-id")
                .taxCode("RSSMRA80A01H501U")
                .email("manager@test.it")
                .build();

        ContractPdfRequest request = ContractPdfRequest.builder()
                .onboardingId("onb-123")
                .productId("prod-io")
                .productName("IO")
                .contractTemplatePath("/templates/contract.html")
                
                .institution(institution)
                .manager(manager)
                .pricingPlan("BASE")
                .build();

        Map<String, Object> map = PdfMapperData.setUpCommonData(request);

        // When
        PdfMapperData.setupProdIOData(request, map, manager);

        // Then
        assertEquals("", map.get(PdfMapperData.PRICING_PLAN_FAST_CHECKBOX));
        assertEquals("X", map.get(PdfMapperData.PRICING_PLAN_BASE_CHECKBOX));
        assertEquals("", map.get(PdfMapperData.PRICING_PLAN_PREMIUM_CHECKBOX));
        assertEquals(PricingPlan.BASE.getValue(), map.get(PdfMapperData.PRICING_PLAN));
    }

    @Test
    void setupProdIOData_shouldHandleNullPricingPlanForNonIO() {
        // Given
        InstitutionPdfData institution = InstitutionPdfData.builder()
                .description("Institution")
                .digitalAddress("test@pec.it")
                .institutionType(InstitutionType.PA)
                .build();

        UserPdfData manager = UserPdfData.builder()
                .id("manager-id")
                .taxCode("RSSMRA80A01H501U")
                .email("manager@test.it")
                .build();

        ContractPdfRequest request = ContractPdfRequest.builder()
                .onboardingId("onb-123")
                .productId("prod-pagopa")
                .productName("PagoPA")
                .contractTemplatePath("/templates/contract.html")
                
                .institution(institution)
                .manager(manager)
                .pricingPlan(null)
                .build();

        Map<String, Object> map = PdfMapperData.setUpCommonData(request);

        // When
        PdfMapperData.setupProdIOData(request, map, manager);

        // Then
        // When pricingPlan is null, decodePricingPlan sets PREMIUM for non-prod-io
        assertEquals("", map.get(PdfMapperData.PRICING_PLAN_FAST_CHECKBOX));
        assertEquals("", map.get(PdfMapperData.PRICING_PLAN_BASE_CHECKBOX));
        // But addPricingPlan overwrites PREMIUM_CHECKBOX to empty because plan is not C0
        assertEquals("", map.get(PdfMapperData.PRICING_PLAN_PREMIUM_CHECKBOX));
        assertEquals(PricingPlan.PREMIUM.getValue(), map.get(PdfMapperData.PRICING_PLAN));
    }

    @ParameterizedTest
    @ValueSource(strings = {"C1", "C2", "C3", "C4", "C5", "C6", "C7"})
    void setupProdIOData_shouldHandleAllCPlans(String plan) {
        // Given
        InstitutionPdfData institution = InstitutionPdfData.builder()
                .description("Institution")
                .digitalAddress("test@pec.it")
                .institutionType(InstitutionType.PA)
                .build();

        UserPdfData manager = UserPdfData.builder()
                .id("manager-id")
                .taxCode("RSSMRA80A01H501U")
                .email("manager@test.it")
                .build();

        ContractPdfRequest request = ContractPdfRequest.builder()
                .onboardingId("onb-123")
                .productId("prod-io")
                .productName("IO")
                .contractTemplatePath("/templates/contract.html")
                
                .institution(institution)
                .manager(manager)
                .pricingPlan(plan)
                .build();

        Map<String, Object> map = PdfMapperData.setUpCommonData(request);

        // When
        PdfMapperData.setupProdIOData(request, map, manager);

        // Then
        String expectedNumber = plan.replace("C", "");
        assertEquals(expectedNumber, map.get(PdfMapperData.PRICING_PLAN_PREMIUM));
        assertEquals(plan, map.get("pricingPlanPremiumBase"));
    }

    @Test
    void setupProdIOData_shouldHandleC0Plan() {
        // Given
        InstitutionPdfData institution = InstitutionPdfData.builder()
                .description("Institution")
                .digitalAddress("test@pec.it")
                .institutionType(InstitutionType.PA)
                .build();

        UserPdfData manager = UserPdfData.builder()
                .id("manager-id")
                .taxCode("RSSMRA80A01H501U")
                .email("manager@test.it")
                .build();

        ContractPdfRequest request = ContractPdfRequest.builder()
                .onboardingId("onb-123")
                .productId("prod-io")
                .productName("IO")
                .contractTemplatePath("/templates/contract.html")
                
                .institution(institution)
                .manager(manager)
                .pricingPlan("C0")
                .build();

        Map<String, Object> map = PdfMapperData.setUpCommonData(request);

        // When
        PdfMapperData.setupProdIOData(request, map, manager);

        // Then
        assertEquals("X", map.get(PdfMapperData.PRICING_PLAN_PREMIUM_CHECKBOX));
        assertEquals("C0", map.get("pricingPlanPremiumBase"));
    }

    // ---- setupSAProdInteropData ----

    @Test
    void setupSAProdInteropData_shouldSetOriginIdToUnderscoreForSA() {
        // Given
        InstitutionPdfData institution = InstitutionPdfData.builder()
                .description("SA Institution")
                .institutionType(InstitutionType.SA)
                .rea("RM-111111")
                .shareCapital("100000")
                .businessRegisterPlace("Roma")
                .build();

        Map<String, Object> map = new java.util.HashMap<>();

        // When
        PdfMapperData.setupSAProdInteropData(map, institution);

        // Then
        assertEquals("_______________", map.get("originId"));
        assertEquals("RM-111111", map.get(PdfMapperData.INSTITUTION_REA));
    }

    @Test
    void setupSAProdInteropData_shouldNotSetOriginIdForNonSA() {
        // Given
        InstitutionPdfData institution = InstitutionPdfData.builder()
                .description("PA Institution")
                .institutionType(InstitutionType.PA)
                .rea("RM-222222")
                .build();

        Map<String, Object> map = new java.util.HashMap<>();

        // When
        PdfMapperData.setupSAProdInteropData(map, institution);

        // Then
        assertFalse(map.containsKey("originId"));
        assertEquals("RM-222222", map.get(PdfMapperData.INSTITUTION_REA));
    }

    // ---- setupProdPNData ----

    @Test
    void setupProdPNData_shouldMapPNFields() {
        // Given
        PaymentServiceProviderPdfData psp = PaymentServiceProviderPdfData.builder()
                .businessRegisterNumber("PN123")
                .build();

        InstitutionPdfData institution = InstitutionPdfData.builder()
                .description("PN Institution")
                .paymentServiceProvider(psp)
                .build();

        BillingPdfData billing = BillingPdfData.builder()
                .recipientCode("PN456")
                .build();

        Map<String, Object> map = new java.util.HashMap<>();

        // When
        PdfMapperData.setupProdPNData(map, institution, billing);

        // Then
        assertEquals("PN456", map.get(PdfMapperData.INSTITUTION_RECIPIENT_CODE));
        assertTrue(((String) map.get(PdfMapperData.INSTITUTION_REGISTER_LABEL_VALUE)).contains("${number}"));
        assertEquals("PN123", map.get("number"));
    }

    // ---- setupPaymentData ----

    @Test
    void setupPaymentData_shouldMapPaymentFields() {
        // Given
        PaymentPdfData payment = PaymentPdfData.builder()
                .holder("Mario Rossi")
                .iban("IT60X0542811101000000123456")
                .build();

        Map<String, Object> map = new java.util.HashMap<>();

        // When
        PdfMapperData.setupPaymentData(map, payment);

        // Then
        assertEquals("Mario Rossi", map.get("holder"));
        assertEquals("IT60X0542811101000000123456", map.get("holder-iban"));
    }

    @Test
    void setupPaymentData_shouldHandleNullPayment() {
        // Given
        Map<String, Object> map = new java.util.HashMap<>();

        // When & Then
        assertDoesNotThrow(() -> PdfMapperData.setupPaymentData(map, null));
        assertFalse(map.containsKey("holder"));
    }

    @Test
    void setupPaymentData_shouldHandleNullPaymentFields() {
        // Given
        PaymentPdfData payment = PaymentPdfData.builder().build();
        Map<String, Object> map = new java.util.HashMap<>();

        // When
        PdfMapperData.setupPaymentData(map, payment);

        // Then
        assertEquals("", map.get("holder"));
        assertEquals("", map.get("holder-iban"));
    }

    // ---- decodeInstitutionType ----

    @ParameterizedTest
    @EnumSource(InstitutionType.class)
    void setUpCommonData_shouldDecodeAllInstitutionTypes(InstitutionType type) {
        // Given
        InstitutionPdfData institution = InstitutionPdfData.builder()
                .description("Institution")
                .digitalAddress("test@pec.it")
                .institutionType(type)
                .build();

        UserPdfData manager = UserPdfData.builder()
                .id("manager-id")
                .taxCode("RSSMRA80A01H501U")
                .email("test@test.it")
                .build();

        ContractPdfRequest request = ContractPdfRequest.builder()
                .onboardingId("onb-123")
                .productId("prod-pagopa")
                .productName("PagoPA")
                .contractTemplatePath("/templates/contract.html")
                
                .institution(institution)
                .manager(manager)
                .build();

        // When
        Map<String, Object> result = PdfMapperData.setUpCommonData(request);

        // Then
        String decodedType = (String) result.get("institutionType");
        assertNotNull(decodedType);

        switch (type) {
            case PA -> assertEquals("Pubblica Amministrazione", decodedType);
            case GSP -> assertEquals("Gestore di servizi pubblici", decodedType);
            case PT -> assertEquals("Partner tecnologico", decodedType);
            case SCP -> assertEquals("Società a controllo pubblico", decodedType);
            case PSP -> assertEquals("Prestatori Servizi di Pagamento", decodedType);
            default -> assertEquals("", decodedType);
        }
    }

    @Test
    void setUpCommonData_shouldReturnEmptyStringForNullInstitutionType() {
        // Given
        InstitutionPdfData institution = InstitutionPdfData.builder()
                .description("Institution")
                .digitalAddress("test@pec.it")
                .institutionType(null)
                .build();

        UserPdfData manager = UserPdfData.builder()
                .id("manager-id")
                .taxCode("RSSMRA80A01H501U")
                .email("test@test.it")
                .build();

        ContractPdfRequest request = ContractPdfRequest.builder()
                .onboardingId("onb-123")
                .productId("prod-pagopa")
                .productName("PagoPA")
                .contractTemplatePath("/templates/contract.html")
                
                .institution(institution)
                .manager(manager)
                .build();

        // When
        Map<String, Object> result = PdfMapperData.setUpCommonData(request);

        // Then
        assertEquals("", result.get("institutionType"));
    }

    // ---- Aggregates CSV Link ----

    @Test
    void setUpCommonData_shouldAddAggregatesCsvLinkForProdIO() {
        // Given
        InstitutionPdfData institution = InstitutionPdfData.builder()
                .description("Institution")
                .digitalAddress("test@pec.it")
                .institutionType(InstitutionType.PA)
                .build();

        UserPdfData manager = UserPdfData.builder()
                .id("manager-id")
                .taxCode("RSSMRA80A01H501U")
                .email("test@test.it")
                .build();

        ContractPdfRequest request = ContractPdfRequest.builder()
                .onboardingId("onb-123")
                .productId("prod-io")
                .productName("IO")
                .contractTemplatePath("/templates/contract.html")
                
                .institution(institution)
                .manager(manager)
                .isAggregator(true)
                .aggregatesCsvBaseUrl("https://example.com/")
                .build();

        // When
        Map<String, Object> result = PdfMapperData.setUpCommonData(request);

        // Then
        String csvLink = (String) result.get(PdfMapperData.CSV_AGGREGATES_LABEL_VALUE);
        assertTrue(csvLink.contains("https://example.com/onb-123/products/prod-io/aggregates"));
        assertTrue(csvLink.contains("Dati degli Enti Aggregati_IO"));
    }

    @Test
    void setUpCommonData_shouldAddAggregatesCsvLinkForProdPN() {
        // Given
        InstitutionPdfData institution = InstitutionPdfData.builder()
                .description("Institution")
                .digitalAddress("test@pec.it")
                .institutionType(InstitutionType.PA)
                .build();

        UserPdfData manager = UserPdfData.builder()
                .id("manager-id")
                .taxCode("RSSMRA80A01H501U")
                .email("test@test.it")
                .build();

        ContractPdfRequest request = ContractPdfRequest.builder()
                .onboardingId("onb-456")
                .productId("prod-pn")
                .productName("PN")
                .contractTemplatePath("/templates/contract.html")
                
                .institution(institution)
                .manager(manager)
                .isAggregator(true)
                .aggregatesCsvBaseUrl("https://example.com/")
                .build();

        // When
        Map<String, Object> result = PdfMapperData.setUpCommonData(request);

        // Then
        String csvLink = (String) result.get(PdfMapperData.CSV_AGGREGATES_LABEL_VALUE);
        assertTrue(csvLink.contains("https://example.com/onb-456/products/prod-pn/aggregates"));
        assertTrue(csvLink.contains("Dati di Enti Aggregati"));
    }

    @Test
    void setUpCommonData_shouldNotAddAggregatesCsvLink_whenIsAggregatorIsFalse() {
        // Given
        InstitutionPdfData institution = InstitutionPdfData.builder()
                .description("Institution")
                .digitalAddress("test@pec.it")
                .institutionType(InstitutionType.PA)
                .build();

        UserPdfData manager = UserPdfData.builder()
                .id("manager-id")
                .taxCode("RSSMRA80A01H501U")
                .email("test@test.it")
                .build();

        ContractPdfRequest request = ContractPdfRequest.builder()
                .onboardingId("onb-123")
                .productId("prod-io")
                .productName("IO")
                .contractTemplatePath("/templates/contract.html")
                
                .institution(institution)
                .manager(manager)
                .isAggregator(false)
                .aggregatesCsvBaseUrl("https://example.com")
                .build();

        // When
        Map<String, Object> result = PdfMapperData.setUpCommonData(request);

        // Then
        assertEquals("", result.get(PdfMapperData.CSV_AGGREGATES_LABEL_VALUE));
    }

    @Test
    void setUpCommonData_shouldNotAddAggregatesCsvLink_whenBaseUrlIsEmpty() {
        // Given
        InstitutionPdfData institution = InstitutionPdfData.builder()
                .description("Institution")
                .digitalAddress("test@pec.it")
                .institutionType(InstitutionType.PA)
                .build();

        UserPdfData manager = UserPdfData.builder()
                .id("manager-id")
                .taxCode("RSSMRA80A01H501U")
                .email("test@test.it")
                .build();

        ContractPdfRequest request = ContractPdfRequest.builder()
                .onboardingId("onb-123")
                .productId("prod-io")
                .productName("IO")
                .contractTemplatePath("/templates/contract.html")
                
                .institution(institution)
                .manager(manager)
                .isAggregator(true)
                .aggregatesCsvBaseUrl("")
                .build();

        // When
        Map<String, Object> result = PdfMapperData.setUpCommonData(request);

        // Then
        assertEquals("", result.get(PdfMapperData.CSV_AGGREGATES_LABEL_VALUE));
    }

    // ---- Delegates HTML generation ----

    @Test
    void setUpCommonData_shouldGenerateCorrectDelegatesHTML() {
        // Given
        UserPdfData delegate = UserPdfData.builder()
                .id("delegate-id")
                .name("Luigi")
                .surname("Verdi")
                .taxCode("VRDLGU85B02H501V")
                .email("luigi.verdi@test.it")
                .build();

        InstitutionPdfData institution = InstitutionPdfData.builder()
                .description("Institution")
                .digitalAddress("test@pec.it")
                .institutionType(InstitutionType.PA)
                .build();

        UserPdfData manager = UserPdfData.builder()
                .id("manager-id")
                .taxCode("RSSMRA80A01H501U")
                .email("test@test.it")
                .build();

        ContractPdfRequest request = ContractPdfRequest.builder()
                .onboardingId("onb-123")
                .productId("prod-pagopa")
                .productName("PagoPA")
                .contractTemplatePath("/templates/contract.html")
                
                .institution(institution)
                .manager(manager)
                .delegates(List.of(delegate))
                .build();

        // When
        Map<String, Object> result = PdfMapperData.setUpCommonData(request);

        // Then
        String delegates = (String) result.get("delegates");
        assertTrue(delegates.contains("Luigi"));
        assertTrue(delegates.contains("Verdi"));
        assertTrue(delegates.contains("VRDLGU85B02H501V"));
        assertTrue(delegates.contains("luigi.verdi@test.it"));
        assertTrue(delegates.contains("Nome e Cognome:"));
        assertTrue(delegates.contains("Codice Fiscale:"));
        assertTrue(delegates.contains("e-mail:"));
    }

    @Test
    void setupPRVData_shouldGenerateCorrectDelegatesPrvHTML() {
        // Given
        UserPdfData delegate = UserPdfData.builder()
                .id("delegate-id")
                .name("Luigi")
                .surname("Verdi")
                .taxCode("VRDLGU85B02H501V")
                .email("luigi.verdi@test.it")
                .build();

        InstitutionPdfData institution = InstitutionPdfData.builder()
                .description("Institution")
                .digitalAddress("test@pec.it")
                .institutionType(InstitutionType.PA)
                .build();

        UserPdfData manager = UserPdfData.builder()
                .id("manager-id")
                .taxCode("RSSMRA80A01H501U")
                .email("test@test.it")
                .build();

        ContractPdfRequest request = ContractPdfRequest.builder()
                .onboardingId("onb-123")
                .productId("prod-pagopa")
                .productName("PagoPA")
                .contractTemplatePath("/templates/contract.html")
                
                .institution(institution)
                .manager(manager)
                .delegates(List.of(delegate))
                .build();

        Map<String, Object> map = PdfMapperData.setUpCommonData(request);

        // When
        PdfMapperData.setupPRVData(map, request);

        // Then
        String delegatesPrv = (String) map.get("delegatesPrv");
        assertTrue(delegatesPrv.contains("Luigi"));
        assertTrue(delegatesPrv.contains("Verdi"));
        assertTrue(delegatesPrv.contains("VRDLGU85B02H501V"));
        assertTrue(delegatesPrv.contains("luigi.verdi@test.it"));
        assertTrue(delegatesPrv.contains("Cognome:"));
        assertTrue(delegatesPrv.contains("Nome:"));
        assertTrue(delegatesPrv.contains("Posta Elettronica aziendale:"));
        assertTrue(delegatesPrv.contains("<ol"));
        assertTrue(delegatesPrv.contains("</ol>"));
    }

    @Test
    void setUpCommonData_shouldGenerateCorrectDelegatesSendHTML() {
        // Given
        UserPdfData delegate = UserPdfData.builder()
                .id("delegate-id")
                .name("Luigi")
                .surname("Verdi")
                .taxCode("VRDLGU85B02H501V")
                .email("luigi.verdi@test.it")
                .build();

        InstitutionPdfData institution = InstitutionPdfData.builder()
                .description("Institution")
                .digitalAddress("test@pec.it")
                .institutionType(InstitutionType.PA)
                .build();

        UserPdfData manager = UserPdfData.builder()
                .id("manager-id")
                .taxCode("RSSMRA80A01H501U")
                .email("test@test.it")
                .build();

        ContractPdfRequest request = ContractPdfRequest.builder()
                .onboardingId("onb-123")
                .productId("prod-pagopa")
                .productName("PagoPA")
                .contractTemplatePath("/templates/contract.html")
                
                .institution(institution)
                .manager(manager)
                .delegates(List.of(delegate))
                .build();

        // When
        Map<String, Object> result = PdfMapperData.setUpCommonData(request);

        // Then
        String delegatesSend = (String) result.get("delegatesSend");
        assertTrue(delegatesSend.contains("Luigi"));
        assertTrue(delegatesSend.contains("Verdi"));
        assertTrue(delegatesSend.contains("VRDLGU85B02H501V"));
        assertTrue(delegatesSend.contains("luigi.verdi@test.it"));
        assertTrue(delegatesSend.contains("Nome e Cognome:"));
        assertTrue(delegatesSend.contains("lst-kix_list_23-0"));
    }

    @Test
    void setUpCommonData_shouldHandleDelegateWithNullName() {
        // Given
        UserPdfData delegate = UserPdfData.builder()
                .id("delegate-id")
                .name(null)
                .surname(null)
                .taxCode("VRDLGU85B02H501V")
                .email(null)
                .build();

        InstitutionPdfData institution = InstitutionPdfData.builder()
                .description("Institution")
                .digitalAddress("test@pec.it")
                .institutionType(InstitutionType.PA)
                .build();

        UserPdfData manager = UserPdfData.builder()
                .id("manager-id")
                .taxCode("RSSMRA80A01H501U")
                .email("test@test.it")
                .build();

        ContractPdfRequest request = ContractPdfRequest.builder()
                .onboardingId("onb-123")
                .productId("prod-pagopa")
                .productName("PagoPA")
                .contractTemplatePath("/templates/contract.html")
                
                .institution(institution)
                .manager(manager)
                .delegates(List.of(delegate))
                .build();

        // When & Then
        assertDoesNotThrow(() -> PdfMapperData.setUpCommonData(request));
    }

    // ---- Multiple delegates ----

    @Test
    void setUpCommonData_shouldHandleMultipleDelegates() {
        // Given
        UserPdfData delegate1 = UserPdfData.builder()
                .id("delegate-1")
                .name("Luigi")
                .surname("Verdi")
                .taxCode("VRDLGU85B02H501V")
                .email("luigi@test.it")
                .build();

        UserPdfData delegate2 = UserPdfData.builder()
                .id("delegate-2")
                .name("Anna")
                .surname("Neri")
                .taxCode("NRANNA90C03H501X")
                .email("anna@test.it")
                .build();

        InstitutionPdfData institution = InstitutionPdfData.builder()
                .description("Institution")
                .digitalAddress("test@pec.it")
                .institutionType(InstitutionType.PA)
                .build();

        UserPdfData manager = UserPdfData.builder()
                .id("manager-id")
                .taxCode("RSSMRA80A01H501U")
                .email("test@test.it")
                .build();

        ContractPdfRequest request = ContractPdfRequest.builder()
                .onboardingId("onb-123")
                .productId("prod-pagopa")
                .productName("PagoPA")
                .contractTemplatePath("/templates/contract.html")
                
                .institution(institution)
                .manager(manager)
                .delegates(List.of(delegate1, delegate2))
                .build();

        // When
        Map<String, Object> result = PdfMapperData.setUpCommonData(request);

        // Then
        String delegates = (String) result.get("delegates");
        assertTrue(delegates.contains("Luigi"));
        assertTrue(delegates.contains("Verdi"));
        assertTrue(delegates.contains("Anna"));
        assertTrue(delegates.contains("Neri"));
    }

    // ---- Constructor test ----

    @Test
    void constructor_shouldBePrivate() throws Exception {
        // Use reflection to test private constructor
        var constructor = PdfMapperData.class.getDeclaredConstructor();
        assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());
    }
}







