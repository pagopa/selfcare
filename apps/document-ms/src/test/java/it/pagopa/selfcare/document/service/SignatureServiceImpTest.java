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
import eu.europa.esig.validationreport.jaxb.ValidationStatusType;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.document.config.PagoPaSignatureConfig;
import it.pagopa.selfcare.document.exception.InvalidRequestException;
import it.pagopa.selfcare.document.model.FormItem;
import it.pagopa.selfcare.document.model.entity.Document;
import it.pagopa.selfcare.document.service.impl.SignatureServiceImp;
import it.pagopa.selfcare.onboarding.crypto.PadesSignService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SignatureServiceImp - Test Unitari")
class SignatureServiceImpTest {

    @Mock
    private TrustedListsCertificateSource trustedListsCertificateSource;

    @Mock
    private PagoPaSignatureConfig pagoPaSignatureConfig;

    @Mock
    private PadesSignService padesSignService;

    @Mock
    private DocumentService documentService;

    // Iniettato manualmente per gestire il campo @ConfigProperty
    private SignatureServiceImp service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        service = new SignatureServiceImp(trustedListsCertificateSource, pagoPaSignatureConfig, padesSignService);
        // Imposta il campo isVerifyEnabled tramite reflection (CDI @ConfigProperty)
        setField(service, "isVerifyEnabled", Boolean.TRUE);
        // Inietta il documentService tramite reflection
        setField(service, "documentService", documentService);
    }

    // ------------------------------------------------------------------ utility
    private static void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private AdvancedSignature mockSignature(String id, Date signingTime, SignatureForm form) {
        AdvancedSignature sig = mock(AdvancedSignature.class);
        lenient().when(sig.getId()).thenReturn(id);
        lenient().when(sig.getSigningTime()).thenReturn(signingTime);
        lenient().when(sig.getSignatureForm()).thenReturn(form);
        return sig;
    }

    private Reports mockReports(Indication indication) {
        Reports reports = mock(Reports.class);
        eu.europa.esig.validationreport.jaxb.ValidationReportType etsi =
                mock(eu.europa.esig.validationreport.jaxb.ValidationReportType.class);

        SignatureValidationReportType sigReport = mock(SignatureValidationReportType.class);
        ValidationStatusType statusType = mock(ValidationStatusType.class);
        when(statusType.getMainIndication()).thenReturn(indication);
        when(sigReport.getSignatureValidationStatus()).thenReturn(statusType);
        when(etsi.getSignatureValidationReport()).thenReturn(List.of(sigReport));
        when(reports.getEtsiValidationReportJaxb()).thenReturn(etsi);
        return reports;
    }

    // ===================================================
    // isDocumentSigned
    // ===================================================
    @Nested
    @DisplayName("isDocumentSigned")
    class IsDocumentSignedTest {

        @Test
        @DisplayName("Lancia InvalidRequestException se non ci sono firme")
        void shouldThrowWhenNoSignatures() {
            SignedDocumentValidator validator = mock(SignedDocumentValidator.class);
            when(validator.getSignatures()).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> service.isDocumentSigned(validator))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Non lancia eccezione se ci sono firme")
        void shouldNotThrowWhenSignaturesPresent() {
            SignedDocumentValidator validator = mock(SignedDocumentValidator.class);
            AdvancedSignature sig = mockSignature("sig1", new Date(), SignatureForm.CAdES);
            when(validator.getSignatures()).thenReturn(List.of(sig));

            assertThatCode(() -> service.isDocumentSigned(validator)).doesNotThrowAnyException();
        }
    }

    // ===================================================
    // verifyOriginalDocument
    // ===================================================
    @Nested
    @DisplayName("verifyOriginalDocument")
    class VerifyOriginalDocumentTest {

        @Test
        @DisplayName("Lancia InvalidRequestException se non ci sono documenti originali")
        void shouldThrowWhenNoOriginalDocuments() {
            SignedDocumentValidator validator = mock(SignedDocumentValidator.class);
            AdvancedSignature sig = mockSignature("sig1", new Date(), SignatureForm.CAdES);
            when(validator.getSignatures()).thenReturn(List.of(sig));
            when(validator.getOriginalDocuments("sig1")).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> service.verifyOriginalDocument(validator))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Non lancia eccezione se ci sono documenti originali")
        void shouldNotThrowWhenOriginalDocumentsPresent() {
            SignedDocumentValidator validator = mock(SignedDocumentValidator.class);
            AdvancedSignature sig = mockSignature("sig1", new Date(), SignatureForm.CAdES);
            DSSDocument doc = mock(DSSDocument.class);
            when(validator.getSignatures()).thenReturn(List.of(sig));
            when(validator.getOriginalDocuments("sig1")).thenReturn(List.of(doc));

            assertThatCode(() -> service.verifyOriginalDocument(validator)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Lancia InvalidRequestException se le firme sono null (nessun documento originale)")
        void shouldThrowWhenSignaturesNull() {
            SignedDocumentValidator validator = mock(SignedDocumentValidator.class);
            when(validator.getSignatures()).thenReturn(null);

            // Se le firme sono null, dssDocuments resta vuoto e viene lanciata eccezione
            assertThatThrownBy(() -> service.verifyOriginalDocument(validator))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Lancia InvalidRequestException se le firme sono lista vuota (nessun documento originale)")
        void shouldThrowWhenSignaturesEmpty() {
            SignedDocumentValidator validator = mock(SignedDocumentValidator.class);
            when(validator.getSignatures()).thenReturn(Collections.emptyList());

            // Se le firme sono vuote, dssDocuments resta vuoto e viene lanciata eccezione
            assertThatThrownBy(() -> service.verifyOriginalDocument(validator))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Lancia InvalidRequestException se getOriginalDocuments restituisce null per tutte le firme")
        void shouldThrowWhenGetOriginalDocumentsReturnsNull() {
            SignedDocumentValidator validator = mock(SignedDocumentValidator.class);
            AdvancedSignature sig = mockSignature("sig1", new Date(), SignatureForm.CAdES);
            when(validator.getSignatures()).thenReturn(List.of(sig));
            when(validator.getOriginalDocuments("sig1")).thenReturn(null);

            // Optional.ofNullable(null).ifPresent non aggiunge nulla, quindi dssDocuments resta vuoto
            assertThatThrownBy(() -> service.verifyOriginalDocument(validator))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Non lancia eccezione se almeno una firma ha documenti originali")
        void shouldNotThrowWhenAtLeastOneSignatureHasOriginals() {
            SignedDocumentValidator validator = mock(SignedDocumentValidator.class);
            AdvancedSignature sig1 = mockSignature("sig1", new Date(), SignatureForm.CAdES);
            AdvancedSignature sig2 = mockSignature("sig2", new Date(), SignatureForm.CAdES);
            DSSDocument doc = mock(DSSDocument.class);
            when(validator.getSignatures()).thenReturn(List.of(sig1, sig2));
            when(validator.getOriginalDocuments("sig1")).thenReturn(Collections.emptyList());
            when(validator.getOriginalDocuments("sig2")).thenReturn(List.of(doc));

            assertThatCode(() -> service.verifyOriginalDocument(validator)).doesNotThrowAnyException();
        }
    }

    // ===================================================
    // verifySignatureForm
    // ===================================================
    @Nested
    @DisplayName("verifySignatureForm")
    class VerifySignatureFormTest {

        @Test
        @DisplayName("Non lancia eccezione se tutte le firme sono CAdES")
        void shouldNotThrowWhenAllCAdES() {
            SignedDocumentValidator validator = mock(SignedDocumentValidator.class);
            AdvancedSignature sig1 = mockSignature("s1", new Date(), SignatureForm.CAdES);
            AdvancedSignature sig2 = mockSignature("s2", new Date(), SignatureForm.CAdES);
            when(validator.getSignatures()).thenReturn(List.of(sig1, sig2));

            assertThatCode(() -> service.verifySignatureForm(validator)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Lancia InvalidRequestException se una firma non è CAdES")
        void shouldThrowWhenNonCAdESPresent() {
            SignedDocumentValidator validator = mock(SignedDocumentValidator.class);
            AdvancedSignature sig1 = mockSignature("s1", new Date(), SignatureForm.CAdES);
            AdvancedSignature sig2 = mockSignature("s2", new Date(), SignatureForm.PAdES);
            when(validator.getSignatures()).thenReturn(List.of(sig1, sig2));

            assertThatThrownBy(() -> service.verifySignatureForm(validator))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("PAdES");
        }

        @Test
        @DisplayName("Lancia InvalidRequestException se la forma è XAdES")
        void shouldThrowWhenXAdES() {
            SignedDocumentValidator validator = mock(SignedDocumentValidator.class);
            AdvancedSignature sig = mockSignature("s1", new Date(), SignatureForm.XAdES);
            when(validator.getSignatures()).thenReturn(List.of(sig));

            assertThatThrownBy(() -> service.verifySignatureForm(validator))
                    .isInstanceOf(InvalidRequestException.class);
        }
    }

    // ===================================================
    // verifySignature(Reports)
    // ===================================================
    @Nested
    @DisplayName("verifySignature(Reports)")
    class VerifySignatureReportsTest {

        @Test
        @DisplayName("Non lancia eccezione se TOTAL_PASSED")
        void shouldNotThrowWhenTotalPassed() {
            Reports reports = mockReports(Indication.TOTAL_PASSED);
            assertThatCode(() -> service.verifySignature(reports)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Lancia InvalidRequestException se INDETERMINATE")
        void shouldThrowWhenIndeterminate() {
            Reports reports = mockReports(Indication.INDETERMINATE);
            assertThatThrownBy(() -> service.verifySignature(reports))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Lancia InvalidRequestException se TOTAL_FAILED")
        void shouldThrowWhenTotalFailed() {
            Reports reports = mockReports(Indication.TOTAL_FAILED);
            assertThatThrownBy(() -> service.verifySignature(reports))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Lancia InvalidRequestException se la lista di report è vuota")
        void shouldThrowWhenNoReports() {
            Reports reports = mock(Reports.class);
            eu.europa.esig.validationreport.jaxb.ValidationReportType etsi =
                    mock(eu.europa.esig.validationreport.jaxb.ValidationReportType.class);
            when(etsi.getSignatureValidationReport()).thenReturn(Collections.emptyList());
            when(reports.getEtsiValidationReportJaxb()).thenReturn(etsi);

            assertThatThrownBy(() -> service.verifySignature(reports))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Lancia InvalidRequestException se il report ETSI è null")
        void shouldThrowWhenEtsiReportNull() {
            Reports reports = mock(Reports.class);
            when(reports.getEtsiValidationReportJaxb()).thenReturn(null);

            assertThatThrownBy(() -> service.verifySignature(reports))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Lancia InvalidRequestException se il ValidationStatus è null")
        void shouldThrowWhenValidationStatusNull() {
            Reports reports = mock(Reports.class);
            eu.europa.esig.validationreport.jaxb.ValidationReportType etsi =
                    mock(eu.europa.esig.validationreport.jaxb.ValidationReportType.class);
            SignatureValidationReportType sigReport = mock(SignatureValidationReportType.class);
            when(sigReport.getSignatureValidationStatus()).thenReturn(null);
            when(etsi.getSignatureValidationReport()).thenReturn(List.of(sigReport));
            when(reports.getEtsiValidationReportJaxb()).thenReturn(etsi);

            assertThatThrownBy(() -> service.verifySignature(reports))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Non lancia eccezione se tutti i report hanno TOTAL_PASSED")
        void shouldNotThrowWhenAllReportsAreTotalPassed() {
            Reports reports = mock(Reports.class);
            eu.europa.esig.validationreport.jaxb.ValidationReportType etsi =
                    mock(eu.europa.esig.validationreport.jaxb.ValidationReportType.class);

            SignatureValidationReportType sigReport1 = mock(SignatureValidationReportType.class);
            SignatureValidationReportType sigReport2 = mock(SignatureValidationReportType.class);
            ValidationStatusType status1 = mock(ValidationStatusType.class);
            ValidationStatusType status2 = mock(ValidationStatusType.class);

            when(status1.getMainIndication()).thenReturn(Indication.TOTAL_PASSED);
            when(status2.getMainIndication()).thenReturn(Indication.TOTAL_PASSED);
            when(sigReport1.getSignatureValidationStatus()).thenReturn(status1);
            when(sigReport2.getSignatureValidationStatus()).thenReturn(status2);
            when(etsi.getSignatureValidationReport()).thenReturn(List.of(sigReport1, sigReport2));
            when(reports.getEtsiValidationReportJaxb()).thenReturn(etsi);

            assertThatCode(() -> service.verifySignature(reports)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Lancia InvalidRequestException se almeno un report non è TOTAL_PASSED")
        void shouldThrowWhenAnyReportIsNotTotalPassed() {
            Reports reports = mock(Reports.class);
            eu.europa.esig.validationreport.jaxb.ValidationReportType etsi =
                    mock(eu.europa.esig.validationreport.jaxb.ValidationReportType.class);

            SignatureValidationReportType sigReport1 = mock(SignatureValidationReportType.class);
            SignatureValidationReportType sigReport2 = mock(SignatureValidationReportType.class);
            ValidationStatusType status1 = mock(ValidationStatusType.class);
            ValidationStatusType status2 = mock(ValidationStatusType.class);

            when(status1.getMainIndication()).thenReturn(Indication.TOTAL_PASSED);
            when(status2.getMainIndication()).thenReturn(Indication.INDETERMINATE);
            when(sigReport1.getSignatureValidationStatus()).thenReturn(status1);
            when(sigReport2.getSignatureValidationStatus()).thenReturn(status2);
            when(etsi.getSignatureValidationReport()).thenReturn(List.of(sigReport1, sigReport2));
            when(reports.getEtsiValidationReportJaxb()).thenReturn(etsi);

            assertThatThrownBy(() -> service.verifySignature(reports))
                    .isInstanceOf(InvalidRequestException.class);
        }
    }

    // ===================================================
    // checkSignature(Reports)
    // ===================================================
    @Nested
    @DisplayName("checkSignature(Reports)")
    class CheckSignatureTest {

        @Test
        @DisplayName("Non lancia eccezione se lo stato di validazione è presente")
        void shouldNotThrowWhenStatusPresent() {
            Reports reports = mock(Reports.class);
            eu.europa.esig.validationreport.jaxb.ValidationReportType etsi =
                    mock(eu.europa.esig.validationreport.jaxb.ValidationReportType.class);
            SignatureValidationReportType sigReport = mock(SignatureValidationReportType.class);
            ValidationStatusType statusType = mock(ValidationStatusType.class);
            when(sigReport.getSignatureValidationStatus()).thenReturn(statusType);
            when(etsi.getSignatureValidationReport()).thenReturn(List.of(sigReport));
            when(reports.getEtsiValidationReportJaxb()).thenReturn(etsi);

            assertThatCode(() -> service.checkSignature(reports)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Lancia InvalidRequestException se la lista di report è vuota")
        void shouldThrowWhenNoReports() {
            Reports reports = mock(Reports.class);
            eu.europa.esig.validationreport.jaxb.ValidationReportType etsi =
                    mock(eu.europa.esig.validationreport.jaxb.ValidationReportType.class);
            when(etsi.getSignatureValidationReport()).thenReturn(Collections.emptyList());
            when(reports.getEtsiValidationReportJaxb()).thenReturn(etsi);

            assertThatThrownBy(() -> service.checkSignature(reports))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Lancia InvalidRequestException se lo stato è null")
        void shouldThrowWhenStatusNull() {
            Reports reports = mock(Reports.class);
            eu.europa.esig.validationreport.jaxb.ValidationReportType etsi =
                    mock(eu.europa.esig.validationreport.jaxb.ValidationReportType.class);
            SignatureValidationReportType sigReport = mock(SignatureValidationReportType.class);
            when(sigReport.getSignatureValidationStatus()).thenReturn(null);
            when(etsi.getSignatureValidationReport()).thenReturn(List.of(sigReport));
            when(reports.getEtsiValidationReportJaxb()).thenReturn(etsi);

            assertThatThrownBy(() -> service.checkSignature(reports))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Lancia InvalidRequestException se il report ETSI è null")
        void shouldThrowWhenEtsiReportNull() {
            Reports reports = mock(Reports.class);
            when(reports.getEtsiValidationReportJaxb()).thenReturn(null);

            assertThatThrownBy(() -> service.checkSignature(reports))
                    .isInstanceOf(InvalidRequestException.class);
        }
    }

    // ===================================================
    // verifyDigest
    // ===================================================
    @Nested
    @DisplayName("verifyDigest")
    class VerifyDigestTest {

        @Test
        @DisplayName("Non lancia eccezione se il digest corrisponde")
        void shouldNotThrowWhenDigestMatches() {
            SignedDocumentValidator validator = mock(SignedDocumentValidator.class);
            AdvancedSignature sig = mockSignature("sig1", new Date(), SignatureForm.CAdES);
            DSSDocument doc = mock(DSSDocument.class);
            Digest digest = mock(Digest.class);
            when(digest.getBase64Value()).thenReturn("correctChecksum");
            when(doc.getDigest(DigestAlgorithm.SHA256)).thenReturn(digest);
            when(validator.getSignatures()).thenReturn(List.of(sig));
            when(validator.getOriginalDocuments("sig1")).thenReturn(List.of(doc));

            assertThatCode(() -> service.verifyDigest(validator, "correctChecksum"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Lancia InvalidRequestException se il digest non corrisponde")
        void shouldThrowWhenDigestMismatch() {
            SignedDocumentValidator validator = mock(SignedDocumentValidator.class);
            AdvancedSignature sig = mockSignature("sig1", new Date(), SignatureForm.CAdES);
            DSSDocument doc = mock(DSSDocument.class);
            Digest digest = mock(Digest.class);
            when(digest.getBase64Value()).thenReturn("wrongChecksum");
            when(doc.getDigest(DigestAlgorithm.SHA256)).thenReturn(digest);
            when(validator.getSignatures()).thenReturn(List.of(sig));
            when(validator.getOriginalDocuments("sig1")).thenReturn(List.of(doc));

            assertThatThrownBy(() -> service.verifyDigest(validator, "expectedChecksum"))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Non lancia eccezione se le firme sono null")
        void shouldNotThrowWhenSignaturesNull() {
            SignedDocumentValidator validator = mock(SignedDocumentValidator.class);
            when(validator.getSignatures()).thenReturn(null);

            assertThatCode(() -> service.verifyDigest(validator, "anyChecksum"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Non lancia eccezione se le firme sono vuote")
        void shouldNotThrowWhenSignaturesEmpty() {
            SignedDocumentValidator validator = mock(SignedDocumentValidator.class);
            when(validator.getSignatures()).thenReturn(Collections.emptyList());

            assertThatCode(() -> service.verifyDigest(validator, "anyChecksum"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Verifica tutte le firme - lancia eccezione se una non ha il digest corretto")
        void shouldCheckAllSignaturesAndThrowIfAnyFails() {
            SignedDocumentValidator validator = mock(SignedDocumentValidator.class);
            AdvancedSignature sig1 = mockSignature("sig1", new Date(), SignatureForm.CAdES);
            AdvancedSignature sig2 = mockSignature("sig2", new Date(), SignatureForm.CAdES);

            DSSDocument doc1 = mock(DSSDocument.class);
            DSSDocument doc2 = mock(DSSDocument.class);
            Digest digest1 = mock(Digest.class);
            Digest digest2 = mock(Digest.class);

            when(digest1.getBase64Value()).thenReturn("correctChecksum");
            when(digest2.getBase64Value()).thenReturn("wrongChecksum");
            when(doc1.getDigest(DigestAlgorithm.SHA256)).thenReturn(digest1);
            when(doc2.getDigest(DigestAlgorithm.SHA256)).thenReturn(digest2);

            when(validator.getSignatures()).thenReturn(List.of(sig1, sig2));
            when(validator.getOriginalDocuments("sig1")).thenReturn(List.of(doc1));
            when(validator.getOriginalDocuments("sig2")).thenReturn(List.of(doc2));

            // La prima firma ha il checksum corretto, ma la seconda no
            assertThatThrownBy(() -> service.verifyDigest(validator, "correctChecksum"))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Non lancia eccezione se tutte le firme hanno il digest corretto")
        void shouldNotThrowWhenAllSignaturesHaveCorrectDigest() {
            SignedDocumentValidator validator = mock(SignedDocumentValidator.class);
            AdvancedSignature sig1 = mockSignature("sig1", new Date(), SignatureForm.CAdES);
            AdvancedSignature sig2 = mockSignature("sig2", new Date(), SignatureForm.CAdES);

            DSSDocument doc1 = mock(DSSDocument.class);
            DSSDocument doc2 = mock(DSSDocument.class);
            Digest digest1 = mock(Digest.class);
            Digest digest2 = mock(Digest.class);

            when(digest1.getBase64Value()).thenReturn("correctChecksum");
            when(digest2.getBase64Value()).thenReturn("correctChecksum");
            when(doc1.getDigest(DigestAlgorithm.SHA256)).thenReturn(digest1);
            when(doc2.getDigest(DigestAlgorithm.SHA256)).thenReturn(digest2);

            when(validator.getSignatures()).thenReturn(List.of(sig1, sig2));
            when(validator.getOriginalDocuments("sig1")).thenReturn(List.of(doc1));
            when(validator.getOriginalDocuments("sig2")).thenReturn(List.of(doc2));

            assertThatCode(() -> service.verifyDigest(validator, "correctChecksum"))
                    .doesNotThrowAnyException();
        }
    }

    // ===================================================
    // verifyManagerTaxCode
    // ===================================================
    @Nested
    @DisplayName("verifyManagerTaxCode")
    class VerifyManagerTaxCodeTest {

        private Reports buildReportsWithCertificates(List<String> subjectSerialNumbers) {
            Reports reports = mock(Reports.class);
            DiagnosticData diagnosticData = mock(DiagnosticData.class);
            List<CertificateWrapper> certs = new ArrayList<>();
            for (String sn : subjectSerialNumbers) {
                CertificateWrapper cert = mock(CertificateWrapper.class);
                when(cert.getSubjectSerialNumber()).thenReturn(sn);
                certs.add(cert);
            }
            when(diagnosticData.getUsedCertificates()).thenReturn(certs);
            when(reports.getDiagnosticData()).thenReturn(diagnosticData);
            return reports;
        }

        @Test
        @DisplayName("Non lancia eccezione quando il tax code del firmatario è nella lista utenti")
        void shouldNotThrowWhenTaxCodeMatches() {
            Reports reports = buildReportsWithCertificates(List.of("TINIT-RSSMRA80A01H501R"));
            assertThatCode(() -> service.verifyManagerTaxCode(reports, List.of("RSSMRA80A01H501R")))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Lancia InvalidRequestException se non ci sono tax code nella firma")
        void shouldThrowWhenNoTaxCodeInSignature() {
            Reports reports = buildReportsWithCertificates(List.of("NO_MATCH_PREFIX"));
            assertThatThrownBy(() -> service.verifyManagerTaxCode(reports, List.of("RSSMRA80A01H501R")))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Lancia InvalidRequestException se i dati diagnostici sono null")
        void shouldThrowWhenDiagnosticDataNull() {
            Reports reports = mock(Reports.class);
            when(reports.getDiagnosticData()).thenReturn(null);

            assertThatThrownBy(() -> service.verifyManagerTaxCode(reports, List.of("RSSMRA80A01H501R")))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Lancia InvalidRequestException se il tax code del firmatario non include quello dell'utente")
        void shouldThrowWhenTaxCodeNotInUserList() {
            Reports reports = buildReportsWithCertificates(List.of("TINIT-XXXXXXXXXXXXXXXX"));
            assertThatThrownBy(() -> service.verifyManagerTaxCode(reports, List.of("RSSMRA80A01H501R")))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Lancia InvalidRequestException se la lista utenti è vuota")
        void shouldThrowWhenUsersTaxCodeEmpty() {
            Reports reports = buildReportsWithCertificates(List.of("TINIT-RSSMRA80A01H501R"));
            assertThatThrownBy(() -> service.verifyManagerTaxCode(reports, Collections.emptyList()))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Non lancia eccezione quando più firmatari coprono tutti gli utenti")
        void shouldNotThrowWhenMultipleSignatorsCoversAllUsers() {
            Reports reports = buildReportsWithCertificates(List.of("TINIT-AAA111", "TINIT-BBB222"));
            assertThatCode(() -> service.verifyManagerTaxCode(reports, List.of("AAA111", "BBB222")))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Lancia InvalidRequestException se solo alcuni utenti sono coperti dalla firma")
        void shouldThrowWhenOnlySomeUsersAreCovered() {
            Reports reports = buildReportsWithCertificates(List.of("TINIT-AAA111"));
            assertThatThrownBy(() -> service.verifyManagerTaxCode(reports, List.of("AAA111", "BBB222")))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Il serial number null viene ignorato")
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
        @DisplayName("Lancia InvalidRequestException se usedCertificates è null")
        void shouldThrowWhenUsedCertificatesNull() {
            Reports reports = mock(Reports.class);
            DiagnosticData diagnosticData = mock(DiagnosticData.class);
            when(diagnosticData.getUsedCertificates()).thenReturn(null);
            when(reports.getDiagnosticData()).thenReturn(diagnosticData);

            assertThatThrownBy(() -> service.verifyManagerTaxCode(reports, List.of("RSSMRA80A01H501R")))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Lancia InvalidRequestException se usedCertificates è lista vuota")
        void shouldThrowWhenUsedCertificatesEmpty() {
            Reports reports = mock(Reports.class);
            DiagnosticData diagnosticData = mock(DiagnosticData.class);
            when(diagnosticData.getUsedCertificates()).thenReturn(Collections.emptyList());
            when(reports.getDiagnosticData()).thenReturn(diagnosticData);

            assertThatThrownBy(() -> service.verifyManagerTaxCode(reports, List.of("RSSMRA80A01H501R")))
                    .isInstanceOf(InvalidRequestException.class);
        }
    }

    // ===================================================
    // isSignatureVerificationEnabled
    // ===================================================
    @Nested
    @DisplayName("isSignatureVerificationEnabled")
    class IsSignatureVerificationEnabledTest {

        @Test
        @DisplayName("Ritorna true quando isVerifyEnabled è TRUE")
        void shouldReturnTrueWhenEnabled() {
            setField(service, "isVerifyEnabled", Boolean.TRUE);
            assertTrue(service.isSignatureVerificationEnabled());
        }

        @Test
        @DisplayName("Ritorna false quando isVerifyEnabled è FALSE")
        void shouldReturnFalseWhenDisabled() {
            setField(service, "isVerifyEnabled", Boolean.FALSE);
            assertFalse(service.isSignatureVerificationEnabled());
        }

        @Test
        @DisplayName("Ritorna false quando isVerifyEnabled è null")
        void shouldReturnFalseWhenNull() {
            setField(service, "isVerifyEnabled", null);
            assertFalse(service.isSignatureVerificationEnabled());
        }
    }

    // ===================================================
    // verifyContractSignature
    // ===================================================
    @Nested
    @DisplayName("verifyContractSignature")
    class VerifyContractSignatureTest {

        @Test
        @DisplayName("Ritorna Uni<Void> immediatamente se la verifica è disabilitata")
        void shouldReturnImmediatelyWhenVerifyDisabled() {
            setField(service, "isVerifyEnabled", Boolean.FALSE);
            File file = tempDir.resolve("test.pdf").toFile();

            Uni<Void> result = service.verifyContractSignature("onboarding-id", file, List.of("CF1"));

            assertThatCode(() -> result.await().indefinitely()).doesNotThrowAnyException();
            verifyNoInteractions(documentService);
        }

        @Test
        @DisplayName("Chiama documentService quando la verifica è abilitata")
        void shouldCallDocumentServiceWhenEnabled() {
            setField(service, "isVerifyEnabled", Boolean.TRUE);
            Document document = mock(Document.class);
            when(document.getChecksum()).thenReturn("checksum123");
            when(documentService.getDocumentById("onboarding-id")).thenReturn(Uni.createFrom().item(document));

            // La verifySignature(File, String, List) fallirà perché il file non è un documento firmato reale,
            // quindi ci aspettiamo un'eccezione ma verifichiamo comunque che documentService sia stato chiamato
            File file = tempDir.resolve("test.pdf").toFile();
            Uni<Void> result = service.verifyContractSignature("onboarding-id", file, List.of("CF1"));

            assertThatThrownBy(() -> result.await().indefinitely())
                    .isInstanceOf(Exception.class);

            verify(documentService).getDocumentById("onboarding-id");
        }

        @Test
        @DisplayName("Propaga l'eccezione se documentService fallisce")
        void shouldPropagateWhenDocumentServiceFails() {
            setField(service, "isVerifyEnabled", Boolean.TRUE);
            when(documentService.getDocumentById("onboarding-id"))
                    .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));

            File file = tempDir.resolve("test.pdf").toFile();
            Uni<Void> result = service.verifyContractSignature("onboarding-id", file, List.of("CF1"));

            assertThatThrownBy(() -> result.await().indefinitely())
                    .isInstanceOf(Exception.class)
                    .hasMessageContaining("DB error");

            verify(documentService).getDocumentById("onboarding-id");
        }
    }

    // ===================================================
    // chooseEarliestSignature
    // ===================================================
    @Nested
    @DisplayName("chooseEarliestSignature")
    class ChooseEarliestSignatureTest {

        @Test
        @DisplayName("Ritorna la firma con signingTime precedente")
        void shouldReturnEarliestBySigningTime() {
            Date earlier = Date.from(Instant.parse("2024-01-01T00:00:00Z"));
            Date later   = Date.from(Instant.parse("2024-06-01T00:00:00Z"));
            AdvancedSignature sig1 = mockSignature("sig-later",   later,   SignatureForm.CAdES);
            AdvancedSignature sig2 = mockSignature("sig-earlier", earlier, SignatureForm.CAdES);

            AdvancedSignature result = service.chooseEarliestSignature(List.of(sig1, sig2));

            assertThat(result.getId()).isEqualTo("sig-earlier");
        }

        @Test
        @DisplayName("In caso di signingTime uguale, ordina per id")
        void shouldFallbackToIdWhenSameSigningTime() {
            Date sameDate = new Date();
            AdvancedSignature sigB = mockSignature("sig-B", sameDate, SignatureForm.CAdES);
            AdvancedSignature sigA = mockSignature("sig-A", sameDate, SignatureForm.CAdES);

            AdvancedSignature result = service.chooseEarliestSignature(List.of(sigB, sigA));

            assertThat(result.getId()).isEqualTo("sig-A");
        }

        @Test
        @DisplayName("Gestisce signingTime null (null-last)")
        void shouldHandleNullSigningTime() {
            Date realDate = new Date();
            AdvancedSignature sigNull = mockSignature("sig-null", null, SignatureForm.CAdES);
            AdvancedSignature sigReal = mockSignature("sig-real", realDate, SignatureForm.CAdES);

            AdvancedSignature result = service.chooseEarliestSignature(List.of(sigNull, sigReal));

            assertThat(result.getId()).isEqualTo("sig-real");
        }

        @Test
        @DisplayName("Ritorna l'unica firma se la lista ne contiene una sola")
        void shouldReturnSingleSignature() {
            AdvancedSignature sig = mockSignature("only-sig", new Date(), SignatureForm.CAdES);

            AdvancedSignature result = service.chooseEarliestSignature(List.of(sig));

            assertThat(result.getId()).isEqualTo("only-sig");
        }
    }

    // ===================================================
    // extractPdfFromSignedContainer
    // ===================================================
    @Nested
    @DisplayName("extractPdfFromSignedContainer")
    class ExtractPdfFromSignedContainerTest {

        @Test
        @DisplayName("Ritorna il documento originale se la firma ha documenti originali")
        void shouldReturnOriginalDocument() {
            SignedDocumentValidator validator = mock(SignedDocumentValidator.class);
            DSSDocument inputDoc  = mock(DSSDocument.class);
            DSSDocument originalDoc = mock(DSSDocument.class);
            AdvancedSignature sig = mockSignature("sig1", new Date(), SignatureForm.CAdES);

            when(validator.getSignatures()).thenReturn(List.of(sig));
            when(validator.getOriginalDocuments("sig1")).thenReturn(List.of(originalDoc));

            DSSDocument result = service.extractPdfFromSignedContainer(validator, inputDoc);

            assertThat(result).isSameAs(originalDoc);
        }

        @Test
        @DisplayName("Ritorna il documento input se le firme sono null")
        void shouldReturnInputDocWhenSignaturesNull() {
            SignedDocumentValidator validator = mock(SignedDocumentValidator.class);
            DSSDocument inputDoc = mock(DSSDocument.class);
            when(validator.getSignatures()).thenReturn(null);

            DSSDocument result = service.extractPdfFromSignedContainer(validator, inputDoc);

            assertThat(result).isSameAs(inputDoc);
        }

        @Test
        @DisplayName("Ritorna il documento input se le firme sono vuote")
        void shouldReturnInputDocWhenSignaturesEmpty() {
            SignedDocumentValidator validator = mock(SignedDocumentValidator.class);
            DSSDocument inputDoc = mock(DSSDocument.class);
            when(validator.getSignatures()).thenReturn(Collections.emptyList());

            DSSDocument result = service.extractPdfFromSignedContainer(validator, inputDoc);

            assertThat(result).isSameAs(inputDoc);
        }

        @Test
        @DisplayName("Ritorna il documento input se non ci sono documenti originali per la firma")
        void shouldReturnInputDocWhenNoOriginals() {
            SignedDocumentValidator validator = mock(SignedDocumentValidator.class);
            DSSDocument inputDoc = mock(DSSDocument.class);
            AdvancedSignature sig = mockSignature("sig1", new Date(), SignatureForm.CAdES);

            when(validator.getSignatures()).thenReturn(List.of(sig));
            when(validator.getOriginalDocuments("sig1")).thenReturn(Collections.emptyList());

            DSSDocument result = service.extractPdfFromSignedContainer(validator, inputDoc);

            assertThat(result).isSameAs(inputDoc);
        }

        @Test
        @DisplayName("Ritorna il documento input se la lista originali è null")
        void shouldReturnInputDocWhenOriginalsNull() {
            SignedDocumentValidator validator = mock(SignedDocumentValidator.class);
            DSSDocument inputDoc = mock(DSSDocument.class);
            AdvancedSignature sig = mockSignature("sig1", new Date(), SignatureForm.CAdES);

            when(validator.getSignatures()).thenReturn(List.of(sig));
            when(validator.getOriginalDocuments("sig1")).thenReturn(null);

            DSSDocument result = service.extractPdfFromSignedContainer(validator, inputDoc);

            assertThat(result).isSameAs(inputDoc);
        }
    }

    // ===================================================
    // computeDigestOfSignedRevision
    // ===================================================
    @Nested
    @DisplayName("computeDigestOfSignedRevision")
    class ComputeDigestOfSignedRevisionTest {

        @Test
        @DisplayName("Calcola digest dell'intero documento se non ci sono firme")
        void shouldComputeFullDocDigestWhenNoSignatures() {
            SignedDocumentValidator validator = mock(SignedDocumentValidator.class);
            DSSDocument doc = mock(DSSDocument.class);
            Digest digest = mock(Digest.class);
            when(validator.getSignatures()).thenReturn(Collections.emptyList());
            when(doc.getDigest(DigestAlgorithm.SHA256)).thenReturn(digest);
            when(digest.getBase64Value()).thenReturn("fullDocDigest");

            String result = service.computeDigestOfSignedRevision(validator, doc);

            assertThat(result).isEqualTo("fullDocDigest");
        }

        @Test
        @DisplayName("Calcola digest dell'intero documento se le firme sono null")
        void shouldComputeFullDocDigestWhenSignaturesNull() {
            SignedDocumentValidator validator = mock(SignedDocumentValidator.class);
            DSSDocument doc = mock(DSSDocument.class);
            Digest digest = mock(Digest.class);
            when(validator.getSignatures()).thenReturn(null);
            when(doc.getDigest(DigestAlgorithm.SHA256)).thenReturn(digest);
            when(digest.getBase64Value()).thenReturn("fullDocDigest");

            String result = service.computeDigestOfSignedRevision(validator, doc);

            assertThat(result).isEqualTo("fullDocDigest");
        }

        @Test
        @DisplayName("Calcola digest del documento originale se ci sono firme con originali")
        void shouldComputeDigestOfOriginalDocument() {
            SignedDocumentValidator validator = mock(SignedDocumentValidator.class);
            DSSDocument doc = mock(DSSDocument.class);
            DSSDocument originalDoc = mock(DSSDocument.class);
            Digest originalDigest = mock(Digest.class);
            AdvancedSignature sig = mockSignature("sig1", new Date(), SignatureForm.CAdES);

            when(validator.getSignatures()).thenReturn(List.of(sig));
            when(validator.getOriginalDocuments("sig1")).thenReturn(List.of(originalDoc));
            when(originalDoc.getDigest(DigestAlgorithm.SHA256)).thenReturn(originalDigest);
            when(originalDigest.getBase64Value()).thenReturn("originalDocDigest");

            String result = service.computeDigestOfSignedRevision(validator, doc);

            assertThat(result).isEqualTo("originalDocDigest");
        }

        @Test
        @DisplayName("Calcola digest del documento input se la firma non ha originali")
        void shouldComputeDigestOfInputDocWhenNoOriginals() {
            SignedDocumentValidator validator = mock(SignedDocumentValidator.class);
            DSSDocument doc = mock(DSSDocument.class);
            Digest docDigest = mock(Digest.class);
            AdvancedSignature sig = mockSignature("sig1", new Date(), SignatureForm.CAdES);

            when(validator.getSignatures()).thenReturn(List.of(sig));
            when(validator.getOriginalDocuments("sig1")).thenReturn(Collections.emptyList());
            when(doc.getDigest(DigestAlgorithm.SHA256)).thenReturn(docDigest);
            when(docDigest.getBase64Value()).thenReturn("inputDocDigest");

            String result = service.computeDigestOfSignedRevision(validator, doc);

            assertThat(result).isEqualTo("inputDocDigest");
        }
    }

    // ===================================================
    // verifyUploadedFileDigest
    // ===================================================
    @Nested
    @DisplayName("verifyUploadedFileDigest")
    class VerifyUploadedFileDigestTest {

        @Test
        @DisplayName("Lancia NullPointerException se il FormItem è null")
        void shouldThrowWhenFileIsNull() {
            assertThatThrownBy(() -> service.verifyUploadedFileDigest(null, "templateDigest", false))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Lancia NullPointerException se il templateDigest è null")
        void shouldThrowWhenTemplateDigestIsNull() throws IOException {
            File tempFile = Files.createTempFile(tempDir, "doc", ".pdf").toFile();
            FormItem formItem = FormItem.builder().file(tempFile).build();

            assertThatThrownBy(() -> service.verifyUploadedFileDigest(formItem, null, false))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // ===================================================
    // signDocument
    // ===================================================
    @Nested
    @DisplayName("signDocument")
    class SignDocumentTest {

        @Test
        @DisplayName("Ritorna il file originale se la firma PagoPA è disabilitata")
        void shouldReturnOriginalFileWhenSignatureDisabled() throws Exception {
            when(pagoPaSignatureConfig.source()).thenReturn("disabled");
            File pdf = tempDir.resolve("test.pdf").toFile();
            Files.createFile(pdf.toPath());

            File result = service.signDocument(pdf, "Ente Test", "prod-io")
                    .await().indefinitely();

            assertThat(result).isEqualTo(pdf);
            verifyNoInteractions(padesSignService);
        }

        @Test
        @DisplayName("Invoca padesSignService quando la firma è abilitata")
        void shouldCallPadesSignServiceWhenEnabled() throws Exception {
            when(pagoPaSignatureConfig.source()).thenReturn("enabled");
            when(pagoPaSignatureConfig.applyOnboardingTemplateReason())
                    .thenReturn("Reason for ${institutionName} and ${productName}");
            when(pagoPaSignatureConfig.signer()).thenReturn("Test Signer");
            when(pagoPaSignatureConfig.location()).thenReturn("Rome");

            File pdf = tempDir.resolve("input.pdf").toFile();
            Files.createFile(pdf.toPath());

            doNothing().when(padesSignService).padesSign(any(File.class), any(File.class), any());

            File result = service.signDocument(pdf, "Ente Test", "prod-io")
                    .await().indefinitely();

            assertThat(result).isNotNull();
            verify(padesSignService).padesSign(eq(pdf), any(File.class), any());
        }

        @Test
        @DisplayName("Il reason viene correttamente interpolato con i placeholder")
        void shouldInterpolatePlaceholdersInReason() throws Exception {
            when(pagoPaSignatureConfig.source()).thenReturn("active");
            when(pagoPaSignatureConfig.applyOnboardingTemplateReason())
                    .thenReturn("Firma per ${institutionName} - ${productName}");
            when(pagoPaSignatureConfig.signer()).thenReturn("Signer");
            when(pagoPaSignatureConfig.location()).thenReturn("Milan");

            File pdf = tempDir.resolve("input2.pdf").toFile();
            Files.createFile(pdf.toPath());

            doAnswer(invocation -> {
                String reasonArg = invocation.<it.pagopa.selfcare.onboarding.crypto.entity.SignatureInformation>getArgument(2)
                        .getReason();
                assertThat(reasonArg).isEqualTo("Firma per Istituto XYZ - prod-pn");
                return null;
            }).when(padesSignService).padesSign(any(), any(), any());

            service.signDocument(pdf, "Istituto XYZ", "prod-pn").await().indefinitely();
        }

        @Test
        @DisplayName("Lancia IllegalArgumentException quando padesSignService fallisce con eccezione")
        void shouldThrowIllegalArgumentExceptionWhenPadesSignFails() throws Exception {
            when(pagoPaSignatureConfig.source()).thenReturn("enabled");
            when(pagoPaSignatureConfig.applyOnboardingTemplateReason())
                    .thenReturn("Reason");
            when(pagoPaSignatureConfig.signer()).thenReturn("Signer");
            when(pagoPaSignatureConfig.location()).thenReturn("Location");

            File pdf = tempDir.resolve("fail.pdf").toFile();
            Files.createFile(pdf.toPath());

            doThrow(new RuntimeException("Signing failed")).when(padesSignService)
                    .padesSign(any(File.class), any(File.class), any());

            assertThatThrownBy(() -> service.signDocument(pdf, "Ente", "prod")
                    .await().indefinitely())
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // ===================================================
    // createSafeTempFile / createTempFileWithPosix
    // ===================================================
    @Nested
    @DisplayName("createSafeTempFile e createTempFileWithPosix")
    class CreateTempFileTest {

        @Test
        @DisplayName("createTempFileWithPosix crea un file temporaneo con permessi POSIX")
        void shouldCreateTempFileWithPosixPermissions() throws IOException {
            Path tempFile = service.createTempFileWithPosix();

            assertThat(tempFile).isNotNull();
            assertThat(tempFile.toFile()).exists();
            assertThat(tempFile.toString()).endsWith(".pdf");

            // Cleanup
            Files.deleteIfExists(tempFile);
        }

        @Test
        @DisplayName("createSafeTempFile ritorna un percorso valido")
        void shouldReturnValidPath() throws IOException {
            Path result = service.createSafeTempFile();

            assertThat(result).isNotNull();
            assertThat(result.toFile()).exists();

            // Cleanup
            Files.deleteIfExists(result);
        }
    }

    // ===================================================
    // validateDocument
    // ===================================================
    @Nested
    @DisplayName("validateDocument")
    class ValidateDocumentTest {

        @Test
        @DisplayName("Lancia InvalidRequestException se il validator lancia un'eccezione")
        void shouldThrowInvalidRequestExceptionOnFailure() {
            SignedDocumentValidator validator = mock(SignedDocumentValidator.class);
            when(validator.validateDocument()).thenThrow(new RuntimeException("validation failed"));

            assertThatThrownBy(() -> service.validateDocument(validator))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Restituisce Reports quando la validazione ha successo")
        void shouldReturnReportsOnSuccess() {
            SignedDocumentValidator validator = mock(SignedDocumentValidator.class);
            Reports expectedReports = mock(Reports.class);
            when(validator.validateDocument()).thenReturn(expectedReports);

            Reports result = service.validateDocument(validator);

            assertThat(result).isSameAs(expectedReports);
        }
    }

    // ===================================================
    // verifySignature(File) — boolean
    // ===================================================
    @Nested
    @DisplayName("verifySignature(File) - boolean")
    class VerifySignatureFileBooleanTest {

        @Test
        @DisplayName("Lancia InvalidRequestException se il file non contiene un documento firmato valido")
        void shouldThrowWhenFileIsNotSignedDocument() throws IOException {
            File invalidFile = tempDir.resolve("invalid.pdf").toFile();
            Files.writeString(invalidFile.toPath(), "not-a-signed-document");

            assertThatThrownBy(() -> service.verifySignature(invalidFile))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Lancia InvalidRequestException se il file non esiste")
        void shouldThrowWhenFileDoesNotExist() {
            File nonExistentFile = tempDir.resolve("nonexistent.pdf").toFile();

            assertThatThrownBy(() -> service.verifySignature(nonExistentFile))
                    .isInstanceOf(InvalidRequestException.class);
        }
    }

    // ===================================================
    // verifySignature(File, String, List) — void
    // ===================================================
    @Nested
    @DisplayName("verifySignature(File, String, List)")
    class VerifySignatureFileChecksumTaxCodeTest {

        @Test
        @DisplayName("Lancia InvalidRequestException se il file è vuoto")
        void shouldThrowWhenFileContainsInvalidData() throws IOException {
            File invalidFile = tempDir.resolve("bad.pdf").toFile();
            Files.writeString(invalidFile.toPath(), "garbage");

            assertThatThrownBy(() ->
                    service.verifySignature(invalidFile, "someChecksum", List.of("CF1")))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Rilancia InvalidRequestException senza wrapping se già InvalidRequestException")
        void shouldRethrowInvalidRequestExceptionDirectly() throws IOException {
            File invalidFile = tempDir.resolve("bad2.pdf").toFile();
            Files.writeString(invalidFile.toPath(), "garbage-data");

            // Il metodo deve propagare InvalidRequestException (non wrappare in un'altra)
            assertThatThrownBy(() ->
                    service.verifySignature(invalidFile, "checksum", List.of("CF1")))
                    .isExactlyInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Lancia InvalidRequestException se il file non esiste")
        void shouldThrowWhenFileDoesNotExist() {
            File missingFile = tempDir.resolve("missing.p7m").toFile();

            assertThatThrownBy(() ->
                    service.verifySignature(missingFile, "checksum", List.of("CF1")))
                    .isInstanceOf(InvalidRequestException.class);
        }
    }

    // ===================================================
    // extractOriginalDocument (metodo statico)
    // ===================================================
    @Nested
    @DisplayName("extractOriginalDocument")
    class ExtractOriginalDocumentTest {

        @Test
        @DisplayName("Lancia InvalidRequestException se il file non è un documento firmato valido")
        void shouldThrowWhenFileIsNotSigned() throws IOException {
            File invalidFile = tempDir.resolve("not-signed.pdf").toFile();
            Files.writeString(invalidFile.toPath(), "not a signed document");

            assertThatThrownBy(() -> SignatureServiceImp.extractOriginalDocument(invalidFile))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Lancia InvalidRequestException se il file non esiste")
        void shouldThrowWhenFileDoesNotExist() {
            File nonExistentFile = tempDir.resolve("nonexistent.p7m").toFile();

            assertThatThrownBy(() -> SignatureServiceImp.extractOriginalDocument(nonExistentFile))
                    .isInstanceOf(InvalidRequestException.class);
        }
    }

    // ===================================================
    // extractFile
    // ===================================================
    @Nested
    @DisplayName("extractFile")
    class ExtractFileTest {

        @Test
        @DisplayName("Lancia eccezione se il contratto non è un documento firmato valido")
        void shouldThrowWhenContractIsNotSigned() throws IOException {
            File invalidContract = tempDir.resolve("invalid-contract.pdf").toFile();
            Files.writeString(invalidContract.toPath(), "not a signed contract");

            // extractOriginalDocument lancerà InvalidRequestException che viene propagata
            assertThatThrownBy(() -> service.extractFile(invalidContract))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Lancia eccezione se il file non esiste")
        void shouldThrowWhenFileDoesNotExist() {
            File nonExistentFile = tempDir.resolve("missing-contract.p7m").toFile();

            assertThatThrownBy(() -> service.extractFile(nonExistentFile))
                    .isInstanceOf(InvalidRequestException.class);
        }
    }
}
