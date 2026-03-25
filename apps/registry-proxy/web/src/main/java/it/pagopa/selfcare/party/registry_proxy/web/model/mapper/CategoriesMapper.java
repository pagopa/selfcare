package it.pagopa.selfcare.party.registry_proxy.web.model.mapper;

import it.pagopa.selfcare.party.registry_proxy.web.model.CategoriesResource;
import it.pagopa.selfcare.party.registry_proxy.web.model.CategoryResource;

import java.util.List;

public class CategoriesMapper {

    public static CategoriesResource toResource(List<CategoryResource> categories) {
        CategoriesResource categoriesResource = null;
        if (categories != null) {
            categoriesResource = new CategoriesResource();
            categoriesResource.setItems(categories);
        }
        return categoriesResource;
    }

}
