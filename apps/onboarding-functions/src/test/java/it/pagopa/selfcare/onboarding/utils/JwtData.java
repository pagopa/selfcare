package it.pagopa.selfcare.onboarding.utils;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JwtData {
  private String username;
  private String password;
  private Map<String, Object> jwtHeader;
  private Map<String, String> jwtPayload;

}
