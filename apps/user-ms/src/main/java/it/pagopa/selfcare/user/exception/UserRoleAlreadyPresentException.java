package it.pagopa.selfcare.user.exception;

public class UserRoleAlreadyPresentException extends  RuntimeException{

    public UserRoleAlreadyPresentException(String message) {
        super(message);
    }
}
