package iuh.fit.se.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@Getter
@NoArgsConstructor
public enum ErrorCode {
    // Existing codes
    UNCATEGORIZED_EXCEPTION(1001, "Uncategorized exception", HttpStatus.INTERNAL_SERVER_ERROR),
    USER_EXITS(1002, "User already exists", HttpStatus.BAD_REQUEST),
    NAME_INVALID(1003, "Name must not contain numbers or special characters", HttpStatus.BAD_REQUEST),
    EMAIL_INVALID(1004, "Email must be in the correct format", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(1005, "User not found", HttpStatus.NOT_FOUND),
    PASSWORD_INCORRECT(1006, "Password is incorrect", HttpStatus.UNAUTHORIZED),
    PASSWORD_INVALID(
            1007,
            "Password must contain at least one uppercase letter, one special character, and be at least 8 characters long. Ex: Thinh@123",
            HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1008, "Email or password is incorrect", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1009, "You are not authorized to perform this action", HttpStatus.FORBIDDEN),
    SELLER_EXISTS(1010, "Seller already exists", HttpStatus.BAD_REQUEST),
    SELLER_NOT_FOUND(1011, "Seller not found", HttpStatus.NOT_FOUND),
    UPLOAD_FILE_FAILED(1012, "Upload file failed", HttpStatus.INTERNAL_SERVER_ERROR),
    TOKEN_INVALID(1013, "Token is invalid", HttpStatus.UNAUTHORIZED),
    TOKEN_FAKE(1014, "Token is fake or has been tampered with", HttpStatus.UNAUTHORIZED),
    INVALID_REQUEST(1015, "Invalid request body", HttpStatus.BAD_REQUEST),
    METHOD_NOT_ALLOWED(1016, "HTTP method not supported", HttpStatus.METHOD_NOT_ALLOWED),
    MISSING_PARAMETER(1017, "Missing required parameter", HttpStatus.BAD_REQUEST),
    FEIGN_ENCODE_ERROR(1018, "Failed to encode request body", HttpStatus.BAD_REQUEST),
    INCORRECT_OTP(1019, "Incorrect OTP", HttpStatus.BAD_REQUEST),

    // Additional error codes for new exceptions
    FEIGN_CLIENT_ERROR(1020, "External service error", HttpStatus.BAD_GATEWAY),
    INVALID_PARAMETER_TYPE(1021, "Invalid parameter type", HttpStatus.BAD_REQUEST),
    FILE_SIZE_EXCEEDED(1022, "File size exceeded maximum limit", HttpStatus.PAYLOAD_TOO_LARGE),
    FILE_PROCESSING_ERROR(1023, "File processing error", HttpStatus.INTERNAL_SERVER_ERROR),
    ENDPOINT_NOT_FOUND(1024, "Endpoint not found", HttpStatus.NOT_FOUND),
    DATA_INTEGRITY_VIOLATION(1025, "Data integrity violation", HttpStatus.CONFLICT),
    DUPLICATE_ENTRY(1026, "Duplicate entry detected", HttpStatus.CONFLICT),
    DATABASE_ERROR(1027, "Database operation error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_ARGUMENT(1028, "Invalid argument provided", HttpStatus.BAD_REQUEST),
    NULL_POINTER_ERROR(1029, "Null pointer error occurred", HttpStatus.INTERNAL_SERVER_ERROR),

    // Additional validation errors
    INVALID_DATE_FORMAT(1030, "Invalid date format", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL_FORMAT(1031, "Invalid email format", HttpStatus.BAD_REQUEST),
    INVALID_PHONE_FORMAT(1032, "Invalid phone number format", HttpStatus.BAD_REQUEST),
    INVALID_UUID_FORMAT(1033, "Invalid UUID format", HttpStatus.BAD_REQUEST),

    // Business logic errors
    INSUFFICIENT_PERMISSIONS(1034, "Insufficient permissions", HttpStatus.FORBIDDEN),
    RESOURCE_LOCKED(1035, "Resource is currently locked", HttpStatus.LOCKED),
    OPERATION_NOT_ALLOWED(1036, "Operation not allowed in current state", HttpStatus.CONFLICT),
    QUOTA_EXCEEDED(1037, "Quota exceeded", HttpStatus.TOO_MANY_REQUESTS),

    // External service errors
    EXTERNAL_SERVICE_UNAVAILABLE(1038, "External service unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    EXTERNAL_SERVICE_TIMEOUT(1039, "External service timeout", HttpStatus.GATEWAY_TIMEOUT),

    // Rate limiting
    RATE_LIMIT_EXCEEDED(1040, "Rate limit exceeded", HttpStatus.TOO_MANY_REQUESTS),

    // Content errors
    CONTENT_TOO_LARGE(1041, "Content too large", HttpStatus.PAYLOAD_TOO_LARGE),
    UNSUPPORTED_MEDIA_TYPE(1042, "Unsupported media type", HttpStatus.UNSUPPORTED_MEDIA_TYPE),

    // Session and token errors
    SESSION_EXPIRED(1043, "Session expired", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(1044, "Token expired", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_INVALID(1045, "Refresh token invalid", HttpStatus.UNAUTHORIZED),
    PRODUCT_NOT_FOUND(1046, "Product not found", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND(1047, "Category not found", HttpStatus.NOT_FOUND),
    QUANTITY_INVALID(1048, "Quantity must be greater than zero", HttpStatus.BAD_REQUEST),
    CONCURRENT_MODIFICATION(1049, "Concurrent Product update conflict", HttpStatus.CONFLICT),;

    int code;
    String message;
    HttpStatusCode httpStatusCode;
}