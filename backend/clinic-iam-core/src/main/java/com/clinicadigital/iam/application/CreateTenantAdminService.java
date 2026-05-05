package com.clinicadigital.iam.application;

import com.clinicadigital.iam.domain.IamUser;
import com.clinicadigital.iam.domain.IamUserRepository;
import com.clinicadigital.iam.domain.Organization;
import com.clinicadigital.iam.domain.OrganizationRepository;
import com.clinicadigital.iam.domain.Practitioner;
import com.clinicadigital.iam.domain.PractitionerRepository;
import com.clinicadigital.iam.application.PiiCryptoService.EncryptedValue;
import com.clinicadigital.shared.api.TraceContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * T035/T036/T037/T039 [US2] Transactional service that creates a new tenant
 * together with its Organization (FHIR BREstabelecimentoSaude), admin Practitioner
 * (FHIR BRProfissionalSaude) and admin IamUser (profile=10) in a single atomic
 * transaction.
 *
 * <h3>Conflict checks (T036)</h3>
 * <ol>
 *   <li>CNES must be globally unique ({@link TenantAlreadyExistsException})</li>
 *   <li>Organization display_name must be globally unique, case-insensitive
 *       ({@link TenantAlreadyExistsException})</li>
 *   <li>Admin email must not already belong to another admin (profile=10) in any
 *       tenant ({@link AdminEmailAlreadyExistsException})</li>
 * </ol>
 *
 * <h3>RNDS validation (T037)</h3>
 * Both FHIR profiles ({@code BREstabelecimentoSaude}, {@code BRProfissionalSaude})
 * are validated via {@link RndsStructureDefinitionRegistry#assertSupported}.
 *
 * <h3>Audit (T039)</h3>
 * On success, an {@code IamAuditEvent} of type {@code TENANT_ADMIN_CREATED} is
 * written within the same transaction. The generated audit ID is returned in
 * {@link CreateTenantAdminResult}.
 *
 * <p>Tenant row is inserted via {@link JdbcTemplate} to avoid an unnecessary
 * dependency on {@code clinic-tenant-core} from within {@code clinic-iam-core}.
 *
 * Refs: FR-003, FR-009, FR-010, FR-022
 */
@Service
public class CreateTenantAdminService {

    // RNDS FHIR profile URIs
    public static final String RNDS_ORG_PROFILE =
            "http://www.saude.gov.br/fhir/r4/StructureDefinition/BREstabelecimentoSaude";
    public static final String RNDS_PRACTITIONER_PROFILE =
            "http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude";

    // FHIR identifier system URIs
    private static final String CNES_SYSTEM  = "https://saude.gov.br/sid/cnes";
    private static final String CPF_SYSTEM   = "https://saude.gov.br/sid/cpf";

    private static final int ADMIN_PROFILE = 10;
    private static final String CNES_PATTERN = "\\d{7}";
    private static final String CPF_PATTERN  = "\\d{11}";

    private final JdbcTemplate jdbcTemplate;
    private final OrganizationRepository organizationRepository;
    private final PractitionerRepository practitionerRepository;
    private final IamUserRepository iamUserRepository;
    private final PasswordService passwordService;
    private final PiiCryptoService piiCryptoService;
    private final AuditService auditService;
    private final RndsStructureDefinitionRegistry rndsRegistry;

    public CreateTenantAdminService(
            JdbcTemplate jdbcTemplate,
            OrganizationRepository organizationRepository,
            PractitionerRepository practitionerRepository,
            IamUserRepository iamUserRepository,
            PasswordService passwordService,
            PiiCryptoService piiCryptoService,
            AuditService auditService,
            RndsStructureDefinitionRegistry rndsRegistry) {
        this.jdbcTemplate = jdbcTemplate;
        this.organizationRepository = organizationRepository;
        this.practitionerRepository = practitionerRepository;
        this.iamUserRepository = iamUserRepository;
        this.passwordService = passwordService;
        this.piiCryptoService = piiCryptoService;
        this.auditService = auditService;
        this.rndsRegistry = rndsRegistry;
    }

    /**
     * Create a new tenant, organization, admin practitioner and admin iam_user
     * in one atomic transaction.
     *
     * @param tenantName       Human-readable name; also used as organization display_name
     * @param cnes             7-digit CNES code (globally unique)
     * @param adminDisplayName Full name of the admin practitioner
     * @param adminEmail       Email for the admin IamUser login (globally unique for profile=10)
     * @param adminCpf         11-digit CPF (stored encrypted)
     * @param adminPassword    Plaintext password (will be BCrypt-hashed)
     * @return result containing tenantId, adminPractitionerId and auditEventId
     * @throws IllegalArgumentException            if any input fails format validation
     * @throws TenantAlreadyExistsException        if CNES or organization name is already taken
     * @throws AdminEmailAlreadyExistsException    if admin email is already registered globally
     */
    @Transactional
    public CreateTenantAdminResult create(
            String tenantName,
            String cnes,
            String adminDisplayName,
            String adminEmail,
            String adminCpf,
            String adminPassword) {

        // ── 1. Input validation ───────────────────────────────────────────────
        validateInputs(tenantName, cnes, adminDisplayName, adminEmail, adminCpf, adminPassword);

        // ── 2. RNDS profile availability (T037) ────────────────────────────────
        rndsRegistry.assertSupported(RNDS_ORG_PROFILE);
        rndsRegistry.assertSupported(RNDS_PRACTITIONER_PROFILE);

        // ── 3. Conflict checks (T036) ─────────────────────────────────────────
        if (organizationRepository.existsByCnes(cnes)) {
            throw new TenantAlreadyExistsException("cnes", cnes);
        }
        if (organizationRepository.existsByDisplayNameIgnoreCase(tenantName)) {
            throw new TenantAlreadyExistsException("name", tenantName);
        }
        if (iamUserRepository.existsByEmailAndProfile(adminEmail, ADMIN_PROFILE)) {
            throw new AdminEmailAlreadyExistsException(adminEmail);
        }

        String traceId = TraceContext.generate().traceId();

        // ── 4. Create tenant row ──────────────────────────────────────────────
        UUID tenantId = UUID.randomUUID();
        String slug = slugify(tenantName);
        jdbcTemplate.update("""
                INSERT INTO tenants
                    (id, slug, legal_name, status, plan_tier,
                     quota_requests_per_minute, quota_concurrency, quota_storage_mb,
                     created_at, updated_at)
                VALUES (?, ?, ?, 'active', 'basic', 120, 10, 1024, NOW(), NOW())
                """,
                tenantId, slug, tenantName);

        // ── 5. Create Organization ────────────────────────────────────────────
        UUID organizationId = UUID.randomUUID();
        String orgFhirMetaProfile = "[\"" + RNDS_ORG_PROFILE + "\"]";
        String orgFhirIdentifierJson = "[{\"system\":\"" + CNES_SYSTEM + "\",\"value\":\"" + cnes + "\"}]";

        Organization organization = new Organization(
                organizationId,
                tenantId,
                cnes,
                tenantName,
                "org-" + organizationId,
                orgFhirMetaProfile,
                orgFhirIdentifierJson,
                tenantName);
        organizationRepository.save(organization);

        // ── 6. Create Practitioner (with encrypted CPF) ───────────────────────
        UUID practitionerId = UUID.randomUUID();
        EncryptedValue encryptedCpf = piiCryptoService.encrypt(adminCpf);

        String practitionerFhirMetaProfile = "[\"" + RNDS_PRACTITIONER_PROFILE + "\"]";
        // CPF is never stored in plaintext in FHIR resources — use token reference only
        String practitionerFhirIdentifierJson =
                "[{\"system\":\"" + CPF_SYSTEM + "\",\"value\":\"" + maskCpf(adminCpf) + "\"}]";
        String practitionerFhirNameJson =
                "[{\"use\":\"official\",\"text\":\"" + sanitize(adminDisplayName) + "\"}]";

        Practitioner practitioner = new Practitioner(
                practitionerId,
                tenantId,
                "practitioner-" + practitionerId,
                practitionerFhirMetaProfile,
                practitionerFhirIdentifierJson,
                practitionerFhirNameJson,
                adminDisplayName,
                encryptedCpf.cipherText(),
                encryptedCpf.keyVersion());
        practitionerRepository.save(practitioner);

        // ── 7. Create IamUser admin (profile=10) ──────────────────────────────
        String passwordHash = passwordService.hashPassword(adminPassword);
        UUID adminUserId = UUID.randomUUID();
        IamUser adminUser = new IamUser(
                adminUserId,
                tenantId,
                adminEmail,
                adminEmail,
                passwordHash,
                "bcrypt",
                true,
                ADMIN_PROFILE,
                practitionerId);
        iamUserRepository.save(adminUser);

        // ── 8. Audit: TENANT_ADMIN_CREATED (T039) ─────────────────────────────
        String metadataJson = "{\"cnes\":\"" + sanitize(cnes) + "\"," +
                "\"organization_id\":\"" + organizationId + "\"," +
                "\"practitioner_id\":\"" + practitionerId + "\"}";

        UUID auditEventId = auditService.logAuthEventReturningId(
                tenantId,
                adminUserId,
                "TENANT_ADMIN_CREATED",
                "success",
                traceId,
                metadataJson);

        return new CreateTenantAdminResult(tenantId, practitionerId, auditEventId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void validateInputs(String tenantName, String cnes, String adminDisplayName,
                                String adminEmail, String adminCpf, String adminPassword) {
        if (tenantName == null || tenantName.isBlank()) {
            throw new IllegalArgumentException("tenantName must not be blank");
        }
        if (cnes == null || !cnes.matches(CNES_PATTERN)) {
            throw new IllegalArgumentException(
                    "cnes must be exactly 7 numeric digits, got: " + cnes);
        }
        if (adminDisplayName == null || adminDisplayName.isBlank()) {
            throw new IllegalArgumentException("adminDisplayName must not be blank");
        }
        if (adminEmail == null || adminEmail.isBlank() || !adminEmail.contains("@")) {
            throw new IllegalArgumentException("adminEmail must be a valid email address");
        }
        if (adminCpf == null || !adminCpf.matches(CPF_PATTERN)) {
            throw new IllegalArgumentException(
                    "adminCpf must be exactly 11 numeric digits, got: " + adminCpf);
        }
        if (adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalArgumentException("adminPassword must not be blank");
        }
    }

    /**
     * Converts a display name to a URL-safe slug for the {@code tenants.slug} column.
     * Only ASCII letters and digits are kept; all other characters become hyphens.
     */
    private static String slugify(String name) {
        return name.toLowerCase()
                   .replaceAll("[^a-z0-9]+", "-")
                   .replaceAll("^-|-$", "");
    }

    /**
     * Masks CPF so that only the last 2 digits are visible (e.g. {@code "*********01"}).
     * Used in the FHIR identifier JSON — the real CPF is stored encrypted in the DB.
     */
    private static String maskCpf(String cpf) {
        return "*".repeat(Math.max(0, cpf.length() - 2)) + cpf.substring(cpf.length() - 2);
    }

    /** Escapes JSON special characters to prevent injection via user-provided values. */
    private static String sanitize(String value) {
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
}
