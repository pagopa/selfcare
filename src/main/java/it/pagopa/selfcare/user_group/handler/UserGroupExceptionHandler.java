package it.pagopa.selfcare.user_group.handler;

import it.pagopa.selfcare.commons.web.model.Problem;
import it.pagopa.selfcare.commons.web.model.mapper.ProblemMapper;
import it.pagopa.selfcare.user_group.controller.UserGroupV1Controller;
import it.pagopa.selfcare.user_group.exception.ResourceAlreadyExistsException;
import it.pagopa.selfcare.user_group.exception.ResourceNotFoundException;
import it.pagopa.selfcare.user_group.exception.ResourceUpdateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import static org.springframework.http.HttpStatus.*;

@ControllerAdvice(assignableTypes = {UserGroupV1Controller.class})
@Slf4j
public class UserGroupExceptionHandler {

    @ExceptionHandler({ResourceAlreadyExistsException.class})
    ResponseEntity<Problem> handleResourceAlreadyExistsException(ResourceAlreadyExistsException e) {
        log.warn(e.toString());
        return ProblemMapper.toResponseEntity(new Problem(CONFLICT, e.getMessage()));
    }

    @ExceptionHandler({ResourceUpdateException.class})
    ResponseEntity<Problem> handleResourceUpdateException(ResourceUpdateException e) {
        log.warn(e.toString());
        return ProblemMapper.toResponseEntity(new Problem(BAD_REQUEST, e.getMessage()));
    }

    @ExceptionHandler({ResourceNotFoundException.class})
    ResponseEntity<Problem> handleResourceNotFoundException(ResourceNotFoundException e) {
        log.warn(e.toString());
        return ProblemMapper.toResponseEntity(new Problem(NOT_FOUND, e.getMessage()));
    }
}
