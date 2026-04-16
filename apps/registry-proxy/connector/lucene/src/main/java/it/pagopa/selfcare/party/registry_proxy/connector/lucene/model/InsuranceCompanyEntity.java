package it.pagopa.selfcare.party.registry_proxy.connector.lucene.model;

import it.pagopa.selfcare.party.registry_proxy.connector.model.InsuranceCompany;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Origin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(of = "taxCode")
public class InsuranceCompanyEntity implements InsuranceCompany {

    private String id;
    private String originId;
    private String workType;
    private String registerType;
    private String taxCode;
    private String description;
    private String address;
    private String digitalAddress;
    private Origin origin;
}
