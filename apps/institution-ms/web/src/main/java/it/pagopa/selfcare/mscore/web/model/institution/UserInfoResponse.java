package it.pagopa.selfcare.mscore.web.model.institution;

import it.pagopa.selfcare.mscore.model.onboarding.OnboardedProduct;
import lombok.Data;

import java.util.List;

@Data
public class UserInfoResponse {

    private String id;
    private String taxCode;
    private String name;
    private String surname;
    private String email;
    private List<OnboardedProduct> products;

}
