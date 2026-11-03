package com.tmejs.tenantmicro.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tenants")
public class TenantAdminController {
    private final TenantCatalog catalog;

    TenantAdminController(TenantCatalog catalog) { this.catalog = catalog; }

    @PostMapping
    ResponseEntity<TenantResponse> create(@Valid @RequestBody CreateTenantRequest request) {
        Tenant tenant = catalog.createTenant(request.id(), request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(new TenantResponse(tenant.getId(), tenant.getName()));
    }

    @GetMapping
    List<TenantResponse> list() {
        return catalog.listTenants().stream().map(t -> new TenantResponse(t.getId(), t.getName())).toList();
    }

    @PutMapping("/{tenantId}/members/{subject}")
    ResponseEntity<Void> grant(@PathVariable String tenantId, @PathVariable String subject) {
        validate(tenantId, subject);
        catalog.grant(tenantId, subject);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{tenantId}/members/{subject}")
    ResponseEntity<Void> revoke(@PathVariable String tenantId, @PathVariable String subject) {
        validate(tenantId, subject);
        catalog.revoke(tenantId, subject);
        return ResponseEntity.noContent().build();
    }

    private static void validate(String tenantId, String subject) {
        if (!tenantId.matches("[a-z][a-z0-9-]{2,62}") || tenantId.equals("__platform__") || subject.isBlank()) {
            throw new IllegalArgumentException("Invalid tenant ID or subject");
        }
    }

    record CreateTenantRequest(@NotBlank @Pattern(regexp = "[a-z][a-z0-9-]{2,62}") String id,
                               @NotBlank @Size(max = 200) String name) { }
    record TenantResponse(String id, String name) { }
}
