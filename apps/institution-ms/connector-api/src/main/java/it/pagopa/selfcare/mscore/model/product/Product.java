package it.pagopa.selfcare.mscore.model.product;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.pagopa.selfcare.commons.base.security.PartyRole;
import lombok.Data;

import java.util.EnumMap;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Product {

    private String id;
    private EnumMap<PartyRole, ProductRoleInfo> roleMappings;
    private String contractTemplatePath;
    private String contractTemplateVersion;
    private String title;
    private String parentId;
    private ProductStatus status;
}
