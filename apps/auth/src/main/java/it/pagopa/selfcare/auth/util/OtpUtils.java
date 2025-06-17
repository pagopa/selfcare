package it.pagopa.selfcare.auth.util;

import java.security.SecureRandom;

public class OtpUtils {

    private static final SecureRandom random = new SecureRandom();

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }

        String[] parts = email.split("@", 2);
        String username = parts[0];
        String domain = parts[1];

        String[] nameParts = username.split("\\.");

        StringBuilder maskedUsername = new StringBuilder();
        for (int i = 0; i < nameParts.length; i++) {
            maskedUsername.append(maskPart(nameParts[i]));
            if (i < nameParts.length - 1) {
                maskedUsername.append(".");
            }
        }

        return maskedUsername + "@" + domain;
    }

    private static String maskPart(String part) {
        if (part.length() <= 2) {
            return part.charAt(0) + "*";
        }

        return part.charAt(0) +
                "*".repeat(part.length() - 2) +
                part.charAt(part.length() - 1);

    }

    public static String generateOTP() {
        StringBuilder otp = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            int digit = random.nextInt(10); // da 0 a 9
            otp.append(digit);
        }
        return otp.toString();
    }
}
