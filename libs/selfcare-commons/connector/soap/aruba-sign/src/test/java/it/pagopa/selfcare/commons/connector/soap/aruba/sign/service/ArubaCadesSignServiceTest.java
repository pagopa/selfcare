package it.pagopa.selfcare.commons.connector.soap.aruba.sign.service;

import it.pagopa.selfcare.commons.connector.soap.aruba.sign.config.ArubaSignConfig;
import it.pagopa.selfcare.commons.connector.soap.utils.SoapLoggingHandler;
import it.pagopa.selfcare.commons.utils.crypto.service.CadesSignServiceTest;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.operator.OperatorCreationException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.cert.CertificateException;

@SpringBootTest
@Import({ArubaSignConfig.class, ArubaSignServiceImpl.class, SoapLoggingHandler.class, ArubaPkcs7HashSignServiceImpl.class})
@AutoConfigureWireMock(port = 0, stubs = "classpath:wiremock")
@TestPropertySource(properties = {
        //region wiremock
        "logging.level.WireMock=OFF",
        "logging.level.it.pagopa.selfcare.commons.connector.soap.aruba.sign=DEBUG",
        "aruba.sign-service.baseUrl=http://localhost:${wiremock.server.port}",
        //endregion
})
class ArubaCadesSignServiceTest extends CadesSignServiceTest {

    @Test
    @Override
    protected void testCadesSign() throws CertificateException, IOException, OperatorCreationException, CMSException {
        // mocked data will not be aligned with timestamp alway updated, thus base test could not successfully sign
        verifySignerInformation=false;
        inputFilePath=Path.of("../../../utils/crypto", inputFilePath.toString());
        super.testCadesSign();
    }

    @Override
    protected File getOutputPadesFile() {
        return Path.of("target/tmp/signedSignTest-mock.pdf.p7m").toFile();
    }

}
