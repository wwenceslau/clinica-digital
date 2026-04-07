package com.clinicadigital.observability.cli;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Meter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.List;
import java.util.UUID;

/**
 * CLI commands for observability validation/export operations.
 */
@ShellComponent
public class ObservabilityCommands {

    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

    public ObservabilityCommands(ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.meterRegistryProvider = meterRegistryProvider;
    }

    @ShellMethod(key = "trace validate", value = "Validate trace identifier format")
    public String traceValidate(
            @ShellOption(value = "--trace-id", help = "Trace id to validate", defaultValue = ShellOption.NULL) String traceId,
            @ShellOption(value = "--json", help = "Output as structured JSON", defaultValue = "false") boolean json
    ) {
        String effectiveTraceId = traceId == null || traceId.isBlank()
                ? UUID.randomUUID().toString()
                : traceId.trim();

        boolean valid = isTraceIdFormatValid(effectiveTraceId);
        if (json) {
            return "{\n" +
                   "  \"trace_id\": \"" + effectiveTraceId + "\",\n" +
                   "  \"propagation_status\": \"" + (valid ? "valid" : "invalid") + "\",\n" +
                   "  \"operation\": \"trace.validate\",\n" +
                   "  \"outcome\": \"" + (valid ? "success" : "failure") + "\"\n" +
                   "}";
        }

        return "trace_id=" + effectiveTraceId + " status=" + (valid ? "valid" : "invalid");
    }

    @ShellMethod(key = "metrics export", value = "Export current metrics names")
    public String metricsExport(
            @ShellOption(value = "--json", help = "Output as structured JSON", defaultValue = "false") boolean json
    ) {
        MeterRegistry registry = meterRegistryProvider.getIfAvailable();
        if (registry == null) {
            if (json) {
                return "{\n" +
                       "  \"metrics\": [],\n" +
                       "  \"operation\": \"metrics.export\",\n" +
                       "  \"outcome\": \"success\"\n" +
                       "}";
            }
            return "No metrics registry available.";
        }

        List<String> metricNames = registry.getMeters()
                .stream()
                .map(Meter::getId)
                .map(Meter.Id::getName)
                .distinct()
                .sorted()
                .toList();

        if (json) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n  \"metrics\": [");
            for (int i = 0; i < metricNames.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append("\n    \"").append(metricNames.get(i)).append("\"");
            }
            sb.append("\n  ],\n")
              .append("  \"operation\": \"metrics.export\",\n")
              .append("  \"outcome\": \"success\"\n")
              .append("}");
            return sb.toString();
        }

        return String.join("\n", metricNames);
    }

    private boolean isTraceIdFormatValid(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            return false;
        }
        if (traceId.startsWith("trace-")) {
            String value = traceId.substring("trace-".length());
            try {
                UUID.fromString(value);
                return true;
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }
        try {
            UUID.fromString(traceId);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}