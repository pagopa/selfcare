package it.pagopa.selfcare.user_group.handler;

import it.pagopa.selfcare.user_group.controller.UserGroupV1Controller;
import it.pagopa.selfcare.user_group.exception.ResourceAlreadyExistsException;
import it.pagopa.selfcare.user_group.exception.ResourceNotFoundException;
import it.pagopa.selfcare.user_group.exception.ResourceUpdateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.springframework.http.HttpStatus.*;

@RestControllerAdvice(assignableTypes = {UserGroupV1Controller.class})
@Slf4j
public class UserGroupExceptionHandler {

    @ExceptionHandler({ResourceAlreadyExistsException.class})
    ResponseEntity<ProblemDetail> handleResourceAlreadyExistsException(ResourceAlreadyExistsException e) {
        log.warn(e.toString());
        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(CONFLICT, e.getMessage());
        problemDetail.setTitle(CONFLICT.getReasonPhrase());
        return ResponseEntity.status(CONFLICT).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problemDetail);
    }

    @ExceptionHandler({ResourceUpdateException.class})
    ResponseEntity<ProblemDetail> handleResourceUpdateException(ResourceUpdateException e) {
        log.warn(e.toString());
        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(BAD_REQUEST, e.getMessage());
        problemDetail.setTitle(BAD_REQUEST.getReasonPhrase());
        return ResponseEntity.status(BAD_REQUEST).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problemDetail);
    }

    @ExceptionHandler({ResourceNotFoundException.class})
    ResponseEntity<ProblemDetail> handleResourceNotFoundException(ResourceNotFoundException e) {
        log.warn(e.toString());
        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(NOT_FOUND, e.getMessage());
        problemDetail.setTitle(NOT_FOUND.getReasonPhrase());
        return ResponseEntity.status(NOT_FOUND).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problemDetail);
    }
}
