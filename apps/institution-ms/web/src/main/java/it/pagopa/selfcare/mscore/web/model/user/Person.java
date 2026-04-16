package it.pagopa.selfcare.mscore.web.model.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.mscore.constant.Env;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

@Data
@Valid
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
public class Person {

    @NotEmpty(message = "User internal id is required")
    private String id;
    private String taxCode;
    private String name;
    private String surname;
    private String email;
    private PartyRole role;
    private String productRole;
    private Env env = Env.ROOT;
    private String roleLabel;

    public Person(String id) {
        this.id = id;
    }
}
