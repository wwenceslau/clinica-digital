package com.clinicadigital.gateway.aspects;

import com.clinicadigital.shared.api.TenantJdbcContextInterceptor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TenantValidationAspect {

    private final TenantJdbcContextInterceptor tenantJdbcContextInterceptor;

    public TenantValidationAspect(TenantJdbcContextInterceptor tenantJdbcContextInterceptor) {
        this.tenantJdbcContextInterceptor = tenantJdbcContextInterceptor;
    }

    @Around("execution(* com.clinicadigital.tenant.infrastructure..*(..))")
    public Object enforceTenantContext(ProceedingJoinPoint joinPoint) throws Throwable {
        tenantJdbcContextInterceptor.applyTenantContext();
        return joinPoint.proceed();
    }
}
