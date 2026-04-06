package com.clinicadigital.shared.api;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class TraceContextHolder {

    private TraceContext traceContext;

    public void set(TraceContext traceContext) {
        this.traceContext = traceContext;
    }

    public TraceContext getOrCreate() {
        if (traceContext == null) {
            traceContext = TraceContext.generate();
        }
        return traceContext;
    }

    public boolean isPresent() {
        return traceContext != null;
    }

    public void clear() {
        traceContext = null;
    }
}