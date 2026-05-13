package it.pagopa.selfcare.external_api.mapper;

import it.pagopa.selfcare.external_api.model.pnpg.CreatePnPgInstitution;
import it.pagopa.selfcare.external_api.model.pnpg.CreatePnPgInstitutionDto;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PnPgMapper {

    public static CreatePnPgInstitution fromDto(CreatePnPgInstitutionDto model) {
        CreatePnPgInstitution institution = null;
        if (model != null) {
            institution = new CreatePnPgInstitution();
            institution.setDescription(model.getDescription());
            institution.setExternalId(model.getExternalId());
        }
        return institution;
    }
}
