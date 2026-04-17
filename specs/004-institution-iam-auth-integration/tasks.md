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

- [ ] T001 Consolidar baseline da feature e estrutura de artefatos em specs/004-institution-iam-auth-integration/tasks.md (refs: FR-021)
- [ ] T002 [P] Configurar suíte de testes backend para a feature em backend/clinic-gateway-app/src/test/java (refs: FR-001, FR-003, FR-004)
- [ ] T003 [P] Configurar suíte de testes frontend/e2e para a feature em frontend/src/test e frontend/e2e (refs: SC-001, SC-004)
- [ ] T004 [P] Configurar validação de contratos OpenAPI/CLI no pipeline em .github/workflows (refs: FR-009, FR-010)

---

## Phase 2: Foundational (Blocking Prerequisites)

- [ ] T005 Criar migration base de organizations, locations, practitioners, practitioner_roles, iam_users, iam_auth_challenges, iam_sessions e iam_audit_events em backend/clinic-gateway-app/src/main/resources/db/migration
- [ ] T006 Aplicar políticas RLS nas tabelas organizations, locations, practitioners, practitioner_roles, iam_users, iam_groups, iam_user_groups, iam_sessions e iam_audit_events em backend/clinic-gateway-app/src/main/resources/db/migration (refs: FR-014)
- [ ] T007 Implementar policy RLS explícita para super-user profile 0 em backend/clinic-gateway-app/src/main/resources/db/migration (refs: FR-014)
- [ ] T008 [P] Implementar infraestrutura de criptografia pgcrypto para CPF/PII em backend/clinic-iam-core/src/main/java (refs: FR-011)
- [ ] T009 [P] Implementar hashing de senha Argon2id/bcrypt compartilhado em backend/clinic-iam-core/src/main/java (refs: FR-011)
- [ ] T010 [P] Implementar infraestrutura OperationOutcome compartilhada com `issue.severity`, `issue.code`, `issue.details.text` e `issue.diagnostics` em backend/clinic-iam-core/src/main/java (refs: FR-009)
- [ ] T011 [P] Implementar middleware de trace_id + tenant-context logging em backend/clinic-gateway-app/src/main/java (refs: FR-016)
- [ ] T012 [P] Implementar Sanitization and Validation Gate para API e CLI em backend/clinic-gateway-app/src/main/java (refs: FR-009, FR-018)
- [ ] T013 [P] Configurar pacotes/artefatos versionados de StructureDefinitions RNDS carregados localmente em backend/clinic-iam-core/src/main/java (refs: FR-015, FR-020)
- [ ] T014 [P] Implementar resolvedor central de `identifier.system` para CNES/CPF e mapping DB -> API -> FHIR em backend/clinic-iam-core/src/main/java (refs: FR-020, FR-022)
- [ ] T015 [P] Implementar infraestrutura de sessão opaca com cookie seguro em produção e memória em dev em backend/clinic-gateway-app/src/main/java (refs: FR-007)
- [ ] T016 Implementar framework central de rate limiting/quotas por `tenant_id` para toda superfície autenticada (API/CLI), incluindo lockout de login (5 tentativas/15 min, bloqueio 15 min) em backend/clinic-gateway-app/src/main/java (refs: FR-016, FR-023, FR-025)
- [ ] T017 Definir mapa central de RBAC/permissões em backend/clinic-iam-core/src/main/java (refs: FR-005, FR-006)
- [ ] T018 Definir contrato de integração com spec/003 para HeaderContext e MainTemplate em specs/004-institution-iam-auth-integration/quickstart.md (refs: FR-012, FR-013)
- [ ] T019 [P] Configurar drift verification (`spec-kit-verify-tasks`) em .github/workflows (refs: SC-004)
- [ ] T020 [P] Configurar geração SBOM para backend/frontend em .github/workflows (refs: FR-011, FR-021)
- [ ] T021 Validar e fixar naming mapping DB->API->FHIR nas entidades base em specs/004-institution-iam-auth-integration/data-model.md (refs: FR-017, FR-020, FR-022)
- [ ] T127 [P] Integrar provedor externo de segredos (Vault/KMS ou equivalente) para chave de criptografia PII em backend/clinic-gateway-app/src/main/resources e backend/clinic-iam-core/src/main/java (refs: FR-011)
- [ ] T128 [P] Implementar e validar procedimento de rotação de chave para PII criptografada sem indisponibilidade em backend/clinic-iam-core/src/test/java (refs: FR-011)

**Checkpoint**: Foundation complete. User stories can proceed.

---

## Phase 3: User Story 1 - Bootstrap do Super-User via CLI (Priority: P0)

**Goal**: Permitir bootstrap seguro e auditado do super-user (profile 0).

**Independent Test**: Executar CLI de bootstrap em base vazia e em base já inicializada.

### Tests for User Story 1 (MANDATORY)

- [ ] T022 [P] [US1] Criar contract test do comando bootstrap-super-user em backend/clinic-gateway-app/src/test/java
- [ ] T023 [P] [US1] Criar integration test de unicidade de super-user em backend/clinic-gateway-app/src/test/java
- [ ] T024 [P] [US1] Criar test de auditoria append-only para bootstrap em backend/clinic-gateway-app/src/test/java

### Implementation for User Story 1

- [ ] T025 [US1] Implementar comando CLI BootstrapSuperUserCommand em backend/clinic-gateway-app/src/main/java (refs: FR-001, FR-002, FR-010)
- [ ] T026 [US1] Implementar serviço de bootstrap com criação de iam_user + practitioner global em backend/clinic-iam-core/src/main/java (refs: FR-001, FR-002, FR-021)
- [ ] T027 [US1] Implementar persistência de audit event do bootstrap em backend/clinic-iam-core/src/main/java (refs: FR-002, FR-016)
- [ ] T028 [US1] Implementar retorno de erro em OperationOutcome para bootstrap duplicado em backend/clinic-gateway-app/src/main/java (refs: FR-009)
- [ ] T029 [US1] Validar RNDS/meta.profile do practitioner bootstrap em specs/004-institution-iam-auth-integration/data-model.md (refs: FR-017, FR-020)

---

## Phase 4: User Story 2 - Criacao de Organizacao e Admin (Priority: P1)

**Goal**: Permitir ao super-user criar tenant e primeiro admin sem duplicidade e com validação de CNES.

**Independent Test**: Criar tenant/admin com sucesso e validar conflito por nome/CNES/email.

### Tests for User Story 2 (MANDATORY)

- [ ] T030 [P] [US2] Criar contract test POST /api/admin/tenants em backend/clinic-gateway-app/src/test/java
- [ ] T031 [P] [US2] Criar integration test de conflito de nome/CNES/email em backend/clinic-gateway-app/src/test/java
- [ ] T032 [P] [US2] Criar integration test de criação transacional Organization + admin em backend/clinic-gateway-app/src/test/java
- [ ] T033 [P] [US2] Criar CLI contract test create-tenant-admin em backend/clinic-gateway-app/src/test/java

### Implementation for User Story 2

- [ ] T034 [US2] Implementar endpoint POST /api/admin/tenants em backend/clinic-gateway-app/src/main/java (refs: FR-003, FR-009, FR-022)
- [ ] T035 [US2] Implementar serviço de criação transacional Organization + iam_user admin + practitioner em backend/clinic-iam-core/src/main/java (refs: FR-003, FR-017, FR-021)
- [ ] T036 [US2] Implementar validação estrutural de CNES e unicidade na criação em backend/clinic-iam-core/src/main/java (refs: FR-022)
- [ ] T037 [US2] Implementar validação RNDS para Organization e Practitioner na criação em backend/clinic-iam-core/src/main/java (refs: FR-015, FR-017, FR-020)
- [ ] T038 [US2] Implementar operação CLI create-tenant-admin com JSON output em backend/clinic-gateway-app/src/main/java (refs: FR-010)
- [ ] T039 [US2] Implementar logs estruturados + auditoria para create-tenant-admin em backend/clinic-iam-core/src/main/java (refs: FR-016)

---

## Phase 5: User Story 3 - Registro de Clinica via Formulario (Priority: P1)

**Goal**: Permitir registro público de clínica com validação RNDS/CNES e criação do primeiro admin.

**Independent Test**: Submeter formulário com payload válido e inválido.

### Tests for User Story 3 (MANDATORY)

- [ ] T040 [P] [US3] Criar contract test POST /api/public/clinic-registration em backend/clinic-gateway-app/src/test/java
- [ ] T041 [P] [US3] Criar integration test de validação RNDS e CNES no registro público em backend/clinic-gateway-app/src/test/java
- [ ] T042 [P] [US3] Criar test frontend do formulário de registro em frontend/src/test
- [ ] T043 [P] [US3] Criar e2e de registro público com sucesso e conflito de CNES em frontend/e2e

### Implementation for User Story 3

- [ ] T044 [US3] Implementar endpoint público de registro de clínica e admin em backend/clinic-gateway-app/src/main/java (refs: FR-003, FR-009, FR-022)
- [ ] T045 [US3] Reutilizar serviço transacional de criação institucional com validações específicas de entrada pública em backend/clinic-iam-core/src/main/java (refs: FR-003)
- [ ] T046 [US3] Implementar organismo/molécula de registro de clínica em frontend/src/components (refs: FR-013)
- [ ] T047 [US3] Integrar feedback de erro (Toast/Alert + OperationOutcome) no registro em frontend/src/app (refs: FR-009)

---

## Phase 6: User Story 4 - Login Multi-Perfil com Selecao de Organizacao (Priority: P1)

**Goal**: Autenticar por email/senha e resolver seleção automática/manual de organização.

**Independent Test**: Login com 0, 1 e múltiplos vínculos de organização.

### Tests for User Story 4 (MANDATORY)

- [ ] T048 [P] [US4] Criar contract test POST /api/auth/login em backend/clinic-gateway-app/src/test/java
- [ ] T049 [P] [US4] Criar contract test POST /api/auth/select-organization em backend/clinic-gateway-app/src/test/java
- [ ] T050 [P] [US4] Criar integration test para modos single/multiple/no-org em backend/clinic-gateway-app/src/test/java
- [ ] T051 [P] [US4] Criar integration test para challenge expirada e organização não permitida em backend/clinic-gateway-app/src/test/java
- [ ] T052 [P] [US4] Criar e2e de login multi-tenant com seleção de organização em frontend/e2e

### Implementation for User Story 4

- [ ] T053 [US4] Implementar serviço de login por email/senha com filtro de organizações e roles ativas em backend/clinic-iam-core/src/main/java (refs: FR-004, FR-019)
- [ ] T054 [US4] Implementar endpoint /api/auth/login (mode single/multiple) em backend/clinic-gateway-app/src/main/java (refs: FR-004)
- [ ] T055 [US4] Implementar tabela/serviço de `iam_auth_challenges` em backend/clinic-iam-core/src/main/java (refs: FR-004, FR-007)
- [ ] T056 [US4] Implementar endpoint /api/auth/select-organization com challenge token em backend/clinic-gateway-app/src/main/java (refs: FR-004, FR-007)
- [ ] T057 [US4] Implementar emissão de sessão opaca com cookie seguro em backend/clinic-iam-core/src/main/java (refs: FR-007)
- [ ] T058 [US4] Implementar telas de Login e OrganizationSelection em frontend/src/app (refs: FR-004, FR-013)
- [ ] T059 [US4] Integrar endpoint `/api/auth/login` ao framework central de lockout/rate limiting com retorno OperationOutcome em backend/clinic-gateway-app/src/main/java (refs: FR-016, FR-023, FR-025)

---

## Phase 7: User Story 7 - Feedback Visual e Erros (Priority: P1)

**Goal**: Exibir erros amigáveis e consistentes em OperationOutcome.

**Independent Test**: Forçar erros de autenticação/validação e verificar UX.

### Tests for User Story 7 (MANDATORY)

- [ ] T060 [P] [US7] Criar unit test de parser OperationOutcome no frontend em frontend/src/test
- [ ] T061 [P] [US7] Criar integration test de mapeamento de erros backend -> OperationOutcome em backend/clinic-gateway-app/src/test/java

### Implementation for User Story 7

- [ ] T062 [US7] Implementar adapter unificado de erro no frontend (Toast/Alert do Shell) em frontend/src/services
- [ ] T063 [US7] Padronizar respostas de erro no backend com `issue.details.text` e `issue.diagnostics` em backend/clinic-gateway-app/src/main/java
- [ ] T064 [US7] Traduzir erros técnicos RNDS para mensagens amigáveis sem perder rastreabilidade em frontend/src/i18n (refs: FR-009, FR-015)

---

## Phase 8: User Story 9 - Seguranca e Criptografia (Priority: P1)

**Goal**: Garantir criptografia de CPF/PII, hash de senha e auditoria de operações sensíveis.

**Independent Test**: Validar persistência criptografada e auditoria para ações críticas.

### Tests for User Story 9 (MANDATORY)

- [ ] T065 [P] [US9] Criar integration test de criptografia de CPF/PII em backend/clinic-iam-core/src/test/java
- [ ] T066 [P] [US9] Criar integration test de hash de senha em backend/clinic-iam-core/src/test/java
- [ ] T067 [P] [US9] Criar integration test de trilha de auditoria sensível em backend/clinic-iam-core/src/test/java

### Implementation for User Story 9

- [ ] T068 [US9] Implementar serviço de criptografia AES-256-GCM (pgcrypto integration) para PII em backend/clinic-iam-core/src/main/java (refs: FR-011)
- [ ] T069 [US9] Implementar política de hash de senha robusta em backend/clinic-iam-core/src/main/java (refs: FR-011)
- [ ] T070 [US9] Implementar auditoria imutável para login/bootstrap/criação de tenant/logout em backend/clinic-iam-core/src/main/java (refs: FR-016, FR-024)

---

## Phase 9: User Story 5 - Persistencia e Contexto Multi-Tenant no Shell (Priority: P2)

**Goal**: Manter contexto autenticado de tenant, practitioner e sessão em toda navegação protegida.

**Independent Test**: Navegação entre rotas protegidas com/sem token válido.

### Tests for User Story 5 (MANDATORY)

- [ ] T071 [P] [US5] Criar unit test de AuthContext/TenantContext em frontend/src/test
- [ ] T072 [P] [US5] Criar e2e de persistência de contexto no header em frontend/e2e
- [ ] T073 [P] [US5] Criar integration test de validação tenant claim em rotas protegidas em backend/clinic-gateway-app/src/test/java
- [ ] T074 [P] [US5] Criar test de logout e limpeza de contexto no frontend em frontend/src/test

### Implementation for User Story 5

- [ ] T075 [US5] Implementar injeção de AuthProvider e TenantProvider no entrypoint em frontend/src/app/App.tsx (refs: FR-012)
- [ ] T076 [US5] Implementar contrato de contexto mínimo (`tenantId`, `organizationName`, `locationName`, `practitionerName`, `profileType`) em frontend/src/context (refs: FR-012)
- [ ] T077 [US5] Integrar MainTemplate com contexto de practitioner/tenant em frontend/src/components/templates (refs: FR-008)
- [ ] T078 [US5] Implementar guard de rotas com fallback para `/login` e limpeza de contexto na expiração de sessão em frontend/src/app (refs: FR-012, FR-024)
- [ ] T079 [US5] Implementar validação obrigatória de tenant claim e tenant ativo no backend em backend/clinic-gateway-app/src/main/java (refs: FR-007)
- [ ] T080 [US5] Implementar logout explícito com invalidação de cookie/sessão e redirect em frontend/src/app e backend/clinic-gateway-app/src/main/java (refs: FR-024)

---

## Phase 10: User Story 11 - Vinculo de Atuacao Profissional por Unidade (Priority: P2)

**Goal**: Resolver `PractitionerRole` e `Location` ativa como contexto operacional seguro.

**Independent Test**: Usuário com múltiplas locations seleciona contexto válido; acesso indevido é bloqueado.

### Tests for User Story 11 (MANDATORY)

- [ ] T081 [P] [US11] Criar contract test GET /api/users/me/context em backend/clinic-gateway-app/src/test/java
- [ ] T082 [P] [US11] Criar contract test POST /api/users/me/active-location em backend/clinic-gateway-app/src/test/java
- [ ] T083 [P] [US11] Criar integration test de bloqueio por PractitionerRole inválido em backend/clinic-gateway-app/src/test/java
- [ ] T084 [P] [US11] Criar e2e de seleção de location ativa e atualização do header em frontend/e2e

### Implementation for User Story 11

- [ ] T085 [US11] Implementar resolvedor de `PractitionerRole` ativo por tenant/location em backend/clinic-iam-core/src/main/java (refs: FR-019)
- [ ] T086 [US11] Implementar endpoint GET /api/users/me/context em backend/clinic-gateway-app/src/main/java (refs: FR-008, FR-019)
- [ ] T087 [US11] Implementar endpoint POST /api/users/me/active-location em backend/clinic-gateway-app/src/main/java (refs: FR-018, FR-019)
- [ ] T088 [US11] Integrar seleção e exibição de location ativa no frontend em frontend/src/context e frontend/src/components/templates (refs: FR-008)

---

## Phase 10b: Cadastro de Usuario Profile 20 pelo Admin (Priority: P2)

**Goal**: Permitir que administradores (profile 10) criem usuários (profile 20) dentro de seu tenant, com validação FHIR/RNDS e isolamento RLS.

**Independent Test**: Admin cria usuário profile 20; usuário consta no tenant correto; RLS impede acesso cruzado; auditoria registrada.

### Tests for Profile 20 User Creation (MANDATORY)

- [ ] T089 [P] Criar contract test do endpoint POST /api/admin/users (perfil 20) em backend/clinic-gateway-app/src/test/java (refs: FR-006)
- [ ] T090 [P] Criar integration test de criação de Practitioner profile 20 com RLS isolation em backend/clinic-gateway-app/src/test/java (refs: FR-006, FR-014)
- [ ] T091 [P] Criar integration test de conflito de email duplicado por tenant em backend/clinic-gateway-app/src/test/java (refs: FR-006, FR-009)
- [ ] T092 [P] Criar e2e do formulário de criação de usuário profile 20 em frontend/e2e (refs: FR-006, FR-013)

### Implementation for Profile 20 User Creation

- [ ] T093 Implementar endpoint POST /api/admin/users em backend/clinic-gateway-app/src/main/java (refs: FR-006, FR-009)
- [ ] T094 Implementar serviço de criação de iam_user + practitioner + practitioner_role profile 20 em backend/clinic-iam-core/src/main/java (refs: FR-006, FR-011, FR-015, FR-017, FR-019)
- [ ] T095 Implementar unicidade de email por tenant e retorno OperationOutcome em backend/clinic-iam-core/src/main/java (refs: FR-009)
- [ ] T096 Implementar auditoria imutável para criação de usuário profile 20 em backend/clinic-iam-core/src/main/java (refs: FR-016)
- [ ] T097 Implementar formulário/modal de criação de usuário profile 20 no painel admin em frontend/src/components (refs: FR-006, FR-013)
- [ ] T098 Integrar lista de usuários do tenant e roles ativas no painel admin em frontend/src/app (refs: FR-006, FR-019)

---

## Phase 11: User Story 6 - Gestao de Permissoes e Grupos (Priority: P2)

**Goal**: Permitir gestão de grupos/permissões e enforcement RBAC.

**Independent Test**: Criar grupo, atribuir usuário, bloquear acesso indevido.

### Tests for User Story 6 (MANDATORY)

- [ ] T099 [P] [US6] Criar contract test dos endpoints RBAC (grupos/permissoes) em backend/clinic-gateway-app/src/test/java
- [ ] T100 [P] [US6] Criar integration test de enforcement RBAC em backend/clinic-gateway-app/src/test/java
- [ ] T101 [P] [US6] Criar teste frontend de visibilidade por permissão em frontend/src/test

### Implementation for User Story 6

- [ ] T102 [US6] Implementar serviço de criação/atribuição de grupos e permissões em backend/clinic-iam-core/src/main/java (refs: FR-006)
- [ ] T103 [US6] Implementar endpoints RBAC tenant-aware em backend/clinic-gateway-app/src/main/java (refs: FR-005, FR-006)
- [ ] T104 [US6] Implementar componentes UI de gestão RBAC em frontend/src/components (refs: FR-006)
- [ ] T105 [US6] Implementar service-layer permission checks nas operações clínicas em backend/clinic-gateway-app/src/main/java (refs: FR-005)

---

## Phase 12: User Story 8 - Integracao CLI e API (Priority: P2)

**Goal**: Executar fluxos críticos via CLI com saída JSON estruturada.

**Independent Test**: Rodar comandos bootstrap/login/select-organization/create-tenant-admin/logout em modo não interativo.

### Tests for User Story 8 (MANDATORY)

- [ ] T106 [P] [US8] Criar CLI contract test para login e select-organization em backend/clinic-gateway-app/src/test/java
- [ ] T107 [P] [US8] Criar CLI contract test para create-tenant-admin e bootstrap em backend/clinic-gateway-app/src/test/java
- [ ] T108 [P] [US8] Criar CLI contract test para logout em backend/clinic-gateway-app/src/test/java

### Implementation for User Story 8

- [ ] T109 [US8] Implementar comandos CLI login e select-organization em backend/clinic-gateway-app/src/main/java (refs: FR-010)
- [ ] T110 [US8] Implementar comando CLI logout em backend/clinic-gateway-app/src/main/java (refs: FR-010, FR-024)
- [ ] T111 [US8] Garantir payload JSON consistente entre CLI e API em backend/clinic-gateway-app/src/main/java (refs: FR-010)
- [ ] T112 [US8] Implementar logs e códigos de saída determinísticos para CLI em backend/clinic-gateway-app/src/main/java (refs: FR-010, FR-016)

---

## Phase 13: User Story 10 - Padrao Atomico e Integracao Shell (Priority: P2)

**Goal**: Garantir padrões atômicos e integração visual/funcional com Shell.

**Independent Test**: Renderizar e navegar fluxos de Login/Registro no Shell com acessibilidade básica.

### Tests for User Story 10 (MANDATORY)

- [ ] T113 [P] [US10] Criar unit tests para moléculas/organismos de Login/Registro em frontend/src/test
- [ ] T114 [P] [US10] Criar e2e de integração Shell + Auth flows em frontend/e2e
- [ ] T115 [P] [US10] Criar teste a11y básico para fluxos de autenticação em frontend/e2e

### Implementation for User Story 10

- [ ] T116 [US10] Implementar/refinar moléculas de Login e Registro com MUI/Tailwind em frontend/src/components/molecules (refs: FR-013)
- [ ] T117 [US10] Implementar/refinar organismos de Login e Registro em frontend/src/components/organisms (refs: FR-013)
- [ ] T118 [US10] Integrar fluxos visuais de autenticacao nas rotas internas e `MainTemplate`, sem alterar a ordem de providers definida em US5, em frontend/src/app (refs: FR-012, FR-013)

---

## Phase 14: Polish & Cross-Cutting Concerns

- [ ] T119 [P] Atualizar documentação técnica final da feature em specs/004-institution-iam-auth-integration/quickstart.md (refs: SC-001, SC-004)
- [ ] T120 Atualizar checklist único com evidências de fases em specs/004-institution-iam-auth-integration/checklists/checklist.md (refs: SC-004)
- [ ] T121 Atualizar contratos OpenAPI/CLI com endpoints, schemas e exemplos finais em specs/004-institution-iam-auth-integration/contracts (refs: FR-009, FR-010)
- [ ] T122 Atualizar data-model com colunas/constraints/mappings finais em specs/004-institution-iam-auth-integration/data-model.md (refs: FR-017, FR-020, FR-022)
- [ ] T123 Validar Fallback Strategy sob falha de dependência RNDS em backend/clinic-gateway-app/src/test/java (refs: FR-015, FR-020)
- [ ] T124 Validar cobertura de RBAC + logs de auditoria + tenant desativado em backend/clinic-gateway-app/src/test/java (refs: FR-006, FR-014, FR-016)
- [ ] T125 Executar checklist de consistência final (`/speckit.checklist`) e registrar resultado em specs/004-institution-iam-auth-integration/checklists/checklist.md (refs: SC-004)
- [ ] T126 Rodar validação quickstart end-to-end e registrar evidência em specs/004-institution-iam-auth-integration/quickstart.md (refs: SC-001, SC-004)
- [ ] T129 Executar teste de performance backend para meta de login p95 < 300ms e publicar evidência em specs/004-institution-iam-auth-integration/quickstart.md (refs: SC-001)
- [ ] T130 Executar teste de performance frontend para render inicial de login < 1.5s (perfil 4G simulado) e publicar evidência em specs/004-institution-iam-auth-integration/quickstart.md (refs: SC-001)
- [ ] T131 Validar quotas e rate limiting por `tenant_id` em endpoints autenticados e comandos CLI sensíveis com resposta OperationOutcome em backend/clinic-gateway-app/src/test/java (refs: FR-025)

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
