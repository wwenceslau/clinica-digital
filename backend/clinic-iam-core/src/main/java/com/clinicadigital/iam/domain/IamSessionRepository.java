package com.clinicadigital.iam.domain;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface IamSessionRepository {

    IamSession save(IamSession session);

    Optional<IamSession> findById(UUID sessionId);

    Optional<IamSession> findByOpaqueTokenDigest(String opaqueTokenDigest);

    List<IamSession> findByTenantIdOrderByIssuedAtDesc(UUID tenantId, int limit);

    void revoke(UUID sessionId, UUID tenantId);

    void revokeByOpaqueTokenDigest(String opaqueTokenDigest, UUID tenantId, String revocationReason);

    /** Updates the active_practitioner_role_id for the given session. */
    void updateActivePractitionerRole(UUID sessionId, UUID practitionerRoleId);
}
