package com.tmejs.tenantmicro.api;

import com.tmejs.tenantmicro.admin.TenantAlreadyExistsException;
import com.tmejs.tenantmicro.admin.TenantNotFoundException;
import com.tmejs.tenantmicro.project.ProjectNotFoundException;
import com.tmejs.tenantmicro.issue.IssueNotFoundException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(TenantAlreadyExistsException.class)
    ResponseEntity<Map<String, String>> duplicate() {
        return problem(HttpStatus.CONFLICT, "tenant_exists", "Tenant already exists");
    }

    @ExceptionHandler(TenantNotFoundException.class)
    ResponseEntity<Map<String, String>> missing() {
        return problem(HttpStatus.NOT_FOUND, "tenant_not_found", "Tenant not found");
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    ResponseEntity<Map<String, String>> projectMissing() {
        return problem(HttpStatus.NOT_FOUND, "project_not_found", "Project not found");
    }

    @ExceptionHandler(IssueNotFoundException.class)
    ResponseEntity<Map<String, String>> issueMissing() {
        return problem(HttpStatus.NOT_FOUND, "issue_not_found", "Issue not found");
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class, IllegalArgumentException.class})
    ResponseEntity<Map<String, String>> invalid() {
        return problem(HttpStatus.BAD_REQUEST, "invalid_request", "Invalid request");
    }

    private static ResponseEntity<Map<String, String>> problem(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(Map.of("code", code, "message", message));
    }
}
