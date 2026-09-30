package com.rentalert.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionController {

    private final MessageSource messageSource;

    @ExceptionHandler(AppException.class)
    public ResponseEntity<@NonNull ExceptionBody> handleAppException(AppException e, HttpServletRequest request) {
        ExceptionBody body = buildBody(e, e.getErrorCode(), request, e.getHttpStatus(), false);
        return ResponseEntity.status(e.getHttpStatus()).body(body);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<@NonNull ExceptionBody> handleNoResourceFound(NoResourceFoundException exception, HttpServletRequest request) {
        ExceptionBody body = buildBody(exception, InternalExceptionCode.EX404_ROUTE, request, HttpStatus.NOT_FOUND, false);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<@NonNull ExceptionBody> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> errors = exception.getBindingResult()
            .getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                fieldError -> fieldError.getDefaultMessage() == null ? "Invalid value" : fieldError.getDefaultMessage(),
                (exist, newMessage) -> exist + " " + newMessage
            ));
        ExceptionBody body = buildBody(exception, InternalExceptionCode.EX400, request, HttpStatus.BAD_REQUEST, false);
        body.setDetails(errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<@NonNull ExceptionBody> handleNotReadable(HttpMessageNotReadableException exception, HttpServletRequest request) {
        ExceptionBody body = buildBody(exception, InternalExceptionCode.EX400_MALFORMED, request, HttpStatus.BAD_REQUEST, false);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<@NonNull ExceptionBody> handleMissingParam(MissingServletRequestParameterException exception, HttpServletRequest request) {
        ExceptionBody body = buildBody(exception, InternalExceptionCode.EX400_MALFORMED, request, HttpStatus.BAD_REQUEST, false);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<@NonNull ExceptionBody> handleTypeMismatch(MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        ExceptionBody body = buildBody(exception, InternalExceptionCode.EX400_MALFORMED, request, HttpStatus.BAD_REQUEST, false);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<@NonNull ExceptionBody> handleNoHandler(NoHandlerFoundException exception, HttpServletRequest request) {
        ExceptionBody body = buildBody(exception, InternalExceptionCode.EX404_ROUTE, request, HttpStatus.NOT_FOUND, false);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<@NonNull ExceptionBody> handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        ExceptionBody body = buildBody(exception, InternalExceptionCode.EX403, request, HttpStatus.FORBIDDEN, false);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(IncorrectResultSizeDataAccessException.class)
    public ResponseEntity<@NonNull ExceptionBody> handleIncorrectResultSize(IncorrectResultSizeDataAccessException exception, HttpServletRequest request) {
        ExceptionBody body = buildBody(exception, InternalExceptionCode.EX400, request, HttpStatus.BAD_REQUEST, false);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<@NonNull ExceptionBody> handleOptimisticLock(OptimisticLockingFailureException exception, HttpServletRequest request) {
        ExceptionBody body = buildBody(exception, InternalExceptionCode.EX409_OPTIMISTIC_LOCK, request, HttpStatus.CONFLICT, true);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<@NonNull ExceptionBody> handleDataIntegrityViolation(DataIntegrityViolationException exception, HttpServletRequest request) {
        ExceptionBody body = buildBody(exception, InternalExceptionCode.EX409, request, HttpStatus.CONFLICT, true);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<@NonNull ExceptionBody> handleException(Exception exception, HttpServletRequest request) {
        ExceptionBody body = buildBody(exception, InternalExceptionCode.EX500, request, HttpStatus.INTERNAL_SERVER_ERROR, true);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private ExceptionBody buildBody(Exception exception, InternalExceptionCode defaultErrorCode, HttpServletRequest request,
        HttpStatus status, boolean forceDefaultMessage) {

        Locale locale = resolveLocale(request);

        InternalExceptionCode errorCode = defaultErrorCode;
        if (!forceDefaultMessage && exception instanceof AppException appException) {
            errorCode = appException.getErrorCode();
        }

        String propertiesKey = "ERROR_" + errorCode.getLabel();

        String localizedMessage;
        try {
            localizedMessage = messageSource.getMessage(propertiesKey, null, locale);
        } catch (NoSuchMessageException e) {
            log.warn("Translation key not found in bundle: {}", propertiesKey);
            localizedMessage = propertiesKey;
        }

        UUID errorId = UUID.randomUUID();
        String path = request.getServletPath();

        if (status.is5xxServerError()) {
            log.error("PATH: {} --- STATUS: {} --- errorId: {}", path, status.value(), errorId, exception);
        } else {
            log.warn("PATH: {} --- STATUS: {} --- '{}' --- errorId: {}", path, status.value(), exception.getMessage(), errorId);
        }

        Map<String, String> details = Map.of(
            "errorId", errorId.toString(),
            "errorCode", String.valueOf(errorCode.getCode())
        );

        return ExceptionBody.builder()
            .message(localizedMessage)
            .details(details)
            .build();
    }

    private Locale resolveLocale(HttpServletRequest request) {
        String acceptLanguage = request.getHeader("Accept-Language");
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return Locale.forLanguageTag("ru");
        }
        return request.getLocale();
    }
}