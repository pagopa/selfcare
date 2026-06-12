package it.pagopa.selfcare.external_api.model.user;

import lombok.Data;

import jakarta.validation.Valid;
import java.util.List;

@Data
public class UserInstitution {

    private String id;
    private String institutionId;
    private String institutionDescription;
    private String institutionRootName;
    private String userId;
    private String userMailUuid;

    @Valid
    private List<OnboardedProductResponse> products = null;
}
