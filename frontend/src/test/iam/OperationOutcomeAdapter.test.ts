/**
 * T060 [P] [US7] Unit test: OperationOutcome parser no frontend.
 *
 * Verifica que o adapter unificado de erro (T062) converte corretamente
 * respostas de erro (OperationOutcome) em mensagens exibíveis ao usuário:
 *
 * 1. Extrai diagnostics de issues únicas.
 * 2. Concatena múltiplos issues.
 * 3. Fallback quando diagnostics está ausente (usa issue.code).
 * 4. Retorna mensagem genérica quando outcome é nulo ou vazio.
 * 5. Aplica tradução i18n para códigos RNDS conhecidos.
 * 6. Preserva rastreabilidade (não suprime diagnostics técnico).
 *
 * Refs: FR-009, FR-015, SC-002
 */

import { describe, it, expect } from 'vitest';
import {
  extractErrorMessage,
  toDisplayMessage,
  type ParsedOutcome,
} from '../../services/operationOutcomeAdapter';

describe('extractErrorMessage', () => {
  it('retorna diagnostics da primeira issue', () => {
    const result = extractErrorMessage({
      resourceType: 'OperationOutcome',
      issue: [{ severity: 'error', code: 'invalid', diagnostics: 'Campo obrigatório ausente.' }],
    });
    expect(result).toBe('Campo obrigatório ausente.');
  });

  it('concatena múltiplas issues com bullet separator', () => {
    const result = extractErrorMessage({
      resourceType: 'OperationOutcome',
      issue: [
        { severity: 'error', code: 'invalid', diagnostics: 'CPF inválido.' },
        { severity: 'error', code: 'invalid', diagnostics: 'CNES inválido.' },
      ],
    });
    expect(result).toBe('CPF inválido. • CNES inválido.');
  });

  it('usa issue.code como fallback quando diagnostics está ausente', () => {
    const result = extractErrorMessage({
      resourceType: 'OperationOutcome',
      issue: [{ severity: 'error', code: 'security' }],
    });
    expect(result).toBe('security');
  });

  it('retorna mensagem genérica quando outcome é nulo', () => {
    const result = extractErrorMessage(null);
    expect(result).toBe('Erro inesperado. Tente novamente.');
  });

  it('retorna mensagem genérica quando issue array está vazio', () => {
    const result = extractErrorMessage({
      resourceType: 'OperationOutcome',
      issue: [],
    });
    expect(result).toBe('Erro inesperado. Tente novamente.');
  });
});

describe('toDisplayMessage', () => {
  it('traduz código RNDS unsupported-profile para mensagem amigável', () => {
    const result = toDisplayMessage({
      resourceType: 'OperationOutcome',
      issue: [
        {
          severity: 'error',
          code: 'invalid',
          diagnostics: 'unsupported RNDS StructureDefinition profile: http://example.com/bad',
        },
      ],
    });
    expect(result.userMessage).toContain('perfil RNDS');
    // rastreabilidade preservada
    expect(result.technicalDetails).toContain('unsupported RNDS StructureDefinition profile');
  });

  it('traduz código throttled para mensagem de rate-limit amigável', () => {
    const result = toDisplayMessage({
      resourceType: 'OperationOutcome',
      issue: [{ severity: 'error', code: 'throttled', diagnostics: 'Limite de tentativas excedido. Tente novamente em 15 minutos.' }],
    });
    expect(result.userMessage).toContain('tentativas');
    expect(result.technicalDetails).not.toBe('');
  });

  it('traduz código conflict para mensagem amigável de duplicidade', () => {
    const result = toDisplayMessage({
      resourceType: 'OperationOutcome',
      issue: [{ severity: 'error', code: 'conflict', diagnostics: 'Tenant already exists: cnes=1234567' }],
    });
    expect(result.userMessage).toContain('cadastrado');
    expect(result.technicalDetails).toContain('cnes=1234567');
  });

  it('passa mensagem genérica sem transformação para erros desconhecidos', () => {
    const result = toDisplayMessage({
      resourceType: 'OperationOutcome',
      issue: [{ severity: 'error', code: 'exception', diagnostics: 'Unexpected server error.' }],
    });
    expect(result.userMessage).toBe('Unexpected server error.');
    expect(result.technicalDetails).toBe('Unexpected server error.');
  });

  it('retorna objeto ParsedOutcome válido com fallbacks quando outcome é nulo', () => {
    const result = toDisplayMessage(null);
    expect(result).toMatchObject<Partial<ParsedOutcome>>({
      userMessage: expect.any(String),
      technicalDetails: expect.any(String),
      code: null,
    });
    expect(result.userMessage.length).toBeGreaterThan(0);
  });
});
