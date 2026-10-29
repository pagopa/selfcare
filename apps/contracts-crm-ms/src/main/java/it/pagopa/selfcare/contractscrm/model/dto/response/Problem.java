package it.pagopa.selfcare.contractscrm.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Error body as described by RFC 7807 (https://datatracker.ietf.org/doc/html/rfc7807). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Problem {
  private String title;
  private Integer status;
  private String detail;
}
