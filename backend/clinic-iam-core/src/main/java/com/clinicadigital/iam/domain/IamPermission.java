package com.clinicadigital.iam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * T102 [US6] IAM Permission entity.
 *
 * Maps to {@code iam_permissions} table (after V203 schema rename).
 * Each permission is global (no tenant scope): code must be globally unique.
 *
 * Refs: FR-006, data-model.md, V203 migration
 */
@Entity
@Table(name = "iam_permissions")
public class IamPermission {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", nullable = false, length = 120, unique = true)
    private String code;

    @Column(name = "resource", nullable = false, length = 120)
    private String resource;

    @Column(name = "action", nullable = false, length = 60)
    private String action;

    @Column(name = "description")
    private String description;

    protected IamPermission() {
        // JPA constructor
    }

    public IamPermission(UUID id, String code, String resource, String action, String description) {
        this.id = id;
        this.code = code;
        this.resource = resource;
        this.action = action;
        this.description = description;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getResource() { return resource; }
    public String getAction() { return action; }
    public String getDescription() { return description; }
}
