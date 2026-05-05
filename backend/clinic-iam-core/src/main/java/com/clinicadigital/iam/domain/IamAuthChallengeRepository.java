package com.clinicadigital.iam.domain;

import java.util.Optional;

/**
 * Domain repository for {@link IamAuthChallenge}.
 */
public interface IamAuthChallengeRepository {

    IamAuthChallenge save(IamAuthChallenge challenge);

    /**
     * Find an unexpired challenge by its token digest (SHA-256 of the opaque token).
     */
    Optional<IamAuthChallenge> findByTokenDigest(String tokenDigest);
}
