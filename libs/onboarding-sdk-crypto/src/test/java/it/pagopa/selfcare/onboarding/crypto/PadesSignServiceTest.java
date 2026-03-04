package it.pagopa.selfcare.onboarding.crypto;


import it.pagopa.selfcare.onboarding.crypto.entity.SignatureInformation;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class PadesSignServiceTest {

    private PadesSignService service;

    // mocked data will not be aligned with timestamp alway updated, thus base test could not successfully sign
    protected boolean verifySignerInformation = true;
    protected Path inputFilePath = Path.of("src/test/resources/signTest.pdf");

    private Pkcs7HashSignService pkcs7HashSignService;

    @BeforeEach
    void setup() {
        pkcs7HashSignService = mock(Pkcs7HashSignService.class);
        service = new PadesSignServiceImpl(pkcs7HashSignService);
    }

    @Test
    void testPadesSign() throws IOException {
        File inputFile = inputFilePath.toFile();
        File outputFile = getOutputPadesFile();
        if (outputFile.exists()) {
            Assertions.assertTrue(outputFile.delete());
        }

        when(pkcs7HashSignService.sign(any())).thenReturn(Files.readAllBytes(inputFilePath));

        service.padesSign(inputFile, outputFile, new SignatureInformation("PagoPA S.P.A", "Rome", "onboarding contract"));
        Assertions.assertTrue(outputFile.exists());

        checkPadesSignature(inputFile, outputFile);
    }

    protected File getOutputPadesFile() {
        return Path.of("target/tmp/signedSignTest-selfSigned.pdf").toFile();
    }

    @SuppressWarnings("unchecked")
    private void checkPadesSignature(File origFile, File signedFile)
            throws IOException
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
        }
        document.close();
    }

    @Test
    void testHandleException() throws IOException {
        File inputFile = inputFilePath.toFile();
        File outputFile = getOutputPadesFile();
        if (outputFile.exists()) {
            Assertions.assertTrue(outputFile.delete());
        }


        when(pkcs7HashSignService.sign(any())).thenThrow(new RuntimeException());
        assertThrows(IllegalStateException.class, () -> service.padesSign(inputFile, outputFile, new SignatureInformation("PagoPA S.P.A", "Rome", "onboarding contract")));
    }
}