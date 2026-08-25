package it.pagopa.selfcare.commons.utils.crypto.service;

import it.pagopa.selfcare.commons.utils.crypto.model.SignatureInformation;

import java.io.File;

/** It allows to build PAdES digital signature on a single pdf. */
public interface PadesSignService {
    void padesSign(File pdfFile, File signedPdfFile, SignatureInformation signInfo);
}
