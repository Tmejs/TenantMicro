package com.tmejs.tenantmicro.project;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants/{tenantId}/projects")
public class ProjectController {
    private final ProjectService service;

    ProjectController(ProjectService service) { this.service = service; }

    @PostMapping
    ResponseEntity<ProjectResponse> create(@Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(response(service.create(request.name())));
    }

    @GetMapping
    List<ProjectResponse> list() {
        return service.list().stream().map(ProjectController::response).toList();
    }

    @GetMapping("/{projectId}")
    ProjectResponse get(@PathVariable UUID projectId) {
        return response(service.get(projectId));
    }

    private static ProjectResponse response(Project project) {
        return new ProjectResponse(project.getId(), project.getName());
    }

    record CreateProjectRequest(@NotBlank @Size(max = 200) String name) { }
    record ProjectResponse(UUID id, String name) { }
}
