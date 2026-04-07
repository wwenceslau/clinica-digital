package com.clinicadigital.iam.domain;

import java.util.Optional;
import java.util.UUID;

public interface IamUserRepository {

    Optional<IamUser> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<IamUser> findByEmailAndTenantId(String email, UUID tenantId);

    IamUser save(IamUser user);
}
