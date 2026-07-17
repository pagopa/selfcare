package it.pagopa.selfcare.user.controller.response;

import lombok.Data;

@Data
public abstract class BaseUserInstitutionResponse {

    private String id;
    private String userId;
    private String institutionId;
    private String institutionDescription;
    private String institutionRootName;
    private String userMailUuid;

}
