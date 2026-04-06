package com.clinicadigital.shared.api;

import java.util.UUID;

/**
 * Trace context contract used across modules.
 */
public record TraceContext(String traceId) {

	public TraceContext {
		if (traceId == null || traceId.isBlank()) {
			throw new IllegalArgumentException("traceId must not be blank");
		}
	}

	public static TraceContext generate() {
		return new TraceContext(UUID.randomUUID().toString());
	}

	public static TraceContext from(String traceId) {
		return new TraceContext(traceId);
	}
}
