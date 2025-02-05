package it.pagopa.selfcare.user.controller.response;

import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Data
public class UserInstitutionRoleResponse {

    private String institutionId;
    private String institutionName;
    private String institutionRootName;
    @Schema(description = "Available values: MANAGER, DELEGATE, SUB_DELEGATE, OPERATOR, ADMIN_EA, ADMIN_EA_IO")
    private String role;
    private OnboardedProductState status;
}
