package com.tmejs.tenantmicro.issue;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants/{tenantId}/projects/{projectId}")
public class IssueController {
    private final IssueService service;

    IssueController(IssueService service) { this.service = service; }

    @PostMapping("/issues")
    ResponseEntity<IssueResponse> create(@PathVariable UUID projectId, @Valid @RequestBody CreateIssueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(response(service.create(projectId, request.title())));
    }

    @GetMapping("/issues")
    List<IssueResponse> list(@PathVariable UUID projectId) {
        return service.list(projectId).stream().map(IssueController::response).toList();
    }

    @GetMapping("/issues/{issueId}")
    IssueResponse get(@PathVariable UUID projectId, @PathVariable UUID issueId) {
        return response(service.get(projectId, issueId));
    }

    @PatchMapping("/issues/{issueId}/status")
    IssueResponse changeStatus(@PathVariable UUID projectId, @PathVariable UUID issueId,
                               @Valid @RequestBody ChangeStatusRequest request) {
        return response(service.changeStatus(projectId, issueId, request.status()));
    }

    @GetMapping("/board")
    Map<IssueStatus, List<IssueResponse>> board(@PathVariable UUID projectId) {
        Map<IssueStatus, List<IssueResponse>> columns = new EnumMap<>(IssueStatus.class);
        for (IssueStatus status : IssueStatus.values()) columns.put(status, new java.util.ArrayList<>());
        for (Issue issue : service.board(projectId)) columns.get(issue.getStatus()).add(response(issue));
        return columns;
    }

    private static IssueResponse response(Issue issue) {
        return new IssueResponse(issue.getId(), issue.getProjectId(), issue.getTitle(), issue.getStatus());
    }

    record CreateIssueRequest(@NotBlank @Size(max = 300) String title) { }
    record ChangeStatusRequest(@NotNull IssueStatus status) { }
    record IssueResponse(UUID id, UUID projectId, String title, IssueStatus status) { }
}
