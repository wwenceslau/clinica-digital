package com.clinicadigital.gateway.api;

import com.clinicadigital.gateway.filters.AuthenticationFilter;
import com.clinicadigital.gateway.filters.TenantContextFilter;
import com.clinicadigital.iam.application.CreateProfile20UserService;
import com.clinicadigital.iam.application.PiiCryptoService;
import com.clinicadigital.iam.application.SessionManager;
import com.clinicadigital.iam.application.UserContextService;
import com.clinicadigital.iam.domain.IamSession;
import com.clinicadigital.iam.domain.IamUser;
import com.clinicadigital.iam.domain.IamUserRepository;
import com.clinicadigital.iam.domain.PractitionerRepository;
import com.clinicadigital.iam.domain.PractitionerRoleRepository;
import com.clinicadigital.shared.api.TenantContext;
import com.clinicadigital.shared.api.TenantContextStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserControllerTest {

        static {
                System.setProperty("net.bytebuddy.experimental", "true");
        }

        private static final UUID SYSTEM_TENANT_ID =
                        UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final CreateProfile20UserService createProfile20UserService = mock(CreateProfile20UserService.class);
    private final SessionManager sessionManager = mock(SessionManager.class);
    private final UserContextService userContextService = mock(UserContextService.class);
    private final IamUserRepository iamUserRepository = mock(IamUserRepository.class);
    private final PractitionerRepository practitionerRepository = mock(PractitionerRepository.class);
    private final PractitionerRoleRepository practitionerRoleRepository = mock(PractitionerRoleRepository.class);
    private final PiiCryptoService piiCryptoService = mock(PiiCryptoService.class);

    @AfterEach
    void tearDown() {
        TenantContextStore.clear();
    }

    @Test
    void listUsersShouldReturnGlobalListForSuperUserInSystemTenant() {
        AdminUserController controller = new AdminUserController(
                createProfile20UserService,
                sessionManager,
                userContextService,
                iamUserRepository,
                practitionerRepository,
                practitionerRoleRepository,
                piiCryptoService);

        UUID sessionId = UUID.randomUUID();
        UUID superUserId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(AuthenticationFilter.REQUEST_SESSION_ID_ATTR, sessionId);
        TenantContextStore.set(TenantContext.from(SYSTEM_TENANT_ID));

        IamSession session = mock(IamSession.class);
        when(session.userId()).thenReturn(superUserId);
        IamUser superUser = new IamUser(
                superUserId,
                null,
                "root@system.local",
                "root@system.local",
                "hash",
                "bcrypt",
                true,
                0,
                null);
        IamUser tenantAdmin = new IamUser(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "admin@tenant.local",
                "admin@tenant.local",
                "hash",
                "bcrypt",
                true,
                10,
                null);

        when(sessionManager.findRequiredSession(eq(sessionId))).thenReturn(session);
        when(iamUserRepository.findById(eq(superUserId))).thenReturn(java.util.Optional.of(superUser));
        when(iamUserRepository.findAll()).thenReturn(List.of(superUser, tenantAdmin));

        ResponseEntity<?> response = controller.listUsers(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).asList().hasSize(2);
        verify(userContextService, never()).resolveContext(eq(sessionId), eq(SYSTEM_TENANT_ID));
    }

    @Test
    void listUsersShouldReturnTenantScopedListForTenantAdmin() {
        AdminUserController controller = new AdminUserController(
                createProfile20UserService,
                sessionManager,
                userContextService,
                iamUserRepository,
                practitionerRepository,
                practitionerRoleRepository,
                piiCryptoService);

        UUID sessionId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(AuthenticationFilter.REQUEST_SESSION_ID_ATTR, sessionId);
        TenantContextStore.set(TenantContext.from(tenantId));

        IamSession session = mock(IamSession.class);
        when(session.userId()).thenReturn(adminUserId);
        IamUser adminUser = new IamUser(
                adminUserId,
                tenantId,
                "admin@tenant.local",
                "admin@tenant.local",
                "hash",
                "bcrypt",
                true,
                10,
                null);

        when(sessionManager.findRequiredSession(eq(sessionId))).thenReturn(session);
        when(iamUserRepository.findById(eq(adminUserId))).thenReturn(java.util.Optional.of(adminUser));
        when(userContextService.resolveContext(eq(sessionId), eq(tenantId))).thenReturn(
                new UserContextService.UserContextResult(
                        tenantId,
                        tenantId,
                        "Tenant Admin",
                        null,
                        null,
                        null,
                        null,
                        10,
                        null,
                        null,
                        adminUserId));
        when(iamUserRepository.findByTenantId(eq(tenantId))).thenReturn(List.of(adminUser));

        ResponseEntity<?> response = controller.listUsers(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).asList().hasSize(1);
        verify(userContextService).resolveContext(eq(sessionId), eq(tenantId));
    }
}
