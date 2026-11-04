package com.tmejs.tenantmicro.issue;

import com.tmejs.tenantmicro.project.ProjectService;
import com.tmejs.tenantmicro.security.VerifiedTenantContext;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IssueService {
    private final IssueRepository issues;
    private final ProjectService projects;

    IssueService(IssueRepository issues, ProjectService projects) {
        this.issues = issues;
        this.projects = projects;
    }

    @Transactional
    public Issue create(UUID projectId, String title) {
        verifyProject(projectId);
        return issues.save(new Issue(projectId, title));
    }

    @Transactional(readOnly = true)
    public List<Issue> list(UUID projectId) {
        verifyProject(projectId);
        return issues.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public Issue get(UUID projectId, UUID issueId) {
        verifyProject(projectId);
        return findInProject(projectId, issueId);
    }

    @Transactional
    public Issue changeStatus(UUID projectId, UUID issueId, IssueStatus status) {
        verifyProject(projectId);
        Issue issue = findInProject(projectId, issueId);
        issue.setStatus(status);
        return issue;
    }

    @Transactional(readOnly = true)
    public List<Issue> board(UUID projectId) {
        return list(projectId);
    }

    private void verifyProject(UUID projectId) {
        VerifiedTenantContext.requireTenantId();
        projects.get(projectId);
    }

    private Issue findInProject(UUID projectId, UUID issueId) {
        Issue issue = issues.findById(issueId).orElseThrow(IssueNotFoundException::new);
        if (!issue.getProjectId().equals(projectId)) throw new IssueNotFoundException();
        return issue;
    }
}
