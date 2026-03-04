package it.pagopa.selfcare.product.config;

import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.security.SecuritySchemes;

@SecuritySchemes(
    value = {
      @SecurityScheme(
          securitySchemeName = "bearerAuth",
          type = SecuritySchemeType.HTTP,
          scheme = "bearer",
          bearerFormat = "JWT")
    })
public class OpenApiSecurityConfig {}
