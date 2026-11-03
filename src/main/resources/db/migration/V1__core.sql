CREATE TABLE tenants (
    id VARCHAR(63) PRIMARY KEY,
    name VARCHAR(200) NOT NULL
);

CREATE TABLE memberships (
    tenant_id VARCHAR(63) NOT NULL REFERENCES tenants(id),
    subject VARCHAR(255) NOT NULL,
    PRIMARY KEY (tenant_id, subject)
);

CREATE TABLE projects (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(63) NOT NULL REFERENCES tenants(id),
    name VARCHAR(200) NOT NULL,
    UNIQUE (tenant_id, id)
);

CREATE TABLE issues (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(63) NOT NULL REFERENCES tenants(id),
    project_id UUID NOT NULL,
    title VARCHAR(300) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE')),
    CONSTRAINT fk_issues_tenant_project FOREIGN KEY (tenant_id, project_id)
        REFERENCES projects(tenant_id, id)
);

CREATE INDEX idx_issues_tenant_project ON issues(tenant_id, project_id);
