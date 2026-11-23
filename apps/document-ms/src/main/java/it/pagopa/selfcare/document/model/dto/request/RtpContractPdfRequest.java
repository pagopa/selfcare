package it.pagopa.selfcare.document.model.dto.request;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

/**
 * Contract request extension for prod-rtp specific data.
 */
@Schema(allOf = ContractPdfRequest.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RtpContractPdfRequest extends ContractPdfRequest {

    private List<@Valid RtpServiceType> rtpServiceTypes;
}
