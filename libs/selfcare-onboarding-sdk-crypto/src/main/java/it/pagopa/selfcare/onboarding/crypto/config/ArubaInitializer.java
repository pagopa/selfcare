package it.pagopa.selfcare.onboarding.crypto.config;

import it.pagopa.selfcare.onboarding.crypto.soap.aruba.sign.generated.client.Auth;

import java.util.Optional;

public class ArubaInitializer {

    private ArubaInitializer() {}

    public static ArubaSignConfig initializeConfig() {

        ArubaSignConfig config = new ArubaSignConfig();
        config.setConnectTimeoutMs(Optional.ofNullable(System.getenv("ARUBA_SIGN_SERVICE_CONNECT_TIMEOUT_MS"))
                .map(Integer::parseInt).orElse(0));
        config.setRequestTimeoutMs(Optional.ofNullable(System.getenv("ARUBA_SIGN_SERVICE_REQUEST_TIMEOUT_MS"))
                .map(Integer::parseInt).orElse(0));
        config.setBaseUrl(Optional.ofNullable(System.getenv("ARUBA_SIGN_SERVICE_BASE_URL"))
                .orElse("https://arss.demo.firma-automatica.it:443/ArubaSignService/ArubaSignService"));

        Auth auth = new Auth();
        auth.setTypeOtpAuth(System.getenv("ARUBA_SIGN_SERVICE_IDENTITY_TYPE_OTP_AUTH"));
        auth.setOtpPwd(System.getenv("ARUBA_SIGN_SERVICE_IDENTITY_OTP_PWD"));
        auth.setUser(System.getenv("ARUBA_SIGN_SERVICE_IDENTITY_USER"));
        auth.setDelegatedUser(System.getenv("ARUBA_SIGN_SERVICE_IDENTITY_DELEGATED_USER"));
        auth.setDelegatedPassword(System.getenv("ARUBA_SIGN_SERVICE_IDENTITY_DELEGATED_PASSWORD"));
        auth.setDelegatedDomain(System.getenv("ARUBA_SIGN_SERVICE_IDENTITY_DELEGATED_DOMAIN"));
        auth.setTypeHSM("COSIGN");
        config.setAuth(auth);

        return config;
    }
}
