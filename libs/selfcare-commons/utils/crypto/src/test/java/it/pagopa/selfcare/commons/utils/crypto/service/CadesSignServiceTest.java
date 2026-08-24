package it.pagopa.selfcare.commons.utils.crypto.service;

import it.pagopa.selfcare.commons.utils.crypto.config.CryptoConfig;
import it.pagopa.selfcare.commons.utils.crypto.config.LocalCryptoConfig;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.Iterator;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {CadesSignServiceImpl.class, CryptoConfig.class, LocalCryptoConfig.class, Pkcs7HashSignServiceImpl.class})
@SuppressWarnings("squid:S5786") // There is at least one module which will extend this test class
public class CadesSignServiceTest {

    @Autowired
    private CadesSignService service;

    // mocked data will not be aligned with timestamp alway updated, thus base test could not successfully sign
    protected boolean verifySignerInformation = true;
    protected Path inputFilePath = Path.of("src/test/resources/signTest.pdf");

    @Test
    protected void testCadesSign() throws IOException, CertificateException, OperatorCreationException, CMSException {
        File inputFile = inputFilePath.toFile();
        File outputFile = getOutputPadesFile();
        if (outputFile.exists()) {
            Assertions.assertTrue(outputFile.delete());
        }

        service.cadesSign(inputFile, outputFile);
        Assertions.assertTrue(outputFile.exists());

        checkPadesSignature(outputFile);
    }

    protected File getOutputPadesFile() {
        return Path.of("target/tmp/signedSignTest-selfSigned.pdf.p7m").toFile();
    }

    public void checkPadesSignature(File signedFile) throws FileNotFoundException, CMSException, CertificateException, OperatorCreationException {
            CMSSignedData s = new CMSSignedData(new FileInputStream(signedFile));
            Store<X509CertificateHolder> certs = s.getCertificates();
            SignerInformationStore signers = s.getSignerInfos();
            Collection<SignerInformation> c = signers.getSigners();

            Assertions.assertFalse(c.isEmpty(), "The inputStream has not sign");

            if(verifySignerInformation) {
                for (SignerInformation signer : c) {
                    @SuppressWarnings("unchecked")
                    Collection<X509CertificateHolder> certCollection = certs.getMatches(signer.getSID());
                    Iterator<X509CertificateHolder> certIt = certCollection.iterator();
                    X509CertificateHolder certHolder = certIt.next();
                    boolean result = signer.verify(new JcaSimpleSignerInfoVerifierBuilder().build(certHolder));
                    Assertions.assertTrue(result, String.format("The inputStream has an invalid sign: %s", signer.getSID()));
                }
            }
    }
}