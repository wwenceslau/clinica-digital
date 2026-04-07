package com.clinicadigital.observability.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * T070 [US2] Contract test for clinic-observability-core public API.
 */
class ObservabilityCorePublicAPITest {

    private static final Path MAIN_JAVA = Paths.get("src/main/java").toAbsolutePath().normalize();

    private static final Set<String> EXPECTED_PUBLIC_TYPES = Set.of(
            "com.clinicadigital.observability.JsonLogger",
            "com.clinicadigital.observability.cli.ObservabilityCommands",
            "com.clinicadigital.observability.TenantAwareMeterRegistry"
    );

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;", Pattern.MULTILINE);
    private static final Pattern TYPE_PATTERN = Pattern.compile("^\\s*public\\s+(?:final\\s+)?(?:class|interface|record|enum)\\s+(\\w+)\\b", Pattern.MULTILINE);

    @Test
    void observabilityCoreMustExposeOnlyExpectedPublicTypes() throws IOException {
        Set<String> actual = new TreeSet<>();

        try (Stream<Path> paths = Files.walk(MAIN_JAVA)) {
            paths.filter(p -> p.toString().endsWith(".java"))
                    .forEach(path -> collectPublicType(path, actual));
        }

        assertEquals(new TreeSet<>(EXPECTED_PUBLIC_TYPES), actual,
                "Public API drift detected in clinic-observability-core");
    }

    private static void collectPublicType(Path file, Set<String> sink) {
        try {
            String source = Files.readString(file);
            Matcher packageMatcher = PACKAGE_PATTERN.matcher(source);
            Matcher typeMatcher = TYPE_PATTERN.matcher(source);

            if (packageMatcher.find() && typeMatcher.find()) {
                sink.add(packageMatcher.group(1) + "." + typeMatcher.group(1));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read source file: " + file, ex);
        }
    }
}
