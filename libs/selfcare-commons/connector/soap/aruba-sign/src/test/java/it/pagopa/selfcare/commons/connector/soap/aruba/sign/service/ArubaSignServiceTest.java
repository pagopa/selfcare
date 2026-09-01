package it.pagopa.selfcare.commons.connector.soap.aruba.sign.service;

import it.pagopa.selfcare.commons.connector.soap.aruba.sign.config.ArubaSignConfig;
import it.pagopa.selfcare.commons.connector.soap.utils.SoapLoggingHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

@SpringBootTest(classes = {ArubaSignConfig.class, ArubaSignServiceImpl.class, SoapLoggingHandler.class, ArubaPkcs7HashSignServiceImpl.class})
@AutoConfigureWireMock(port = 0, stubs = "classpath:wiremock")
@TestPropertySource(properties = {
        //region wiremock
        "logging.level.WireMock=OFF",
        "logging.level.it.pagopa.selfcare.commons.connector.soap.aruba.sign=DEBUG",
        "aruba.sign-service.baseUrl=http://localhost:${wiremock.server.port}",
        //endregion
})
class ArubaSignServiceTest {

    @Autowired
    private ArubaSignService service;

    @Test
    void testHashSign() throws IOException {
        try (InputStream is = new ClassPathResource("signTest.pdf").getInputStream()) {
            byte[] result = service.hashSign(is);
            Assertions.assertNotNull(result);
            checkHashSign(Base64.getEncoder().encodeToString(result));
        }
    }

    protected void checkHashSign(String result) {
        Assertions.assertEquals(
                "ivxrc3sxInay9IRpIvR32rJDg6VeVo03xiw/tXHrgR7cXCSIpWPM6V35HVpIdtE5Uc5NljWoL8vbxDhNK4zd4ULEaf7FjTVINAMLcRy0LQAURQqcg2frKLNLdesWwq0q2smxa4vZa87f7XuySSMoyTDvTL/zuPZBo9v8C+COc1oON1NSF7XQhIOc0Cz6i5U+AET2yegqJRvW/RDYJCtLAi2RN3ZKwLy7XqeEbP7LKVYD+EyuMkQPt473uXptEAtVDHUyznBboN6EKDItM3x9Ga/0RUEckx4nc1tQQGY5FwEXtUelSgu/9QoSBD4pfnXkCDEZpzCHHiI+QKbwbHhJUQ==",
                result
        );
    }

    @Test
    void testPkcs7HashSign() throws IOException {
        try (InputStream is = new ClassPathResource("signTest.pdf").getInputStream()) {
            byte[] result = service.pkcs7Signhash(is);
            Assertions.assertNotNull(result);
            checkPkcs7HashSign(Base64.getEncoder().encodeToString(result));
        }
    }

    protected void checkPkcs7HashSign(String result) {
        Assertions.assertEquals(
                "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",
                result
        );
    }

}
