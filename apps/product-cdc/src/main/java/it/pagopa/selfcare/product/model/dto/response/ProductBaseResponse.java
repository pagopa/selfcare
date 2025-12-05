package it.pagopa.selfcare.product.model.dto.response;

import it.pagopa.selfcare.product.model.enums.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductBaseResponse {
    private String id;
    private String productId;
    private ProductStatus status;
}
