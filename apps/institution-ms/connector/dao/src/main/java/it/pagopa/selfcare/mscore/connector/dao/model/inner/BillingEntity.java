package it.pagopa.selfcare.mscore.connector.dao.model.inner;

import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants(asEnum = true)
public class BillingEntity {

    private String vatNumber;
    private String taxCodeInvoicing;
    private String recipientCode;
    private boolean publicServices;

}
