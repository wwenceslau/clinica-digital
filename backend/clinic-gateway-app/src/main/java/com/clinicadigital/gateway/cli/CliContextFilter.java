package com.clinicadigital.gateway.cli;

import com.clinicadigital.shared.api.TenantContext;
import com.clinicadigital.shared.api.TenantContextStore;
import com.clinicadigital.shared.api.TraceContext;
import com.clinicadigital.shared.metrics.CLIMetrics;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Logs structured JSON for CLI command entry and exit.
 */
@Aspect
@Component
public class CliContextFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CliContextFilter.class);
    private final CLIMetrics cliMetrics;

    public CliContextFilter(CLIMetrics cliMetrics) {
        this.cliMetrics = cliMetrics;
    }

    @Around("@annotation(shellMethod)")
    public Object aroundCliCommand(ProceedingJoinPoint joinPoint, ShellMethod shellMethod) throws Throwable {
        Map<String, String> previous = MDC.getCopyOfContextMap();

        String command = resolveCommand(shellMethod, joinPoint);
        String operation = "cli." + command.replace(' ', '.');
        String traceId = resolveTraceId();
        String tenantId = resolveTenantId();

        try {
            MDC.put("trace_id", traceId);
            MDC.put("tenant_id", tenantId);
            MDC.put("operation", operation);

            LOGGER.info(json("cli.entry", operation, command, traceId, tenantId, "started", null));
            Object output = cliMetrics.recordThrowing(command, joinPoint::proceed);
            LOGGER.info(json("cli.exit", operation, command, traceId, tenantId, "success", null));
            return output;
        } catch (Throwable ex) {
            LOGGER.warn(json("cli.exit", operation, command, traceId, tenantId, "failure", ex.getMessage()));
            throw ex;
        } finally {
            if (previous == null || previous.isEmpty()) {
                MDC.clear();
            } else {
                MDC.setContextMap(previous);
            }
        }
    }

    private String resolveCommand(ShellMethod shellMethod, ProceedingJoinPoint joinPoint) {
        if (shellMethod != null && shellMethod.key().length > 0 && !shellMethod.key()[0].isBlank()) {
            return shellMethod.key()[0].trim();
        }
        return joinPoint.getSignature().toShortString();
    }

    private String resolveTraceId() {
        String current = MDC.get("trace_id");
        if (TraceContext.isValid(current)) {
            return current;
        }
        return TraceContext.generate().traceId();
    }

    private String resolveTenantId() {
        String current = MDC.get("tenant_id");
        if (current != null && !current.isBlank()) {
            return current;
        }
        TenantContext tenantContext = TenantContextStore.get();
        if (tenantContext != null) {
            return tenantContext.tenantId().toString();
        }
        return "unknown";
    }

    private String json(String event, String operation, String command, String traceId, String tenantId, String outcome, String error) {
        StringBuilder sb = new StringBuilder();
        sb.append("{")
                .append("\"event\":\"").append(escape(event)).append("\",")
                .append("\"operation\":\"").append(escape(operation)).append("\",")
                .append("\"command\":\"").append(escape(command)).append("\",")
                .append("\"trace_id\":\"").append(escape(traceId)).append("\",")
                .append("\"tenant_id\":\"").append(escape(tenantId)).append("\",")
                .append("\"outcome\":\"").append(escape(outcome)).append("\"");
        if (error != null && !error.isBlank()) {
            sb.append(",\"error\":\"").append(escape(error)).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}