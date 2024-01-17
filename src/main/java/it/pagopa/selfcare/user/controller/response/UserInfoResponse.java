package it.pagopa.selfcare.user.controller.response;

import lombok.Data;

import java.util.List;

@Data
public class UserInfoResponse {

    private String id;
    private String userId;
    private List<UserInstitutionRoleResponse> institutions;

}
