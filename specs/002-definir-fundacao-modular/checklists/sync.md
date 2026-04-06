# Checklist de Sincronizacao Plan vs Done

**Purpose**: Fonte unica para reconciliar progresso entre tasks.md e validacao constitucional/qualidade, reduzindo divergencias entre artefatos.
**Created**: 2026-04-06
**Feature**: [spec.md](../spec.md) | [plan.md](../plan.md) | [tasks.md](../tasks.md)
**Policy**: A partir deste ponto, manter sincronizados apenas este arquivo e tasks.md.

## Requirement Completeness

- [x] CHK001 - Todas as tasks concluidas da Fase 1 e Fase 2 estao explicitamente refletidas no estado plan-vs-done? [Completeness, tasks.md T001-T039]
- [x] CHK002 - As remediacoes de RLS faltantes (iam_roles, iam_user_roles, iam_audit_events) estao mapeadas como concluidas? [Completeness, tasks.md T022a-T022c]
- [x] CHK003 - A exigencia de `SET LOCAL app.tenant_id` com cobertura de teste esta mapeada como concluida? [Completeness, tasks.md T030a-T030c]
- [x] CHK004 - A exigencia de isolamento de sessao cross-tenant apos renovacao esta mapeada como concluida? [Completeness, tasks.md T030d]
- [x] CHK005 - A propagacao async de contexto + limpeza explicita de contexto esta mapeada como concluida? [Completeness, tasks.md T030e-T030g]
- [x] CHK006 - O escopo IAM-critical e filtro JaCoCo estao mapeados como concluidos e verificaveis? [Completeness, tasks.md T038]

## Requirement Clarity

- [x] CHK007 - A regra de fonte unica esta clara: `tasks.md` = progresso de execucao; `checklists/sync.md` = reconciliacao de conformidade e consistencia? [Clarity]
- [x] CHK008 - Cada item de reconciliacao aponta explicitamente para IDs de task (Txxx) para permitir auditoria objetiva? [Clarity, Traceability]
- [ ] CHK009 - Existe evidência auditavel de sequencia test-first (commit-order Red/Green/Refactor) para fechar Art. I sem ambiguidade? [Clarity, Gap, tasks.md T165]

## Requirement Consistency

- [x] CHK010 - O status de CI/PR constitutional gates esta consistente entre reconciliacao e tasks concluidas? [Consistency, tasks.md T036, T039]
- [x] CHK011 - O status de base fundacional (Phase 2 completa) permanece consistente com o checkpoint em tasks.md? [Consistency, tasks.md T015-T039]
- [x] CHK012 - O fechamento definitivo de Art. I esta consistente (ainda pendente para sign-off fundacional)? [Consistency, Gap, tasks.md T165]

## Acceptance Criteria Quality

- [x] CHK013 - O criterio para considerar uma remediacao "fechada" esta objetivo: task [x] + referencia ao ID no checklist? [Acceptance Criteria]
- [x] CHK014 - O criterio para considerar uma remediacao "pendente" esta objetivo: task [ ] ou evidencia constitucional ausente? [Acceptance Criteria]
- [x] CHK015 - O criterio de progresso global e mensuravel (done/total e percentual por fase) esta definido e rastreavel em tasks.md? [Measurability, tasks.md]

## Scenario Coverage

- [x] CHK016 - O checklist cobre cenario primario de sincronizacao (tasks atualizadas apos entrega tecnica)? [Coverage]
- [x] CHK017 - O checklist cobre cenario de divergencia (texto legado em checklists antigas conflitando com tasks.md)? [Coverage, Ambiguity]
- [x] CHK018 - O checklist cobre cenario de continuidade (atualizacao incremental por fase/US sem criar novas checklists)? [Coverage]

## Dependencies & Assumptions

- [x] CHK019 - Assuncao explicita: checklists historicas (constitution/foundational/phase2-full/remediation/phase-summary/requirements) sao referencia historica, nao fonte de status atual? [Assumption]
- [x] CHK020 - Dependencia explicita: qualquer mudanca de status em tasks.md deve atualizar este arquivo no mesmo PR/commit? [Dependency]
- [ ] CHK021 - Dependencia pendente: evidencia final de verificacao constitucional completa documentada em artefato dedicado? [Dependency, Gap, tasks.md T165]

## Matriz de Reconciliacao (CHK legado -> Task atual)

- [x] CHK022 - CHK legado de RLS incompleto (phase2-full CHK001/CHK011) reconciliado com T022a/T022b/T022c. [Traceability, tasks.md T022a-T022c]
- [x] CHK023 - CHK legado de ausencia de `SET LOCAL` (phase2-full CHK004/CHK013) reconciliado com T030a/T030b/T030c. [Traceability, tasks.md T030a-T030c]
- [x] CHK024 - CHK legado de isolamento de sessao cross-tenant (phase2-full CHK062) reconciliado com T030d. [Traceability, tasks.md T030d]
- [x] CHK025 - CHK legado de propagacao async/context cleanup (phase2-full CHK017/CHK018/CHK019) reconciliado com T030e/T030f/T030g. [Traceability, tasks.md T030e-T030g]
- [x] CHK026 - CHK legado de escopo IAM-critical indefinido (phase2-full CHK043) reconciliado com T038. [Traceability, tasks.md T038]
- [ ] CHK027 - CHK legado de fechamento total Art. I permanece pendente ate evidence package final. [Traceability, Gap, tasks.md T165]

## Reconciliacao Incremental - Fase 3.B (US1)

- [x] CHK031 - T044 entregue: entidade `Tenant` implementada com campos obrigatorios do schema tenants (id, slug, legal_name, status, plan_tier, quota_*, created_at, updated_at). [Incremental, tasks.md T044]
- [x] CHK032 - T045 entregue: contrato `ITenantRepository` implementado com `findBySlug` e `findById`. [Incremental, tasks.md T045]
- [x] CHK033 - T046 entregue: `TenantRepository` implementado com Spring Data JPA e consultas sob enforcement de RLS no banco. [Incremental, tasks.md T046]
- [x] CHK034 - T047 entregue: `TenantService` implementado com `createTenant`, `getTenant`, `updateQuota`. [Incremental, tasks.md T047]

## Reconciliacao Incremental - Fase 3.C (US1)

- [x] CHK035 - T048 entregue: filtro HTTP de fronteira para `X-Tenant-ID` com fail-closed para header ausente/invalido. [Incremental, tasks.md T048]
- [x] CHK036 - T049 entregue: aspect de validacao tenant executando `SET LOCAL app.tenant_id` antes de acesso ao repositorio tenant. [Incremental, tasks.md T049]
- [x] CHK037 - T050 entregue: endpoint `POST /tenants/create` implementado no gateway e integrado ao TenantService. [Incremental, tasks.md T050]
- [x] CHK038 - T051 entregue: endpoint `GET /tenants/{id}` com validacao tenant de path vs contexto ativo. [Incremental, tasks.md T051]
- [x] CHK039 - T052 entregue: handler global retornando OperationOutcome para tenant context missing/invalid (403). [Incremental, tasks.md T052]

## Reconciliacao Incremental - Fase 3.D (US1)

- [x] CHK040 - T053 entregue: CLI command `tenant create` implementado em `TenantCommands.java` com args --slug, --legal-name, --plan-tier, --json e saida JSON conforme contrato CLI. [Incremental, tasks.md T053]
- [x] CHK041 - T054 entregue: CLI command `tenant list` implementado em `TenantCommands.java` com --json output listando tenants visiveis no contexto ativo. [Incremental, tasks.md T054]
- [x] CHK042 - T055 entregue: `CliShellConfig.java` criado no gateway com `PromptProvider` customizado; commands descobertos via scanBasePackages em ClinicGatewayApplication. [Incremental, tasks.md T055]
- [x] CHK043 - `ITenantRepository` atualizado com `findAll()` necessario para `tenant list`. [Incremental, tasks.md T054]
- [x] CHK044 - `TenantService` atualizado com `listTenants()` delegando ao repositorio. [Incremental, tasks.md T054]
- [x] CHK045 - `TenantJdbcContextInterceptor` atualizado para verificar `TenantContextStore` como fallback quando request scope nao esta ativo (CLI/async), mantendo fail-closed (FR-016a). [Incremental, Art. II]
- [x] CHK046 - `spring-shell-starter:3.3.4` adicionado ao BOM e a `clinic-tenant-core` e `clinic-gateway-app`; todos modulos compilam sem erros. [Incremental, tasks.md T055]

## Operating Rules (para evitar novas divergencias)

- [x] CHK028 - Regra 1: nao criar novos arquivos de checklist para esta feature; atualizar apenas `tasks.md` e `checklists/sync.md`. [Governance]
- [x] CHK029 - Regra 2: cada task marcada `[x]` deve ter ao menos um CHK correlato atualizado no mesmo PR. [Governance, Traceability]
- [x] CHK030 - Regra 3: antes de fechar uma fase/US, revisar CHKs pendentes e registrar justificativa para qualquer excecao aprovada. [Governance]

## Ritual de Atualizacao (3 passos)

1. Atualizar `tasks.md`
	- Marcar a task como `[x]` somente apos evidencia tecnica valida (codigo/teste/config/documento) no mesmo branch.
	- Se a task for bloqueada, manter `[ ]` e registrar impedimento no PR (nao marcar como feita parcialmente).

2. Atualizar `checklists/sync.md`
	- Localizar o CHK correlato e ajustar status (`[x]` ou `[ ]`) na mesma mudanca.
	- Se nao existir CHK correlato, adicionar novo CHK no grupo correto (Completeness/Consistency/Traceability etc.) com referencia explicita ao ID Txxx.

3. Validar sincronizacao antes do merge
	- Conferir se toda task nova marcada `[x]` tem pelo menos 1 CHK atualizado.
	- Conferir se todo CHK marcado `[x]` aponta para task `[x]` ou evidencia documental objetiva.
	- Se houver excecao aprovada, registrar em linha no CHK com tag `[Assumption]` ou `[Gap]` e data.

## Notes

- Progresso atual no plano: 65/178 tasks concluidas (36.5%).
- Fase 1: 14/14 (100%). Fase 2: 35/35 (100%). Fase 3: 16/26 (61.5%). Fase 4: 0/19. Fase 5: 0/44. Fase 6: 0/40.
- Pendencia critica remanescente para sign-off fundacional: evidencia final Art. I (T165).
