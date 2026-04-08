package it.pagopa.selfcare.document.service;

import eu.europa.esig.dss.diagnostic.CertificateWrapper;
import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.enumerations.CertificateSourceType;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.Indication;
import eu.europa.esig.dss.enumerations.SignatureForm;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.Digest;
import eu.europa.esig.dss.spi.signature.AdvancedSignature;
import eu.europa.esig.dss.spi.tsl.TrustedListsCertificateSource;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.reports.Reports;
import eu.europa.esig.validationreport.jaxb.SignatureValidationReportType;
import eu.europa.esig.validationreport.jaxb.ValidationReportType;
import eu.europa.esig.validationreport.jaxb.ValidationStatusType;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.document.config.PagoPaSignatureConfig;
import it.pagopa.selfcare.document.exception.InvalidRequestException;
import it.pagopa.selfcare.document.exception.ResourceNotFoundException;
import it.pagopa.selfcare.document.model.FormItem;
import it.pagopa.selfcare.document.model.entity.Document;
import it.pagopa.selfcare.document.service.impl.SignatureServiceImpl;
import it.pagopa.selfcare.onboarding.crypto.PadesSignService;
import it.pagopa.selfcare.onboarding.crypto.entity.SignatureInformation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@QuarkusTest
class SignatureServiceImplTest {

    private TrustedListsCertificateSource trustedListsCertificateSource;
    private PagoPaSignatureConfig pagoPaSignatureConfig;
    private PadesSignService padesSignService;

    @InjectMock
    DocumentService documentService;

    private SignatureServiceImpl service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        trustedListsCertificateSource = Mockito.mock(TrustedListsCertificateSource.class);
        lenient().when(trustedListsCertificateSource.getCertificateSourceType())
                .thenReturn(CertificateSourceType.TRUSTED_LIST);
        pagoPaSignatureConfig = Mockito.mock(PagoPaSignatureConfig.class);
        padesSignService = Mockito.mock(PadesSignService.class);

        service = new SignatureServiceImpl(trustedListsCertificateSource, pagoPaSignatureConfig, padesSignService);
        setField(service, "isVerifyEnabled", Boolean.TRUE);
        setField(service, "documentService", documentService);
    }

    // ==================== UTILITY METHODS ====================

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }

    private AdvancedSignature createMockSignature(String id, Date signingTime, SignatureForm form) {
        AdvancedSignature sig = mock(AdvancedSignature.class);
        lenient().when(sig.getId()).thenReturn(id);
        lenient().when(sig.getSigningTime()).thenReturn(signingTime);
        lenient().when(sig.getSignatureForm()).thenReturn(form);
        return sig;
    }

    private Reports createMockReports(Indication indication) {
        Reports reports = mock(Reports.class);
        ValidationReportType etsi = mock(ValidationReportType.class);
        SignatureValidationReportType sigReport = mock(SignatureValidationReportType.class);
        ValidationStatusType statusType = mock(ValidationStatusType.class);

        when(statusType.getMainIndication()).thenReturn(indication);
        when(sigReport.getSignatureValidationStatus()).thenReturn(statusType);
        when(etsi.getSignatureValidationReport()).thenReturn(List.of(sigReport));
        when(reports.getEtsiValidationReportJaxb()).thenReturn(etsi);

        return reports;
    }

    private Reports createMockReportsWithCertificates(List<String> serialNumbers) {
        Reports reports = mock(Reports.class);
        DiagnosticData diagnosticData = mock(DiagnosticData.class);

        List<CertificateWrapper> certs = serialNumbers.stream()
                .map(sn -> {
                    CertificateWrapper cert = mock(CertificateWrapper.class);
                    when(cert.getSubjectSerialNumber()).thenReturn(sn);
                    return cert;
                })
                .toList();

        when(diagnosticData.getUsedCertificates()).thenReturn(certs);
        when(reports.getDiagnosticData()).thenReturn(diagnosticData);

        return reports;
    }

    private SignedDocumentValidator createMockValidator(List<AdvancedSignature> signatures) {
        SignedDocumentValidator validator = mock(SignedDocumentValidator.class);
        when(validator.getSignatures()).thenReturn(signatures);
        return validator;
    }

    private File createTempFile(String content) throws IOException {
        File file = tempDir.resolve("test-" + System.nanoTime() + ".pdf").toFile();
        Files.writeString(file.toPath(), content);
        return file;
    }

    private DSSDocument createMockDocWithDigest(String digestValue) {
        DSSDocument doc = mock(DSSDocument.class);
        Digest digest = mock(Digest.class);
        when(digest.getBase64Value()).thenReturn(digestValue);
        when(doc.getDigest(DigestAlgorithm.SHA256)).thenReturn(digest);
        return doc;
    }



    // ==================== createDocumentValidator ====================

    @Test
    void createDocumentValidator_shouldThrowInvalidRequestExceptionWhenValidatorCreationFails() {
        byte[] bytes = "signed-content".getBytes();

        try (MockedStatic<SignedDocumentValidator> validatorStatic = Mockito.mockStatic(SignedDocumentValidator.class)) {
            validatorStatic.when(() -> SignedDocumentValidator.fromDocument(any(DSSDocument.class)))
                    .thenThrow(new RuntimeException("validator creation failed"));

            assertThatThrownBy(() -> service.createDocumentValidator(bytes))
                    .isInstanceOf(InvalidRequestException.class);
        }
    }

    // ==================== isDocumentSigned ====================

    @Test
    void isDocumentSigned_shouldThrowWhenNoSignatures() {
        SignedDocumentValidator validator = createMockValidator(Collections.emptyList());

        assertThatThrownBy(() -> service.isDocumentSigned(validator))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void isDocumentSigned_shouldNotThrowWhenSignaturesPresent() {
        AdvancedSignature sig = createMockSignature("sig1", new Date(), SignatureForm.CAdES);
        SignedDocumentValidator validator = createMockValidator(List.of(sig));

        assertThatCode(() -> service.isDocumentSigned(validator)).doesNotThrowAnyException();
    }

    @Test
    void isDocumentSigned_shouldNotThrowWhenMultipleSignatures() {
        List<AdvancedSignature> sigs = List.of(
                createMockSignature("sig1", new Date(), SignatureForm.CAdES),
                createMockSignature("sig2", new Date(), SignatureForm.PAdES)
        );
        SignedDocumentValidator validator = createMockValidator(sigs);

        assertThatCode(() -> service.isDocumentSigned(validator)).doesNotThrowAnyException();
    }

    // ==================== verifyOriginalDocument ====================

    @Test
    void verifyOriginalDocument_shouldThrowWhenSignaturesNull() {
        SignedDocumentValidator validator = createMockValidator(null);

        assertThatThrownBy(() -> service.verifyOriginalDocument(validator))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void verifyOriginalDocument_shouldThrowWhenSignaturesEmpty() {
        SignedDocumentValidator validator = createMockValidator(Collections.emptyList());

        assertThatThrownBy(() -> service.verifyOriginalDocument(validator))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void verifyOriginalDocument_shouldThrowWhenNoOriginalDocuments() {
        AdvancedSignature sig = createMockSignature("sig1", new Date(), SignatureForm.CAdES);
        SignedDocumentValidator validator = createMockValidator(List.of(sig));
        when(validator.getOriginalDocuments("sig1")).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.verifyOriginalDocument(validator))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void verifyOriginalDocument_shouldThrowWhenOriginalDocumentsNull() {
        AdvancedSignature sig = createMockSignature("sig1", new Date(), SignatureForm.CAdES);
        SignedDocumentValidator validator = createMockValidator(List.of(sig));
        when(validator.getOriginalDocuments("sig1")).thenReturn(null);

        assertThatThrownBy(() -> service.verifyOriginalDocument(validator))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void verifyOriginalDocument_shouldNotThrowWhenOriginalDocumentsPresent() {
        AdvancedSignature sig = createMockSignature("sig1", new Date(), SignatureForm.CAdES);
        SignedDocumentValidator validator = createMockValidator(List.of(sig));
        when(validator.getOriginalDocuments("sig1")).thenReturn(List.of(mock(DSSDocument.class)));

        assertThatCode(() -> service.verifyOriginalDocument(validator)).doesNotThrowAnyException();
    }

    @Test
    void verifyOriginalDocument_shouldNotThrowWhenAtLeastOneHasOriginals() {
        AdvancedSignature sig1 = createMockSignature("sig1", new Date(), SignatureForm.CAdES);
        AdvancedSignature sig2 = createMockSignature("sig2", new Date(), SignatureForm.CAdES);
        SignedDocumentValidator validator = createMockValidator(List.of(sig1, sig2));
        when(validator.getOriginalDocuments("sig1")).thenReturn(Collections.emptyList());
        when(validator.getOriginalDocuments("sig2")).thenReturn(List.of(mock(DSSDocument.class)));

        assertThatCode(() -> service.verifyOriginalDocument(validator)).doesNotThrowAnyException();
    }

    // ==================== verifySignatureForm ====================

    @Test
    void verifySignatureForm_shouldNotThrowWhenAllCAdES() {
        List<AdvancedSignature> sigs = List.of(
                createMockSignature("sig1", new Date(), SignatureForm.CAdES),
                createMockSignature("sig2", new Date(), SignatureForm.CAdES)
        );
        SignedDocumentValidator validator = createMockValidator(sigs);

        assertThatCode(() -> service.verifySignatureForm(validator)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(value = SignatureForm.class, names = {"PAdES", "XAdES", "JAdES"})
    void verifySignatureForm_shouldThrowForNonCAdESForm(SignatureForm invalidForm) {
        AdvancedSignature sig = createMockSignature("sig1", new Date(), invalidForm);
        SignedDocumentValidator validator = createMockValidator(List.of(sig));

        assertThatThrownBy(() -> service.verifySignatureForm(validator))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining(invalidForm.toString());
    }

    @Test
    void verifySignatureForm_shouldThrowListingAllInvalidForms() {
        List<AdvancedSignature> sigs = List.of(
                createMockSignature("sig1", new Date(), SignatureForm.CAdES),
                createMockSignature("sig2", new Date(), SignatureForm.PAdES),
                createMockSignature("sig3", new Date(), SignatureForm.XAdES)
        );
        SignedDocumentValidator validator = createMockValidator(sigs);

        assertThatThrownBy(() -> service.verifySignatureForm(validator))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("PAdES")
                .hasMessageContaining("XAdES");
    }

    // ==================== verifySignature(Reports) ====================

    @Test
    void verifySignatureReports_shouldNotThrowWhenTotalPassed() {
        Reports reports = createMockReports(Indication.TOTAL_PASSED);

        assertThatCode(() -> service.verifySignature(reports)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(value = Indication.class, names = {"INDETERMINATE", "TOTAL_FAILED", "FAILED", "PASSED"})
    void verifySignatureReports_shouldThrowForNonTotalPassedIndications(Indication indication) {
        Reports reports = createMockReports(indication);

        assertThatThrownBy(() -> service.verifySignature(reports))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void verifySignatureReports_shouldThrowWhenEtsiReportNull() {
        Reports reports = mock(Reports.class);
        when(reports.getEtsiValidationReportJaxb()).thenReturn(null);

        assertThatThrownBy(() -> service.verifySignature(reports))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void verifySignatureReports_shouldThrowWhenReportsListEmpty() {
        Reports reports = mock(Reports.class);
        ValidationReportType etsi = mock(ValidationReportType.class);
        when(etsi.getSignatureValidationReport()).thenReturn(Collections.emptyList());
        when(reports.getEtsiValidationReportJaxb()).thenReturn(etsi);

        assertThatThrownBy(() -> service.verifySignature(reports))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void verifySignatureReports_shouldThrowWhenValidationStatusNull() {
        Reports reports = mock(Reports.class);
        ValidationReportType etsi = mock(ValidationReportType.class);
        SignatureValidationReportType sigReport = mock(SignatureValidationReportType.class);
        when(sigReport.getSignatureValidationStatus()).thenReturn(null);
        when(etsi.getSignatureValidationReport()).thenReturn(List.of(sigReport));
        when(reports.getEtsiValidationReportJaxb()).thenReturn(etsi);

        assertThatThrownBy(() -> service.verifySignature(reports))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void verifySignatureReports_shouldThrowWhenAnyReportNotTotalPassed() {
        Reports reports = mock(Reports.class);
        ValidationReportType etsi = mock(ValidationReportType.class);

        SignatureValidationReportType goodReport = mock(SignatureValidationReportType.class);
        ValidationStatusType goodStatus = mock(ValidationStatusType.class);
        when(goodStatus.getMainIndication()).thenReturn(Indication.TOTAL_PASSED);
        when(goodReport.getSignatureValidationStatus()).thenReturn(goodStatus);

        SignatureValidationReportType badReport = mock(SignatureValidationReportType.class);
        ValidationStatusType badStatus = mock(ValidationStatusType.class);
        when(badStatus.getMainIndication()).thenReturn(Indication.INDETERMINATE);
        when(badReport.getSignatureValidationStatus()).thenReturn(badStatus);

        when(etsi.getSignatureValidationReport()).thenReturn(List.of(goodReport, badReport));
        when(reports.getEtsiValidationReportJaxb()).thenReturn(etsi);

        assertThatThrownBy(() -> service.verifySignature(reports))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void verifySignatureReports_shouldNotThrowWhenAllReportsTotalPassed() {
        Reports reports = mock(Reports.class);
        ValidationReportType etsi = mock(ValidationReportType.class);

        List<SignatureValidationReportType> sigReports = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            SignatureValidationReportType sigReport = mock(SignatureValidationReportType.class);
            ValidationStatusType status = mock(ValidationStatusType.class);
            when(status.getMainIndication()).thenReturn(Indication.TOTAL_PASSED);
            when(sigReport.getSignatureValidationStatus()).thenReturn(status);
            sigReports.add(sigReport);
        }

        when(etsi.getSignatureValidationReport()).thenReturn(sigReports);
        when(reports.getEtsiValidationReportJaxb()).thenReturn(etsi);

        assertThatCode(() -> service.verifySignature(reports)).doesNotThrowAnyException();
    }

    // ==================== checkSignature(Reports) ====================

    @Test
    void checkSignature_shouldNotThrowWhenStatusPresent() {
        Reports reports = createMockReports(Indication.TOTAL_PASSED);

        assertThatCode(() -> service.checkSignature(reports)).doesNotThrowAnyException();
    }

    @Test
    void checkSignature_shouldThrowWhenEtsiNull() {
        Reports reports = mock(Reports.class);
        when(reports.getEtsiValidationReportJaxb()).thenReturn(null);

        assertThatThrownBy(() -> service.checkSignature(reports))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void checkSignature_shouldThrowWhenReportsEmpty() {
        Reports reports = mock(Reports.class);
        ValidationReportType etsi = mock(ValidationReportType.class);
        when(etsi.getSignatureValidationReport()).thenReturn(Collections.emptyList());
        when(reports.getEtsiValidationReportJaxb()).thenReturn(etsi);

        assertThatThrownBy(() -> service.checkSignature(reports))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void checkSignature_shouldThrowWhenStatusNull() {
        Reports reports = mock(Reports.class);
        ValidationReportType etsi = mock(ValidationReportType.class);
        SignatureValidationReportType sigReport = mock(SignatureValidationReportType.class);
        when(sigReport.getSignatureValidationStatus()).thenReturn(null);
        when(etsi.getSignatureValidationReport()).thenReturn(List.of(sigReport));
        when(reports.getEtsiValidationReportJaxb()).thenReturn(etsi);

        assertThatThrownBy(() -> service.checkSignature(reports))
                .isInstanceOf(InvalidRequestException.class);
    }

    // ==================== verifyDigest ====================

    @Test
    void verifyDigest_shouldNotThrowWhenDigestMatches() {
        String checksum = "correctChecksum";
        DSSDocument doc = createMockDocWithDigest(checksum);
        AdvancedSignature sig = createMockSignature("sig1", new Date(), SignatureForm.CAdES);
        SignedDocumentValidator validator = createMockValidator(List.of(sig));
        when(validator.getOriginalDocuments("sig1")).thenReturn(List.of(doc));

        assertThatCode(() -> service.verifyDigest(validator, checksum)).doesNotThrowAnyException();
    }

    @Test
    void verifyDigest_shouldThrowWhenDigestMismatch() {
        DSSDocument doc = createMockDocWithDigest("wrongChecksum");
        AdvancedSignature sig = createMockSignature("sig1", new Date(), SignatureForm.CAdES);
        SignedDocumentValidator validator = createMockValidator(List.of(sig));
        when(validator.getOriginalDocuments("sig1")).thenReturn(List.of(doc));

        assertThatThrownBy(() -> service.verifyDigest(validator, "expectedChecksum"))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void verifyDigest_shouldNotThrowWhenSignaturesNull() {
        SignedDocumentValidator validator = createMockValidator(null);

        assertThatCode(() -> service.verifyDigest(validator, "anyChecksum")).doesNotThrowAnyException();
    }

    @Test
    void verifyDigest_shouldNotThrowWhenSignaturesEmpty() {
        SignedDocumentValidator validator = createMockValidator(Collections.emptyList());

        assertThatCode(() -> service.verifyDigest(validator, "anyChecksum")).doesNotThrowAnyException();
    }

    @Test
    void verifyDigest_shouldThrowWhenAnySignatureHasWrongDigest() {
        String checksum = "correctChecksum";
        DSSDocument doc1 = createMockDocWithDigest(checksum);
        DSSDocument doc2 = createMockDocWithDigest("wrongChecksum");
        AdvancedSignature sig1 = createMockSignature("sig1", new Date(), SignatureForm.CAdES);
        AdvancedSignature sig2 = createMockSignature("sig2", new Date(), SignatureForm.CAdES);
        SignedDocumentValidator validator = createMockValidator(List.of(sig1, sig2));

        when(validator.getOriginalDocuments("sig1")).thenReturn(List.of(doc1));
        when(validator.getOriginalDocuments("sig2")).thenReturn(List.of(doc2));

        assertThatThrownBy(() -> service.verifyDigest(validator, checksum))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void verifyDigest_shouldNotThrowWhenAllSignaturesHaveCorrectDigest() {
        String checksum = "correctChecksum";
        DSSDocument doc1 = createMockDocWithDigest(checksum);
        DSSDocument doc2 = createMockDocWithDigest(checksum);
        AdvancedSignature sig1 = createMockSignature("sig1", new Date(), SignatureForm.CAdES);
        AdvancedSignature sig2 = createMockSignature("sig2", new Date(), SignatureForm.CAdES);
        SignedDocumentValidator validator = createMockValidator(List.of(sig1, sig2));

        when(validator.getOriginalDocuments("sig1")).thenReturn(List.of(doc1));
        when(validator.getOriginalDocuments("sig2")).thenReturn(List.of(doc2));

        assertThatCode(() -> service.verifyDigest(validator, checksum)).doesNotThrowAnyException();
    }

    // ==================== verifyManagerTaxCode ====================

    @Test
    void verifyManagerTaxCode_shouldNotThrowWhenTaxCodeMatches() {
        Reports reports = createMockReportsWithCertificates(List.of("TINIT-RSSMRA80A01H501R"));

        assertThatCode(() -> service.verifyManagerTaxCode(reports, List.of("RSSMRA80A01H501R")))
                .doesNotThrowAnyException();
    }

    @Test
    void verifyManagerTaxCode_shouldThrowWhenDiagnosticDataNull() {
        Reports reports = mock(Reports.class);
        when(reports.getDiagnosticData()).thenReturn(null);

        assertThatThrownBy(() -> service.verifyManagerTaxCode(reports, List.of("RSSMRA80A01H501R")))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void verifyManagerTaxCode_shouldThrowWhenUsedCertificatesNull() {
        Reports reports = mock(Reports.class);
        DiagnosticData diagnosticData = mock(DiagnosticData.class);
        when(diagnosticData.getUsedCertificates()).thenReturn(null);
        when(reports.getDiagnosticData()).thenReturn(diagnosticData);

        assertThatThrownBy(() -> service.verifyManagerTaxCode(reports, List.of("RSSMRA80A01H501R")))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void verifyManagerTaxCode_shouldThrowWhenUsedCertificatesEmpty() {
        Reports reports = mock(Reports.class);
        DiagnosticData diagnosticData = mock(DiagnosticData.class);
        when(diagnosticData.getUsedCertificates()).thenReturn(Collections.emptyList());
        when(reports.getDiagnosticData()).thenReturn(diagnosticData);

        assertThatThrownBy(() -> service.verifyManagerTaxCode(reports, List.of("RSSMRA80A01H501R")))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void verifyManagerTaxCode_shouldThrowWhenNoTinitPrefix() {
        Reports reports = createMockReportsWithCertificates(List.of("NO_MATCH_PREFIX"));

        assertThatThrownBy(() -> service.verifyManagerTaxCode(reports, List.of("RSSMRA80A01H501R")))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void verifyManagerTaxCode_shouldThrowWhenTaxCodeNotInUserList() {
        Reports reports = createMockReportsWithCertificates(List.of("TINIT-DIFFERENT"));

        assertThatThrownBy(() -> service.verifyManagerTaxCode(reports, List.of("RSSMRA80A01H501R")))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void verifyManagerTaxCode_shouldThrowWhenUsersTaxCodeEmpty() {
        Reports reports = createMockReportsWithCertificates(List.of("TINIT-RSSMRA80A01H501R"));

        assertThatThrownBy(() -> service.verifyManagerTaxCode(reports, Collections.emptyList()))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void verifyManagerTaxCode_shouldNotThrowWhenMultipleSignersCoverAllUsers() {
        Reports reports = createMockReportsWithCertificates(List.of("TINIT-AAA111", "TINIT-BBB222"));

        assertThatCode(() -> service.verifyManagerTaxCode(reports, List.of("AAA111", "BBB222")))
                .doesNotThrowAnyException();
    }

    @Test
    void verifyManagerTaxCode_shouldThrowWhenOnlySomeUsersCovered() {
        Reports reports = createMockReportsWithCertificates(List.of("TINIT-AAA111"));

        assertThatThrownBy(() -> service.verifyManagerTaxCode(reports, List.of("AAA111", "BBB222")))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void verifyManagerTaxCode_shouldIgnoreNullSerialNumbers() {
        Reports reports = mock(Reports.class);
        DiagnosticData diagnosticData = mock(DiagnosticData.class);
        CertificateWrapper cert = mock(CertificateWrapper.class);
        when(cert.getSubjectSerialNumber()).thenReturn(null);
        when(diagnosticData.getUsedCertificates()).thenReturn(List.of(cert));
        when(reports.getDiagnosticData()).thenReturn(diagnosticData);

        assertThatThrownBy(() -> service.verifyManagerTaxCode(reports, List.of("RSSMRA80A01H501R")))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void verifyManagerTaxCode_shouldIgnoreEmptySerialNumbers() {
        Reports reports = mock(Reports.class);
        DiagnosticData diagnosticData = mock(DiagnosticData.class);
        CertificateWrapper cert = mock(CertificateWrapper.class);
        when(cert.getSubjectSerialNumber()).thenReturn("");
        when(diagnosticData.getUsedCertificates()).thenReturn(List.of(cert));
        when(reports.getDiagnosticData()).thenReturn(diagnosticData);

        assertThatThrownBy(() -> service.verifyManagerTaxCode(reports, List.of("RSSMRA80A01H501R")))
                .isInstanceOf(InvalidRequestException.class);
    }

    // ==================== isSignatureVerificationEnabled ====================

    @Test
    void isSignatureVerificationEnabled_shouldReturnTrueWhenEnabled() {
        setField(service, "isVerifyEnabled", Boolean.TRUE);
        assertTrue(service.isSignatureVerificationEnabled());
    }

    @Test
    void isSignatureVerificationEnabled_shouldReturnFalseWhenDisabled() {
        setField(service, "isVerifyEnabled", Boolean.FALSE);
        assertFalse(service.isSignatureVerificationEnabled());
    }

    @Test
    void isSignatureVerificationEnabled_shouldReturnFalseWhenNull() {
        setField(service, "isVerifyEnabled", null);
        assertFalse(service.isSignatureVerificationEnabled());
    }

    // ==================== verifyContractSignature ====================

    @Test
    void verifyContractSignature_shouldReturnImmediatelyWhenDisabled() throws IOException {
        setField(service, "isVerifyEnabled", Boolean.FALSE);
        File file = createTempFile("test");

        Uni<Void> result = service.verifyContractSignature("onboarding-id", file, List.of("CF1"), false);

        assertThatCode(() -> result.await().indefinitely()).doesNotThrowAnyException();
        verifyNoInteractions(documentService);
    }

    @Test
    void verifyContractSignature_shouldReturnImmediatelyWhenSkipSignatureVerificationIsTrue() throws IOException {
        setField(service, "isVerifyEnabled", Boolean.TRUE);
        File file = createTempFile("test");

        Uni<Void> result = service.verifyContractSignature("onboarding-id", file, List.of("CF1"), true);

        assertThatCode(() -> result.await().indefinitely()).doesNotThrowAnyException();
        verifyNoInteractions(documentService);
    }

    @Test
    void verifyContractSignature_shouldCallDocumentServiceWhenEnabled() throws IOException {
        setField(service, "isVerifyEnabled", Boolean.TRUE);
        Document document = mock(Document.class);
        when(document.getChecksum()).thenReturn("checksum");
        when(documentService.getDocumentByOnboardingId("onboarding-id")).thenReturn(Uni.createFrom().item(document));

        File file = createTempFile("test");
        Uni<Void> result = service.verifyContractSignature("onboarding-id", file, List.of("CF1"), false);

        assertThatThrownBy(() -> result.await().indefinitely()).isInstanceOf(Exception.class);
        verify(documentService).getDocumentByOnboardingId("onboarding-id");
    }

    @Test
    void verifyContractSignature_shouldPropagateExceptionWhenDocumentServiceFails() throws IOException {
        setField(service, "isVerifyEnabled", Boolean.TRUE);
        when(documentService.getDocumentByOnboardingId("onboarding-id"))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));

        File file = createTempFile("test");
        Uni<Void> result = service.verifyContractSignature("onboarding-id", file, List.of("CF1"), false);

        assertThatThrownBy(() -> result.await().indefinitely())
                .hasMessageContaining("DB error");
        verify(documentService).getDocumentByOnboardingId("onboarding-id");
    }

    // ==================== chooseEarliestSignature ====================

    @Test
    void chooseEarliestSignature_shouldReturnEarliestBySigningTime() {
        Date earlier = Date.from(Instant.parse("2024-01-01T00:00:00Z"));
        Date later = Date.from(Instant.parse("2024-06-01T00:00:00Z"));
        List<AdvancedSignature> sigs = List.of(
                createMockSignature("sig-later", later, SignatureForm.CAdES),
                createMockSignature("sig-earlier", earlier, SignatureForm.CAdES)
        );

        AdvancedSignature result = service.chooseEarliestSignature(sigs);

        assertThat(result.getId()).isEqualTo("sig-earlier");
    }

    @Test
    void chooseEarliestSignature_shouldFallbackToIdWhenSameSigningTime() {
        Date sameDate = new Date();
        List<AdvancedSignature> sigs = List.of(
                createMockSignature("sig-B", sameDate, SignatureForm.CAdES),
                createMockSignature("sig-A", sameDate, SignatureForm.CAdES)
        );

        AdvancedSignature result = service.chooseEarliestSignature(sigs);

        assertThat(result.getId()).isEqualTo("sig-A");
    }

    @Test
    void chooseEarliestSignature_shouldHandleNullSigningTime() {
        Date realDate = new Date();
        List<AdvancedSignature> sigs = List.of(
                createMockSignature("sig-null", null, SignatureForm.CAdES),
                createMockSignature("sig-real", realDate, SignatureForm.CAdES)
        );

        AdvancedSignature result = service.chooseEarliestSignature(sigs);

        assertThat(result.getId()).isEqualTo("sig-real");
    }

    @Test
    void chooseEarliestSignature_shouldReturnSingleSignature() {
        AdvancedSignature sig = createMockSignature("only-sig", new Date(), SignatureForm.CAdES);

        AdvancedSignature result = service.chooseEarliestSignature(List.of(sig));

        assertThat(result.getId()).isEqualTo("only-sig");
    }

    @Test
    void chooseEarliestSignature_shouldHandleAllNullSigningTimes() {
        List<AdvancedSignature> sigs = List.of(
                createMockSignature("sig-B", null, SignatureForm.CAdES),
                createMockSignature("sig-A", null, SignatureForm.CAdES)
        );

        AdvancedSignature result = service.chooseEarliestSignature(sigs);

        assertThat(result.getId()).isEqualTo("sig-A");
    }

    // ==================== extractPdfFromSignedContainer ====================

    @Test
    void extractPdfFromSignedContainer_shouldReturnOriginalDocument() {
        DSSDocument inputDoc = mock(DSSDocument.class);
        DSSDocument originalDoc = mock(DSSDocument.class);
        AdvancedSignature sig = createMockSignature("sig1", new Date(), SignatureForm.CAdES);
        SignedDocumentValidator validator = createMockValidator(List.of(sig));
        when(validator.getOriginalDocuments("sig1")).thenReturn(List.of(originalDoc));

        DSSDocument result = service.extractPdfFromSignedContainer(validator, inputDoc);

        assertThat(result).isSameAs(originalDoc);
    }

    @Test
    void extractPdfFromSignedContainer_shouldReturnInputWhenSignaturesNull() {
        DSSDocument inputDoc = mock(DSSDocument.class);
        SignedDocumentValidator validator = createMockValidator(null);

        DSSDocument result = service.extractPdfFromSignedContainer(validator, inputDoc);

        assertThat(result).isSameAs(inputDoc);
    }

    @Test
    void extractPdfFromSignedContainer_shouldReturnInputWhenSignaturesEmpty() {
        DSSDocument inputDoc = mock(DSSDocument.class);
        SignedDocumentValidator validator = createMockValidator(Collections.emptyList());

        DSSDocument result = service.extractPdfFromSignedContainer(validator, inputDoc);

        assertThat(result).isSameAs(inputDoc);
    }

    @Test
    void extractPdfFromSignedContainer_shouldReturnInputWhenNoOriginals() {
        DSSDocument inputDoc = mock(DSSDocument.class);
        AdvancedSignature sig = createMockSignature("sig1", new Date(), SignatureForm.CAdES);
        SignedDocumentValidator validator = createMockValidator(List.of(sig));
        when(validator.getOriginalDocuments("sig1")).thenReturn(Collections.emptyList());

        DSSDocument result = service.extractPdfFromSignedContainer(validator, inputDoc);

        assertThat(result).isSameAs(inputDoc);
    }

    @Test
    void extractPdfFromSignedContainer_shouldReturnInputWhenOriginalsNull() {
        DSSDocument inputDoc = mock(DSSDocument.class);
        AdvancedSignature sig = createMockSignature("sig1", new Date(), SignatureForm.CAdES);
        SignedDocumentValidator validator = createMockValidator(List.of(sig));
        when(validator.getOriginalDocuments("sig1")).thenReturn(null);

        DSSDocument result = service.extractPdfFromSignedContainer(validator, inputDoc);

        assertThat(result).isSameAs(inputDoc);
    }

    // ==================== computeDigestOfSignedRevision ====================

    @Test
    void computeDigestOfSignedRevision_shouldComputeFullDocDigestWhenNoSignatures() {
        DSSDocument doc = createMockDocWithDigest("fullDocDigest");
        SignedDocumentValidator validator = createMockValidator(Collections.emptyList());

        String result = service.computeDigestOfSignedRevision(validator, doc);

        assertThat(result).isEqualTo("fullDocDigest");
    }

    @Test
    void computeDigestOfSignedRevision_shouldComputeFullDocDigestWhenSignaturesNull() {
        DSSDocument doc = createMockDocWithDigest("fullDocDigest");
        SignedDocumentValidator validator = createMockValidator(null);

        String result = service.computeDigestOfSignedRevision(validator, doc);

        assertThat(result).isEqualTo("fullDocDigest");
    }

    @Test
    void computeDigestOfSignedRevision_shouldComputeDigestOfOriginalDocument() {
        DSSDocument inputDoc = mock(DSSDocument.class);
        DSSDocument originalDoc = createMockDocWithDigest("originalDigest");
        AdvancedSignature sig = createMockSignature("sig1", new Date(), SignatureForm.CAdES);
        SignedDocumentValidator validator = createMockValidator(List.of(sig));
        when(validator.getOriginalDocuments("sig1")).thenReturn(List.of(originalDoc));

        String result = service.computeDigestOfSignedRevision(validator, inputDoc);

        assertThat(result).isEqualTo("originalDigest");
    }

    @Test
    void computeDigestOfSignedRevision_shouldComputeDigestOfInputDocWhenNoOriginals() {
        DSSDocument inputDoc = createMockDocWithDigest("inputDigest");
        AdvancedSignature sig = createMockSignature("sig1", new Date(), SignatureForm.CAdES);
        SignedDocumentValidator validator = createMockValidator(List.of(sig));
        when(validator.getOriginalDocuments("sig1")).thenReturn(Collections.emptyList());

        String result = service.computeDigestOfSignedRevision(validator, inputDoc);

        assertThat(result).isEqualTo("inputDigest");
    }

    // ==================== verifyUploadedFileDigest ====================

    @Test
    void verifyUploadedFileDigest_shouldThrowWhenFormItemNull() {
        assertThatThrownBy(() -> service.verifyUploadedFileDigest(null, "digest", false))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void verifyUploadedFileDigest_shouldThrowWhenTemplateDigestNull() throws IOException {
        File file = createTempFile("content");
        FormItem formItem = mock(FormItem.class);
        when(formItem.getFile()).thenReturn(file);

        assertThatThrownBy(() -> service.verifyUploadedFileDigest(formItem, null, false))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void verifyUploadedFileDigest_shouldReturnDigestWhenMatches() throws IOException {
        // Arrange
        File testFile = createTempFile("pdf-content");
        FormItem formItem = mock(FormItem.class);
        when(formItem.getFile()).thenReturn(testFile);

        String templateDigest = "matchingDigest123";

        DSSDocument mockDocument = mock(DSSDocument.class);
        Digest mockDigest = mock(Digest.class);
        when(mockDigest.getBase64Value()).thenReturn(templateDigest);
        when(mockDocument.getDigest(DigestAlgorithm.SHA256)).thenReturn(mockDigest);

        SignedDocumentValidator mockValidator = mock(SignedDocumentValidator.class);
        when(mockValidator.getSignatures()).thenReturn(Collections.emptyList());

        try (MockedStatic<SignedDocumentValidator> validatorMock = Mockito.mockStatic(SignedDocumentValidator.class)) {
            validatorMock.when(() -> SignedDocumentValidator.fromDocument(any(DSSDocument.class)))
                    .thenReturn(mockValidator);

            // Create spy to control extractPdfFromSignedContainer and computeDigestOfSignedRevision
            SignatureServiceImpl spyService = spy(new SignatureServiceImpl(
                    trustedListsCertificateSource, pagoPaSignatureConfig, padesSignService));
            setField(spyService, "isVerifyEnabled", Boolean.TRUE);
            setField(spyService, "documentService", documentService);

            doReturn(mockDocument).when(spyService).extractPdfFromSignedContainer(any(), any());
            doReturn(templateDigest).when(spyService).computeDigestOfSignedRevision(any(), any());

            // Act
            String result = spyService.verifyUploadedFileDigest(formItem, templateDigest, false);

            // Assert
            assertThat(result).isEqualTo(templateDigest);
        }
    }

    @Test
    void verifyUploadedFileDigest_shouldThrowWhenDigestMismatchAndSkipCheckFalse() throws IOException {
        // Arrange
        File testFile = createTempFile("pdf-content");
        FormItem formItem = mock(FormItem.class);
        when(formItem.getFile()).thenReturn(testFile);

        String templateDigest = "expectedDigest";
        String uploadedDigest = "differentDigest";

        DSSDocument mockDocument = mock(DSSDocument.class);
        SignedDocumentValidator mockValidator = mock(SignedDocumentValidator.class);
        when(mockValidator.getSignatures()).thenReturn(Collections.emptyList());

        try (MockedStatic<SignedDocumentValidator> validatorMock = Mockito.mockStatic(SignedDocumentValidator.class)) {
            validatorMock.when(() -> SignedDocumentValidator.fromDocument(any(DSSDocument.class)))
                    .thenReturn(mockValidator);

            SignatureServiceImpl spyService = spy(new SignatureServiceImpl(
                    trustedListsCertificateSource, pagoPaSignatureConfig, padesSignService));
            setField(spyService, "isVerifyEnabled", Boolean.TRUE);
            setField(spyService, "documentService", documentService);

            doReturn(mockDocument).when(spyService).extractPdfFromSignedContainer(any(), any());
            doReturn(uploadedDigest).when(spyService).computeDigestOfSignedRevision(any(), any());

            // Act & Assert
            assertThatThrownBy(() -> spyService.verifyUploadedFileDigest(formItem, templateDigest, false))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("File has been changed");
        }
    }

    @Test
    void verifyUploadedFileDigest_shouldReturnDigestWhenMismatchButSkipCheckTrue() throws IOException {
        // Arrange
        File testFile = createTempFile("pdf-content");
        FormItem formItem = mock(FormItem.class);
        when(formItem.getFile()).thenReturn(testFile);

        String templateDigest = "expectedDigest";
        String uploadedDigest = "differentDigest";

        DSSDocument mockDocument = mock(DSSDocument.class);
        SignedDocumentValidator mockValidator = mock(SignedDocumentValidator.class);
        when(mockValidator.getSignatures()).thenReturn(Collections.emptyList());

        try (MockedStatic<SignedDocumentValidator> validatorMock = Mockito.mockStatic(SignedDocumentValidator.class)) {
            validatorMock.when(() -> SignedDocumentValidator.fromDocument(any(DSSDocument.class)))
                    .thenReturn(mockValidator);

            SignatureServiceImpl spyService = spy(new SignatureServiceImpl(
                    trustedListsCertificateSource, pagoPaSignatureConfig, padesSignService));
            setField(spyService, "isVerifyEnabled", Boolean.TRUE);
            setField(spyService, "documentService", documentService);

            doReturn(mockDocument).when(spyService).extractPdfFromSignedContainer(any(), any());
            doReturn(uploadedDigest).when(spyService).computeDigestOfSignedRevision(any(), any());

            // Act - skipDigestCheck = true, should not throw
            String result = spyService.verifyUploadedFileDigest(formItem, templateDigest, true);

            // Assert
            assertThat(result).isEqualTo(uploadedDigest);
        }
    }

    @Test
    void verifyUploadedFileDigest_shouldProcessSignedDocument() throws IOException {
        // Arrange
        File testFile = createTempFile("signed-pdf-content");
        FormItem formItem = mock(FormItem.class);
        when(formItem.getFile()).thenReturn(testFile);

        String templateDigest = "signedDocDigest";

        DSSDocument mockExtractedPdf = mock(DSSDocument.class);

        AdvancedSignature mockSig = createMockSignature("sig1", new Date(), SignatureForm.PAdES);
        SignedDocumentValidator mockValidator = mock(SignedDocumentValidator.class);
        when(mockValidator.getSignatures()).thenReturn(List.of(mockSig));
        when(mockValidator.getOriginalDocuments("sig1")).thenReturn(List.of(mockExtractedPdf));

        Digest mockDigest = mock(Digest.class);
        when(mockDigest.getBase64Value()).thenReturn(templateDigest);
        when(mockExtractedPdf.getDigest(DigestAlgorithm.SHA256)).thenReturn(mockDigest);

        try (MockedStatic<SignedDocumentValidator> validatorMock = Mockito.mockStatic(SignedDocumentValidator.class)) {
            validatorMock.when(() -> SignedDocumentValidator.fromDocument(any(DSSDocument.class)))
                    .thenReturn(mockValidator);

            SignatureServiceImpl spyService = spy(new SignatureServiceImpl(
                    trustedListsCertificateSource, pagoPaSignatureConfig, padesSignService));
            setField(spyService, "isVerifyEnabled", Boolean.TRUE);
            setField(spyService, "documentService", documentService);

            // Don't mock internal methods - let them run to cover lines 427-448
            // But we need to mock the static validator creation

            // Act
            String result = spyService.verifyUploadedFileDigest(formItem, templateDigest, false);

            // Assert
            assertThat(result).isEqualTo(templateDigest);
        }
    }

    // ==================== signDocument ====================

    @Test
    void signDocument_shouldReturnOriginalFileWhenDisabled() throws Exception {
        when(pagoPaSignatureConfig.source()).thenReturn("disabled");
        File pdf = createTempFile("content");

        File result = service.signDocument(pdf, "Institution", "product").await().indefinitely();

        assertThat(result).isEqualTo(pdf);
        verifyNoInteractions(padesSignService);
    }

    @Test
    void signDocument_shouldCallPadesSignServiceWhenEnabled() throws Exception {
        when(pagoPaSignatureConfig.source()).thenReturn("enabled");
        when(pagoPaSignatureConfig.applyOnboardingTemplateReason()).thenReturn("Reason");
        when(pagoPaSignatureConfig.signer()).thenReturn("Signer");
        when(pagoPaSignatureConfig.location()).thenReturn("Location");

        File pdf = createTempFile("content");
        doNothing().when(padesSignService).padesSign(any(File.class), any(File.class), any());

        File result = service.signDocument(pdf, "Institution", "product").await().indefinitely();

        assertThat(result).isNotNull();
        verify(padesSignService).padesSign(eq(pdf), any(File.class), any(SignatureInformation.class));
    }

    @Test
    void signDocument_shouldInterpolatePlaceholders() throws Exception {
        when(pagoPaSignatureConfig.source()).thenReturn("active");
        when(pagoPaSignatureConfig.applyOnboardingTemplateReason())
                .thenReturn("Firma per ${institutionName} - ${productName}");
        when(pagoPaSignatureConfig.signer()).thenReturn("Signer");
        when(pagoPaSignatureConfig.location()).thenReturn("Location");

        File pdf = createTempFile("content");

        ArgumentCaptor<SignatureInformation> captor = ArgumentCaptor.forClass(SignatureInformation.class);
        doNothing().when(padesSignService).padesSign(any(), any(), captor.capture());

        service.signDocument(pdf, "TestInstitution", "TestProduct").await().indefinitely();

        assertThat(captor.getValue().getReason()).isEqualTo("Firma per TestInstitution - TestProduct");
    }

    @Test
    void signDocument_shouldThrowWhenPadesSignServiceFails() throws Exception {
        when(pagoPaSignatureConfig.source()).thenReturn("enabled");
        when(pagoPaSignatureConfig.applyOnboardingTemplateReason()).thenReturn("Reason");
        when(pagoPaSignatureConfig.signer()).thenReturn("Signer");
        when(pagoPaSignatureConfig.location()).thenReturn("Location");

        File pdf = createTempFile("content");
        doThrow(new RuntimeException("Signing failed")).when(padesSignService)
                .padesSign(any(File.class), any(File.class), any());

        assertThatThrownBy(() -> service.signDocument(pdf, "Institution", "product").await().indefinitely())
                .isInstanceOf(RuntimeException.class);
    }

    // ==================== validateDocument ====================

    @Test
    void validateDocument_shouldThrowOnValidationFailure() {
        SignedDocumentValidator validator = mock(SignedDocumentValidator.class);
        when(validator.validateDocument()).thenThrow(new RuntimeException("validation failed"));

        assertThatThrownBy(() -> service.validateDocument(validator))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void validateDocument_shouldReturnReportsOnSuccess() {
        SignedDocumentValidator validator = mock(SignedDocumentValidator.class);
        Reports expectedReports = mock(Reports.class);
        when(validator.validateDocument()).thenReturn(expectedReports);

        Reports result = service.validateDocument(validator);

        assertThat(result).isSameAs(expectedReports);
    }

    // ==================== verifySignature(File) ====================

    @Test
    void verifySignatureFile_shouldThrowWhenNotSignedDocument() throws IOException {
        File invalidFile = createTempFile("not-a-signed-document");

        assertThatThrownBy(() -> service.verifySignature(invalidFile))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void verifySignatureFile_shouldThrowWhenFileDoesNotExist() {
        File nonExistentFile = tempDir.resolve("nonexistent.pdf").toFile();

        assertThatThrownBy(() -> service.verifySignature(nonExistentFile))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void verifySignatureFile_shouldThrowWhenFileIsNull() {
        assertThatThrownBy(() -> service.verifySignature((File) null))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void verifySignatureFile_shouldThrowWhenFileOutsideTempDir() throws IOException {
        Path outsideDir = Files.createDirectories(Path.of("target", "verify-signature-outside"));
        File outsideFile = outsideDir.resolve("outside.pdf").toFile();
        Files.writeString(outsideFile.toPath(), "content-outside");

        assertThatThrownBy(() -> service.verifySignature(outsideFile))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void verifySignatureFile_shouldReturnTrueWhenFullValidationChainPasses() throws IOException {
        SignatureServiceImpl spyService = spy(new SignatureServiceImpl(
                trustedListsCertificateSource, pagoPaSignatureConfig, padesSignService));
        setField(spyService, "isVerifyEnabled", Boolean.TRUE);
        setField(spyService, "documentService", documentService);

        File testFile = createTempFile("valid-content");

        SignedDocumentValidator mockValidator = mock(SignedDocumentValidator.class);
        AdvancedSignature sig = createMockSignature("sig1", new Date(), SignatureForm.CAdES);
        when(mockValidator.getSignatures()).thenReturn(List.of(sig));
        when(mockValidator.getOriginalDocuments("sig1")).thenReturn(List.of(mock(DSSDocument.class)));

        Reports reports = mock(Reports.class);
        ValidationReportType etsi = mock(ValidationReportType.class);
        SignatureValidationReportType sigReport = mock(SignatureValidationReportType.class);
        ValidationStatusType status = mock(ValidationStatusType.class);
        when(sigReport.getSignatureValidationStatus()).thenReturn(status);
        when(etsi.getSignatureValidationReport()).thenReturn(List.of(sigReport));
        when(reports.getEtsiValidationReportJaxb()).thenReturn(etsi);
        when(mockValidator.validateDocument()).thenReturn(reports);

        doReturn(mockValidator).when(spyService).createDocumentValidator(any(byte[].class));

        boolean result = spyService.verifySignature(testFile);

        assertTrue(result);
        verify(spyService).isDocumentSigned(mockValidator);
        verify(spyService).verifyOriginalDocument(mockValidator);
        verify(spyService).validateDocument(mockValidator);
        verify(spyService).verifySignatureForm(mockValidator);
        verify(spyService).checkSignature(reports);
    }

    // ==================== verifySignature(File, String, List) ====================

    @Test
    void verifySignatureFileChecksumTaxCode_shouldThrowWhenInvalidData() throws IOException {
        File invalidFile = createTempFile("garbage");

        assertThatThrownBy(() -> service.verifySignature(invalidFile, "checksum", List.of("CF1")))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void verifySignatureFileChecksumTaxCode_shouldRethrowInvalidRequestExceptionDirectly() throws IOException {
        File invalidFile = createTempFile("garbage-data");

        assertThatThrownBy(() -> service.verifySignature(invalidFile, "checksum", List.of("CF1")))
                .isExactlyInstanceOf(InvalidRequestException.class);
    }

    @Test
    void verifySignatureFileChecksumTaxCode_shouldThrowWhenFileDoesNotExist() {
        File missingFile = tempDir.resolve("missing.p7m").toFile();

        assertThatThrownBy(() -> service.verifySignature(missingFile, "checksum", List.of("CF1")))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void verifySignatureFileChecksumTaxCode_shouldExecuteFullValidationChain() throws IOException {
        // Arrange: create spy to mock createDocumentValidator
        SignatureServiceImpl spyService = spy(new SignatureServiceImpl(
                trustedListsCertificateSource, pagoPaSignatureConfig, padesSignService));
        setField(spyService, "isVerifyEnabled", Boolean.TRUE);
        setField(spyService, "documentService", documentService);

        File testFile = createTempFile("test-content");
        String checksum = "correctChecksum";
        List<String> usersTaxCode = List.of("RSSMRA80A01H501R");

        // Mock validator with signatures
        SignedDocumentValidator mockValidator = mock(SignedDocumentValidator.class);
        AdvancedSignature sig = createMockSignature("sig1", new Date(), SignatureForm.CAdES);
        when(mockValidator.getSignatures()).thenReturn(List.of(sig));

        // Mock original documents with correct checksum
        DSSDocument originalDoc = createMockDocWithDigest(checksum);
        when(mockValidator.getOriginalDocuments("sig1")).thenReturn(List.of(originalDoc));

        // Mock reports with TOTAL_PASSED and valid tax code
        Reports mockReports = mock(Reports.class);
        ValidationReportType etsi = mock(ValidationReportType.class);
        SignatureValidationReportType sigReport = mock(SignatureValidationReportType.class);
        ValidationStatusType statusType = mock(ValidationStatusType.class);
        when(statusType.getMainIndication()).thenReturn(Indication.TOTAL_PASSED);
        when(sigReport.getSignatureValidationStatus()).thenReturn(statusType);
        when(etsi.getSignatureValidationReport()).thenReturn(List.of(sigReport));
        when(mockReports.getEtsiValidationReportJaxb()).thenReturn(etsi);

        // Mock diagnostic data with tax code
        DiagnosticData diagnosticData = mock(DiagnosticData.class);
        CertificateWrapper cert = mock(CertificateWrapper.class);
        when(cert.getSubjectSerialNumber()).thenReturn("TINIT-RSSMRA80A01H501R");
        when(diagnosticData.getUsedCertificates()).thenReturn(List.of(cert));
        when(mockReports.getDiagnosticData()).thenReturn(diagnosticData);

        // Mock validateDocument to return our mock reports
        when(mockValidator.validateDocument()).thenReturn(mockReports);

        // Stub createDocumentValidator to return our mock
        doReturn(mockValidator).when(spyService).createDocumentValidator(any(byte[].class));

        // Act & Assert: should complete without exception
        assertThatCode(() -> spyService.verifySignature(testFile, checksum, usersTaxCode))
                .doesNotThrowAnyException();
    }

    @Test
    void verifySignatureFileChecksumTaxCode_shouldThrowWhenSignatureFormInvalid() throws IOException {
        // Arrange: create spy to mock createDocumentValidator
        SignatureServiceImpl spyService = spy(new SignatureServiceImpl(
                trustedListsCertificateSource, pagoPaSignatureConfig, padesSignService));
        setField(spyService, "isVerifyEnabled", Boolean.TRUE);
        setField(spyService, "documentService", documentService);

        File testFile = createTempFile("test-content");
        String checksum = "correctChecksum";
        List<String> usersTaxCode = List.of("RSSMRA80A01H501R");

        // Mock validator with PAdES signature (invalid form)
        SignedDocumentValidator mockValidator = mock(SignedDocumentValidator.class);
        AdvancedSignature sig = createMockSignature("sig1", new Date(), SignatureForm.PAdES);
        when(mockValidator.getSignatures()).thenReturn(List.of(sig));

        // Mock original documents
        DSSDocument originalDoc = createMockDocWithDigest(checksum);
        when(mockValidator.getOriginalDocuments("sig1")).thenReturn(List.of(originalDoc));

        // Mock reports
        Reports mockReports = mock(Reports.class);
        when(mockValidator.validateDocument()).thenReturn(mockReports);

        // Stub createDocumentValidator
        doReturn(mockValidator).when(spyService).createDocumentValidator(any(byte[].class));

        // Act & Assert: should throw for invalid signature form
        assertThatThrownBy(() -> spyService.verifySignature(testFile, checksum, usersTaxCode))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("PAdES");
    }

    @Test
    void verifySignatureFileChecksumTaxCode_shouldThrowWhenDigestMismatch() throws IOException {
        // Arrange: create spy
        SignatureServiceImpl spyService = spy(new SignatureServiceImpl(
                trustedListsCertificateSource, pagoPaSignatureConfig, padesSignService));
        setField(spyService, "isVerifyEnabled", Boolean.TRUE);
        setField(spyService, "documentService", documentService);

        File testFile = createTempFile("test-content");
        String expectedChecksum = "expectedChecksum";
        List<String> usersTaxCode = List.of("RSSMRA80A01H501R");

        // Mock validator with CAdES signature
        SignedDocumentValidator mockValidator = mock(SignedDocumentValidator.class);
        AdvancedSignature sig = createMockSignature("sig1", new Date(), SignatureForm.CAdES);
        when(mockValidator.getSignatures()).thenReturn(List.of(sig));

        // Mock original documents with WRONG checksum
        DSSDocument originalDoc = createMockDocWithDigest("wrongChecksum");
        when(mockValidator.getOriginalDocuments("sig1")).thenReturn(List.of(originalDoc));

        // Mock reports with TOTAL_PASSED
        Reports mockReports = mock(Reports.class);
        ValidationReportType etsi = mock(ValidationReportType.class);
        SignatureValidationReportType sigReport = mock(SignatureValidationReportType.class);
        ValidationStatusType statusType = mock(ValidationStatusType.class);
        when(statusType.getMainIndication()).thenReturn(Indication.TOTAL_PASSED);
        when(sigReport.getSignatureValidationStatus()).thenReturn(statusType);
        when(etsi.getSignatureValidationReport()).thenReturn(List.of(sigReport));
        when(mockReports.getEtsiValidationReportJaxb()).thenReturn(etsi);
        when(mockValidator.validateDocument()).thenReturn(mockReports);

        // Stub createDocumentValidator
        doReturn(mockValidator).when(spyService).createDocumentValidator(any(byte[].class));

        // Act & Assert: should throw for digest mismatch
        assertThatThrownBy(() -> spyService.verifySignature(testFile, expectedChecksum, usersTaxCode))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void verifySignatureFileChecksumTaxCode_shouldThrowWhenTaxCodeMismatch() throws IOException {
        // Arrange: create spy
        SignatureServiceImpl spyService = spy(new SignatureServiceImpl(
                trustedListsCertificateSource, pagoPaSignatureConfig, padesSignService));
        setField(spyService, "isVerifyEnabled", Boolean.TRUE);
        setField(spyService, "documentService", documentService);

        File testFile = createTempFile("test-content");
        String checksum = "correctChecksum";
        List<String> usersTaxCode = List.of("EXPECTED_TAX_CODE");

        // Mock validator with CAdES signature
        SignedDocumentValidator mockValidator = mock(SignedDocumentValidator.class);
        AdvancedSignature sig = createMockSignature("sig1", new Date(), SignatureForm.CAdES);
        when(mockValidator.getSignatures()).thenReturn(List.of(sig));

        // Mock original documents with correct checksum
        DSSDocument originalDoc = createMockDocWithDigest(checksum);
        when(mockValidator.getOriginalDocuments("sig1")).thenReturn(List.of(originalDoc));

        // Mock reports with TOTAL_PASSED
        Reports mockReports = mock(Reports.class);
        ValidationReportType etsi = mock(ValidationReportType.class);
        SignatureValidationReportType sigReport = mock(SignatureValidationReportType.class);
        ValidationStatusType statusType = mock(ValidationStatusType.class);
        when(statusType.getMainIndication()).thenReturn(Indication.TOTAL_PASSED);
        when(sigReport.getSignatureValidationStatus()).thenReturn(statusType);
        when(etsi.getSignatureValidationReport()).thenReturn(List.of(sigReport));
        when(mockReports.getEtsiValidationReportJaxb()).thenReturn(etsi);

        // Mock diagnostic data with DIFFERENT tax code
        DiagnosticData diagnosticData = mock(DiagnosticData.class);
        CertificateWrapper cert = mock(CertificateWrapper.class);
        when(cert.getSubjectSerialNumber()).thenReturn("TINIT-DIFFERENT_TAX_CODE");
        when(diagnosticData.getUsedCertificates()).thenReturn(List.of(cert));
        when(mockReports.getDiagnosticData()).thenReturn(diagnosticData);

        when(mockValidator.validateDocument()).thenReturn(mockReports);

        // Stub createDocumentValidator
        doReturn(mockValidator).when(spyService).createDocumentValidator(any(byte[].class));

        // Act & Assert: should throw for tax code mismatch
        assertThatThrownBy(() -> spyService.verifySignature(testFile, checksum, usersTaxCode))
                .isInstanceOf(InvalidRequestException.class);
    }

    // ==================== extractOriginalDocument ====================

    @Test
    void extractOriginalDocument_shouldThrowWhenFileNotSigned() throws IOException {
        File invalidFile = createTempFile("not a signed document");

        assertThatThrownBy(() -> SignatureServiceImpl.extractOriginalDocument(invalidFile))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void extractOriginalDocument_shouldSetCertificateVerifierAndThrowWhenNoSignatures() throws IOException {
        File contract = createTempFile("signed-content");
        SignedDocumentValidator validator = mock(SignedDocumentValidator.class);
        when(validator.getSignatures()).thenReturn(Collections.emptyList());

        try (MockedStatic<SignedDocumentValidator> validatorStatic = Mockito.mockStatic(SignedDocumentValidator.class)) {
            validatorStatic.when(() -> SignedDocumentValidator.fromDocument(any(DSSDocument.class)))
                    .thenReturn(validator);

            assertThatThrownBy(() -> SignatureServiceImpl.extractOriginalDocument(contract))
                    .isInstanceOf(InvalidRequestException.class);

            verify(validator).setCertificateVerifier(any());
            verify(validator).getSignatures();
        }
    }

    @Test
    void extractOriginalDocument_shouldThrowWhenOriginalDocumentsEmpty() throws IOException {
        File contract = createTempFile("signed-content");
        SignedDocumentValidator validator = mock(SignedDocumentValidator.class);
        AdvancedSignature signature = mock(AdvancedSignature.class);

        when(signature.getId()).thenReturn("sig-1");
        when(validator.getSignatures()).thenReturn(List.of(signature));
        when(validator.getOriginalDocuments("sig-1")).thenReturn(Collections.emptyList());

        try (MockedStatic<SignedDocumentValidator> validatorStatic = Mockito.mockStatic(SignedDocumentValidator.class)) {
            validatorStatic.when(() -> SignedDocumentValidator.fromDocument(any(DSSDocument.class)))
                    .thenReturn(validator);

            assertThatThrownBy(() -> SignatureServiceImpl.extractOriginalDocument(contract))
                    .isInstanceOf(InvalidRequestException.class);

            verify(validator).setCertificateVerifier(any());
            verify(validator).getSignatures();
            verify(validator).getOriginalDocuments("sig-1");
        }
    }

    @Test
    void extractOriginalDocument_shouldReturnFirstOriginalDocumentWhenPresent() throws IOException {
        File contract = createTempFile("signed-content");
        SignedDocumentValidator validator = mock(SignedDocumentValidator.class);
        AdvancedSignature signature = mock(AdvancedSignature.class);
        DSSDocument expectedOriginal = mock(DSSDocument.class);

        when(signature.getId()).thenReturn("sig-1");
        when(validator.getSignatures()).thenReturn(List.of(signature));
        when(validator.getOriginalDocuments("sig-1")).thenReturn(List.of(expectedOriginal));

        try (MockedStatic<SignedDocumentValidator> validatorStatic = Mockito.mockStatic(SignedDocumentValidator.class)) {
            validatorStatic.when(() -> SignedDocumentValidator.fromDocument(any(DSSDocument.class)))
                    .thenReturn(validator);

            DSSDocument result = SignatureServiceImpl.extractOriginalDocument(contract);

            assertThat(result).isSameAs(expectedOriginal);
            verify(validator).setCertificateVerifier(any());
            verify(validator).getSignatures();
            verify(validator).getOriginalDocuments("sig-1");
        }
    }

    @Test
    void extractOriginalDocument_shouldThrowWhenFileDoesNotExist() {
        File nonExistentFile = tempDir.resolve("nonexistent.p7m").toFile();

        assertThatThrownBy(() -> SignatureServiceImpl.extractOriginalDocument(nonExistentFile))
                .isInstanceOf(InvalidRequestException.class);
    }

    // ==================== extractFile ====================

    @Test
    void extractFile_shouldThrowWhenContractNotSigned() throws IOException {
        File invalidContract = createTempFile("not a signed contract");

        assertThatThrownBy(() -> service.extractFile(invalidContract))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void extractFile_shouldThrowWhenFileDoesNotExist() {
        File nonExistentFile = tempDir.resolve("missing-contract.p7m").toFile();

        assertThatThrownBy(() -> service.extractFile(nonExistentFile))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void extractFile_shouldReturnDestinationWhenOriginalContractPresent() throws IOException {
        File contract = createTempFile("signed-content");
        DSSDocument originalContract = mock(DSSDocument.class);
        byte[] pdfBytes = "pdf-content".getBytes();
        when(originalContract.openStream()).thenReturn(new ByteArrayInputStream(pdfBytes));

        try (MockedStatic<SignatureServiceImpl> signatureServiceImpMockedStatic =
                     Mockito.mockStatic(SignatureServiceImpl.class, Mockito.CALLS_REAL_METHODS)) {
            signatureServiceImpMockedStatic
                    .when(() -> SignatureServiceImpl.extractOriginalDocument(contract))
                    .thenReturn(originalContract);

            File result = service.extractFile(contract);

            assertThat(result).isNotNull();
            assertThat(result.getAbsolutePath()).isEqualTo(contract.getAbsolutePath() + ".pdf");
            assertThat(result).exists();

            Files.deleteIfExists(result.toPath());
        }
    }

    @Test
    void extractFile_shouldThrowResourceNotFoundWhenOriginalContractNull() throws IOException {
        File contract = createTempFile("signed-content");

        try (MockedStatic<SignatureServiceImpl> signatureServiceImpMockedStatic =
                     Mockito.mockStatic(SignatureServiceImpl.class, Mockito.CALLS_REAL_METHODS)) {
            signatureServiceImpMockedStatic
                    .when(() -> SignatureServiceImpl.extractOriginalDocument(contract))
                    .thenReturn(null);

            assertThatThrownBy(() -> service.extractFile(contract))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Test
    void extractFile_shouldThrowResourceNotFoundWhenOpenStreamFails() throws IOException {
        File contract = createTempFile("signed-content");
        DSSDocument originalContract = mock(DSSDocument.class);
        when(originalContract.openStream()).thenThrow(new RuntimeException("stream-failure"));

        try (MockedStatic<SignatureServiceImpl> signatureServiceImpMockedStatic =
                     Mockito.mockStatic(SignatureServiceImpl.class, Mockito.CALLS_REAL_METHODS)) {
            signatureServiceImpMockedStatic
                    .when(() -> SignatureServiceImpl.extractOriginalDocument(contract))
                    .thenReturn(originalContract);

            assertThatThrownBy(() -> service.extractFile(contract))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
