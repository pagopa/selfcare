package it.pagopa.selfcare.user.controller.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserDataWithProductInfoResponse extends BaseUserDataResponse {

    private List<OnboardedProductDataResponse> products;

}
