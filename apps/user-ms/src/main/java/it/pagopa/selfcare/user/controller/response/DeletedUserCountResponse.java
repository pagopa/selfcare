package it.pagopa.selfcare.user.controller.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeletedUserCountResponse {

    private String institutionId;
    private String productId;
    private Long deletedUserCount;

}
