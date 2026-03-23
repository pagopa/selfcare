package it.pagopa.selfcare.party.registry_proxy.web.handler;

import feign.FeignException;
import it.pagopa.selfcare.commons.web.model.Problem;
import it.pagopa.selfcare.commons.web.model.mapper.ProblemMapper;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.BadGatewayException;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.InvalidRequestException;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.party.registry_proxy.web.exception.ValidationFailedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Optional;

import static it.pagopa.selfcare.commons.web.handler.RestExceptionsHandler.UNHANDLED_EXCEPTION;
import static org.springframework.http.HttpStatus.*;

@Slf4j
@RestControllerAdvice
@Order(1)
public class PartyRegistryProxyExceptionHandler {

    public PartyRegistryProxyExceptionHandler() {
        log.trace("Initializing {}", PartyRegistryProxyExceptionHandler.class.getSimpleName());
    }

    @ExceptionHandler({ResourceNotFoundException.class})
    ResponseEntity<Problem> handleResourceNotFoundException(ResourceNotFoundException e) {
        log.warn(e.toString());
        return ProblemMapper.toResponseEntity(new Problem(NOT_FOUND, e.getMessage()));
    }

    @ExceptionHandler({InvalidRequestException.class})
    ResponseEntity<Problem> handleInvalidRequestException(InvalidRequestException e) {
        log.warn(e.toString());
        return ProblemMapper.toResponseEntity(new Problem(BAD_REQUEST, e.getMessage()));
    }

    @ExceptionHandler({BadGatewayException.class})
    ResponseEntity<Problem> handleBadGatewayException(BadGatewayException e) {
        log.warn(e.toString());
        return ProblemMapper.toResponseEntity(new Problem(BAD_GATEWAY, e.getMessage()));
    }

    @ExceptionHandler({ValidationFailedException.class})
    ResponseEntity<Problem> handleValidationFailedException(ValidationFailedException e) {
        log.warn(e.toString());
        return ProblemMapper.toResponseEntity(new Problem(BAD_REQUEST, e.getMessage()));
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Problem> handleFeignException(FeignException e) {
        HttpStatus httpStatus = Optional.ofNullable(HttpStatus.resolve(e.status()))
                .filter(status -> !status.is2xxSuccessful())
                .orElse(HttpStatus.INTERNAL_SERVER_ERROR);
        if (httpStatus.is4xxClientError()) {
            log.warn(e.toString());
        } else {
            log.error(UNHANDLED_EXCEPTION, e);
        }
        return ProblemMapper.toResponseEntity(new Problem(httpStatus, "An error occurred during a downstream service request"));
    }

}
