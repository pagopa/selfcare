package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Objects;

@Mapper(componentModel = "cdi")
public interface ProductMapper {

    @Mapping(source = "onboarding.productId", target = "productId")
    @Mapping(source = "onboarding.delegationId", target = "delegationId")
    @Mapping(target = "productRoles", expression = "java(setProductRoles(user))")
    @Mapping(target = "role", expression = "java(setRole(user))")
    org.openapi.quarkus.user_json.model.Product toProduct(Onboarding onboarding, User user);

    @Named("setRole")
    default String setRole(User user) {
        if(Objects.nonNull(user.getRole())) {
            return user.getRole().name();
        }
        return null;
    }

    @Named("setProductRoles")
    default List<String> setProductRoles(User user) {
        if(Objects.nonNull(user.getProductRole())) {
            return List.of(user.getProductRole());
        }
        return null;
    }
}
