import i18n from "i18next";
import { initReactI18next } from "react-i18next";

const resources = {
  "pt-BR": {
    translation: {
      "sidebar.domain.administration": "Administracao",
      "sidebar.domain.professionals": "Profissionais",
      "sidebar.domain.patients": "Pacientes",
      "sidebar.domain.scheduling": "Agenda",
      "sidebar.domain.clinical-care": "Atendimento",
      "sidebar.domain.diagnostics-therapy": "Diagnostico e Terapeutica",
      "sidebar.domain.prevention": "Prevencao",
      "sidebar.domain.billing": "Financeiro e Faturamento",
      "sidebar.domain.security": "Seguranca",
      "sidebar.admin.tenant-settings": "Configuracoes do tenant",
      "sidebar.security.user-management": "Gestao de Usuarios Internos",
      "sidebar.security.access-profiles": "Perfis de Acesso",
      "sidebar.security.audit": "Trilha de Auditoria",
      "header.tenant": "Clinica",
      "header.location": "Unidade",
      "header.profile": "Perfil",
      "telemetry.trace-id": "Trace ID",
      "telemetry.tenant-id": "Tenant ID",
      "a11y.permission-restricted": "Acesso restrito",
    },
  },
  "en-US": {
    translation: {
      "sidebar.domain.administration": "Administration",
      "sidebar.domain.professionals": "Professionals",
      "sidebar.domain.patients": "Patients",
      "sidebar.domain.scheduling": "Scheduling",
      "sidebar.domain.clinical-care": "Clinical Care",
      "sidebar.domain.diagnostics-therapy": "Diagnostics and Therapy",
      "sidebar.domain.prevention": "Prevention",
      "sidebar.domain.billing": "Billing and Revenue",
      "sidebar.domain.security": "Security",
      "sidebar.admin.tenant-settings": "Tenant settings",
      "sidebar.security.user-management": "User management",
      "sidebar.security.access-profiles": "Access profiles",
      "sidebar.security.audit": "Audit trail",
      "header.tenant": "Clinic",
      "header.location": "Location",
      "header.profile": "Profile",
      "telemetry.trace-id": "Trace ID",
      "telemetry.tenant-id": "Tenant ID",
      "a11y.permission-restricted": "Restricted access",
    },
  },
} as const;

i18n.use(initReactI18next).init({
  resources,
  lng: "pt-BR",
  fallbackLng: "pt-BR",
  interpolation: {
    escapeValue: false,
  },
});

export default i18n;
