package it.pagopa.selfcare.party.registry_proxy.connector.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(of = "id")
public class DummyInsuranceCompany implements InsuranceCompany {

    private String id;
    private String originId;
    private String taxCode;
    private String description;
    private String digitalAddress;
    private String workType;
    private String registerType;
    private String address;

}