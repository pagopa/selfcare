package it.pagopa.selfcare.user.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserToNotify {

    private String userId;
    private String name;
    private String familyName;
    private String email;
    @Schema(description = "Available values: MANAGER, DELEGATE, SUB_DELEGATE, OPERATOR, ADMIN_EA")
    private String role;
    private String productRole;
    private List<String> roles;
    private OnboardedProductState relationshipStatus;

}
