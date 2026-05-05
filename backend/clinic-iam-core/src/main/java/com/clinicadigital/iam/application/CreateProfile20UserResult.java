package com.clinicadigital.iam.application;

import java.util.UUID;

/**
 * Value object returned by {@link CreateProfile20UserService#create}.
 *
 * Refs: FR-006, FR-022
 *
 * @param userId              UUID of the newly created IamUser
 * @param practitionerId      UUID of the newly created Practitioner record
 * @param practitionerRoleId  UUID of the newly created PractitionerRole record
 * @param auditEventId        ID of the persisted IamAuditEvent (type=PROFILE20_USER_CREATED)
 */
public record CreateProfile20UserResult(
        UUID userId,
        UUID practitionerId,
        UUID practitionerRoleId,
        UUID auditEventId) {
}
