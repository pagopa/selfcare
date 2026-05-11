package it.pagopa.selfcare.external_api.model.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDetailsWrapper {
    private String userId;
    private String institutionId;
    private ProductDetails productDetails;
}
