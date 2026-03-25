package it.pagopa.selfcare.party.registry_proxy.web.model;

import java.util.List;
import lombok.Data;

@Data
public class PDNDBusinessResource {

    private String businessTaxId;
    private String businessName;
    private String legalNature;
    private String legalNatureDescription;
    private String cciaa;
    private String nRea;
    private String businessStatus;
    private String vatNumber;
    private String legalForm;
    private String city;
    private String county;
    private String zipCode;
    private String address;
    private String digitalAddress;
    private List<String> atecoCodes;
    private String disabledStateInstitution;
    private String descriptionStateInstitution;
    private String statusCompanyRI;
    private String statusCompanyRD;

}
