package com.clinicadigital.shared.api;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Trace context contract used across modules.
 */
public record TraceContext(String traceId) {

	private static final String TRACE_PREFIX = "trace-";
	private static final Pattern TRACE_PATTERN = Pattern.compile("^trace-[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

	public TraceContext {
		if (traceId == null || traceId.isBlank()) {
			throw new IllegalArgumentException("traceId must not be blank");
		}
	}

	public static TraceContext generate() {
		return new TraceContext(TRACE_PREFIX + UUID.randomUUID());
	}

	public static TraceContext from(String traceId) {
		return new TraceContext(traceId);
	}

	public static boolean isValid(String traceId) {
		return traceId != null && TRACE_PATTERN.matcher(traceId).matches();
	}
}
