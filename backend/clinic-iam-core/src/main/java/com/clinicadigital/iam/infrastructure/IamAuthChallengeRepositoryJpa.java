package com.clinicadigital.iam.infrastructure;

import com.clinicadigital.iam.domain.IamAuthChallenge;
import com.clinicadigital.iam.domain.IamAuthChallengeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class IamAuthChallengeRepositoryJpa implements IamAuthChallengeRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public IamAuthChallenge save(IamAuthChallenge challenge) {
        if (entityManager.find(IamAuthChallenge.class, challenge.getId()) == null) {
            entityManager.persist(challenge);
            return challenge;
        }
        return entityManager.merge(challenge);
    }

    @Override
    public Optional<IamAuthChallenge> findByTokenDigest(String tokenDigest) {
        return entityManager.createQuery(
                        "SELECT c FROM IamAuthChallenge c WHERE c.challengeTokenDigest = :digest",
                        IamAuthChallenge.class)
                .setParameter("digest", tokenDigest)
                .getResultList()
                .stream()
                .findFirst();
    }
}
