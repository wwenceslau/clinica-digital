package com.clinicadigital.tenant.infrastructure;

import com.clinicadigital.tenant.domain.ITenantRepository;
import com.clinicadigital.tenant.domain.Tenant;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public class TenantRepository implements ITenantRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public TenantRepository() {
    }

    @Override
    public Optional<Tenant> findBySlug(String slug) {
        return entityManager.createQuery(
                        "SELECT t FROM Tenant t WHERE t.slug = :slug",
                        Tenant.class)
                .setParameter("slug", slug)
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    public Optional<Tenant> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(Tenant.class, id));
    }

    @Override
    public List<Tenant> findAll() {
        return entityManager.createQuery("SELECT t FROM Tenant t", Tenant.class)
                .getResultList();
    }

    @Override
    public Tenant save(Tenant tenant) {
        if (entityManager.find(Tenant.class, tenant.getId()) == null) {
            entityManager.persist(tenant);
            return tenant;
        }
        return entityManager.merge(tenant);
    }

    @Override
    public void deleteById(UUID tenantId) {
        Tenant tenant = entityManager.find(Tenant.class, tenantId);
        if (tenant != null) {
            entityManager.remove(tenant);
        }
    }
}
