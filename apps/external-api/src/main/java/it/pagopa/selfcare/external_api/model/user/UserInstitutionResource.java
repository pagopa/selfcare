package it.pagopa.selfcare.external_api.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserInstitutionResource {

    @Schema(description = "${swagger.external_api.userInstitution.model.id}")
    private String id;

    @Schema(description = "${swagger.external_api.user.model.id}")
    @NotNull
    private String userId;
    @Schema(description = "${swagger.external_api.institutions.model.id}")
    private String institutionId;
    @Schema(description = "${swagger.external_api.user.model.onboardedInstitutions.description}")
    private String institutionDescription;
    @Schema(description = "${swagger.external_api.institutions.model.rootName}")
    private String institutionRootName;
    @Schema(description = "${swagger.external_api.userInfo.model.onboardedInstitutions}")
    private List<UserProductResource> products;

    @Data
    public static class UserProductResource {
        @Schema(description = "${swagger.external_api.api.tokens.productId}")
        private String productId;
        @Schema(description = "${swagger.external_api.api.tokens.id}")
        private String tokenId;
        @Schema(description = "${swagger.external_api.model.states}")
        private String status;
        @Schema(description = "${swagger.external_api.tokens.model.productRole}")
        private String productRole;
        @Schema(description = "${swagger.external_api.tokens.model.productRoleLabel}")
        private String productRoleLabel;
        @Schema(description = "Available values: MANAGER, DELEGATE, SUB_DELEGATE, OPERATOR, ADMIN_EA")
        private String role;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

    }

}
