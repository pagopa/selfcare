package it.pagopa.selfcare.mscore.api;


import it.pagopa.selfcare.mscore.model.institution.InstitutionUpdate;

import java.util.List;

public interface UserApiConnector {

  List<String> getUserEmails(String institutionId, String productId);

  void updateUserInstitution(String institutionId, InstitutionUpdate institutionUpdate);

}
