package it.pagopa.selfcare.onboarding.entity;


import java.util.List;

public class PaymentServiceProvider extends BusinessData {

    private String abiCode;
    private boolean vatNumberGroup;
    private List<String> providerNames;
    private String contractType;
    private String contractId;

    public String getAbiCode() {
        return abiCode;
    }

    public void setAbiCode(String abiCode) {
        this.abiCode = abiCode;
    }

    public boolean isVatNumberGroup() {
        return vatNumberGroup;
    }

    public void setVatNumberGroup(boolean vatNumberGroup) {
        this.vatNumberGroup = vatNumberGroup;
    }

    public List<String> getProviderNames() {
        return providerNames;
    }

    public void setProviderNames(List<String> providerNames) {
        this.providerNames = providerNames;
    }

    public String getContractType() {
        return contractType;
    }

    public void setContractType(String contractType) {
        this.contractType = contractType;
    }

    public String getContractId() {
        return contractId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }
}
