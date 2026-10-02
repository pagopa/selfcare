package it.pagopa.selfcare.onboarding.core;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.connector.model.product.Product;

import java.util.List;

public interface ProductAzureService {

    Product getProduct(String id, InstitutionType institutionType);

    Product getProductValid(String id);

    List<Product> getProducts(boolean rootOnly);

}
