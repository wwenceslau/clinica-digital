# Tasks: Fundacao e Organizacao Modular da Plataforma Clinica

**Input**: Design documents from `/specs/002-definir-fundacao-modular/` (plan.md, spec.md, research.md, data-model.md, contracts/)  
**Prerequisites**: Plano técnico validado, pesquisa de IAM completa, modelo de dados baseline definido  
**Strategy**: TDD obrigatório (Art. I), CLI via cada módulo (Art. II), RLS em todas tabelas tenant-scoped (Art. 0), IAM in-app (Art. XXII), observabilidade ponta a ponta (Art. XI)  
**Parallelization**: Módulos bem-delimitados permitem entregar independentemente; US1 MVP, depois US2+US3  

---

## Constitution Validation Checkpoints (MANDATORY)

- [x] Toda tarefa de implementação é precedida por teste (TDD, Art. I)
- [x] Cada módulo tem contrato CLI + testes de contrato JSON (Art. II)
- [x] Isolamento tenant_id + RLS obrigatório em todas as entidades (Art. 0)
- [x] Sem uso de IdP externo; IAM 100% in-app (Art. XXII)
- [x] Propagação de trace_id + logs JSON estruturados (Art. XI)
- [x] Cada tarefa refencia FR(s) da spec.md
- [x] Verificação constitucional ao final de cada módulo/user story

---

## Phase 1: Setup - Estrutura do Projeto

**Purpose**: Inicializar estrutura Maven multi-module + frontend React

### Phase 1.A: Backend Maven e Shared Kernel

- [x] T001 Criar estrutura Maven multi-module com clinic-platform-bom (pom.xml raiz) em `backend/pom.xml`
- [x] T002 [P] Configurar clinic-shared-kernel module em `backend/clinic-shared-kernel/pom.xml` com classes base (TenantContext, TraceContext, DomainEvent, OperationOutcome)
- [x] T003 [P] Configurar clinic-tenant-core module em `backend/clinic-tenant-core/pom.xml`
- [x] T004 [P] Configurar clinic-iam-core module em `backend/clinic-iam-core/pom.xml`
- [x] T005 [P] Configurar clinic-observability-core module em `backend/clinic-observability-core/pom.xml`
- [x] T006 [P] Configurar clinic-gateway-app module em `backend/clinic-gateway-app/pom.xml` (Spring Boot entrypoint)
- [x] T007 [P] Configurar linting (SpotBugs, SonarQube) e testes (JUnit 5, Mockito) em profiles de build
- [x] T008 Integrar Testcontainers para testes de integração com PostgreSQL real em `backend/pom.xml`

### Phase 1.B: Frontend React com Atomic Components

- [x] T009 [P] Criar estrutura React 19 + TypeScript em `frontend/` com Node 22
- [x] T010 [P] Configurar Tailwind CSS + MUI 7 em `frontend/package.json`
- [x] T011 [P] Criar pastas de componentes atômicos: `frontend/src/components/atoms/`, `molecules/`, `organisms/`, `templates/`
- [x] T012 [P] Configurar eslint, prettier, Vitest para frontend em `frontend/`

### Phase 1.C: Database e Migrations

- [x] T013 Configurar Flyway ou Liquibase em `backend/clinic-shared-kernel/src/main/resources/db/migration/` e criar `backend/clinic-gateway-app/src/main/resources/application-dev.yml.template` com placeholders `${DB_URL}`, `${DB_USER}`, `${DB_PASSWORD}` (sem segredos versionados, refs: Art. VI)
- [x] T014 Definir migration V001 para criação de tabela base `tenants` (sem RLS ainda, apenas schema) em `V001__create_tenants_table.sql`

**Checkpoint**: Estrutura Maven + frontend pronta, tabelas base iniciadas

---

## Phase 2: Foundational - Infraestrutura Bloqueante

**Purpose**: Base de dados com RLS, contexto multitenant, IAM baseline, observabilidade comum  
**⚠️ CRITICAL**: Nenhuma user story pode começar até Phase 2 estar 100% completa

### Phase 2.A: Database Schema + RLS

- [x] T015 [P] Criar migration V002 para tabelas IAM base: `iam_users`, `iam_roles`, `iam_permissions`, `iam_user_roles` em `V002__create_iam_tables.sql` (refs: FR-005, FR-006, FR-006a)
- [x] T016 [P] Criar migration V003 para tabela `iam_sessions` com (id uuid, tenant_id, user_id, expires_at, revoked_at, ip, user_agent) em `V003__create_sessions_table.sql` (refs: FR-007, FR-010a)
- [x] T017 [P] Criar migration V004 para tabela `iam_audit_events` (append-only, sem update/delete) em `V004__create_audit_events_table.sql` (refs: FR-011, FR-012)
- [x] T018 [P] Criar migration V005 com RLS policies para `iam_users`: ENABLE RLS + FORCE RLS + POLICY for ALL using tenant_id em `V005__enable_rls_iam_users.sql` (refs: Art. 0, FR-001, FR-002)
- [x] T019 [P] Criar migration V006 com RLS policy para `iam_sessions` em `V006__enable_rls_iam_sessions.sql` (refs: Art. 0)
- [x] T020 [P] Criar migration V007 com RLS policy para `tenants` em `V007__enable_rls_tenants.sql` (refs: Art. 0)
- [x] T021 [P] Criar migration V008 com indices compostos (tenant_id, field) para perfomance em `V008__create_composite_indices.sql` (ex: iam_users(tenant_id, username), iam_sessions(tenant_id, expires_at)) (refs: research.md)
- [x] T022 Criar rollback scripts para migrations V001-V008 (obrigatório por constituição). Formato: SQL idempotente, nomeado R00N__rollback_{descricao}.sql, contendo DROP POLICY / DISABLE RLS / DROP TABLE / DROP INDEX conforme o inverso exato da migration correspondente, de forma que executar R00N restaura o schema ao estado pre-V00N.
- [x] T022a [P] Criar migration V009 com ENABLE FORCE RLS + policy FOR ALL para `iam_roles` em `V009__enable_rls_iam_roles.sql` e rollback idempotente `R009__rollback_rls_iam_roles.sql` (refs: Art. 0, data-model.md §RLS baseline)
- [x] T022b [P] Criar migration V010 com ENABLE FORCE RLS + policy FOR ALL via JOIN para `iam_user_roles` em `V010__enable_rls_iam_user_roles.sql` e rollback `R010__rollback_rls_iam_user_roles.sql` (refs: Art. 0, data-model.md §iam_user_roles)
- [x] T022c [P] Criar migration V011 com ENABLE FORCE RLS + policy INSERT + REVOKE UPDATE/DELETE para `iam_audit_events` em `V011__enable_rls_iam_audit_events.sql` e rollback `R011__rollback_rls_iam_audit_events.sql` (refs: Art. 0, FR-012, data-model.md §iam_audit_events)

### Phase 2.B: Shared Kernel - Contexto Multitenant

Testes para Shared Kernel:
- [x] T023 [P] Unit test para TenantContext (validação de tenant_id não-nulo, imutabilidade) em `backend/clinic-shared-kernel/src/test/java/.../TenantContextTest.java` (refs: FR-002, FR-002a)
- [x] T024 [P] Unit test para TraceContext (geração de trace_id, propagação) em `backend/clinic-shared-kernel/src/test/java/.../TraceContextTest.java` (refs: FR-010, FR-010a)
- [x] T025 [P] Unit test para OperationOutcome builder (formato FHIR, severities) em backend/clinic-shared-kernel/.../OperationOutcomeTest.java` (refs: Art. VI)

Implementação de Shared Kernel:
- [x] T026 [P] Implementar classe TenantContext com validações em `backend/clinic-shared-kernel/src/main/java/.../TenantContext.java` (refs: FR-002a)
- [x] T027 [P] Implementar classe TraceContext com geração UUID + propagação em `backend/clinic-shared-kernel/src/main/java/.../TraceContext.java` (refs: FR-010a)
- [x] T028 [P] Implementar OperationOutcome builder conforme FHIR R4 em `backend/clinic-shared-kernel/src/main/java/.../OperationOutcome.java` (refs: Art. VI)
- [x] T029 [P] Implementar TenantContextHolder para Spring RequestScope em `backend/clinic-shared-kernel/src/main/.../TenantContextHolder.java` (refs: Fr-002)
- [x] T030 [P] Implementar TraceContextHolder para Spring RequestScope (propagação entre layers) em `backend/clinic-shared-kernel/src/main/.../TraceContextHolder.java` (refs: FR-010a)
- [x] T030a [P] Unit test para TenantJdbcContextInterceptor: verificar que SET LOCAL é executado antes de qualquer statement em contexto com tenant, e que operação falha imediatamente sem tenant_id em `backend/clinic-shared-kernel/src/test/java/.../TenantJdbcContextInterceptorTest.java` (refs: FR-016, FR-016a)
- [x] T030b [P] Implementar TenantJdbcContextInterceptor (Spring TransactionSynchronization ou DataSource proxy) que executa `SET LOCAL app.tenant_id = :tenantId` no inicio de cada transacao, usando TenantContextHolder, em `backend/clinic-shared-kernel/src/main/java/.../TenantJdbcContextInterceptor.java` — cobrindo HTTP, CLI, @Async e @Scheduled (refs: FR-016, Art. 0)
- [x] T030c [P] Integration test com Testcontainers: validar que query em tabela tenant-scoped sem SET LOCAL prévio falha (não retorna linhas silenciosamente) e que query com SET LOCAL correto isola por tenant em `backend/clinic-shared-kernel/src/test/java/.../TenantRlsEnforcementIntegrationTest.java` (refs: FR-016, FR-016a, Art. 0)
- [x] T030d [P] Integration test com Testcontainers: dado Tenant A e Tenant B com sessoes ativas, quando credenciais de Tenant A sao renovadas, entao sessoes de Tenant B permanecem validas e token de Tenant A e rejeitado como sessao de Tenant B em `backend/clinic-shared-kernel/src/test/java/.../CrossTenantSessionIsolationIntegrationTest.java` (refs: FR-007a, Art. 0, Art. XXII)
- [x] T030e [P] Unit test: TenantMdcTaskDecorator deve copiar TenantContext e snapshot de MDC para a thread filha antes do handoff, e restaurar o MDC original apos execucao em `backend/clinic-shared-kernel/src/test/java/.../TenantMdcTaskDecoratorTest.java` (refs: FR-002b, FR-010a)
- [x] T030f [P] Implementar TenantMdcTaskDecorator (Spring TaskDecorator) que captura TenantContext + MDC snapshot da thread chamadora e os restaura na thread de execucao em `backend/clinic-shared-kernel/src/main/java/.../TenantMdcTaskDecorator.java` (refs: FR-002b); configurar como decorator padrao no AsyncConfigurer do gateway
- [x] T030g [P] Unit test: contexto de tenant e MDC MUST ser limpos (cleared) no finally de cada request/CLI invocation para evitar vazamento em pool de threads em `backend/clinic-shared-kernel/src/test/java/.../TenantContextCleanupTest.java` (refs: FR-002b, thread safety)

### Phase 2.C: Observability Core - Logging Estruturado + Tracing

Testes:
- [x] T031 [P] Unit test para JsonLogger com campos tenant_id, trace_id, operation, outcome em `backend/clinic-observability-core/src/test/java/.../JsonLoggerTest.java` (refs: FR-011)
- [x] T032 [P] Contract test para trace propagation end-to-end em `backend/clinic-observability-core/src/test/java/.../TraceContextPropagationContractTest.java` (refs: FR-010a)

Implementação:
- [x] T033 [P] Implementar JsonLogger usando Logback com MDC (tenant_id, trace_id) em `backend/clinic-observability-core/src/main/java/.../JsonLogger.java` (refs: FR-010, FR-011)
- [x] T034 [P] Configurar Logback para saída JSON estruturada em `backend/clinic-observability-core/src/main/resources/logback-spring.xml` (refs: FR-011)
- [x] T035 [P] Implementar TenantAwareMeterRegistry (Micrometer) com tags tenant_id em `backend/clinic-observability-core/src/main/java/.../TenantAwareMeterRegistry.java` (refs: Fr-010a)

### Phase 2.D: CI/CD + Gates Constitucionais

Nota de governanca: por aprovacao explicita do usuario em 2026-04-05, a execucao pode seguir para a Fase 2.B com o bloqueador de Art. I ainda aberto, desde que a pendencia de auditabilidade TDD seja mantida visivel e fechada antes do sign-off fundacional.

- [x] T036 Configurar script de drift verification (spec.md ↔ plan.md ↔ tasks.md ↔ código) em `.github/workflows/drift-verify.yml` (refs: Art. IX)
- [x] T037 Configurar SBOM generation (CycloneDX) em `.github/workflows/release.yml` para cada module release (refs: Art. XVI)
- [x] T038 Configurar blockers em CI: cobertura ≥80% unit, ≥90% IAM-critical (definido abaixo), SpotBugs + SonarQube (refs: Art. I). **Escopo "IAM-critical"** = classes nos pacotes `com.clinicadigital.iam` e `com.clinicadigital.shared.tenant` cujo nome termina em: `*PasswordHasher`, `*SessionRepository`, `*TenantContext`, `*TenantContextHolder`, `*SessionRevocation`, `*AuditEventWriter`, `*TenantJdbcContextInterceptor`. JaCoCo MUST incluir filtro de pacote `com/clinicadigital/iam/**` + `com/clinicadigital/shared/tenant/**` com threshold de instrucao ≥90% no perfil Maven `quality` (refs: Art. I, Art. 0, Art. XXII).
- [x] T039 Configurar checklist de validação constitucional em PR template `.github/PULL_REQUEST_TEMPLATE.md` (refs: Art. XIX)

**Checkpoint**: Database + RLS pronta, shared kernel disponível, observabilidade configurada, CI/CD bloqueante - **user stories podem começar agora**

---

## Phase 3: User Story 1 - Isolamento Multitenant como Base (Priority: P1) 🎯 MVP

**Goal**: Garantir que tenant_id seja identificado na fronteira, propagado por todas camadas, com RLS enforçando isolamento na persistência.  
**Independent Test**: Validar que uma requisição com tenant_id inválido é rejeitada imediatamente; uma requisição de tenant A não consegue acessar dados de tenant B mesmo se burlasse a aplicação.  
**Traceability**: FR-001, FR-002, FR-002a, FR-008, FR-009, FR-009a

### Phase 3.A: Testes para Isolamento Tenant (MANDATORY - write first)

- [x] T040 [P] [US1] Contract test para HTTP endpoint com validação de tenant_id obrigatório em `backend/clinic-gateway-app/src/test/java/.../TenantBoundaryContractTest.java` (refs: FR-002a)
- [x] T041 [P] [US1] Integration test com Testcontainers: criar 2 tenants, inserir dados em cada um, verificar RLS isola automaticamente em `backend/.../integration/TenantIsolationRLSTest.java` (refs: Art. 0, FR-001, FR-002)
- [x] T042 [P] [US1] Contract test para CLI command com --tenant obrigatório e fail-closed sem ele em `backend/clinic-tenant-core/src/test/.../TenantCLIContractTest.java` (refs: FR-002a)
- [x] T043 [P] [US1] Integration test: requisição sem header tenant_id deve retornar 403 Forbidden com OperationOutcome em `backend/.../integration/TenantMissingContextTest.java` (refs: FR-002a, Art. VI)

### Phase 3.B: Implementação de Tenant Context (Models + Services)

- [x] T044 [P] [US1] Implementar entity Tenant em `backend/clinic-tenant-core/src/main/java/.../domain/Tenant.java` com fields: id, slug, legal_name, status, plan_tier, quota_*, created_at, updated_at (refs: FR-001, FR-008)
- [x] T045 [P] [US1] Criar repository interface ITenantRepository em `backend/clinic-tenant-core/src/main/java/.../domain/ITenantRepository.java` com findBySlug, findById (refs: FR-013)
- [x] T046 [US1] Implementar TenantRepository usando Spring Data JPA com RLS-aware queries em `backend/clinic-tenant-core/src/main/java/.../infrastructure/TenantRepository.java` (depends: T044, T045; refs: Art. 0)
- [x] T047 [US1] Implementar TenantService in `backend/clinic-tenant-core/src/main/java/.../application/TenantService.java` com métodos: createTenant, getTenant, updateQuota (refs: FR-001, FR-008)

### Phase 3.C: HTTP Adapter - Boundary Validation

- [x] T048 [P] [US1] Criar TenantContextFilter (Spring Filter) que extrai tenant_id do header X-Tenant-ID e valida em `backend/clinic-gateway-app/src/main/java/.../filters/TenantContextFilter.java` (refs: FR-002a)
- [x] T049 [P] [US1] Criar TenantValidationAspect (Spring AOP) que enforce `SET LOCAL app.tenant_id` antes de qualquer query em `backend/clinic-gateway-app/src/main/java/.../aspects/TenantValidationAspect.java` (refs: Art. 0)
- [x] T050 [US1] Implementar endpoint POST /tenants/create em `backend/clinic-gateway-app/src/main/java/.../api/TenantController.java` (depends: T047; refs: FR-001)
- [x] T051 [US1] Implementar endpoint GET /tenants/{id} com validação de tenant_id em path + context em `backend/clinic-gateway-app/src/main/java/.../api/TenantController.java` (depends: T047; refs: FR-002)
- [x] T052 [US1] Adicionar error handler global que retorna FHIR OperationOutcome para tenant context missing/invalid em `backend/clinic-gateway-app/src/main/java/.../exception/GlobalExceptionHandler.java` (refs: FR-002a, Art. VI)

### Phase 3.D: CLI para Tenant Core

- [x] T053 [P] [US1] Criar CLI command `tenant create` em `backend/clinic-tenant-core/src/main/java/.../cli/TenantCommands.java` com args --slug, --legal-name, --plan-tier, suport --json output em JSON структурованы (refs: FR-004, Art. II)
- [x] T054 [P] [US1] Criar CLI command `tenant list` em `backend/clinic-tenant-core/.../cli/TenantCommands.java` com --json output (refs: Art. II)
- [x] T055 [P] [US1] Integrar CLI commands em gateway boot app via Spring Shell em `backend/clinic-gateway-app/src/main/java/.../CliShellConfig.java` (refs: Art. II)

### Phase 3.E: Quota Enforcement (Defesa contra Noisy Neighbors)

- [x] T056 [P] [US1] Criar QuotaService em `backend/clinic-tenant-core/src/main/java/.../application/QuotaService.java` com método: checkAndEnforceQuota(tenantId, metric) retorna void ou throws QuotaExceededException (refs: FR-008, FR-009a)
- [x] T057 [P] [US1] Criar QuotaBoundaryFilter (Spring Filter) que intercepta requisições, checa quotas por tenant_id, retorna 429 se excedido em `backend/clinic-gateway-app/src/main/java/.../filters/QuotaBoundaryFilter.java` (refs: FR-009a)
- [x] T058 [US1] Integration test: criar tenant com quota_requests_per_minute=5, disparar 6 requests, verificar que 6º retorna 429 QuotaExceeded em `backend/.../integration/QuotaEnforcementTest.java` (refs: FR-009a)
- [x] T059 [P] [US1] Tarefa Verificacao de Quotas para tenant operations em `backend/clinic-tenant-core/src/main/java/.../quota/TenantQuotaPolicy.yaml` (refs: Art. X, FR-008, FR-009a)

### Phase 3.F: Observabilidade para US1

- [x] T060 [P] [US1] Implementar trace_id em TenantContextFilter (gerar se ausente, preservar se valido) em `backend/clinic-gateway-app/src/main/java/.../filters/TenantContextFilter.java` (refs: FR-010a)
- [x] T061 [P] [US1] Adicionar logs JSON estruturados com tenant_id, trace_id, operation, outcome a todas querys de tenant em `backend/clinic-tenant-core/src/main/java/.../application/TenantService.java` (refs: FR-010, FR-011)
- [x] T062 [P] [US1] Configurar dashboard Grafana para monitorar tenant isolation (failed isolation attempts, quota blocks) em `observability/grafana/tenant-isolation.json` (refs: Art. XI)
- [x] T063 [P] [US1] Configurar alertas para tentativas de acesso cross-tenant em `observability/prometheus/tenant_isolation_rules.yml` (refs: Art. XI, FR-002)

### Phase 3.G: Validação Constitucional de US1

- [x] T064 [US1] Executar /speckit.checklist para validar conformidade de US1 com constituição (Art. 0, Art. I, Art. II, Art. XI) em `backend/` (refs: Art. XIX)
- [x] T065 [US1] Revisar edge cases de US1 em spec.md e atualizar Clarifications se novos riscos surgidos (refs: Art. XXI)

**Checkpoint**: US1 MVP concluída - isolamento tenant_id + RLS + quota enforcement funcionando, pronto para testes de aceitação e demo

---

## Phase 4: User Story 2 - Modularizacao por Bibliotecas + CLI (Priority: P2)

**Goal**: Garantir que cada módulo funcional (biblioteca Maven) é independente, reutilizável, com contrato CLI definido e testável.  
**Independent Test**: Cada módulo pode ser compilado, testado, deployado isoladamente sem quebrar outros módulos (exceto dependências explícitas).  
**Traceability**: FR-003, FR-004, FR-013, FR-014

### Phase 4.A: Testes para Modularização (MANDATORY)

- [x] T066 [P] [US2] Contract test para API pública de clinic-tenant-core em `backend/clinic-tenant-core/src/test/.../contract/TenantCorePublicAPITest.java` validando que públicas são apenas as esperadas (refs: FR-013)
- [x] T067 [P] [US2] Contract test para CLI de clinic-tenant-core (tenant create/list/update/delete com JSON) em `backend/clinic-tenant-core/src/test/.../contract/TenantCLIContractTest.java` (refs: FR-004, Art. II)
- [x] T068 [P] [US2] Contract test para API pública de clinic-iam-core em `backend/clinic-iam-core/src/test/.../contract/IAMCorePublicAPITest.java` (refs: FR-013)
- [x] T069 [P] [US2] Contract test para CLI de clinic-iam-core (auth login/logout/whoami com JSON) em `backend/clinic-iam-core/src/test/.../contract/IAMCLIContractTest.java` (refs: FR-004, Art. II, Art. XXII)
- [x] T070 [P] [US2] Contract test para API pública de clinic-observability-core em `backend/clinic-observability-core/src/test/.../contract/ObservabilityCorePublicAPITest.java` (refs: FR-013)

### Phase 4.B: Contratos e Limites de Módulos

- [x] T071 [P] [US2] Documentar API pública de clinic-tenant-core em `backend/clinic-tenant-core/docs/PUBLIC_API.md` com DTOs, métodos e versionamento (refs: FR-013)
- [x] T072 [P] [US2] Documentar API pública de clinic-iam-core em `backend/clinic-iam-core/docs/PUBLIC_API.md` (refs: FR-013)
- [x] T073 [P] [US2] Documentar API pública de clinic-observability-core em `backend/clinic-observability-core/docs/PUBLIC_API.md` (refs: FR-013)
- [x] T074 [P] [US2] Resolver qualquer acoplamento circular entre modules (se encontrado) em `backend/pom.xml` (refs: Art. III)

### Phase 4.C: CLI Expandida para Todos Módulos

- [x] T075 [P] [US2] Expandir TenantCommands com `tenant quota update`, `tenant block`, `tenant unblock` em `backend/clinic-tenant-core/src/main/java/.../cli/TenantCommands.java` (refs: FR-004, Art. II)
- [x] T076 [P] [US2] Criar QuotaCommands em `backend/clinic-tenant-core/src/main/java/.../cli/QuotaCommands.java` com `quota check <tenant-id>` suportando JSON output (refs: FR-004, Art. II)
- [x] T077 [P] [US2] Integrar observability commands em `backend/clinic-observability-core/src/main/java/.../cli/ObservabilityCommands.java` com `trace validate --trace-id`, `metrics export` (refs: FR-004, Art. II, FR-010a)

### Phase 4.D: Validação de Independência de Módulos

- [x] T078 [US2] Build test: cada módulo deve compilar/testar isoladamente (sem compilar outros) em `backend/` com `mvn clean verify -pl clinic-tenant-core` (refs: FR-014)
- [x] T079 [US2] Verificar que clinic-tenant-core não importa de clinic-iam-core (vs vice versa permitido) em `backend/` (refs: Art. III)
- [x] T080 [US2] Documentar diagrama de dependências entre módulos em `backend/docs/ARCHITECTURE.md` (refs: FR-003, FR-013)

### Phase 4.E: Observabilidade para US2

- [x] T081 [P] [US2] Implementar métricas Micrometer para CLI command execution time em `backend/clinic-shared-kernel/src/main/java/.../metrics/CLIMetrics.java` (refs: Art. II, Art. XI)
- [x] T082 [P] [US2] Adicionar logs JSON a cada CLI command entry/exit em `backend/clinic-gateway-app/src/main/java/.../cli/CliContextFilter.java` (refs: FR-010, FR-011)

### Phase 4.F: Validação Constitucional de US2

- [x] T083 [US2] Executar /speckit.checklist para validar conformidade de US2 (Art. III, Art. II, Art. XIII) em `backend/` (refs: Art. XIX)
- [x] T084 [US2] Validar que cada novo módulo tem SBOM em setup de release CI/CD (refs: Art. XVI)

**Checkpoint**: US2 concluída - módulos independentes, contratos definidos, CLI completa, CI/CD com SBOM

---

## Phase 5: User Story 3 - IAM Interno + Observabilidade Ponta a Ponta (Priority: P3)

**Goal**: Implementar autenticacao/sessão 100% in-app (sem IdP externo), com isolamento de tenant_id na sessão, revogação imediata e observabilidade end-to-end.  
**Independent Test**: Login com tenant + credenciais válidas cria sessão com trace_id, logout revoga imediatamente, tentativa de usar sessão revogada falha, outro tenant não consegue usar sessão do primeiro (RLS + contexto validado).  
**Traceability**: FR-005, FR-006, FR-006a, FR-007, FR-010, FR-010a, FR-011

### Phase 5.A: Testes para IAM (MANDATORY - TDD)

- [x] T085 [P] [US3] Unit test para BCryptPasswordEncoder (hash + verify) em `backend/clinic-iam-core/src/test/java/.../BCryptPasswordTest.java` (refs: FR-006, research.md)
- [x] T086 [P] [US3] Unit test para SessionManager (create, validate, revoke) em `backend/clinic-iam-core/src/test/java/.../SessionManagerTest.java` (refs: FR-007, FR-010a)
- [x] T087 [P] [US3] Integration test com RLS: criar 2 sessions em 2 tenants distintos, verificar que cada sessão filtra dados por tenant via RLS em `backend/.../integration/SessionIsolationRLSTest.java` (refs: Art. 0, FR-007)
- [x] T088 [P] [US3] Contract test para auth.login endpoint com validação de tenant_id + email + password em `backend/clinic-iam-core/src/test/.../contract/AuthLoginContractTest.java` (refs: FR-006a)
- [x] T089 [P] [US3] Contract test para CLI auth login/logout/whoami com suporte --json em `backend/clinic-iam-core/src/test/.../contract/AuthCLIContractTest.java` (refs: FR-004, Art. II, Art. XXII)
- [x] T090 [P] [US3] Integration test: login falha quando tenant_id é inválido/ausente em `backend/.../integration/AuthTenantBoundaryTest.java` (refs: FR-006a, Art. XXII)
- [x] T091 [P] [US3] Integration test: logout revoga sessão imediatamente (próxima requisição com mesmo session_id falha) em `backend/.../integration/SessionRevocationTest.java` (refs: FR-007)

### Phase 5.B: Implementação de IAM Interno

#### 5.B.1: Modelos e Persistência

- [x] T092 [P] [US3] Criar entity IamUser em `backend/clinic-iam-core/src/main/java/.../domain/IamUser.java` com: tenant_id, username, email, password_hash, is_active, last_login_at (refs: FR-006, FR-006a)
- [x] T093 [P] [US3] Criar entity IamRole em `backend/clinic-iam-core/src/main/java/.../domain/IamRole.java` com: tenant_id, role_key, description (refs: FR-006)
- [x] T094 [P] [US3] Criar entity IamSession em `backend/clinic-iam-core/src/main/java/.../domain/IamSession.java` com: id (uuid token), tenant_id, user_id, issued_at, expires_at, revoked_at, client_ip, user_agent, trace_id (refs: FR-007, FR-010a)
- [x] T095 [P] [US3] Criar entity IamAuditEvent append-only em `backend/clinic-iam-core/src/main/java/.../domain/IamAuditEvent.java` com: tenant_id, actor_user_id, event_type, outcome, trace_id, metadata_json, created_at (refs: FR-011, Art. VI)
- [x] T096 [P] [US3] Criar repository interfaces (IamUserRepository, IamSessionRepository, IamAuditEventRepository) em `backend/clinic-iam-core/src/main/java/.../domain/` (refs: FR-013)
- [x] T097 [US3] Implementar repositories com Spring Data JPA + RLS-aware queries em `backend/clinic-iam-core/src/main/java/.../infrastructure/` (refs: Art. 0)

#### 5.B.2: Serviços de Autenticação

- [x] T098 [P] [US3] Implementar PasswordService com BCrypt hashing (cost 12) em `backend/clinic-iam-core/src/main/java/.../application/PasswordService.java` com métodos: hashPassword, verifyPassword (refs: research.md, FR-006)
- [x] T099 [P] [US3] Implementar SessionManager em `backend/clinic-iam-core/src/main/java/.../application/SessionManager.java` com: createSession(user, tenantId, traceId), validateSession, revokeSession (refs: FR-007, FR-010a)
- [x] T100 [P] [US3] Implementar AuthenticationService em `backend/clinic-iam-core/src/main/java/.../application/AuthenticationService.java` com login, logout, whoami (refs: FR-005, FR-006a, Fr-007)
- [x] T101 [US3] Implementar AuditService em `backend/clinic-iam-core/src/main/java/.../application/AuditService.java` para registrar eventos de autenticação (login/logout/failed attempts) append-only (refs: FR-011, Art. VI)

#### 5.B.3: HTTP Adapter para IAM

- [x] T102 [P] [US3] Criar AuthenticationFilter (Spring) que valida Authorization header (session UUID) em cada request em `backend/clinic-gateway-app/src/main/java/.../filters/AuthenticationFilter.java` (refs: FR-006a)
- [x] T103 [US3] Implementar endpoint POST /auth/login em `backend/clinic-gateway-app/src/main/java/.../api/AuthController.java` com body {email, password, tenant_id}, retorna {session_id, expires_at} (refs: FR-005, FR-006a)
- [x] T104 [US3] Implementar endpoint POST /auth/logout em `backend/clinic-gateway-app/src/main/java/.../api/AuthController.java` revoga sessão atual (refs: FR-007)
- [x] T105 [US3] Implementar endpoint GET /auth/whoami em `backend/clinic-gateway-app/src/main/java/.../api/AuthController.java` retorna {user_id, email, tenant_id, roles} (refs: FR-006a)
- [x] T106 [US3] Adicionar validação de request.session_id != null em AuthenticationFilter, retorna FHIR OperationOutcome se null (refs: FR-006a, Art. VI)

#### 5.B.4: CLI para IAM

- [ ] T107 [P] [US3] Criar AuthCommands em `backend/clinic-iam-core/src/main/java/.../cli/AuthCommands.java` com: auth login --tenant --username --password, auth logout --session-id, auth whoami (refs: FR-004, Art. II, Art. XXII)
- [ ] T108 [P] [US3] Integrar AuthCommands via Spring Shell em `backend/clinic-gateway-app/src/main/java/.../CliShellConfig.java` (refs: Art. II)

### Phase 5.C: Observabilidade Ponta a Ponta

- [ ] T109 [P] [US3] Implementar trace_id propagation em AuthenticationFilter (gerar se ausente, armazenar em IamSession, propagar a downstream) em `backend/clinic-gateway-app/src/main/java/.../filters/AuthenticationFilter.java` (refs: FR-010a)
- [ ] T110 [P] [US3] Adicionar logs JSON estruturados a AuthenticationService.login/logout/verify com fields: tenant_id, user_id, trace_id, operation, outcome, session_duration (refs: FR-010, FR-011)
- [ ] T111 [P] [US3] Criar tabela audit_log_entries para registrar todos os eventos de autenticacao em `backend/clinic-iam-core/src/main/.../iam_audit_events.sql` (refs: FR-011)
- [ ] T112 [P] [US3] Integrar OpenTelemetry collector em `backend/clinic-gateway-app/src/main/resources/application.yml` para exportar traces (refs: FR-010a)
- [ ] T113 [P] [US3] Configurar Jaeger/Zipkin para visualizar traces ponta a ponta em `observability/docker-compose.yml` (refs: Fr-010a)
- [ ] T114 [US3] Criar dashboard Grafana para monitorar autenticacao (login_success_rate, session_revocation_rate, failed_auth_attempts) em `observability/grafana/iam-monitoring.json` (refs: Art. XI)

### Phase 5.D: Resiliência + Fallback

- [ ] T115 [US3] Implementar CircuitBreaker para SessionManager (timeout curto se DB lento) em `backend/clinic-iam-core/src/main/java/.../resilience/SessionCircuitBreaker.java` (refs: Art. XV, FR-007)
- [ ] T116 [US3] Implementar Redis cache para sessions ativas (TTL 30min) como fallback se postgres lento em `backend/clinic-gateway-app/src/main/java/.../cache/SessionCache.java` (refs: Art. XV, research.md)
- [ ] T117 [US3] Implementar retry policy com jitter para operações de leitura de sessão em `backend/clinic-iam-core/src/main/java/.../resilience/SessionRetryPolicy.java` (refs: Art. XV)
- [ ] T118 [US3] Integration test: sessão degradada com cache ativo continua validando requisições (fail-safe) em `backend/.../integration/SessionCacheFallbackTest.java` (refs: Art. XV)

### Phase 5.E: RBAC Service-Layer Enforcement

- [ ] T119 [P] [US3] Criar RBACPolicy em `backend/clinic-iam-core/src/main/java/.../authorization/RBACPolicy.yaml` mapeando role -> permissions (ex: tenant_admin -> iam.user.create, iam.user.block) (refs: Art. XVII, FR-006)
- [ ] T120 [P] [US3] Implementar AuthorizationService em `backend/clinic-iam-core/src/main/java/.../application/AuthorizationService.java` com método: hasPermission(user, permission) (refs: Art. XVII)
- [ ] T121 [US3] Criar AuthorizationAspect em `backend/clinic-gateway-app/src/main/java/.../aspects/AuthorizationAspect.java` que intercepta operações clinicas e checa permissões (refs: Art. XVII)
- [ ] T122 [US3] Integration test: usuário sem role `tenant_admin` não consegue executar `iam.user.create` (retorna 403 Forbidden com OperationOutcome) em `backend/.../integration/RBACEnforcementTest.java` (refs: Art. XVII, Fr-006)

### Phase 5.F: Cross-Tenant Operation Prevention

- [ ] T123 [US3] Implementar AdminContext (separado de normal TenantContext) para operações administrativas cross-tenant em `backend/clinic-shared-kernel/src/main/java/.../AdminContext.java` (refs: Fr-015)
- [ ] T124 [US3] Criar AdminContextFilter que valida `X-Admin-Token` (apenas em endpoints /admin/**) em `backend/clinic-gateway-app/src/main/java/.../filters/AdminContextFilter.java` (refs: Fr-015)
- [ ] T125 [US3] Garantir que nenhum endpoint public pode acessar dados cross-tenant sem AdminContext (validar via testes) em `backend/.../integration/CrossTenantPreventionTest.java` (refs: Fr-015)

### Phase 5.G: Validação Constitucional de US3

- [ ] T126 [US3] Executar /speckit.checklist para validar conformidade com Art. XXII (IAM in-app, sem IdP externo), Art. XI (observabilidade), Art. XV (resiliência) em `backend/` (refs: Art. XIX)
- [ ] T127 [US3] Code review: verificar que ZERO imports de Keycloak/Auth0/Okta/Cognito/Azure AD B2C em `backend/` (refs: Art. XXII)
- [ ] T128 [US3] Revisar edge cases de US3 em spec.md Edge Cases section e atualizar com novos riscos descobertos (refs: Art. XXI)

**Checkpoint**: US3 concluída - IAM in-app + RLS isolamento de sessão + observabilidade ponta a ponta funcionando

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Melhorias que afetam múltiplas user stories, validação final, documentação

### Phase 6.A: Frontend Atomic Components

- [ ] T129 [P] Criar atoms (Button, Input, Icon, Badge) em `frontend/src/components/atoms/` seguindo design tokens MUI 7 (refs: Art. III, Design System)
- [ ] T130 [P] Criar molecules (Form, Card, Header) em `frontend/src/components/molecules/` compostas por atoms (refs: Art. III)
- [ ] T131 [P] Criar organisms (LoginForm, TenantAdmin, SessionHistory) em `frontend/src/components/organisms/` compostas por molecules (refs: Art. III)
- [ ] T132 [P] Criar templates (AuthTemplate, AdminTemplate) em `frontend/src/components/templates/` (refs: Art. III)
- [ ] T133 [P] Testes Vitest para cada atom/molecule em `frontend/src/components/**/__tests__/` com snapshot testing (refs: Art. I)
- [ ] T134 Integração de frontend com backend em `frontend/src/services/api.ts` (auth login/logout, tenant list/create) (refs: Art. IV)
- [ ] T135 Testes end-to-end (E2E) com Playwright em `frontend/e2e/` para fluxos de login e isolamento visual por tenant (refs: Art. I, Art. 0)

### Phase 6.B: Documentação Final

- [ ] T136 [P] Gerar README.md em `backend/` com setup, build, test, run instructions (refs: Art. IV, Art. XIII)
- [ ] T137 [P] Gerar README.md em `frontend/` com setup, build, test, run instructions (refs: Art. IV, Art. XIII)
- [ ] T138 [P] Gerar ARCHITECTURE.md em `backend/docs/` com diagrama de módulos Maven, fluxos de tenant/IAM/observabilidade (refs: Art. IV, Art. XIII)
- [ ] T139 [P] Gerar SECURITY.md em `backend/docs/` documentando IAM in-app, isolamento de tenant, RLS policies, hashing strategy (refs: Art. VI, Art. XXII)
- [ ] T140 [P] Gerar OBSERVABILITY.md em `backend/docs/` documentando trace_id propagation, JSON logging, metrics, alertas (refs: Art. XI)
- [ ] T141 Gerar DEPLOYMENT.md com instruções de deployment em produção (vars de ambiente, migrations, SBOM publishing) (refs: Art. XVI, Art. IV)
- [ ] T142 Validar que todos módulos Maven têm arquivo PUBLIC_API.md ou API.md documentado (refs: Art. III, Art. XIII)

### Phase 6.C: Testing Coverage & Quality Gates

- [ ] T143 [P] Executar `mvn clean verify` em raiz backend para verificar cobertura ≥80% unit, ≥90% IAM-critical (refs: Art. I)
- [ ] T144 [P] Executar SpotBugs + SonarQube em todos os módulos backend (refs: Art. I)
- [ ] T145 [P] Executar OWASP Dependency-Check para identificar CVEs em dependências (refs: Art. XVI)
- [ ] T146 [P] Executar Testcontainers integration tests em PostgreSQL 15 real em `backend/.../src/test/.../` (refs: Art. 0, Art. I)
- [ ] T147 Gerar relatório de cobertura em HTML e validar manualmente (refs: Art. I, Art. XIX)

### Phase 6.D: CI/CD Final Setup

- [ ] T148 [P] Configurar GitHub Actions workflow para build + test + coverage gate em `.github/workflows/ci.yml` (refs: Art. IX)
- [ ] T149 [P] Configurar GitHub Actions workflow para drift verification em `.github/workflows/drift-verify.yml` (refs: Art. IX)
- [ ] T150 [P] Configurar GitHub Actions workflow para SBOM generation em `.github/workflows/release.yml` (refs: Art. XVI)
- [ ] T151 [P] Configurar GitHub Actions workflow para E2E frontend tests em `.github/workflows/frontend-e2e.yml` (refs: Art. I)
- [ ] T152 Testar CI/CD workflows em branch e validar que merge blockers funcionam (refs: Art. IX)

### Phase 6.E: Schema Compatibility & Event Contracts

- [ ] T153 [P] Definir schema inicial para eventos (iam.session.revoked.v1, tenant.quota.blocked.v1) em `backend/.../events/schema/` em JSON Schema (refs: Art. XIV)
- [ ] T154 [P] Validar schema compatibility com Schema Registry (Confluent) ou JSON Schema validator em CI (refs: Art. XIV)
- [ ] T155 [P] Documentar versionamento semantico de schemas em `backend/docs/EVENT_VERSIONING.md` (refs: Art. XIV)

### Phase 6.F: SBOM & Supply Chain

- [ ] T156 [P] Gerar SBOM (CycloneDX) para cada módulo Maven em release em `target/*.sbom.json` (refs: Art. XVI)
- [ ] T157 [P] Armazenar SBOM artifacts em artifact registry (GitHub Packages ou similar) junto com releases (refs: Art. XVI)
- [ ] T158 [P] Validar SBOM com dependency check tool em CI/CD (refs: Art. XVI)

### Phase 6.G: Quickstart Validation

- [ ] T159 Executar `quickstart.md` end-to-end (setup DB, build, run, executar CLI commands) em ambiente limpo e validar (refs: Art. IV, Art. XIII)
- [ ] T160 [P] Atualizar `quickstart.md` com novos módulos/commands descobertos durante implementação (refs: Art. IV)

### Phase 6.H: Research Validation

- [ ] T161 Validar que research.md findings sobre BCrypt + sessão stateful foram implementadas corretamente em código IAM (refs: Art. XX)
- [ ] T162 Validar RLS melhores práticas do research.md estão presentes: `SET LOCAL`, `FORCE RLS`, índices compostos, prepared statements em migrations e queries (refs: Art. XX, research.md)

### Phase 6.I: Incident/QA to Spec Feedback

- [ ] T163 Revisar qualquer incidente ou QA finding durante testes de US1/US2/US3 e atualizar `spec.md` Edge Cases section (refs: Art. XXI)
- [ ] T164 Criar task corretiva em `tasks.md` para cada edge case atualizado na spec (refs: Art. XXI)

### Phase 6.J: Constitutional Final Verification

- [x] T165 [P] Executar `speckit.checklist` completo (19 gates constitucionais) e documentar evidence links em `backend/docs/CONSTITUTION_VERIFICATION.md` (refs: Art. XIX)
- [ ] T166 [P] Revisar Complexity Tracking em plan.md e validar que nenhuma violacao do Art. VIII foi introduzida (refs: Art. VIII)
- [ ] T167 [P] Validar que todos FRs da spec.md foram mapeados a tasks completadas em tasks.md (rastreabilidade bidirecional) (refs: Art. IV)
- [ ] T168 Assinar compliance gate: cada reviewer (arquitetura + compliance LGPD) assina "aprovado para produção" em PR (refs: Phase 2.d: T039)

---

## Dependencies & Execution Order

### Phase Dependencies

1. **Phase 1 (Setup)**: Sem dependências → pode começar imediatamente
2. **Phase 2 (Foundational)**: Depende de Phase 1 completa → **BLOQUEIA todas user stories**
3. **Phase 3 (US1 MVP)**: Depende de Phase 2 completa → puede começar travado
4. **Phase 4 (US2)**: Depende de Phase 2 + [opcional] Phase 3 para integração
5. **Phase 5 (US3)**: Depende de Phase 2 + [opcional] Phase 3, Phase 4
6. **Phase 6 (Polish)**: Depende de Phase 3 + Phase 4 + Phase 5

### User Story Order (MVP Strategy)

- **Minimum Viable Product (MVP)**: Phase 1 + Phase 2 + Phase 3 (US1 only)
  - Isolamento tenant_id + RLS + quota enforcement funcionando
  - Pronto para primeiro cliente em produção

- **First Increment**: Phase 1 + Phase 2 + Phase 3 + Phase 4 (US2)
  - Adiciona modularização + CLI para operações

- **Full Foundation**: Phase 1 + Phase 2 + Phase 3 + Phase 4 + Phase 5
  - Adiciona IAM completo + observabilidade ponta a ponta

- **Release Ready**: Fases 1-6 completas
  - CI/CD pronto, documentação completa, compliance gate assinado

### Parallel Opportunities (Within Phase)

- **Setup (Phase 1)**: Todas tarefas [P] podem rodar em paralelo (Maven modules independentes)
- **Foundational (Phase 2)**: Migrations [P], Shared Kernel [P], Observability [P] em paralelo
- **US1 (Phase 3)**: Testes, Models, Services, HTTP Adapter, CLI, Quota em paralelo
- **US2 (Phase 4)**: Contratos, CLI expandida em paralelo
- **US3 (Phase 5)**: Testes IAM, Models, Serviços, HTTP, CLI em paralelo
- **Polish (Phase 6)**: Frontend atoms/molecules/organisms, documentação, CI/CD, SBOM em paralelo

### Example Parallel Gantt (1 team de 3 devs + 1 QA)

```
Week 1-2  Dev A: Phase 1 + 2.A (Setup + Database)
          Dev B: Phase 2.B (Shared Kernel)
          Dev C: Phase 2.C (Observability)
          QA:    Prepare test harness

Week 3    Dev A: Phase 3 (US1 - testes + modelos)
          Dev B: Phase 4 (US2 - contratos + CLI expandida)
          Dev C: Phase 5 (US3 - IAM models)
          QA:    Integration test Phase 2

Week 4    Dev A: Phase 3 (US1 - serviços + HTTP)
          Dev B: Phase 5 (US3 - IAM serviços)
          Dev C: Phase 6 (Frontend)
          QA:    Contract tests Phase 3

Week 5    Dev A: Phase 5 (US3 - HTTP + CLI)
          Dev B: Phase 6 (Docs + CI/CD)
          Dev C: Phase 6 (Frontend + E2E)
          QA:    E2E testing + compliance checklist

Week 6    Todos: Phase 6.J (Constitutional verification + final sign-off)
```

---

## Notes

- Cada task refencia pelo menos 1 FR da spec.md via `(refs: FR-XXX)`
- Cada task de módulo refencia sua user story via `[US1]`, `[US2]`, `[US3]`
- Tarefas [P] = podem rodar em paralelo (files distintos, sem dependências bloqueantes)
- Testes DEVEM ser escritos ANTES de implementação (TDD, Art. I obrigatório)
- Cada módulo Maven tem CLI + contract tests (Art. II obrigatório)
- Isolamento tenant_id + RLS em todas entidades (Art. 0 obrigatório)
- Zero imports de IdP externo (Art. XXII obrigatório)
- trace_id propagado ponta a ponta + JSON logs (Art. XI obrigatório)
- Cada task completada requer `/speckit.checklist` antes de fechar user story
- Este tasks.md pode ser executado diretamente via `/speckit.implement` para começar Phase 1

---

## Próximos Passos

1. **Revisar esta lista** com timee validar order das fases
2. **Começar Phase 1** (Setup Maven + Frontend structure)
3. **Desbloquear Phase 2** (fundação crítica) antes de paralelizar user stories
4. **MVP delivery** = Phase 1 + 2 + 3 (isolamento tenant funcionando)
5. **Incremental delivery** = adicionar US2, depois US3, então Polish
