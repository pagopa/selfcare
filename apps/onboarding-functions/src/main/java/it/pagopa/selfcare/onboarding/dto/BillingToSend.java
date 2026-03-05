package it.pagopa.selfcare.onboarding.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BillingToSend {
    private String vatNumber;
    private String recipientCode;
    private Boolean publicServices;
    private Boolean publicService;
    private String taxCodeInvoicing;

    public String getVatNumber() {
        return vatNumber;
    }

    public void setVatNumber(String vatNumber) {
        this.vatNumber = vatNumber;
    }

    public String getRecipientCode() {
        return recipientCode;
    }

    public void setRecipientCode(String recipientCode) {
        this.recipientCode = recipientCode;
    }

    public Boolean isPublicServices() {
        return publicServices;
    }

    public void setPublicServices(Boolean publicServices) {
        this.publicServices = publicServices;
    }

    public Boolean isPublicService() {
        return publicService;
    }

    public void setPublicService(Boolean publicService) {
        this.publicService = publicService;
    }

    public String getTaxCodeInvoicing() { return taxCodeInvoicing; }

    public void setTaxCodeInvoicing(String taxCodeInvoicing) { this.taxCodeInvoicing = taxCodeInvoicing; }
}
