package com.clinicadigital.iam.application;

import com.clinicadigital.shared.api.OperationOutcome;
import org.springframework.stereotype.Component;

@Component
public class IamOperationOutcomeFactory {

    public OperationOutcome error(String code, String detailsText, String diagnostics) {
        return OperationOutcome.builder()
                .addIssue(OperationOutcome.Severity.ERROR, code, new OperationOutcome.Details(detailsText), diagnostics)
                .build();
    }
}
