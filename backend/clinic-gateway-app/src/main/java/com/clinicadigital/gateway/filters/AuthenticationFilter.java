package com.clinicadigital.gateway.filters;

import com.clinicadigital.gateway.exception.AuthSessionException;
import com.clinicadigital.shared.api.TenantContext;
import com.clinicadigital.shared.api.TenantContextStore;
import com.clinicadigital.iam.application.SessionManager;
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
    private final HandlerExceptionResolver handlerExceptionResolver;

    public AuthenticationFilter(SessionManager sessionManager,
                                @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        this.sessionManager = sessionManager;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (!uri.startsWith("/auth")) {
            return true;
        }
        return "POST".equalsIgnoreCase(request.getMethod()) && "/auth/login".equals(uri);
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

            UUID sessionId = resolveSessionId(request.getHeader(AUTHORIZATION_HEADER));
            request.setAttribute(REQUEST_SESSION_ID_ATTR, sessionId);

            // T106: enforce request.session_id presence at the boundary before controller logic.
            if (request.getAttribute(REQUEST_SESSION_ID_ATTR) == null) {
                throw new AuthSessionException("request.session_id must not be null");
            }

            boolean valid = sessionManager.validateSession(sessionId, tenantContext.tenantId());
            if (!valid) {
                throw new AuthSessionException("invalid or revoked session");
            }

            filterChain.doFilter(request, response);
        } catch (AuthSessionException ex) {
            handlerExceptionResolver.resolveException(request, response, null, ex);
        }
    }

    private UUID resolveSessionId(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            throw new AuthSessionException("Authorization Bearer session id is required");
        }
        if (!authorization.startsWith(BEARER_PREFIX)) {
            throw new AuthSessionException("Authorization must use Bearer scheme");
        }

        String rawToken = authorization.substring(BEARER_PREFIX.length()).trim();
        if (rawToken.isBlank()) {
            throw new AuthSessionException("request.session_id must not be null");
        }
        try {
            return UUID.fromString(rawToken);
        } catch (IllegalArgumentException ex) {
            throw new AuthSessionException("Authorization session id must be a UUID");
        }
    }
}