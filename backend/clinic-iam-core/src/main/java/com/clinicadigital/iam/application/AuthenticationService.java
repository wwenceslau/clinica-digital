package com.clinicadigital.iam.application;

import com.clinicadigital.iam.domain.IamSession;
import com.clinicadigital.iam.domain.IamUser;
import com.clinicadigital.iam.domain.IamUserRepository;
import com.clinicadigital.iam.domain.Organization;
import com.clinicadigital.iam.domain.OrganizationRepository;
import com.clinicadigital.iam.domain.PractitionerRole;
import com.clinicadigital.iam.domain.PractitionerRoleRepository;
import com.clinicadigital.shared.api.TenantJdbcContextInterceptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuthenticationService {

    private final IamUserRepository userRepository;
    private final PasswordService passwordService;
    private final SessionManager sessionManager;
    private final AuditService auditService;
    private final OrganizationRepository organizationRepository;
    private final PractitionerRoleRepository practitionerRoleRepository;
    private final AuthChallengeService authChallengeService;
    private final TenantJdbcContextInterceptor tenantJdbcContextInterceptor;

    public AuthenticationService(IamUserRepository userRepository,
                                 PasswordService passwordService,
                                 SessionManager sessionManager,
                                 AuditService auditService,
                                 OrganizationRepository organizationRepository,
                                 PractitionerRoleRepository practitionerRoleRepository,
                                 AuthChallengeService authChallengeService,
                                 TenantJdbcContextInterceptor tenantJdbcContextInterceptor) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.sessionManager = sessionManager;
        this.auditService = auditService;
        this.organizationRepository = organizationRepository;
        this.practitionerRoleRepository = practitionerRoleRepository;
        this.authChallengeService = authChallengeService;
        this.tenantJdbcContextInterceptor = tenantJdbcContextInterceptor;
    }

    /**
     * Legacy login — kept for backward-compatible internal use only.
     * Prefer {@link #loginByEmail} for US4 multi-org flow.
     */
    @Transactional
    public LoginResult login(UUID tenantId,
                             String email,
                             String password,
                             String traceId,
                             String clientIp,
                             String userAgent) {
        if (tenantId == null || email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("tenantId, email and password are required");
        }

        IamUser user = userRepository.findByEmailAndTenantId(email, tenantId)
                .orElseThrow(() -> {
                    auditService.logAuthEvent(tenantId, null, "auth.login", "failure", traceId,
                            metadata(clientIp, userAgent, "user_not_found"));
                    return new IllegalArgumentException("invalid credentials");
                });

        if (!user.isActive() || !passwordService.verifyPassword(password, user.getPasswordHash())) {
            auditService.logAuthEvent(tenantId, user.getId(), "auth.login", "failure", traceId,
                    metadata(clientIp, userAgent, "invalid_password_or_inactive_user"));
            throw new IllegalArgumentException("invalid credentials");
        }

        IamSession session = sessionManager.createSession(user.getId(), tenantId, traceId);
        auditService.logAuthEvent(tenantId, user.getId(), "auth.login", "success", traceId,
                metadata(clientIp, userAgent, "session_created"));

        return new LoginResult(session.id(), session.expiresAt(), tenantId, user.getId(), traceId);
    }

    /**
     * US4: Multi-org login — email/password only, no tenant context required.
     *
     * Returns either:
     *  - SingleOrg   when user has exactly 1 active org → session emitted immediately
     *  - MultipleOrgs when user has 2+ active orgs     → challenge token returned
     *
     * @throws InvalidCredentialsException if credentials are invalid or user has no active orgs
     */
    @Transactional
    public MultiOrgLoginResult loginByEmail(String email,
                                            String password,
                                            String traceId,
                                            String clientIp,
                                            String userAgent) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("email and password are required");
        }

        // Enable cross-tenant RLS policies within this transaction (US4, V204)
        tenantJdbcContextInterceptor.applyLoginContext();

        List<IamUser> candidates = userRepository.findByEmail(email);

        IamUser authenticated = null;
        for (IamUser candidate : candidates) {
            if (candidate.isActive() && passwordService.verifyPassword(password, candidate.getPasswordHash())) {
                authenticated = candidate;
                break;
            }
        }

        if (authenticated == null) {
            // No tenant context available for global invalid-credentials events;
            // rate-limiting is handled by LoginLockoutService in the controller.
            throw new InvalidCredentialsException("invalid credentials");
        }

        final IamUser user = authenticated;

        // Super-user (profile 0) gets direct session with null tenant
        if (user.getProfile() == 0) {
            IamSession session = sessionManager.createSession(user.getId(), null, traceId);
            // Audit deferred: super-user sessions have no tenant context;
            // full audit will be emitted once crosslogin infrastructure is in place (V204).
            return new MultiOrgLoginResult.SingleOrg(
                    session.id(), session.expiresAt(), null, user.getId(), traceId);
        }

        // Tenant-scoped user: resolve active organizations via PractitionerRole
        if (user.getPractitionerId() == null) {
            auditService.logAuthEvent(user.getTenantId(), user.getId(), "auth.login", "failure", traceId,
                    metadata(clientIp, userAgent, "no_practitioner_record"));
            throw new InvalidCredentialsException("invalid credentials");
        }

        List<PractitionerRole> activeRoles =
                practitionerRoleRepository.findActiveByPractitionerId(user.getPractitionerId());

        List<Organization> activeOrgs = activeRoles.stream()
                .map(r -> organizationRepository.findById(r.getOrganizationId()).orElse(null))
                .filter(org -> org != null && org.isAccountActive())
                .distinct()
                .toList();

        if (activeOrgs.isEmpty()) {
            auditService.logAuthEvent(user.getTenantId(), user.getId(), "auth.login", "failure", traceId,
                    metadata(clientIp, userAgent, "no_active_organization"));
            throw new InvalidCredentialsException("invalid credentials");
        }

        if (activeOrgs.size() == 1) {
            Organization org = activeOrgs.get(0);
            IamSession session = sessionManager.createSession(user.getId(), org.getId(), traceId);
            auditService.logAuthEvent(org.getId(), user.getId(), "auth.login", "success", traceId,
                    metadata(clientIp, userAgent, "single_org_session"));
            return new MultiOrgLoginResult.SingleOrg(
                    session.id(), session.expiresAt(), org.getId(), user.getId(), traceId);
        }

        // Multiple orgs: issue challenge
        List<AuthChallengeService.OrganizationOption> options = activeOrgs.stream()
                .map(org -> new AuthChallengeService.OrganizationOption(
                        org.getId(), org.getDisplayName(), org.getCnes()))
                .toList();

        String challengeToken = authChallengeService.createChallenge(user.getId(), options);
        auditService.logAuthEvent(user.getTenantId(), user.getId(), "auth.login", "challenge_issued", traceId,
                metadata(clientIp, userAgent, "multiple_orgs"));

        return new MultiOrgLoginResult.MultipleOrgs(challengeToken, options);
    }

    /**
     * US4: Complete login after org selection via challenge token.
     */
    @Transactional
    public LoginResult selectOrganization(String challengeToken,
                                          UUID organizationId,
                                          String traceId) {
        // Enable cross-tenant RLS policies within this transaction (US4, V204)
        tenantJdbcContextInterceptor.applyLoginContext();

        AuthChallengeService.ResolvedChallenge resolved =
                authChallengeService.resolveChallenge(challengeToken, organizationId);

        IamSession session = sessionManager.createSession(resolved.iamUserId(), organizationId, traceId);
        auditService.logAuthEvent(organizationId, resolved.iamUserId(), "auth.select_org", "success", traceId,
                "{\"organization_id\":\"" + organizationId + "\"}");

        return new LoginResult(session.id(), session.expiresAt(), organizationId, resolved.iamUserId(), traceId);
    }

    @Transactional
    public void logout(UUID sessionId, UUID tenantId, UUID actorUserId, String traceId) {
        if (sessionId == null || tenantId == null) {
            throw new IllegalArgumentException("sessionId and tenantId are required");
        }
        sessionManager.revokeSession(sessionId, tenantId);
        auditService.logAuthEvent(tenantId, actorUserId, "auth.logout", "success", traceId,
                "{\"session_id\":\"" + sessionId + "\"}");
    }

    @Transactional(readOnly = true)
    public WhoAmIResult whoami(UUID sessionId, UUID tenantId, String traceId) {
        if (sessionId == null || tenantId == null) {
            throw new IllegalArgumentException("sessionId and tenantId are required");
        }
        IamSession session = sessionManager.validateSession(sessionId, tenantId)
                ? sessionManager.findRequiredSession(sessionId)
                : null;

        if (session == null) {
            throw new IllegalArgumentException("session is invalid");
        }

        IamUser user = userRepository.findByIdAndTenantId(session.userId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("user not found for active session"));

        return new WhoAmIResult(user.getId(), user.getEmail(), tenantId, List.of(), traceId);
    }

    private static String metadata(String clientIp, String userAgent, String reason) {
        return "{\"client_ip\":\"" + safe(clientIp) + "\",\"user_agent\":\"" + safe(userAgent)
                + "\",\"reason\":\"" + safe(reason) + "\"}";
    }

    private static String safe(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public record LoginResult(UUID sessionId, Instant expiresAt, UUID tenantId, UUID userId, String traceId) {}

    public record WhoAmIResult(UUID userId, String email, UUID tenantId, List<String> roles, String traceId) {}

    /** Sealed hierarchy for the US4 multi-org login result. */
    public sealed interface MultiOrgLoginResult
            permits MultiOrgLoginResult.SingleOrg, MultiOrgLoginResult.MultipleOrgs {

        record SingleOrg(UUID sessionId, Instant expiresAt, UUID organizationId,
                         UUID userId, String traceId) implements MultiOrgLoginResult {}

        record MultipleOrgs(String challengeToken,
                            List<AuthChallengeService.OrganizationOption> organizations)
                implements MultiOrgLoginResult {}
    }

    /** Thrown when credentials are invalid or user has no active organization access. */
    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException(String message) {
            super(message);
        }
    }
}
