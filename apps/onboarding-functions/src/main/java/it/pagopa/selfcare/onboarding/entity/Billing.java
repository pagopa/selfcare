package it.pagopa.selfcare.onboarding.entity;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Billing {
    private String vatNumber;
    private String recipientCode;
    private boolean publicServices;
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

    public boolean isPublicServices() {
        return publicServices;
    }

    public void setPublicServices(boolean publicServices) {
        this.publicServices = publicServices;
    }

    public String getTaxCodeInvoicing() { return taxCodeInvoicing; }

    public void setTaxCodeInvoicing(String taxCodeInvoicing) { this.taxCodeInvoicing = taxCodeInvoicing; }
}
