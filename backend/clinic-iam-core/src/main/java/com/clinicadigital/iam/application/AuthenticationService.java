package com.clinicadigital.iam.application;

import com.clinicadigital.iam.domain.IamSession;
import com.clinicadigital.iam.domain.IamUser;
import com.clinicadigital.iam.domain.IamUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AuthenticationService {

    private final IamUserRepository userRepository;
    private final PasswordService passwordService;
    private final SessionManager sessionManager;
    private final AuditService auditService;

    public AuthenticationService(IamUserRepository userRepository,
                                 PasswordService passwordService,
                                 SessionManager sessionManager,
                                 AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.sessionManager = sessionManager;
        this.auditService = auditService;
    }

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
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public record LoginResult(UUID sessionId, java.time.Instant expiresAt, UUID tenantId, UUID userId, String traceId) {
    }

    public record WhoAmIResult(UUID userId, String email, UUID tenantId, List<String> roles, String traceId) {
    }
}
