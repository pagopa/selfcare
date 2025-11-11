package it.pagopa.selfcare.product.controller.response;

import it.pagopa.selfcare.product.model.enums.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductBaseResponse {
    String id;
    String productId;
    ProductStatus productStatus;
}
