package it.pagopa.selfcare.user.model;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class LoggedUser {
    String uid;
    String name;
    String familyName;
}