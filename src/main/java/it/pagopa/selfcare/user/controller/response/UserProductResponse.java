package it.pagopa.selfcare.user.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProductResponse {

    private String id;
    private String taxCode;
    private String name;
    private String surname;
    private String email;
    private List<OnboardedProductResponse> products;

}