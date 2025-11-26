package it.pagopa.selfcare.product.controller.response;

import it.pagopa.selfcare.product.controller.base.ProductOrigins;
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
public class ProductOriginResponse extends ProductOrigins {
}
