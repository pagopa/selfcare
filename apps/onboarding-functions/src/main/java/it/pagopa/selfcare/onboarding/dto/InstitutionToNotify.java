package it.pagopa.selfcare.onboarding.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.entity.PaymentServiceProvider;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class InstitutionToNotify {

    private InstitutionType institutionType;
    private String description;
    private String digitalAddress;
    private String address;
    private String taxCode;
    private String origin;
    private String originId;
    private String zipCode;
    private String fileName;//fixme: needs to be moved the the upper class
    private String contentType;//fixme: ditto
    private PaymentServiceProvider paymentServiceProvider;
    private String istatCode;
    private String city;
    private String country;
    private String county;
    private String subUnitCode;
    private String category;
    private String subUnitType;
    private RootParent rootParent;
    private String supportEmail;

    public InstitutionType getInstitutionType() {
        return institutionType;
    }

    public void setInstitutionType(InstitutionType institutionType) {
        this.institutionType = institutionType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDigitalAddress() {
        return digitalAddress;
    }

    public void setDigitalAddress(String digitalAddress) {
        this.digitalAddress = digitalAddress;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getOriginId() {
        return originId;
    }

    public void setOriginId(String originId) {
        this.originId = originId;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public PaymentServiceProvider getPaymentServiceProvider() {
        return paymentServiceProvider;
    }

    public void setPaymentServiceProvider(PaymentServiceProvider paymentServiceProvider) {
        this.paymentServiceProvider = paymentServiceProvider;
    }

    public String getIstatCode() {
        return istatCode;
    }

    public void setIstatCode(String istatCode) {
        this.istatCode = istatCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCounty() {
        return county;
    }

    public void setCounty(String county) {
        this.county = county;
    }

    public String getSubUnitCode() {
        return subUnitCode;
    }

    public void setSubUnitCode(String subUnitCode) {
        this.subUnitCode = subUnitCode;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubUnitType() {
        return subUnitType;
    }

    public void setSubUnitType(String subUnitType) {
        this.subUnitType = subUnitType;
    }

    public RootParent getRootParent() {
        return rootParent;
    }

    public void setRootParent(RootParent rootParent) {
        this.rootParent = rootParent;
    }

    public String getSupportEmail() {
        return supportEmail;
    }

    public void setSupportEmail(String supportEmail) {
        this.supportEmail = supportEmail;
    }

}
