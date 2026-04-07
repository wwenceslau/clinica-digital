package com.clinicadigital.shared.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CLIMetricsTest {

    @Test
    void shouldRecordExecutionTimeAndSuccessCount() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        CLIMetrics metrics = new CLIMetrics(registry);

        String output = metrics.record("tenant.list", () -> "ok");

        assertEquals("ok", output);

        Timer timer = registry.find("cli.command.execution.time")
                .tags("command", "tenant.list", "outcome", "success")
                .timer();
        Counter counter = registry.find("cli.command.execution.count")
                .tags("command", "tenant.list", "outcome", "success")
                .counter();

        assertNotNull(timer);
        assertNotNull(counter);
        assertEquals(1L, timer.count());
        assertEquals(1.0d, counter.count());
    }

    @Test
    void shouldRecordExecutionTimeAndFailureCountWhenCommandThrows() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        CLIMetrics metrics = new CLIMetrics(registry);

        assertThrows(IllegalStateException.class,
                () -> metrics.record("tenant.block", () -> {
                    throw new IllegalStateException("boom");
                }));

        Timer timer = registry.find("cli.command.execution.time")
                .tags("command", "tenant.block", "outcome", "failure")
                .timer();
        Counter counter = registry.find("cli.command.execution.count")
                .tags("command", "tenant.block", "outcome", "failure")
                .counter();

        assertNotNull(timer);
        assertNotNull(counter);
        assertEquals(1L, timer.count());
        assertEquals(1.0d, counter.count());
    }
}