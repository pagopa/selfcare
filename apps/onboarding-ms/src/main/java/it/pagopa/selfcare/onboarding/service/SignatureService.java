package it.pagopa.selfcare.onboarding.service;

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.spi.signature.AdvancedSignature;
import eu.europa.esig.dss.validation.SignedDocumentValidator;

import java.io.File;
import java.util.List;

public interface SignatureService {
    void verifySignature(File file, String checksum, List<String> usersTaxCode);

    boolean verifySignature(File file);

    File extractFile(File contract);

    DSSDocument extractPdfFromSignedContainer(SignedDocumentValidator validator, DSSDocument inputDoc);

    String computeDigestOfSignedRevision(SignedDocumentValidator validator, DSSDocument doc);

    AdvancedSignature chooseEarliestSignature(List<AdvancedSignature> sigs);
}
