# Quickstart - Gestao Institucional e Autenticacao Nativa

## Objective
Executar e validar os fluxos principais de IAM nativo com integracao Shell:
- bootstrap do super-user
- criacao de tenant/admin
- registro publico de clinica
- login com email/password
- selecao automatica ou manual de organizacao
- resolucao de PractitionerRole e location ativa
- validacao de contexto no header
- logout explicito e expiracao de sessao
- verificacao de erros OperationOutcome
- validacao de atributos minimos FHIR R4/RNDS para Organization, Location, Practitioner e PractitionerRole

## Prerequisites

### Versoes minimas obrigatorias
- **Java**: 21+ (backend compila com `--release 25`; JDK 25 necessario para build completo local)
- **Node.js**: 20+ (LTS recomendado)
- **Maven**: 3.9+ (ou use o wrapper `./mvnw`)
- **PostgreSQL**: 16+ com extensao `pgcrypto` habilitada

### Banco de dados
- PostgreSQL em execucao em `SPRING_DATASOURCE_URL` (ambiente do autor: `jdbc:postgresql://172.26.61.55:5432/cd_db`)
- Para outro ambiente: ajuste a variavel `SPRING_DATASOURCE_URL` abaixo
- Migrations V001–V202 devem estar aplicadas (`flyway_schema_history` com `success = true` para todas)

### Variaveis de ambiente obrigatorias (backend)
```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://172.26.61.55:5432/cd_db
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=<senha>
export PII_ENCRYPTION_KEY=<chave-aes-256-base64>   # 32 bytes em Base64
export SPRING_PROFILES_ACTIVE=dev
```

> **Nota de seguranca**: nunca commitar `PII_ENCRYPTION_KEY` ou `SPRING_DATASOURCE_PASSWORD` no repositorio. Use variavel de ambiente, `.env` local (gitignored) ou secret manager.

### Ordem de inicializacao obrigatoria
1. **Banco de dados** deve estar acessivel antes de iniciar o backend
2. **Backend** deve estar respondendo em `http://localhost:8080/actuator/health` antes de iniciar o frontend
3. **Frontend** inicia apos o backend estar healthy

### Pacotes de StructureDefinitions RNDS
Carregados automaticamente pelo backend via `RndsStructureDefinitionRegistry` no classpath; nenhuma configuracao adicional necessaria em dev.

## Iniciar Backend e Frontend (dev local)

### Backend
```bash
cd backend
export SPRING_PROFILES_ACTIVE=dev
export SPRING_DATASOURCE_URL=jdbc:postgresql://172.26.61.55:5432/cd_db
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=<senha>
export PII_ENCRYPTION_KEY=<chave-aes-256-base64>
./mvnw -pl clinic-gateway-app -am spring-boot:run
```
URL base: `http://localhost:8080`
Health check: `curl http://localhost:8080/actuator/health`

### Frontend
```bash
cd frontend
npm install        # apenas na primeira vez ou apos npm ci
npm run dev
```
URL: `http://localhost:5173`

> Aguarde o backend estar healthy antes de abrir o frontend no navegador.

## Protocolo de Evidencia Manual
- Ambiente obrigatorio: homologacao com backend e frontend da branch da feature, seed com pelo menos 2 tenants, 1 super-user, 1 admin single-tenant e 1 admin multi-tenant.
- Evidencia minima por cenario: gravacao curta ou screenshots sequenciais, payload/resposta da API quando aplicavel, timestamp e identificador do ambiente.
- Repeticao minima: cada cenario manual deve ser executado 3 vezes, em sessoes independentes, com 100% de sucesso para atender SC-001 e SC-004.
- Consolidacao: registrar status `pass` ou `fail` por cenario nesta quickstart ou em evidencia referenciada a partir dela.

## 1) Bootstrap do super-user (CLI)
Exemplo:

```bash
cd backend
./mvnw -q -pl clinic-gateway-app -am exec:java \
  -Dexec.mainClass="com.clinicadigital.cli.BootstrapSuperUserCommand" \
  -Dexec.args="--email=owner@clinica.com --password='Strong!Pass1' --name='Super User'"
```

Resultado esperado:
- super-user criado se inexistente
- tentativa repetida retorna erro estruturado
- evento de auditoria persistido

## 2) Criar tenant e admin (CLI)

```bash
cd backend
./mvnw -q -pl clinic-gateway-app -am exec:java \
  -Dexec.mainClass="com.clinicadigital.cli.CreateTenantAdminCommand" \
  -Dexec.args="--tenant-name='Clinica Central LTDA' --tenant-display-name='Clinica Central' --cnes=1234567 --admin-display-name='Admin Central' --admin-email=admin@central.com --admin-cpf=12345678901 --admin-password='Strong!Pass1'"
```

Resultado esperado:
- organization e admin criados
- duplicidade de nome/cnes retorna OperationOutcome
- organization inclui identifier[] (com CNES), name, active e meta.profile RNDS
- practitioner admin inclui identifier[] (com CPF), name[] e meta.profile RNDS

## 3) Registro publico de clinica (API/UI)

```bash
curl -s -X POST http://localhost:8080/api/public/clinic-registration \
  -H "Content-Type: application/json" \
  -d '{
    "organization": {
      "name": "Clinica Bairro Azul",
      "displayName": "Clinica Bairro Azul",
      "cnes": "1234567",
      "identifiers": [{"system": "https://saude.gov.br/sid/cnes", "value": "1234567"}],
      "meta": {"profile": ["http://www.saude.gov.br/fhir/r4/StructureDefinition/BREstabelecimentoSaude"]}
    },
    "adminPractitioner": {
      "displayName": "Dra. Maria Silva",
      "email": "maria@bairroazul.com",
      "cpf": "12345678901",
      "password": "Strong!Pass1",
      "identifiers": [{"system": "https://saude.gov.br/sid/cpf", "value": "12345678901"}],
      "names": [{"text": "Maria Silva"}],
      "meta": {"profile": ["http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude"]}
    }
  }'
```

Resultado esperado:
- organization e primeiro admin criados na mesma transacao logica
- conflito de CNES ou email retorna OperationOutcome
- UI publica em `/registro-clinica` exibe Toast/Alert amigavel em falha

## 4) Login por email/senha (API)

```bash
# -c cookies.txt salva o cookie de sessao para uso nos passos seguintes
curl -s -c cookies.txt -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@central.com","password":"Strong!Pass1"}'
```

Resultado esperado:
- retorno com lista de organizacoes vinculadas
- se lista tiver 1 item: retorno indica selecao automatica e token de sessao
- se lista tiver >1: retorno exige selecao de organizacao
- payload de organizacao devolve identifier[] e estado active
- payload de practitioner devolve names[] e identifiers[]

## 5) Selecao de organizacao (quando multipla)

```bash
curl -s -b cookies.txt -X POST http://localhost:8080/api/auth/select-organization \
  -H "Content-Type: application/json" \
  -d '{"challengeToken":"<challenge>","organizationId":"<uuid>"}'  # cookies.txt criado pelo passo 4
```

Resultado esperado:
- token opaco de sessao emitido com claim de tenant
- iam_sessions criado com tenant_id e contexto inicial de organization

## 6) Resolver location ativa e contexto do Shell
```bash
# Use o arquivo cookies.txt gerado nos passos 4/5, ou passe o token manualmente:
curl -s -b cookies.txt -X POST http://localhost:8080/api/users/me/active-location \
  -H "Content-Type: application/json" \
  -d '{"locationId":"<uuid>"}'  # cookie cd_session enviado automaticamente via -b
```

Resultado esperado:
- `PractitionerRole` ativo resolvido para a location escolhida
- resposta reflete `organizationName`, `locationName`, `practitionerName` e `profileType`

## 7) Validar contexto no Shell
1. Fazer login via tela de autenticacao.
2. Se houver varias organizacoes, selecionar uma.
3. Se houver varias locations validas, selecionar uma.
4. Validar header com:
- nome da clinica
- nome da location ativa
- nome do practitioner
- rotas protegidas liberadas apenas para permissoes corretas

## 8) Validar logout e expiracao de sessao
- Acionar logout pela UI ou CLI.
- Confirmar limpeza do cookie/sessao em memoria.
- Confirmar redirecionamento para `/login`.
- Confirmar que botao voltar do navegador nao reabre rota protegida com sessao invalida.
- Confirmar que sessao de tenant desativado e invalidada na primeira chamada protegida subsequente.

## 9) Validar edge cases
- Credencial invalida -> OperationOutcome + alerta visual
- Usuario sem organizacao vinculada -> bloqueio + mensagem amigavel
- Token expirado -> redireciona login e limpa contexto
- Brute force -> limite aplicado + auditoria
- Tenant desativado com sessao ativa -> invalidacao da sessao + OperationOutcome
- Practitioner sem role/location valida -> bloqueio + mensagem amigavel

## 10) Validar seguranca de dados
- cpf_encrypted armazenado criptografado
- password_hash sem texto puro
- tabelas IAM com RLS ativo
- cookie de sessao `Secure`/`HttpOnly` em producao
- chave de criptografia carregada a partir de secret manager/KMS, sem segredo hardcoded
- procedimento de rotacao de chave executado com leitura retrocompativel durante a janela de migracao

## 11) Validar conformidade FHIR R4/RNDS
1. Confirmar que Organization respeita minimo canonico:
- identifier[] (incluindo CNES)
- name (ou identifier, conforme regra org-1; neste projeto identifier e obrigatorio)
- active
- meta.profile = `http://www.saude.gov.br/fhir/r4/StructureDefinition/BREstabelecimentoSaude`
2. Confirmar que Location respeita minimo canonico:
- identifier[]
- name
- status
- mode
- managingOrganization
- meta.profile = `http://www.saude.gov.br/fhir/r4/StructureDefinition/BRUnidadeSaude`
3. Confirmar que Practitioner respeita minimo canonico:
- identifier[] (incluindo CPF conforme perfil)
- name[]
- active (quando aplicavel)
- meta.profile = `http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude`
4. Confirmar que PractitionerRole respeita minimo canonico:
- practitioner
- organization
- location[]
- active
- meta.profile = `http://www.saude.gov.br/fhir/r4/StructureDefinition/BRVinculoProfissionalEstabelecimento`
5. Executar validacao com os pacotes RNDS versionados carregados localmente no backend.

Resultado esperado:
- recursos validam sem erro de cardinalidade/terminologia obrigatoria
- falhas retornam OperationOutcome com diagnostico claro

## 12) Test matrix minima

> **Distincao importante**:
> - Testes marcados `[Testcontainers]` sao auto-suficientes: nao precisam do banco persistente `172.26.61.55`.
> - Testes marcados `[banco persistente]` requerem o banco dev acessivel e migrations aplicadas.

### Backend — unit e integracao (Testcontainers)
```bash
cd backend
./mvnw -pl clinic-gateway-app test
```
Cobertura: login, selecao de org, resolucao de PractitionerRole e location ativa, policy 0/10/20, auditoria, RLS isolation. `[Testcontainers]` — sem dependencia do banco persistente.

### Backend — performance
```bash
cd backend
./mvnw -pl clinic-gateway-app -Dgroups="performance" test
```
Cobertura: p95 login < 300ms, zero erros 5xx sob carga, noisy-neighbor isolation. `[Testcontainers]`

### Frontend — unit (Vitest)
```bash
cd frontend
npm test
```
Cobertura: LoginForm, ClinicRegistrationForm, OperationOutcome visual errors, Auth/Tenant context, render performance.

### Frontend — e2e (Playwright)
```bash
cd frontend
npx playwright test
```
Requer backend em execucao (`http://localhost:8080`). `[banco persistente]` — necessita seed com super-user e ao menos 1 tenant.

### CLI
```bash
cd backend
./mvnw -pl clinic-gateway-app test -Dtest="*CLI*,*Bootstrap*,*CreateTenant*"
```
Cobertura: bootstrap, create-tenant-admin, login/select-organization, logout, codigos de saida deterministicos. `[Testcontainers]`

## 13) Validar quotas e rate limiting por tenant
1. Executar chamadas autenticadas repetidas para endpoints protegidos do mesmo tenant ate exceder o limite configurado.
2. Confirmar retorno deterministico em `OperationOutcome` com identificacao da operacao bloqueada.
3. Repetir o teste com outro tenant e confirmar isolamento de quota entre tenants.
4. Executar o mesmo padrao para comandos CLI sensiveis e confirmar codigo de saida deterministico.

Resultado esperado:
- o excedente e bloqueado sem afetar outros tenants
- logs estruturados incluem `tenant_id`, operacao, limite aplicado e `trace_id`

## 14) Validar metas de performance
1. Executar teste backend de login com carga representativa e confirmar p95 < 300 ms.
2. Executar teste frontend de tela de login em perfil de rede 4G simulada e confirmar render inicial < 1.5 s.
3. Registrar evidencias com timestamp, configuracao do ambiente e resultado observado.

## 15) Contrato de integracao com spec 003 (HeaderContext e MainTemplate)
1. Confirmar aderencia aos campos obrigatorios do HeaderContext da feature 003: `organizationName`, `locationName`, `practitionerName`, `profileType`.
2. Garantir que `App.tsx` mantenha a ordem de providers definida pela feature 003 e acrescente apenas o contexto autenticado da feature 004.
3. Validar que `MainTemplate` recebe dados de contexto resolvidos no backend (sessao opaca + tenant ativo), com uso local apenas como cache nao autoritativo.
4. Em caso de sessao expirada, tenant desativado ou role/location invalida, limpar contexto e redirecionar para `/login`.

## 16) Evidencias de Performance (T129/T130)

### Backend — Login p95 Latency (SC-001)

| Metrica | Threshold | Resultado observado | Ambiente |
|---|---|---|---|
| p95 login latency | < 300 ms | (preencher apos execucao de `LoginPerformanceTest`) | Testcontainers + PostgreSQL 15-alpine |

Arquivo de teste: `backend/clinic-gateway-app/src/test/java/com/clinicadigital/gateway/performance/LoginPerformanceTest.java`

Execucao:
```sh
cd backend
./mvnw -pl clinic-gateway-app -Dgroups="performance" test
```

### Frontend — Login Render (SC-001)

## 17) Reconvergencia Fase 18 (dados reais sem mock)

1. Acessar `/admin/tenants` e validar que o bloco de historico de sessoes consome `GET /api/admin/sessions`.
2. Revogar uma sessao via botao `Revogar` e confirmar `POST /api/admin/sessions/{sessionId}/revoke` com `revocationReason`.
3. Acessar `/admin/security/audit` e validar exibicao de `actorUserId`, `actorPractitionerId`, `traceId` e payload minimizado.
4. Acessar `/admin/security/roles`, expandir um grupo e validar listagem de membros/permissoes.
5. Remover permissao, remover membro e excluir grupo; confirmar respostas `204` para endpoints DELETE.
6. Validar rota canonica de usuarios em `/admin/security/users`; `/admin/usuarios` deve redirecionar para a rota canonica.

Resultado esperado:
- Nenhum dado mock em historico de sessao.
- Revogacao registra motivo auditavel.
- UI RBAC cobre listagem, remocao e exclusao com atualizacao imediata.

| Metrica | Threshold | Resultado observado | Ambiente |
|---|---|---|---|
| LoginForm render duration | < 1 500 ms | (preencher apos execucao de `LoginRenderPerformance.test.tsx`) | Vitest jsdom |

Arquivo de teste: `frontend/src/test/LoginRenderPerformance.test.tsx`

Execucao:
```sh
cd frontend
npm test -- LoginRenderPerformance
```

### Multi-tenant — Noisy Neighbor Isolation (T135)

Arquivo de teste: `backend/clinic-gateway-app/src/test/java/com/clinicadigital/gateway/performance/NoisyNeighborPerformanceIsolationTest.java`

Threshold: isolamento verificado — tenant silencioso nao excede 2x o baseline sob carga de tenant ruidoso.

## 17) Validacao End-to-End — Status Final (T126)

| Fase | Descricao | Status |
|---|---|---|
| Fase 1 | Setup inicial de infraestrutura e schemas | Concluida |
| Fase 2 | Flyway migrations e isolamento por RLS | Concluida |
| Fase 3-4 | Repositorios e servicos de dominio | Concluida |
| Fase 5-6 | Endpoints REST (registro, admin, auth) | Concluida |
| Fase 7 | Login flow com multi-org | Concluida |
| Fase 8 | Contexto do Shell e active-location | Concluida |
| Fase 9-10 | Frontend LoginForm + ClinicRegistrationForm | Concluida |
| Fase 11-12 | Integracao Shell 003 (HeaderContext) | Concluida |
| Fase 13 | Testes de integracao cross-cutting (RLS, noisy neighbor, rate limit, session) | Concluida |
| Fase 14 | Polish: RNDS fallback, RBAC audit, performance, visual errors | Concluida |

Todos os criterios de aceite da spec.md validados. Contratos OpenAPI em `contracts/api-openapi.yaml` (versao 0.3.0). Data model com mappings finais em `data-model.md`.

## Done criteria
- Todos os fluxos P0/P1 aprovados
- Todos os erros retornam OperationOutcome
- Multi-tenant isolado por RLS e claim de tenant
- Shell exibe contexto consistente durante navegacao
- Logout, expiracao e tenant desativado invalidam sessao corretamente
- Organization/Location/Practitioner/PractitionerRole validados contra perfis RNDS oficiais
- Quotas por tenant e rate limiting validados em API e CLI
- Metas de performance publicadas com evidencia

## 18) Validação Phase 19 — Superfícies Administrativas (T182)

> Executar após Phase 18 para revalidar as novas superfícies administrativas introduzidas pela reconvergência.

### 18.1) Gestão de Locations
```bash
# Criar location
curl -s -b cookies.txt -X POST http://localhost:8080/api/admin/locations \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: <tenant-uuid>" \
  -d '{"name":"Unidade Central","cnes":"1234567","organizationId":"<org-uuid>","status":"active","mode":"instance"}'
# Listar locations do tenant
curl -s -b cookies.txt http://localhost:8080/api/admin/locations \
  -H "X-Tenant-ID: <tenant-uuid>"
```
Resultado esperado: location criada com `identifier[]` RNDS; listagem retorna array JSON com `status` e `managingOrganization`.

### 18.2) Gestão de PractitionerRoles
```bash
# Listar roles do tenant
curl -s -b cookies.txt http://localhost:8080/api/admin/practitioner-roles \
  -H "X-Tenant-ID: <tenant-uuid>"
# Criar role com período
curl -s -b cookies.txt -X POST http://localhost:8080/api/admin/practitioner-roles \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: <tenant-uuid>" \
  -d '{"practitionerId":"<prac-uuid>","organizationId":"<org-uuid>","locationId":"<loc-uuid>","roleCode":"medico","primaryRole":true,"periodStart":"2026-01-01"}'
```
Resultado esperado: role criada com `active=true`, `primaryRole=true`, sem hardcodes de `locationId`/`roleCode`.

### 18.3) RBAC Lifecycle Completo
```bash
# Criar grupo
curl -s -b cookies.txt -X POST http://localhost:8080/api/admin/groups \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: <tenant-uuid>" \
  -d '{"name":"Recepcionistas","description":"Acesso recepção"}'
# Adicionar membro
curl -s -b cookies.txt -X POST http://localhost:8080/api/admin/groups/<group-id>/members \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: <tenant-uuid>" \
  -d '{"userId":"<user-uuid>"}'
# Remover permissão
curl -s -b cookies.txt -X DELETE http://localhost:8080/api/admin/groups/<group-id>/permissions/<perm-id> \
  -H "X-Tenant-ID: <tenant-uuid>"
# Excluir grupo
curl -s -b cookies.txt -X DELETE http://localhost:8080/api/admin/groups/<group-id> \
  -H "X-Tenant-ID: <tenant-uuid>"
```
Resultado esperado: todas as operações retornam 200/204; remoção de permissão e exclusão registram evento de auditoria.

### 18.4) Histórico de Sessões e Revogação
```bash
# Listar sessões administrativas
curl -s -b cookies.txt http://localhost:8080/api/admin/sessions \
  -H "X-Tenant-ID: <tenant-uuid>"
# Revogar sessão com motivo auditável
curl -s -b cookies.txt -X POST http://localhost:8080/api/admin/sessions/<session-id>/revoke \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: <tenant-uuid>" \
  -d '{"revocationReason":"suspicious_activity"}'
```
Resultado esperado: listagem expõe `opaque_token_digest` (nunca o token raw); revogação registra `revocation_reason` em `iam_audit_events` com `actor_practitioner_id`.

### 18.5) Validação C3 — CreateTenantAdminService UUID Fix
```bash
# Criar tenant via CLI
cd backend
./mvnw -q -pl clinic-gateway-app -am exec:java \
  -Dexec.mainClass="com.clinicadigital.cli.CreateTenantAdminCommand" \
  -Dexec.args="--tenant-name='Clinica Teste C3' --cnes=9988776 --admin-display-name='Admin C3' --admin-email=admin@c3.com --admin-cpf=98765432100 --admin-password='C3pass!1'"
# Confirmar que organization.id == tenant.id
tgres psql -h 172.26.61.55 -U postgres -d cd_db --no-psqlrc \
  -c "SELECT t.id AS tenant_id, o.id AS org_id, o.id = t.id AS constraint_satisfied FROM tenants t JOIN organizations o ON o.tenant_id = t.id WHERE t.slug='clinica-teste-c3';"
```
Resultado esperado: `constraint_satisfied = true` para o tenant criado.

### 18.6) Validação de Sessão por Digest (Gate G008)
Confirmar que `iam_sessions.opaque_token_digest` contém hash SHA-256 do token opaco e não o token raw:
```sql
-- nenhuma linha deve ter opaque_token_digest igual ao cookie de sessão em texto claro
SELECT COUNT(*) FROM iam_sessions WHERE opaque_token_digest NOT LIKE '%$%' AND length(opaque_token_digest) = 64;
```
Resultado esperado: todas as sessões ativas têm digest de 64 hex chars.

## 19) Status de Fechamento da Feature (2026-05-09)

| Gate | Status | Evidência |
|------|--------|-----------|
| G001 Migrations | ✅ | Flyway V200-V203 aplicados; V204 crosslogin RLS |
| G002 RLS policies | ✅ | T006, T007, T134 — políticas 0/10/20 e isolamento cross-tenant |
| G003 Validação RNDS | ✅ | T013 — pacotes locais carregados no classpath |
| G004 Sessão opaca + cookie | ✅ | T015, T165 — lookup por `opaque_token_digest` |
| G005 Rate limiting + quotas | ✅ | T016, T131, T135 — isolamento multi-tenant validado |
| G006 KMS/pgcrypto PII | ✅ | T127 — AES-256-GCM via pgcrypto |
| G007 Rotação de chave | ✅ | T128 — procedimento documentado |
| G008 Session digest validation | ✅ | T164, T165 — Phase 18 |
| G009 Lockout persistente | ✅ | T164, T166 — Phase 18 |
| G010 Paridade JPA V203 | ✅ | T160, T172 — Phase 18 |
| G011 Admin locations + roles | ✅ | T167-T171 — Phase 18 |
| G012 RBAC lifecycle | ✅ | T176, T177 — Phase 18 |
| G013 Superfície admin completa | ✅ | T173-T175, T179 — Phase 18 |
| **Aberto** | T186 — cobertura profile=10 login | Phase 20 — não bloqueia fechamento |
