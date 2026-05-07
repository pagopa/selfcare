package it.pagopa.selfcare.dashboard.exception.handler;

import it.pagopa.selfcare.commons.web.model.Problem;
import it.pagopa.selfcare.commons.web.model.mapper.ProblemMapper;
import it.pagopa.selfcare.dashboard.exception.BadGatewayException;
import it.pagopa.selfcare.dashboard.exception.ResourceNotFoundException;
import it.pagopa.selfcare.dashboard.exception.SupportException;
import it.pagopa.selfcare.dashboard.exception.FileValidationException;
import it.pagopa.selfcare.dashboard.exception.InvalidProductRoleException;
import it.pagopa.selfcare.dashboard.exception.InvalidUserGroupException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ValidationException;

import static org.springframework.http.HttpStatus.*;

/**
 * Exception handler for dashboard controllers
 */
@Slf4j
@RestControllerAdvice
public class DashboardExceptionsHandler {

    @ExceptionHandler({
            FileValidationException.class,
            InvalidProductRoleException.class,
            ValidationException.class
    })
    public ResponseEntity<Problem> handleBadRequestException(Exception e) {
        log.warn(e.toString());
        return ProblemMapper.toResponseEntity(new Problem(BAD_REQUEST, e.getMessage()));
    }


    @ExceptionHandler({
            ResourceNotFoundException.class,
            InvalidUserGroupException.class
    })
    public ResponseEntity<Problem> handleNotFoundException(Exception e) {
        log.warn(e.toString());
        return ProblemMapper.toResponseEntity(new Problem(NOT_FOUND, e.getMessage()));
    }

    @ExceptionHandler({
            BadGatewayException.class,
    })
    public ResponseEntity<Problem> handleBadGatewayException(Exception e) {
        log.warn(e.toString());
        return ProblemMapper.toResponseEntity(new Problem(BAD_GATEWAY, e.getMessage()));
    }

    @ExceptionHandler({
            SupportException.class,
    })
    public ResponseEntity<Problem> handleSupportException(Exception e) {
        log.warn(e.toString());
        return ProblemMapper.toResponseEntity(new Problem(INTERNAL_SERVER_ERROR, e.getMessage()));
    }
  
    @ExceptionHandler({
            RuntimeException.class,
    })
    ResponseEntity<Problem> handleRuntimeException(RuntimeException e) {
        log.error(e.toString());
        return ProblemMapper.toResponseEntity(new Problem(INTERNAL_SERVER_ERROR, e.getMessage()));
    }

    @ExceptionHandler({AccessDeniedException.class})
    public ResponseEntity<Problem> handleAccessDeniedException(AccessDeniedException e) {
        log.warn(e.toString());
        return ProblemMapper.toResponseEntity(new Problem(FORBIDDEN, e.getMessage()));
    }

}