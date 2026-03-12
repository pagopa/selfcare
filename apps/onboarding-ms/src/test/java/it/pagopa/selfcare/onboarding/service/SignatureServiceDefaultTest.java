package it.pagopa.selfcare.onboarding.service;

import eu.europa.esig.dss.detailedreport.jaxb.XmlDetailedReport;
import eu.europa.esig.dss.diagnostic.OrphanCertificateTokenWrapper;
import eu.europa.esig.dss.diagnostic.jaxb.*;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.EncryptionAlgorithm;
import eu.europa.esig.dss.model.DigestDocument;
import eu.europa.esig.dss.model.identifier.TokenIdentifierProvider;
import eu.europa.esig.dss.simplereport.jaxb.XmlSimpleReport;
import eu.europa.esig.dss.spi.signature.AdvancedSignature;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.reports.Reports;
import eu.europa.esig.dss.validation.timestamp.DetachedTimestampValidator;
import eu.europa.esig.dss.xades.validation.XAdESSignature;
import eu.europa.esig.validationreport.jaxb.ValidationReportType;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.service.impl.SignatureServiceDefault;
import jakarta.inject.Inject;
import org.apache.batik.anim.dom.BindableElement;
import org.apache.batik.anim.dom.SVG12DOMImplementation;
import org.apache.batik.anim.dom.SVG12OMDocument;
import org.apache.batik.dom.GenericDocumentType;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.math.BigInteger;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static it.pagopa.selfcare.onboarding.util.ErrorMessage.INVALID_DOCUMENT_SIGNATURE;
import static it.pagopa.selfcare.onboarding.util.ErrorMessage.TAX_CODE_NOT_FOUND_IN_SIGNATURE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@QuarkusTest
@QuarkusTestResource(value = MongoTestResource.class, restrictToAnnotatedClass = true)
class SignatureServiceDefaultTest {


    @Inject
    SignatureServiceDefault signatureService;

    /**
     * Method under test: {@link SignatureServiceDefault#isDocumentSigned(SignedDocumentValidator)}
     */
    @Test
    void isDocumentSigned_shouldThrowInvalidRequestException() {
        assertThrows(InvalidRequestException.class,
                () -> signatureService.isDocumentSigned(new DetachedTimestampValidator(new DigestDocument())));
    }

    /**
     * Method under test: {@link SignatureServiceDefault#isDocumentSigned(SignedDocumentValidator)}
     */
    @Test
    void testIsDocumentSigned3() {
        DetachedTimestampValidator detachedTimestampValidator = mock(DetachedTimestampValidator.class);
        when(detachedTimestampValidator.getSignatures()).thenReturn(new ArrayList<>());
        assertThrows(InvalidRequestException.class, () -> signatureService.isDocumentSigned(detachedTimestampValidator));
        verify(detachedTimestampValidator).getSignatures();
    }

    /**
     * Method under test: {@link SignatureServiceDefault#isDocumentSigned(SignedDocumentValidator)}
     */
    @Test
    void testIsDocumentSigned4() {
        ArrayList<AdvancedSignature> advancedSignatureList = new ArrayList<>();
        GenericDocumentType dt = new GenericDocumentType("Qualified Name", "42", "42");

        advancedSignatureList.add(new XAdESSignature(
                new BindableElement("Prefix", new SVG12OMDocument(dt, new SVG12DOMImplementation()), "Ns", "Ln")));
        DetachedTimestampValidator detachedTimestampValidator = mock(DetachedTimestampValidator.class);
        when(detachedTimestampValidator.getSignatures()).thenReturn(advancedSignatureList);
        signatureService.isDocumentSigned(detachedTimestampValidator);
        verify(detachedTimestampValidator).getSignatures();
    }

    @Test
    void validateDocument_shouldThrowInvalidRequestException() {
        SignedDocumentValidator signedDocumentValidator = mock(SignedDocumentValidator.class);
        when(signedDocumentValidator.validateDocument()).thenThrow(new RuntimeException());
        assertThrows(InvalidRequestException.class, () -> signatureService.validateDocument(signedDocumentValidator));
    }


    /**
     * Method under test: {@link SignatureServiceDefault#verifySignatureForm(SignedDocumentValidator)}
     */
    @Test
    void testVerifySignatureForm() {
        DetachedTimestampValidator detachedTimestampValidator = new DetachedTimestampValidator(new DigestDocument());
        assertDoesNotThrow(() -> signatureService.verifySignatureForm(detachedTimestampValidator));
        assertTrue(detachedTimestampValidator.getSignatures().isEmpty());
    }


    /**
     * Method under test: {@link SignatureServiceDefault#verifySignatureForm(SignedDocumentValidator)}
     */
    @Test
    void verifySignatureForm_withTokenIdentifierProvider() {
        DetachedTimestampValidator detachedTimestampValidator = new DetachedTimestampValidator(new DigestDocument());
        detachedTimestampValidator.setTokenIdentifierProvider(mock(TokenIdentifierProvider.class));
        assertDoesNotThrow(() -> signatureService.verifySignatureForm(detachedTimestampValidator));
        assertTrue(detachedTimestampValidator.getSignatures().isEmpty());
    }

    /**
     * Method under test: {@link SignatureServiceDefault#verifySignatureForm(SignedDocumentValidator)}
     */
    @Test
    void verifySignatureForm_shouldSignaturesNotEmpty() {
        SignedDocumentValidator signedDocumentValidator = mock(SignedDocumentValidator.class);
        when(signedDocumentValidator.getSignatures()).thenReturn(new ArrayList<>());
        assertDoesNotThrow(() -> signatureService.verifySignatureForm(signedDocumentValidator));
        verify(signedDocumentValidator).getSignatures();
    }

    /**
     * Method under test: {@link SignatureServiceDefault#verifySignatureForm(SignedDocumentValidator)}
     */
    @Test
    void testVerifySignatureForm6() {
        ArrayList<AdvancedSignature> advancedSignatureList = new ArrayList<>();
        GenericDocumentType dt = new GenericDocumentType("VALID", "42", "42");

        advancedSignatureList.add(new XAdESSignature(
                new BindableElement("VALID", new SVG12OMDocument(dt, new SVG12DOMImplementation()), "VALID", "VALID")));
        SignedDocumentValidator signedDocumentValidator = mock(SignedDocumentValidator.class);
        when(signedDocumentValidator.getSignatures()).thenReturn(advancedSignatureList);
        assertThrows(InvalidRequestException.class, () -> signatureService.verifySignatureForm(signedDocumentValidator),
                "Only CAdES signature form is admitted. Invalid signatures forms detected: [XAdES]");
        verify(signedDocumentValidator).getSignatures();
    }

    /**
     * Method under test: {@link SignatureServiceDefault#verifySignatureForm(SignedDocumentValidator)}
     */
    @Test
    void testVerifySignatureForm8() {
        ArrayList<AdvancedSignature> advancedSignatureList = new ArrayList<>();
        GenericDocumentType dt = new GenericDocumentType("Qualified Name", "42", "42");

        advancedSignatureList.add(new XAdESSignature(
                new BindableElement("Prefix", new SVG12OMDocument(dt, new SVG12DOMImplementation()), "Ns", "Ln")));
        advancedSignatureList.addAll(new ArrayList<>());
        GenericDocumentType dt1 = new GenericDocumentType("VALID", "42", "42");

        advancedSignatureList.add(new XAdESSignature(
                new BindableElement("VALID", new SVG12OMDocument(dt1, new SVG12DOMImplementation()), "VALID", "VALID")));
        SignedDocumentValidator signedDocumentValidator = mock(SignedDocumentValidator.class);
        when(signedDocumentValidator.getSignatures()).thenReturn(advancedSignatureList);
        assertThrows(InvalidRequestException.class, () -> signatureService.verifySignatureForm(signedDocumentValidator),
                "Only CAdES signature form is admitted. Invalid signatures forms detected: [XAdES, XAdES]");
        verify(signedDocumentValidator).getSignatures();
    }

    /**
     * Method under test: {@link SignatureServiceDefault#verifySignature(Reports)}
     */
    @Test
    void testVerifySignature() {
        XmlDiagnosticData diagnosticDataJaxb = new XmlDiagnosticData();
        XmlDetailedReport detailedReport = new XmlDetailedReport();
        XmlSimpleReport simpleReport = new XmlSimpleReport();
        Reports reports = new Reports(diagnosticDataJaxb, detailedReport, simpleReport, new ValidationReportType());

        assertThrows(InvalidRequestException.class, () -> signatureService.verifySignature(reports), INVALID_DOCUMENT_SIGNATURE.getMessage());
    }

    /**
     * Method under test: {@link SignatureServiceDefault#verifySignature(Reports)}
     */
    @Test
    void testVerifySignature2() {
        XmlDiagnosticData diagnosticDataJaxb = new XmlDiagnosticData();
        XmlDetailedReport detailedReport = new XmlDetailedReport();
        assertThrows(InvalidRequestException.class, () -> signatureService
                .verifySignature(new Reports(diagnosticDataJaxb, detailedReport, new XmlSimpleReport(), null)), INVALID_DOCUMENT_SIGNATURE.getMessage());
    }

    /**
     * Method under test: {@link SignatureServiceDefault#verifySignature(Reports)}
     */
    @Test
    void testVerifySignature3() {
        XmlDiagnosticData diagnosticDataJaxb = new XmlDiagnosticData();
        XmlDetailedReport detailedReport = new XmlDetailedReport();
        XmlSimpleReport simpleReport = new XmlSimpleReport();
        Reports reports = new Reports(diagnosticDataJaxb, detailedReport, simpleReport, new ValidationReportType());
        assertThrows(InvalidRequestException.class, () -> signatureService.verifySignature(reports), INVALID_DOCUMENT_SIGNATURE.getMessage());

        List<OrphanCertificateTokenWrapper> expectedSignatureValidationReport = reports.getDiagnosticData()
                .getAllOrphanCertificateObjects();
        assertEquals(expectedSignatureValidationReport,
                reports.getEtsiValidationReportJaxb().getSignatureValidationReport());
    }

    /**
     * Method under test: {@link SignatureServiceDefault#verifySignature(Reports)}
     */
    @Test
    void testVerifySignature4() {
        XmlDiagnosticData xmlDiagnosticData = new XmlDiagnosticData();
        xmlDiagnosticData.setValidationDate(mock(Date.class));
        XmlDetailedReport detailedReport = new XmlDetailedReport();
        XmlSimpleReport simpleReport = new XmlSimpleReport();
        Reports reports = new Reports(xmlDiagnosticData, detailedReport, simpleReport, new ValidationReportType());

        assertThrows(InvalidRequestException.class, () -> signatureService.verifySignature(reports), INVALID_DOCUMENT_SIGNATURE.getMessage());
    }

    /**
     * Method under test: {@link SignatureServiceDefault#verifySignature(Reports)}
     */
    @Test
    void testVerifySignature5() {
        XmlDiagnosticData diagnosticDataJaxb = mock(XmlDiagnosticData.class);
        XmlDetailedReport detailedReport = new XmlDetailedReport();
        XmlSimpleReport simpleReport = new XmlSimpleReport();
        Reports reports = new Reports(diagnosticDataJaxb, detailedReport, simpleReport, new ValidationReportType());

        assertThrows(InvalidRequestException.class, () -> signatureService.verifySignature(reports), INVALID_DOCUMENT_SIGNATURE.getMessage());

        List<OrphanCertificateTokenWrapper> expectedSignatureValidationReport = reports.getDiagnosticData()
                .getAllOrphanCertificateObjects();
        assertEquals(expectedSignatureValidationReport,
                reports.getEtsiValidationReportJaxb().getSignatureValidationReport());
    }

    /**
     * Method under test: {@link SignatureServiceDefault#verifyDigest(SignedDocumentValidator, String)}
     */
    @Test
    void testVerifyDigest() {
        DetachedTimestampValidator detachedTimestampValidator = new DetachedTimestampValidator(new DigestDocument());
        assertDoesNotThrow(() -> signatureService.verifyDigest(detachedTimestampValidator, "Checksum"));
        assertTrue(detachedTimestampValidator.getSignatures().isEmpty());
    }

    /**
     * Method under test: {@link SignatureServiceDefault#verifyDigest(SignedDocumentValidator, String)}
     */
    @Test
    void testVerifyDigest2() {
        DetachedTimestampValidator detachedTimestampValidator = new DetachedTimestampValidator(
                mock(DigestDocument.class));
        assertDoesNotThrow(() -> signatureService.verifyDigest(detachedTimestampValidator, "Checksum"));
        assertTrue(detachedTimestampValidator.getSignatures().isEmpty());
    }

    /**
     * Method under test: {@link SignatureServiceDefault#verifyDigest(SignedDocumentValidator, String)}
     */
    @Test
    void testVerifyDigest3() {
        DetachedTimestampValidator detachedTimestampValidator = new DetachedTimestampValidator(new DigestDocument());
        detachedTimestampValidator.setTokenIdentifierProvider(mock(TokenIdentifierProvider.class));
        assertDoesNotThrow(() -> signatureService.verifyDigest(detachedTimestampValidator, "Checksum"));
        assertTrue(detachedTimestampValidator.getSignatures().isEmpty());
    }


    /**
     * Method under test: {@link SignatureServiceDefault#verifyDigest(SignedDocumentValidator, String)}
     */
    @Test
    void testVerifyDigest5() {
        DetachedTimestampValidator detachedTimestampValidator = mock(DetachedTimestampValidator.class);
        when(detachedTimestampValidator.getSignatures()).thenReturn(new ArrayList<>());
        assertDoesNotThrow(() -> signatureService.verifyDigest(detachedTimestampValidator, "Checksum"));
        verify(detachedTimestampValidator).getSignatures();
    }

    /**
     * Method under test: {@link SignatureServiceDefault#verifyDigest(SignedDocumentValidator, String)}
     */
    @Test
    void testVerifyDigest6() {
        ArrayList<AdvancedSignature> advancedSignatureList = new ArrayList<>();
        GenericDocumentType dt = new GenericDocumentType("VALID", "42", "42");

        advancedSignatureList.add(new XAdESSignature(
                new BindableElement("VALID", new SVG12OMDocument(dt, new SVG12DOMImplementation()), "VALID", "VALID")));
        DetachedTimestampValidator detachedTimestampValidator = mock(DetachedTimestampValidator.class);
        when(detachedTimestampValidator.getOriginalDocuments((String) any())).thenReturn(new ArrayList<>());
        when(detachedTimestampValidator.getSignatures()).thenReturn(advancedSignatureList);
        assertThrows(InvalidRequestException.class, () -> signatureService.verifyDigest(detachedTimestampValidator, "Checksum"), "Invalid file digest");
        verify(detachedTimestampValidator).getSignatures();
        verify(detachedTimestampValidator).getOriginalDocuments((String) any());
    }

    /**
     * Method under test: {@link SignatureServiceDefault#verifyDigest(SignedDocumentValidator, String)}
     */
    @Test
    void testVerifyDigest9() {
        XAdESSignature xAdESSignature = mock(XAdESSignature.class);
        when(xAdESSignature.getId()).thenReturn("42");

        ArrayList<AdvancedSignature> advancedSignatureList = new ArrayList<>();
        advancedSignatureList.add(xAdESSignature);
        DetachedTimestampValidator detachedTimestampValidator = mock(DetachedTimestampValidator.class);
        when(detachedTimestampValidator.getOriginalDocuments((String) any())).thenReturn(new ArrayList<>());
        when(detachedTimestampValidator.getSignatures()).thenReturn(advancedSignatureList);
        assertThrows(InvalidRequestException.class, () -> signatureService.verifyDigest(detachedTimestampValidator, "Checksum"), "Invalid file digest");
        verify(detachedTimestampValidator).getSignatures();
        verify(detachedTimestampValidator).getOriginalDocuments((String) any());
        verify(xAdESSignature).getId();
    }

    /**
     * Method under test: {@link SignatureServiceDefault#verifyManagerTaxCode(Reports, List)}
     */
    @Test
    void testVerifyManagerTaxCode() {
        XmlDetailedReport detailedReport = new XmlDetailedReport();
        XmlSimpleReport simpleReport = new XmlSimpleReport();
        Reports reports = new Reports(dummyXmlDiagnosticData(), detailedReport, simpleReport, new ValidationReportType());

        assertThrows(InvalidRequestException.class, () -> signatureService.verifyManagerTaxCode(reports, new ArrayList<>()),
                TAX_CODE_NOT_FOUND_IN_SIGNATURE.getMessage());
    }

    XmlDiagnosticData dummyXmlDiagnosticData() {
        XmlDiagnosticData diagnosticDataJaxb = new XmlDiagnosticData();
        XmlCertificate xmlCertificate = new XmlCertificate();
        xmlCertificate.setSubjectSerialNumber("TINIT-EXAMPLE");
        diagnosticDataJaxb.setUsedCertificates(List.of(xmlCertificate));
        return diagnosticDataJaxb;
    }

    /**
     * Method under test: {@link SignatureServiceDefault#verifyManagerTaxCode(Reports, List)}
     */
    @Test
    void testVerifyManagerTaxCode2() {
        XmlDiagnosticData diagnosticDataJaxb = new XmlDiagnosticData();
        XmlDetailedReport detailedReport = new XmlDetailedReport();
        XmlSimpleReport simpleReport = new XmlSimpleReport();
        Reports reports = new Reports(diagnosticDataJaxb, detailedReport, simpleReport, new ValidationReportType());
        ArrayList<String> userList = new ArrayList<>();

        assertThrows(InvalidRequestException.class, () -> signatureService.verifyManagerTaxCode(reports, userList),
                TAX_CODE_NOT_FOUND_IN_SIGNATURE.getMessage());

        assertEquals(userList, reports.getDiagnosticDataJaxb().getUsedCertificates());
    }

    /**
     * Method under test: {@link SignatureServiceDefault#verifyManagerTaxCode(Reports, List)}
     */
    @Test
    void testVerifyManagerTaxCode4() {
        XmlDiagnosticData xmlDiagnosticData = new XmlDiagnosticData();
        xmlDiagnosticData.setValidationDate(mock(Date.class));
        XmlDetailedReport detailedReport = new XmlDetailedReport();
        XmlSimpleReport simpleReport = new XmlSimpleReport();
        Reports reports = new Reports(xmlDiagnosticData, detailedReport, simpleReport, new ValidationReportType());

        assertThrows(InvalidRequestException.class, () -> signatureService.verifyManagerTaxCode(reports, new ArrayList<>()),
                TAX_CODE_NOT_FOUND_IN_SIGNATURE.getMessage());
    }

    /**
     * Method under test: {@link SignatureServiceDefault#verifyManagerTaxCode(Reports, List)}
     */
    @Test
    void testVerifyManagerTaxCode5() {
        XmlDiagnosticData xmlDiagnosticData = mock(XmlDiagnosticData.class);
        when(xmlDiagnosticData.getUsedCertificates()).thenReturn(new ArrayList<>());
        XmlDetailedReport detailedReport = new XmlDetailedReport();
        XmlSimpleReport simpleReport = new XmlSimpleReport();
        Reports reports = new Reports(xmlDiagnosticData, detailedReport, simpleReport, new ValidationReportType());

        assertThrows(InvalidRequestException.class, () -> signatureService.verifyManagerTaxCode(reports, new ArrayList<>()),
                TAX_CODE_NOT_FOUND_IN_SIGNATURE.getMessage());

        verify(xmlDiagnosticData).getUsedCertificates();
    }

    /**
     * Method under test: {@link SignatureServiceDefault#verifyManagerTaxCode(Reports, List)}
     */
    @Test
    void testVerifyManagerTaxCode6() {
        XmlBasicSignature xmlBasicSignature = new XmlBasicSignature();
        xmlBasicSignature.setDigestAlgoUsedToSignThisToken(DigestAlgorithm.SHA1);
        xmlBasicSignature.setEncryptionAlgoUsedToSignThisToken(EncryptionAlgorithm.RSA);
        xmlBasicSignature.setKeyLengthUsedToSignThisToken("42");
        xmlBasicSignature.setSignatureIntact(true);
        xmlBasicSignature.setSignatureValid(true);

        XmlDigestAlgoAndValue xmlDigestAlgoAndValue = new XmlDigestAlgoAndValue();
        xmlDigestAlgoAndValue.setDigestMethod(DigestAlgorithm.SHA1);
        xmlDigestAlgoAndValue.setDigestValue(new byte[]{'A', 1, 'A', 1, 'A', 1, 'A', 1});
        xmlDigestAlgoAndValue.setMatch(true);

        XmlOriginalThirdCountryQcStatementsMapping xmlOriginalThirdCountryQcStatementsMapping = new XmlOriginalThirdCountryQcStatementsMapping();
        xmlOriginalThirdCountryQcStatementsMapping.setOtherOIDs(new ArrayList<>());
        xmlOriginalThirdCountryQcStatementsMapping.setQcCClegislation(new ArrayList<>());
        xmlOriginalThirdCountryQcStatementsMapping.setQcCompliance(new XmlQcCompliance());
        xmlOriginalThirdCountryQcStatementsMapping.setQcSSCD(new XmlQcSSCD());
        xmlOriginalThirdCountryQcStatementsMapping.setQcTypes(new ArrayList<>());


        XmlTrustServiceEquivalenceInformation trustServiceEquivalenceInformation = new XmlTrustServiceEquivalenceInformation();
        trustServiceEquivalenceInformation.setTrustServiceLegalIdentifier("42");
        trustServiceEquivalenceInformation.setCertificateContentEquivalenceList(new ArrayList<>());

        XmlMRACertificateMapping xmlMRACertificateMapping = new XmlMRACertificateMapping();
        xmlMRACertificateMapping.setTrustServiceEquivalenceInformation(trustServiceEquivalenceInformation);
        xmlMRACertificateMapping.setOriginalThirdCountryMapping(xmlOriginalThirdCountryQcStatementsMapping);

        XmlPSD2QcInfo xmlPSD2QcInfo = new XmlPSD2QcInfo();
        xmlPSD2QcInfo.setNcaId("42");
        xmlPSD2QcInfo.setNcaName("42");
        xmlPSD2QcInfo.setRolesOfPSP(new ArrayList<>());

        XmlQcCompliance xmlQcCompliance = new XmlQcCompliance();
        xmlQcCompliance.setPresent(true);

        XmlQcEuLimitValue xmlQcEuLimitValue = new XmlQcEuLimitValue();
        xmlQcEuLimitValue.setAmount(42);
        xmlQcEuLimitValue.setCurrency("42");
        xmlQcEuLimitValue.setExponent(42);

        XmlQcSSCD xmlQcSSCD = new XmlQcSSCD();
        xmlQcSSCD.setPresent(true);

        XmlOID xmlOID = new XmlOID();
        xmlOID.setDescription("42");
        xmlOID.setValue("42");

        XmlQcStatements xmlQcStatements = new XmlQcStatements();
        xmlQcStatements.setEnactedMRA(true);
        xmlQcStatements.setMRACertificateMapping(xmlMRACertificateMapping);
        xmlQcStatements.setOtherOIDs(new ArrayList<>());
        xmlQcStatements.setPSD2QcInfo(xmlPSD2QcInfo);
        xmlQcStatements.setQcCClegislation(new ArrayList<>());
        xmlQcStatements.setQcCompliance(xmlQcCompliance);
        xmlQcStatements.setQcEuLimitValue(xmlQcEuLimitValue);
        xmlQcStatements.setQcEuPDS(new ArrayList<>());
        xmlQcStatements.setQcEuRetentionPeriod(42);
        xmlQcStatements.setQcSSCD(xmlQcSSCD);
        xmlQcStatements.setQcTypes(new ArrayList<>());
        xmlQcStatements.setSemanticsIdentifier(xmlOID);

        XmlBasicSignature xmlBasicSignature1 = new XmlBasicSignature();
        xmlBasicSignature1.setDigestAlgoUsedToSignThisToken(DigestAlgorithm.SHA1);
        xmlBasicSignature1.setEncryptionAlgoUsedToSignThisToken(EncryptionAlgorithm.RSA);
        xmlBasicSignature1.setKeyLengthUsedToSignThisToken("42");
        xmlBasicSignature1.setSignatureIntact(true);
        xmlBasicSignature1.setSignatureValid(true);

        XmlDigestAlgoAndValue xmlDigestAlgoAndValue1 = new XmlDigestAlgoAndValue();
        xmlDigestAlgoAndValue1.setDigestMethod(DigestAlgorithm.SHA1);
        xmlDigestAlgoAndValue1.setDigestValue(new byte[]{'A', 1, 'A', 1, 'A', 1, 'A', 1});
        xmlDigestAlgoAndValue1.setMatch(true);

        XmlQcStatements xmlQcStatements1 = new XmlQcStatements();
        xmlQcStatements1.setEnactedMRA(true);
        xmlQcStatements1.setMRACertificateMapping(new XmlMRACertificateMapping());
        xmlQcStatements1.setOtherOIDs(new ArrayList<>());
        xmlQcStatements1.setPSD2QcInfo(new XmlPSD2QcInfo());
        xmlQcStatements1.setQcCClegislation(new ArrayList<>());
        xmlQcStatements1.setQcCompliance(new XmlQcCompliance());
        xmlQcStatements1.setQcEuLimitValue(new XmlQcEuLimitValue());
        xmlQcStatements1.setQcEuPDS(new ArrayList<>());
        xmlQcStatements1.setQcEuRetentionPeriod(42);
        xmlQcStatements1.setQcSSCD(new XmlQcSSCD());
        xmlQcStatements1.setQcTypes(new ArrayList<>());
        xmlQcStatements1.setSemanticsIdentifier(new XmlOID());

        XmlSigningCertificate xmlSigningCertificate = new XmlSigningCertificate();
        xmlSigningCertificate.setCertificate(new XmlCertificate());
        xmlSigningCertificate.setPublicKey(new byte[]{'A', 1, 'A', 1, 'A', 1, 'A', 1});

        XmlCertificate xmlCertificate = new XmlCertificate();
        xmlCertificate.setBase64Encoded(new byte[]{'A', 1, 'A', 1, 'A', 1, 'A', 1});
        xmlCertificate.setBasicSignature(xmlBasicSignature1);
        xmlCertificate.setCertificateExtensions(new ArrayList<>());
        xmlCertificate.setCertificateChain(new ArrayList<>());
        xmlCertificate.setCommonName("42");
        xmlCertificate.setCountryName("42");
        xmlCertificate.setDigestAlgoAndValue(xmlDigestAlgoAndValue1);
        xmlCertificate.setEmail("42");
        xmlCertificate.setEntityKey("42");
        xmlCertificate.setGivenName("42");
        xmlCertificate.setId("42");
        xmlCertificate.setLocality("42");
        xmlCertificate
                .setNotAfter(java.util.Date.from(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant()));
        xmlCertificate.setNotBefore(
                java.util.Date.from(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant()));
        xmlCertificate.setOrganizationIdentifier("42");
        xmlCertificate.setOrganizationName("42");
        xmlCertificate.setOrganizationalUnit("42");
        xmlCertificate.setPseudonym("42");
        xmlCertificate.setPublicKeyEncryptionAlgo(EncryptionAlgorithm.RSA);
        xmlCertificate.setPublicKeySize(42);
        xmlCertificate.setRevocations(new ArrayList<>());
        xmlCertificate.setSelfSigned(true);
        xmlCertificate.setSerialNumber(BigInteger.valueOf(42L));
        xmlCertificate.setSigningCertificate(xmlSigningCertificate);
        xmlCertificate.setSources(new ArrayList<>());
        xmlCertificate.setState("42");
        xmlCertificate.setSubjectSerialNumber("42");
        xmlCertificate.setSurname("42");
        xmlCertificate.setTitle("42");
        XmlTrusted xmlTrusted = new XmlTrusted();
        xmlTrusted.setValue(false);
        xmlCertificate.setTrusted(xmlTrusted);
        xmlCertificate.setTrustServiceProviders(new ArrayList<>());

        XmlSigningCertificate xmlSigningCertificate1 = new XmlSigningCertificate();
        xmlSigningCertificate1.setCertificate(xmlCertificate);
        xmlSigningCertificate1.setPublicKey(new byte[]{'A', 1, 'A', 1, 'A', 1, 'A', 1});

        XmlCertificate xmlCertificate1 = new XmlCertificate();
        xmlCertificate1.setBase64Encoded(new byte[]{'A', 1, 'A', 1, 'A', 1, 'A', 1});
        xmlCertificate1.setBasicSignature(xmlBasicSignature);
        xmlCertificate1.setCertificateChain(new ArrayList<>());
        xmlCertificate1.setCommonName("42");
        xmlCertificate1.setCountryName("42");
        xmlCertificate1.setDigestAlgoAndValue(xmlDigestAlgoAndValue);
        xmlCertificate1.setEmail("42");
        xmlCertificate1.setEntityKey("42");
        xmlCertificate1.setGivenName("42");
        xmlCertificate1.setId("42");
        xmlCertificate1.setLocality("42");
        xmlCertificate1
                .setNotAfter(java.util.Date.from(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant()));
        xmlCertificate1.setNotBefore(
                java.util.Date.from(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant()));
        xmlCertificate1.setOrganizationIdentifier("42");
        xmlCertificate1.setOrganizationName("42");
        xmlCertificate1.setOrganizationalUnit("42");
        xmlCertificate1.setPseudonym("42");
        xmlCertificate1.setPublicKeyEncryptionAlgo(EncryptionAlgorithm.RSA);
        xmlCertificate1.setPublicKeySize(42);
        xmlCertificate1.setRevocations(new ArrayList<>());
        xmlCertificate1.setSelfSigned(true);
        xmlCertificate1.setSerialNumber(BigInteger.valueOf(42L));
        xmlCertificate1.setSigningCertificate(xmlSigningCertificate1);
        xmlCertificate1.setSources(new ArrayList<>());
        xmlCertificate1.setState("42");
        xmlCertificate1.setSubjectSerialNumber("42");
        xmlCertificate1.setSurname("42");
        xmlCertificate1.setTitle("42");
        XmlTrusted xmlTrusted1 = new XmlTrusted();
        xmlTrusted1.setValue(false);
        xmlCertificate1.setTrusted(xmlTrusted1);
        xmlCertificate1.setTrustServiceProviders(new ArrayList<>());


        ArrayList<XmlCertificate> xmlCertificateList = new ArrayList<>();
        xmlCertificateList.add(xmlCertificate1);
        XmlDiagnosticData xmlDiagnosticData = mock(XmlDiagnosticData.class);
        when(xmlDiagnosticData.getUsedCertificates()).thenReturn(xmlCertificateList);
        XmlDetailedReport detailedReport = new XmlDetailedReport();
        XmlSimpleReport simpleReport = new XmlSimpleReport();
        Reports reports = new Reports(xmlDiagnosticData, detailedReport, simpleReport, new ValidationReportType());

        assertThrows(InvalidRequestException.class, () -> signatureService.verifyManagerTaxCode(reports, new ArrayList<>()),
                TAX_CODE_NOT_FOUND_IN_SIGNATURE.getMessage());

        verify(xmlDiagnosticData).getUsedCertificates();
    }

    /**
     * Method under test: {@link SignatureServiceDefault#verifyManagerTaxCode(Reports, List)}
     */
    @Test
    void verifyManagerTaxCode() {
        Reports reports = mock(Reports.class);
        when(reports.getDiagnosticData()).thenReturn(null);

        assertThrows(InvalidRequestException.class, () -> signatureService.verifyManagerTaxCode(reports, new ArrayList<>()),
                TAX_CODE_NOT_FOUND_IN_SIGNATURE.getMessage());

    verify(reports).getDiagnosticData();
  }

  @Test
  void isP7mValidOKTest() {
    // given
    ClassLoader classLoader = getClass().getClassLoader();
    String resourcePath = Objects.requireNonNull(classLoader.getResource("documents/test.pdf.p7m")).getPath();
    assertDoesNotThrow(() -> signatureService.verifySignature(new File(resourcePath)));
  }

  @Test
  void isP7mValidKOTest() {
    // given
    ClassLoader classLoader = getClass().getClassLoader();
    String resourcePath = Objects.requireNonNull(classLoader.getResource("documents/contract_error.p7m")).getPath();
    assertThrows(InvalidRequestException.class, () -> signatureService.verifySignature(new File(resourcePath)));
  }

  @Test
  void extractOriginalDocumentTest() {
    ClassLoader classLoader = getClass().getClassLoader();
    String resourcePath = Objects.requireNonNull(classLoader.getResource("documents/test.pdf.p7m")).getPath();
    assertDoesNotThrow(() -> SignatureServiceDefault.extractOriginalDocument(new File(resourcePath)));
  }

  @Test
  void extractOriginalDocumentErrorTest() {
    ClassLoader classLoader = getClass().getClassLoader();
    String resourcePath = Objects.requireNonNull(classLoader.getResource("documents/contract_error.p7m")).getPath();
    assertThrows(InvalidRequestException.class, () -> SignatureServiceDefault.extractOriginalDocument(new File(resourcePath)));
  }

  @Test
  void extractFileTest() {
    ClassLoader classLoader = getClass().getClassLoader();
    String resourcePath = Objects.requireNonNull(classLoader.getResource("documents/test.pdf.p7m")).getPath();
    assertDoesNotThrow(() -> signatureService.extractFile(new File(resourcePath)));
  }

  @Test
  void extractFileErrorTest() {
    ClassLoader classLoader = getClass().getClassLoader();
    String resourcePath = Objects.requireNonNull(classLoader.getResource("documents/contract_error.p7m")).getPath();
    assertThrows(InvalidRequestException.class, () -> signatureService.extractFile(new File(resourcePath)));
  }
}

