package com.clinicadigital.gateway.api;

import com.clinicadigital.iam.application.AdminEmailAlreadyExistsException;
import com.clinicadigital.iam.application.PasswordService;
import com.clinicadigital.iam.application.PiiCryptoService;
import com.clinicadigital.iam.application.PiiCryptoService.EncryptedValue;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TenantAdminProfileService {

    private static final int ADMIN_PROFILE = 10;
    private static final String CPF_PATTERN = "\\d{11}";
    private static final String CPF_SYSTEM = "https://saude.gov.br/sid/cpf";
    private static final String RNDS_PRACTITIONER_PROFILE =
            "http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude";

    private final JdbcTemplate jdbcTemplate;
    private final PiiCryptoService piiCryptoService;
    private final PasswordService passwordService;

    public TenantAdminProfileService(
            JdbcTemplate jdbcTemplate,
            PiiCryptoService piiCryptoService,
            PasswordService passwordService) {
        this.jdbcTemplate = jdbcTemplate;
        this.piiCryptoService = piiCryptoService;
        this.passwordService = passwordService;
    }

    public Optional<TenantAdminSummary> findTenantAdmin(UUID tenantId) {
        return findAdminRow(tenantId)
                .map(row -> new TenantAdminSummary(
                        row.userId,
                        row.practitionerId,
                        row.displayName,
                        row.email,
                        decryptCpf(row.cpfEncrypted, row.encryptionKeyVersion)));
    }

    private Optional<AdminRow> findAdminRow(UUID tenantId) {
        List<AdminRow> rows = jdbcTemplate.query(
                """
                SELECT u.id AS user_id,
                       u.tenant_id AS organization_id,
                       p.id AS practitioner_id,
                       u.email AS email,
                       p.display_name AS display_name,
                       p.cpf_encrypted AS cpf_encrypted,
                       p.encryption_key_version AS encryption_key_version
                FROM iam_users u
                LEFT JOIN organizations o ON o.id = u.tenant_id
                JOIN practitioners p ON p.id = u.practitioner_id
                WHERE (o.tenant_id = ? OR u.tenant_id = ?)
                  AND u.profile = ?
                  AND u.account_active = TRUE
                ORDER BY u.created_at ASC
                LIMIT 1
                """,
                (rs, rowNum) -> new AdminRow(
                        UUID.fromString(rs.getString("user_id")),
                        parseUuid(rs.getString("organization_id")),
                        UUID.fromString(rs.getString("practitioner_id")),
                        rs.getString("email"),
                        rs.getString("display_name"),
                        rs.getBytes("cpf_encrypted"),
                        rs.getString("encryption_key_version")),
                tenantId,
                tenantId,
                ADMIN_PROFILE);

        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.getFirst());
    }

    @Transactional
    public TenantAdminSummary updateTenantAdmin(
            UUID tenantId,
            String organizationDisplayName,
            String organizationCnes,
            String displayName,
            String email,
            String cpf,
            String password) {

        validateInputs(displayName, email, cpf, password);

        UUID organizationId = resolveOrCreateOrganizationId(tenantId, organizationDisplayName, organizationCnes);
        AdminRow current = findAdminRow(tenantId).orElse(null);

                Long conflicts;
                if (current == null) {
                        conflicts = jdbcTemplate.queryForObject(
                                        """
                                        SELECT COUNT(1)
                                        FROM iam_users
                                        WHERE profile = ?
                                            AND lower(email) = lower(?)
                                        """,
                                        Long.class,
                                        ADMIN_PROFILE,
                                        email);
                } else {
                        conflicts = jdbcTemplate.queryForObject(
                                        """
                                        SELECT COUNT(1)
                                        FROM iam_users
                                        WHERE profile = ?
                                            AND lower(email) = lower(?)
                                            AND id <> ?
                                        """,
                                        Long.class,
                                        ADMIN_PROFILE,
                                        email,
                                        current.userId);
                }

        if (conflicts != null && conflicts > 0) {
            throw new AdminEmailAlreadyExistsException(email);
        }

        EncryptedValue encryptedCpf = piiCryptoService.encrypt(cpf);
        String fhirIdentifierJson = "[{\"system\":\"" + CPF_SYSTEM + "\",\"value\":\"" + maskCpf(cpf) + "\"}]";
        String fhirNameJson = "[{\"use\":\"official\",\"text\":\"" + sanitize(displayName) + "\"}]";
        String fhirMetaProfileJson = "[\"" + RNDS_PRACTITIONER_PROFILE + "\"]";

        String passwordHash = passwordService.hashPassword(password);

        if (current != null) {
            jdbcTemplate.update(
                    """
                    UPDATE iam_users
                    SET email = ?,
                        username = ?,
                        updated_at = NOW()
                    WHERE id = ?
                      AND tenant_id = ?
                    """,
                    email,
                    email,
                    current.userId,
                    current.organizationId);

            jdbcTemplate.update(
                    """
                    UPDATE practitioners
                    SET display_name = ?,
                        cpf_encrypted = ?,
                        encryption_key_version = ?,
                        fhir_identifier_json = CAST(? AS jsonb),
                        fhir_name_json = CAST(? AS jsonb),
                        updated_at = NOW()
                    WHERE id = ?
                      AND tenant_id = ?
                    """,
                    displayName,
                    encryptedCpf.cipherText(),
                    encryptedCpf.keyVersion(),
                    fhirIdentifierJson,
                    fhirNameJson,
                    current.practitionerId,
                    tenantId);

            jdbcTemplate.update(
                    """
                    UPDATE iam_users
                    SET password_hash = ?,
                        password_algo = 'bcrypt',
                        updated_at = NOW()
                    WHERE id = ?
                      AND tenant_id = ?
                    """,
                    passwordHash,
                    current.userId,
                    current.organizationId);

            return new TenantAdminSummary(
                    current.userId,
                    current.practitionerId,
                    displayName,
                    email,
                    cpf);
        }

        UUID practitionerId = UUID.randomUUID();
        String practitionerResourceId = "practitioner-" + practitionerId;
        jdbcTemplate.update(
                """
                INSERT INTO practitioners (
                    id,
                    tenant_id,
                    fhir_resource_id,
                    fhir_meta_profile,
                    fhir_identifier_json,
                    fhir_name_json,
                    fhir_active,
                    display_name,
                    cpf_encrypted,
                    encryption_key_version,
                    account_active,
                    created_at,
                    updated_at
                ) VALUES (?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), CAST(? AS jsonb), true, ?, ?, ?, true, NOW(), NOW())
                """,
                practitionerId,
                tenantId,
                practitionerResourceId,
                fhirMetaProfileJson,
                fhirIdentifierJson,
                fhirNameJson,
                displayName,
                encryptedCpf.cipherText(),
                encryptedCpf.keyVersion());

        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO iam_users (
                    id,
                    tenant_id,
                    username,
                    email,
                    password_hash,
                    password_algo,
                    account_active,
                    profile,
                    practitioner_id,
                    created_at,
                    updated_at
                ) VALUES (?, ?, ?, ?, ?, 'bcrypt', true, ?, ?, NOW(), NOW())
                """,
                userId,
                organizationId,
                email,
                email,
                passwordHash,
                ADMIN_PROFILE,
                practitionerId);

        return new TenantAdminSummary(
                userId,
                practitionerId,
                displayName,
                email,
                cpf);
    }

    private UUID resolveOrganizationId(UUID tenantId) {
        String rawOrganizationId = jdbcTemplate.query(
                """
                SELECT id
                FROM organizations
                WHERE tenant_id = ?
                ORDER BY created_at ASC
                LIMIT 1
                """,
                rs -> rs.next() ? rs.getString("id") : null,
                tenantId);
        if (rawOrganizationId != null) {
            return UUID.fromString(rawOrganizationId);
        }

        String sameIdOrganization = jdbcTemplate.query(
                """
                SELECT id
                FROM organizations
                WHERE id = ?
                LIMIT 1
                """,
                rs -> rs.next() ? rs.getString("id") : null,
                tenantId);
        if (sameIdOrganization != null) {
            return UUID.fromString(sameIdOrganization);
        }
        return null;
    }

    private UUID resolveOrCreateOrganizationId(UUID tenantId, String organizationDisplayName, String organizationCnes) {
        UUID existingOrganizationId = resolveOrganizationId(tenantId);
        if (existingOrganizationId != null) {
            return existingOrganizationId;
        }

        String fhirMetaProfileJson = "[\"http://hl7.org/fhir/StructureDefinition/Organization\"]";
        String fhirIdentifierJson = "[{\"system\":\"https://saude.gov.br/sid/cnes\",\"value\":\""
                + sanitize(organizationCnes)
                + "\"}]";

        jdbcTemplate.update(
                """
                INSERT INTO organizations (
                    id,
                    tenant_id,
                    cnes,
                    display_name,
                    fhir_resource_id,
                    fhir_meta_profile,
                    fhir_identifier_json,
                    fhir_name,
                    fhir_active,
                    quota_tier,
                    account_active,
                    created_at,
                    updated_at
                ) VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), ?, true, 'standard', true, NOW(), NOW())
                """,
                tenantId,
                tenantId,
                organizationCnes,
                organizationDisplayName,
                "organization-" + tenantId,
                fhirMetaProfileJson,
                fhirIdentifierJson,
                organizationDisplayName);

        return tenantId;
    }

    private static UUID parseUuid(String rawValue) {
        return rawValue == null || rawValue.isBlank() ? null : UUID.fromString(rawValue);
    }

    private String decryptCpf(byte[] cipherText, String keyVersion) {
        if (cipherText == null || keyVersion == null || keyVersion.isBlank()) {
            return null;
        }
        return piiCryptoService.decrypt(cipherText, keyVersion);
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

    private static void validateInputs(String displayName, String email, String cpf, String password) {
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("adminDisplayName must not be blank");
        }
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new IllegalArgumentException("adminEmail must be a valid email address");
        }
        if (cpf == null || !cpf.matches(CPF_PATTERN)) {
            throw new IllegalArgumentException("adminCpf must be exactly 11 numeric digits");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("adminPassword must not be blank");
        }
    }

    private record AdminRow(
            UUID userId,
            UUID organizationId,
            UUID practitionerId,
            String email,
            String displayName,
            byte[] cpfEncrypted,
            String encryptionKeyVersion) {
    }

    public record TenantAdminSummary(
            UUID userId,
            UUID practitionerId,
            String displayName,
            String email,
            String cpf) {
    }
}