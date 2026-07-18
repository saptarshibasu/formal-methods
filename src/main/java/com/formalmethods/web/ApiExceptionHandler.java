package com.formalmethods.web;

import com.formalmethods.dto.ErrorResponse;
import com.formalmethods.service.IllegalTransitionException;
import com.formalmethods.service.OrderNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Maps domain exceptions to generic, client-facing {@link ErrorResponse}
 * bodies with the HTTP codes plan.md defines — fail-closed by default
 * (SEC-07/FR-017): any unmapped exception falls through to a generic 500
 * with no internal detail, never a state change.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(OrderNotFoundException ex) {
        LOG.warn("outcome=rejected reason=order_not_found detail={}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("order not found"));
    }

    @ExceptionHandler(IllegalTransitionException.class)
    public ResponseEntity<ErrorResponse> handleIllegalTransition(IllegalTransitionException ex) {
        LOG.warn("outcome=rejected reason=illegal_transition detail={}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("illegal transition"));
    }

    /**
     * FR-014/SEC-01: a malformed body (including a target status outside the
     * {@code OrderStatus} allow-list, which Jackson rejects while binding)
     * or a failed {@code @NotNull} check is rejected at the boundary with a
     * generic 400, before any lifecycle logic runs.
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<ErrorResponse> handleMalformedRequest(Exception ex) {
        LOG.warn("outcome=rejected reason=malformed_input detail={}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("invalid request"));
    }

    /** FR-014/SEC-01: a malformed order id (not a well-formed UUID) is rejected at the boundary. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMalformedPathVariable(MethodArgumentTypeMismatchException ex) {
        LOG.warn("outcome=rejected reason=malformed_order_id detail={}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("invalid request"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        LOG.error("outcome=rejected reason=unexpected_error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("internal error"));
    }
}
