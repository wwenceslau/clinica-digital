package com.clinicadigital.gateway.filters;

import com.clinicadigital.gateway.exception.AuthSessionException;
import com.clinicadigital.gateway.security.SessionCookieService;
import com.clinicadigital.shared.api.TenantContext;
import com.clinicadigital.shared.api.TenantContextStore;
import com.clinicadigital.iam.application.SessionManager;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class AuthenticationFilter extends OncePerRequestFilter {

    public static final String REQUEST_SESSION_ID_ATTR = "request.session_id";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final SessionManager sessionManager;
    private final SessionCookieService sessionCookieService;
    private final HandlerExceptionResolver handlerExceptionResolver;

    public AuthenticationFilter(SessionManager sessionManager,
                                SessionCookieService sessionCookieService,
                                @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        this.sessionManager = sessionManager;
        this.sessionCookieService = sessionCookieService;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    /**
     * T079 [US5] — Applies session validation to all protected API routes.
     *
     * <p>Skipped for:
     * <ul>
     *   <li>{@code /api/auth/**} — unauthenticated login/logout endpoints</li>
     *   <li>{@code /api/public/**} — public clinic registration</li>
     *   <li>Everything that is NOT under {@code /api/**}</li>
     * </ul>
     *
     * Refs: FR-007
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // Only apply to /api/** routes
        if (!uri.startsWith("/api/")) return true;
        // Skip unauthenticated auth endpoints
        if (uri.startsWith("/api/auth/")) return true;
        // Skip public endpoints (clinic registration, etc.)
        if (uri.startsWith("/api/public/")) return true;
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            TenantContext tenantContext = TenantContextStore.get();
            if (tenantContext == null) {
                throw new AuthSessionException("tenant context missing for authenticated operation");
            }

            UUID sessionId = resolveSessionId(request);
            request.setAttribute(REQUEST_SESSION_ID_ATTR, sessionId);

            // T106: enforce request.session_id presence at the boundary before controller logic.
            if (request.getAttribute(REQUEST_SESSION_ID_ATTR) == null) {
                throw new AuthSessionException("request.session_id must not be null");
            }

            boolean valid = sessionManager.validateSession(sessionId, superUserTenantScope(tenantContext.tenantId()));
            if (!valid) {
                throw new AuthSessionException("invalid or revoked session");
            }

            filterChain.doFilter(request, response);
        } catch (AuthSessionException ex) {
            handlerExceptionResolver.resolveException(request, response, null, ex);
        }
    }

    private UUID resolveSessionId(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization != null && !authorization.isBlank()) {
            if (!authorization.startsWith(BEARER_PREFIX)) {
                throw new AuthSessionException("Authorization must use Bearer scheme");
            }
            return parseSessionId(authorization.substring(BEARER_PREFIX.length()).trim(), "Authorization");
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (sessionCookieService.cookieName().equals(cookie.getName())) {
                    return parseSessionId(cookie.getValue(), "Cookie");
                }
            }
        }

        throw new AuthSessionException("Authorization Bearer session id or session cookie is required");
    }

    private UUID parseSessionId(String authorizationToken, String scheme) {
        if (authorizationToken == null || authorizationToken.isBlank()) {
            throw new AuthSessionException("request.session_id must not be null");
        }
        try {
            return UUID.fromString(authorizationToken);
        } catch (IllegalArgumentException ex) {
            throw new AuthSessionException(scheme + " session id must be a UUID");
        }
    }

    /**
     * Super-user sessions are stored with {@code tenantId = null} in the database.
     * When the request operates under the system tenant context (SYSTEM_TENANT_ID),
     * we must pass {@code null} to {@link SessionManager#validateSession} so that
     * the tenant-match check correctly recognises a super-user session.
     */
    private static UUID superUserTenantScope(UUID tenantId) {
        return TenantContextFilter.SYSTEM_TENANT_ID.equals(tenantId) ? null : tenantId;
    }
}