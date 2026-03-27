package it.pagopa.selfcare.onboarding.web.handler;


import it.pagopa.selfcare.commons.web.model.Problem;
import it.pagopa.selfcare.commons.web.model.mapper.ProblemMapper;
import it.pagopa.selfcare.onboarding.connector.exceptions.UnauthorizedUserException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class TokenExceptionHandler {

    @ExceptionHandler({UnauthorizedUserException.class})
    ResponseEntity<Problem> handleUserNotAllowedException(UnauthorizedUserException e) {
        log.warn(e.toString());
        return ProblemMapper.toResponseEntity(new Problem(HttpStatus.FORBIDDEN, e.getMessage()));
    }
}
