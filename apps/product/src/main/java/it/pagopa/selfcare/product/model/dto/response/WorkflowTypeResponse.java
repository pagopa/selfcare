package it.pagopa.selfcare.product.model.dto.response;

import it.pagopa.selfcare.product.model.enums.WorkflowType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTypeResponse {
  private WorkflowType workflowType;
}

