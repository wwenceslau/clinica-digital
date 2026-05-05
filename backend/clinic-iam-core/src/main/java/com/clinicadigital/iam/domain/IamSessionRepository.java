package com.clinicadigital.iam.domain;

import java.util.Optional;
import java.util.UUID;

public interface IamSessionRepository {

    IamSession save(IamSession session);

    Optional<IamSession> findById(UUID sessionId);

    void revoke(UUID sessionId, UUID tenantId);

    /** Updates the active_practitioner_role_id for the given session. */
    void updateActivePractitionerRole(UUID sessionId, UUID practitionerRoleId);
}
