package com.clinicadigital.gateway.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class SessionCookieService {

    private final String cookieName;
    private final boolean secure;

    public SessionCookieService(
            @Value("${iam.session.cookie.name:cd_session}") String cookieName,
            @Value("${iam.session.cookie.secure:false}") boolean secure) {
        this.cookieName = cookieName;
        this.secure = secure;
    }

    public void writeSessionCookie(HttpServletResponse response, UUID sessionId, Duration ttl) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, sessionId.toString())
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(ttl)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public void clearSessionCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public String cookieName() {
        return cookieName;
    }
}
