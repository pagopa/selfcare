package it.pagopa.selfcare.external_api.model.user;

import it.pagopa.selfcare.commons.base.security.PartyRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductDetails{
    private String productId;
    private PartyRole role;
    private List<String> roles;
    private OffsetDateTime createdAt;
}