package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.client.model.OriginResult;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.product.entity.Product;

import java.util.List;

public interface ProductService {

    OriginResult getOrigins(String productId);

    Product getProduct(String id, InstitutionType institutionType);

    Product getProductValid(String id);

    List<Product> getProducts(boolean rootOnly);

}
