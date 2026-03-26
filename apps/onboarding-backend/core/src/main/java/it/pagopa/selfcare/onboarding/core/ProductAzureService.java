package it.pagopa.selfcare.onboarding.core;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.product.entity.Product;

import java.util.List;

public interface ProductAzureService {

    Product getProduct(String id, InstitutionType institutionType);

    Product getProductValid(String id);

    List<Product> getProducts(boolean rootOnly);

}
