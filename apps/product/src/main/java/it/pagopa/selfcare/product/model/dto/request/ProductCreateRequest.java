package it.pagopa.selfcare.product.model.dto.request;

import it.pagopa.selfcare.product.model.dto.base.ProductBaseFields;
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
public class ProductCreateRequest extends ProductBaseFields {
}