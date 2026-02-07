package it.pagopa.selfcare.product.model.dto.response;

import it.pagopa.selfcare.product.model.OriginEntry;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@ToString(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
public class ProductOriginResponse {
  private List<OriginEntry> origins;
}
