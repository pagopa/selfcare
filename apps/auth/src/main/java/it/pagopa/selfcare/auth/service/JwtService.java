package it.pagopa.selfcare.auth.service;

import io.smallrye.mutiny.Uni;
import java.util.Map;

public interface JwtService {
  Uni<Map<String, String>> extractClaimsFromJwtToken(String jwt);
}
