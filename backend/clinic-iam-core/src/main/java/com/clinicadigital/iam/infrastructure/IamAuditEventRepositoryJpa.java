package com.clinicadigital.iam.infrastructure;

import com.clinicadigital.iam.domain.IamAuditEvent;
import com.clinicadigital.iam.domain.IamAuditEventRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

@Repository
class IamAuditEventRepositoryJpa implements IamAuditEventRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public IamAuditEvent save(IamAuditEvent event) {
        entityManager.persist(event);
        return event;
    }
}
