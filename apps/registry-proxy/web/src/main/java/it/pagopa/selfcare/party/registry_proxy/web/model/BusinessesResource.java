package it.pagopa.selfcare.party.registry_proxy.web.model;

import lombok.Data;

import java.util.List;

@Data
public class BusinessesResource {
    private String requestDateTime;
    private String legalTaxId;
    private List<BusinessResource> businesses;
}
