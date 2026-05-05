# Tasks: Gestao Institucional e Autenticacao Nativa

**Input**: Design documents from `/specs/004-institution-iam-auth-integration/`
**Prerequisites**: plan.md, spec.md, data-model.md, contracts/, quickstart.md

**Tests**: Tests are MANDATORY. Every user story starts with failing tests before implementation.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Constitution Validation Checkpoints (MANDATORY)

- Test-first em todas as user stories
- CLI contract e CLI tests para fluxos principais
- Rastreabilidade por requirement IDs
- OperationOutcome padronizado para erros
- RLS e isolamento por tenant
- Observabilidade com trace_id + tenant-context
- Drift verification no CI
- RBAC e Sanitization/Validation Gate
- Naming compliance (DB snake_case, API camelCase, mapping DB->API->FHIR)
- IAM in-app only (sem IdP externo)
- RNDS validation via StructureDefinitions versionados carregados localmente
- Sessao opaca com cookie seguro em producao, sem `localStorage` e sem `silent refresh`

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: User story label (US1, US2...)
- Every task includes file path

## Phase 1: Setup (Shared Infrastructure)

- [x] T001 Consolidar baseline da feature e estrutura de artefatos em specs/004-institution-iam-auth-integration/tasks.md (refs: FR-021)
- [x] T002 [P] Configurar suíte de testes backend para a feature em backend/clinic-gateway-app/src/test/java (refs: FR-001, FR-003, FR-004)
- [x] T003 [P] Configurar suíte de testes frontend/e2e para a feature em frontend/src/test e frontend/e2e (refs: SC-001, SC-004)
- [x] T004 [P] Configurar validação de contratos OpenAPI/CLI no pipeline em .github/workflows (refs: FR-009, FR-010)

---

## Phase 2: Foundational (Blocking Prerequisites)

- [x] T005 Criar migration base de organizations, locations, practitioners, practitioner_roles, iam_users, iam_auth_challenges, iam_sessions e iam_audit_events em backend/clinic-gateway-app/src/main/resources/db/migration
- [x] T006 Aplicar políticas RLS nas tabelas organizations, locations, practitioners, practitioner_roles, iam_users, iam_groups, iam_user_groups, iam_sessions e iam_audit_events em backend/clinic-gateway-app/src/main/resources/db/migration (refs: FR-014)
- [x] T007 Implementar policy RLS explícita para super-user profile 0 em backend/clinic-gateway-app/src/main/resources/db/migration (refs: FR-014)
- [x] T008 [P] Implementar infraestrutura de criptografia pgcrypto para CPF/PII em backend/clinic-iam-core/src/main/java (refs: FR-011)
- [x] T009 [P] Implementar hashing de senha Argon2id/bcrypt compartilhado em backend/clinic-iam-core/src/main/java (refs: FR-011)
- [x] T010 [P] Implementar infraestrutura OperationOutcome compartilhada com `issue.severity`, `issue.code`, `issue.details.text` e `issue.diagnostics` em backend/clinic-iam-core/src/main/java (refs: FR-009)
- [x] T011 [P] Implementar middleware de trace_id + tenant-context logging em backend/clinic-gateway-app/src/main/java (refs: FR-016)
- [x] T012 [P] Implementar Sanitization and Validation Gate para API e CLI em backend/clinic-gateway-app/src/main/java (refs: FR-009, FR-018)
- [x] T013 [P] Configurar pacotes/artefatos versionados de StructureDefinitions RNDS carregados localmente em backend/clinic-iam-core/src/main/java (refs: FR-015, FR-020)
- [x] T014 [P] Implementar resolvedor central de `identifier.system` para CNES/CPF e mapping DB -> API -> FHIR em backend/clinic-iam-core/src/main/java (refs: FR-020, FR-022)
- [x] T015 [P] Implementar infraestrutura de sessão opaca com cookie seguro em produção e memória em dev em backend/clinic-gateway-app/src/main/java (refs: FR-007)
- [x] T016 Implementar framework central de rate limiting/quotas por `tenant_id` para toda superfície autenticada (API/CLI), incluindo lockout de login (5 tentativas/15 min, bloqueio 15 min) em backend/clinic-gateway-app/src/main/java (refs: FR-016, FR-023, FR-025)
- [x] T017 Definir mapa central de RBAC/permissões em backend/clinic-iam-core/src/main/java (refs: FR-005, FR-006)
- [x] T018 Definir contrato de integração com spec/003 para HeaderContext e MainTemplate em specs/004-institution-iam-auth-integration/quickstart.md (refs: FR-012, FR-013)
- [x] T019 [P] Configurar drift verification (`spec-kit-verify-tasks`) em .github/workflows (refs: SC-004)
- [x] T020 [P] Configurar geração SBOM para backend/frontend em .github/workflows (refs: FR-011, FR-021)
- [x] T021 Validar e fixar naming mapping DB->API->FHIR nas entidades base em specs/004-institution-iam-auth-integration/data-model.md (refs: FR-017, FR-020, FR-022)
- [x] T127 [P] Integrar provedor externo de segredos (Vault/KMS ou equivalente) para chave de criptografia PII em backend/clinic-gateway-app/src/main/resources e backend/clinic-iam-core/src/main/java (refs: FR-011)
- [x] T128 [P] Implementar e validar procedimento de rotação de chave para PII criptografada sem indisponibilidade em backend/clinic-iam-core/src/test/java (refs: FR-011)

**Checkpoint**: Foundation complete. User stories can proceed.

---

## Phase 3: User Story 1 - Bootstrap do Super-User via CLI (Priority: P0)

**Goal**: Permitir bootstrap seguro e auditado do super-user (profile 0).

**Independent Test**: Executar CLI de bootstrap em base vazia e em base já inicializada.

### Tests for User Story 1 (MANDATORY)

- [x] T022 [P] [US1] Criar contract test do comando bootstrap-super-user em backend/clinic-gateway-app/src/test/java
- [x] T023 [P] [US1] Criar integration test de unicidade de super-user em backend/clinic-gateway-app/src/test/java
- [x] T024 [P] [US1] Criar test de auditoria append-only para bootstrap em backend/clinic-gateway-app/src/test/java

### Implementation for User Story 1

- [x] T025 [US1] Implementar comando CLI BootstrapSuperUserCommand em backend/clinic-gateway-app/src/main/java (refs: FR-001, FR-002, FR-010)
- [x] T026 [US1] Implementar serviço de bootstrap com criação de iam_user + practitioner global em backend/clinic-iam-core/src/main/java (refs: FR-001, FR-002, FR-021)
- [x] T027 [US1] Implementar persistência de audit event do bootstrap em backend/clinic-iam-core/src/main/java (refs: FR-002, FR-016)
- [x] T028 [US1] Implementar retorno de erro em OperationOutcome para bootstrap duplicado em backend/clinic-gateway-app/src/main/java (refs: FR-009)
- [x] T029 [US1] Validar RNDS/meta.profile do practitioner bootstrap em specs/004-institution-iam-auth-integration/data-model.md (refs: FR-017, FR-020)

---

## Phase 4: User Story 2 - Criacao de Organizacao e Admin (Priority: P1)

**Goal**: Permitir ao super-user criar tenant e primeiro admin sem duplicidade e com validação de CNES.

**Independent Test**: Criar tenant/admin com sucesso e validar conflito por nome/CNES/email.

### Tests for User Story 2 (MANDATORY)

- [x] T030 [P] [US2] Criar contract test POST /api/admin/tenants em backend/clinic-gateway-app/src/test/java
- [x] T031 [P] [US2] Criar integration test de conflito de nome/CNES/email em backend/clinic-gateway-app/src/test/java
- [x] T032 [P] [US2] Criar integration test de criação transacional Organization + admin em backend/clinic-gateway-app/src/test/java
- [x] T033 [P] [US2] Criar CLI contract test create-tenant-admin em backend/clinic-gateway-app/src/test/java

### Implementation for User Story 2

- [x] T034 [US2] Implementar endpoint POST /api/admin/tenants em backend/clinic-gateway-app/src/main/java (refs: FR-003, FR-009, FR-022)
- [x] T035 [US2] Implementar serviço de criação transacional Organization + iam_user admin + practitioner em backend/clinic-iam-core/src/main/java (refs: FR-003, FR-017, FR-021)
- [x] T036 [US2] Implementar validação estrutural de CNES e unicidade na criação em backend/clinic-iam-core/src/main/java (refs: FR-022)
- [x] T037 [US2] Implementar validação RNDS para Organization e Practitioner na criação em backend/clinic-iam-core/src/main/java (refs: FR-015, FR-017, FR-020)
- [x] T038 [US2] Implementar operação CLI create-tenant-admin com JSON output em backend/clinic-gateway-app/src/main/java (refs: FR-010)
- [x] T039 [US2] Implementar logs estruturados + auditoria para create-tenant-admin em backend/clinic-iam-core/src/main/java (refs: FR-016)

---

## Phase 5: User Story 3 - Registro de Clinica via Formulario (Priority: P1)

**Goal**: Permitir registro público de clínica com validação RNDS/CNES e criação do primeiro admin.

**Independent Test**: Submeter formulário com payload válido e inválido.

### Tests for User Story 3 (MANDATORY)

- [x] T040 [P] [US3] Criar contract test POST /api/public/clinic-registration em backend/clinic-gateway-app/src/test/java
- [x] T041 [P] [US3] Criar integration test de validação RNDS e CNES no registro público em backend/clinic-gateway-app/src/test/java
- [x] T042 [P] [US3] Criar test frontend do formulário de registro em frontend/src/test
- [x] T043 [P] [US3] Criar e2e de registro público com sucesso e conflito de CNES em frontend/e2e

### Implementation for User Story 3

- [x] T044 [US3] Implementar endpoint público de registro de clínica e admin em backend/clinic-gateway-app/src/main/java (refs: FR-003, FR-009, FR-022)
- [x] T045 [US3] Reutilizar serviço transacional de criação institucional com validações específicas de entrada pública em backend/clinic-iam-core/src/main/java (refs: FR-003)
- [x] T046 [US3] Implementar organismo/molécula de registro de clínica em frontend/src/components (refs: FR-013)
- [x] T047 [US3] Integrar feedback de erro (Toast/Alert + OperationOutcome) no registro em frontend/src/app (refs: FR-009)

---

## Phase 6: User Story 4 - Login Multi-Perfil com Selecao de Organizacao (Priority: P1)

**Goal**: Autenticar por email/senha e resolver seleção automática/manual de organização.

**Independent Test**: Login com 0, 1 e múltiplos vínculos de organização.

### Tests for User Story 4 (MANDATORY)

- [x] T048 [P] [US4] Criar contract test POST /api/auth/login em backend/clinic-gateway-app/src/test/java
- [x] T049 [P] [US4] Criar contract test POST /api/auth/select-organization em backend/clinic-gateway-app/src/test/java
- [x] T050 [P] [US4] Criar integration test para modos single/multiple/no-org em backend/clinic-gateway-app/src/test/java
- [x] T051 [P] [US4] Criar integration test para challenge expirada e organização não permitida em backend/clinic-gateway-app/src/test/java
- [x] T052 [P] [US4] Criar e2e de login multi-tenant com seleção de organização em frontend/e2e

### Implementation for User Story 4

- [x] T053 [US4] Implementar serviço de login por email/senha com filtro de organizações e roles ativas em backend/clinic-iam-core/src/main/java (refs: FR-004, FR-019)
- [x] T054 [US4] Implementar endpoint /api/auth/login (mode single/multiple) em backend/clinic-gateway-app/src/main/java (refs: FR-004)
- [x] T055 [US4] Implementar tabela/serviço de `iam_auth_challenges` em backend/clinic-iam-core/src/main/java (refs: FR-004, FR-007)
- [x] T056 [US4] Implementar endpoint /api/auth/select-organization com challenge token em backend/clinic-gateway-app/src/main/java (refs: FR-004, FR-007)
- [x] T057 [US4] Implementar emissão de sessão opaca com cookie seguro em backend/clinic-iam-core/src/main/java (refs: FR-007)
- [x] T058 [US4] Implementar telas de Login e OrganizationSelection em frontend/src/app (refs: FR-004, FR-013)
- [x] T059 [US4] Integrar endpoint `/api/auth/login` ao framework central de lockout/rate limiting com retorno OperationOutcome em backend/clinic-gateway-app/src/main/java (refs: FR-016, FR-023, FR-025)

---

## Phase 7: User Story 7 - Feedback Visual e Erros (Priority: P1)

**Goal**: Exibir erros amigáveis e consistentes em OperationOutcome.

**Independent Test**: Forçar erros de autenticação/validação e verificar UX.

### Tests for User Story 7 (MANDATORY)

- [x] T060 [P] [US7] Criar unit test de parser OperationOutcome no frontend em frontend/src/test
- [x] T061 [P] [US7] Criar integration test de mapeamento de erros backend -> OperationOutcome em backend/clinic-gateway-app/src/test/java

### Implementation for User Story 7

- [x] T062 [US7] Implementar adapter unificado de erro no frontend (Toast/Alert do Shell) em frontend/src/services
- [x] T063 [US7] Padronizar respostas de erro no backend com `issue.details.text` e `issue.diagnostics` em backend/clinic-gateway-app/src/main/java
- [x] T064 [US7] Traduzir erros técnicos RNDS para mensagens amigáveis sem perder rastreabilidade em frontend/src/i18n (refs: FR-009, FR-015)

---

## Phase 8: User Story 9 - Seguranca e Criptografia (Priority: P1)

**Goal**: Garantir criptografia de CPF/PII, hash de senha e auditoria de operações sensíveis.

**Independent Test**: Validar persistência criptografada e auditoria para ações críticas.

### Tests for User Story 9 (MANDATORY)

- [x] T065 [P] [US9] Criar integration test de criptografia de CPF/PII em backend/clinic-iam-core/src/test/java
- [x] T066 [P] [US9] Criar integration test de hash de senha em backend/clinic-iam-core/src/test/java
- [x] T067 [P] [US9] Criar integration test de trilha de auditoria sensível em backend/clinic-iam-core/src/test/java

### Implementation for User Story 9

- [x] T068 [US9] Implementar serviço de criptografia AES-256-GCM (pgcrypto integration) para PII em backend/clinic-iam-core/src/main/java (refs: FR-011)
- [x] T069 [US9] Implementar política de hash de senha robusta em backend/clinic-iam-core/src/main/java (refs: FR-011)
- [x] T070 [US9] Implementar auditoria imutável para login/bootstrap/criação de tenant/logout em backend/clinic-iam-core/src/main/java (refs: FR-016, FR-024)

---

## Phase 9: User Story 5 - Persistencia e Contexto Multi-Tenant no Shell (Priority: P2)

**Goal**: Manter contexto autenticado de tenant, practitioner e sessão em toda navegação protegida.

**Independent Test**: Navegação entre rotas protegidas com/sem token válido.

### Tests for User Story 5 (MANDATORY)

- [x] T071 [P] [US5] Criar unit test de AuthContext/TenantContext em frontend/src/test
- [x] T072 [P] [US5] Criar e2e de persistência de contexto no header em frontend/e2e
- [x] T073 [P] [US5] Criar integration test de validação tenant claim em rotas protegidas em backend/clinic-gateway-app/src/test/java
- [x] T074 [P] [US5] Criar test de logout e limpeza de contexto no frontend em frontend/src/test

### Implementation for User Story 5

- [ ] T075 [US5] Implementar injeção de AuthProvider e TenantProvider no entrypoint em frontend/src/app/App.tsx (refs: FR-012)
  - **Problema identificado (HIGH/C4 — speckit.analyze 2026-05-01)**: `ShellPage` em `App.tsx` injeta `trainingContext` com valores **hardcoded** (`role: ['support']`, `tenant_id: 'tenant-clinica-digital'`, `user_id: 'admin@clinica.local'`) em vez de usar `useAuth()` e `useTenant()`. Todo RBAC guard e display condicional no shell opera com dados falsos, desacoplando completamente o contexto real de autenticação.
  - **Correção necessária**: Usar `useAuth()` para `session.practitioner.profileType` → `role` e `session.practitioner.id` → `user_id`; usar `useTenant()` para `tenant.id` → `tenant_id`. Coberto por T137 (Phase 15).
- [x] T076 [US5] Implementar contrato de contexto mínimo (`tenantId`, `organizationName`, `locationName`, `practitionerName`, `profileType`) em frontend/src/context (refs: FR-012)
- [x] T077 [US5] Integrar MainTemplate com contexto de practitioner/tenant em frontend/src/components/templates (refs: FR-008)
- [x] T078 [US5] Implementar guard de rotas com fallback para `/login` e limpeza de contexto na expiração de sessão em frontend/src/app (refs: FR-012, FR-024)
- [x] T079 [US5] Implementar validação obrigatória de tenant claim e tenant ativo no backend em backend/clinic-gateway-app/src/main/java (refs: FR-007)
- [x] T080 [US5] Implementar logout explícito com invalidação de cookie/sessão e redirect em frontend/src/app e backend/clinic-gateway-app/src/main/java (refs: FR-024)

---

## Phase 10: User Story 11 - Vinculo de Atuacao Profissional por Unidade (Priority: P2)

**Goal**: Resolver `PractitionerRole` e `Location` ativa como contexto operacional seguro.

**Independent Test**: Usuário com múltiplas locations seleciona contexto válido; acesso indevido é bloqueado.

### Tests for User Story 11 (MANDATORY)

- [x] T081 [P] [US11] Criar contract test GET /api/users/me/context em backend/clinic-gateway-app/src/test/java
- [x] T082 [P] [US11] Criar contract test POST /api/users/me/active-location em backend/clinic-gateway-app/src/test/java
- [x] T083 [P] [US11] Criar integration test de bloqueio por PractitionerRole inválido em backend/clinic-gateway-app/src/test/java
- [x] T084 [P] [US11] Criar e2e de seleção de location ativa e atualização do header em frontend/e2e

### Implementation for User Story 11

- [x] T085 [US11] Implementar resolvedor de `PractitionerRole` ativo por tenant/location em backend/clinic-iam-core/src/main/java (refs: FR-019)
- [x] T086 [US11] Implementar endpoint GET /api/users/me/context em backend/clinic-gateway-app/src/main/java (refs: FR-008, FR-019)
- [x] T087 [US11] Implementar endpoint POST /api/users/me/active-location em backend/clinic-gateway-app/src/main/java (refs: FR-018, FR-019)
- [x] T088 [US11] Integrar seleção e exibição de location ativa no frontend em frontend/src/context e frontend/src/components/templates (refs: FR-008)

---

## Phase 10b: Cadastro de Usuario Profile 20 pelo Admin (Priority: P2)

**Goal**: Permitir que administradores (profile 10) criem usuários (profile 20) dentro de seu tenant, com validação FHIR/RNDS e isolamento RLS.

**Independent Test**: Admin cria usuário profile 20; usuário consta no tenant correto; RLS impede acesso cruzado; auditoria registrada.

### Tests for Profile 20 User Creation (MANDATORY)

- [x] T089 [P] Criar contract test do endpoint POST /api/admin/users (perfil 20) em backend/clinic-gateway-app/src/test/java (refs: FR-006)
- [x] T090 [P] Criar integration test de criação de Practitioner profile 20 com RLS isolation em backend/clinic-gateway-app/src/test/java (refs: FR-006, FR-014)
- [x] T091 [P] Criar integration test de conflito de email duplicado por tenant em backend/clinic-gateway-app/src/test/java (refs: FR-006, FR-009)
- [x] T092 [P] Criar e2e do formulário de criação de usuário profile 20 em frontend/e2e (refs: FR-006, FR-013)

### Implementation for Profile 20 User Creation

- [x] T093 Implementar endpoint POST /api/admin/users em backend/clinic-gateway-app/src/main/java (refs: FR-006, FR-009)
- [x] T094 Implementar serviço de criação de iam_user + practitioner + practitioner_role profile 20 em backend/clinic-iam-core/src/main/java (refs: FR-006, FR-011, FR-015, FR-017, FR-019)
- [x] T095 Implementar unicidade de email por tenant e retorno OperationOutcome em backend/clinic-iam-core/src/main/java (refs: FR-009)
- [x] T096 Implementar auditoria imutável para criação de usuário profile 20 em backend/clinic-iam-core/src/main/java (refs: FR-016)
- [x] T097 Implementar formulário/modal de criação de usuário profile 20 no painel admin em frontend/src/components (refs: FR-006, FR-013)
- [x] T098 Integrar lista de usuários do tenant e roles ativas no painel admin em frontend/src/app (refs: FR-006, FR-019)

---

## Phase 11: User Story 6 - Gestao de Permissoes e Grupos (Priority: P2)

**Goal**: Permitir gestão de grupos/permissões e enforcement RBAC.

**Independent Test**: Criar grupo, atribuir usuário, bloquear acesso indevido.

### Tests for User Story 6 (MANDATORY)

- [X] T099 [P] [US6] Criar contract test dos endpoints RBAC (grupos/permissoes) em backend/clinic-gateway-app/src/test/java
- [X] T100 [P] [US6] Criar integration test de enforcement RBAC em backend/clinic-gateway-app/src/test/java
- [X] T101 [P] [US6] Criar teste frontend de visibilidade por permissão em frontend/src/test

### Implementation for User Story 6

- [X] T102 [US6] Implementar serviço de criação/atribuição de grupos e permissões em backend/clinic-iam-core/src/main/java (refs: FR-006)
- [X] T103 [US6] Implementar endpoints RBAC tenant-aware em backend/clinic-gateway-app/src/main/java (refs: FR-005, FR-006)
- [X] T104 [US6] Implementar componentes UI de gestão RBAC em frontend/src/components (refs: FR-006)
- [X] T105 [US6] Implementar service-layer permission checks nas operações clínicas em backend/clinic-gateway-app/src/main/java (refs: FR-005)

---

## Phase 12: User Story 8 - Integracao CLI e API (Priority: P2)

**Goal**: Executar fluxos críticos via CLI com saída JSON estruturada.

**Independent Test**: Rodar comandos bootstrap/login/select-organization/create-tenant-admin/logout em modo não interativo.

### Tests for User Story 8 (MANDATORY)

- [X] T106 [P] [US8] Criar CLI contract test para login e select-organization em backend/clinic-gateway-app/src/test/java
- [X] T107 [P] [US8] Criar CLI contract test para create-tenant-admin e bootstrap em backend/clinic-gateway-app/src/test/java
- [X] T108 [P] [US8] Criar CLI contract test para logout em backend/clinic-gateway-app/src/test/java

### Implementation for User Story 8

- [X] T109 [US8] Implementar comandos CLI login e select-organization em backend/clinic-gateway-app/src/main/java (refs: FR-010)
- [X] T110 [US8] Implementar comando CLI logout em backend/clinic-gateway-app/src/main/java (refs: FR-010, FR-024)
- [X] T111 [US8] Garantir payload JSON consistente entre CLI e API em backend/clinic-gateway-app/src/main/java (refs: FR-010)
- [X] T112 [US8] Implementar logs e códigos de saída determinísticos para CLI em backend/clinic-gateway-app/src/main/java (refs: FR-010, FR-016)

---

## Phase 13: User Story 10 - Padrao Atomico e Integracao Shell (Priority: P2)

**Goal**: Garantir padrões atômicos e integração visual/funcional com Shell.

**Independent Test**: Renderizar e navegar fluxos de Login/Registro no Shell com acessibilidade básica.

### Tests for User Story 10 (MANDATORY)

- [X] T113 [P] [US10] Criar unit tests para moléculas/organismos de Login/Registro em frontend/src/test
- [X] T114 [P] [US10] Criar e2e de integração Shell + Auth flows em frontend/e2e
- [X] T115 [P] [US10] Criar teste a11y básico para fluxos de autenticação em frontend/e2e

### Implementation for User Story 10

- [X] T116 [US10] Implementar/refinar moléculas de Login e Registro com MUI/Tailwind em frontend/src/components/molecules (refs: FR-013)
- [X] T117 [US10] Implementar/refinar organismos de Login e Registro em frontend/src/components/organisms (refs: FR-013)
- [X] T118 [US10] Integrar fluxos visuais de autenticacao nas rotas internas e `MainTemplate`, sem alterar a ordem de providers definida em US5, em frontend/src/app (refs: FR-012, FR-013)

---

## Phase 14: Polish & Cross-Cutting Concerns

- [X] T119 [P] Atualizar documentação técnica final da feature em specs/004-institution-iam-auth-integration/quickstart.md (refs: SC-001, SC-004)
- [X] T120 Atualizar checklist único com evidências de fases em specs/004-institution-iam-auth-integration/checklists/checklist.md (refs: SC-004)
- [X] T121 Atualizar contratos OpenAPI/CLI com endpoints, schemas e exemplos finais em specs/004-institution-iam-auth-integration/contracts (refs: FR-009, FR-010)
- [X] T122 Atualizar data-model com colunas/constraints/mappings finais em specs/004-institution-iam-auth-integration/data-model.md (refs: FR-017, FR-020, FR-022)
- [X] T123 Validar Fallback Strategy sob falha de dependência RNDS em backend/clinic-gateway-app/src/test/java (refs: FR-015, FR-020)
- [X] T124 Validar cobertura de RBAC + logs de auditoria + tenant desativado em backend/clinic-gateway-app/src/test/java (refs: FR-006, FR-014, FR-016)
- [X] T125 Executar checklist de consistência final (`/speckit.checklist`) e registrar resultado em specs/004-institution-iam-auth-integration/checklists/checklist.md (refs: SC-004)
- [X] T126 Rodar validação quickstart end-to-end e registrar evidência em specs/004-institution-iam-auth-integration/quickstart.md (refs: SC-001, SC-004)
- [X] T129 Executar teste de performance backend para meta de login p95 < 300ms e publicar evidência em specs/004-institution-iam-auth-integration/quickstart.md (refs: SC-001)
- [X] T130 Executar teste de performance frontend para render inicial de login < 1.5s (perfil 4G simulado) e publicar evidência em specs/004-institution-iam-auth-integration/quickstart.md (refs: SC-001)
- [x] T131 Validar quotas e rate limiting por `tenant_id` em endpoints autenticados e comandos CLI sensíveis com resposta OperationOutcome em backend/clinic-gateway-app/src/test/java (refs: FR-025)
- [X] T133 [P] Implementar suite de testes para validação visual de erros (SC-002): Testar que 100% de respostas de erro retornam FHIR OperationOutcome renderizado como Toast/Alert MUI 7 em 10+ cenários de erro (login inválido, email duplicado, CNES inválido, rate limit) em frontend/src/test (refs: SC-002)
- [x] T134 Implementar teste RLS isolamento cross-tenant (SC-003): Executar 5+ cenários de tentativa de acesso cross-tenant (profile 10 em tenant A tenta acessar dados de tenant B) e validar que 0 linhas são retornadas, com auditoria em iam_audit_events em backend/clinic-gateway-app/src/test/java (refs: SC-003)
- [x] T135 [P] Implementar teste de isolamento de performance multi-tenant (noisy-neighbor): Executar carga síncrona em 5 tenants simultâneos com spike de 1000 req/min em um tenant e validar que latência (p95) dos outros 4 tenants NOT aumenta >10% (Constitution Principle X) em backend/clinic-gateway-app/src/test/java

---

## Phase 15: Correcoes Criticas de Runtime — CRUD de Tenants (Priority: P0)

**Goal**: Resolver os problemas funcionais identificados no speckit.analyze (2026-05-01) que impedem o fluxo de criação de tenants via UI de funcionar end-to-end.

**Trigger**: Issues CRITICAL C1, C2, C3 e HIGH C4, C5, I1, D1 do relatório de análise.

**Independent Test**: Navegar até `/admin/tenants` como super-user (profile 0), criar tenant preenchendo todos os campos obrigatórios (organization name, CNES, adminDisplayName, adminEmail, adminCPF, adminPassword) e verificar inserção no banco via `AdminTenantController`.

### Tests for Phase 15 (MANDATORY)

- [X] T141 [P] [US2] Criar e2e de criação de tenant via formulário corrigido como super-user em frontend/e2e/tenant-admin.spec.ts (refs: FR-003, FR-022)
  - Verificar que campos CNES, adminDisplayName, adminEmail, adminCPF e adminPassword são renderizados
  - Verificar que submit chama `POST /api/admin/tenants` (não `/tenants/create`)
  - Verificar que Toast/Alert de sucesso é exibido após criação com status 201
  - Verificar que conflito de CNES retorna Toast de erro com texto do OperationOutcome
  - Verificar que formulário está oculto/desabilitado para profile 10/20 (RbacPermissionGuard ativo)

- [X] T142 [P] [US5] Criar unit test de `ShellPage` em frontend/src/test/ShellPage.test.tsx verificando que `trainingContext` reflete o contexto real de autenticação (refs: FR-007, FR-012)
  - Verificar que `user_id` corresponde a `session.practitioner.id`
  - Verificar que `role` corresponde a `session.practitioner.profileType`
  - Verificar que `tenant_id` corresponde a `tenant.id`
  - Verificar que ShellPage renderiza fallback quando session é null (sessão não carregada)

### Implementation for Phase 15

- [X] T136 [US2] Corrigir formulário `TenantAdmin.tsx` e serviço `tenantApi.ts` para chamar `POST /api/admin/tenants` com payload completo em frontend/src/components/organisms/TenantAdmin.tsx e frontend/src/services/tenantApi.ts (refs: FR-003, FR-009, FR-022)
  - **Problema C1 (CRITICAL)**: `tenantApi.ts` chama `POST /tenants/create` e `GET /tenants` (TenantController, rota sem autenticação) em vez de `POST /api/admin/tenants` e `GET /api/admin/tenants` (AdminTenantController). Rotas fora de `/api/**` bypassam completamente `AuthenticationFilter` (`shouldNotFilter` retorna `true`).
  - **Problema C2 (CRITICAL)**: Formulário tem apenas campo `newTenantName`. `AdminTenantController.CreateTenantAdminRequest` exige obrigatoriamente: `organization.displayName`, `organization.cnes` (7 dígitos), `adminPractitioner.displayName`, `adminPractitioner.email`, `adminPractitioner.cpf`, `adminPractitioner.password`.
  - **Problema C3 (CRITICAL)**: Nenhuma task anterior definiu a implementação do formulário frontend de criação de tenant conectado ao contrato real do `AdminTenantController`. TenantAdmin foi criado sem rastreabilidade de tarefa e sem aderir ao payload do endpoint.
  - **Checklist**:
    - [X] Adicionar campos ao formulário: `organizationDisplayName`, `cnes` (validação 7 dígitos), `adminDisplayName`, `adminEmail`, `adminCpf`, `adminPassword`
    - [X] Atualizar `createTenantApi()` em `tenantApi.ts` para chamar `POST /api/admin/tenants` com payload `CreateTenantAdminRequest`
    - [X] Atualizar `listTenants()` em `tenantApi.ts` para chamar endpoint autenticado correto (`GET /api/admin/tenants` ou equivalente)
    - [X] Remover uso de `X-Tenant-ID: 00000000-0000-0000-0000-000000000000` como substituto de sessão — autenticação via cookie opaco (sessão real)
    - [X] Manter botão submit `disabled` até todos os campos obrigatórios preenchidos e válidos

- [X] T137 [US5] Corrigir `ShellPage` em `frontend/src/app/App.tsx` para injetar contexto real de autenticação em `MainTemplate.trainingContext` em vez de valores hardcoded (refs: FR-007, FR-012, FR-014)
  - **Problema C4 (HIGH)**: `ShellPage` hardcoda `role: ['support']`, `tenant_id: 'tenant-clinica-digital'` e `user_id: 'admin@clinica.local'`. Todo RBAC guard e display condicional no shell usa dados falsos em vez do usuário autenticado real. Resolve T075 (reaberto).
  - **Checklist**:
    - [X] Chamar `useAuth()` dentro de `ShellPage` para obter `session`
    - [X] Chamar `useTenant()` dentro de `ShellPage` para obter `tenant`
    - [X] Mapear `session.practitioner.profileType` → `role` (array ou tipo compatível com `trainingContext`)
    - [X] Mapear `session.practitioner.id` (ou campo de identificação) → `user_id`
    - [X] Mapear `tenant.id` → `tenant_id`
    - [X] Preservar `trace_id` dinâmico (header de sessão ou uuid v4 por montagem)
    - [X] Retornar fallback (spinner ou null) quando `session` ainda não carregou para evitar piscar valores hardcoded

- [X] T138 [US2, US6] Adicionar `RbacPermissionGuard permission="iam.tenant.create"` ao formulário de criação de tenant em `frontend/src/app/App.tsx` (TenantAdminPage) (refs: FR-005, FR-006)
  - **Problema C5 (HIGH)**: `TenantAdminPage` não usa `RbacPermissionGuard`. Qualquer usuário autenticado (profile 10 ou 20) pode ver e tentar submeter o formulário de criação de tenant. Apenas profile 0 (super) tem permissão `iam.tenant.create` (conforme `RbacPermissionMap` e `RbacPermissionGuard`).
  - **Checklist**:
    - [X] Importar `RbacPermissionGuard` em `App.tsx`
    - [X] Envolver o bloco de criação de tenant com `<RbacPermissionGuard permission="iam.tenant.create">`
    - [X] Exibir mensagem informativa (ex: Alert MUI "Operação disponível apenas para super-usuário") quando guard rejeita renderização do formulário

- [X] T139 [US7] Corrigir tratamento de erros em `TenantAdminPage` — substituir `.catch(() => {})` por feedback `OperationOutcome` visível em `frontend/src/app/App.tsx` (refs: FR-009, SC-002)
  - **Problema I1 (HIGH)**: `listTenants()` e `createTenantApi()` em `TenantAdminPage` usam `.catch(() => {})`. Erros de API são silenciados completamente — usuário não recebe nenhum feedback de falha. Viola FR-009 (OperationOutcome padronizado) e SC-002 (100% dos erros renderizados como Toast/Alert).
  - **Checklist**:
    - [X] Substituir `.catch(() => {})` em chamada de `listTenants()` por handler que exibe Toast/Alert de erro com texto do OperationOutcome
    - [X] Substituir `.catch(() => {})` em chamada de `createTenantApi()` por handler que parseia `OperationOutcome.issue[0].details.text` e exibe mensagem amigável via Toast
    - [X] Reutilizar adapter unificado de erro implementado em T062 (`frontend/src/services`)
    - [X] Adicionar estado `loading` ao formulário durante submit (desabilitar botão enquanto aguarda resposta)
    - [X] Limpar formulário e atualizar lista de tenants após criação bem-sucedida (status 201)

- [X] T140 [US2] Avaliar e restringir `POST /tenants/create` em `TenantController` no backend em backend/clinic-gateway-app/src/main/java/com/clinicadigital/gateway/api/TenantController.java (refs: FR-007)
  - **Problema D1 (LOW)**: `TenantController` expõe `POST /tenants/create` e `GET /tenants` sem autenticação (rotas fora de `/api/**` bypassam `AuthenticationFilter`). Duplica funcionalidade de `AdminTenantController` e representa risco de segurança — permite criação de tenant por qualquer cliente sem sessão válida.
  - **Checklist**:
    - [X] Verificar se `POST /tenants/create` é referenciado em outro fluxo legítimo (além do frontend incorreto)
    - [X] Se não utilizado: adicionar `@Deprecated` e remover ou bloquear acesso externo (ex.: teste de integração que verifica retorno 404/403)
    - [X] Se utilizado por outro fluxo: mover para path `/api/admin/tenants/*` e garantir cobertura pelo `AuthenticationFilter`
    - [X] Atualizar testes de integração para não dependerem de `POST /tenants/create`

---

## Dependencies & Execution Order

### Phase Dependencies

- Setup (Phase 1): immediate
- Foundational (Phase 2): depends on Setup; blocks all user stories
- User stories (Phases 3-13): depend on Foundational
- Polish (Phase 14): depends on stories selected for entrega

### User Story Completion Order

1. US1 (P0)
2. US2 (P1)
3. US3 (P1)
4. US4 (P1)
5. US7 (P1)
6. US9 (P1)
7. US5 (P2)
8. US11 (P2)
9. Cadastro de usuario profile 20 (P2)
10. US6 (P2)
11. US8 (P2)
12. US10 (P2)

### Parallel Opportunities

- T002, T003, T004 can run in parallel
- T008, T009, T010, T011, T012, T013, T014, T015 can run in parallel
- Test tasks marked [P] within each story can run in parallel
- US5 and US11 can overlap after backend context endpoints are stable

---

## Parallel Example: User Story 4

```bash
Task: T048 Contract test POST /api/auth/login
Task: T049 Contract test POST /api/auth/select-organization
Task: T050 Integration test single/multiple/no-org
Task: T052 E2E login multi-tenant
```

---

## Implementation Strategy

### MVP First

1. Phase 1
2. Phase 2
3. US1
4. US2
5. US3
6. US4
7. Validate MVP (bootstrap + criar tenant/admin + registro público + login)

### Incremental Delivery

1. Entrega 1: US1 + US2 + US3 + US4
2. Entrega 2: US7 + US9 + US5
3. Entrega 3: US11 + profile 20 + US6 + US8 + US10

### Final Validation

1. Run naming DoD checklist
2. Run quickstart validation
3. Run consistency checklist before task closure

---

## Phase 16: Runtime Bug Fixes — Cadeia de Filtros e Super-User Session (2026-05-01)

**Goal**: Corrigir cascata de erros de runtime descobertos após a Phase 15: 403 → 401 → white page → 403+crash.

**Trigger**: Testes manuais pós-Phase 15 revelaram 4 bugs bloqueantes (RT1–RT4) que impediam qualquer fluxo pós-login de funcionar.

**Independent Test**: Fazer login como super-user (profile 0), navegar até `/admin/tenants` e verificar que: página carrega sem erros de console, lista de tenants é exibida, formulário de criação é visível.

### RT1 — 403 em GET /api/admin/tenants (X-Tenant-ID ausente)

- [X] T143 [US2] Adicionar `/api/admin/tenants` à lista de paths públicos em `TenantContextFilter.isPublicPath()` em backend/clinic-iam-core/src/main/java/.../filters/TenantContextFilter.java (refs: FR-014)
  - **Problema**: `TenantContextFilter` exigia `X-Tenant-ID` em todos os paths não-públicos. Super-user não envia esse header ao listar tenants.
  - **Fix**: Incluir `uri.startsWith("/api/admin/tenants")` em `isPublicPath()`.
- [X] T144 [US2] Adicionar `shouldNotFilter()` override em `QuotaBoundaryFilter` para ignorar `/api/admin/tenants` em backend/clinic-iam-core/src/main/java/.../filters/QuotaBoundaryFilter.java
  - **Problema**: `QuotaBoundaryFilter` aplicava verificação de quota antes da autenticação para essa rota.
  - **Fix**: Override `shouldNotFilter()` retorna `true` para paths `/api/admin/tenants`.

### RT2 — 401 "invalid or revoked session" em GET /api/admin/tenants

- [X] T145 [US5] Adicionar helper `superUserTenantScope()` em `AuthenticationFilter` para converter `SYSTEM_TENANT_ID` em `null` antes de `validateSession()` em backend/clinic-iam-core/src/main/java/.../filters/AuthenticationFilter.java (refs: FR-014)
  - **Problema**: Super-user tem sessão criada com `tenantId = null` no banco. `AuthenticationFilter` passava `SYSTEM_TENANT_ID` (UUID zeros) para `validateSession()`, que não encontrava a sessão.
  - **Fix**: `superUserTenantScope(UUID tenantId)` — se `tenantId == SYSTEM_TENANT_ID`, retorna `null`; caso contrário, retorna `tenantId`.
  - **Mudança associada**: `SYSTEM_TENANT_ID` promovido de `private static final` para `static final` em `TenantContextFilter` para ser acessível pelo filtro.

### RT3 — White page após login (session undefined no frontend)

- [X] T146 [US4] Corrigir mapeamento de resposta de login em `iamAuthApi.ts` — adicionar `buildSession()` para converter resposta flat do backend em `SessionIssuedResponse` aninhada em frontend/src/services/iamAuthApi.ts (refs: FR-007)
  - **Problema**: Backend retorna `LoginMultiOrgResponse` com campos flat (`sessionId`, `organizationId`, `userId`). Frontend esperava `SessionIssuedResponse` com estrutura aninhada (`session.practitioner.id`, `session.tenant.id`). Sem mapeamento, `session` ficava `undefined` e o shell renderizava página em branco.
  - **Fix**: Função `buildSession()` mapeia campos flat → `SessionIssuedResponse`. `practitioner.profileType = -1` como placeholder (valor real resolvido via `getMyContext()`).

### RT4 — 403 em GET /api/users/me/context + crash useTenant + tenant_id='null'

- [X] T147 [US5] Adicionar parâmetro `tenantId` em `getMyContext()` e enviar `X-Tenant-ID` header quando disponível em frontend/src/services/iamAuthApi.ts (refs: FR-008, FR-019)
  - **Problema (RT4a)**: `getMyContext()` não enviava `X-Tenant-ID`. `TenantContextFilter` rejeitava com 403.
  - **Fix**: Signature `getMyContext(tenantId?: string)`. Quando `tenantId` é fornecido, adiciona header `X-Tenant-ID`.

- [X] T148 [US5] Corrigir `TenantContext.tsx` — pular `getMyContext()` para super-user e passar `session.tenant.id` como tenantId em frontend/src/context/TenantContext.tsx (refs: FR-008)
  - **Problema (RT4a continuação)**: `TenantProvider` chamava `getMyContext()` sem tenantId.
  - **Fix**: Guard `if (session.tenant.id === SYSTEM_TENANT_ID) return;` antes da chamada. Caso contrário, chama `getMyContext(session.tenant.id)`.

- [X] T149 [US4, US5] Corrigir `buildSession()` em `iamAuthApi.ts` — detectar `organizationId == null` (super-user profile 0) e usar `SYSTEM_TENANT_ID` + `profileType = 0` em frontend/src/services/iamAuthApi.ts (refs: FR-007, FR-014)
  - **Problema (RT4c)**: `AuthenticationService.loginByEmail()` retorna `organizationId = null` para profile 0. `String(null!)` → `tenant.id = 'null'` (string). Telemetria mostrava `tenant_id: 'null'`.
  - **Fix**: Em `login()`, `isSuperUser = raw.organizationId == null`. `buildSession({ ..., tenantId: isSuperUser ? SYSTEM_TENANT_ID : String(raw.organizationId!), isSuperUser })`. `buildSession` usa `isSuperUser` para setar `profileType = 0`.

- [X] T150 [US5] Remover `useTenant()` de `ShellPage` em `frontend/src/app/App.tsx` — usar `session.tenant.id` diretamente em frontend/src/app/App.tsx (refs: FR-007)
  - **Problema (RT4b)**: `ShellPage` é renderizada dentro de `TenantProvider` mas o hook `useTenant()` gerava erro de contexto em certos re-renders pós-login. Error: `useTenant must be used inside TenantProvider at ShellPage (App.tsx:129:18)`.
  - **Fix**: Remover `const tenant = useTenant()` e substituir `tenant.tenantId ?? session.tenant.id` por `session.tenant.id` diretamente (que agora é sempre válido após RT4c).

### Outcome

Após RT1–RT4: login como super-user carrega `/admin/tenants` corretamente, lista de tenants é exibida, sem erros de console. Total de bugs de runtime resolvidos: **4** (12 sub-fixes em 4 arquivos backend + 3 arquivos frontend).

---

## Phase 17: Persistência Completa de Tenants — PUT/DELETE + Contratos/Testes (2026-05-01)

**Goal**: Fechar o CRUD de tenants end-to-end com persistência real de edição/exclusão e rastreabilidade atualizada em contratos/testes.

**Independent Test**: Como super-user, editar um tenant existente (slug/nome/plano) e excluir outro tenant em `/admin/tenants`, validando persistência no backend e atualização imediata na lista.

- [X] T151 [US2] Adicionar cobertura de contrato OpenAPI para `PUT/DELETE /api/admin/tenants/{tenantId}` em `specs/004-institution-iam-auth-integration/contracts/api-openapi.yaml` e `backend/clinic-gateway-app/src/test/java/com/clinicadigital/gateway/contract/CreateTenantAdminContractTest.java` (refs: FR-003, FR-009)
  - **Entregue**: path com `operationId updateTenant/deleteTenant`, schemas `UpdateTenantRequest` e `TenantSummaryResponse`, respostas `200/204/400/404`.

- [X] T152 [US2] Criar integration test de persistência para update/delete de tenant em `backend/clinic-gateway-app/src/test/java/com/clinicadigital/gateway/integration/TenantAdminUpdateDeleteIntegrationTest.java` (refs: FR-003)
  - **Entregue**: cenários de atualização persistida, conflito de slug, exclusão efetiva e not-found.

- [X] T153 [US2] Implementar endpoint backend de edição/exclusão de tenant em `backend/clinic-gateway-app/src/main/java/com/clinicadigital/gateway/api/AdminTenantController.java` + `backend/clinic-tenant-core/src/main/java/com/clinicadigital/tenant/application/TenantService.java` + `backend/clinic-tenant-core/src/main/java/com/clinicadigital/tenant/domain/ITenantRepository.java` + `backend/clinic-tenant-core/src/main/java/com/clinicadigital/tenant/infrastructure/TenantRepository.java` (refs: FR-003)
  - **Entregue**: `PUT /api/admin/tenants/{tenantId}` retorna `TenantSummaryResponse`; `DELETE /api/admin/tenants/{tenantId}` retorna `204`.

- [X] T154 [US2] Integrar frontend para persistência real de update/delete em `frontend/src/services/tenantApi.ts` e `frontend/src/app/App.tsx` (refs: FR-003, FR-009)
  - **Entregue**: `updateTenantApi` e `deleteTenantApi` consumidos em `TenantAdminPage` com feedback de erro via `OperationOutcome`.

- [X] T155 [US2] Corrigir edição de tenant para persistir payload completo de admin em `backend/clinic-gateway-app/src/main/java/com/clinicadigital/gateway/api/AdminTenantController.java` + `backend/clinic-gateway-app/src/main/java/com/clinicadigital/gateway/api/TenantAdminProfileService.java` + `frontend/src/services/tenantApi.ts` + `frontend/src/app/App.tsx` (refs: FR-003, FR-011)
  - **Entregue**: `PUT /api/admin/tenants/{tenantId}` agora atualiza tenant + admin (displayName/email/cpf/password), e `GET /api/admin/tenants` retorna dados de admin para reabrir modal com dados persistidos.
