package it.pagopa.selfcare.commons.utils.crypto.service;

import it.pagopa.selfcare.commons.utils.crypto.model.SignatureInformation;
import it.pagopa.selfcare.commons.utils.crypto.utils.CryptoUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;

@Service
public class PadesSignServiceImpl implements PadesSignService {

    private final Pkcs7HashSignService pkcs7Signature;

    public PadesSignServiceImpl(Pkcs7HashSignService pkcs7Signature) {
        this.pkcs7Signature = pkcs7Signature;
    }

    @Override
    public void padesSign(File pdfFile, File signedPdfFile, SignatureInformation signInfo) {
        CryptoUtils.createParentDirectoryIfNotExists(signedPdfFile);

        try (
                FileOutputStream fos = new FileOutputStream(signedPdfFile);
                PDDocument doc = PDDocument.load(pdfFile)
        ) {
            // create signature dictionary
            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);

            signature.setName(signInfo.getName());
            signature.setLocation(signInfo.getLocation());
            signature.setReason(signInfo.getReason());

            // the signing date, needed for valid signature
            signature.setSignDate(Calendar.getInstance());

            SignatureOptions signatureOptions = new SignatureOptions();
            // Size can vary, but should be enough for purpose.
            signatureOptions.setPreferredSignatureSize(SignatureOptions.DEFAULT_SIGNATURE_SIZE * 2);
            // register signature dictionary and sign interface
            doc.addSignature(signature, pkcs7Signature, signatureOptions);

            // write incremental (only for signing purpose)
            doc.saveIncremental(fos);

        } catch (Exception e) {
            throw new IllegalStateException(String.format("Something gone wrong while signing input pdf %s and storing it into %s", pdfFile.getAbsolutePath(), signedPdfFile.getAbsolutePath()), e);
        }
    }

}
