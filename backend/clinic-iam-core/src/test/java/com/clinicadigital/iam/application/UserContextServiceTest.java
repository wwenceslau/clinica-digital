package com.clinicadigital.iam.application;

import com.clinicadigital.iam.domain.IamSession;
import com.clinicadigital.iam.domain.IamSessionRepository;
import com.clinicadigital.iam.domain.IamUser;
import com.clinicadigital.iam.domain.IamUserRepository;
import com.clinicadigital.iam.domain.LocationRepository;
import com.clinicadigital.iam.domain.Organization;
import com.clinicadigital.iam.domain.OrganizationRepository;
import com.clinicadigital.iam.domain.PractitionerRepository;
import com.clinicadigital.iam.domain.PractitionerRoleRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserContextServiceTest {

        static {
                System.setProperty("net.bytebuddy.experimental", "true");
        }

    @Test
    void resolveContextShouldAllowSuperUserWhenTenantScopedLookupMisses() {
        IamSessionRepository sessionRepository = mock(IamSessionRepository.class);
        IamUserRepository userRepository = mock(IamUserRepository.class);
        OrganizationRepository organizationRepository = mock(OrganizationRepository.class);
        PractitionerRepository practitionerRepository = mock(PractitionerRepository.class);
        PractitionerRoleRepository practitionerRoleRepository = mock(PractitionerRoleRepository.class);
        LocationRepository locationRepository = mock(LocationRepository.class);

        UserContextService service = new UserContextService(
                sessionRepository,
                userRepository,
                organizationRepository,
                practitionerRepository,
                practitionerRoleRepository,
                locationRepository);

        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        IamSession superUserSession = new IamSession(
                UUID.randomUUID(),
                null,
                null,
                userId,
                Instant.now(),
                Instant.now().plusSeconds(1800),
                null,
                "trace-test",
                sha256Hex(sessionId));

        IamUser superUser = new IamUser(
                userId,
                null,
                "superuser@test.local",
                "superuser@test.local",
                "hash",
                "bcrypt",
                true,
                0,
                null);

        Organization org = new Organization(
                tenantId,
                tenantId,
                "1234567",
                "Tenant Test",
                "org-" + tenantId,
                "[]",
                "[]",
                "Tenant Test");

        when(sessionRepository.findByOpaqueTokenDigest(eq(sha256Hex(sessionId)))).thenReturn(Optional.of(superUserSession));
        when(userRepository.findByIdAndTenantId(eq(userId), eq(tenantId))).thenReturn(Optional.empty());
        when(userRepository.findById(eq(userId))).thenReturn(Optional.of(superUser));
        when(organizationRepository.findById(eq(tenantId))).thenReturn(Optional.of(org));

        UserContextService.UserContextResult result = service.resolveContext(sessionId, tenantId);

        assertEquals(userId, result.userId());
        assertEquals(0, result.profileType());
        assertEquals(tenantId, result.tenantId());
    }

    @Test
    void resolveContextShouldAllowLegacyNullTenantUserWithTenantScopedSession() {
        IamSessionRepository sessionRepository = mock(IamSessionRepository.class);
        IamUserRepository userRepository = mock(IamUserRepository.class);
        OrganizationRepository organizationRepository = mock(OrganizationRepository.class);
        PractitionerRepository practitionerRepository = mock(PractitionerRepository.class);
        PractitionerRoleRepository practitionerRoleRepository = mock(PractitionerRoleRepository.class);
        LocationRepository locationRepository = mock(LocationRepository.class);

        UserContextService service = new UserContextService(
                sessionRepository,
                userRepository,
                organizationRepository,
                practitionerRepository,
                practitionerRoleRepository,
                locationRepository);

        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        IamSession tenantSession = new IamSession(
                UUID.randomUUID(),
                tenantId,
                tenantId,
                userId,
                Instant.now(),
                Instant.now().plusSeconds(1800),
                null,
                "trace-test",
                sha256Hex(sessionId));

        IamUser legacyAdmin = new IamUser(
                userId,
                null,
                "admin@test.local",
                "admin@test.local",
                "hash",
                "bcrypt",
                true,
                10,
                null);

        Organization org = new Organization(
                tenantId,
                tenantId,
                "7654321",
                "Tenant Legacy",
                "org-" + tenantId,
                "[]",
                "[]",
                "Tenant Legacy");

        when(sessionRepository.findByOpaqueTokenDigest(eq(sha256Hex(sessionId)))).thenReturn(Optional.of(tenantSession));
        when(userRepository.findByIdAndTenantId(eq(userId), eq(tenantId))).thenReturn(Optional.empty());
        when(userRepository.findById(eq(userId))).thenReturn(Optional.of(legacyAdmin));
        when(organizationRepository.findById(eq(tenantId))).thenReturn(Optional.of(org));

        UserContextService.UserContextResult result = service.resolveContext(sessionId, tenantId);

                assertEquals(userId, result.userId());
                assertEquals(10, result.profileType());
                assertEquals(tenantId, result.tenantId());
    }

        private static String sha256Hex(UUID token) {
                try {
                        byte[] hash = MessageDigest.getInstance("SHA-256")
                                        .digest(token.toString().getBytes(StandardCharsets.UTF_8));
                        StringBuilder sb = new StringBuilder(hash.length * 2);
                        for (byte b : hash) {
                                sb.append(String.format("%02x", b));
                        }
                        return sb.toString();
                } catch (NoSuchAlgorithmException e) {
                        throw new IllegalStateException("SHA-256 not available", e);
                }
        }
}
