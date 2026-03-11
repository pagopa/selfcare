package it.pagopa.selfcare.user.controller.response;

import it.pagopa.selfcare.user.controller.response.product.OnboardedProductWithActions;
import lombok.Data;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserInstitutionWithActions {

    private String userId;
    private String institutionId;
    private String institutionDescription;
    private String institutionRootName;
    private List<OnboardedProductWithActions> products = new ArrayList<>();
    private String userMailUuid;

}
