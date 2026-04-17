# Checklist Unico de Execucao - Feature 004

**Feature**: [spec.md](../spec.md)
**Plano**: [plan.md](../plan.md)
**Tasks**: [tasks.md](../tasks.md)

- Este e o unico checklist operacional da feature.
- Deve ser atualizado ao final de cada fase executada.
- Nenhuma fase pode ser marcada como concluida sem evidencias correspondentes.

## Gates Fundamentais

- [ ] Gate G001 - Migrations de `organizations`, `locations`, `practitioners`, `practitioner_roles`, `iam_users`, `iam_auth_challenges`, `iam_sessions` e `iam_audit_events` aplicadas (Tasks: T005)
- [ ] Gate G002 - Policies RLS separadas para perfis 0, 10 e 20 validadas (Tasks: T006, T007)
- [ ] Gate G003 - Validacao RNDS com pacotes locais configurada (Tasks: T013)
- [ ] Gate G004 - Sessao opaca com cookie seguro em producao e memoria em dev validada (Tasks: T015)
- [ ] Gate G005 - Framework central de rate limiting e quotas por tenant validado (Tasks: T016, T131)
- [ ] Gate G006 - Secret manager/KMS para chaves PII configurado (Tasks: T127)
- [ ] Gate G007 - Procedimento de rotacao de chave validado sem indisponibilidade (Tasks: T128)

## Controle de Fases

### Phase 1 - Setup
- [ ] Phase 1.1 - Estrutura de artefatos consolidada (Tasks: T001)
- [ ] Phase 1.2 - Suites de teste backend, frontend e e2e inicializadas (Tasks: T002, T003)
- [ ] Phase 1.3 - Validacao de contratos OpenAPI e CLI ativa no pipeline (Tasks: T004)
- [ ] Phase 1 - Concluida (Tasks: T001, T002, T003, T004)

### Phase 2 - Foundational
- [ ] Phase 2.1 - Migrations IAM + RLS aplicadas (Tasks: T005, T006, T007)
- [ ] Phase 2.2 - Trace e tenant logging ativos (Tasks: T011)
- [ ] Phase 2.3 - OperationOutcome compartilhado configurado (Tasks: T010)
- [ ] Phase 2.4 - Naming mapping DB -> API -> FHIR validado (Tasks: T021)
- [ ] Phase 2.5 - Drift verification + SBOM no CI (Tasks: T019, T020)
- [ ] Phase 2.6 - Quotas e rate limiting por tenant configurados (Tasks: T016)
- [ ] Phase 2.7 - Secret manager/KMS e rotacao de chave validados (Tasks: T127, T128)
- [ ] Phase 2 - Concluida (Tasks: T005, T006, T007, T008, T009, T010, T011, T012, T013, T014, T015, T016, T017, T018, T019, T020, T021, T127, T128)

### Phase 3 - US1 (Bootstrap do Super-User)
- [ ] Phase 3.1 - CLI de bootstrap funcionando em base vazia (Tasks: T022, T025, T026)
- [ ] Phase 3.2 - Tentativa duplicada retorna OperationOutcome (Tasks: T023, T028)
- [ ] Phase 3.3 - Auditoria append-only registrada (Tasks: T024, T027)
- [ ] Phase 3 - Concluida (Tasks: T022, T023, T024, T025, T026, T027, T028, T029)

### Phase 4 - US2 (Criacao de Organization e Admin)
- [ ] Phase 4.1 - Endpoint e CLI `create-tenant-admin` funcionando (Tasks: T030, T033, T034, T038)
- [ ] Phase 4.2 - CNES validado por formato e unicidade (Tasks: T031, T036)
- [ ] Phase 4.3 - Criacao transacional de `Organization` + primeiro admin confirmada (Tasks: T032, T035, T037, T039)
- [ ] Phase 4 - Concluida (Tasks: T030, T031, T032, T033, T034, T035, T036, T037, T038, T039)

### Phase 5 - US3 (Registro de Clinica)
- [ ] Phase 5.1 - Registro publico implementado (Tasks: T040, T044, T045)
- [ ] Phase 5.2 - Validacoes RNDS e feedback visual testados (Tasks: T041, T042, T046, T047)
- [ ] Phase 5.3 - Conflito de CNES retorna OperationOutcome amigavel (Tasks: T043, T047)
- [ ] Phase 5 - Concluida (Tasks: T040, T041, T042, T043, T044, T045, T046, T047)

### Phase 6 - US4 (Login Multi-Perfil)
- [ ] Phase 6.1 - Login single, multiple e no-org testado (Tasks: T048, T050, T053, T054)
- [ ] Phase 6.2 - Selecao de organizacao funcionando (Tasks: T049, T051, T055, T056, T058)
- [ ] Phase 6.3 - Sessao opaca + tenant claim validadas (Tasks: T052, T057)
- [ ] Phase 6.4 - Endpoint de login integrado ao framework central de lockout/rate limiting (Tasks: T059)
- [ ] Phase 6 - Concluida (Tasks: T048, T049, T050, T051, T052, T053, T054, T055, T056, T057, T058, T059)

### Phase 7 - US7 (Feedback Visual e Erros)
- [ ] Phase 7.1 - Parser/render de OperationOutcome no frontend (Tasks: T060, T062)
- [ ] Phase 7.2 - Toast/Alert do Shell integrados (Tasks: T062, T063)
- [ ] Phase 7.3 - Mensagens RNDS traduzidas sem perder `diagnostics` (Tasks: T061, T064)
- [ ] Phase 7 - Concluida (Tasks: T060, T061, T062, T063, T064)

### Phase 8 - US9 (Seguranca e Criptografia)
- [ ] Phase 8.1 - CPF/PII criptografados e senha hash validados (Tasks: T065, T066, T068, T069)
- [ ] Phase 8.2 - Auditoria imutavel para eventos sensiveis (Tasks: T067, T070)
- [ ] Phase 8.3 - Origem de chave externa e rotacao comprovadas (Tasks: T127, T128)
- [ ] Phase 8 - Concluida (Tasks: T065, T066, T067, T068, T069, T070, T127, T128)

### Phase 9 - US5 (Contexto Multi-Tenant no Shell)
- [ ] Phase 9.1 - `App.tsx` injeta providers conforme contrato (Tasks: T071, T075, T076)
- [ ] Phase 9.2 - Guards de rota implementados (Tasks: T078, T079)
- [ ] Phase 9.3 - Header exibe practitioner, organization e location ativos (Tasks: T072, T077)
- [ ] Phase 9.4 - Logout e expiracao limpam contexto corretamente (Tasks: T074, T080)
- [ ] Phase 9 - Concluida (Tasks: T071, T072, T073, T074, T075, T076, T077, T078, T079, T080)

### Phase 10 - US11 (Vinculo de Atuacao Profissional por Unidade)
- [ ] Phase 10.1 - `PractitionerRole` ativo resolvido no backend (Tasks: T081, T083, T085)
- [ ] Phase 10.2 - Endpoint de contexto do usuario funcionando (Tasks: T081, T086)
- [ ] Phase 10.3 - Endpoint de selecao de location ativa funcionando (Tasks: T082, T087)
- [ ] Phase 10.4 - Header reflete location e role ativos (Tasks: T084, T088)
- [ ] Phase 10 - Concluida (Tasks: T081, T082, T083, T084, T085, T086, T087, T088)

### Phase 10b - Cadastro de Usuario Profile 20
- [ ] Phase 10b.1 - Endpoint `POST /api/admin/users` implementado e testado (Tasks: T089, T093)
- [ ] Phase 10b.2 - Servico backend com `iam_user` + `practitioner` + `practitioner_role` (Tasks: T094, T097, T098)
- [ ] Phase 10b.3 - Unicidade de email por tenant com OperationOutcome (Tasks: T091, T095)
- [ ] Phase 10b.4 - RLS isolation validada com integration test (Tasks: T090, T096)
- [ ] Phase 10b - Concluida (Tasks: T089, T090, T091, T092, T093, T094, T095, T096, T097, T098)

### Phase 11 - US6 (RBAC)
- [ ] Phase 11.1 - Endpoints e servicos RBAC funcionando (Tasks: T099, T102, T103)
- [ ] Phase 11.2 - Enforcement de permissao testado (Tasks: T100, T105)
- [ ] Phase 11.3 - Visibilidade frontend por permissao validada (Tasks: T101, T104)
- [ ] Phase 11 - Concluida (Tasks: T099, T100, T101, T102, T103, T104, T105)

### Phase 12 - US8 (Integracao CLI/API)
- [ ] Phase 12.1 - Comandos CLI login, select-organization, create-tenant-admin e logout funcionando (Tasks: T106, T107, T108, T109, T110)
- [ ] Phase 12.2 - JSON output consistente e deterministico (Tasks: T111)
- [ ] Phase 12.3 - Quotas e codigos de saida deterministas validados na CLI (Tasks: T112, T131)
- [ ] Phase 12 - Concluida (Tasks: T106, T107, T108, T109, T110, T111, T112, T131)

### Phase 13 - US10 (Padrao Atomico + Shell)
- [ ] Phase 13.1 - Moleculas e organismos de Login/Registro implementados (Tasks: T113, T116, T117)
- [ ] Phase 13.2 - Integracao com `MainTemplate` validada (Tasks: T118)
- [ ] Phase 13.3 - Testes e2e e a11y executados (Tasks: T114, T115)
- [ ] Phase 13 - Concluida (Tasks: T113, T114, T115, T116, T117, T118)

### Phase 14 - Polish
- [ ] Phase 14.1 - Quickstart atualizado com evidencias manuais (Tasks: T119, T126)
- [ ] Phase 14.2 - Contratos OpenAPI/CLI atualizados (Tasks: T121)
- [ ] Phase 14.3 - Data model final atualizado (Tasks: T122)
- [ ] Phase 14.4 - Fallback strategy validado (Tasks: T123)
- [ ] Phase 14.5 - Cobertura RBAC, auditoria e tenant desativado validada (Tasks: T124, T131)
- [ ] Phase 14.6 - Performance backend e frontend comprovadas (Tasks: T129, T130)
- [ ] Phase 14.7 - Checklist de consistencia executado (Tasks: T120, T125)
- [ ] Phase 14.8 - Validacao end-to-end final concluida (Tasks: T126)
- [ ] Phase 14 - Concluida (Tasks: T119, T120, T121, T122, T123, T124, T125, T126, T129, T130, T131)

## Naming DoD

- [ ] Naming N001 - DB em snake_case (Tasks: T021, T122)
- [ ] Naming N002 - API/DTO em camelCase (Tasks: T021, T121, T122)
- [ ] Naming N003 - Semantica FHIR preservada nos contratos (Tasks: T014, T021, T121, T122)
- [ ] Naming N004 - Sem termos legados proibidos (`fullName`, `is_active`) (Tasks: T021, T122)
- [ ] Naming N005 - Tabelas de mapeamento DB -> API -> FHIR atualizadas no data-model (Tasks: T021, T122)

## Fechamento da Feature

- [ ] Close C001 - Todas as fases marcadas como concluidas (Tasks: T120)
- [ ] Close C002 - Sem pendencias criticas abertas (Tasks: T124, T125)
- [ ] Close C003 - Pronta para inicio/continuidade da implementacao conforme tasks (Tasks: T125, T126)
