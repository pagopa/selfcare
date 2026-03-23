package it.pagopa.selfcare.party.registry_proxy.web.model.mapper;

import it.pagopa.selfcare.party.registry_proxy.connector.model.Institution;
import it.pagopa.selfcare.party.registry_proxy.web.model.InstitutionResource;

public class InstitutionMapper {

    public static InstitutionResource toResource(Institution institution) {
        InstitutionResource institutionResource = null;
        if (institution != null) {
            institutionResource = new InstitutionResource();
            institutionResource.setId(institution.getId());
            institutionResource.setOriginId(institution.getOriginId());
            institutionResource.setO(institution.getO());
            institutionResource.setOu(institution.getOu());
            institutionResource.setAoo(institution.getAoo());
            institutionResource.setTaxCode(institution.getTaxCode());
            institutionResource.setCategory(institution.getCategory());
            institutionResource.setDescription(institution.getDescription());
            institutionResource.setDigitalAddress(institution.getDigitalAddress());
            institutionResource.setAddress(institution.getAddress());
            institutionResource.setZipCode(institution.getZipCode());
            institutionResource.setOrigin(institution.getOrigin());
            institutionResource.setIstatCode(institution.getIstatCode());
        }
        return institutionResource;
    }

}
