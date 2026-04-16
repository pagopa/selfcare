package it.pagopa.selfcare.product.model.dto.base;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
public class ProductBase extends ProductBaseFields {
  private String id;
  private String createdBy;
  private Integer version;
}
