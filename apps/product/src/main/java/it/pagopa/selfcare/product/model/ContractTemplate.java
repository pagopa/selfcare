package it.pagopa.selfcare.product.model;

import it.pagopa.selfcare.product.model.enums.InstitutionType;
import it.pagopa.selfcare.product.model.enums.WorkflowType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractTemplate {
    private String type;
    private InstitutionType institutionType;
    private String path;
    private String version;
    private int order;
    private boolean generated;
    private boolean mandatory;
    private String name;
    private String workflowState;
    private List<WorkflowType> workflowType;
}
