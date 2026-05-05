package com.clinicadigital.iam.application;

import com.clinicadigital.iam.domain.IamUser;
import com.clinicadigital.iam.domain.IamUserRepository;
import com.clinicadigital.iam.domain.Practitioner;
import com.clinicadigital.iam.domain.PractitionerRepository;
import com.clinicadigital.iam.domain.PractitionerRole;
import com.clinicadigital.iam.domain.PractitionerRoleRepository;
import com.clinicadigital.iam.application.PiiCryptoService.EncryptedValue;
import com.clinicadigital.shared.api.TraceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * T094/T095/T096 Application service that creates a new profile-20 (regular) user
 * within an existing tenant.
 *
 * <p>Creates atomically: {@link Practitioner}, {@link IamUser} (profile=20),
 * and {@link PractitionerRole} for the given location and role code.
 *
 * <h3>Email uniqueness (T095, FR-009)</h3>
 * Throws {@link EmailAlreadyTakenException} if the email is already taken within
 * the tenant (unique constraint: tenant_id + lower(email) for profiles 10/20).
 *
 * <h3>Audit (T096, FR-016)</h3>
 * On success, an {@code IamAuditEvent} of type {@code PROFILE20_USER_CREATED} is
 * written within the same transaction.
 *
 * Refs: FR-006, FR-009, FR-011, FR-015, FR-016, FR-017, FR-019
 */
@Service
public class CreateProfile20UserService {

    // RNDS FHIR profile URIs
    private static final String RNDS_PRACTITIONER_PROFILE =
            "http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude";
    private static final String RNDS_ROLE_PROFILE =
            "http://www.saude.gov.br/fhir/r4/StructureDefinition/BRLotacaoProfissional";

    // FHIR identifier system URIs
    private static final String CPF_SYSTEM = "https://saude.gov.br/sid/cpf";

    private static final int USER_PROFILE = 20;
    private static final String CPF_PATTERN = "\\d{11}";

    private final PractitionerRepository practitionerRepository;
    private final PractitionerRoleRepository practitionerRoleRepository;
    private final IamUserRepository iamUserRepository;
    private final PasswordService passwordService;
    private final PiiCryptoService piiCryptoService;
    private final AuditService auditService;

    /**
     * Thrown when an attempt to create a profile-20 user fails because the
     * given email is already registered within the same tenant.
     * Maps to HTTP 409 Conflict.
     *
     * @see EmailAlreadyTakenException
     */
    public static class EmailAlreadyTakenException extends com.clinicadigital.iam.application.EmailAlreadyTakenException {
        public EmailAlreadyTakenException(String email) {
            super(email);
        }
    }

    public CreateProfile20UserService(
            PractitionerRepository practitionerRepository,
            PractitionerRoleRepository practitionerRoleRepository,
            IamUserRepository iamUserRepository,
            PasswordService passwordService,
            PiiCryptoService piiCryptoService,
            AuditService auditService) {
        this.practitionerRepository = practitionerRepository;
        this.practitionerRoleRepository = practitionerRoleRepository;
        this.iamUserRepository = iamUserRepository;
        this.passwordService = passwordService;
        this.piiCryptoService = piiCryptoService;
        this.auditService = auditService;
    }

    /**
     * Creates a profile-20 user within an existing tenant.
     *
     * @param tenantId      tenant scope (must exist)
     * @param organizationId organization scope (must equal tenantId per constraint)
     * @param locationId    the location to assign as initial practitioner role
     * @param displayName   full display name of the new practitioner
     * @param email         login email (unique within tenant for profiles 10/20)
     * @param cpf           11-digit CPF (stored encrypted)
     * @param password      plaintext password (will be BCrypt-hashed)
     * @param roleCode      role code for the practitioner role (e.g. "MD", "RN")
     * @param adminUserId   the requesting admin's IamUser UUID (for audit)
     * @return result containing userId, practitionerId, practitionerRoleId, auditEventId
     * @throws IllegalArgumentException    if any input fails format validation
     * @throws EmailAlreadyTakenException  if email is already registered in the same tenant
     */
    @Transactional
    public CreateProfile20UserResult create(
            UUID tenantId,
            UUID organizationId,
            UUID locationId,
            String displayName,
            String email,
            String cpf,
            String password,
            String roleCode,
            UUID adminUserId) {

        // ── 1. Input validation ───────────────────────────────────────────────
        validateInputs(displayName, email, cpf, password, roleCode, locationId);

        // ── 2. Email uniqueness check (T095, FR-009) ──────────────────────────
        if (iamUserRepository.existsByEmailAndTenantId(email, tenantId)) {
            throw new EmailAlreadyTakenException(email);
        }

        String traceId = TraceContext.generate().traceId();

        // ── 3. Create Practitioner (with encrypted CPF) ───────────────────────
        UUID practitionerId = UUID.randomUUID();
        EncryptedValue encryptedCpf = piiCryptoService.encrypt(cpf);

        String practitionerFhirMetaProfile = "[\"" + RNDS_PRACTITIONER_PROFILE + "\"]";
        String practitionerFhirIdentifierJson =
                "[{\"system\":\"" + CPF_SYSTEM + "\",\"value\":\"" + maskCpf(cpf) + "\"}]";
        String practitionerFhirNameJson =
                "[{\"use\":\"official\",\"text\":\"" + sanitize(displayName) + "\"}]";

        Practitioner practitioner = new Practitioner(
                practitionerId,
                tenantId,
                "practitioner-" + practitionerId,
                practitionerFhirMetaProfile,
                practitionerFhirIdentifierJson,
                practitionerFhirNameJson,
                displayName,
                encryptedCpf.cipherText(),
                encryptedCpf.keyVersion());
        practitionerRepository.save(practitioner);

        // ── 4. Create IamUser (profile=20) ────────────────────────────────────
        String passwordHash = passwordService.hashPassword(password);
        UUID userId = UUID.randomUUID();
        IamUser newUser = new IamUser(
                userId,
                tenantId,
                email,
                email,
                passwordHash,
                "bcrypt",
                true,
                USER_PROFILE,
                practitionerId);
        iamUserRepository.save(newUser);

        // ── 5. Create PractitionerRole ────────────────────────────────────────
        UUID roleId = UUID.randomUUID();
        String roleFhirMetaProfile = "[\"" + RNDS_ROLE_PROFILE + "\"]";
        PractitionerRole practitionerRole = new PractitionerRole(
                roleId,
                tenantId,
                organizationId,
                locationId,
                practitionerId,
                "role-" + roleId,
                roleFhirMetaProfile,
                roleCode,
                true,
                false);
        practitionerRoleRepository.save(practitionerRole);

        // ── 6. Audit: PROFILE20_USER_CREATED (T096, FR-016) ──────────────────
        String metadataJson = "{\"practitioner_id\":\"" + practitionerId + "\"," +
                "\"iam_user_id\":\"" + userId + "\"," +
                "\"location_id\":\"" + locationId + "\"}";

        UUID auditEventId = auditService.logAuthEventReturningId(
                tenantId,
                adminUserId,
                "PROFILE20_USER_CREATED",
                "success",
                traceId,
                metadataJson);

        return new CreateProfile20UserResult(userId, practitionerId, roleId, auditEventId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void validateInputs(String displayName, String email, String cpf,
                                String password, String roleCode, UUID locationId) {
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new IllegalArgumentException("email must be a valid email address");
        }
        if (cpf == null || !cpf.matches(CPF_PATTERN)) {
            throw new IllegalArgumentException(
                    "cpf must be exactly 11 numeric digits, got: " + cpf);
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("password must not be blank");
        }
        if (roleCode == null || roleCode.isBlank()) {
            throw new IllegalArgumentException("roleCode must not be blank");
        }
        if (locationId == null) {
            throw new IllegalArgumentException("locationId must not be null");
        }
    }

    private static String maskCpf(String cpf) {
        return "*".repeat(Math.max(0, cpf.length() - 2)) + cpf.substring(cpf.length() - 2);
    }

    private static String sanitize(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
