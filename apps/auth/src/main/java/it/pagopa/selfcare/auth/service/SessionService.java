package it.pagopa.selfcare.auth.service;

import io.smallrye.mutiny.Uni;

public interface SessionService {
    Uni<String> generateSessionToken(String fiscalNumber, String name, String familyName);
}
