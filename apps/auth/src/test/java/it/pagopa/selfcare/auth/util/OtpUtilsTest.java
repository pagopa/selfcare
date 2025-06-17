package it.pagopa.selfcare.auth.util;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class OtpUtilsTest {

    @Test
    public void maskEmailCorrectly(){
        String email = "pippo.pluto@test.com";
        Assertions.assertEquals("p***o.p***o@test.com", OtpUtils.maskEmail(email));
    }

    @Test
    public void generateRandomNumericOtpEveryTime(){
        String firstOtp = OtpUtils.generateOTP();
        String secondOtp = OtpUtils.generateOTP();
        Assertions.assertNotEquals(firstOtp, secondOtp);
        Assertions.assertTrue(firstOtp.matches("\\d{6}"));
        Assertions.assertTrue(secondOtp.matches("\\d{6}"));
    }
}
