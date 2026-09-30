package it.pagopa.selfcare.product.model;

import it.pagopa.selfcare.product.model.enums.InstitutionType;
import it.pagopa.selfcare.product.model.enums.Origin;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequiredDocumentFilter {

  private List<InstitutionType> institutionType;
  private List<Origin> origin;
}
