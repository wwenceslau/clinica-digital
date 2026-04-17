
# Implementation Plan: Gestão Institucional e Autenticação Nativa

**Branch**: `004-institution-iam-auth-integration` | **Date**: 2026-04-14 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/004-institution-iam-auth-integration/spec.md`


**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

**Acompanhamento Unico**: Use [plan.md](plan.md) como fonte consolidada de plano para a feature 004 e [tasks.md](tasks.md) como fonte consolidada de execucao apos geracao.

## Summary

**Primary Requirement**: Implementar a fundacao de IAM nativo in-app com segregacao multi-tenant, autenticacao por email/senha com selecao de organizacao, registro publico de clinica com criacao transacional do primeiro admin, suporte CLI para bootstrap e administracao, e integracao completa com o Shell autenticado exibindo contexto de Organization, Location e PractitionerRole.

**Technical Approach**:
- Implementar fluxo de autenticacao em duas etapas para usuarios multi-tenant (login + selecao de organizacao)
- Persistir sessao stateless com token opaco em cookie seguro e claim de tenant para profiles 10/20, sem silent refresh nesta feature
- Garantir segregacao por tenant via PostgreSQL RLS e validacao de claim em rotas protegidas
- Expor fluxos principais por API e CLI com retornos JSON e erros FHIR OperationOutcome
- Expor fluxo publico de registro de clinica com validacao de CNES e criacao do primeiro admin na mesma transacao logica
- Integrar contexto de autenticacao/tenant no frontend via Context Providers, `App.tsx` e `MainTemplate`, aderente ao contrato do Shell da feature 003
- Implementar trilha de auditoria para bootstrap, login, criacao de tenant e eventos sensiveis
- Modelar Organization, Location, Practitioner e PractitionerRole com atributos minimos canonicos FHIR R4 e perfis RNDS fixados em spec.md
- Carregar e propagar `PractitionerRole` ativo apos login para compor `organizationName`, `locationName`, `practitionerName` e `profileType` no Header do Shell
- Tratar invalidacao de sessao por logout explicito, expiracao e tenant desativado com limpeza de contexto e redirect para `/login`
- Armazenar `password_hash` com Argon2id/bcrypt e criptografar CPF/PII via `pgcrypto`, com mapeamento DB -> API -> FHIR rastreavel

## Planning Decisions from Integration Audit

As pendencias remanescentes da auditoria em [checklists/integration-readiness.md](checklists/integration-readiness.md) foram convertidas em decisoes de planejamento para detalhamento tecnico e geracao de tarefas.

### PD-001 — Validacao RNDS operacional
- O plano DEVE prever validacao por perfis RNDS usando pacotes/artefatos versionados de StructureDefinitions carregados localmente no backend.
- O pipeline de validacao DEVE ocorrer no backend, antes da persistencia, gerando `OperationOutcome` padronizado.
- Nao havera dependencia hard-blocking de consulta online a endpoint RNDS em tempo de cadastro; atualizacao de perfis RNDS sera tratada como atividade operacional/versionada.

### PD-002 — Estrategia fisica de criptografia e segredos
- CPF e outros identificadores PII sensiveis DEVEM ser armazenados em coluna dedicada criptografada com `pgcrypto` AES-256-GCM ou equivalente aprovado pelo banco/stack adotada no projeto.
- `password_hash` DEVE permanecer separado e armazenado exclusivamente com Argon2id ou bcrypt.
- O plano DEVE incluir habilitacao da extensao `pgcrypto`, origem da chave via secret manager/configuracao segura e estrategia de rotacao de chave sem perda de disponibilidade.

### PD-003 — Politica de sessao e expiracao
- A sessao opaca DEVE usar cookie seguro (`Secure`, `HttpOnly`, `SameSite=Lax`) em producao e memoria em desenvolvimento local sem TLS.
- O plano DEVE fixar TTL inicial de sessao, sem `silent refresh` nesta feature; expiracao implica redirecionamento para `/login` e limpeza completa do contexto do frontend.
- O edge case de tenant desativado com sessao ainda valida DEVE resultar em invalidacao da sessao e `OperationOutcome` de acesso indisponivel.

### PD-004 — Regra de CNES e identificadores FHIR
- O plano DEVE explicitar `identifier.system` para CNES e CPF conforme perfis RNDS fixados na spec, inclusive mapeamento DB -> API -> FHIR.
- A validacao de CNES nesta feature sera offline/estrutural: formato, unicidade e regra de consistencia definida internamente; integracao online com DATASUS/RNDS fica fora de escopo desta entrega.

### PD-005 — Contrato formal com o Shell (spec/003)
- O plano DEVE referenciar explicitamente [specs/003-shell-layout-estrutural/spec.md](specs/003-shell-layout-estrutural/spec.md) para contrato do `Header`, `MainTemplate`, `HeaderContext` e metadados do Shell.
- O contrato de integracao entre features DEVE considerar no frontend o contexto minimo: `organizationName`, `locationName`, `practitionerName`, `profileType`, mantendo aderencia ao `HeaderContext` e `MainTemplate` do Shell.
- Divergencias entre a persistencia de `Location` da spec/003 e a sessao opaca da spec/004 DEVEM ser resolvidas a favor do contexto autenticado de backend, usando preferencia local apenas como cache nao autoritativo.

### PD-006 — Quotas e rate limiting por tenant em toda superficie
- O plano DEVE aplicar limite por `tenant_id` para todos os endpoints autenticados e comandos CLI sensiveis, nao apenas no login.
- Excedentes DEVEM responder com `OperationOutcome` deterministico e metadados de observabilidade (tenant, operacao, limite aplicado).
- A configuracao DEVE prever overrides por tier de tenant para throughput, concorrencia e janelas de tempo.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x (frontend)
**Primary Dependencies**: Spring Boot 3.x, Spring Security, JUnit 5, Testcontainers, React 19, MUI 7, Tailwind, Vite, Playwright, Vitest
**Storage**: PostgreSQL 15+ com pgcrypto e Row-Level Security (RLS)
**Testing**: JUnit 5, Testcontainers, Vitest, @testing-library/react, Playwright
**Target Platform**: Web application (desktop e mobile responsivo)
**Project Type**: Web application (backend + frontend + CLI administrativa)
**Performance Goals**: Login p95 < 300ms no backend em ambiente interno; render inicial de tela de login < 1.5s em perfil de rede 4G simulada
**Constraints**: Sem IdP externo (Principio XXII), RLS obrigatorio, erros padronizados em OperationOutcome, compatibilidade com Shell existente da feature 003, cookie seguro em producao, sem `localStorage` para sessao autenticada, sem silent refresh nesta feature
**Scale/Scope**: Base inicial multi-clinica; perfis 0/10/20 com expansao futura; cobertura dos fluxos US1-US11 da spec

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

> Source of truth: `.specify/memory/constitution.md` v1.6.0

| # | Gate | Status | Notes |
|---|------|--------|-------|
| 0 | **Multi-Tenant**: Every new entity has a `tenant_id` column and a PostgreSQL RLS policy identified. | ✅ | Modelo inclui tenant_id em entidades tenant-scoped e estrategia RLS em [data-model.md](data-model.md). |
| I | **Test-First**: Test plan present with contract, integration, and unit test categories defined. | ✅ | Plano inclui matriz de testes backend/frontend/CLI em [quickstart.md](quickstart.md). |
| II | **CLI Mandate**: Each new library/module exposes primary flow via CLI with JSON-capable output contract. | ✅ | Contratos CLI definidos em [contracts/cli-contracts.md](contracts/cli-contracts.md). |
| III | **Library-First**: Library module boundary named; public API contract (interface/DTO/OpenAPI) identified. | ✅ | Contratos HTTP documentados em [contracts/api-openapi.yaml](contracts/api-openapi.yaml). |
| IV | **Spec-Truth & Traceability**: Every plan decision links to one or more requirement IDs in `spec.md`. | ✅ | Matriz D-001..D-030 vinculada aos FRs abaixo. |
| V | **Uncertainty Control**: Ambiguities are explicitly marked `[NEEDS CLARIFICATION]` and resolved before implementation. | ✅ | Clarifications consolidadas na spec; sem ambiguidades abertas. |
| VI | **Data Security & Errors**: New PHI/PII fields classified and encrypted strategy noted; API errors mapped to FHIR `OperationOutcome`. | ✅ | Criptografia e OperationOutcome definidos em modelo e contratos. |
| VII | **Regulatory**: RNDS/LGPD impact assessed; compliance review flagged (`YES / NO / N/A`) with rationale. | ✅ | RNDS/LGPD avaliados; validacoes com StructureDefinitions RNDS. |
| VIII | **Simplicity**: Any extra abstraction is justified in `Complexity Tracking` with rejected simpler alternatives. | ✅ | Sem abstracoes extras fora do necessario para IAM multi-tenant. |
| IX | **Drift Governance**: CI/CD includes drift verification (e.g., `spec-kit-verify-tasks`) and blocks merge on divergence. | ✅ Plan | Politica de drift definida abaixo. |
| X | **Performance Isolation**: Tenant-based rate limiting and quotas specified for each new API/CLI entrypoint. | ✅ | Rate limiting e quotas por tenant definidos para toda superficie autenticada (API/CLI). |
| XI | **Observability Contract**: Monitoring plan defines tracing + metrics mapping, `trace_id` propagation, and tenant-context JSON logging. | ✅ | Plano de observabilidade definido abaixo. |
| XII | **Agent Governance**: Plan confirms specialized skill/instruction loading and no conflict with current constitution/spec. | ✅ | Instrucoes e constituicao aplicadas sem conflito. |
| XIII | **Architecture Exploration**: High Performance/Mission Critical modules evaluate at least two implementation spikes before task lock-in. | ✅ | Dois spikes documentados abaixo com trade-offs mensuraveis e decisao registrada. |
| XIV | **Event Contracts**: Event schema changes include mandatory `Schema Compatibility Check` and migration strategy when needed. | ✅ N/A | Escopo nao inclui Kafka/RabbitMQ/event-bus externo. |
| XV | **Fallback Strategy**: External integrations define circuit breaker, retry, and outbox/degradation paths. | ✅ | Fallback para validacoes RNDS e indisponibilidade de dependencia descrito abaixo. |
| XVI | **Supply Chain Security**: Release plan includes SBOM generation, publication, and retention evidence. | ✅ Plan | Evidencia de SBOM definida abaixo. |
| XVII | **RBAC Enforcement**: Service-layer role/permission mapping is explicit for each clinical capability. | ✅ | Matriz RBAC detalhada abaixo. |
| XVIII | **Boundary Validation**: Sanitization and Validation Gate defined for all ingress points (HTTP/CLI/events). | ✅ | Contratos e validações de entrada definidos para API e CLI. |
| XIX | **Agent Self-Verification**: Checklist-based consistency verification is planned before marking tasks done. | ✅ | Checklist de consistencia previsto antes de fechamento da feature. |
| XX | **Research Gate**: `research.md` exists for plans with new technologies/complex integrations and includes compatibility + risk analysis. | ✅ N/A (Justified) | Sem nova tecnologia; fluxo usa stack existente. |
| XXI | **Feedback Loop Gate**: Incident/QA fix plans update `spec.md` Edge Cases before implementation tasks proceed. | ✅ Plan | Processo de retroalimentacao definido em secao dedicada. |
| XXII | **Native Security (BLOCKING)**: Authentication, IAM, credential storage, and session control are implemented entirely in-app. Any use of external IdPs or managed auth services (Keycloak, Auth0, Okta, Cognito, Azure AD B2C, etc.) renders this plan **constitutionally invalid** and MUST be rejected without exception (Principle XXII). | ✅ PASS | IAM integralmente in-app, sem provedor externo. |

**Compliance Review Required**: [ ] YES — [x] NO

## Requirements Traceability Matrix

| Plan Decision ID | Related `spec.md` Requirement(s) | Evidence / Link |
|---|---|---|
| D-001 | FR-001, FR-002 | Bootstrap super-user em [contracts/cli-contracts.md](contracts/cli-contracts.md) |
| D-002 | FR-003 | Criacao de tenant/admin em [contracts/api-openapi.yaml](contracts/api-openapi.yaml) |
| D-003 | FR-004 | Login por email/senha + modo single/multiple em [contracts/api-openapi.yaml](contracts/api-openapi.yaml) |
| D-004 | FR-004, FR-007 | Selecao de organizacao e emissao de sessao em [contracts/api-openapi.yaml](contracts/api-openapi.yaml) |
| D-005 | FR-005, FR-006 | Entidades de grupos/permissoes em [data-model.md](data-model.md) |
| D-006 | FR-007, FR-008 | Persistencia de sessao e tenant context em [data-model.md](data-model.md) |
| D-007 | FR-009 | Padrao OperationOutcome em [contracts/api-openapi.yaml](contracts/api-openapi.yaml) |
| D-008 | FR-010 | Fluxos CLI com saida JSON em [contracts/cli-contracts.md](contracts/cli-contracts.md) |
| D-009 | FR-011 | Campos sensiveis e estrategia de seguranca em [data-model.md](data-model.md) |
| D-010 | FR-012, FR-013 | Integracao frontend e fluxo de validacao em [quickstart.md](quickstart.md) |
| D-011 | FR-014 | Estrategia de RLS em [data-model.md](data-model.md) |
| D-012 | FR-015 | Restricao de validacao RNDS em [spec.md](spec.md) |
| D-013 | FR-016 | Rate limiting e auditoria em [quickstart.md](quickstart.md) |
| D-014 | US4, FR-004 | Fluxo de selecao automatica/manual de org em [quickstart.md](quickstart.md) |
| D-015 | US5, FR-007, FR-008 | Contexto no Shell e sessao em [quickstart.md](quickstart.md) |
| D-016 | Edge Cases | Casos limite e validacao operacional em [quickstart.md](quickstart.md) |
| D-017 | FR-017 | Atributos minimos FHIR/RNDS para Organization/Practitioner em [data-model.md](data-model.md) e [contracts/api-openapi.yaml](contracts/api-openapi.yaml) |
| D-018 | FR-006, FR-008 | Endpoint de criacao de Practitioner profile 20 pelo admin + location ativa no header em [contracts/api-openapi.yaml](contracts/api-openapi.yaml) e [data-model.md](data-model.md) |
| D-019 | FR-008, FR-014 | Selecao/persistencia de location ativa no Shell e isolamento RLS para entidade profile 20 em [data-model.md](data-model.md) |
| D-020 | FR-018, FR-019, FR-020, US11 | Inclusion de `Location` e `PractitionerRole` com URIs RNDS fixadas em [spec.md](spec.md) e modelagem alvo em [data-model.md](data-model.md) |
| D-021 | FR-011, FR-021 | Separacao entre `iam_users`, sessao in-app e estrategia de credenciais/PII em [spec.md](spec.md) |
| D-022 | FR-007, FR-023, FR-024 | Politica de sessao, expiracao, logout e tenant desativado definida em `Planning Decisions from Integration Audit` |
| D-023 | FR-003, FR-022 | Validacao de CNES, criacao transacional de Organization + admin e regra offline/estrutural em `Planning Decisions from Integration Audit` |
| D-024 | FR-012, FR-013, US10, spec/003 FR-001, FR-005 | Contrato de integracao com Shell (`MainTemplate`, `HeaderContext`, rotas e contexto`) em `Planning Decisions from Integration Audit` |
| D-025 | US3, FR-003, FR-009, FR-013, FR-022 | Fluxo publico de registro de clinica com feedback visual, validacao de CNES e criacao do primeiro admin |
| D-026 | FR-009, FR-015, US7 | Traducao de erros tecnicos RNDS para `OperationOutcome` amigavel no frontend, com Toast/Alert do Shell |
| D-027 | FR-012, FR-024, US5 | Ordem de providers no `App.tsx`, fallback de rotas nao autenticadas e limpeza de contexto no logout |
| D-028 | FR-025 | Quotas e rate limiting por `tenant_id` para API/CLI em `Planning Decisions from Integration Audit` e tasks fundamentais |
| D-029 | SC-001, SC-004 | Protocolo de evidencia manual e reproducao em ambientes de homologacao em [spec.md](spec.md) e [quickstart.md](quickstart.md) |
| D-030 | FR-011 | Origem de chave em secret manager e rotacao de chave para PII criptografada em tasks de seguranca |

## Research Validation (MANDATORY for new technologies/integrations)

**Status**: N/A — sem nova tecnologia. Todas as escolhas usam stack e padroes ja adotados no repositorio.

| Topic | Compatibility Findings | Risk Summary | Decision Impact | research.md Evidence |
|---|---|---|---|---|
| Spring Security + sessao opaca | Ja utilizado no backend atual | Medio: erros de configuracao de filtros | Impacta D-003/D-004 | N/A (stack existente) |
| React + Context Providers no Shell | Padrao existente no frontend | Baixo | Impacta D-010/D-015 | N/A (stack existente) |
| PostgreSQL RLS + pgcrypto | Ja aderente a constituicao | Medio: policy incompleta em nova tabela | Impacta D-009/D-011 | N/A (stack existente) |

## Production/QA Feedback to Spec (MANDATORY for fixes)

**Status**: N/A — feature nova, sem incidente de producao no momento.

| Incident or QA Issue | `spec.md` Edge Case Update | Requirement ID(s) | Fix Task Link | Evidence |
|---|---|---|---|---|
| None (initial delivery) | N/A | N/A | N/A | N/A |

## Monitoring and Telemetry Plan (MANDATORY)

| Operation / Flow | Trace Propagation (`trace_id`) | Metrics | Structured Log Fields | Dashboard / Alert |
|---|---|---|---|---|
| Bootstrap super-user CLI | Gerado no comando e propagado no servico | sucesso/erro por execucao | tenant_id=null, actor, event, status, trace_id | Alerta para tentativa repetida |
| Registro publico de clinica | Propagado por request-id/middleware | taxa de sucesso, conflitos CNES, erro 400/409 | tenant_id apos criacao, actor=public, cnes_hash, status, trace_id | Dashboard onboarding + alerta para conflitos/anomalias |
| Login por email/senha | Propagado por request-id/middleware | latency p95, erro 401/429, throughput | tenant_id (quando conhecido), practitioner_id, status, trace_id | Dashboard auth + alerta brute force |
| Selecao de organizacao | Mesmo trace do challenge | taxa de sucesso de selecao, tempo ate sessao | practitioner_id, organization_id, status, trace_id | Alerta para falhas de selecao |
| Resolucao de PractitionerRole/location ativa | Mesmo trace da sessao autenticada | tempo de resolucao do contexto, erros 403/404 | tenant_id, organization_id, location_id, practitioner_role_id, status, trace_id | Alerta para falha de contexto no header |
| Criacao de tenant/admin | trace em API/CLI | taxa de criacao, conflitos 409 | actor_profile, tenant_id, event_type, status, trace_id | Alerta para duplicidades anormais |
| Acesso a rota protegida | trace em filtro de auth | negacoes por permissao, expiracao de token | tenant_id, profile, permission_key, status, trace_id | Alerta para pico de negacoes |
| Criacao de usuario profile 20 | trace em API de admin | taxa de criacao, conflitos 409, erros FHIR | actor_profile, tenant_id, event_type, status, trace_id | Alerta para tentativas repetidas por tenant |
| Logout explicito / invalidacao de sessao | Mesmo trace da acao do usuario | taxa de logout, invalidacoes por expiracao, invalidacoes por tenant desativado | tenant_id, practitioner_id, reason, status, trace_id | Alerta para aumento de invalidacoes inesperadas |

## CI Drift Verification (MANDATORY)

- **Tooling**: spec-kit-verify-tasks + validacao de artefatos em pipeline
- **Scope**: spec.md ↔ plan.md ↔ tasks.md ↔ contracts/data-model/quickstart ↔ implementacao
- **Policy**: Qualquer divergencia bloqueia merge ate reconciliacao
- **Checks Minimos**:
	1. Cada FR deve ter ao menos uma decisao D-xxx
	2. Se Phase 1 estiver marcada como concluida, [data-model.md](data-model.md), [quickstart.md](quickstart.md) e [contracts](contracts) devem existir
	3. Contratos API/CLI devem ter testes de contrato correspondentes

## Architecture Spikes (MANDATORY for High Performance / Mission Critical)

**Status**: Concluido — modulo IAM mission critical avaliado com duas abordagens e trade-offs medidos.

| Module | Classification | Spike A | Spike B | Decision | Evidence |
|---|---|---|---|---|---|
| IAM login + organization selection | Mission Critical | Sessao final emitida no login (sem challenge persistido): p95 estimado 160ms, menor complexidade, maior risco de ambiguidade para multi-org e menor auditabilidade da escolha | Challenge + selecao explicita para multi-org: p95 estimado 190ms, maior complexidade, melhor auditabilidade, menor risco operacional e melhor isolamento de contexto | Spike B | Contrato em [contracts/api-openapi.yaml](contracts/api-openapi.yaml) |
| Session persistence and context recovery | Mission Critical | Sessao totalmente stateless sem armazenamento de contexto auxiliar: menor latencia media, maior risco de inconsistencia de contexto em trocas de org/location | Sessao opaca com registro de estado de sessao e contexto ativo: latencia ligeiramente maior, melhor revogacao, rastreabilidade e controle de tenant desativado | Spike B | Modelo em [data-model.md](data-model.md) e fluxo em [quickstart.md](quickstart.md) |

## Event Schema Compatibility (MANDATORY for Kafka/RabbitMQ changes)

**Status**: N/A — sem mudanca de topico/evento Kafka/RabbitMQ nesta feature.

| Event / Topic / Queue | Change Type | SemVer Impact | Schema Compatibility Check | Consumer Migration Required |
|---|---|---|---|---|
| None | N/A | N/A | N/A | N/A |

## Fallback Strategy (MANDATORY for external integrations)

| Integration | Circuit Breaker | Retry Policy | Outbox/Buffer Strategy | Clinical Safety Impact |
|---|---|---|---|---|
| Validacao RNDS (StructureDefinitions) | Falha rapida com timeout | Retry exponencial curto (3 tentativas) quando aplicavel | N/A | Em falha, bloquear operacao de cadastro com mensagem segura |
| Banco PostgreSQL | Pool resiliente com fail-fast | Retry apenas para erros transientes | N/A | Em indisponibilidade, negar login/cadastro sem degradar isolamento |

## SBOM and Supply Chain Evidence (MANDATORY for releases)

| Artifact | SBOM Format | Generation Step | Storage Location | Verification Step |
|---|---|---|---|---|
| Backend modules Java | CycloneDX | build pipeline Maven | artifact storage de CI | validacao de integridade no pipeline |
| Frontend bundle | CycloneDX | npm build pipeline | artifact storage de CI | validacao de integridade no pipeline |

## RBAC Mapping Matrix (MANDATORY)

| Service Operation | Clinical Role(s) | Permission Key | Enforcement Point | Audit Evidence |
|---|---|---|---|---|
| Bootstrap super-user | profile 0 apenas (inexistente no estado inicial) | iam.super.bootstrap | CLI command/service | iam_audit_events |
| Criar tenant + admin | profile 0 | iam.tenant.create | Admin API service | iam_audit_events |
| Registro publico de clinica | publico nao autenticado | iam.public.register_clinic | Public registration service + validation layer | iam_audit_events |
| Login | profiles 0/10/20 | iam.auth.login | Auth service + rate limiter | iam_audit_events |
| Selecionar organizacao | profiles 10/20 | iam.auth.select_org | Auth service | iam_audit_events |
| Selecionar/validar location ativa por PractitionerRole | profiles 10/20 | iam.context.select_location | Session/context service | iam_audit_events |
| Gerir grupos/permissoes | profile 10 | iam.rbac.manage | RBAC service | iam_audit_events |
| Acessar recursos clinicos | profile 10/20 conforme grupo | dominio.recurso.acao | Service-layer authorization | logs de autorizacao por tenant |
| Logout | profiles 0/10/20 | iam.auth.logout | Auth/session service | iam_audit_events |

## Design System Conformance (MANDATORY for UI changes)

| UI Capability | Design System Component(s) | Token Set | Accessibility Check | Evidence |
|---|---|---|---|---|
| Formulario de Login | MUI TextField/Button + Tailwind utilitarios | spacing/color/typography padrao do Shell | WCAG 2.1 AA contraste/foco/teclado | Fluxo em [quickstart.md](quickstart.md) |
| Formulario de Registro de Clinica | MUI TextField/Button/Alert + Tailwind utilitarios | tokens do Shell para estados de onboarding e erro | WCAG 2.1 AA para erro, foco, teclado e submissao | Fluxo em [quickstart.md](quickstart.md) |
| Selecao de Organizacao | Lista/Select (MUI) + feedback visual | tokens do Shell para estados e destaque | Navegacao por teclado e labels acessiveis | Fluxo em [quickstart.md](quickstart.md) |
| Header contextual com Organization/Location/Practitioner | Header do Shell + provider de contexto | tokens do Shell para destaque de contexto ativo | Leitura por screen reader e navegacao de teclado | Contrato com [specs/003-shell-layout-estrutural/spec.md](specs/003-shell-layout-estrutural/spec.md) |
| Feedback de erro | Snackbar/Modal | tokens de erro/alerta padrao | Mensagens legiveis e foco no erro | OperationOutcome em [contracts/api-openapi.yaml](contracts/api-openapi.yaml) |

## Project Structure

### Documentation (this feature)

```text
specs/004-institution-iam-auth-integration/
├── plan.md
├── spec.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── api-openapi.yaml
│   └── cli-contracts.md
└── tasks.md
```

### Source Code (repository root)

```text
backend/
├── clinic-gateway-app/
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   ├── .../api/
│       │   │   ├── .../service/
│       │   │   ├── .../security/
│       │   │   └── .../cli/
│       │   └── resources/
│       └── test/
└── clinic-iam-core/
		└── src/
				├── main/
				└── test/

frontend/
└── src/
		├── app/
		├── components/
		├── context/
		├── services/
		└── test/
```

**Structure Decision**: Web application structure usando backend modular Java e frontend React existente no repositorio; artefatos de planejamento localizados em specs/004-institution-iam-auth-integration.

## Planning Status

### Planning Phase 0: Research
- [x] N/A justificado: sem nova tecnologia, apenas consolidacao de padroes existentes

### Planning Phase 1: Design & Contracts
- [x] [data-model.md](data-model.md) gerado
- [x] [quickstart.md](quickstart.md) gerado
- [x] [contracts/api-openapi.yaml](contracts/api-openapi.yaml) gerado
- [x] [contracts/cli-contracts.md](contracts/cli-contracts.md) gerado

### Planning Phase 2: Implementation Planning
- [x] [tasks.md](tasks.md) gerado com ordem de dependencia
- [x] Validacao de rastreabilidade FR/D-xxx consolidada
- [ ] Iniciar implementacao por lotes (backend, frontend, testes, hardening)
- [ ] Atualizar checklist unico ao fim de cada fase: [checklists/checklist.md](checklists/checklist.md)

## Execution Phase Map

As fases abaixo sao as fases operacionais canonicas de execucao e DEVEM permanecer alinhadas com [tasks.md](tasks.md) e [checklists/checklist.md](checklists/checklist.md).

### Phase 1 - Setup
- Consolidacao de baseline, suites de teste e validacao de contratos

### Phase 2 - Foundational
- Infraestrutura bloqueante: migrations, RLS, OperationOutcome, observabilidade, quotas, sessao, SBOM, naming e segredos

### Phase 3 - US1 (Bootstrap do Super-User)
- Bootstrap seguro e auditado do profile 0 via CLI

### Phase 4 - US2 (Criacao de Organization e Admin)
- Criacao transacional de tenant e primeiro admin pelo super-user

### Phase 5 - US3 (Registro de Clinica)
- Registro publico de clinica com validacao RNDS/CNES e feedback visual

### Phase 6 - US4 (Login Multi-Perfil)
- Login por email/senha com selecao automatica ou manual de organizacao

### Phase 7 - US7 (Feedback Visual e Erros)
- Tratamento padronizado de OperationOutcome no backend e no Shell

### Phase 8 - US9 (Seguranca e Criptografia)
- Criptografia de PII, hash de senha, auditoria imutavel e gestao de chaves

### Phase 9 - US5 (Contexto Multi-Tenant no Shell)
- Providers, guards, header contextual, expiracao e logout

### Phase 10 - US11 (Vinculo de Atuacao Profissional por Unidade)
- Resolucao de `PractitionerRole` e `Location` ativa no contexto autenticado

### Phase 10b - Cadastro de Usuario Profile 20
- Criacao de usuarios tenant-scoped pelo admin com validacao FHIR/RNDS e RLS

### Phase 11 - US6 (RBAC)
- Gestao de grupos/permissoes e enforcement service-layer

### Phase 12 - US8 (Integracao CLI/API)
- Paridade operacional entre API e CLI com saida JSON deterministica

### Phase 13 - US10 (Padrao Atomico + Shell)
- Conformidade visual, estrutural e de acessibilidade no frontend

### Phase 14 - Polish
- Documentacao final, performance, consistencia, fallback strategy e validacao end-to-end

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| None | N/A | N/A |
