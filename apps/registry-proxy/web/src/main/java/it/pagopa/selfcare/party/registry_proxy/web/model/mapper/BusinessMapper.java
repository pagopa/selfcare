package it.pagopa.selfcare.party.registry_proxy.web.model.mapper;


import it.pagopa.selfcare.party.registry_proxy.connector.model.national_registries.Business;
import it.pagopa.selfcare.party.registry_proxy.web.model.BusinessResource;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.NONE)
public class BusinessMapper {
    public static BusinessResource toResource(Business business) {
        BusinessResource resource = null;
        if(business != null) {
            resource = new BusinessResource();
            resource.setBusinessName(business.getBusinessName());
            resource.setBusinessTaxId(business.getBusinessTaxId());
        }
        return resource;
    }
}
