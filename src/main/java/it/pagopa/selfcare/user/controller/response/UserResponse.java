package it.pagopa.selfcare.user.controller.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.Map;

@Data
@Valid
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {

    @NotEmpty(message = "User internal id is required")
    private String id;
    private String taxCode;
    @NotBlank
    private String name;
    @NotBlank
    private String surname;
    private String email;
    private Map<String, String> workContacts;
}
