package it.pagopa.selfcare.dashboard.model.onboarding;

import it.pagopa.selfcare.dashboard.model.user.Certification;
import it.pagopa.selfcare.dashboard.model.user.InstitutionContact;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * PersonInfo
 */
@Data
public class PersonInfo {

    private String name;
    private String surname;
    private String taxCode;
    private Certification certification;
    private Map<String, List<InstitutionContact>> institutionContacts;

}
