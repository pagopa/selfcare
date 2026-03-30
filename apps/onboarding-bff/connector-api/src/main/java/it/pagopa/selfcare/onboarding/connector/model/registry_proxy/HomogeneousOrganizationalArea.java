package it.pagopa.selfcare.onboarding.connector.model.registry_proxy;

import it.pagopa.selfcare.commons.base.utils.Origin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HomogeneousOrganizationalArea {
    private String id;
    private String ipaCode;
    private String institutionDescription;
    private String institutionFiscalCode;
    private String uniAooCode;
    private String aooDescription;
    private String mail1;
    private String codAoo;
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
    private String itProtocol;
    private String uriProtocol;
    private String updateDate;

}
