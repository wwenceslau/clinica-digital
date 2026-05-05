package com.clinicadigital.gateway.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginLockoutService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final Duration LOCKOUT = Duration.ofMinutes(15);

    private final Map<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    public void assertNotLocked(String tenantScopedLogin) {
        AttemptWindow window = attempts.get(key(tenantScopedLogin));
        if (window == null) {
            return;
        }
        window.resetWindowIfElapsed();
        if (window.lockedUntil != null && window.lockedUntil.isAfter(Instant.now())) {
            throw new IllegalArgumentException("too many login attempts; locked for 15 minutes");
        }
    }

    public void registerFailure(String tenantScopedLogin) {
        AttemptWindow window = attempts.computeIfAbsent(key(tenantScopedLogin), ignored -> new AttemptWindow());
        window.resetWindowIfElapsed();
        window.failures++;
        if (window.failures >= MAX_ATTEMPTS) {
            window.lockedUntil = Instant.now().plus(LOCKOUT);
        }
    }

    public void registerSuccess(String tenantScopedLogin) {
        attempts.remove(key(tenantScopedLogin));
    }

    private static String key(String login) {
        if (login == null) {
            return "";
        }
        return login.trim().toLowerCase(Locale.ROOT);
    }

    private static final class AttemptWindow {
        private Instant windowStartedAt = Instant.now();
        private int failures;
        private Instant lockedUntil;

        private void resetWindowIfElapsed() {
            if (windowStartedAt.plus(WINDOW).isBefore(Instant.now())) {
                windowStartedAt = Instant.now();
                failures = 0;
                lockedUntil = null;
            }
        }
    }
}
