package it.pagopa.selfcare.mscore.model.institution;

import lombok.Data;

@Data
public class InstitutionProxyInfo {
    private String id;
    private String originId;
    private String o;
    private String ou;
    private String aoo;
    private String taxCode;
    private String category;
    private String description;
    private String digitalAddress;
    private String address;
    private String zipCode;
    private String origin;
    private String istatCode;
}
