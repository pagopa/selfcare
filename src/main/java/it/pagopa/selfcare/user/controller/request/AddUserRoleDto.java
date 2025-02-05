package it.pagopa.selfcare.user.controller.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Data
public class AddUserRoleDto {

    @NotEmpty(message = "institutionId is required")
    private String institutionId;

    @NotNull(message = "product is required")
    private Product product;

    private String institutionDescription;
    private String institutionRootName;
    private String userMailUuid;
    private boolean hasToSendEmail;

    @Data
    public static class Product {

        @NotEmpty(message = "productId is required")
        private String productId;

        @NotNull(message = "role is required")
        @Schema(description = "Available values: MANAGER, DELEGATE, SUB_DELEGATE, OPERATOR, ADMIN_EA, ADMIN_EA_IO")
        private String role;

        private String tokenId;

        @NotNull(message = "productRoles is required")
        private List<String> productRoles;

        private String delegationId;
    }
}
