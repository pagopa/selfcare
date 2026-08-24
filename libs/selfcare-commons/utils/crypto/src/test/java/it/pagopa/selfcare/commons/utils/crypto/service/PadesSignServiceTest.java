package it.pagopa.selfcare.commons.utils.crypto.service;

import it.pagopa.selfcare.commons.utils.crypto.config.CryptoConfig;
import it.pagopa.selfcare.commons.utils.crypto.config.LocalCryptoConfig;
import it.pagopa.selfcare.commons.utils.crypto.model.SignatureInformation;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.Store;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.List;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {PadesSignServiceImpl.class, CryptoConfig.class, LocalCryptoConfig.class, Pkcs7HashSignServiceImpl.class})
@SuppressWarnings("squid:S5786") // There is at least one module which will extend this test class
public class PadesSignServiceTest {

    @Autowired
    private PadesSignService service;
    @Autowired(required = false)
    private LocalCryptoConfig localCryptoConfig;

    // mocked data will not be aligned with timestamp alway updated, thus base test could not successfully sign
    protected boolean verifySignerInformation = true;
    protected Path inputFilePath = Path.of("src/test/resources/signTest.pdf");

    @Test
    protected void testPadesSign() throws IOException, GeneralSecurityException, OperatorCreationException, CMSException {
        File inputFile = inputFilePath.toFile();
        File outputFile = getOutputPadesFile();
        if (outputFile.exists()) {
            Assertions.assertTrue(outputFile.delete());
        }

        service.padesSign(inputFile, outputFile, new SignatureInformation("PagoPA S.P.A", "Rome", "onboarding contract"));
        Assertions.assertTrue(outputFile.exists());

        checkPadesSignature(inputFile, outputFile);
    }

    protected File getOutputPadesFile() {
        return Path.of("target/tmp/signedSignTest-selfSigned.pdf").toFile();
    }

    @SuppressWarnings("unchecked")
    private void checkPadesSignature(File origFile, File signedFile)
            throws IOException, CMSException, OperatorCreationException, GeneralSecurityException
    {
        PDDocument document = PDDocument.load(origFile);
        // get string representation of pages COSObject
        String origPageKey = document.getDocumentCatalog().getCOSObject().getItem(COSName.PAGES).toString();
        document.close();

        document = PDDocument.load(signedFile);

        // early detection of problems in the page structure
        int p = 0;
        PDPageTree pageTree = document.getPages();
        for (PDPage page : document.getPages())
        {
            Assertions.assertEquals(p, pageTree.indexOf(page));
            ++p;
        }

        Assertions.assertEquals(origPageKey, document.getDocumentCatalog().getCOSObject().getItem(COSName.PAGES).toString());

        List<PDSignature> signatureDictionaries = document.getSignatureDictionaries();
        if (signatureDictionaries.isEmpty())
        {
            Assertions.fail("no signature found");
        }
        for (PDSignature sig : document.getSignatureDictionaries())
        {
            byte[] contents = sig.getContents();

            byte[] buf = sig.getSignedContent(new FileInputStream(signedFile));

            // verify that getSignedContent() brings the same content
            // regardless whether from an InputStream or from a byte array
            FileInputStream fis2 = new FileInputStream(signedFile);
            byte[] buf2 = sig.getSignedContent(IOUtils.toByteArray(fis2));
            Assertions.assertArrayEquals(buf, buf2);
            fis2.close();

            // verify that all getContents() methods returns the same content
            FileInputStream fis3 = new FileInputStream(signedFile);
            byte[] contents2 = sig.getContents(IOUtils.toByteArray(fis3));
            Assertions.assertArrayEquals(contents, contents2);
            fis3.close();
            byte[] contents3 = sig.getContents(new FileInputStream(signedFile));
            Assertions.assertArrayEquals(contents, contents3);

            CMSSignedData signedData = new CMSSignedData(new CMSProcessableByteArray(buf), contents);
            Store<?> certificatesStore = signedData.getCertificates();
            Collection<SignerInformation> signers = signedData.getSignerInfos().getSigners();
            SignerInformation signerInformation = signers.iterator().next();
            Collection<?> matches = certificatesStore.getMatches(signerInformation.getSID());
            X509CertificateHolder certificateHolder = (X509CertificateHolder) matches.iterator().next();
            verifyCertificateHolder(certificateHolder);
            if(verifySignerInformation){
                verifySignerInformation(signerInformation, certificateHolder);
            }
        }
        document.close();
    }

    protected void verifyCertificateHolder(X509CertificateHolder certificateHolder) throws IOException, CertificateEncodingException {
        Assertions.assertArrayEquals(localCryptoConfig.getCertificate().getEncoded(), certificateHolder.getEncoded());
    }

    protected void verifySignerInformation(SignerInformation signerInformation, X509CertificateHolder certificateHolder)
            throws CertificateException, OperatorCreationException, CMSException {
        // CMSVerifierCertificateNotValidException means that the keystore wasn't valid at signing time
        if (!signerInformation.verify(new JcaSimpleSignerInfoVerifierBuilder().build(certificateHolder)))
        {
            Assertions.fail("Signature verification failed");
        }
    }
}