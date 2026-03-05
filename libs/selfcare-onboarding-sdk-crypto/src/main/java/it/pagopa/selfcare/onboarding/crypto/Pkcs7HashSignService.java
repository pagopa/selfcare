package it.pagopa.selfcare.onboarding.crypto;

import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;

public interface Pkcs7HashSignService extends SignatureInterface {

    /**
     * Determines whether the signing service returns a fully signed PDF.
     * - If the service returns the entire PDF signed, this method should return true.
     * - If the service returns only the PKCS7 signature, this method should return false.
     */
    boolean returnsFullPdf();
}
