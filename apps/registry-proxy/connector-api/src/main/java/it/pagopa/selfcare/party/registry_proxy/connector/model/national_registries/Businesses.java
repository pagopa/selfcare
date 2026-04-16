package it.pagopa.selfcare.party.registry_proxy.connector.model.national_registries;

import lombok.Data;

import java.util.List;

@Data
public class Businesses{
    private String dateTimeExtraction;
    private String legalTaxId;
    private List<Business> businessList;
    private String code;
    private String appName;
    private String description;
    private String timestamp;
}
