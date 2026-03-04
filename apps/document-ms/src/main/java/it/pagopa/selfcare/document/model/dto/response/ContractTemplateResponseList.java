package it.pagopa.selfcare.product.model.dto.response;

import java.util.List;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContractTemplateResponseList {

  private List<ContractTemplateResponse> items;
}
