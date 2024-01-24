package it.pagopa.selfcare.user.mapper;

import it.pagopa.selfcare.user.controller.response.product.InstitutionProducts;
import it.pagopa.selfcare.user.controller.response.product.Product;
import it.pagopa.selfcare.user.controller.response.product.UserProductsResponse;
import it.pagopa.selfcare.user.entity.OnboardedProduct;
import it.pagopa.selfcare.user.entity.UserInstitution;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "cdi")
public interface UserMapper {
    @Mapping(target = "id", source = "userId")
    @Mapping(target = "bindings", expression = "java(userInstitutionToBindings(userInstitution))")
    default UserProductsResponse toUserProductsResponse(List<UserInstitution> userInstitutions) {
        UserProductsResponse response = new UserProductsResponse();
        if(userInstitutions != null && !userInstitutions.isEmpty()) {
            response.setId(userInstitutions.get(0).getUserId());

            List<InstitutionProducts> institutionProducts = userInstitutions.stream().map(userInstitution -> {
                InstitutionProducts institutionProduct = new InstitutionProducts();
                institutionProduct.setInstitutionId(userInstitution.getInstitutionId());
                institutionProduct.setInstitutionName(userInstitution.getInstitutionDescription());
                institutionProduct.setProducts(toProducts(userInstitution.getProducts()));
                return institutionProduct;
            }).toList();
            response.setBindings(institutionProducts);
        }

        return response;
    }

    List<Product> toProducts(List<OnboardedProduct> products);

}
