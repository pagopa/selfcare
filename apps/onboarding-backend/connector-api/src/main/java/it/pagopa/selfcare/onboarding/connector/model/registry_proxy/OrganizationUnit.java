package it.pagopa.selfcare.onboarding.connector.model.registry_proxy;

import it.pagopa.selfcare.commons.base.utils.Origin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrganizationUnit {
    private String id;
    private String ipaCode;
    private String institutionDescription;
    private String institutionFiscalCode;
    private String uniUoCode;
    private String uniUoParentCode;
    private String uoDescription;
    private String uniAooCode;
    private String mail1;
    private Origin origin;
    private String institutionDate;
    private String delegateName;
    private String delegateSurname;
    private String delegateMail;
    private String delegatePhoneNumber;
    private String municipalIstatCode;
    private String municipalCadastralCode;
    private String cap;
    private String address;
    private String phoneNumber;
    private String fax;
    private String mailType;
    private String url;
    private String updateDate;
}
