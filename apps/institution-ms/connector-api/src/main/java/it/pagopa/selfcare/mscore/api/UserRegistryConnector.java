package it.pagopa.selfcare.mscore.api;

import it.pagopa.selfcare.mscore.model.user.User;

public interface UserRegistryConnector {

    User getUserByInternalIdWithFiscalCode(String userId);

    User getUserByInternalId(String userId);

    User getUserByInternalIdWithCustomFields(String userId, String fieldList);

    User getUserByFiscalCode(String fiscalCode);

    User persistUserUsingPatch(String name, String familyName, String fiscalCode, String email, String institutionId);

    User persistUserWorksContractUsingPatch(String fiscalCode, String email, String institutionId);
}
