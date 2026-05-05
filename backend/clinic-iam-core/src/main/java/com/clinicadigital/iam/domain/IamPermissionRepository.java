package com.clinicadigital.iam.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * T102 [US6] Repository for {@link IamPermission}.
 *
 * Refs: FR-006
 */
public interface IamPermissionRepository {

    IamPermission save(IamPermission permission);

    Optional<IamPermission> findById(UUID id);

    Optional<IamPermission> findByCode(String code);

    List<IamPermission> findAll();
}
