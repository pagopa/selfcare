package it.pagopa.selfcare.commons.connector.soap.aruba.sign.service;

import org.junit.jupiter.api.Assertions;
import org.springframework.test.context.TestPropertySource;

/* Download /secrets/arubaSignServiceSecrets.properties from the following link: https://pagopa.atlassian.net/wiki/spaces/SCP/pages/616857618/Firma+digitale+per+mezzo+dei+servizi+di+Aruba */
@TestPropertySource(locations = {
        "classpath:/secrets/arubaSignServiceSecrets.properties",
},
        properties = {
                "aruba.sign-service.baseUrl=https://arss.demo.firma-automatica.it:443/ArubaSignService/ArubaSignService"
        }
)
@SuppressWarnings({"squid:S3577", "NewClassNamingConvention"}) // The name is intended to be different from maven's default because it should be manually executed once obtained secrets file
class ArubaSignServiceTestIntegrated extends ArubaSignServiceTest {

        @Override
        protected void checkPkcs7HashSign(String result) {
                String expectedStart = "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";

                Assertions.assertTrue(
                        result.startsWith(
                                expectedStart
                        ),
                        () -> String.format("The hash retrieved don't start as expected:%s\n%s", result, expectedStart)
                );
        }
}
