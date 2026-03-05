package it.pagopa.selfcare.product.entity;

import java.util.Objects;

public class ProductRole {

    private String code;
    private String label;
    private String productLabel;
    private String description;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getProductLabel() {
        return productLabel;
    }

    public void setProductLabel(String productLabel) {
        this.productLabel = productLabel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductRole that)) return false;
        return Objects.equals(getCode(), that.getCode()) && Objects.equals(getLabel(), that.getLabel()) && Objects.equals(getDescription(), that.getDescription());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCode(), getLabel(), getDescription());
    }
}
