package it.pagopa.selfcare.mscore.model.user;

import it.pagopa.selfcare.mscore.model.onboarding.OnboardedProduct;
import lombok.Data;

import java.util.List;

@Data
public class UserInfo {

    private String id;
    private User user;
    private List<OnboardedProduct> products;

}
