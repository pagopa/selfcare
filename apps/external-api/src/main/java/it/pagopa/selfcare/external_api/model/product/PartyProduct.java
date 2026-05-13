package it.pagopa.selfcare.external_api.model.product;

import it.pagopa.selfcare.commons.base.security.PartyRole;
import lombok.Data;

@Data
public class PartyProduct {
    private ProductOnboardingStatus status;
    private PartyRole role;
    private String id;
}