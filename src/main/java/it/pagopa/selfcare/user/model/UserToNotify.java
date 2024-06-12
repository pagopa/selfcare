package it.pagopa.selfcare.user.model;

import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserToNotify {

    private String userId;
    private String name;
    private String familyName;
    private String email;
    @Schema(description = "Available values: MANAGER, DELEGATE, SUB_DELEGATE, OPERATOR, ADMIN_EA")
    private String role;
    private String productRole;
    private OnboardedProductState relationshipStatus;

}
