/**
 * T062 [US7] Adapter unificado de erro no frontend.
 *
 * Converte qualquer resposta de erro (OperationOutcome, IamAuthError, erro
 * genérico) em uma estrutura de exibição coerente com:
 *   - userMessage: mensagem amigável em pt-BR para o usuário final
 *   - technicalDetails: diagnóstico técnico preservado para rastreabilidade
 *   - code: código FHIR original (null se ausente)
 *
 * Estratégia de tradução: usa chaves i18n para códigos RNDS/IAM conhecidos;
 * caso não encontre mapeamento, usa o diagnostics original diretamente.
 *
 * Compatível com:
 *   - OperationOutcome (FHIR R4) de qualquer endpoint
 *   - IamAuthError (iamAuthApi)
 *   - ClinicRegistrationError (clinicRegistrationApi)
 *   - Erros genéricos (Error, string, unknown)
 *
 * Refs: FR-009, FR-015, SC-002
 */

import type { OperationOutcome, OperationOutcomeIssue } from './clinicRegistrationApi';

export type { OperationOutcome, OperationOutcomeIssue };

/** Shape retornada para consumo nos componentes. */
export interface ParsedOutcome {
  /** Mensagem amigável exibida ao usuário. */
  userMessage: string;
  /** Diagnóstico técnico (para logs/telemetria). Nunca vazio. */
  technicalDetails: string;
  /** Código FHIR original, ou null se ausente. */
  code: string | null;
}

const FALLBACK_MESSAGE = 'Erro inesperado. Tente novamente.';

// ── Mapeamento de códigos FHIR/IAM → mensagem amigável ───────────────────────

const CODE_MESSAGES: Record<string, string> = {
  throttled:
    'Número máximo de tentativas excedido. Aguarde alguns minutos e tente novamente.',
  conflict: 'O registro já está cadastrado no sistema.',
  forbidden:
    'Acesso não autorizado. Verifique suas credenciais ou permissões.',
  security:
    'Acesso não autorizado. Verifique suas credenciais ou permissões.',
  invalid: 'Os dados informados são inválidos. Verifique e tente novamente.',
  exception: null as unknown as string, // usa diagnostics original
  'not-found': 'Recurso não encontrado.',
  timeout:
    'A operação demorou mais do que o esperado. Tente novamente em instantes.',
};

// Padrões em diagnostics → mensagem amigável
const DIAGNOSTICS_PATTERNS: Array<[RegExp, string]> = [
  [
    /unsupported RNDS StructureDefinition profile/i,
    'O perfil RNDS informado não é suportado. Verifique o código do estabelecimento.',
  ],
  [
    /tenant already exists|cnes já cadastrado|already registered/i,
    'O CNES informado já está cadastrado no sistema.',
  ],
  [
    /limite de tentativas|tentativas excedido|rate.?limit/i,
    'Número máximo de tentativas excedido. Aguarde alguns minutos e tente novamente.',
  ],
  [
    /invalid credentials|credenciais inválidas|wrong password/i,
    'E-mail ou senha incorretos. Verifique os dados e tente novamente.',
  ],
  [
    /connection|conexão|network/i,
    'Falha de conexão com o servidor. Verifique sua rede e tente novamente.',
  ],
];

// ── Helpers internos ─────────────────────────────────────────────────────────

function diagnoseIssue(issue: OperationOutcomeIssue): string {
  return issue.diagnostics?.trim() || issue.code || FALLBACK_MESSAGE;
}

function resolveUserMessage(issue: OperationOutcomeIssue): string {
  // 1. Checa padrões em diagnostics
  const raw = issue.diagnostics ?? '';
  for (const [pattern, msg] of DIAGNOSTICS_PATTERNS) {
    if (pattern.test(raw)) return msg;
  }

  // 2. Checa mapeamento de código
  const codeKey = (issue.code ?? '').toLowerCase();
  if (codeKey in CODE_MESSAGES && CODE_MESSAGES[codeKey] !== null) {
    return CODE_MESSAGES[codeKey];
  }

  // 3. Fallback: usa diagnostics original
  return raw || issue.code || FALLBACK_MESSAGE;
}

// ── API pública ──────────────────────────────────────────────────────────────

/**
 * Extrai a mensagem principal de um OperationOutcome para exibição simples.
 * Múltiplos issues são separados por " • ".
 *
 * @param outcome - OperationOutcome ou null/undefined
 * @returns string pronta para exibição
 */
export function extractErrorMessage(outcome: OperationOutcome | null | undefined): string {
  if (!outcome || !outcome.issue || outcome.issue.length === 0) {
    return FALLBACK_MESSAGE;
  }
  return outcome.issue.map(diagnoseIssue).join(' • ');
}

/**
 * Converte um OperationOutcome em ParsedOutcome com mensagem amigável +
 * diagnóstico técnico preservado para rastreabilidade.
 *
 * @param outcome - OperationOutcome ou null/undefined
 * @returns ParsedOutcome pronto para consumo nos componentes
 */
export function toDisplayMessage(outcome: OperationOutcome | null | undefined): ParsedOutcome {
  if (!outcome || !outcome.issue || outcome.issue.length === 0) {
    return {
      userMessage: FALLBACK_MESSAGE,
      technicalDetails: FALLBACK_MESSAGE,
      code: null,
    };
  }

  const primaryIssue = outcome.issue[0];
  const technicalDetails = outcome.issue.map(diagnoseIssue).join(' • ');
  const userMessage =
    outcome.issue.length === 1
      ? resolveUserMessage(primaryIssue)
      : outcome.issue.map(resolveUserMessage).join(' • ');

  return {
    userMessage,
    technicalDetails,
    code: primaryIssue.code ?? null,
  };
}

/**
 * Converte qualquer erro capturado em catch block em ParsedOutcome.
 * Suporta OperationOutcome aninhado (ex: IamAuthError, ClinicRegistrationError).
 *
 * @param error - Valor capturado (unknown)
 * @returns ParsedOutcome coerente
 */
export function fromCaughtError(error: unknown): ParsedOutcome {
  if (error == null) {
    return { userMessage: FALLBACK_MESSAGE, technicalDetails: FALLBACK_MESSAGE, code: null };
  }

  // IamAuthError / ClinicRegistrationError: { status, body: OperationOutcome | null }
  if (
    typeof error === 'object' &&
    'body' in error &&
    (error as { body: unknown }).body !== null &&
    typeof (error as { body: unknown }).body === 'object' &&
    (error as { body: { resourceType?: unknown } }).body &&
    (error as { body: { resourceType: unknown } }).body.resourceType === 'OperationOutcome'
  ) {
    return toDisplayMessage((error as { body: OperationOutcome }).body);
  }

  // Bare OperationOutcome
  if (
    typeof error === 'object' &&
    'resourceType' in error &&
    (error as { resourceType: unknown }).resourceType === 'OperationOutcome'
  ) {
    return toDisplayMessage(error as OperationOutcome);
  }

  // Error object
  if (error instanceof Error) {
    const msg = error.message || FALLBACK_MESSAGE;
    return { userMessage: msg, technicalDetails: msg, code: null };
  }

  // String
  if (typeof error === 'string') {
    return { userMessage: error || FALLBACK_MESSAGE, technicalDetails: error || FALLBACK_MESSAGE, code: null };
  }

  return { userMessage: FALLBACK_MESSAGE, technicalDetails: FALLBACK_MESSAGE, code: null };
}
