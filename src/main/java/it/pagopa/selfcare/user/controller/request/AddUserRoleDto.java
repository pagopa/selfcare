package it.pagopa.selfcare.user.controller.request;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

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
        private PartyRole role;

        private String tokenId;
        private List<String> productRoles;
    }
}
