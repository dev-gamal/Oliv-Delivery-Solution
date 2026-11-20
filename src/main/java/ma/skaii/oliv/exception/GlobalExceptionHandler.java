package ma.skaii.oliv.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import ma.skaii.oliv.dto.error.ApiErrorResponse;
import ma.skaii.oliv.service.exception.BusinessRuleException;
import ma.skaii.oliv.service.exception.ForbiddenAccessException;
import ma.skaii.oliv.service.exception.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // =========================================================================
    // 1. EXCEPTIONS MÉTIER O'LIV
    // =========================================================================

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessRule(
            BusinessRuleException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "BUSINESS_RULE_VIOLATION", ex.getMessage(), request, null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), request, null);
    }

    @ExceptionHandler(ForbiddenAccessException.class)
    public ResponseEntity<ApiErrorResponse> handleForbiddenBusiness(
            ForbiddenAccessException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "FORBIDDEN_RESOURCE", ex.getMessage(), request, null);
    }

    // =========================================================================
    // 2. VALIDATION DES DONNÉES & REQUÊTES HTTP
    // =========================================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Certains champs de la requête sont invalides ou manquants.",
                request,
                fieldErrors
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> violations = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v ->
                violations.put(v.getPropertyPath().toString(), v.getMessage())
        );
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "CONSTRAINT_VIOLATION",
                "Les paramètres fournis ne respectent pas les contraintes attendues.",
                request,
                violations
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableJson(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "MALFORMED_JSON",
                "Corps de requête JSON invalide ou valeur d'énumération non reconnue.",
                request,
                null
        );
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ApiErrorResponse> handleParameterError(
            Exception ex, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_PARAMETER",
                "Paramètre de requête manquant ou de type invalide (ex. UUID attendu).",
                request,
                null
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSize(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "FILE_TOO_LARGE",
                "La taille du fichier envoyé dépasse la limite autorisée (10 Mo).",
                request,
                null
        );
    }

    // =========================================================================
    // 3. SÉCURITÉ & AUTHENTIFICATION (SPRING SECURITY)
    // =========================================================================

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "INVALID_CREDENTIALS",
                "Identifiant ou mot de passe incorrect.",
                request,
                null
        );
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiErrorResponse> handleLockedAccount(
            LockedException ex, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.FORBIDDEN,
                "ACCOUNT_SUSPENDED",
                "Votre compte est temporairement suspendu. Contactez le support O'liv.",
                request,
                null
        );
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiErrorResponse> handleDisabledAccount(
            DisabledException ex, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.FORBIDDEN,
                "ACCOUNT_DISABLED",
                "Votre compte est désactivé.",
                request,
                null
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(
            AuthenticationException ex, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED",
                "Authentification requise ou session expirée.",
                request,
                null
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.FORBIDDEN,
                "ACCESS_DENIED",
                "Vous ne disposez pas des droits nécessaires pour effectuer cette action.",
                request,
                null
        );
    }

    // =========================================================================
    // 4. INTÉGRITÉ BASE DE DONNÉES (MYSQL)
    // =========================================================================

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Conflit d'intégrité SQL sur {} : {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return buildResponse(
                HttpStatus.CONFLICT,
                "DATA_CONFLICT",
                "Conflit de données : un enregistrement identique existe déjà ou une contrainte métier n'est pas respectée.",
                request,
                null
        );
    }

    // =========================================================================
    // 5. FALLBACK GÉNÉRAL (500 - SANS FUITE DE DONNÉES SENSIBLES)
    // =========================================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception ex, HttpServletRequest request) {
        log.error("Erreur interne non gérée sur [{} {}]", request.getMethod(), request.getRequestURI(), ex);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "Une erreur interne est survenue. Veuillez réessayer ultérieurement.",
                request,
                null
        );
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            Map<String, String> validationErrors
    ) {
        ApiErrorResponse body = new ApiErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                request.getRequestURI(),
                validationErrors
        );
        return ResponseEntity.status(status).body(body);
    }
}