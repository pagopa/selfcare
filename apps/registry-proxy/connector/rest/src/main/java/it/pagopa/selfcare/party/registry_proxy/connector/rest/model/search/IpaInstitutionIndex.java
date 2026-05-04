package it.pagopa.selfcare.party.registry_proxy.connector.rest.model.search;

import it.pagopa.selfcare.party.registry_proxy.connector.model.Institution;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IpaInstitutionIndex extends SearchServiceIndex {

    private String id;
    private String originId;
    private String taxCode;
    private String description;
    private String category;
    private String digitalAddress;
    private String address;
    private String zipCode;
    private String istatCode;
    private String origin;
    private String updateDate;

    public static IpaInstitutionIndex fromInstitution(Institution institution) {
        IpaInstitutionIndex index = new IpaInstitutionIndex();
        index.setAction("mergeOrUpload");
        index.setId(institution.getId());
        index.setOriginId(institution.getOriginId());
        index.setTaxCode(institution.getTaxCode());
        index.setDescription(institution.getDescription());
        index.setCategory(institution.getCategory());
        index.setDigitalAddress(institution.getDigitalAddress());
        index.setAddress(institution.getAddress());
        index.setZipCode(institution.getZipCode());
        index.setIstatCode(institution.getIstatCode());
        index.setOrigin(institution.getOrigin() != null ? institution.getOrigin().toString() : null);
        index.setUpdateDate(institution.getUpdateDate());
        return index;
    }

}
