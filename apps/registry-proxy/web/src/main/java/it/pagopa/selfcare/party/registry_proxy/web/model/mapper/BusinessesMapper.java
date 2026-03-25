package it.pagopa.selfcare.party.registry_proxy.web.model.mapper;


import it.pagopa.selfcare.party.registry_proxy.connector.model.national_registries.Businesses;
import it.pagopa.selfcare.party.registry_proxy.web.model.BusinessesResource;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.NONE)
public class BusinessesMapper {

    public static BusinessesResource toResource(Businesses businesses) {
        BusinessesResource resource = null;
        if(businesses != null) {
            resource = new BusinessesResource();
            resource.setLegalTaxId(businesses.getLegalTaxId());
            resource.setRequestDateTime(businesses.getDateTimeExtraction());
            if(businesses.getBusinessList() != null) {
                resource.setBusinesses(
                        businesses.getBusinessList()
                                .stream()
                                .map(BusinessMapper::toResource)
                                .collect(Collectors.toList())
                );
            }

        }

        return resource;
    }
}
