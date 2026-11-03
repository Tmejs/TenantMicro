package com.tmejs.tenantmicro.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "projects")
public class Project {
    @Id
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, length = 63)
    private String tenantId;

    @Column(nullable = false, length = 200)
    private String name;

    protected Project() { }

    public Project(String name) {
        this.id = UUID.randomUUID();
        this.name = name;
    }

    public UUID getId() { return id; }
    public String getTenantId() { return tenantId; }
    public String getName() { return name; }
}
