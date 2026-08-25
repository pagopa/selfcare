package it.pagopa.selfcare.commons.web.handler;

import it.pagopa.selfcare.commons.web.model.Problem;
import it.pagopa.selfcare.commons.web.model.mapper.ProblemMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.servlet.ServletException;
import jakarta.validation.ValidationException;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

/**
 * The Class RestExceptionsHandler.
 */
@Slf4j
@Order
@RestControllerAdvice
public class RestExceptionsHandler {

    public static final String UNHANDLED_EXCEPTION = "unhandled exception: ";


    public RestExceptionsHandler() {
        log.trace("Initializing {}", RestExceptionsHandler.class.getSimpleName());
    }


    @ExceptionHandler({Exception.class})
    ResponseEntity<Problem> handleThrowable(Throwable e) {
        log.error(UNHANDLED_EXCEPTION, e);
        return ProblemMapper.toResponseEntity(new Problem(INTERNAL_SERVER_ERROR, e.getMessage()));
    }


    @ExceptionHandler({
            ValidationException.class,
            BindException.class,
            ServletException.class,
            MethodArgumentTypeMismatchException.class,
            MaxUploadSizeExceededException.class,
            HttpMessageNotReadableException.class
    })
    ResponseEntity<Problem> handleBadRequestException(Exception e) {
        log.warn(e.toString());
        return ProblemMapper.toResponseEntity(new Problem(BAD_REQUEST, e.getMessage()));
    }


    @ExceptionHandler({HttpMediaTypeNotAcceptableException.class})
    ResponseEntity<Problem> handleHttpMediaTypeNotAcceptableException(HttpMediaTypeNotAcceptableException e) {
        log.warn(e.toString());
        return ProblemMapper.toResponseEntity(new Problem(NOT_ACCEPTABLE, e.getMessage()));
    }


    @ExceptionHandler({HttpRequestMethodNotSupportedException.class})
    ResponseEntity<Problem> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn(e.toString());
        return ProblemMapper.toResponseEntity(new Problem(METHOD_NOT_ALLOWED, e.getMessage()));
    }


    @ExceptionHandler({MethodArgumentNotValidException.class})
    ResponseEntity<Problem> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        final Problem problem = new Problem(BAD_REQUEST, "Validation failed");
        problem.setInvalidParams(e.getFieldErrors().stream()
                .map(fieldError -> new Problem.InvalidParam(fieldError.getObjectName() + "." + fieldError.getField(), fieldError.getDefaultMessage()))
                .collect(Collectors.toList()));
        log.warn(e.toString());
        return ProblemMapper.toResponseEntity(problem);
    }


    @ExceptionHandler({AccessDeniedException.class})
    ResponseEntity<Problem> handleAccessDeniedException(AccessDeniedException e) {
        log.warn(e.toString());
        return ProblemMapper.toResponseEntity(new Problem(FORBIDDEN, e.getMessage()));
    }

}
