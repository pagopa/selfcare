package it.pagopa.selfcare.webhook.util;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class JwtData {
  private String username;
  private String password;
  private Map<String, Object> jwtHeader;
  private Map<String, String> jwtPayload;
}
