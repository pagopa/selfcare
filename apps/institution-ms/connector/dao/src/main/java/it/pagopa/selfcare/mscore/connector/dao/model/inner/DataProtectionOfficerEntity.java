package it.pagopa.selfcare.mscore.connector.dao.model.inner;

import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants(asEnum = true)
public class DataProtectionOfficerEntity {
    private String address;
    private String email;
    private String pec;
}
