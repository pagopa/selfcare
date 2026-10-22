package it.pagopa.selfcare.user.controller.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Data
public class UpdateProductUsersDto {

    @NotBlank(message = "institutionDescription is required")
    private String institutionDescription;

    private String institutionRootName;

    @Schema(description = "Onboarding id to set as tokenId on the created roles")
    private String tokenId;

    @NotEmpty(message = "users is required")
    @Valid
    private List<ProductUser> users;

    @Data
    public static class ProductUser {

        @NotBlank(message = "userId is required")
        private String userId;

        @NotBlank(message = "role is required")
        @Schema(description = "Available values: MANAGER, DELEGATE, ADMIN_EA")
        private String role;

        @NotEmpty(message = "productRoles is required")
        private List<String> productRoles;

        private String userMailUuid;
    }
}
