package it.pagopa.selfcare.party.registry_proxy.connector.lucene.model;

import it.pagopa.selfcare.party.registry_proxy.connector.model.Institution;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Origin;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = "id")
public class InstitutionEntity implements Institution {

    private String id;
    private String originId;
    private String o;
    private String ou;
    private String aoo;
    private String taxCode;
    private String category;
    private String description;
    private String descriptionfull;
    private String digitalAddress;
    private String address;
    private String zipCode;
    private Origin origin;
    private String istatCode;

}
