package it.pagopa.selfcare.product.model.dto.response;

import it.pagopa.selfcare.product.model.ProductMetadata;
import it.pagopa.selfcare.product.model.dto.base.ProductBase;
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
public class ProductResponse extends ProductBase {
    private ProductMetadata metadata;
}
