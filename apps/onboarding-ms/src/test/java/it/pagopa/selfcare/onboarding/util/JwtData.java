package it.pagopa.selfcare.onboarding.util;

import java.util.Map;
import lombok.*;

@Data
@Builder
public class JwtData {
  private String username;
  private String password;
  private Map<String, Object> jwtHeader;
  private Map<String, String> jwtPayload;
}
