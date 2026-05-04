package it.pagopa.selfcare.user.controller.response;

import lombok.Data;

import java.util.List;

@Data
public class UsersNotificationResponse {
    private List<UserNotificationResponse> users;
}
