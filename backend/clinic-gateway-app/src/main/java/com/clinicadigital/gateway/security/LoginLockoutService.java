package com.clinicadigital.gateway.security;

import com.clinicadigital.iam.domain.IamUser;
import com.clinicadigital.iam.domain.IamUserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class LoginLockoutService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final Duration LOCKOUT = Duration.ofMinutes(15);

    private final IamUserRepository iamUserRepository;

    public LoginLockoutService(IamUserRepository iamUserRepository) {
        this.iamUserRepository = iamUserRepository;
    }

    public void assertNotLocked(String tenantScopedLogin) {
        Instant now = Instant.now();
        for (IamUser user : resolveUsers(tenantScopedLogin)) {
            resetWindowIfElapsed(user, now);
            if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
                throw new IllegalArgumentException("too many login attempts; locked for 15 minutes");
            }
        }
    }

    @Transactional
    public void registerFailure(String tenantScopedLogin) {
        Instant now = Instant.now();
        for (IamUser user : resolveUsers(tenantScopedLogin)) {
            resetWindowIfElapsed(user, now);
            user.registerLoginFailure(MAX_ATTEMPTS, now.plus(LOCKOUT));
            iamUserRepository.save(user);
        }
    }

    @Transactional
    public void registerSuccess(String tenantScopedLogin) {
        for (IamUser user : resolveUsers(tenantScopedLogin)) {
            user.clearLockoutState();
            iamUserRepository.save(user);
        }
    }

    private static String key(String login) {
        if (login == null) {
            return "";
        }
        return login.trim().toLowerCase(Locale.ROOT);
    }

    private List<IamUser> resolveUsers(String tenantScopedLogin) {
        String normalized = key(tenantScopedLogin);
        int separator = normalized.indexOf(':');
        if (separator > 0) {
            String tenantRaw = normalized.substring(0, separator);
            String email = normalized.substring(separator + 1);
            try {
                UUID tenantId = UUID.fromString(tenantRaw);
                Optional<IamUser> user = iamUserRepository.findByEmailAndTenantId(email, tenantId);
                return user.map(List::of).orElse(List.of());
            } catch (IllegalArgumentException ignored) {
                // Fall back to global email lookup when tenant prefix is malformed.
            }
        }
        return iamUserRepository.findByEmail(normalized);
    }

    private static void resetWindowIfElapsed(IamUser user, Instant now) {
        Instant reference = user.getUpdatedAt();
        if (reference != null && reference.plus(WINDOW).isBefore(now)) {
            user.clearLockoutState();
        }
    }
}
