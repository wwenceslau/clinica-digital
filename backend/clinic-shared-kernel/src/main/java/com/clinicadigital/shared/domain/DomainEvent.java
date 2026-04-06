package com.clinicadigital.shared.domain;

import java.time.Instant;

/**
 * Marker interface for domain events shared by all modules.
 */
public interface DomainEvent {
    String eventType();

    Instant occurredAt();
}
