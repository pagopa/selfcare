package it.pagopa.selfcare.webhook.dto;

import java.util.List;
import lombok.Data;

@Data
public class NotificationResendResponse {

  private int resentCount;
  private List<String> notificationIds;
}
