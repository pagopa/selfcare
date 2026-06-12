package it.pagopa.selfcare.external_api.model.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.commons.base.security.PartyRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;

@Data
@Valid
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
public class Person {

    @Schema(description = "User ID of a user already in the registry. A role will be added to the institution/product for the user. It takes precedence over taxCode")
    private String id;

    @Schema(description = "The tax code of the user to add / create. If id is present, the field is ignored")
    private String taxCode;

    @Schema(description = "The name of the user to add / create. If id is present, the field is ignored")
    private String name;

    @Schema(description = "The surname of the user to add / create. If id is present, the field is ignored")
    private String surname;

    @Schema(description = "The email address of the user to add / create. If id is present, the field is ignored")
    private String email;

    @Schema(description = "The email ID of the user already in the registry to be assigned to the institution/product. This field is only used when adding a user by ID")
    private String userMailUuid;

    @Schema(description = "The role to assign to the user")
    private PartyRole role;

    @Schema(description = "The product role to assign to the user based on the product specifications")
    private String productRole;

    @Schema(description = "Environment. The field is currently not used to create users. It's only returned in response")
    private Env env = Env.ROOT;

    @Schema(description = "The product role label based on the product specifications. The field is currently not used to create users")
    private String roleLabel;

    public Person(String id) {
        this.id = id;
    }
}
