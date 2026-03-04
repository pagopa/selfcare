package it.pagopa.selfcare.onboarding.crypto;


import it.pagopa.selfcare.onboarding.crypto.config.LocalCryptoConfig;
import it.pagopa.selfcare.onboarding.crypto.config.LocalCryptoInitializer;
import it.pagopa.selfcare.onboarding.crypto.utils.CMSTypedDataInputStream;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateEncodingException;
import java.util.Collections;

/**
 * Implementation of {@link Pkcs7HashSignService} which will use provided private and public keys to perform sign operations
 */
public class Pkcs7HashSignServiceImpl implements Pkcs7HashSignService {

    private final CMSSignedDataGenerator cmsSignGenerator;

    public Pkcs7HashSignServiceImpl() {
        try {
            LocalCryptoConfig localCryptoConfig = LocalCryptoInitializer.initializeConfig();
            BouncyCastleProvider bc = new BouncyCastleProvider();
            Store<?> certStore = new JcaCertStore(Collections.singletonList(localCryptoConfig.getCertificate()));

            cmsSignGenerator = new CMSSignedDataGenerator();
            ContentSigner sha512Signer = new JcaContentSignerBuilder("SHA256WithRSA").setProvider(bc).build(localCryptoConfig.getPrivateKey());

            cmsSignGenerator.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(
                    new JcaDigestCalculatorProviderBuilder().setProvider(bc).build()).build(sha512Signer, new X509CertificateHolder(localCryptoConfig.getCertificate().getEncoded())
            ));
            cmsSignGenerator.addCertificates(certStore);
        } catch (CertificateEncodingException | OperatorCreationException | CMSException | IOException e) {
            throw new IllegalStateException("Something gone wrong while initializing CertStore using provided private and public key", e);
        }
    }

    public byte[] sign(InputStream is) throws IOException {
        try {
            CMSTypedDataInputStream msg = new CMSTypedDataInputStream(is);
            CMSSignedData signedData = cmsSignGenerator.generate(msg, false);
            return signedData.getEncoded();
        } catch (CMSException e) {
            throw new IllegalArgumentException("Something gone wrong while performing pkcs7 hash sign", e);
        }
    }

    @Override
    public boolean returnsFullPdf() {
        return false;
    }
}
