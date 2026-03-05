package it.pagopa.selfcare.onboarding.service.impl;

import eu.europa.esig.dss.diagnostic.CertificateWrapper;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.Indication;
import eu.europa.esig.dss.enumerations.SignatureForm;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.service.crl.OnlineCRLSource;
import eu.europa.esig.dss.service.ocsp.OnlineOCSPSource;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.spi.signature.AdvancedSignature;
import eu.europa.esig.dss.spi.tsl.TrustedListsCertificateSource;
import eu.europa.esig.dss.spi.validation.CertificateVerifier;
import eu.europa.esig.dss.spi.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.spi.x509.aia.DefaultAIASource;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.reports.Reports;
import eu.europa.esig.validationreport.jaxb.SignatureValidationReportType;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.service.SignatureService;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static it.pagopa.selfcare.onboarding.util.ErrorMessage.*;


@Slf4j
@ApplicationScoped
public class SignatureServiceDefault implements SignatureService {

  private static final Integer CF_MATCHER_GROUP = 2;
  private static final Pattern signatureRegex = Pattern.compile("(TINIT-)(.*)");

  private final TrustedListsCertificateSource trustedListsCertificateSource;

  public SignatureServiceDefault(TrustedListsCertificateSource trustedListsCertificateSource) {
    this.trustedListsCertificateSource = trustedListsCertificateSource;
  }


  public SignedDocumentValidator createDocumentValidator(byte[] bytes) {

    CertificateVerifier certificateVerifier = new CommonCertificateVerifier();
    certificateVerifier.setTrustedCertSources(trustedListsCertificateSource);
    certificateVerifier.setAIASource(new DefaultAIASource());
    certificateVerifier.setOcspSource(new OnlineOCSPSource());
    certificateVerifier.setCrlSource(new OnlineCRLSource());
    try {
      DSSDocument document = new InMemoryDocument(bytes);
      SignedDocumentValidator documentValidator = SignedDocumentValidator.fromDocument(document);
      documentValidator.setCertificateVerifier(certificateVerifier);
      return documentValidator;
    } catch (Exception e) {
      log.error("Error message: {}", e.getMessage(), e);
      throw new InvalidRequestException(DOCUMENT_VALIDATION_FAIL.getMessage(), DOCUMENT_VALIDATION_FAIL.getCode());
    }
  }

  public void isDocumentSigned(SignedDocumentValidator documentValidator) {
    if (documentValidator.getSignatures().isEmpty()) {
      throw new InvalidRequestException(SIGNATURE_VALIDATION_ERROR.getMessage(), SIGNATURE_VALIDATION_ERROR.getCode());
    }
  }

  public void verifyOriginalDocument(SignedDocumentValidator validator) {
    List<AdvancedSignature> advancedSignatures = validator.getSignatures();
    List<DSSDocument> dssDocuments = new ArrayList<>();
    if (advancedSignatures != null && !advancedSignatures.isEmpty()) {
      for (AdvancedSignature a : advancedSignatures) {
        Optional.ofNullable(validator.getOriginalDocuments(a.getId())).ifPresent(dssDocuments::addAll);
      }
    }
    if (dssDocuments.isEmpty()) {
      throw new InvalidRequestException(ORIGINAL_DOCUMENT_NOT_FOUND.getMessage(), ORIGINAL_DOCUMENT_NOT_FOUND.getCode());
    }
  }

  public Reports validateDocument(SignedDocumentValidator signedDocumentValidator) {
    try {
      return signedDocumentValidator.validateDocument();
    } catch (Exception e) {
      throw new InvalidRequestException(String.format(DOCUMENT_VALIDATION_FAIL.getMessage(), e.getMessage()), DOCUMENT_VALIDATION_FAIL.getCode());
    }
  }

  public void verifySignatureForm(SignedDocumentValidator validator) {

    String signatureFormErrors = validator.getSignatures().stream()
      .map(AdvancedSignature::getSignatureForm)
      .filter(signatureForm -> signatureForm != SignatureForm.CAdES)
      .map(SignatureForm::toString)
      .collect(Collectors.joining(","));

    if (!StringUtils.isBlank(signatureFormErrors)) {
      throw new InvalidRequestException(String.format(INVALID_SIGNATURE_FORMS.getMessage(), signatureFormErrors),
        INVALID_SIGNATURE_FORMS.getCode());
    }
  }

  public void verifySignature(Reports reports) {
    List<SignatureValidationReportType> signatureValidationReportTypes = new ArrayList<>();

    if (reports.getEtsiValidationReportJaxb() != null) {
      signatureValidationReportTypes = reports.getEtsiValidationReportJaxb().getSignatureValidationReport();
    }
    if (signatureValidationReportTypes.isEmpty()
      || (!signatureValidationReportTypes.stream().allMatch(s -> s.getSignatureValidationStatus() != null
      && Indication.TOTAL_PASSED == s.getSignatureValidationStatus().getMainIndication()))) {
      throw new InvalidRequestException(INVALID_DOCUMENT_SIGNATURE.getMessage(), INVALID_DOCUMENT_SIGNATURE.getCode());
    }
  }

  public void checkSignature(Reports reports) {
    List<SignatureValidationReportType> signatureValidationReportTypes = new ArrayList<>();

    if (reports.getEtsiValidationReportJaxb() != null) {
      signatureValidationReportTypes = reports.getEtsiValidationReportJaxb().getSignatureValidationReport();
    }
    if (signatureValidationReportTypes.isEmpty()
      || (!signatureValidationReportTypes.stream().allMatch(s -> s.getSignatureValidationStatus() != null))) {
      throw new InvalidRequestException(INVALID_DOCUMENT_SIGNATURE.getMessage(), INVALID_DOCUMENT_SIGNATURE.getCode());
    }
  }

  public void verifyDigest(SignedDocumentValidator validator, String checksum) {
    List<AdvancedSignature> advancedSignatures = validator.getSignatures();

    if (advancedSignatures != null && !advancedSignatures.isEmpty()) {
      for (AdvancedSignature a : advancedSignatures) {

        List<DSSDocument> dssDocuments = validator.getOriginalDocuments(a.getId());
        if (!dssDocuments.stream().map(dssDocument -> dssDocument.getDigest(DigestAlgorithm.SHA256).getBase64Value())
          .toList().contains(checksum)) {
          throw new InvalidRequestException(INVALID_CONTRACT_DIGEST.getMessage(), INVALID_CONTRACT_DIGEST.getCode());
        }
      }
    }

  }

  @Override
  public void verifySignature(File file, String checksum, List<String> usersTaxCode) {
    try {
      byte[] byteData = Files.readAllBytes(file.toPath());

      SignedDocumentValidator validator = createDocumentValidator(byteData);
      isDocumentSigned(validator);
      verifyOriginalDocument(validator);
      Reports reports = validateDocument(validator);

      verifySignatureForm(validator);
      verifySignature(reports);
      verifyDigest(validator, checksum);
      verifyManagerTaxCode(reports, usersTaxCode);

    } catch (InvalidRequestException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(GENERIC_ERROR.getMessage(), GENERIC_ERROR.getCode());
    }
  }

  @Override
  public boolean verifySignature(File file) {
    try {
      byte[] byteData = Files.readAllBytes(file.toPath());

      SignedDocumentValidator validator = createDocumentValidator(byteData);
      isDocumentSigned(validator);
      verifyOriginalDocument(validator);
      Reports reports = validateDocument(validator);

      verifySignatureForm(validator);
      checkSignature(reports);
      return true;
    } catch (Exception e) {
      throw new InvalidRequestException(GENERIC_ERROR.getMessage(), GENERIC_ERROR.getCode());
    }
  }

  public void verifyManagerTaxCode(Reports reports, List<String> usersTaxCode) {
    List<String> signatureTaxCodes = extractSubjectSNCFs(reports);
    if (signatureTaxCodes.isEmpty()) {
      throw new InvalidRequestException(TAX_CODE_NOT_FOUND_IN_SIGNATURE.getMessage(), TAX_CODE_NOT_FOUND_IN_SIGNATURE.getCode());
    }

    List<String> taxCodes = extractTaxCode(signatureTaxCodes);

    if (taxCodes.isEmpty() || !isSignedByLegal(usersTaxCode, taxCodes)) {
      throw new InvalidRequestException(INVALID_SIGNATURE_TAX_CODE.getMessage(), INVALID_SIGNATURE_TAX_CODE.getCode());
    }

  }

  private List<String> extractTaxCode(List<String> signatureTaxCodes) {
    List<String> taxCode = new ArrayList<>();
    signatureTaxCodes.forEach(s -> {
      Matcher matcher = signatureRegex.matcher(s);
      if (matcher.matches()) {
        taxCode.add(matcher.group(CF_MATCHER_GROUP));
      }
    });
    return taxCode;
  }

  private boolean isSignedByLegal(List<String> usersTaxCode, List<String> signatureTaxCodes) {
    return !signatureTaxCodes.isEmpty() && !usersTaxCode.isEmpty()
      && new HashSet<>(signatureTaxCodes).containsAll(usersTaxCode);
  }

  private List<String> extractSubjectSNCFs(Reports reports) {
    if (reports.getDiagnosticData() != null && reports.getDiagnosticData().getUsedCertificates() != null) {
      List<String> subjectSNCFs = reports.getDiagnosticData().getUsedCertificates()
        .stream().map(CertificateWrapper::getSubjectSerialNumber)
        .filter(this::serialNumberMatch).collect(Collectors.toList());
      if (!subjectSNCFs.isEmpty()) {
        return subjectSNCFs;
      }
    }
    return Collections.emptyList();
  }

  private boolean serialNumberMatch(String s) {
    if (!StringUtils.isEmpty(s)) {
      return signatureRegex.matcher(s).matches();
    }
    return false;
  }

  public static DSSDocument extractOriginalDocument(File contract) {
    try {
      DSSDocument signedDocument = new FileDocument(contract);

      SignedDocumentValidator validator = SignedDocumentValidator.fromDocument(signedDocument);
      validator.setCertificateVerifier(new CommonCertificateVerifier());

      List<AdvancedSignature> signatures = validator.getSignatures();
      if (signatures.isEmpty()) {
        throw new InvalidRequestException(SIGNATURE_NOT_FOUND.getMessage(), SIGNATURE_NOT_FOUND.getCode());
      }

      List<DSSDocument> originalDocuments = validator.getOriginalDocuments(signatures.get(0).getId());
      if (originalDocuments.isEmpty()) {
        throw new InvalidRequestException(ORIGINAL_DOCUMENT_NOT_FOUND.getMessage(), ORIGINAL_DOCUMENT_NOT_FOUND.getCode());
      }

      return originalDocuments.get(0);
    } catch (Exception e) {
      throw new InvalidRequestException(ORIGINAL_DOCUMENT_NOT_FOUND.getMessage(), ORIGINAL_DOCUMENT_NOT_FOUND.getCode());
    }
  }

  @Override
  public File extractFile(File contract) {
    DSSDocument originalContract = extractOriginalDocument(contract);
    if (originalContract == null) {
      throw new ResourceNotFoundException(INVALID_DOCUMENT_SIGNATURE.getMessage(), INVALID_SIGNATURE_TAX_CODE.getCode());
    }

    try {
      String filePath = contract.getAbsolutePath();
      File destination = new File(filePath + ".pdf");
      DSSUtils.saveToFile(DSSUtils.toByteArray(originalContract.openStream()), destination);
      return destination;
    } catch (Exception e) {
      throw new ResourceNotFoundException(INVALID_DOCUMENT_SIGNATURE.getMessage(), INVALID_SIGNATURE_TAX_CODE.getCode());
    }
  }

    /**
     * If validator exposes original documents for a signature, returns the first original (signed) content.
     * Otherwise returns the input document unchanged.
     */
    public DSSDocument extractPdfFromSignedContainer(SignedDocumentValidator validator, DSSDocument inputDoc) {
        List<AdvancedSignature> sigs = validator.getSignatures();
        if (sigs == null || sigs.isEmpty()) {
            log.debug("No signatures detected; using document as-is.");
            return inputDoc;
        }

        AdvancedSignature selected = chooseEarliestSignature(sigs);
        log.debug("Selected signature id='{}' signingTime='{}' for unwrapping", selected.getId(), selected.getSigningTime());

        List<DSSDocument> originals = validator.getOriginalDocuments(selected.getId());
        if (originals != null && !originals.isEmpty()) {
            log.debug("Unwrapped {} original document(s) from signature id='{}'", originals.size(), selected.getId());
            return originals.get(0);
        }

        log.debug("No originals available for signature id='{}'; returning input document", selected.getId());
        return inputDoc;
    }

    /**
     * Compute SHA-256 (Base64) for the document content corresponding to the chosen signature revision.
     * If there are no signatures, compute digest of the whole document bytes.
     */
    public String computeDigestOfSignedRevision(SignedDocumentValidator validator, DSSDocument doc) {
        List<AdvancedSignature> signatures = validator.getSignatures();
        if (signatures == null || signatures.isEmpty()) {
            log.debug("No PAdES signatures found - computing full-document digest.");
            return doc.getDigest(DigestAlgorithm.SHA256).getBase64Value();
        }

        AdvancedSignature chosen = chooseEarliestSignature(signatures);
        log.debug("Using signature id='{}' signingTime='{}' to compute content digest", chosen.getId(), chosen.getSigningTime());

        List<DSSDocument> originals = validator.getOriginalDocuments(chosen.getId());
        DSSDocument contentDoc = (originals != null && !originals.isEmpty()) ? originals.get(0) : doc;

        String digest = contentDoc.getDigest(DigestAlgorithm.SHA256).getBase64Value();
        log.debug("Computed content digest (base64) for signature id='{}': {}", chosen.getId(), digest);
        return digest;
    }

    /**
     * Choose earliest signature by signingTime, fallback to id ordering.
     */
    public AdvancedSignature chooseEarliestSignature(List<AdvancedSignature> sigs) {
        return sigs.stream()
                .min(Comparator.comparing(AdvancedSignature::getSigningTime, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(AdvancedSignature::getId))
                .orElse(sigs.get(0));
    }
}
