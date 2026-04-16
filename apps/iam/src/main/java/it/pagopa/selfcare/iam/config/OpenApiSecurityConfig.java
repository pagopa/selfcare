package it.pagopa.selfcare.iam.config;

import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.security.SecuritySchemes;

@SecuritySchemes({
  @SecurityScheme(
      securitySchemeName = "bearerAuth",
      type = SecuritySchemeType.HTTP,
      scheme = "bearer",
      bearerFormat = "JWT")
})
public class OpenApiSecurityConfig {}
