package it.pagopa.selfcare.commons.web.handler;

import feign.FeignException;
import it.pagopa.selfcare.commons.web.model.Problem;
import it.pagopa.selfcare.commons.web.model.mapper.ProblemMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Optional;

import static it.pagopa.selfcare.commons.web.handler.RestExceptionsHandler.UNHANDLED_EXCEPTION;

@Slf4j
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 1)
@ConditionalOnClass(FeignException.class)
public class FeignExceptionsHandler {

    public FeignExceptionsHandler() {
        log.trace("Initializing {}", FeignExceptionsHandler.class.getSimpleName());
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