package it.pagopa.selfcare.dashboard.model.product;

import it.pagopa.selfcare.dashboard.model.ProductState;
import lombok.Data;

@Data
public class Product {

    private String id;
    private ProductState state;

}
