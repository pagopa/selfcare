package it.pagopa.selfcare.auth.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpMailInfoResponse {

  /**
   * Identifier of the mail request
   */
  private String mailRequestId;

  /**
   * Current status of the email delivery
   */
  private String status;

  /**
   * Recipient of the OTP email
   */
  private String recipient;

  /**
   * Number of attempts made to send the email
   */
  private Integer attempts;

  /**
   * History of status changes
   */
  private List<MailStatusHistory> history;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MailStatusHistory {

    /**
     * Status after the change
     */
    private String status;

    /**
     * Timestamp of the status change
     */
    private OffsetDateTime changedAt;
  }
}
