package it.pagopa.selfcare.user.controller.response;

import lombok.Data;

import java.util.List;

@Data
public class UserDataResponse {

    private String id;
    private String userId;
    private String institutionId;
    private String institutionDescription;
    private String institutionRootName;
    private String userMailUuid;
    private String role;
    private String status;
    private List<OnboardedProductResponse> products;
    private UserResponse userResponse;

}
