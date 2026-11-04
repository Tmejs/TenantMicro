package com.tmejs.tenantmicro.issue;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssueRepository extends JpaRepository<Issue, UUID> {
    List<Issue> findByProjectId(UUID projectId);
}
