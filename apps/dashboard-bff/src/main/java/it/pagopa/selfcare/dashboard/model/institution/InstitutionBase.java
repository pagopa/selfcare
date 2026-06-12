package it.pagopa.selfcare.dashboard.model.institution;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class InstitutionBase {

    private String id;
    private String name;
    private String userRole;
    private String status;
    private String parentDescription;

}
