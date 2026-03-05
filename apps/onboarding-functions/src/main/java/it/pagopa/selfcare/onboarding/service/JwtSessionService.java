package it.pagopa.selfcare.onboarding.service;

public interface JwtSessionService {

    String createJwt(String userId);
}
