package it.pagopa.selfcare.party.registry_proxy.connector.model.institution;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(asEnum = true)
public class PaymentServiceProvider {

    private String abiCode;
    private String businessRegisterNumber;
    private String legalRegisterName;
    private String legalRegisterNumber;
    private boolean vatNumberGroup;

}
