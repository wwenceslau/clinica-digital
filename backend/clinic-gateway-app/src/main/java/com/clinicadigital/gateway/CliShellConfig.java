package com.clinicadigital.gateway;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.jline.PromptProvider;

/**
 * Spring Shell configuration for the clinic-gateway boot application.
 *
 * <p>Integrates all {@code @ShellComponent} beans discovered in the
 * {@code com.clinicadigital} package tree (including {@code TenantCommands}
 * from {@code clinic-tenant-core}) via the Spring Boot component scan
 * declared in {@link ClinicGatewayApplication}.
 *
 * <p>References: Art. II (CLI as first-class interaction boundary).
 */
@Configuration
public class CliShellConfig {

    /**
     * Provides a branded interactive prompt for the Spring Shell session.
     *
     * @return a yellow {@code "clinica-digital:> "} prompt
     */
    @Bean
    public PromptProvider clinicShellPrompt() {
        return () -> new AttributedString(
                "clinica-digital:> ",
                AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
    }
}
