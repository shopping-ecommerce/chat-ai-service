package iuh.fit.se.exception;
import iuh.fit.se.dto.ApiResponse;
import jakarta.websocket.EncodeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.sql.SQLException;
import java.text.ParseException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse> handleRuntimeException(Exception e) {
        log.error("Uncategorized exception: ", e);
        ErrorCode errorCode = ErrorCode.UNCATEGORIZED_EXCEPTION;
        ApiResponse apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse> handleAppException(AppException e) {
        log.error("Application exception: ", e);
        ErrorCode errorCode = e.getErrorCode();
        ApiResponse apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("Validation exception: ", e);
        String enumKey = e.getFieldError().getDefaultMessage();
        ErrorCode errorCode;
        try {
            errorCode = ErrorCode.valueOf(enumKey);
        } catch (IllegalArgumentException ex) {
            errorCode = ErrorCode.INVALID_REQUEST;
        }
        ApiResponse apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    ResponseEntity<ApiResponse> handleAccessDeniedException(AccessDeniedException e) {
        log.error("Access denied exception: ", e);
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        ApiResponse apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = org.springframework.security.access.AccessDeniedException.class)
    ResponseEntity<ApiResponse> handleSpringAccessDeniedException(org.springframework.security.access.AccessDeniedException e) {
        log.error("Spring security access denied exception: ", e);
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        ApiResponse apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = JwtException.class)
    ResponseEntity<ApiResponse> handleJwtException(JwtException e) {
        log.error("JWT exception: ", e);
        ErrorCode errorCode = ErrorCode.TOKEN_INVALID;
        ApiResponse apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = ParseException.class)
    ResponseEntity<ApiResponse> handleParseException(ParseException e) {
        log.error("Parse exception: ", e);
        ErrorCode errorCode = ErrorCode.TOKEN_FAKE;
        ApiResponse apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = AuthenticationServiceException.class)
    ResponseEntity<ApiResponse> handleAuthenticationServiceException(AuthenticationServiceException e) {
        log.error("Authentication service exception: ", e);
        ErrorCode errorCode = ErrorCode.UNAUTHENTICATED;
        ApiResponse apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = AuthorizationDeniedException.class)
    ResponseEntity<ApiResponse> handleAuthorizationDeniedException(AuthorizationDeniedException e) {
        log.error("Authorization denied exception: ", e);
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        ApiResponse apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    ResponseEntity<ApiResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error("HTTP message not readable exception: ", e);
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
        ApiResponse apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message("Invalid request body format")
                .build();
        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiResponse> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.error("HTTP method not supported exception: ", e);
        ErrorCode errorCode = ErrorCode.METHOD_NOT_ALLOWED;
        ApiResponse apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message("HTTP method '" + e.getMethod() + "' not supported")
                .build();
        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = MissingServletRequestParameterException.class)
    ResponseEntity<ApiResponse> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.error("Missing parameter exception: ", e);
        ErrorCode errorCode = ErrorCode.MISSING_PARAMETER;
        ApiResponse apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message("Missing required parameter: " + e.getParameterName())
                .build();
        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(apiResponse);
    }

    // Feign Client Exceptions
    @ExceptionHandler(value = EncodeException.class)
    ResponseEntity<ApiResponse> handleFeignEncodeException(EncodeException e) {
        log.error("Feign encode exception: ", e);
        ErrorCode errorCode = ErrorCode.FEIGN_ENCODE_ERROR;
        ApiResponse apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message("Failed to encode request body")
                .build();
        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(apiResponse);
    }


    // Additional Common Exceptions
    @ExceptionHandler(value = MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.error("Method argument type mismatch exception: ", e);
        ErrorCode errorCode = ErrorCode.INVALID_PARAMETER_TYPE;
        ApiResponse apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message("Invalid parameter type for: " + e.getName())
                .build();
        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = MaxUploadSizeExceededException.class)
    ResponseEntity<ApiResponse> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.error("Max upload size exceeded exception: ", e);
        ErrorCode errorCode = ErrorCode.FILE_SIZE_EXCEEDED;
        ApiResponse apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = IOException.class)
    ResponseEntity<ApiResponse> handleIOException(IOException e) {
        log.error("IO exception: ", e);
        ErrorCode errorCode = ErrorCode.FILE_PROCESSING_ERROR;
        ApiResponse apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = NoHandlerFoundException.class)
    ResponseEntity<ApiResponse> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.error("No handler found exception: ", e);
        ErrorCode errorCode = ErrorCode.ENDPOINT_NOT_FOUND;
        ApiResponse apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message("Endpoint not found: " + e.getRequestURL())
                .build();
        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(apiResponse);
    }

    // Database Exceptions
    @ExceptionHandler(value = DataIntegrityViolationException.class)
    ResponseEntity<ApiResponse> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.error("Data integrity violation exception: ", e);
        ErrorCode errorCode = ErrorCode.DATA_INTEGRITY_VIOLATION;
        ApiResponse apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = DuplicateKeyException.class)
    ResponseEntity<ApiResponse> handleDuplicateKeyException(DuplicateKeyException e) {
        log.error("Duplicate key exception: ", e);
        ErrorCode errorCode = ErrorCode.DUPLICATE_ENTRY;
        ApiResponse apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = SQLException.class)
    ResponseEntity<ApiResponse> handleSQLException(SQLException e) {
        log.error("SQL exception: ", e);
        ErrorCode errorCode = ErrorCode.DATABASE_ERROR;
        ApiResponse apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = DataAccessException.class)
    ResponseEntity<ApiResponse> handleDataAccessException(DataAccessException e) {
        log.error("Data access exception: ", e);
        ErrorCode errorCode = ErrorCode.DATABASE_ERROR;
        ApiResponse apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = IllegalArgumentException.class)
    ResponseEntity<ApiResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("Illegal argument exception: ", e);
        ErrorCode errorCode = ErrorCode.INVALID_ARGUMENT;
        ApiResponse apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(e.getMessage() != null ? e.getMessage() : errorCode.getMessage())
                .build();
        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = NullPointerException.class)
    ResponseEntity<ApiResponse> handleNullPointerException(NullPointerException e) {
        log.error("Null pointer exception: ", e);
        ErrorCode errorCode = ErrorCode.NULL_POINTER_ERROR;
        ApiResponse apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(apiResponse);
    }
}