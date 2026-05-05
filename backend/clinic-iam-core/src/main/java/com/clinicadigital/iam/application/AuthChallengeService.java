package com.clinicadigital.iam.application;

import com.clinicadigital.iam.domain.IamAuthChallenge;
import com.clinicadigital.iam.domain.IamAuthChallengeRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Creates and resolves opaque challenge tokens for multi-org login (US4).
 *
 * Flow:
 * 1. {@code createChallenge()} — called when a user has 2+ active organizations.
 *    Stores a SHA-256 digest of an opaque random token.
 * 2. {@code resolveChallenge()} — called from /api/auth/select-organization.
 *    Validates the token, checks org is in allowed list, returns the IamUser ID
 *    and selected organization UUID.
 *
 * Refs: US4, FR-007c
 */
@Service
public class AuthChallengeService {

    private static final long CHALLENGE_TTL_SECONDS = 5 * 60; // 5 minutes

    private final IamAuthChallengeRepository repository;
    private final ObjectMapper objectMapper;

    public AuthChallengeService(IamAuthChallengeRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Create a new challenge for the given user and set of allowed organizations.
     *
     * @param iamUserId    the authenticated user id
     * @param organizations list of OrganizationOption records (id, displayName, cnes)
     * @return an opaque token to return to the client (NOT the digest)
     */
    @Transactional
    public String createChallenge(UUID iamUserId, List<OrganizationOption> organizations) {
        if (iamUserId == null || organizations == null || organizations.isEmpty()) {
            throw new IllegalArgumentException("iamUserId and organizations are required");
        }

        String opaqueToken = generateOpaqueToken();
        String digest = sha256Hex(opaqueToken);

        String optionsJson;
        try {
            optionsJson = objectMapper.writeValueAsString(organizations);
        } catch (Exception e) {
            throw new IllegalStateException("failed to serialize organization options", e);
        }

        IamAuthChallenge challenge = new IamAuthChallenge(
                UUID.randomUUID(),
                iamUserId,
                digest,
                optionsJson,
                Instant.now().plusSeconds(CHALLENGE_TTL_SECONDS)
        );
        repository.save(challenge);
        return opaqueToken;
    }

    /**
     * Resolve a challenge token by verifying its digest, checking the org is allowed,
     * and returning the user ID and selected organization ID for session creation.
     *
     * @param opaqueToken    the raw token from the client
     * @param organizationId the organization the client selected
     * @return resolved result with user id and organization id
     * @throws AuthChallengeException if token is invalid, expired or org not allowed
     */
    @Transactional(readOnly = true)
    public ResolvedChallenge resolveChallenge(String opaqueToken, UUID organizationId) {
        if (opaqueToken == null || opaqueToken.isBlank() || organizationId == null) {
            throw new AuthChallengeException("challengeToken and organizationId are required");
        }

        String digest = sha256Hex(opaqueToken);
        IamAuthChallenge challenge = repository.findByTokenDigest(digest)
                .orElseThrow(() -> new AuthChallengeException("invalid or expired challenge token"));

        if (challenge.isExpired()) {
            throw new AuthChallengeException("challenge token has expired");
        }

        List<OrganizationOption> options;
        try {
            options = objectMapper.readValue(challenge.getOrganizationOptionsJson(),
                    new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException("failed to deserialize organization options", e);
        }

        boolean orgAllowed = options.stream()
                .anyMatch(opt -> organizationId.equals(opt.organizationId()));
        if (!orgAllowed) {
            throw new AuthChallengeException("organization not allowed for this challenge");
        }

        return new ResolvedChallenge(challenge.getIamUserId(), organizationId);
    }

    private static String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /** DTO: one organization option returned in the multi-org challenge response. */
    public record OrganizationOption(UUID organizationId, String displayName, String cnes) {}

    /** Result of a successful challenge resolution. */
    public record ResolvedChallenge(UUID iamUserId, UUID organizationId) {}

    /** Thrown when the challenge is invalid, expired, or org not allowed. */
    public static class AuthChallengeException extends RuntimeException {
        public AuthChallengeException(String message) {
            super(message);
        }
    }
}
