package it.pagopa.selfcare.party.registry_proxy.connector.model.institution;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldNameConstants(asEnum = true)
public class DataProtectionOfficer {

    private String address;
    private String email;
    private String pec;

}
