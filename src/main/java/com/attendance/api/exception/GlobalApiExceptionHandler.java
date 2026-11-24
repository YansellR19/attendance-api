package com.attendance.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.attendance.api.controller.api")
public class GlobalApiExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail onNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return createProblem(HttpStatus.NOT_FOUND, "No encontrado", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail onConflict(DuplicateResourceException ex, HttpServletRequest request) {
        return createProblem(HttpStatus.CONFLICT, "Registro duplicado", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail onRuleViolation(BusinessRuleException ex, HttpServletRequest request) {
        return createProblem(HttpStatus.UNPROCESSABLE_ENTITY, "Regla de negocio", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail onInvalidLogin(BadCredentialsException ex, HttpServletRequest request) {
        return createProblem(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas",
                "Correo o contrasena no validos.", request.getRequestURI());
    }

    @ExceptionHandler(DisabledException.class)
    public ProblemDetail onInactiveAccount(DisabledException ex, HttpServletRequest request) {
        return createProblem(HttpStatus.FORBIDDEN, "Usuario inactivo",
                "El empleado se encuentra desactivado.", request.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail onForbidden(AccessDeniedException ex, HttpServletRequest request) {
        return createProblem(HttpStatus.FORBIDDEN, "Permiso denegado",
                "No cuentas con rol suficiente para esta accion.", request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail onValidationFailure(MethodArgumentNotValidException ex, HttpServletRequest request) {
        ProblemDetail detail = createProblem(
                HttpStatus.BAD_REQUEST,
                "Datos invalidos",
                "Verifica los campos enviados en la solicitud.",
                request.getRequestURI()
        );
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(err -> fieldErrors.putIfAbsent(err.getField(), err.getDefaultMessage()));
        detail.setProperty("fieldErrors", fieldErrors);
        return detail;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail onParameterMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return createProblem(
                HttpStatus.BAD_REQUEST,
                "Formato de parametro invalido",
                "Revisa el valor enviado en el parametro: " + ex.getName(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail onFallback(Exception ex, HttpServletRequest request) {
        return createProblem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error inesperado",
                "No fue posible completar la operacion solicitada.",
                request.getRequestURI()
        );
    }

    private ProblemDetail createProblem(HttpStatus status, String title, String message, String uri) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message);
        problem.setTitle(title);
        problem.setInstance(URI.create(uri));
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }
}
