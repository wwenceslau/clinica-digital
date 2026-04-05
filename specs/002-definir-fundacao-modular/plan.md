# Implementation Plan: Fundacao e Organizacao Modular da Plataforma Clinica

**Branch**: `002-definir-fundacao-modular` | **Date**: 2026-04-05 | **Spec**: `/specs/002-definir-fundacao-modular/spec.md`
**Input**: Feature specification from `/specs/002-definir-fundacao-modular/spec.md`

## Summary

Este plano define a fundacao do projeto Clinica Digital com backend Maven multi-module em Java 21/Spring Boot 3.x e frontend React 19/TypeScript com Atomic Components. As decisoes nucleares sao: IAM 100% nativo in-app (sem IdP externo), sessao stateful com token opaco em PostgreSQL 15 sob RLS, propagacao obrigatoria de tenant_id e trace_id em HTTP/CLI/eventos, baseline de resiliencia com timeout, circuit breaker, retry limitado e fail-closed para operacoes sensiveis, e baseline de desenvolvimento com PostgreSQL 15+ nativo no Windows 11 via perfil Spring `dev` com segredos externalizados.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x (frontend)  
**Primary Dependencies**: Spring Boot 3.x, Spring Security, JUnit 5, Testcontainers, React 19, Tailwind, MUI 7  
**Storage**: PostgreSQL 15+ com RLS e criptografia conforme Art. VI  
**Development Environment**: Windows 11 + PostgreSQL 15+ local nativo + Spring profile `dev`  
**Testing**: JUnit 5 + Mockito + Testcontainers + contract tests de CLI/API + frontend tests  
**Target Platform**: Linux server (containers) + navegadores modernos  
**Project Type**: Plataforma web multi-tenant (bibliotecas modulares + adaptadores HTTP/CLI + frontend)  
**Performance Goals**: p95 < 250ms em operacoes IAM e tenant-core; sem degradacao cross-tenant sob excesso de cota  
**Constraints**: Sem IdP externo; RLS obrigatoria; JSON logs com tenant_id/trace_id; segredos de DB fora de arquivos versionados; simplicidade arquitetural  
**Scale/Scope**: Fundacao para multiplos modulos clinicos independentes e evolucao por equipes autonomas

## Development Environment Baseline

- O ambiente de desenvolvimento deve usar PostgreSQL 15+ instalado nativamente no Windows 11 (sem dependencia de banco em container para o ciclo de desenvolvimento diario).
- O backend deve executar com `spring.profiles.active=dev`, carregando configuracoes de banco local.
- As credenciais `DB_URL`, `DB_USER` e `DB_PASSWORD` devem ser injetadas via variaveis de ambiente do Windows ou arquivo `.env` local nao versionado.
- Arquivos de configuracao versionados devem conter apenas placeholders (ex.: `application-dev.yml.template`) e nunca segredos reais.
- Testes de integracao continuam obrigatorios com Testcontainers (PostgreSQL real efemero) para validar RLS e isolamento de tenant, independentemente do banco local de desenvolvimento.

## Constitution Check

*GATE: validado antes da pesquisa e revalidado apos design da Fase 1.*

> Source of truth: `.specify/memory/constitution.md` v1.6.0

| # | Gate | Status |
|---|------|--------|
| 0 | Multi-Tenant com tenant_id + RLS por entidade tenant-scoped. | PASS |
| I | Estrategia Test-First com unit/integration/contract definida. | PASS |
| II | Contrato CLI JSON por modulo definido em `contracts/cli-contracts.md`. | PASS |
| III | Limites de bibliotecas e APIs publicas por modulo definidos. | PASS |
| IV | Decisoes do plano mapeadas para FRs na matriz de rastreabilidade. | PASS |
| V | Sem ambiguidades em aberto; clarificacoes incorporadas na spec. | PASS |
| VI | Classificacao de dados IAM + erro padrao mapeado para OperationOutcome. | PASS |
| VII | Impacto RNDS/LGPD analisado; compliance review marcado. | PASS |
| VIII | Simplicidade preservada; pontos de complexidade registrados. | PASS |
| IX | Drift check obrigatorio em CI (spec-plan-tasks-codigo). | PASS |
| X | Quotas e rate limits por tenant definidos para adapters HTTP/CLI. | PASS |
| XI | Contrato de observabilidade com trace_id e logs JSON definido. | PASS |
| XII | Governanca de agente respeitada (spec + constituicao como fonte de verdade). | PASS |
| XIII | Spikes comparativos para hashing e sessao executados. | PASS |
| XIV | Contratos de eventos versionados previstos para fase assicrona. | PASS |
| XV | Degradacao graciosa para IAM/DB com fallback seguro definida. | PASS |
| XVI | SBOM exigido para releases de modulos. | PASS |
| XVII | Matriz RBAC inicial definida para operacoes IAM. | PASS |
| XVIII | Validation Gate em HTTP/CLI/eventos definido. | PASS |
| XIX | Checklist de consistencia antes de marcar tasks como done. | PASS |
| XX | `research.md` gerado com riscos, alternativas e recomendacoes. | PASS |
| XXI | Fluxo de feedback de incidentes para Edge Cases estabelecido. | PASS |
| XXII | IAM interno nativo sem qualquer IdP externo. | PASS |

**Compliance Review Required**: [x] YES — [ ] NO  
**Reviewer sugerido**: arquitetura + compliance LGPD/RNDS

## Requirements Traceability Matrix

| Plan Decision ID | Related `spec.md` Requirement(s) | Evidence / Link |
|---|---|---|
| D-001: Maven multi-module library-first | FR-003, FR-013, FR-014 | Project Structure |
| D-002: CLI JSON em todos os modulos base | FR-004, FR-011 | `contracts/cli-contracts.md` |
| D-003: IAM in-app com sessao stateful | FR-005, FR-006, FR-006a, FR-007 | `research.md`, `data-model.md` |
| D-004: tenant_id + RLS obrigatorios | FR-001, FR-002, FR-002a, FR-015 | `data-model.md` |
| D-005: quotas com bloqueio imediato do tenant infrator | FR-008, FR-009, FR-009a | Monitoring/Fallback + RBAC |
| D-006: trace_id end-to-end e logs JSON | FR-010, FR-010a, FR-011 | Monitoring and Telemetry Plan |
| D-007: resiliencia com degradacao graciosa | Edge Cases + FR-012 | Fallback Strategy |

## Research Validation

| Topic | Compatibility Findings | Risk Summary | Decision Impact | research.md Evidence |
|---|---|---|---|---|
| Hashing de senha | BCrypt e Argon2id sao viaveis em Java 21/Spring; BCrypt favorece fundacao simples | Argon2id exige tuning inicial mais sensivel | D-003 escolhe BCrypt cost 12 com trilha futura para Argon2id | `/specs/002-definir-fundacao-modular/research.md` |
| Sessao interna | Stateful em DB simplifica revogacao e auditoria; JWT+denylist aumenta complexidade | Maior chance de inconsistencias operacionais no modelo denylist | D-003 escolhe token opaco em tabela `iam_sessions` | `/specs/002-definir-fundacao-modular/research.md` |
| RLS PostgreSQL 15 | `SET LOCAL app.tenant_id`, `ENABLE/FORCE RLS` e indices por tenant estabilizam isolamento | Erros de contexto de tenant em pool podem causar vazamento se mal implementado | D-004 exige middleware de contexto + policies FOR ALL | `/specs/002-definir-fundacao-modular/research.md` |

## Production/QA Feedback to Spec

| Incident or QA Issue | `spec.md` Edge Case Update | Requirement ID(s) | Fix Task Link | Evidence |
|---|---|---|---|---|
| N/A na fundacao inicial | Politica ja definida para tenant_id ausente/invalido (fail-closed) | FR-002a | A definir em `tasks.md` | Clarifications em `spec.md` |

## Monitoring and Telemetry Plan

| Operation / Flow | Trace Propagation (`trace_id`) | Metrics | Structured Log Fields | Dashboard / Alert |
|---|---|---|---|---|
| `auth.login` (HTTP + CLI) | gerar se ausente, preservar se valido, persistir em sessao | login_latency_ms, login_fail_rate | tenant_id, trace_id, operation, outcome, user_id_hash | IAM Auth Dashboard |
| `auth.logout` | propagar trace no revoke e auditoria | revoke_latency_ms, revoke_error_rate | tenant_id, trace_id, operation, outcome, session_id | IAM Session Dashboard |
| `tenant.quota.check` | propagar entre gateway, service e DB | quota_block_count, noisy_neighbor_prevented | tenant_id, trace_id, operation, outcome, quota_rule | Tenant Isolation Dashboard |
| eventos assincronos | headers com trace_id e tenant_id; rehidratar no consumer | consumer_lag, retry_count, dlq_rate | tenant_id, trace_id, operation, outcome, topic | Async Reliability Dashboard |

## CI Drift Verification

- **Tooling**: `spec-kit-verify-tasks` + checks de rastreabilidade em PR
- **Scope**: `spec.md` ↔ `plan.md` ↔ `tasks.md` ↔ evidencias de codigo/teste
- **Policy**: qualquer divergencia bloqueia merge

## Architecture Spikes

| Module | Classification | Spike A | Spike B | Decision | Evidence |
|---|---|---|---|---|---|
| IAM hashing | Mission Critical | BCrypt (cost 12) | Argon2id | A (fundacao) | `research.md` |
| IAM sessao | Mission Critical | Stateful token opaco em DB | JWT + denylist | A (fundacao) | `research.md` |

## Event Schema Compatibility

| Event / Topic / Queue | Change Type | SemVer Impact | Schema Compatibility Check | Consumer Migration Required |
|---|---|---|---|---|
| `iam.session.revoked.v1` | new | MINOR | obrigatorio antes de merge | NO |
| `tenant.quota.blocked.v1` | new | MINOR | obrigatorio antes de merge | NO |

## Fallback Strategy

| Integration | Circuit Breaker | Retry Policy | Outbox/Buffer Strategy | Clinical Safety Impact |
|---|---|---|---|---|
| IAM session store (PostgreSQL) | timeout curto + abertura por erro repetido | retry com jitter apenas em leituras idempotentes | registrar eventos de seguranca em outbox quando DB degradado | mutacoes sensiveis em fail-closed |
| Tenant quota checks | degradar para bloqueio conservador do tenant suspeito | sem retry infinito | fila local para auditoria se persistencia atrasar | protege tenants saudaveis |
| Async broker (Kafka/RabbitMQ) | CB no publisher | retry limitado e DLQ | outbox transacional para envio posterior | mantem rastreabilidade sem perda silenciosa |

## SBOM and Supply Chain Evidence

| Artifact | SBOM Format | Generation Step | Storage Location | Verification Step |
|---|---|---|---|---|
| `clinic-tenant-core` | CycloneDX | pipeline release backend | artifact registry | dependency scan gate |
| `clinic-iam-core` | CycloneDX | pipeline release backend | artifact registry | dependency scan gate |
| `clinic-observability-core` | CycloneDX | pipeline release backend | artifact registry | dependency scan gate |

## RBAC Mapping Matrix

| Service Operation | Clinical Role(s) | Permission Key | Enforcement Point | Audit Evidence |
|---|---|---|---|---|
| `iam.user.create` | tenant_admin | iam.user.create | service layer policy | `iam_audit_events` |
| `iam.user.block` | tenant_admin, security_admin | iam.user.block | service layer policy | `iam_audit_events` |
| `tenant.quota.update` | platform_admin | tenant.quota.update | admin service policy | `iam_audit_events` |
| `tenant.cross.read` | security_auditor (contexto segregado) | tenant.cross.read | isolated admin context | `iam_audit_events` |

## Design System Conformance

| UI Capability | Design System Component(s) | Token Set | Accessibility Check | Evidence |
|---|---|---|---|---|
| Login template | atoms/input, atoms/button, molecules/form-field, templates/auth | spacing/color/typography tokens | WCAG 2.2 AA | frontend review gate |
| Tenant admin template | organisms/table, molecules/filter-bar, templates/admin | spacing/color/typography tokens | WCAG 2.2 AA | frontend review gate |

## Project Structure

### Documentation (this feature)

```text
specs/002-definir-fundacao-modular/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
    └── cli-contracts.md
```

### Source Code (repository root)

```text
backend/
├── pom.xml
├── clinic-platform-bom/
├── clinic-shared-kernel/
├── clinic-tenant-core/
│   ├── src/main/java/.../api/
│   ├── src/main/java/.../application/
│   ├── src/main/java/.../domain/
│   └── src/main/java/.../cli/
├── clinic-iam-core/
│   ├── src/main/java/.../api/
│   ├── src/main/java/.../application/
│   ├── src/main/java/.../domain/
│   └── src/main/java/.../cli/
├── clinic-observability-core/
└── clinic-gateway-app/

frontend/
├── package.json
└── src/
    ├── components/
    │   ├── atoms/
    │   ├── molecules/
    │   ├── organisms/
    │   └── templates/
    ├── features/
    ├── app/
    └── shared/
```

**Structure Decision**: arquitetura web com separacao backend/frontend. Backend em Maven multi-module library-first; frontend estritamente atomico (atoms, molecules, organisms, templates).

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Nenhuma violacao ativa do Artigo VIII identificada na fundacao | N/A | N/A |
