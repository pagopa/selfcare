package it.pagopa.selfcare.user.controller.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsersCountResponse {

    private String institutionId;
    private String productId;
    private List<String> roles;
    private List<String> status;
    private Long count;

}
