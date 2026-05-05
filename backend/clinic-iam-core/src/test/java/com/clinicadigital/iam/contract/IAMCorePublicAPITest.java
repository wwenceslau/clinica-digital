package com.clinicadigital.iam.contract;

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
 * T068 [US2] Contract test for clinic-iam-core public API.
 *
 * The contract starts strict: iam-core should expose only its planned public surface.
 */
class IAMCorePublicAPITest {

    private static final Path MAIN_JAVA = Paths.get("src/main/java").toAbsolutePath().normalize();
        private static final Set<String> EXPECTED_PUBLIC_TYPES = Set.of(
            // Phase 5.B.1 domain and persistence public contracts
            "com.clinicadigital.iam.application.AuthenticationService",
            "com.clinicadigital.iam.application.AuditService",
            "com.clinicadigital.iam.application.PasswordService",
            "com.clinicadigital.iam.application.SessionManager",
            "com.clinicadigital.iam.domain.IamAuditEvent",
            "com.clinicadigital.iam.domain.IamAuditEventRepository",
            "com.clinicadigital.iam.domain.IamRole",
            "com.clinicadigital.iam.domain.IamSession",
            "com.clinicadigital.iam.domain.IamSessionRepository",
            "com.clinicadigital.iam.domain.IamUser",
            "com.clinicadigital.iam.domain.IamUserRepository",
            "com.clinicadigital.iam.domain.IIamSessionRepository",
            // Phase 2 (Foundation) additions — T008, T010, T013, T014, T017
            "com.clinicadigital.iam.application.EncryptionKeyProvider",
            "com.clinicadigital.iam.application.ExternalSecretEncryptionKeyProvider",
            "com.clinicadigital.iam.application.IamOperationOutcomeFactory",
            "com.clinicadigital.iam.application.IdentifierSystemResolver",
            "com.clinicadigital.iam.application.PiiCryptoService",
            "com.clinicadigital.iam.application.RbacPermissionMap",
            "com.clinicadigital.iam.application.RndsStructureDefinitionRegistry"
        );

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;", Pattern.MULTILINE);
    private static final Pattern TYPE_PATTERN = Pattern.compile("^\\s*public\\s+(?:final\\s+)?(?:class|interface|record|enum)\\s+(\\w+)\\b", Pattern.MULTILINE);

    @Test
    void iamCoreMustExposeOnlyExpectedPublicTypes() throws IOException {
        Set<String> actual = new TreeSet<>();
        if (Files.exists(MAIN_JAVA)) {
            try (Stream<Path> paths = Files.walk(MAIN_JAVA)) {
                paths.filter(p -> p.toString().endsWith(".java"))
                        .forEach(path -> collectPublicType(path, actual));
            }
        }

        assertEquals(new TreeSet<>(EXPECTED_PUBLIC_TYPES), actual,
                "Public API drift detected in clinic-iam-core");
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
