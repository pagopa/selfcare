package it.pagopa.selfcare.user.controller.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

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
        @Schema(description = "Available values: MANAGER, DELEGATE, SUB_DELEGATE, OPERATOR, ADMIN_EA, ADMIN_EA_IO")
        private String role;

        private String tokenId;

        @NotNull(message = "productRoles is required")
        private List<String> productRoles;
    }
}
