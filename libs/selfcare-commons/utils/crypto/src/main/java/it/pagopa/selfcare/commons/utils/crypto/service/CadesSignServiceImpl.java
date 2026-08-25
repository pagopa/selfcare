package it.pagopa.selfcare.commons.utils.crypto.service;

import it.pagopa.selfcare.commons.utils.crypto.utils.CMSTypedDataInputStream;
import it.pagopa.selfcare.commons.utils.crypto.utils.CryptoUtils;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Collection;

@Service
public class CadesSignServiceImpl implements CadesSignService {

    private final Pkcs7HashSignService pkcs7Signature;

    public CadesSignServiceImpl(Pkcs7HashSignService pkcs7Signature) {
        this.pkcs7Signature = pkcs7Signature;
    }

    public void cadesSign(File file, File signedFile) {
        CryptoUtils.createParentDirectoryIfNotExists(signedFile);

        try (
                FileInputStream fis = new FileInputStream(file);
                FileInputStream fis2 = new FileInputStream(file);
                FileOutputStream fos = new FileOutputStream(signedFile)
        ) {
            final byte[] signedHash = pkcs7Signature.sign(fis);

            CMSSignedDataGenerator gen = buildSignGenerator(signedHash);

            CMSTypedDataInputStream msg = new CMSTypedDataInputStream(fis2);
            fos.write(gen.generate(msg, true).getEncoded());
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Something gone wrong while signing input file %s and storing it into %s", file.getAbsolutePath(), signedFile.getAbsolutePath()), e);
        }
    }

    private CMSSignedDataGenerator buildSignGenerator(byte[] signedHash) throws CMSException, OperatorCreationException {
        CMSSignedData signedData = new CMSSignedData(signedHash);

        Store<?> certificatesStore = signedData.getCertificates();

        SignerInformation signerInformation = extractSignerInformation(signedData);
        X509CertificateHolder certificateHolder = extractCertificateHolder(certificatesStore, signerInformation);

        ContentSigner nonSigner = new ContentSigner() {

            @Override
            public byte[] getSignature() {
                return signerInformation.getSignature();
            }

            @Override
            public OutputStream getOutputStream() {
                return new ByteArrayOutputStream();
            }

            @Override
            public AlgorithmIdentifier getAlgorithmIdentifier() {
                return new DefaultSignatureAlgorithmIdentifierFinder().find( "SHA256WithRSA" );
            }
        };

        JcaSignerInfoGeneratorBuilder signerInfoBuilder = extractSignedAttributes(signerInformation);

        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
        gen.addSignerInfoGenerator(signerInfoBuilder.build(nonSigner, certificateHolder));
        gen.addCertificates(certificatesStore);
        return gen;
    }

    private SignerInformation extractSignerInformation(CMSSignedData signedData) {
        Collection<SignerInformation> signers = signedData.getSignerInfos().getSigners();
        return signers.iterator().next();
    }

    private X509CertificateHolder extractCertificateHolder(Store<?> certificatesStore, SignerInformation signerInformation) {
        Collection<?> matches = certificatesStore.getMatches(signerInformation.getSID());
        return (X509CertificateHolder) matches.iterator().next();
    }

    private JcaSignerInfoGeneratorBuilder extractSignedAttributes(SignerInformation signerInformation) throws OperatorCreationException {
        AttributeTable signedAttributes = signerInformation.getSignedAttributes();
        JcaSignerInfoGeneratorBuilder signerInfoBuilder = new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().build());
        signerInfoBuilder.setSignedAttributeGenerator(new SimpleAttributeTableGenerator(signedAttributes));
        return signerInfoBuilder;
    }

}
