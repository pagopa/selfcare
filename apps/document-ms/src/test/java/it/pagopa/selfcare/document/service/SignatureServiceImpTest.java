package it.pagopa.selfcare.document.service;

import eu.europa.esig.dss.diagnostic.CertificateWrapper;
import eu.europa.esig.dss.diagnostic.DiagnosticData;
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
import it.pagopa.selfcare.document.model.FormItem;
import it.pagopa.selfcare.document.model.entity.Document;
import it.pagopa.selfcare.document.service.impl.SignatureServiceImp;
import it.pagopa.selfcare.onboarding.crypto.PadesSignService;
import it.pagopa.selfcare.onboarding.crypto.entity.SignatureInformation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
@DisplayName("SignatureServiceImp - Test Suite Completa")
class SignatureServiceImpTest {

    private TrustedListsCertificateSource trustedListsCertificateSource;
    private PagoPaSignatureConfig pagoPaSignatureConfig;
    private PadesSignService padesSignService;

    @InjectMock
    DocumentService documentService;

    private SignatureServiceImp service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        trustedListsCertificateSource = Mockito.mock(TrustedListsCertificateSource.class);
        pagoPaSignatureConfig = Mockito.mock(PagoPaSignatureConfig.class);
        padesSignService = Mockito.mock(PadesSignService.class);

        service = new SignatureServiceImp(trustedListsCertificateSource, pagoPaSignatureConfig, padesSignService);
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

    // ==================== isDocumentSigned ====================

    @Nested
    @DisplayName("isDocumentSigned")
    class IsDocumentSignedTests {

        @Test
        @DisplayName("Should throw InvalidRequestException when no signatures present")
        void shouldThrowWhenNoSignatures() {
            SignedDocumentValidator validator = createMockValidator(Collections.emptyList());

            assertThatThrownBy(() -> service.isDocumentSigned(validator))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Should not throw when signatures are present")
        void shouldNotThrowWhenSignaturesPresent() {
            AdvancedSignature sig = createMockSignature("sig1", new Date(), SignatureForm.CAdES);
            SignedDocumentValidator validator = createMockValidator(List.of(sig));

            assertThatCode(() -> service.isDocumentSigned(validator)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should not throw when multiple signatures are present")
        void shouldNotThrowWhenMultipleSignatures() {
            List<AdvancedSignature> sigs = List.of(
                    createMockSignature("sig1", new Date(), SignatureForm.CAdES),
                    createMockSignature("sig2", new Date(), SignatureForm.PAdES)
            );
            SignedDocumentValidator validator = createMockValidator(sigs);

            assertThatCode(() -> service.isDocumentSigned(validator)).doesNotThrowAnyException();
        }
    }

    // ==================== verifyOriginalDocument ====================

    @Nested
    @DisplayName("verifyOriginalDocument")
    class VerifyOriginalDocumentTests {

        @Test
        @DisplayName("Should throw when signatures list is null")
        void shouldThrowWhenSignaturesNull() {
            SignedDocumentValidator validator = createMockValidator(null);

            assertThatThrownBy(() -> service.verifyOriginalDocument(validator))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Should throw when signatures list is empty")
        void shouldThrowWhenSignaturesEmpty() {
            SignedDocumentValidator validator = createMockValidator(Collections.emptyList());

            assertThatThrownBy(() -> service.verifyOriginalDocument(validator))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Should throw when no original documents found")
        void shouldThrowWhenNoOriginalDocuments() {
            AdvancedSignature sig = createMockSignature("sig1", new Date(), SignatureForm.CAdES);
            SignedDocumentValidator validator = createMockValidator(List.of(sig));
            when(validator.getOriginalDocuments("sig1")).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> service.verifyOriginalDocument(validator))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Should throw when getOriginalDocuments returns null")
        void shouldThrowWhenOriginalDocumentsNull() {
            AdvancedSignature sig = createMockSignature("sig1", new Date(), SignatureForm.CAdES);
            SignedDocumentValidator validator = createMockValidator(List.of(sig));
            when(validator.getOriginalDocuments("sig1")).thenReturn(null);

            assertThatThrownBy(() -> service.verifyOriginalDocument(validator))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Should not throw when original documents are present")
        void shouldNotThrowWhenOriginalDocumentsPresent() {
            AdvancedSignature sig = createMockSignature("sig1", new Date(), SignatureForm.CAdES);
            SignedDocumentValidator validator = createMockValidator(List.of(sig));
            when(validator.getOriginalDocuments("sig1")).thenReturn(List.of(mock(DSSDocument.class)));

            assertThatCode(() -> service.verifyOriginalDocument(validator)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should not throw when at least one signature has original documents")
        void shouldNotThrowWhenAtLeastOneHasOriginals() {
            AdvancedSignature sig1 = createMockSignature("sig1", new Date(), SignatureForm.CAdES);
            AdvancedSignature sig2 = createMockSignature("sig2", new Date(), SignatureForm.CAdES);
            SignedDocumentValidator validator = createMockValidator(List.of(sig1, sig2));
            when(validator.getOriginalDocuments("sig1")).thenReturn(Collections.emptyList());
            when(validator.getOriginalDocuments("sig2")).thenReturn(List.of(mock(DSSDocument.class)));

            assertThatCode(() -> service.verifyOriginalDocument(validator)).doesNotThrowAnyException();
        }
    }

    // ==================== verifySignatureForm ====================

    @Nested
    @DisplayName("verifySignatureForm")
    class VerifySignatureFormTests {

        @Test
        @DisplayName("Should not throw when all signatures are CAdES")
        void shouldNotThrowWhenAllCAdES() {
            List<AdvancedSignature> sigs = List.of(
                    createMockSignature("sig1", new Date(), SignatureForm.CAdES),
                    createMockSignature("sig2", new Date(), SignatureForm.CAdES)
            );
            SignedDocumentValidator validator = createMockValidator(sigs);

            assertThatCode(() -> service.verifySignatureForm(validator)).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @EnumSource(value = SignatureForm.class, names = {"PAdES", "XAdES", "JAdES"})
        @DisplayName("Should throw when signature form is not CAdES")
        void shouldThrowForNonCAdESForm(SignatureForm invalidForm) {
            AdvancedSignature sig = createMockSignature("sig1", new Date(), invalidForm);
            SignedDocumentValidator validator = createMockValidator(List.of(sig));

            assertThatThrownBy(() -> service.verifySignatureForm(validator))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining(invalidForm.toString());
        }

        @Test
        @DisplayName("Should throw listing all invalid forms")
        void shouldThrowListingAllInvalidForms() {
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
    }

    // ==================== verifySignature(Reports) ====================

    @Nested
    @DisplayName("verifySignature(Reports)")
    class VerifySignatureReportsTests {

        @Test
        @DisplayName("Should not throw when TOTAL_PASSED")
        void shouldNotThrowWhenTotalPassed() {
            Reports reports = createMockReports(Indication.TOTAL_PASSED);

            assertThatCode(() -> service.verifySignature(reports)).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @EnumSource(value = Indication.class, names = {"INDETERMINATE", "TOTAL_FAILED", "FAILED", "PASSED"})
        @DisplayName("Should throw for non-TOTAL_PASSED indications")
        void shouldThrowForNonTotalPassedIndications(Indication indication) {
            Reports reports = createMockReports(indication);

            assertThatThrownBy(() -> service.verifySignature(reports))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Should throw when ETSI report is null")
        void shouldThrowWhenEtsiReportNull() {
            Reports reports = mock(Reports.class);
            when(reports.getEtsiValidationReportJaxb()).thenReturn(null);

            assertThatThrownBy(() -> service.verifySignature(reports))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Should throw when signature validation reports list is empty")
        void shouldThrowWhenReportsListEmpty() {
            Reports reports = mock(Reports.class);
            ValidationReportType etsi = mock(ValidationReportType.class);
            when(etsi.getSignatureValidationReport()).thenReturn(Collections.emptyList());
            when(reports.getEtsiValidationReportJaxb()).thenReturn(etsi);

            assertThatThrownBy(() -> service.verifySignature(reports))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Should throw when validation status is null")
        void shouldThrowWhenValidationStatusNull() {
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
        @DisplayName("Should throw when any report is not TOTAL_PASSED")
        void shouldThrowWhenAnyReportNotTotalPassed() {
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
        @DisplayName("Should not throw when all reports are TOTAL_PASSED")
        void shouldNotThrowWhenAllReportsTotalPassed() {
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
    }

    // ==================== checkSignature(Reports) ====================

    @Nested
    @DisplayName("checkSignature(Reports)")
    class CheckSignatureTests {

        @Test
        @DisplayName("Should not throw when validation status is present")
        void shouldNotThrowWhenStatusPresent() {
            Reports reports = createMockReports(Indication.TOTAL_PASSED);

            assertThatCode(() -> service.checkSignature(reports)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should throw when ETSI report is null")
        void shouldThrowWhenEtsiNull() {
            Reports reports = mock(Reports.class);
            when(reports.getEtsiValidationReportJaxb()).thenReturn(null);

            assertThatThrownBy(() -> service.checkSignature(reports))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Should throw when reports list is empty")
        void shouldThrowWhenReportsEmpty() {
            Reports reports = mock(Reports.class);
            ValidationReportType etsi = mock(ValidationReportType.class);
            when(etsi.getSignatureValidationReport()).thenReturn(Collections.emptyList());
            when(reports.getEtsiValidationReportJaxb()).thenReturn(etsi);

            assertThatThrownBy(() -> service.checkSignature(reports))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Should throw when validation status is null")
        void shouldThrowWhenStatusNull() {
            Reports reports = mock(Reports.class);
            ValidationReportType etsi = mock(ValidationReportType.class);
            SignatureValidationReportType sigReport = mock(SignatureValidationReportType.class);
            when(sigReport.getSignatureValidationStatus()).thenReturn(null);
            when(etsi.getSignatureValidationReport()).thenReturn(List.of(sigReport));
            when(reports.getEtsiValidationReportJaxb()).thenReturn(etsi);

            assertThatThrownBy(() -> service.checkSignature(reports))
                    .isInstanceOf(InvalidRequestException.class);
        }
    }

    // ==================== verifyDigest ====================

    @Nested
    @DisplayName("verifyDigest")
    class VerifyDigestTests {

        @Test
        @DisplayName("Should not throw when digest matches")
        void shouldNotThrowWhenDigestMatches() {
            String checksum = "correctChecksum";
            DSSDocument doc = createMockDocWithDigest(checksum);
            AdvancedSignature sig = createMockSignature("sig1", new Date(), SignatureForm.CAdES);
            SignedDocumentValidator validator = createMockValidator(List.of(sig));
            when(validator.getOriginalDocuments("sig1")).thenReturn(List.of(doc));

            assertThatCode(() -> service.verifyDigest(validator, checksum)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should throw when digest does not match")
        void shouldThrowWhenDigestMismatch() {
            DSSDocument doc = createMockDocWithDigest("wrongChecksum");
            AdvancedSignature sig = createMockSignature("sig1", new Date(), SignatureForm.CAdES);
            SignedDocumentValidator validator = createMockValidator(List.of(sig));
            when(validator.getOriginalDocuments("sig1")).thenReturn(List.of(doc));

            assertThatThrownBy(() -> service.verifyDigest(validator, "expectedChecksum"))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Should not throw when signatures list is null")
        void shouldNotThrowWhenSignaturesNull() {
            SignedDocumentValidator validator = createMockValidator(null);

            assertThatCode(() -> service.verifyDigest(validator, "anyChecksum")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should not throw when signatures list is empty")
        void shouldNotThrowWhenSignaturesEmpty() {
            SignedDocumentValidator validator = createMockValidator(Collections.emptyList());

            assertThatCode(() -> service.verifyDigest(validator, "anyChecksum")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should throw when any signature has wrong digest")
        void shouldThrowWhenAnySignatureHasWrongDigest() {
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
        @DisplayName("Should not throw when all signatures have correct digest")
        void shouldNotThrowWhenAllSignaturesHaveCorrectDigest() {
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
    }

    // ==================== verifyManagerTaxCode ====================

    @Nested
    @DisplayName("verifyManagerTaxCode")
    class VerifyManagerTaxCodeTests {

        @Test
        @DisplayName("Should not throw when tax code matches")
        void shouldNotThrowWhenTaxCodeMatches() {
            Reports reports = createMockReportsWithCertificates(List.of("TINIT-RSSMRA80A01H501R"));

            assertThatCode(() -> service.verifyManagerTaxCode(reports, List.of("RSSMRA80A01H501R")))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should throw when diagnostic data is null")
        void shouldThrowWhenDiagnosticDataNull() {
            Reports reports = mock(Reports.class);
            when(reports.getDiagnosticData()).thenReturn(null);

            assertThatThrownBy(() -> service.verifyManagerTaxCode(reports, List.of("RSSMRA80A01H501R")))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Should throw when used certificates is null")
        void shouldThrowWhenUsedCertificatesNull() {
            Reports reports = mock(Reports.class);
            DiagnosticData diagnosticData = mock(DiagnosticData.class);
            when(diagnosticData.getUsedCertificates()).thenReturn(null);
            when(reports.getDiagnosticData()).thenReturn(diagnosticData);

            assertThatThrownBy(() -> service.verifyManagerTaxCode(reports, List.of("RSSMRA80A01H501R")))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Should throw when used certificates is empty")
        void shouldThrowWhenUsedCertificatesEmpty() {
            Reports reports = mock(Reports.class);
            DiagnosticData diagnosticData = mock(DiagnosticData.class);
            when(diagnosticData.getUsedCertificates()).thenReturn(Collections.emptyList());
            when(reports.getDiagnosticData()).thenReturn(diagnosticData);

            assertThatThrownBy(() -> service.verifyManagerTaxCode(reports, List.of("RSSMRA80A01H501R")))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Should throw when no TINIT prefix match")
        void shouldThrowWhenNoTinitPrefix() {
            Reports reports = createMockReportsWithCertificates(List.of("NO_MATCH_PREFIX"));

            assertThatThrownBy(() -> service.verifyManagerTaxCode(reports, List.of("RSSMRA80A01H501R")))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Should throw when tax code not in user list")
        void shouldThrowWhenTaxCodeNotInUserList() {
            Reports reports = createMockReportsWithCertificates(List.of("TINIT-DIFFERENT"));

            assertThatThrownBy(() -> service.verifyManagerTaxCode(reports, List.of("RSSMRA80A01H501R")))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Should throw when users tax code list is empty")
        void shouldThrowWhenUsersTaxCodeEmpty() {
            Reports reports = createMockReportsWithCertificates(List.of("TINIT-RSSMRA80A01H501R"));

            assertThatThrownBy(() -> service.verifyManagerTaxCode(reports, Collections.emptyList()))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Should not throw when multiple signers cover all users")
        void shouldNotThrowWhenMultipleSignersCoverAllUsers() {
            Reports reports = createMockReportsWithCertificates(List.of("TINIT-AAA111", "TINIT-BBB222"));

            assertThatCode(() -> service.verifyManagerTaxCode(reports, List.of("AAA111", "BBB222")))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should throw when only some users are covered")
        void shouldThrowWhenOnlySomeUsersCovered() {
            Reports reports = createMockReportsWithCertificates(List.of("TINIT-AAA111"));

            assertThatThrownBy(() -> service.verifyManagerTaxCode(reports, List.of("AAA111", "BBB222")))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Should ignore null serial numbers")
        void shouldIgnoreNullSerialNumbers() {
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
        @DisplayName("Should ignore empty serial numbers")
        void shouldIgnoreEmptySerialNumbers() {
            Reports reports = mock(Reports.class);
            DiagnosticData diagnosticData = mock(DiagnosticData.class);
            CertificateWrapper cert = mock(CertificateWrapper.class);
            when(cert.getSubjectSerialNumber()).thenReturn("");
            when(diagnosticData.getUsedCertificates()).thenReturn(List.of(cert));
            when(reports.getDiagnosticData()).thenReturn(diagnosticData);

            assertThatThrownBy(() -> service.verifyManagerTaxCode(reports, List.of("RSSMRA80A01H501R")))
                    .isInstanceOf(InvalidRequestException.class);
        }
    }

    // ==================== isSignatureVerificationEnabled ====================

    @Nested
    @DisplayName("isSignatureVerificationEnabled")
    class IsSignatureVerificationEnabledTests {

        @Test
        @DisplayName("Should return true when enabled")
        void shouldReturnTrueWhenEnabled() {
            setField(service, "isVerifyEnabled", Boolean.TRUE);
            assertTrue(service.isSignatureVerificationEnabled());
        }

        @Test
        @DisplayName("Should return false when disabled")
        void shouldReturnFalseWhenDisabled() {
            setField(service, "isVerifyEnabled", Boolean.FALSE);
            assertFalse(service.isSignatureVerificationEnabled());
        }

        @Test
        @DisplayName("Should return false when null")
        void shouldReturnFalseWhenNull() {
            setField(service, "isVerifyEnabled", null);
            assertFalse(service.isSignatureVerificationEnabled());
        }
    }

    // ==================== verifyContractSignature ====================

    @Nested
    @DisplayName("verifyContractSignature")
    class VerifyContractSignatureTests {

        @Test
        @DisplayName("Should return immediately when verification disabled")
        void shouldReturnImmediatelyWhenDisabled() throws IOException {
            setField(service, "isVerifyEnabled", Boolean.FALSE);
            File file = createTempFile("test");

            Uni<Void> result = service.verifyContractSignature("onboarding-id", file, List.of("CF1"));

            assertThatCode(() -> result.await().indefinitely()).doesNotThrowAnyException();
            verifyNoInteractions(documentService);
        }

        @Test
        @DisplayName("Should call documentService when verification enabled")
        void shouldCallDocumentServiceWhenEnabled() throws IOException {
            setField(service, "isVerifyEnabled", Boolean.TRUE);
            Document document = mock(Document.class);
            when(document.getChecksum()).thenReturn("checksum");
            when(documentService.getDocumentById("onboarding-id")).thenReturn(Uni.createFrom().item(document));

            File file = createTempFile("test");
            Uni<Void> result = service.verifyContractSignature("onboarding-id", file, List.of("CF1"));

            assertThatThrownBy(() -> result.await().indefinitely()).isInstanceOf(Exception.class);
            verify(documentService).getDocumentById("onboarding-id");
        }

        @Test
        @DisplayName("Should propagate exception when documentService fails")
        void shouldPropagateExceptionWhenDocumentServiceFails() throws IOException {
            setField(service, "isVerifyEnabled", Boolean.TRUE);
            when(documentService.getDocumentById("onboarding-id"))
                    .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));

            File file = createTempFile("test");
            Uni<Void> result = service.verifyContractSignature("onboarding-id", file, List.of("CF1"));

            assertThatThrownBy(() -> result.await().indefinitely())
                    .hasMessageContaining("DB error");
            verify(documentService).getDocumentById("onboarding-id");
        }
    }

    // ==================== chooseEarliestSignature ====================

    @Nested
    @DisplayName("chooseEarliestSignature")
    class ChooseEarliestSignatureTests {

        @Test
        @DisplayName("Should return earliest by signing time")
        void shouldReturnEarliestBySigningTime() {
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
        @DisplayName("Should fallback to id when same signing time")
        void shouldFallbackToIdWhenSameTime() {
            Date sameDate = new Date();
            List<AdvancedSignature> sigs = List.of(
                    createMockSignature("sig-B", sameDate, SignatureForm.CAdES),
                    createMockSignature("sig-A", sameDate, SignatureForm.CAdES)
            );

            AdvancedSignature result = service.chooseEarliestSignature(sigs);

            assertThat(result.getId()).isEqualTo("sig-A");
        }

        @Test
        @DisplayName("Should handle null signing time (null-last)")
        void shouldHandleNullSigningTime() {
            Date realDate = new Date();
            List<AdvancedSignature> sigs = List.of(
                    createMockSignature("sig-null", null, SignatureForm.CAdES),
                    createMockSignature("sig-real", realDate, SignatureForm.CAdES)
            );

            AdvancedSignature result = service.chooseEarliestSignature(sigs);

            assertThat(result.getId()).isEqualTo("sig-real");
        }

        @Test
        @DisplayName("Should return single signature")
        void shouldReturnSingleSignature() {
            AdvancedSignature sig = createMockSignature("only-sig", new Date(), SignatureForm.CAdES);

            AdvancedSignature result = service.chooseEarliestSignature(List.of(sig));

            assertThat(result.getId()).isEqualTo("only-sig");
        }

        @Test
        @DisplayName("Should handle all null signing times")
        void shouldHandleAllNullSigningTimes() {
            List<AdvancedSignature> sigs = List.of(
                    createMockSignature("sig-B", null, SignatureForm.CAdES),
                    createMockSignature("sig-A", null, SignatureForm.CAdES)
            );

            AdvancedSignature result = service.chooseEarliestSignature(sigs);

            assertThat(result.getId()).isEqualTo("sig-A");
        }
    }

    // ==================== extractPdfFromSignedContainer ====================

    @Nested
    @DisplayName("extractPdfFromSignedContainer")
    class ExtractPdfFromSignedContainerTests {

        @Test
        @DisplayName("Should return original document when present")
        void shouldReturnOriginalDocument() {
            DSSDocument inputDoc = mock(DSSDocument.class);
            DSSDocument originalDoc = mock(DSSDocument.class);
            AdvancedSignature sig = createMockSignature("sig1", new Date(), SignatureForm.CAdES);
            SignedDocumentValidator validator = createMockValidator(List.of(sig));
            when(validator.getOriginalDocuments("sig1")).thenReturn(List.of(originalDoc));

            DSSDocument result = service.extractPdfFromSignedContainer(validator, inputDoc);

            assertThat(result).isSameAs(originalDoc);
        }

        @Test
        @DisplayName("Should return input document when signatures null")
        void shouldReturnInputWhenSignaturesNull() {
            DSSDocument inputDoc = mock(DSSDocument.class);
            SignedDocumentValidator validator = createMockValidator(null);

            DSSDocument result = service.extractPdfFromSignedContainer(validator, inputDoc);

            assertThat(result).isSameAs(inputDoc);
        }

        @Test
        @DisplayName("Should return input document when signatures empty")
        void shouldReturnInputWhenSignaturesEmpty() {
            DSSDocument inputDoc = mock(DSSDocument.class);
            SignedDocumentValidator validator = createMockValidator(Collections.emptyList());

            DSSDocument result = service.extractPdfFromSignedContainer(validator, inputDoc);

            assertThat(result).isSameAs(inputDoc);
        }

        @Test
        @DisplayName("Should return input document when no originals")
        void shouldReturnInputWhenNoOriginals() {
            DSSDocument inputDoc = mock(DSSDocument.class);
            AdvancedSignature sig = createMockSignature("sig1", new Date(), SignatureForm.CAdES);
            SignedDocumentValidator validator = createMockValidator(List.of(sig));
            when(validator.getOriginalDocuments("sig1")).thenReturn(Collections.emptyList());

            DSSDocument result = service.extractPdfFromSignedContainer(validator, inputDoc);

            assertThat(result).isSameAs(inputDoc);
        }

        @Test
        @DisplayName("Should return input document when originals null")
        void shouldReturnInputWhenOriginalsNull() {
            DSSDocument inputDoc = mock(DSSDocument.class);
            AdvancedSignature sig = createMockSignature("sig1", new Date(), SignatureForm.CAdES);
            SignedDocumentValidator validator = createMockValidator(List.of(sig));
            when(validator.getOriginalDocuments("sig1")).thenReturn(null);

            DSSDocument result = service.extractPdfFromSignedContainer(validator, inputDoc);

            assertThat(result).isSameAs(inputDoc);
        }
    }

    // ==================== computeDigestOfSignedRevision ====================

    @Nested
    @DisplayName("computeDigestOfSignedRevision")
    class ComputeDigestOfSignedRevisionTests {

        @Test
        @DisplayName("Should compute full doc digest when no signatures")
        void shouldComputeFullDocDigestWhenNoSignatures() {
            DSSDocument doc = createMockDocWithDigest("fullDocDigest");
            SignedDocumentValidator validator = createMockValidator(Collections.emptyList());

            String result = service.computeDigestOfSignedRevision(validator, doc);

            assertThat(result).isEqualTo("fullDocDigest");
        }

        @Test
        @DisplayName("Should compute full doc digest when signatures null")
        void shouldComputeFullDocDigestWhenSignaturesNull() {
            DSSDocument doc = createMockDocWithDigest("fullDocDigest");
            SignedDocumentValidator validator = createMockValidator(null);

            String result = service.computeDigestOfSignedRevision(validator, doc);

            assertThat(result).isEqualTo("fullDocDigest");
        }

        @Test
        @DisplayName("Should compute digest of original document")
        void shouldComputeDigestOfOriginalDocument() {
            DSSDocument inputDoc = mock(DSSDocument.class);
            DSSDocument originalDoc = createMockDocWithDigest("originalDigest");
            AdvancedSignature sig = createMockSignature("sig1", new Date(), SignatureForm.CAdES);
            SignedDocumentValidator validator = createMockValidator(List.of(sig));
            when(validator.getOriginalDocuments("sig1")).thenReturn(List.of(originalDoc));

            String result = service.computeDigestOfSignedRevision(validator, inputDoc);

            assertThat(result).isEqualTo("originalDigest");
        }

        @Test
        @DisplayName("Should compute digest of input doc when no originals")
        void shouldComputeDigestOfInputDocWhenNoOriginals() {
            DSSDocument inputDoc = createMockDocWithDigest("inputDigest");
            AdvancedSignature sig = createMockSignature("sig1", new Date(), SignatureForm.CAdES);
            SignedDocumentValidator validator = createMockValidator(List.of(sig));
            when(validator.getOriginalDocuments("sig1")).thenReturn(Collections.emptyList());

            String result = service.computeDigestOfSignedRevision(validator, inputDoc);

            assertThat(result).isEqualTo("inputDigest");
        }
    }

    // ==================== verifyUploadedFileDigest ====================

    @Nested
    @DisplayName("verifyUploadedFileDigest")
    class VerifyUploadedFileDigestTests {

        @Test
        @DisplayName("Should throw NullPointerException when FormItem is null")
        void shouldThrowWhenFormItemNull() {
            assertThatThrownBy(() -> service.verifyUploadedFileDigest(null, "digest", false))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should throw NullPointerException when templateDigest is null")
        void shouldThrowWhenTemplateDigestNull() throws IOException {
            File file = createTempFile("content");
            FormItem formItem = FormItem.builder().file(file).build();

            assertThatThrownBy(() -> service.verifyUploadedFileDigest(formItem, null, false))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // ==================== signDocument ====================

    @Nested
    @DisplayName("signDocument")
    class SignDocumentTests {

        @Test
        @DisplayName("Should return original file when signature disabled")
        void shouldReturnOriginalFileWhenDisabled() throws Exception {
            when(pagoPaSignatureConfig.source()).thenReturn("disabled");
            File pdf = createTempFile("content");

            File result = service.signDocument(pdf, "Institution", "product").await().indefinitely();

            assertThat(result).isEqualTo(pdf);
            verifyNoInteractions(padesSignService);
        }

        @Test
        @DisplayName("Should call padesSignService when enabled")
        void shouldCallPadesSignServiceWhenEnabled() throws Exception {
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
        @DisplayName("Should interpolate placeholders in reason")
        void shouldInterpolatePlaceholders() throws Exception {
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
        @DisplayName("Should throw when padesSignService fails")
        void shouldThrowWhenPadesSignServiceFails() throws Exception {
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
    }

    // ==================== createSafeTempFile / createTempFileWithPosix ====================

    @Nested
    @DisplayName("createSafeTempFile and createTempFileWithPosix")
    class CreateTempFileTests {

        @Test
        @DisplayName("createTempFileWithPosix should create file with .pdf extension")
        void shouldCreateFileWithPdfExtension() throws IOException {
            Path tempFile = service.createTempFileWithPosix();

            assertThat(tempFile).isNotNull();
            assertThat(tempFile.toFile()).exists();
            assertThat(tempFile.toString()).endsWith(".pdf");

            Files.deleteIfExists(tempFile);
        }

        @Test
        @DisplayName("createSafeTempFile should return valid path")
        void shouldReturnValidPath() throws IOException {
            Path result = service.createSafeTempFile();

            assertThat(result).isNotNull();
            assertThat(result.toFile()).exists();

            Files.deleteIfExists(result);
        }
    }

    // ==================== validateDocument ====================

    @Nested
    @DisplayName("validateDocument")
    class ValidateDocumentTests {

        @Test
        @DisplayName("Should throw InvalidRequestException on validation failure")
        void shouldThrowOnValidationFailure() {
            SignedDocumentValidator validator = mock(SignedDocumentValidator.class);
            when(validator.validateDocument()).thenThrow(new RuntimeException("validation failed"));

            assertThatThrownBy(() -> service.validateDocument(validator))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Should return reports on success")
        void shouldReturnReportsOnSuccess() {
            SignedDocumentValidator validator = mock(SignedDocumentValidator.class);
            Reports expectedReports = mock(Reports.class);
            when(validator.validateDocument()).thenReturn(expectedReports);

            Reports result = service.validateDocument(validator);

            assertThat(result).isSameAs(expectedReports);
        }
    }

    // ==================== verifySignature(File) ====================

    @Nested
    @DisplayName("verifySignature(File)")
    class VerifySignatureFileTests {

        @Test
        @DisplayName("Should throw when file is not signed document")
        void shouldThrowWhenNotSignedDocument() throws IOException {
            File invalidFile = createTempFile("not-a-signed-document");

            assertThatThrownBy(() -> service.verifySignature(invalidFile))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Should throw when file does not exist")
        void shouldThrowWhenFileDoesNotExist() {
            File nonExistentFile = tempDir.resolve("nonexistent.pdf").toFile();

            assertThatThrownBy(() -> service.verifySignature(nonExistentFile))
                    .isInstanceOf(InvalidRequestException.class);
        }
    }

    // ==================== verifySignature(File, String, List) ====================

    @Nested
    @DisplayName("verifySignature(File, String, List)")
    class VerifySignatureFileChecksumTaxCodeTests {

        @Test
        @DisplayName("Should throw when file contains invalid data")
        void shouldThrowWhenInvalidData() throws IOException {
            File invalidFile = createTempFile("garbage");

            assertThatThrownBy(() -> service.verifySignature(invalidFile, "checksum", List.of("CF1")))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Should rethrow InvalidRequestException directly")
        void shouldRethrowInvalidRequestExceptionDirectly() throws IOException {
            File invalidFile = createTempFile("garbage-data");

            assertThatThrownBy(() -> service.verifySignature(invalidFile, "checksum", List.of("CF1")))
                    .isExactlyInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Should throw when file does not exist")
        void shouldThrowWhenFileDoesNotExist() {
            File missingFile = tempDir.resolve("missing.p7m").toFile();

            assertThatThrownBy(() -> service.verifySignature(missingFile, "checksum", List.of("CF1")))
                    .isInstanceOf(InvalidRequestException.class);
        }
    }

    // ==================== extractOriginalDocument ====================

    @Nested
    @DisplayName("extractOriginalDocument")
    class ExtractOriginalDocumentTests {

        @Test
        @DisplayName("Should throw when file is not signed")
        void shouldThrowWhenFileNotSigned() throws IOException {
            File invalidFile = createTempFile("not a signed document");

            assertThatThrownBy(() -> SignatureServiceImp.extractOriginalDocument(invalidFile))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Should throw when file does not exist")
        void shouldThrowWhenFileDoesNotExist() {
            File nonExistentFile = tempDir.resolve("nonexistent.p7m").toFile();

            assertThatThrownBy(() -> SignatureServiceImp.extractOriginalDocument(nonExistentFile))
                    .isInstanceOf(InvalidRequestException.class);
        }
    }

    // ==================== extractFile ====================

    @Nested
    @DisplayName("extractFile")
    class ExtractFileTests {

        @Test
        @DisplayName("Should throw when contract is not signed")
        void shouldThrowWhenContractNotSigned() throws IOException {
            File invalidContract = createTempFile("not a signed contract");

            assertThatThrownBy(() -> service.extractFile(invalidContract))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Should throw when file does not exist")
        void shouldThrowWhenFileDoesNotExist() {
            File nonExistentFile = tempDir.resolve("missing-contract.p7m").toFile();

            assertThatThrownBy(() -> service.extractFile(nonExistentFile))
                    .isInstanceOf(InvalidRequestException.class);
        }
    }
}
