package it.pagopa.selfcare.document.crypto;


import it.pagopa.selfcare.document.crypto.entity.SignatureInformation;

import java.io.File;

public interface PadesSignService {
    void padesSign(File pdfFile, File signedPdfFile, SignatureInformation signInfo);
}
