package it.pagopa.selfcare.dashboard.model.product.mapper;

import it.pagopa.selfcare.dashboard.model.product.ProductTree;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
@Component
public class ProductMapper {

    public List<ProductTree> toTreeResource(List<Product> model) {
        List<ProductTree> resources = null;
        if (model != null) {
            Map<String, List<Product>> collect = model.stream()
                    .filter(productOperations -> productOperations.getParentId() != null)
                    .collect(Collectors.groupingBy(Product::getParentId, Collectors.toList()));
            resources = model.stream()
                    .filter(productOperations -> productOperations.getParentId() == null)
                    .map(productOperations -> {
                        ProductTree productTreeResource = new ProductTree();
                        productTreeResource.setNode(productOperations);
                        if (collect.get(productOperations.getId()) != null) {
                            productTreeResource.setChildren(collect.get(productOperations.getId()));
                        }
                        return productTreeResource;
                    }).toList();
        }
        return resources;
    }

    public static Optional<String> getLabel(String productRoleCode, Map<PartyRole, ProductRoleInfo> roleMappings) {
        return roleMappings.values().stream()
                .flatMap(productRoleInfo -> productRoleInfo.getRoles().stream())
                .filter(prodRole -> prodRole.getCode().equals(productRoleCode))
                .findAny()
                .map(ProductRole::getLabel);
    }

}