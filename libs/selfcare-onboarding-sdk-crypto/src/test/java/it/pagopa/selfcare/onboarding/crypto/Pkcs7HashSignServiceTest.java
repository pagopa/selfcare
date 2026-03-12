package it.pagopa.selfcare.onboarding.crypto;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Base64;

class Pkcs7HashSignServiceTest {

    private static final String TEST_CERT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIDnzCCAoegAwIBAgIUJ8/0z+sR6Llr9FcIGoc5nvZQydgwDQYJKoZIhvcNAQEL\n" +
            "BQAwXzELMAkGA1UEBhMCSVQxDTALBgNVBAgMBFJPTUUxDTALBgNVBAcMBFJPTUUx\n" +
            "DjAMBgNVBAoMBUlEUEFZMQ4wDAYDVQQLDAVJRFBBWTESMBAGA1UEAwwJbG9jYWxo\n" +
            "b3N0MB4XDTIyMTEwOTE1MTI0NFoXDTMyMDkxNzE1MTI0NFowXzELMAkGA1UEBhMC\n" +
            "SVQxDTALBgNVBAgMBFJPTUUxDTALBgNVBAcMBFJPTUUxDjAMBgNVBAoMBUlEUEFZ\n" +
            "MQ4wDAYDVQQLDAVJRFBBWTESMBAGA1UEAwwJbG9jYWxob3N0MIIBIjANBgkqhkiG\n" +
            "9w0BAQEFAAOCAQ8AMIIBCgKCAQEArDOJKswwCaKdYJbaHZz3bgEIl7z1ArZpNI54\n" +
            "ZGaXcRitiwjr/W9fenW69mG7IAlITuPtaIu4iggXTcSRuaulres2EvuP7KjL0tfo\n" +
            "x/PstqaMZzLF8wOYfJE4iJ8ffcQL67LJ3/Wwn2FhYVV+4D2AYW8QPdRm406HJG7b\n" +
            "NKLmdM9AFUQp6zoTvNegyWQyAfH40i72UopltDubcAykD6YgkRctCtKd8h/BRpIR\n" +
            "tMn0AGLM/o5qwYu+eCAy8/7Ppj3HzCwHkDOJad/g2pRj4soJdvn5rP6TM4OVtZ7V\n" +
            "ehxionkaccBPcyDGSrIo5837XYaGv3r7Rn0rCplfxnU4Gtmd5wIDAQABo1MwUTAd\n" +
            "BgNVHQ4EFgQUPYfJeHRHwSLmcueB8jUQSHUReVIwHwYDVR0jBBgwFoAUPYfJeHRH\n" +
            "wSLmcueB8jUQSHUReVIwDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOC\n" +
            "AQEAK34LEHSVM44Wwbs9nKDKeQTRGosdd+gQSrqGf3nI0vkhckuaoYPnuFKi+eo2\n" +
            "r+J6xXgqhQfrvhXnYxNEJr9U+9ELBc3IjG6bTUS6HyWhu2PJCeckxQJqonVntl99\n" +
            "jmEr4G7QJeDc9oJmC0NJqBmQS/D0tMxChNWpYe1AoGXwqc4S6NTd3x2Z8THzv8du\n" +
            "MMn7+1f/VOWe7/Iuuvx5DHN2JFi0lvhMqwglIweGn/qLGB0+r9GM+QlfGuZvUey2\n" +
            "x3C0DLQnNIkNKktGjaNjCmpZcd9SIVi6TOPpR+AxlIddYvUXu4GYVXyfDPgzPeha\n" +
            "JDiI4WMkIMmYSzhMc/lfuDMGow==\n" +
            "-----END CERTIFICATE-----";

    private static final String TEST_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----\n" +
            "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCsM4kqzDAJop1g\n" +
            "ltodnPduAQiXvPUCtmk0jnhkZpdxGK2LCOv9b196dbr2YbsgCUhO4+1oi7iKCBdN\n" +
            "xJG5q6Wt6zYS+4/sqMvS1+jH8+y2poxnMsXzA5h8kTiInx99xAvrssnf9bCfYWFh\n" +
            "VX7gPYBhbxA91GbjTockbts0ouZ0z0AVRCnrOhO816DJZDIB8fjSLvZSimW0O5tw\n" +
            "DKQPpiCRFy0K0p3yH8FGkhG0yfQAYsz+jmrBi754IDLz/s+mPcfMLAeQM4lp3+Da\n" +
            "lGPiygl2+fms/pMzg5W1ntV6HGKieRpxwE9zIMZKsijnzftdhoa/evtGfSsKmV/G\n" +
            "dTga2Z3nAgMBAAECggEAEC6FmMJ4Tyd7T3zNgVPjQnCRbKTihz858qjislibqZKO\n" +
            "mE6d0oJ5P+o5R/bWHUQSCevMPvNGQ55QBkxO/1ocZxP/0FfYZf5UrPsCEmwfFejf\n" +
            "r8DrLhNr7GS/IcOGM4zNK/hwlP2i+88sVfexRQQygLVtmsnPY1PZSjiqm68lJdu+\n" +
            "aP8TYM10y1aeiYnfuUYvnvXJFXeTEockhaUJTmeIQNbbUy+pyJ0mAPASPtXRLr8h\n" +
            "UflutICnWcx4v/qkCn1jmHw+NMA4q7hOH7UuOAqj53FqGMN+IWfjMmmYoQ7MVURx\n" +
            "8CrnEtlCOua+C8EEIFL2ylvV7X0cv/DqCJLVQoegsQKBgQDLzMaAjNgD8xSXp+Gj\n" +
            "beeUsSGptEaGMuA89AzyTnCyvU9a1HGwDAghoQPae+pVk7R5uokojWkBVzP/kKxv\n" +
            "ZldGwPOegUUdBLS4yJML+OkqtoCgf3Mbcozm5dVYtx7bYdhh3PswzRmn/h/YjEAz\n" +
            "+/mxi6dJir0k0Nd4YNtQbzBctwKBgQDYTtSmJvVQdOHnzqA/LRmMF1l+HaqLuDfu\n" +
            "B4rDlxCdDfOAvHqz+3YapP3B4MQuz29TSDqwAnzoN2XZX5B6g/jKauWpAwZkFXuO\n" +
            "fqcfNG/+MewTcHIYNm+EtgXtIsnCXDfAeXdQapzNsOX+XSF/aWcgGHg18xOBPt0R\n" +
            "7Aoa/h34UQKBgQCsCzGjwcJ2CxXeNPYxfg1ao/HUDoDet0I/kpL/VqKi8Vd1SRS0\n" +
            "VmPi58eWALfBCJD5ljRFjKMRY6lc3KgE3vNconTG4UAUEC30NDaWi8liqnCJjS4C\n" +
            "BMDYBzwEyYn+D2qYqvFOsEYxYEFIEJX+jH+sl0VguwOTec38LF/YVhUQnwKBgG5u\n" +
            "2Kw3SZkZA1ioqjF24gsexKbZmH+avps8qICw+F9mhwIbt/15jVOPFqrMCPzpFKoN\n" +
            "P0ErFAAugEYZPxb9l6AoMTY3gCTKvvkB+mq5B9BcRm2qQ+XOrOKxV5c44o7jK+eN\n" +
            "W/fnZkSxYsqZW4fEFU1SkNTiU/vxT0ZeHs6nHD/xAoGAOIqaqQnJfGj/wLo3Z9o5\n" +
            "/Oxu1zTPGZC6SqpdygCjlQ0kQ8Bp0LV7nL06/VCHAHI2lF12xApRnFk7GY3xyqK8\n" +
            "nYxeRASCj3GGmLupGshtfCtDBeysE2h7kj3Bo0d6g1Ye+j8BUZuZaZm6WNlo7cgE\n" +
            "NLHn1k0IpmXFOiFa1Y1D6Bc=\n" +
            "-----END PRIVATE KEY-----";

    private Pkcs7HashSignService service;

    private static EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @BeforeAll
    static void setup() throws Exception {
        environmentVariables.set("CRYPTO_CERT", TEST_CERT);
        environmentVariables.set("CRYPTO_PRIVATE_KEY", TEST_PRIVATE_KEY);
        environmentVariables.setup();
    }

    @AfterAll
    static void afterAll() throws Exception {
        environmentVariables.teardown();
    }
    @Test
    void shouldAssertEncoding() throws IOException {

        service = new Pkcs7HashSignServiceImpl();
        try (FileInputStream fis = new FileInputStream(Path.of("src/test/resources/signTest.pdf").toFile())) {
            byte[] result = service.sign(fis);
            Assertions.assertNotNull(result);
            checkPkcs7HashSign(Base64.getEncoder().encodeToString(result));
        }
    }

    protected void checkPkcs7HashSign(String result) {
        String expectedPrefix = "MIAGCSqGSIb3DQEHAqCAMIACAQExDTALBglghkgBZQMEAgEwCwYJKoZIhvcNAQcBoIAwggOfMIICh6ADAgECAhQnz";
        Assertions.assertTrue(
                result.startsWith(expectedPrefix),
                String.format("Prefix unexpected:\n%s\n%s", result, expectedPrefix)
        );
    }
}
