import i18n from "i18next";
import { initReactI18next } from "react-i18next";

const resources = {
  "pt-BR": {
    translation: {
      "sidebar.domain.administration": "Administração",
      "sidebar.domain.professionals": "Profissionais",
      "sidebar.domain.patients": "Pacientes",
      "sidebar.domain.scheduling": "Agenda",
      "sidebar.domain.clinical-care": "Atendimento",
      "sidebar.domain.diagnostics-therapy": "Diagnóstico e Terapêutica",
      "sidebar.domain.prevention": "Prevenção",
      "sidebar.domain.billing": "Financeiro e Faturamento",
      "sidebar.domain.security": "Segurança",
      "sidebar.admin.tenant-settings": "Configurações do tenant",
      "sidebar.security.user-management": "Gestão de Usuários Internos",
      "sidebar.security.access-profiles": "Perfis de Acesso",
      "sidebar.security.audit": "Trilha de Auditoria",
      "header.tenant": "Clínica",
      "header.location": "Unidade",
      "header.profile": "Perfil",
      "telemetry.trace-id": "Trace ID",
      "telemetry.tenant-id": "Tenant ID",
      "a11y.permission-restricted": "Acesso restrito",
      "sidebar.collapse": "Recolher menu",
      "sidebar.expand": "Expandir menu",
      // ── US7: Erros RNDS / IAM — mensagens amigáveis ──────────────────────
      "error.rnds.unsupported-profile":
        "O perfil RNDS informado não é suportado. Verifique o código do estabelecimento.",
      "error.rnds.structure-violation":
        "Os dados não estão no formato esperado pelo RNDS. Verifique o cadastro.",
      "error.rnds.throttled":
        "Número máximo de tentativas excedido. Aguarde alguns minutos e tente novamente.",
      "error.rnds.cnes-invalid":
        "O CNES informado é inválido. Verifique o número do estabelecimento.",
      "error.rnds.cnes-already-registered":
        "O CNES informado já está cadastrado no sistema.",
      "error.iam.invalid-credentials":
        "E-mail ou senha incorretos. Verifique os dados e tente novamente.",
      "error.iam.account-locked":
        "Conta temporariamente bloqueada por excesso de tentativas. Tente novamente em 15 minutos.",
      "error.iam.no-organizations":
        "Nenhuma organização associada a este usuário. Entre em contato com o administrador.",
      "error.iam.session-expired":
        "Sua sessão expirou. Faça login novamente para continuar.",
      "error.iam.forbidden":
        "Você não tem permissão para realizar esta ação.",
      "error.network.connection":
        "Falha de conexão com o servidor. Verifique sua rede e tente novamente.",
      "error.generic":
        "Erro inesperado. Tente novamente ou contate o suporte se o problema persistir.",
      "tenantAdmin.title": "Administração de tenants",
      "tenantAdmin.actions.new": "Novo",
      "tenantAdmin.actions.refresh": "Atualizar",
      "tenantAdmin.actions.edit": "Editar",
      "tenantAdmin.actions.delete": "Excluir",
      "tenantAdmin.actions.cancel": "Cancelar",
      "tenantAdmin.actions.create": "Criar",
      "tenantAdmin.actions.save": "Salvar",
      "tenantAdmin.actions.saving": "Salvando...",
      "tenantAdmin.actions.deleteConfirm": "Confirma a exclusão deste tenant?",
      "tenantAdmin.grid.legalName": "Nome legal",
      "tenantAdmin.grid.slug": "CNES/Slug",
      "tenantAdmin.grid.status": "Status",
      "tenantAdmin.grid.plan": "Plano",
      "tenantAdmin.grid.actions": "Ações",
      "tenantAdmin.grid.empty": "Nenhum tenant cadastrado.",
      "tenantAdmin.modal.createTitle": "Novo tenant",
      "tenantAdmin.modal.editTitle": "Editar tenant",
      "tenantAdmin.modal.organizationSection": "Organização",
      "tenantAdmin.modal.organizationDisplayName": "Nome da organização",
      "tenantAdmin.modal.cnes": "CNES (7 dígitos)",
      "tenantAdmin.modal.adminSection": "Administrador",
      "tenantAdmin.modal.adminDisplayName": "Nome do admin",
      "tenantAdmin.modal.adminEmail": "E-mail do admin",
      "tenantAdmin.modal.adminCpf": "CPF (11 dígitos)",
      "tenantAdmin.modal.adminPassword": "Senha do admin",
      "tenantAdmin.validation.cnes": "CNES deve ter exatamente 7 dígitos",
      "tenantAdmin.validation.cpf": "CPF deve ter exatamente 11 dígitos",
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
      "sidebar.collapse": "Collapse menu",
      "sidebar.expand": "Expand menu",
      // ── US7: RNDS / IAM error messages ───────────────────────────────────
      "error.rnds.unsupported-profile":
        "The RNDS profile provided is not supported. Please verify the establishment code.",
      "error.rnds.structure-violation":
        "The data does not match the expected RNDS format. Please review the registration.",
      "error.rnds.throttled":
        "Maximum attempts exceeded. Please wait a few minutes and try again.",
      "error.rnds.cnes-invalid":
        "The CNES provided is invalid. Please verify the establishment number.",
      "error.rnds.cnes-already-registered":
        "The CNES provided is already registered in the system.",
      "error.iam.invalid-credentials":
        "Incorrect email or password. Please verify your credentials.",
      "error.iam.account-locked":
        "Account temporarily locked due to too many attempts. Try again in 15 minutes.",
      "error.iam.no-organizations":
        "No organizations associated with this user. Please contact your administrator.",
      "error.iam.session-expired":
        "Your session has expired. Please log in again to continue.",
      "error.iam.forbidden":
        "You do not have permission to perform this action.",
      "error.network.connection":
        "Failed to connect to the server. Please check your network connection.",
      "error.generic":
        "Unexpected error. Please try again or contact support if the problem persists.",
      "tenantAdmin.title": "Tenant administration",
      "tenantAdmin.actions.new": "New",
      "tenantAdmin.actions.refresh": "Refresh",
      "tenantAdmin.actions.edit": "Edit",
      "tenantAdmin.actions.delete": "Delete",
      "tenantAdmin.actions.cancel": "Cancel",
      "tenantAdmin.actions.create": "Create",
      "tenantAdmin.actions.save": "Save",
      "tenantAdmin.actions.saving": "Saving...",
      "tenantAdmin.actions.deleteConfirm": "Confirm deleting this tenant?",
      "tenantAdmin.grid.legalName": "Legal name",
      "tenantAdmin.grid.slug": "CNES/Slug",
      "tenantAdmin.grid.status": "Status",
      "tenantAdmin.grid.plan": "Plan",
      "tenantAdmin.grid.actions": "Actions",
      "tenantAdmin.grid.empty": "No tenants registered.",
      "tenantAdmin.modal.createTitle": "New tenant",
      "tenantAdmin.modal.editTitle": "Edit tenant",
      "tenantAdmin.modal.organizationSection": "Organization",
      "tenantAdmin.modal.organizationDisplayName": "Organization name",
      "tenantAdmin.modal.cnes": "CNES (7 digits)",
      "tenantAdmin.modal.adminSection": "Administrator",
      "tenantAdmin.modal.adminDisplayName": "Admin name",
      "tenantAdmin.modal.adminEmail": "Admin e-mail",
      "tenantAdmin.modal.adminCpf": "CPF (11 digits)",
      "tenantAdmin.modal.adminPassword": "Admin password",
      "tenantAdmin.validation.cnes": "CNES must be exactly 7 digits",
      "tenantAdmin.validation.cpf": "CPF must be exactly 11 digits",
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
