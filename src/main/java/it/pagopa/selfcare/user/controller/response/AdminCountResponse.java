package it.pagopa.selfcare.user.controller.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminCountResponse {

    private String institutionId;
    private String productId;
    private Long adminCount;

}
