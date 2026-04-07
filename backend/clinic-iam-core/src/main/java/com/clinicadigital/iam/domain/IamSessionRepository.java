package com.clinicadigital.iam.domain;

import java.util.Optional;
import java.util.UUID;

public interface IamSessionRepository {

    IamSession save(IamSession session);

    Optional<IamSession> findById(UUID sessionId);

    void revoke(UUID sessionId, UUID tenantId);
}
