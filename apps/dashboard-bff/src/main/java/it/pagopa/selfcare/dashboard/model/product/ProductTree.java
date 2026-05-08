package it.pagopa.selfcare.dashboard.model.product;

import it.pagopa.selfcare.product.entity.Product;
import lombok.Data;

import java.util.List;

@Data
public class ProductTree {
    private Product node;
    private List<Product> children;
}
