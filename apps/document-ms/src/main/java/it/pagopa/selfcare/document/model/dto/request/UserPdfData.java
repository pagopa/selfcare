package it.pagopa.selfcare.document.model.dto.request;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User data for PDF generation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPdfData {

    @NotBlank
    private String id;

    private PartyRole role;

    private String name;

    private String surname;

    @NotBlank
    private String taxCode;

    private String email;

    private String userMailUuid;
}
