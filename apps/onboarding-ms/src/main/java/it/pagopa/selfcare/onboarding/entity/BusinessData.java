package it.pagopa.selfcare.onboarding.entity;


import lombok.Data;

@Data
public class BusinessData {

    private String businessRegisterNumber;
    private String legalRegisterNumber;
    private String legalRegisterName;
    private boolean longTermPayments;

}