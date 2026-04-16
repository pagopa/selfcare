package it.pagopa.selfcare.onboarding.crypto;


import it.pagopa.selfcare.onboarding.crypto.entity.SignatureInformation;

import java.io.File;

public interface PadesSignService {
    void padesSign(File pdfFile, File signedPdfFile, SignatureInformation signInfo);
}
