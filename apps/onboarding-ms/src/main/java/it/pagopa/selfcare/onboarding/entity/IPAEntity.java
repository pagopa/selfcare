package it.pagopa.selfcare.onboarding.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.openapi.quarkus.party_registry_proxy_json.model.AOOResource;
import org.openapi.quarkus.party_registry_proxy_json.model.InstitutionResource;
import org.openapi.quarkus.party_registry_proxy_json.model.UOResource;

@Data
@Builder
@AllArgsConstructor
public class IPAEntity {

    private InstitutionResource institutionResource;
    private UOResource uoResource;
    private AOOResource aooResource;
}
