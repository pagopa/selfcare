package it.pagopa.selfcare.dashboard.model.institution;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

@Data
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InstitutionBackofficeAdmin implements Serializable {
    private String id;
    @JsonProperty("fiscal_code")
    private String taxCode;
    private String name;
    private List<RoleBackofficeAdmin> roles;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> groups;
    private String subUnitCode;
    private String subUnitType;
    private String aooParent;
    @Deprecated
    private String parentDescription;
    @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = RootParent.class)
    private RootParent rootParent;
    @JsonProperty("ipaCode")
    private String originId;
}
