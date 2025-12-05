package it.pagopa.selfcare.product.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackOfficeEnvironmentConfiguration {
    private String env;
    private String urlPublic;
    private String urlBO;
    private String identityTokenAudience;
}