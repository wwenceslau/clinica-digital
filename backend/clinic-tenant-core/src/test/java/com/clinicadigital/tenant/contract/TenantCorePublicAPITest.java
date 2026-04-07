package com.clinicadigital.tenant.contract;

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
 * T066 [US2] Contract test for clinic-tenant-core public API.
 *
 * Validates that tenant-core exposes only the expected public surface.
 */
class TenantCorePublicAPITest {

    private static final Path MAIN_JAVA = Paths.get("src/main/java").toAbsolutePath().normalize();

    private static final Set<String> EXPECTED_PUBLIC_TYPES = Set.of(
            "com.clinicadigital.tenant.application.QuotaExceededException",
            "com.clinicadigital.tenant.application.QuotaService",
            "com.clinicadigital.tenant.application.TenantService",
            "com.clinicadigital.tenant.cli.QuotaCommands",
            "com.clinicadigital.tenant.cli.TenantCommands",
            "com.clinicadigital.tenant.domain.ITenantRepository",
            "com.clinicadigital.tenant.domain.Tenant",
            "com.clinicadigital.tenant.infrastructure.TenantRepository"
    );

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;", Pattern.MULTILINE);
    private static final Pattern TYPE_PATTERN = Pattern.compile("^\\s*public\\s+(?:final\\s+)?(?:class|interface|record|enum)\\s+(\\w+)\\b", Pattern.MULTILINE);

    @Test
    void tenantCoreMustExposeOnlyExpectedPublicTypes() throws IOException {
        Set<String> actual = new TreeSet<>();

        try (Stream<Path> paths = Files.walk(MAIN_JAVA)) {
            paths.filter(p -> p.toString().endsWith(".java"))
                    .forEach(path -> collectPublicType(path, actual));
        }

        assertEquals(new TreeSet<>(EXPECTED_PUBLIC_TYPES), actual,
                "Public API drift detected in clinic-tenant-core");
    }

    private static void collectPublicType(Path file, Set<String> sink) {
        try {
            String source = Files.readString(file);
            Matcher packageMatcher = PACKAGE_PATTERN.matcher(source);
            Matcher typeMatcher = TYPE_PATTERN.matcher(source);

            if (packageMatcher.find() && typeMatcher.find()) {
                String qualifiedName = packageMatcher.group(1) + "." + typeMatcher.group(1);
                sink.add(qualifiedName);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read source file: " + file, ex);
        }
    }
}
