package com.clinicadigital.iam.application;

import com.clinicadigital.iam.domain.IamUser;
import com.clinicadigital.iam.domain.IamUserRepository;
import com.clinicadigital.iam.domain.Practitioner;
import com.clinicadigital.iam.domain.PractitionerRepository;
import com.clinicadigital.shared.api.TraceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * T026+T027+T028+T029 [US1] Bootstrap service for the super-user (profile 0).
 *
 * <p>Transaction strategy:
 * <ul>
 *   <li>Success path: {@link #bootstrap} is {@code @Transactional}. All writes
 *       (Practitioner, IamUser, success audit) commit atomically.</li>
 *   <li>Failure path (duplicate bootstrap): {@link AuditService#logAuditInNewTransaction}
 *       is {@code REQUIRES_NEW}, so the failure audit event commits independently,
 *       even though the outer transaction never wrote anything and is about to abort.</li>
 * </ul>
 *
 * Refs: FR-001, FR-002, FR-009, FR-010, FR-016, FR-017, FR-020, FR-021
 */
@Service
public class BootstrapSuperUserService {

    /** Sentinel tenant_id for the system-level super-user. */
    public static final UUID SYSTEM_TENANT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000000");

    private static final String RNDS_PRACTITIONER_PROFILE =
            "http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude";

    private static final int SUPER_USER_PROFILE = 0;

    private final IamUserRepository iamUserRepository;
    private final PractitionerRepository practitionerRepository;
    private final PasswordService passwordService;
    private final AuditService auditService;
    private final RndsStructureDefinitionRegistry rndsRegistry;

    public BootstrapSuperUserService(IamUserRepository iamUserRepository,
                                     PractitionerRepository practitionerRepository,
                                     PasswordService passwordService,
                                     AuditService auditService,
                                     RndsStructureDefinitionRegistry rndsRegistry) {
        this.iamUserRepository = iamUserRepository;
        this.practitionerRepository = practitionerRepository;
        this.passwordService = passwordService;
        this.auditService = auditService;
        this.rndsRegistry = rndsRegistry;
    }

    /**
     * Bootstrap the super-user.
     *
     * <p>On duplicate: persists a failure audit event in an independent transaction,
     * then throws {@link SuperUserAlreadyExistsException} — the outer transaction
     * rolls back with no writes pending.
     *
     * @param email    login email for the super-user
     * @param password plaintext password (will be hashed with BCrypt)
     * @param name     display name used for Practitioner and IamUser
     * @return {@link BootstrapResult} with practitionerId and auditEventId
     * @throws SuperUserAlreadyExistsException if a super-user already exists
     * @throws IllegalArgumentException        if any required argument is blank
     */
    @Transactional
    public BootstrapResult bootstrap(String email, String password, String name) {
        validateInputs(email, password, name);

        String traceId = TraceContext.generate().traceId();

        if (iamUserRepository.existsByProfile(SUPER_USER_PROFILE)) {
            // Failure audit goes into its own independent transaction (REQUIRES_NEW)
            // so it commits even though we are about to throw and the outer TX rolls back.
            // System-level event: tenant_id=null (no organization FK required).
            auditService.logAuditInNewTransaction(
                    null,
                    null,
                    "SUPER_USER_BOOTSTRAPPED",
                    "failure",
                    traceId,
                    "{\"reason\":\"super-user already exists\"}");
            throw new SuperUserAlreadyExistsException();
        }

        // T029 — Validate RNDS meta.profile before any persistence
        rndsRegistry.assertSupported(RNDS_PRACTITIONER_PROFILE);

        // T026 — Create global Practitioner (tenant_id=null for super-user)
        UUID practitionerId = UUID.randomUUID();
        String fhirMetaProfile = "[\"" + RNDS_PRACTITIONER_PROFILE + "\"]";
        // Super-user is a system entity (not a real FHIR Practitioner with CPF/CNES).
        // Use an internal system identifier to satisfy ck_practitioners_identifier_not_empty.
        String fhirIdentifierJson = "[{\"system\":\"urn:clinicadigital:internal\",\"value\":\""
                + practitionerId + "\"}]";
        String fhirNameJson = "[{\"use\":\"official\",\"text\":\"" + sanitizeName(name) + "\"}]";

        Practitioner practitioner = new Practitioner(
                practitionerId,
                null,
                "practitioner-" + practitionerId,
                fhirMetaProfile,
                fhirIdentifierJson,
                fhirNameJson,
                name);
        practitionerRepository.save(practitioner);

        // T026 — Create IamUser (profile=0, tenant_id=null per ck_iam_users_tenant_required_for_tenant_profiles)
        String passwordHash = passwordService.hashPassword(password);
        UUID userId = UUID.randomUUID();
        IamUser superUser = new IamUser(
                userId,
                null,
                email,
                email,
                passwordHash,
                "bcrypt",
                true,
                SUPER_USER_PROFILE,
                practitionerId);
        iamUserRepository.save(superUser);

        // T027 — Success audit joins the main transaction (REQUIRED) — atomic with writes
        // System-level event: tenant_id=null (super-user has no organization FK).
        auditService.logAuthEvent(
                null,
                userId,
                "SUPER_USER_BOOTSTRAPPED",
                "success",
                traceId,
                "{\"profile\":0,\"practitioner_id\":\"" + practitionerId + "\"}");

        return new BootstrapResult(practitionerId, traceId);
    }

    private void validateInputs(String email, String password, String name) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("password must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }

    /** Escapes JSON special characters to prevent injection via display name. */
    private static String sanitizeName(String name) {
        return name.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", " ")
                   .replace("\r", " ");
    }

    /**
     * Immutable result of a successful bootstrap operation.
     *
     * @param practitionerId UUID of the created global Practitioner
     * @param traceId        trace identifier for observability (correlates to audit event)
     */
    public record BootstrapResult(UUID practitionerId, String traceId) {

        /**
         * Returns a stable reference to the audit event using the traceId.
         * The DB auto-generated Long id is not exposed at the application boundary.
         */
        public String auditEventId() {
            return traceId;
        }
    }
}
