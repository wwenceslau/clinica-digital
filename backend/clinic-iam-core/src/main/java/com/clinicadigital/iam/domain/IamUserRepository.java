package com.clinicadigital.iam.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IamUserRepository {

    Optional<IamUser> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<IamUser> findByEmailAndTenantId(String email, UUID tenantId);

    /**
     * Global email lookup across all tenants/profiles. Used during multi-org login
     * (US4) where tenant is not known at authentication time.
     */
    List<IamUser> findByEmail(String email);

    /** Returns true if at least one iam_user with the given profile value exists. */
    boolean existsByProfile(int profile);

    /**
     * Returns true if any iam_user (across all tenants) has the given email AND
     * the given profile value. Used to enforce global email uniqueness for admin
     * practitioners (profile=10) before creating a new tenant.
     */
    boolean existsByEmailAndProfile(String email, int profile);

    /**
     * Returns true if an iam_user with the given email exists within the given tenant.
     * Used to enforce per-tenant email uniqueness for profile-20 users (T095, FR-009).
     */
    boolean existsByEmailAndTenantId(String email, UUID tenantId);

    IamUser save(IamUser user);

    /**
     * Returns all iam_users belonging to the given tenant, ordered by creation date.
     */
    List<IamUser> findByTenantId(UUID tenantId);
}
