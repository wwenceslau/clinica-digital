package com.clinicadigital.shared.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Minimal FHIR-aligned outcome skeleton to be evolved in Phase 2 TDD tasks.
 */
public record OperationOutcome(List<Issue> issue) {

    public OperationOutcome {
        if (issue == null || issue.isEmpty()) {
            throw new IllegalArgumentException("issue must not be empty");
        }
        issue = List.copyOf(issue);
    }

    public static Builder builder() {
        return new Builder();
    }

    public enum Severity {
        FATAL,
        ERROR,
        WARNING,
        INFORMATION
    }

    public record Issue(Severity severity, String code, String diagnostics) {

        public Issue {
            if (severity == null) {
                throw new IllegalArgumentException("severity must not be null");
            }
            if (code == null || code.isBlank()) {
                throw new IllegalArgumentException("code must not be blank");
            }
            if (diagnostics == null || diagnostics.isBlank()) {
                throw new IllegalArgumentException("diagnostics must not be blank");
            }
        }
    }

    public static final class Builder {
        private final List<Issue> issues = new ArrayList<>();

        public Builder addIssue(Severity severity, String code, String diagnostics) {
            issues.add(new Issue(severity, code, diagnostics));
            return this;
        }

        public OperationOutcome build() {
            if (issues.isEmpty()) {
                throw new IllegalStateException("At least one issue is required");
            }
            return new OperationOutcome(Collections.unmodifiableList(new ArrayList<>(issues)));
        }
    }
}
