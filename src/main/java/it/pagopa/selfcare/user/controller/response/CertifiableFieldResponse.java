package it.pagopa.selfcare.user.controller.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertifiableFieldResponse<T> {
    private T value;
    private boolean certified;
    public CertifiableFieldResponse(T value, CertifiableFieldResourceOfstring.CertificationEnum certificationEnum){
        this.value = value;
        this.certified = certificationEnum != null && !certificationEnum.NONE.equals(certificationEnum);
    }
}
