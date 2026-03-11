package it.pagopa.selfcare.user.service.utils;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateOrUpdateUserByFiscalCodeResponse {

    private String userId;
    private OPERATION_TYPE operationType;
}
