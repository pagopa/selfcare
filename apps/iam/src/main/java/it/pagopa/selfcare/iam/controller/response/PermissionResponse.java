package it.pagopa.selfcare.iam.controller.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PermissionResponse {
  private boolean hasPermission;

  public PermissionResponse() {}

  public PermissionResponse(boolean hasPermission) {
    this.hasPermission = hasPermission;
  }

  public boolean hasPermission() {
    return hasPermission;
  }
}
