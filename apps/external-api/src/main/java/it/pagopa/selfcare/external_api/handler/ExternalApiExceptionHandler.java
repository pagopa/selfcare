package it.pagopa.selfcare.external_api.handler;

import it.pagopa.selfcare.external_api.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.springframework.http.HttpStatus.*;

@Slf4j
@RestControllerAdvice
public class ExternalApiExceptionHandler {
    @ExceptionHandler({InvalidRequestException.class})
    ResponseEntity<ProblemDetail> handleInvalidRequestException(InvalidRequestException e) {
        log.warn(e.toString());
        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(BAD_REQUEST, e.getMessage());
        problemDetail.setTitle(BAD_REQUEST.getReasonPhrase());
        return ResponseEntity.status(BAD_REQUEST).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problemDetail);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ProblemDetail> handleNotFoundException(Exception e) {
        log.warn(e.toString());
        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(NOT_FOUND, e.getMessage());
        problemDetail.setTitle(NOT_FOUND.getReasonPhrase());
        return ResponseEntity.status(NOT_FOUND).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problemDetail);
    }

    @ExceptionHandler({OnboardingNotAllowedException.class})
    ResponseEntity<ProblemDetail> handleOnboardingNotAllowedException(OnboardingNotAllowedException e) {
        log.warn(e.toString());
        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(FORBIDDEN, e.getMessage());
        problemDetail.setTitle(FORBIDDEN.getReasonPhrase());
        return ResponseEntity.status(FORBIDDEN).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problemDetail);
    }

    @ExceptionHandler({UpdateNotAllowedException.class})
    ResponseEntity<ProblemDetail> handleUpdateNotAllowedException(UpdateNotAllowedException e) {
        log.warn(e.toString());
        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(CONFLICT, e.getMessage());
        problemDetail.setTitle(CONFLICT.getReasonPhrase());
        return ResponseEntity.status(CONFLICT).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problemDetail);
    }

    @ExceptionHandler({InstitutionAlreadyOnboardedException.class})
    ResponseEntity<ProblemDetail> handleInstitutionAlreadyOnboardedException(InstitutionAlreadyOnboardedException e) {
        log.warn(e.toString());
        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(CONFLICT, e.getMessage());
        problemDetail.setTitle(CONFLICT.getReasonPhrase());
        return ResponseEntity.status(CONFLICT).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problemDetail);
    }
}
