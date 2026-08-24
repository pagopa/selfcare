package it.pagopa.selfcare.commons.connector.soap.aruba.sign.service;

import org.springframework.test.context.TestPropertySource;

import java.io.File;
import java.nio.file.Path;

/* Download /secrets/arubaSignServiceSecrets.properties from the following link: https://pagopa.atlassian.net/wiki/spaces/SCP/pages/616857618/Firma+digitale+per+mezzo+dei+servizi+di+Aruba */
@TestPropertySource(locations = {
        "classpath:/secrets/arubaSignServiceSecrets.properties",
},
        properties = {
                "aruba.sign-service.baseUrl=https://arss.demo.firma-automatica.it:443/ArubaSignService/ArubaSignService"
        }
)
@SuppressWarnings({"squid:S3577", "NewClassNamingConvention"}) // The name is intended to be different from maven's default because it should be manually executed once obtained secrets file
class ArubaCadesSignServiceTestIntegrated extends ArubaCadesSignServiceTest {

        @Override
        protected File getOutputPadesFile() {
                return Path.of("target/tmp/signedSignTest-Aruba.pdf.p7m").toFile();
        }

}
