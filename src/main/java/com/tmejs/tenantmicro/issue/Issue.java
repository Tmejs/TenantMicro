package com.tmejs.tenantmicro.issue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "issues")
public class Issue {
    @Id
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, length = 63)
    private String tenantId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(nullable = false, length = 300)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IssueStatus status;

    protected Issue() { }

    public Issue(UUID projectId, String title) {
        this.id = UUID.randomUUID();
        this.projectId = projectId;
        this.title = title;
        this.status = IssueStatus.TODO;
    }

    public UUID getId() { return id; }
    public String getTenantId() { return tenantId; }
    public UUID getProjectId() { return projectId; }
    public String getTitle() { return title; }
    public IssueStatus getStatus() { return status; }
    public void setStatus(IssueStatus status) { this.status = status; }
}
