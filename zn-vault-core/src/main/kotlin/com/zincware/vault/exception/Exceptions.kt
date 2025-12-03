// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincware/vault/exception/Exceptions.kt
package com.zincware.vault.exception

/**
 * Base exception for all ZN-Vault SDK errors.
 */
open class ZnVaultException(
    message: String,
    val statusCode: Int? = null,
    val errorCode: String? = null,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * Authentication failed (401).
 */
class AuthenticationException(
    message: String = "Authentication failed",
    errorCode: String? = null,
    cause: Throwable? = null
) : ZnVaultException(message, 401, errorCode, cause)

/**
 * Authorization/permission denied (403).
 */
class AuthorizationException(
    message: String = "Access denied",
    errorCode: String? = null,
    cause: Throwable? = null
) : ZnVaultException(message, 403, errorCode, cause)

/**
 * Resource not found (404).
 */
class NotFoundException(
    message: String = "Resource not found",
    val resourceType: String? = null,
    val resourceId: String? = null,
    cause: Throwable? = null
) : ZnVaultException(message, 404, "NOT_FOUND", cause)

/**
 * Request validation failed (400).
 */
class ValidationException(
    message: String = "Validation failed",
    val errors: List<String> = emptyList(),
    cause: Throwable? = null
) : ZnVaultException(message, 400, "VALIDATION_ERROR", cause)

/**
 * Conflict error, e.g., resource already exists (409).
 */
class ConflictException(
    message: String = "Resource already exists",
    cause: Throwable? = null
) : ZnVaultException(message, 409, "CONFLICT", cause)

/**
 * Resource expired, e.g., TTL exceeded (410).
 */
class ExpiredException(
    message: String = "Resource has expired",
    cause: Throwable? = null
) : ZnVaultException(message, 410, "EXPIRED", cause)

/**
 * Rate limit exceeded (429).
 */
class RateLimitException(
    message: String = "Rate limit exceeded",
    val retryAfterSeconds: Int? = null,
    cause: Throwable? = null
) : ZnVaultException(message, 429, "RATE_LIMIT_EXCEEDED", cause)

/**
 * Account locked due to failed login attempts (423).
 */
class AccountLockedException(
    message: String = "Account is locked",
    cause: Throwable? = null
) : ZnVaultException(message, 423, "ACCOUNT_LOCKED", cause)

/**
 * Quota exceeded (429).
 */
class QuotaExceededException(
    message: String = "Quota exceeded",
    val resourceType: String? = null,
    val current: Int? = null,
    val limit: Int? = null,
    cause: Throwable? = null
) : ZnVaultException(message, 429, "QUOTA_EXCEEDED", cause)

/**
 * Server error (5xx).
 */
class ServerException(
    message: String = "Internal server error",
    statusCode: Int = 500,
    cause: Throwable? = null
) : ZnVaultException(message, statusCode, "SERVER_ERROR", cause)

/**
 * Connection/network error.
 */
class ConnectionException(
    message: String = "Connection failed",
    cause: Throwable? = null
) : ZnVaultException(message, null, "CONNECTION_ERROR", cause)

/**
 * Request timeout.
 */
class TimeoutException(
    message: String = "Request timed out",
    cause: Throwable? = null
) : ZnVaultException(message, null, "TIMEOUT", cause)

/**
 * TLS/SSL error.
 */
class TlsException(
    message: String = "TLS error",
    cause: Throwable? = null
) : ZnVaultException(message, null, "TLS_ERROR", cause)

/**
 * Two-factor authentication required.
 */
class TwoFactorRequiredException(
    message: String = "Two-factor authentication required",
    cause: Throwable? = null
) : ZnVaultException(message, 401, "2FA_REQUIRED", cause)

/**
 * Invalid two-factor authentication code.
 */
class InvalidTotpException(
    message: String = "Invalid TOTP code",
    cause: Throwable? = null
) : ZnVaultException(message, 401, "INVALID_TOTP", cause)
