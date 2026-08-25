package it.pagopa.selfcare.commons.utils.crypto.service;

import java.io.File;

/** It allows to build CAdES digital signature on a single file. */
public interface CadesSignService {
    void cadesSign(File pdfFile, File signedPdfFile);
}
