package com.clinicadigital.shared.api;

import org.springframework.stereotype.Component;

@Component
public class TraceContextHolder {

    private final ThreadLocal<TraceContext> traceContext = new ThreadLocal<>();

    public void set(TraceContext traceContext) {
        this.traceContext.set(traceContext);
    }

    public TraceContext getOrCreate() {
        TraceContext current = traceContext.get();
        if (current == null) {
            current = TraceContext.generate();
            traceContext.set(current);
        }
        return current;
    }

    public boolean isPresent() {
        return traceContext.get() != null;
    }

    public void clear() {
        traceContext.remove();
    }
}