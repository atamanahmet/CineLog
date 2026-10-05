package com.atamanahmet.cinelog.exception;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.TypeMismatchException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.atamanahmet.cinelog.client.RecommendationUnavailableException;
import com.atamanahmet.cinelog.client.tmdb.TmdbClientException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

/**
 * Shared API exception mapping. Catch-all lives here at lowest precedence.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
@Slf4j
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Maps bean validation failures to 400 with per-field error messages.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.badRequest()
                .body(validationProblemBody(ex.getBindingResult().getFieldErrors(),
                        ex.getBindingResult().getGlobalErrors()));
    }

    /**
     * Query-object bind and validation failures, including type mismatches on record fields.
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ProblemDetail> handleBindException(BindException ex) {
        return ResponseEntity.badRequest()
                .body(validationProblemBody(ex.getFieldErrors(), ex.getGlobalErrors()));
    }

    /**
     * Method-parameter validation failures from Spring 6.1+.
     */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation failed");
        Map<String, List<String>> errors = new LinkedHashMap<>();
        for (ParameterValidationResult result : ex.getParameterValidationResults()) {
            String name = result.getMethodParameter().getParameterName();
            if (name == null) {
                name = "parameter";
            }
            for (var resolvable : result.getResolvableErrors()) {
                errors.computeIfAbsent(name, field -> new ArrayList<>()).add(resolvable.getDefaultMessage());
            }
        }
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    /**
     * Method validation from @Validated on a controller.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation failed");
        Map<String, List<String>> errors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String path = violation.getPropertyPath().toString();
            String field = path;
            int dot = path.lastIndexOf('.');
            if (dot >= 0 && dot < path.length() - 1) {
                field = path.substring(dot + 1);
            }
            errors.computeIfAbsent(field, name -> new ArrayList<>()).add(violation.getMessage());
        }
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    /**
     * Non-numeric path id on typed Integer path variables.
     */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        if (!(ex instanceof MethodArgumentTypeMismatchException mismatch)) {
            return super.handleTypeMismatch(ex, headers, status, request);
        }
        HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();
        String uri = servletRequest.getRequestURI();
        if (mismatch.getRequiredType() != Integer.class) {
            log.warn("Invalid request parameter name={} value={} uri={}", mismatch.getName(), mismatch.getValue(),
                    uri, mismatch);
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid request parameter: " + mismatch.getName()));
        }
        String message = invalidIdMessage(uri, mismatch.getValue());
        log.warn("{} uri={}", message, uri, mismatch);
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    /**
     * Recommendation engine unreachable, timed out, or non-2xx.
     */
    @ExceptionHandler(RecommendationUnavailableException.class)
    public ResponseEntity<ProblemDetail> handleRecommendationUnavailable(RecommendationUnavailableException ex) {
        log.error("Recommendation engine unavailable", ex);
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
        problem.setTitle("Bad Gateway");
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(problem);
    }

    /**
     * Profile photo storage unavailable or provider failure. Generic 503; no stack in the body.
     */
    @ExceptionHandler(ProfilePhotoStorageException.class)
    public ResponseEntity<ProblemDetail> handleProfilePhotoStorage(ProfilePhotoStorageException ex) {
        if (!ex.isStorageDisabled()) {
            log.error("Profile photo storage failed", ex);
        }
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        problem.setTitle("Service Unavailable");
        problem.setDetail("Profile photo storage is unavailable");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }

    /**
     * Auth-context failure: missing principal or current-user row gone.
     */
    @ExceptionHandler({ UnauthorizedActionException.class, UserNotFoundException.class })
    public ResponseEntity<ProblemDetail> handleAuthContextFailure(RuntimeException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setTitle("Unauthorized");
        problem.setDetail(ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    /**
     * Wrong current password on an account change. 403 so the client does not treat it as an expired session.
     */
    @ExceptionHandler(InvalidCurrentPasswordException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCurrentPassword(InvalidCurrentPasswordException ex) {
        return problem(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage());
    }

    /**
     * Requested email belongs to another account.
     */
    @ExceptionHandler(EmailAlreadyInUseException.class)
    public ResponseEntity<ProblemDetail> handleEmailAlreadyInUse(EmailAlreadyInUseException ex) {
        return problem(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    /**
     * New password equals the current one.
     */
    @ExceptionHandler(PasswordUnchangedException.class)
    public ResponseEntity<ProblemDetail> handlePasswordUnchanged(PasswordUnchangedException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    /**
     * Upstream TMDB failure. Missing title is 404. Every other case is 502.
     */
    @ExceptionHandler(TmdbClientException.class)
    public ResponseEntity<?> handleTmdbClient(TmdbClientException ex) {
        if (ex.getUpstreamStatus() == 404) {
            ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
            problem.setTitle("Not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
        }
        log.error("TMDB request failed, mediaType={}, context={}", ex.getMediaType(), ex.getRequestContext(),
                ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message", ex.getMessage()));
    }

    /**
     * Unhandled failures. Logs the stack and returns a generic 500 body.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnhandled(Exception ex) {
        log.error("Unhandled exception", ex);
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Internal Server Error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(title);
        problem.setDetail(detail);
        return ResponseEntity.status(status).body(problem);
    }

    private static ProblemDetail validationProblemBody(List<FieldError> fieldErrors,
            List<ObjectError> globalErrors) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation failed");
        Map<String, List<String>> errors = new LinkedHashMap<>();
        for (FieldError fieldError : fieldErrors) {
            errors.computeIfAbsent(fieldError.getField(), field -> new ArrayList<>())
                    .add(fieldError.getDefaultMessage());
        }
        for (ObjectError objectError : globalErrors) {
            errors.computeIfAbsent(objectError.getObjectName(), field -> new ArrayList<>())
                    .add(objectError.getDefaultMessage());
        }
        problem.setProperty("errors", errors);
        return problem;
    }

    private static String invalidIdMessage(String uri, Object value) {
        if (uri != null && uri.contains("/movie/")) {
            return "Invalid movie id: " + value;
        }
        if (uri != null && uri.contains("/tv/")) {
            return "Invalid TV id: " + value;
        }
        return "Invalid media id: " + value;
    }
}
