package it.pagopa.selfcare.mscore.connector.dao.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DelegationInstitutionEntity extends DelegationEntity {

    private InstitutionEntity institution;

}
