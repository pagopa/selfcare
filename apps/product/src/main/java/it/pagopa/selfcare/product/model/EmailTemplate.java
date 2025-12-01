package it.pagopa.selfcare.product.model;

import it.pagopa.selfcare.product.model.enums.OnboardingStatus;
import it.pagopa.selfcare.product.model.enums.WorkflowType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailTemplate {
    private WorkflowType type;
    private String institutionType;
    private String path;
    private String version;
    private OnboardingStatus status;
}
