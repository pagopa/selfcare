package it.pagopa.selfcare.product.controller.response;

import it.pagopa.selfcare.product.model.enums.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductBaseResponse {
    private String id;
    private String productId;
    private ProductStatus status;
}
