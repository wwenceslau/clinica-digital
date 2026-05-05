package com.clinicadigital.iam.application;

import com.clinicadigital.iam.domain.IamSessionRepository;
import com.clinicadigital.iam.domain.IamUserRepository;
import com.clinicadigital.iam.domain.LocationRepository;
import com.clinicadigital.iam.domain.Location;
import com.clinicadigital.iam.domain.Organization;
import com.clinicadigital.iam.domain.OrganizationRepository;
import com.clinicadigital.iam.domain.Practitioner;
import com.clinicadigital.iam.domain.PractitionerRepository;
import com.clinicadigital.iam.domain.PractitionerRole;
import com.clinicadigital.iam.domain.PractitionerRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Application service for resolving and updating the authenticated user context.
 *
 * <p>Resolves the complete context (org, location, practitioner, role) from a session.
 * Supports selecting an active location, which updates the session's
 * {@code active_practitioner_role_id}.
 *
 * Refs: FR-008, FR-018, FR-019, US11
 */
@Service
@Transactional
public class UserContextService {

    /**
     * Thrown when the practitioner has no active role for the requested location,
     * or when the user has no practitioner profile at all.
     * Controllers map this to HTTP 403.
     */
    public static class NoActivePractitionerRoleException extends RuntimeException {
        public NoActivePractitionerRoleException(String message) {
            super(message);
        }
    }

    /**
     * The complete resolved context for an authenticated session.
     *
     * <p>Fields {@code locationId}, {@code locationName}, {@code practitionerRoleId},
     * and {@code roleCode} may be {@code null} if the user has no active role.
     */
    public record UserContextResult(
            UUID tenantId,
            UUID organizationId,
            String organizationName,
            UUID locationId,
            String locationName,
            UUID practitionerId,
            String practitionerName,
            int profileType,
            UUID practitionerRoleId,
            String roleCode,
            UUID userId
    ) {}

    private final IamSessionRepository sessionRepository;
    private final IamUserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PractitionerRepository practitionerRepository;
    private final PractitionerRoleRepository practitionerRoleRepository;
    private final LocationRepository locationRepository;

    public UserContextService(
            IamSessionRepository sessionRepository,
            IamUserRepository userRepository,
            OrganizationRepository organizationRepository,
            PractitionerRepository practitionerRepository,
            PractitionerRoleRepository practitionerRoleRepository,
            LocationRepository locationRepository) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.practitionerRepository = practitionerRepository;
        this.practitionerRoleRepository = practitionerRoleRepository;
        this.locationRepository = locationRepository;
    }

    /**
     * Resolves the full user context for the given session.
     *
     * <p>If the session has an {@code active_practitioner_role_id} set, that role
     * is used to determine the active location. Otherwise, the primary role (or first
     * active role) for the practitioner in the tenant is auto-selected.
     *
     * @param sessionId session UUID (from {@code cd_session} cookie or Bearer token)
     * @param tenantId  tenant UUID (from {@code X-Tenant-ID} header)
     * @return resolved context
     * @throws IllegalArgumentException if session or user are not found
     */
    @Transactional(readOnly = true)
    public UserContextResult resolveContext(UUID sessionId, UUID tenantId) {
        var session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        var user = userRepository.findByIdAndTenantId(session.userId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("User not found in tenant: " + session.userId()));

        Organization org = organizationRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + tenantId));

        UUID practitionerId = user.getPractitionerId();
        String practitionerName = null;
        if (practitionerId != null) {
            practitionerName = practitionerRepository.findById(practitionerId)
                    .map(Practitioner::getDisplayName)
                    .orElse(null);
        }

        // Resolve active role and location
        UUID activeRoleId = session.activePractitionerRoleId();
        UUID locationId = null;
        String locationName = null;
        String roleCode = null;
        UUID resolvedRoleId = null;

        if (activeRoleId != null) {
            var roleOpt = practitionerRoleRepository.findById(activeRoleId);
            if (roleOpt.isPresent() && roleOpt.get().isActive()) {
                PractitionerRole role = roleOpt.get();
                resolvedRoleId = role.getId();
                roleCode = role.getRoleCode();
                var locOpt = locationRepository.findById(role.getLocationId());
                if (locOpt.isPresent()) {
                    locationId = locOpt.get().getId();
                    locationName = locOpt.get().getDisplayName();
                }
            }
        } else if (practitionerId != null) {
            // Auto-select: prefer primary role, else first active role in tenant
            List<PractitionerRole> roles =
                    practitionerRoleRepository.findActiveByPractitionerIdAndTenantId(practitionerId, tenantId);
            if (!roles.isEmpty()) {
                PractitionerRole role = roles.stream()
                        .filter(PractitionerRole::isPrimaryRole)
                        .findFirst()
                        .orElse(roles.get(0));
                resolvedRoleId = role.getId();
                roleCode = role.getRoleCode();
                var locOpt = locationRepository.findById(role.getLocationId());
                if (locOpt.isPresent()) {
                    locationId = locOpt.get().getId();
                    locationName = locOpt.get().getDisplayName();
                }
            }
        }

        return new UserContextResult(
                tenantId,
                org.getId(),
                org.getDisplayName(),
                locationId,
                locationName,
                practitionerId,
                practitionerName,
                user.getProfile(),
                resolvedRoleId,
                roleCode,
                user.getId()
        );
    }

    /**
     * Selects the active location for the session.
     *
     * <p>Validates that the practitioner has an active {@link PractitionerRole} for
     * the requested location within the tenant. If valid, updates the session's
     * {@code active_practitioner_role_id} and returns the refreshed context.
     *
     * @param sessionId  session UUID
     * @param tenantId   tenant UUID
     * @param locationId location UUID to set as active
     * @return updated context after location selection
     * @throws NoActivePractitionerRoleException if no active role exists for the location
     */
    public UserContextResult setActiveLocation(UUID sessionId, UUID tenantId, UUID locationId) {
        var session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        var user = userRepository.findByIdAndTenantId(session.userId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("User not found in tenant: " + session.userId()));

        UUID practitionerId = user.getPractitionerId();
        if (practitionerId == null) {
            throw new NoActivePractitionerRoleException("User has no practitioner profile");
        }

        // Find an active role that links this practitioner to the requested location within the tenant
        List<PractitionerRole> roles =
                practitionerRoleRepository.findActiveByPractitionerIdAndTenantId(practitionerId, tenantId);

        PractitionerRole matchingRole = roles.stream()
                .filter(r -> locationId.equals(r.getLocationId()))
                .findFirst()
                .orElseThrow(() -> new NoActivePractitionerRoleException(
                        "No active practitioner role for location: " + locationId));

        sessionRepository.updateActivePractitionerRole(sessionId, matchingRole.getId());

        return resolveContext(sessionId, tenantId);
    }
}
