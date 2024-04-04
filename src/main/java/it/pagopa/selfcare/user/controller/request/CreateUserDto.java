package it.pagopa.selfcare.user.controller.request;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateUserDto {

    @NotEmpty(message = "institutionId is required")
    private String institutionId;

    @NotNull(message = "user is required")
    private User user;

    @NotNull(message = "product is required")
    private Product product;

    private String institutionDescription;
    private String institutionRootName;
    private Boolean hasToSendEmail = Boolean.TRUE;

    @Data
    public static class User {
        private String birthDate;

        private String familyName;
        private String name;

        @NotEmpty(message = "fiscalCode is required")
        private String fiscalCode;

        @NotEmpty(message = "email is required")
        private String institutionEmail;
    }

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
