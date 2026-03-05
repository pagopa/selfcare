package it.pagopa.selfcare.product.entity;

public class BackOfficeConfigurations {

    private String url;
    private String identityTokenAudience;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getIdentityTokenAudience() {
        return identityTokenAudience;
    }

    public void setIdentityTokenAudience(String identityTokenAudience) {
        this.identityTokenAudience = identityTokenAudience;
    }
}
