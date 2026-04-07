package com.clinicadigital.shared.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Records standardized metrics for CLI command execution.
 */
@Component
public class CLIMetrics {

    private static final String TIMER_NAME = "cli.command.execution.time";
    private static final String COUNTER_NAME = "cli.command.execution.count";

    private final MeterRegistry meterRegistry;

    public CLIMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public <T> T record(String command, Supplier<T> execution) {
        String normalizedCommand = normalize(command);
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "success";
        try {
            T result = execution.get();
            counter(normalizedCommand, outcome).increment();
            return result;
        } catch (RuntimeException ex) {
            outcome = "failure";
            counter(normalizedCommand, outcome).increment();
            throw ex;
        } finally {
            sample.stop(timer(normalizedCommand, outcome));
        }
    }

    public <T> T recordThrowing(String command, CheckedSupplier<T> execution) throws Throwable {
        String normalizedCommand = normalize(command);
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "success";
        try {
            T result = execution.get();
            counter(normalizedCommand, outcome).increment();
            return result;
        } catch (Throwable ex) {
            outcome = "failure";
            counter(normalizedCommand, outcome).increment();
            throw ex;
        } finally {
            sample.stop(timer(normalizedCommand, outcome));
        }
    }

    private Timer timer(String command, String outcome) {
        return Timer.builder(TIMER_NAME)
                .tag("command", command)
                .tag("outcome", outcome)
                .register(meterRegistry);
    }

    private Counter counter(String command, String outcome) {
        return Counter.builder(COUNTER_NAME)
                .tag("command", command)
                .tag("outcome", outcome)
                .register(meterRegistry);
    }

    private String normalize(String command) {
        if (command == null || command.isBlank()) {
            return "unknown";
        }
        return command.trim().replaceAll("\\s+", " ");
    }

    @FunctionalInterface
    public interface CheckedSupplier<T> {
        T get() throws Throwable;
    }
}