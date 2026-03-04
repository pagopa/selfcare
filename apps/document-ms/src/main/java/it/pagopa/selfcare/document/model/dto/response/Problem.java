package it.pagopa.selfcare.product.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Problem {
  /**
   * A \"problem detail\" as a way to carry machine-readable details of errors
   * (https://datatracker.ietf.org/doc/html/rfc7807)
   */
  private String detail;

  /**
   * A \"problem detail\" as a way to carry machine-readable details of errors
   * (https://datatracker.ietf.org/doc/html/rfc7807)
   */
  private String instance;

  /**
   * A \"problem detail\" as a way to carry machine-readable details of errors
   * (https://datatracker.ietf.org/doc/html/rfc7807)
   */
  private Integer status;

  /**
   * A \"problem detail\" as a way to carry machine-readable details of errors
   * (https://datatracker.ietf.org/doc/html/rfc7807)
   */
  private String title;

  /**
   * A \"problem detail\" as a way to carry machine-readable details of errors
   * (https://datatracker.ietf.org/doc/html/rfc7807)
   */
  private String type;
}
