package com.tmejs.tenantmicro.project;

import com.tmejs.tenantmicro.security.VerifiedTenantContext;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {
    private final ProjectRepository projects;

    ProjectService(ProjectRepository projects) { this.projects = projects; }

    @Transactional
    public Project create(String name) {
        VerifiedTenantContext.requireTenantId();
        return projects.save(new Project(name));
    }

    @Transactional(readOnly = true)
    public List<Project> list() {
        VerifiedTenantContext.requireTenantId();
        return projects.findAll();
    }

    @Transactional(readOnly = true)
    public Project get(UUID id) {
        VerifiedTenantContext.requireTenantId();
        return projects.findById(id).orElseThrow(ProjectNotFoundException::new);
    }
}
