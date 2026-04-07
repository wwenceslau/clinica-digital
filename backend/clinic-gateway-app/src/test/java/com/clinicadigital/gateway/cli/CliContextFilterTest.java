package com.clinicadigital.gateway.cli;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.clinicadigital.shared.api.TenantContext;
import com.clinicadigital.shared.api.TenantContextStore;
import com.clinicadigital.shared.metrics.CLIMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.shell.standard.ShellMethod;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CliContextFilterTest {

    private ListAppender<ILoggingEvent> appender;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        TenantContextStore.clear();
        MDC.clear();
        meterRegistry = new SimpleMeterRegistry();

        Logger logger = (Logger) LoggerFactory.getLogger(CliContextFilter.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        Logger logger = (Logger) LoggerFactory.getLogger(CliContextFilter.class);
        logger.detachAppender(appender);
        meterRegistry.close();
        TenantContextStore.clear();
        MDC.clear();
    }

    @Test
    void shouldLogJsonEntryAndExitOnSuccessfulCommand() throws Throwable {
        UUID tenantId = UUID.randomUUID();
        TenantContextStore.set(TenantContext.from(tenantId));

        ProceedingJoinPoint joinPoint = mockJoinPoint("TenantCommands.list()", "ok");
        ShellMethod shellMethod = mockShellMethod("tenant list");

        CliContextFilter filter = new CliContextFilter(new CLIMetrics(meterRegistry));
        Object result = filter.aroundCliCommand(joinPoint, shellMethod);

        assertEquals("ok", result);
        List<ILoggingEvent> logs = appender.list;
        assertEquals(2, logs.size());
        assertTrue(logs.get(0).getFormattedMessage().contains("\"event\":\"cli.entry\""));
        assertTrue(logs.get(1).getFormattedMessage().contains("\"event\":\"cli.exit\""));
        assertTrue(logs.get(1).getFormattedMessage().contains("\"outcome\":\"success\""));
        assertTrue(logs.get(0).getFormattedMessage().contains(tenantId.toString()));
        Timer timer = meterRegistry.find("cli.command.execution.time")
            .tags("command", "tenant list", "outcome", "success")
            .timer();
        Counter counter = meterRegistry.find("cli.command.execution.count")
            .tags("command", "tenant list", "outcome", "success")
            .counter();
        assertEquals(1L, timer.count());
        assertEquals(1.0d, counter.count());
        assertNull(MDC.get("operation"));
        assertNull(MDC.get("trace_id"));
        assertNull(MDC.get("tenant_id"));
    }

    @Test
    void shouldLogJsonExitAsFailureWhenCommandThrows() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPointWithError("TenantCommands.block()", new IllegalArgumentException("denied"));
        ShellMethod shellMethod = mockShellMethod("tenant block");

        CliContextFilter filter = new CliContextFilter(new CLIMetrics(meterRegistry));
        assertThrows(IllegalArgumentException.class, () -> filter.aroundCliCommand(joinPoint, shellMethod));

        List<ILoggingEvent> logs = appender.list;
        assertEquals(2, logs.size());
        assertTrue(logs.get(0).getFormattedMessage().contains("\"event\":\"cli.entry\""));
        assertTrue(logs.get(1).getFormattedMessage().contains("\"event\":\"cli.exit\""));
        assertTrue(logs.get(1).getFormattedMessage().contains("\"outcome\":\"failure\""));
        Timer timer = meterRegistry.find("cli.command.execution.time")
            .tags("command", "tenant block", "outcome", "failure")
            .timer();
        Counter counter = meterRegistry.find("cli.command.execution.count")
            .tags("command", "tenant block", "outcome", "failure")
            .counter();
        assertEquals(1L, timer.count());
        assertEquals(1.0d, counter.count());
    }

    private static ProceedingJoinPoint mockJoinPoint(String shortSignature, Object value) throws Throwable {
        ProceedingJoinPoint joinPoint = Mockito.mock(ProceedingJoinPoint.class);
        Signature signature = Mockito.mock(Signature.class);
        Mockito.when(signature.toShortString()).thenReturn(shortSignature);
        Mockito.when(joinPoint.getSignature()).thenReturn(signature);
        Mockito.when(joinPoint.proceed()).thenReturn(value);
        return joinPoint;
    }

    private static ProceedingJoinPoint mockJoinPointWithError(String shortSignature, RuntimeException error) throws Throwable {
        ProceedingJoinPoint joinPoint = Mockito.mock(ProceedingJoinPoint.class);
        Signature signature = Mockito.mock(Signature.class);
        Mockito.when(signature.toShortString()).thenReturn(shortSignature);
        Mockito.when(joinPoint.getSignature()).thenReturn(signature);
        Mockito.when(joinPoint.proceed()).thenThrow(error);
        return joinPoint;
    }

    private static ShellMethod mockShellMethod(String commandKey) {
        ShellMethod shellMethod = Mockito.mock(ShellMethod.class);
        Mockito.when(shellMethod.key()).thenReturn(new String[]{commandKey});
        return shellMethod;
    }
}