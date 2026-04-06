package com.clinicadigital.gateway.api;

import com.clinicadigital.gateway.exception.InvalidTenantContextException;
import com.clinicadigital.shared.api.TenantContextHolder;
import com.clinicadigital.tenant.application.TenantService;
import com.clinicadigital.tenant.domain.Tenant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/tenants")
@Validated
public class TenantController {

    private final TenantService tenantService;
    private final TenantContextHolder tenantContextHolder;

    public TenantController(TenantService tenantService, TenantContextHolder tenantContextHolder) {
        this.tenantService = tenantService;
        this.tenantContextHolder = tenantContextHolder;
    }

    @PostMapping("/create")
    public ResponseEntity<TenantResponse> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        Tenant created = tenantService.createTenant(request.slug(), request.legalName(), request.planTier());
        return ResponseEntity.status(HttpStatus.CREATED).body(TenantResponse.from(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TenantResponse> getTenant(@PathVariable UUID id) {
        UUID contextTenantId = tenantContextHolder.getRequired().tenantId();
        if (!id.equals(contextTenantId)) {
            throw new InvalidTenantContextException("tenant context invalid: path tenant does not match context tenant");
        }
        Tenant tenant = tenantService.getTenant(id);
        return ResponseEntity.ok(TenantResponse.from(tenant));
    }

    public record CreateTenantRequest(
            @NotBlank String slug,
            @NotBlank String legalName,
            @NotBlank String planTier
    ) {
    }

    public record TenantResponse(
            UUID id,
            String slug,
            String legalName,
            String status,
            String planTier,
            Integer quotaRequestsPerMinute,
            Integer quotaConcurrency,
            Integer quotaStorageMb
    ) {
        static TenantResponse from(Tenant tenant) {
            return new TenantResponse(
                    tenant.getId(),
                    tenant.getSlug(),
                    tenant.getLegalName(),
                    tenant.getStatus(),
                    tenant.getPlanTier(),
                    tenant.getQuotaRequestsPerMinute(),
                    tenant.getQuotaConcurrency(),
                    tenant.getQuotaStorageMb()
            );
        }
    }
}
