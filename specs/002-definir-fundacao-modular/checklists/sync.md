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
- [x] CHK009 - Existe evidência auditavel de sequencia test-first (commit-order Red/Green/Refactor) para fechar Art. I sem ambiguidade, consolidada em `backend/docs/CONSTITUTION_VERIFICATION.md` com referencias de execucao e relatorios Surefire de US1. [Clarity, tasks.md T165]

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
- [x] CHK021 - Evidencia final de verificacao constitucional completa documentada em artefato dedicado (`backend/docs/CONSTITUTION_VERIFICATION.md`). [Dependency, tasks.md T165]

## Matriz de Reconciliacao (CHK legado -> Task atual)

- [x] CHK022 - CHK legado de RLS incompleto (phase2-full CHK001/CHK011) reconciliado com T022a/T022b/T022c. [Traceability, tasks.md T022a-T022c]
- [x] CHK023 - CHK legado de ausencia de `SET LOCAL` (phase2-full CHK004/CHK013) reconciliado com T030a/T030b/T030c. [Traceability, tasks.md T030a-T030c]
- [x] CHK024 - CHK legado de isolamento de sessao cross-tenant (phase2-full CHK062) reconciliado com T030d. [Traceability, tasks.md T030d]
- [x] CHK025 - CHK legado de propagacao async/context cleanup (phase2-full CHK017/CHK018/CHK019) reconciliado com T030e/T030f/T030g. [Traceability, tasks.md T030e-T030g]
- [x] CHK026 - CHK legado de escopo IAM-critical indefinido (phase2-full CHK043) reconciliado com T038. [Traceability, tasks.md T038]
- [x] CHK027 - CHK legado de fechamento total Art. I fechado com evidence package final em `backend/docs/CONSTITUTION_VERIFICATION.md` e relatorios Surefire US1 anexados no workspace. [Traceability, tasks.md T165]

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

## Reconciliacao Incremental - Fase 3.E (US1)

- [x] CHK047 - T056 entregue: `QuotaService` implementado com `checkAndEnforceQuota(tenantId, metric)`, janela por minuto e `QuotaExceededException` para bloqueio imediato do tenant infrator. [Incremental, tasks.md T056]
- [x] CHK048 - T057 entregue: `QuotaBoundaryFilter` implementado na fronteira HTTP com retorno 429 quando quota e excedida, preservando tenants saudaveis. [Incremental, tasks.md T057]
- [x] CHK049 - T058 entregue: `QuotaEnforcementTest` criado cobrindo cenario de 6 requisicoes para limite 5 e isolamento de impacto para outro tenant. [Incremental, tasks.md T058]
- [x] CHK050 - T059 entregue: politica de quotas adicionada em `TenantQuotaPolicy.yaml` com fail-closed e bloqueio imediato. [Incremental, tasks.md T059]
- [x] CHK051 - `GlobalExceptionHandler` atualizado para mapear `QuotaExceededException` em OperationOutcome com status 429 e code `throttled`. [Incremental, tasks.md T057]
- [x] CHK052 - Ordenacao de filtros definida (`TenantContextFilter` antes de `QuotaBoundaryFilter`) para garantir validacao de tenant antes do cheque de quota. [Incremental, tasks.md T057]

## Reconciliacao Incremental - Fase 3.F (US1)

- [x] CHK053 - T060 entregue: `TenantContextFilter` agora gera `X-Trace-ID` quando ausente, preserva valores validos recebidos e limpa `TraceContextHolder` + MDC ao final da request. [Incremental, tasks.md T060]
- [x] CHK054 - T060 validado: contrato HTTP cobre geracao e preservacao do `X-Trace-ID` na fronteira para requests com tenant valido. [Incremental, tasks.md T060]
- [x] CHK055 - T061 entregue: `TenantService` passou a emitir logs estruturados com `tenant_id`, `trace_id`, `operation` e `outcome` em `create`, `get`, `list` e `updateQuota`. [Incremental, tasks.md T061]
- [x] CHK056 - T062 entregue: dashboard Grafana `observability/grafana/tenant-isolation.json` criado com paineis para tentativas de isolamento falhas e bloqueios por quota por tenant. [Incremental, tasks.md T062]
- [x] CHK057 - T063 entregue: regras Prometheus `observability/prometheus/tenant_isolation_rules.yml` criadas para alertar path mismatch cross-tenant, bursts de rejeicao na fronteira e repetidos quota blocks. [Incremental, tasks.md T063]
- [x] CHK058 - `GlobalExceptionHandler` passou a publicar metricas `tenant.isolation.failures` e `tenant.quota.blocks`, alinhando dashboard e alertas com sinais reais do runtime. [Incremental, tasks.md T062, tasks.md T063]

## Reconciliacao Incremental - Fase 3.G (US1)


## Reconciliacao Incremental - US2 (Fases 4.A a 4.E)

- [x] CHK063 - T066/T068/T070 entregues: testes de contrato da API publica de `clinic-tenant-core`, `clinic-iam-core` e `clinic-observability-core` criados para travar a surface publica esperada por modulo. [Incremental, tasks.md T066, tasks.md T068, tasks.md T070]
- [x] CHK064 - T067/T069 entregues: contratos CLI de `tenant-core` e `iam-core` validados com suporte `--json`, incluindo fechamento do gap `auth whoami` no documento de contratos. [Incremental, tasks.md T067, tasks.md T069]
- [x] CHK065 - T071/T072/T073 entregues: arquivos `PUBLIC_API.md` publicados para `clinic-tenant-core`, `clinic-iam-core` e `clinic-observability-core` com boundary, tipos e versionamento. [Incremental, tasks.md T071, tasks.md T072, tasks.md T073]
- [x] CHK066 - T074 entregue: `backend/pom.xml` passou a aplicar `maven-enforcer-plugin` com `BanCircularDependencies`, estabelecendo bloqueio automatizado contra acoplamento circular. [Incremental, tasks.md T074]
- [x] CHK067 - T075/T076/T077 entregues: CLI expandida implementada com `tenant quota update`, `tenant block`, `tenant unblock`, `quota check`, `trace validate` e `metrics export`, mantendo saida JSON e contratos por modulo. [Incremental, tasks.md T075, tasks.md T076, tasks.md T077]
- [x] CHK068 - T078 entregue: `clinic-tenant-core`, `clinic-iam-core` e `clinic-observability-core` passaram em `mvn clean verify -pl <module>` de forma isolada como evidencia de independencia de build/teste. [Incremental, tasks.md T078]
- [x] CHK069 - T079 entregue: varredura de imports confirmou que `clinic-tenant-core` nao depende de `clinic-iam-core`, preservando o boundary arquitetural previsto no Art. III. [Incremental, tasks.md T079]
- [x] CHK070 - T080 entregue: `backend/docs/ARCHITECTURE.md` criado com diagrama Mermaid e regras explicitas de dependencia entre os modulos Maven. [Incremental, tasks.md T080]
- [x] CHK071 - T081 entregue: `CLIMetrics` implementado em `clinic-shared-kernel` com `Timer` e `Counter` padronizados para tempo e contagem de execucao de comandos CLI, validado por `CLIMetricsTest` (2 testes, 0 falhas). [Incremental, tasks.md T081]
- [x] CHK072 - T082 entregue: `CliContextFilter` passou a registrar logs JSON de entrada/saida e a publicar metricas por comando via `CLIMetrics`, validado por `CliContextFilterTest` (2 testes, 0 falhas). [Incremental, tasks.md T082]

## Reconciliacao Incremental - Fase 4.F (US2)

- [x] CHK073 - T083 entregue: validacao constitucional de US2 executada contra Art. II, Art. III, Art. XIII e Art. XIX, com evidencia consolidada em `backend/docs/CONSTITUTION_VERIFICATION.md` e reconciliada neste checklist. [Incremental, tasks.md T083]
- [x] CHK074 - T084 entregue: workflow `.github/workflows/release.yml` validado para gerar SBOM CycloneDX por modulo selecionavel, incluindo `clinic-platform-bom`, com `-am` para resolver dependencias do reactor antes do release. [Incremental, tasks.md T084]

## Reconciliacao Incremental - Fase 5.A (US3)

- [x] CHK075 - T085 entregue: teste unitario de senha com contrato de hash/verify, formato BCrypt e comportamento fail-closed para entradas invalidas. [Incremental, tasks.md T085]
- [x] CHK076 - T086 entregue: teste unitario de `SessionManager` cobrindo create, validate e revoke com isolamento de tenant e cenarios de expiracao/revogacao. [Incremental, tasks.md T086]
- [x] CHK077 - T087 entregue: teste de integracao RLS para `iam_sessions` validando isolamento por tenant, invisibilidade cross-tenant e deny-by-default sem contexto. [Incremental, tasks.md T087]
- [x] CHK078 - T088 entregue: teste de contrato de `auth.login` validando campos obrigatorios de entrada/saida, semantica de erro e observabilidade minima. [Incremental, tasks.md T088]
- [x] CHK079 - T089 entregue: teste de contrato CLI `auth login/logout/whoami` com suporte JSON e clausula de IAM in-app sem uso de IdP externo. [Incremental, tasks.md T089]
- [x] CHK080 - T090 entregue: teste de integracao de fronteira para falha de login com tenant ausente/invalido conforme fail-closed. [Incremental, tasks.md T090]
- [x] CHK081 - T091 entregue: teste de integracao para revogacao imediata de sessao e rejeicao de reuso do mesmo `session_id`. [Incremental, tasks.md T091]

## Reconciliacao Incremental - Fase 5.B.1 (US3)

- [x] CHK082 - T092 entregue: entidade `IamUser` criada com campos tenant-scoped, estado ativo e timestamps de auditoria (`created_at`, `updated_at`, `last_login_at`). [Incremental, tasks.md T092]
- [x] CHK083 - T093 entregue: entidade `IamRole` criada com escopo por tenant, `role_key` unico por tenant e `description` obrigatoria. [Incremental, tasks.md T093]
- [x] CHK084 - T094 entregue: `IamSession` evoluida para entidade JPA com `issued_at`, `expires_at`, `revoked_at`, `client_ip`, `user_agent` e `trace_id`, mantendo compatibilidade do contrato atual de testes. [Incremental, tasks.md T094]
- [x] CHK085 - T095 entregue: entidade append-only `IamAuditEvent` criada com `metadata_json` e `created_at` gerenciado em persistencia. [Incremental, tasks.md T095]
- [x] CHK086 - T096 entregue: contratos de repositorio `IamUserRepository`, `IamSessionRepository` e `IamAuditEventRepository` adicionados ao dominio IAM (com alias de compatibilidade `IIamSessionRepository`). [Incremental, tasks.md T096]
- [x] CHK087 - T097 entregue: implementacoes JPA em `infrastructure/` adicionadas com consultas tenant-scoped e operacao de revoke por `(session_id, tenant_id)`, com build do modulo e contrato de API publica validados. [Incremental, tasks.md T097]

## Reconciliacao Incremental - Fase 5.B.2 (US3)

- [x] CHK088 - T098 entregue: `PasswordService` implementado com BCrypt cost 12, `hashPassword` e `verifyPassword` fail-closed para entradas invalidas. [Incremental, tasks.md T098]
- [x] CHK089 - T099 entregue: `SessionManager` implementado com criacao de sessao opaca, validacao tenant-scoped e revogacao imediata por `(sessionId, tenantId)`. [Incremental, tasks.md T099]
- [x] CHK090 - T100 entregue: `AuthenticationService` implementado com fluxos `login`, `logout` e `whoami`, orquestrando repositorio de usuario, sessao e auditoria in-app. [Incremental, tasks.md T100]
- [x] CHK091 - T101 entregue: `AuditService` implementado para persistencia append-only de eventos de autenticacao (`auth.login`/`auth.logout`), alinhado ao Art. VI. [Incremental, tasks.md T101]

## Reconciliacao Incremental - Fase 5.B.3 (US3)

- [x] CHK092 - T102 entregue: `AuthenticationFilter` implementado para validar `Authorization: Bearer <session_uuid>` na fronteira de rotas protegidas de IAM e anexar `request.session_id` para uso do controller. [Incremental, tasks.md T102]
- [x] CHK093 - T103 entregue: `AuthController` com `POST /auth/login` implementado, validando tenant de contexto vs. payload e retornando `session_id` e `expires_at` com metadados de observabilidade. [Incremental, tasks.md T103]
- [x] CHK094 - T104 entregue: `POST /auth/logout` implementado com revogacao da sessao autenticada e resposta de sucesso com `revoked=true`. [Incremental, tasks.md T104]
- [x] CHK095 - T105 entregue: `GET /auth/whoami` implementado retornando `user_id`, `email`, `tenant_id` e `roles` a partir da sessao validada no boundary. [Incremental, tasks.md T105]
- [x] CHK096 - T106 entregue: validacao explicita de `request.session_id != null` no `AuthenticationFilter`, com resposta FHIR OperationOutcome via `AuthSessionException` + `GlobalExceptionHandler` (401). [Incremental, tasks.md T106]

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

- Progresso atual no plano: 124/178 tasks concluidas (69.7%).
- Fase 1: 14/14 (100%). Fase 2: 35/35 (100%). Fase 3: 26/26 (100%). Fase 4: 19/19 (100%). Fase 5: 22/44. Fase 6: 1/40.
- Fase 5.B.3 esta reconciliada; a proxima frente em aberto permanece a Fase 5.B.4 (CLI para IAM) com continuidade ate 5.G.
