package it.pagopa.selfcare.party.registry_proxy.web.model.mapper;

import it.pagopa.selfcare.party.registry_proxy.connector.model.Category;
import it.pagopa.selfcare.party.registry_proxy.web.model.CategoryResource;

public class CategoryMapper {

    public static CategoryResource toResource(Category category) {
        CategoryResource categoryResource = null;
        if (category != null) {
            categoryResource = new CategoryResource();
            categoryResource.setCode(category.getCode());
            categoryResource.setName(category.getName());
            categoryResource.setKind(category.getKind());
            categoryResource.setOrigin(category.getOrigin());
        }
        return categoryResource;
    }

}
