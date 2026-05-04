package it.pagopa.selfcare.mscore.web.exception;

import it.pagopa.selfcare.mscore.constant.GenericError;
import it.pagopa.selfcare.mscore.exception.InvalidRequestException;
import it.pagopa.selfcare.mscore.exception.MsCoreException;
import it.pagopa.selfcare.mscore.exception.ResourceConflictException;
import it.pagopa.selfcare.mscore.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ValidationException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@Slf4j
public class CustomExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(@NonNull MissingServletRequestParameterException ex, @NonNull HttpHeaders headers, @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        log.error("InvalidRequestException Occurred --> MESSAGE:{}, STATUS: {}",ex.getMessage(), HttpStatus.BAD_REQUEST, ex);
        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "MISSING PARAMETER");
        problemDetail.setTitle(HttpStatus.BAD_REQUEST.getReasonPhrase());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problemDetail);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(@NonNull MethodArgumentNotValidException ex, HttpHeaders headers, @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        log.error("InvalidRequestException Occurred --> MESSAGE:{}, STATUS: {}",ex.getMessage(), HttpStatus.BAD_REQUEST, ex);
        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "INVALID ARGUMENT");
        problemDetail.setTitle(HttpStatus.BAD_REQUEST.getReasonPhrase());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problemDetail);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFoundException(HttpServletRequest request, ResourceNotFoundException ex) {
        log.error("ResourceNotFoundException Occurred --> URL:{}, MESSAGE:{}, STATUS: {}",  Encode.forJava(String.valueOf(request.getRequestURL())), ex.getMessage(), HttpStatus.NOT_FOUND, ex);
        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle(HttpStatus.NOT_FOUND.getReasonPhrase());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problemDetail);
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ProblemDetail> handleResourceConflictException(HttpServletRequest request, ResourceConflictException ex) {
        log.error("ResourceConflictException Occurred --> URL:{}, MESSAGE:{}, STATUS: {}",  Encode.forJava(String.valueOf(request.getRequestURL())), ex.getMessage(), HttpStatus.CONFLICT, ex);
        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle(HttpStatus.CONFLICT.getReasonPhrase());
        return ResponseEntity.status(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problemDetail);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ProblemDetail> handleInvalidRequestException(HttpServletRequest request, InvalidRequestException ex) {
        log.error("InvalidRequestException Occurred --> URL:{}, MESSAGE:{}, STATUS:{}", Encode.forJava(String.valueOf(request.getRequestURL())), ex.getMessage(), HttpStatus.BAD_REQUEST, ex);
        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle(HttpStatus.BAD_REQUEST.getReasonPhrase());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problemDetail);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(HttpServletRequest request, ValidationException ex) {
        log.error("ValidationException Occurred --> URL:{}, MESSAGE:{}, STATUS:{}", Encode.forJava(String.valueOf(request.getRequestURL())), ex.getMessage(), HttpStatus.BAD_REQUEST, ex);
        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle(HttpStatus.BAD_REQUEST.getReasonPhrase());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problemDetail);
    }

    @ExceptionHandler(MsCoreException.class)
    public ResponseEntity<ProblemDetail> handleMsCoreException(HttpServletRequest request, MsCoreException ex) {
        log.error("Exception Occurred --> URL:{}, MESSAGE:{}, STATUS:{}", Encode.forJava(String.valueOf(request.getRequestURL())), ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, ex);
        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        problemDetail.setTitle(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problemDetail);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleException(HttpServletRequest request, Exception ex) {
        log.error("{} Occurred --> URL:{}, MESSAGE:{}, STATUS:{}",ex.getCause(), Encode.forJava(String.valueOf(request.getRequestURL())), ex.getMessage(), HttpStatus.BAD_REQUEST, ex);
        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        problemDetail.setTitle(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problemDetail);
    }

    private GenericError retrieveGenericError(HttpServletRequest request){
        GenericError genericError = (GenericError) request.getAttribute("errorEnum");
        if(genericError == null){
            genericError = GenericError.GENERIC_ERROR;
        }
        return genericError;
    }
}
