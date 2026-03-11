package it.pagopa.selfcare.user.controller.response;

import it.pagopa.selfcare.user.constant.CertificationEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertifiableFieldResponse<T> {
    private T value;
    private CertificationEnum certified;
}
