package com.clinicadigital.iam.application;

import com.clinicadigital.iam.domain.Location;
import com.clinicadigital.iam.domain.Organization;
import com.clinicadigital.iam.domain.Practitioner;
import com.clinicadigital.iam.domain.PractitionerRole;
import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T160 [Phase 18] Validates JPA parity for V203 optional FHIR columns.
 *
 * This test protects schema->entity alignment for columns introduced in
 * V203__align_schema_with_spec_004_data_model.sql.
 */
class SchemaJpaParityV203IntegrationTest {

    @Test
    void organizationShouldMapOptionalV203Columns() {
        Set<String> columns = columnNames(Organization.class);

        assertContainsAll(columns, List.of(
                "fhir_type_json",
                "fhir_alias_json",
                "fhir_telecom_json",
                "fhir_address_json",
                "fhir_part_of_org_id",
            "fhir_endpoint_refs_json"));
    }

    @Test
    void locationShouldMapOptionalV203Columns() {
        Set<String> columns = columnNames(Location.class);

        assertContainsAll(columns, List.of(
                "fhir_telecom_json",
            "fhir_address_json"));
    }

    @Test
    void practitionerShouldMapOptionalV203Columns() {
        Set<String> columns = columnNames(Practitioner.class);

        assertContainsAll(columns, List.of(
                "fhir_telecom_json",
                "fhir_address_json",
                "fhir_gender",
                "fhir_birth_date",
                "fhir_qualification_json",
            "fhir_communication_json"));
    }

    @Test
    void practitionerRoleShouldMapOptionalV203Columns() {
        Set<String> columns = columnNames(PractitionerRole.class);

        assertContainsAll(columns, List.of(
                "fhir_code_json",
                "fhir_specialty_json",
                "fhir_telecom_json",
            "fhir_available_time_json"));
    }

    private static Set<String> columnNames(Class<?> entityType) {
        return Arrays.stream(entityType.getDeclaredFields())
                .map(SchemaJpaParityV203IntegrationTest::columnName)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toSet());
    }

    private static String columnName(Field field) {
        Column column = field.getAnnotation(Column.class);
        if (column == null) {
            return null;
        }
        return column.name();
    }

    private static void assertContainsAll(Set<String> actual, List<String> expected) {
        assertTrue(actual.containsAll(expected),
                () -> "Missing mapped columns. expected=" + expected + ", actual=" + actual);
    }
}
