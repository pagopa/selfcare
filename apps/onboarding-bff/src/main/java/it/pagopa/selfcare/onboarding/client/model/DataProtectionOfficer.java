package it.pagopa.selfcare.onboarding.client.model;

import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Data
@Schema(description = "${openapi.onboarding.institutions.model.dataProtectionOfficer}")
public class DataProtectionOfficer {

    private String address;
    private String email;
    private String pec;

}
