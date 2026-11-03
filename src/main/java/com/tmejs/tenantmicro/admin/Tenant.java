package com.tmejs.tenantmicro.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tenants")
public class Tenant {
    @Id
    private String id;

    @Column(nullable = false, length = 200)
    private String name;

    protected Tenant() { }

    public Tenant(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }
}
