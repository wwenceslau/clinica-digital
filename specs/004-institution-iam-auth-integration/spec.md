# Feature Specification: Gestão Institucional e Autenticação Nativa

**Feature Branch**: `004-institution-iam-auth-integration`
**Created**: 2026-04-14
**Status**: Draft
**Input**: User description: "Gerar a especificação completa da Fase (Gestão Institucional e Autenticação Nativa), garantindo a integração funcional com o Shell construído, a verificação visual dos fluxos de Login/Registro e a conformidade estrita com o Princípio XXII (In-App IAM)."

## Clarifications

### Session 2026-04-16
- Q: Devemos criptografar senha com AES-256-GCM ou usar hash forte para senha? → A: Senha com hash forte (Argon2id/bcrypt), mantendo AES-256-GCM apenas para CPF/PII.
- Q: Devemos fixar URIs explícitas de `meta.profile` RNDS nesta spec? → A: Sim, URIs explícitas e versionadas na spec.
- Q: Qual política de acesso global do super-user devemos adotar no RLS? → A: Policy RLS explícita e controlada para profile 0, sem bypass fora do banco.

## User Scenarios & Testing *(mandatory)*

### US1 – Bootstrap do Super-User via CLI (P0)
Como operador administrativo, desejo inicializar o sistema criando o super-user (profile 0) via CLI, caso não exista nenhum cadastrado, garantindo acesso global e auditável para configuração inicial.
**Acceptance Scenarios:**
1. **Given** não existe super-user cadastrado, **When** executo o comando CLI de bootstrap, **Then** o super-user é criado, evento auditado e acesso liberado.
2. **Given** já existe super-user, **When** executo o comando, **Then** operação é bloqueada e mensagem de erro retornada.

---

### US2 – Criação de Organização e Admin (P1)
Como super-user, desejo criar uma nova organização (tenant) e seu usuário administrador (profile 10) via painel administrativo, garantindo unicidade de nome e CNES, e segregação multi-tenant.
**Acceptance Scenarios:**
1. **Given** estou autenticado como super-user, **When** cadastro nova organização e admin, **Then** ambos são criados, sem duplicidade de nome/CNES.
2. **Given** nome ou CNES já existem, **When** tento cadastrar, **Then** recebo erro amigável (OperationOutcome).

---

### US3 – Registro de Clínica via Formulário (P1)
Como representante de clínica, desejo registrar minha organização via formulário público, informando dados obrigatórios validados pela RNDS, para criar um novo tenant e acessar o sistema.
**Acceptance Scenarios:**
1. **Given** acesso o formulário de registro, **When** preencho dados válidos e submeto, **Then** clínica e admin são criados, confirmação exibida.
2. **Given** dados inválidos (ex: CNES fora do padrão), **When** submeto, **Then** recebo feedback visual amigável (OperationOutcome).

---

### US4 – Login Multi-Perfil com Seleção de Organização (P1)
Como usuário (admin ou comum), desejo autenticar informando email e senha, para que o sistema filtre todas as organizações às quais tenho acesso. Caso eu tenha vínculo com mais de uma organização, devo selecionar qual acessar; se houver apenas uma, o sistema deve seguir direto para o dashboard institucional protegido pelo Shell.
**Acceptance Scenarios:**
1. **Given** estou na tela de login, **When** informo email e senha válidos, **Then** o sistema filtra as organizações vinculadas ao usuário.
2. **Given** tenho vínculo com múltiplas organizações, **When** autentico, **Then** devo selecionar a organização antes de acessar o sistema.
3. **Given** tenho vínculo com apenas uma organização, **When** autentico, **Then** sou direcionado automaticamente para o dashboard dessa organização, sem necessidade de seleção.
4. **Given** credenciais inválidas, **When** tento autenticar, **Then** recebo feedback visual amigável (OperationOutcome).

---

### US5 – Persistência e Contexto Multi-Tenant (P2)
Como usuário autenticado, desejo que o contexto do tenant (clínica) e do practitioner (usuário) estejam sempre visíveis no header do Shell, com persistência de sessão stateless e proteção de rotas.
**Acceptance Scenarios:**
1. **Given** autentiquei com sucesso, **When** navego entre páginas, **Then** header exibe nome da clínica e usuário, sem perda de contexto.
2. **Given** sessão expira ou token é inválido, **When** tento acessar rotas protegidas, **Then** sou redirecionado para login e contexto é limpo.

---

### US6 – Gestão de Permissões e Grupos (P2)
Como admin, desejo criar grupos/permissões customizadas e atribuir usuários a esses grupos, visualizando permissões disponíveis por página/funcionalidade, para garantir RBAC granular.
**Acceptance Scenarios:**
1. **Given** sou admin autenticado, **When** crio grupo/permissão e atribuo usuários, **Then** permissões são aplicadas e refletidas nas funcionalidades.
2. **Given** usuário sem permissão tenta acessar funcionalidade, **When** acessa, **Then** acesso é negado e feedback visual exibido.

---

### US7 – Feedback Visual e Erros (P1)
Como usuário, desejo receber feedback visual amigável (Snackbar/Modal) para erros de autenticação, validação e edge cases, sempre no padrão FHIR OperationOutcome.
**Acceptance Scenarios:**
1. **Given** erro de login, registro ou permissão, **When** ocorre, **Then** mensagem amigável é exibida conforme OperationOutcome.
2. **Given** CNES já cadastrado, perda de conexão ou tentativa de brute force, **When** ocorre, **Then** feedback visual e logs/auditoria são gerados.

---

### US8 – Integração CLI e API (P2)
Como operador/admin, desejo executar fluxos de criação de tenant, admin e login via CLI, recebendo retorno JSON estruturado, para automação e integração com outros sistemas.
**Acceptance Scenarios:**
1. **Given** executo comando CLI para criar tenant/login, **When** operação é válida, **Then** retorno JSON estruturado é exibido.
2. **Given** operação inválida, **When** executo, **Then** erro estruturado é retornado.

---

### US9 – Segurança e Criptografia (P1)
Como gestor de dados, desejo que CPF (PII) seja criptografado com AES-256-GCM via pgcrypto e que senhas sejam armazenadas com hash forte (Argon2id/bcrypt), além de auditoria de operações sensíveis, para garantir segurança e conformidade.
**Acceptance Scenarios:**
1. **Given** cadastro ou autenticação, **When** dados são persistidos, **Then** CPF está criptografado e senha está protegida por hash forte não reversível.
2. **Given** operação sensível (bootstrap, login, criação de tenant), **When** ocorre, **Then** evento é auditado conforme política.

---

### US10 – Padrão Atômico e Integração Shell (P2)
Como desenvolvedor frontend, desejo que formulários de Login e Registro sejam implementados como moléculas/organismos (MUI 7 + Tailwind), e que o App.tsx injete os Context Providers de Autenticação e Tenant, protegendo rotas via MainTemplate, garantindo integração total com o Shell.
**Acceptance Scenarios:**
1. **Given** acesso o sistema, **When** navego entre rotas protegidas, **Then** Context Providers garantem persistência e proteção.
2. **Given** formulário de login/registro, **When** renderizado, **Then** segue padrão atômico e integra visualmente ao Shell.

---

### US11 – Vínculo de Atuação Profissional por Unidade (P2)
Como gestor institucional, desejo que o profissional autenticado atue em uma unidade física específica da organização por meio de vínculo explícito de papel assistencial, para garantir autorização correta por local de atendimento.
**Acceptance Scenarios:**
1. **Given** um practitioner vinculado a múltiplas locations, **When** seleciona a location ativa no contexto de sessão, **Then** somente permissões e dados daquela atuação são aplicados no shell e nas APIs protegidas.
2. **Given** um practitioner sem vínculo ativo com a location solicitada, **When** tenta acessar funcionalidade dependente de unidade, **Then** o acesso é negado com OperationOutcome.

---

### Edge Cases
- CNES já cadastrado: exibir erro amigável e bloquear registro.
- Perda de conexão durante login/registro: exibir mensagem de erro e permitir nova tentativa.
- Token expirado durante navegação: redirecionar para login e limpar contexto.
- Tentativa de brute force: bloquear temporariamente e auditar evento.

## Requirements *(mandatory)*

### Functional Requirements

#### Lista Consolidada de Requisitos Funcionais

- **FR-001**: O sistema DEVE permitir login de super-user (profile 0) sem tenant_id, com acesso global para criar/gerenciar organizações e usuários admins. Só pode existir um único super-user na base (profile 0).
- **FR-002**: O sistema DEVE permitir bootstrap seguro do primeiro super-user apenas via CLI administrativa, caso não exista nenhum cadastrado. O evento deve ser auditado conforme política de segurança.
- **FR-003**: O super-user (profile 0) DEVE poder criar organizações (tenants) e usuários administradores (profile 10). Não pode haver organizações com o mesmo nome.
	- A criação de Organization DEVE exigir CNES válido e único.
	- O registro de Organization DEVE criar, na mesma transação lógica, o primeiro usuário administrador vinculado (profile 10).
- **FR-004**: O sistema DEVE permitir login de administradores (profile 10) e usuários (profile 20) informando email e senha. Após autenticação, o sistema DEVE filtrar todas as organizações às quais o usuário tem acesso e:
	- Se houver múltiplas organizações, DEVE exibir uma tela de seleção para o usuário escolher qual organização acessar.
	- Se houver apenas uma organização, DEVE direcionar automaticamente para o dashboard dessa organização, sem necessidade de seleção.
	- O campo de login NÃO deve mais aceitar nome ou CNES diretamente, apenas email.
- **FR-005**: O sistema DEVE permitir granularidade de permissões via profiles: 0 (super-user), 10 (admin), 20 (user), reservando o range para futuros perfis.
- **FR-006**: O sistema DEVE permitir que administradores (profile 10) criem grupos/permissões customizadas e atribuam usuários (profile 20) a esses grupos, exibindo todas as permissões disponíveis para cada página/funcionalidade.
- **FR-007**: O contexto do tenant_id DEVE ser capturado após a seleção da organização e persistido em sessão stateless (token opaco) para profiles 10 e 20. A validação do tenant_id deve ser feita via claims do token em todas as rotas protegidas (ver spec/002 e spec/003).
	- O token opaco DEVE ser persistido prioritariamente em cookie seguro (`Secure`, `HttpOnly`, `SameSite=Lax`) no ambiente web.
	- Em ambientes de desenvolvimento sem cookie seguro disponível, a persistência DEVE ocorrer apenas em memória de sessão (sem `localStorage`).
	- O frontend DEVE encaminhar o contexto de tenant para o middleware de contexto multi-tenant antes de chamadas protegidas.
- **FR-008**: Após login, o practitioner logado e a location ativa DEVEM ser exibidos no header do Shell.
- **FR-009**: Erros de autenticação/validação DEVEM ser retornados como FHIR OperationOutcome e renderizados como alertas visuais (Toast/Alert MUI 7).
	- O payload DEVE conter `issue[].severity`, `issue[].code`, `issue[].details.text` e, quando aplicável, `issue[].diagnostics`.
- **FR-010**: O fluxo de criação de tenant e login DEVE ser executável via CLI, com retorno JSON estruturado.
- **FR-011**: CPF (e demais PII sensíveis) DEVE ser criptografado com AES-256-GCM via pgcrypto, e senhas DEVEM ser armazenadas com hash forte não reversível (Argon2id ou bcrypt).
- **FR-012**: O App.tsx (ou entrypoint principal) DEVE injetar os Context Providers de Autenticação e Tenant, protegendo rotas internas via MainTemplate.
	- O entrypoint DEVE carregar o PractitionerRole ativo após login e injetar no contexto consumido pelo Shell.
	- Rotas não autenticadas DEVEM fazer fallback para a página de login.
- **FR-013**: Os formulários de Login e Registro DEVEM ser implementados como moléculas/organismos, usando MUI 7 e Tailwind, seguindo o padrão atômico.
- **FR-014**: O sistema DEVE aplicar PostgreSQL RLS nas tabelas de IAM.
	- O isolamento por tenant DEVE ser aplicado explicitamente nas tabelas de Organization, Practitioner, Location, PractitionerRole, iam_users e iam_sessions.
	- O profile 0 DEVE usar política de acesso global controlada por RLS (ex.: `USING true` condicionada a role/contexto de sessão), sem bypass na camada de aplicação.
	- A policy de super-user DEVE ser separada das policies de perfis 10/20 e auditável em logs de migração e segurança.
- **FR-015**: O sistema NÃO DEVE usar validação HAPI genérica para Organization/Practitioner, apenas StructureDefinitions RNDS.
- **FR-016**: O sistema DEVE implementar proteção contra brute force/rate limiting no endpoint de login e logging/auditoria de acessos sensíveis, conforme política de segurança (ver spec/002 e spec/003).
- **FR-017**: As entidades de Organization e Practitioner DEVEM manter, no mínimo, os atributos canônicos compatíveis com FHIR R4 e com os perfis RNDS aplicáveis: `identifier` (com identificadores nacionais, ex. CNES/CPF conforme perfil), `name`, `active`, `telecom`, `address`, além de `gender` e `birthDate` para Practitioner quando exigido pelo perfil. O `meta.profile` de cada recurso DEVE referenciar o StructureDefinition RNDS correspondente, e a validação DEVE ser feita contra esses perfis oficiais.
- **FR-018**: O recurso FHIR `Location` DEVE ser obrigatório no domínio institucional, com vínculo de pertencimento a `Organization` e uso para contexto operacional de unidade ativa.
- **FR-019**: O recurso FHIR `PractitionerRole` DEVE ser obrigatório como vínculo pivô entre `Practitioner`, `Organization` e `Location`, definindo onde e como o profissional pode atuar.
- **FR-020**: A validação RNDS DEVE ser feita explicitamente contra os StructureDefinitions oficiais da RNDS Brasil para `Organization`, `Practitioner`, `Location` e `PractitionerRole`; validação FHIR genérica isolada não atende ao requisito.
	- `Organization.meta.profile` fixo: `http://www.saude.gov.br/fhir/r4/StructureDefinition/BREstabelecimentoSaude`.
	- `Practitioner.meta.profile` fixo: `http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude`.
	- `Location.meta.profile` fixo: `http://www.saude.gov.br/fhir/r4/StructureDefinition/BRUnidadeSaude`.
	- `PractitionerRole.meta.profile` fixo: `http://www.saude.gov.br/fhir/r4/StructureDefinition/BRVinculoProfissionalEstabelecimento`.
	- Mudança de versão/URI RNDS DEVE gerar revisão controlada desta spec e dos contratos associados.
- **FR-021**: O fluxo de autenticação DEVE operar exclusivamente com IAM interno (sem IdPs externos), usando as tabelas `iam_users` e `iam_sessions` como fontes canônicas de identidade e sessão.
- **FR-022**: A validação de CNES DEVE ser obrigatória na criação de tenant e incluir, no mínimo, verificação estrutural de formato e unicidade na base.
- **FR-023**: O fluxo de login DEVE aplicar limiar configurável de tentativas falhas (padrão: 5 tentativas em 15 minutos) com bloqueio temporário (padrão: 15 minutos), registro de auditoria e resposta OperationOutcome.
- **FR-024**: O fluxo de logout explícito DEVE invalidar o contexto de sessão no cliente (remoção de cookie seguro ou limpeza da sessão em memória) e redirecionar para login.
- **FR-025**: O sistema DEVE aplicar rate limiting e quotas por `tenant_id` em todos os endpoints autenticados de API e operações CLI sensíveis (ex.: autenticação, criação administrativa, alteração de contexto), com resposta determinística em `OperationOutcome` quando limite for excedido.
## Constituição e Conformidade

Esta especificação foi revisada à luz da constituição do projeto (v1.6.0) e dos princípios:
- Princípio XXII (In-App IAM): Garantido fluxo 100% interno, segregação de perfis, e persistência de contexto multi-tenant.
- Art. 0 e XXII: O contexto do tenant_id é capturado e refletido no Shell, exceto para super-user.
- Art. XVIII: Todos os erros de autenticação/validação são retornados como OperationOutcome e exibidos de forma amigável.
- Art. VI: CPF/PII protegidos com AES-256-GCM via pgcrypto e senhas protegidas com hash forte não reversível.
- Art. VII: Validação de recursos clínico-administrativos contra StructureDefinitions oficiais da RNDS Brasil para Organization, Practitioner, Location e PractitionerRole.
- Art. I e II: Testes E2E, CLI e checklist manual garantem verificabilidade e rastreabilidade.
- Todos os fluxos de criação, login e gestão respeitam a separação de privilégios e a governança multi-tenant.

Se houver atualização na constituição, esta spec deve ser revisada para garantir aderência total.


### Key Entities
- **Organization (Clínica)**: `identifier[]` (incluindo CNES), `name`, `active`, `type[]`, `telecom[]`, `address[]`, `partOf`, `endpoint[]`, `meta.profile = http://www.saude.gov.br/fhir/r4/StructureDefinition/BREstabelecimentoSaude`, além de campos de autenticação/gestão interna.
- **Location (Unidade Física)**: `identifier[]`, `name`, `status`, `mode`, `telecom[]`, `address`, `managingOrganization` (Organization), `meta.profile = http://www.saude.gov.br/fhir/r4/StructureDefinition/BRUnidadeSaude`.
- **Practitioner (Usuário)**: `identifier[]` (incluindo CPF conforme perfil), `name[]`, `active`, `telecom[]`, `address[]`, `gender`, `birthDate`, `qualification[]`, `communication[]`, `meta.profile = http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude`, senha segura e vínculo com Organization/profile (0, 10, 20).
- **PractitionerRole (Vínculo de atuação)**: `practitioner`, `organization`, `location[]`, `code[]`, `specialty[]`, `active`, `period`, `meta.profile = http://www.saude.gov.br/fhir/r4/StructureDefinition/BRVinculoProfissionalEstabelecimento`.
- **organizations (persistência)**: inclui campos canônicos FHIR (`fhir_*`) e campos de aplicação `display_name` e `account_active`.
- **locations (persistência)**: inclui chaves de tenant, referência para `organizations`, dados canônicos FHIR de unidade e status de ativação.
- **practitioners (persistência)**: inclui campos canônicos FHIR (`fhir_*`) e campos de aplicação `display_name`, `email`, `profile`, `account_active`, credenciais seguras.
- **practitioner_roles (persistência)**: inclui tenant_id, organization_id, location_id, practitioner_id, role_code, active, period_start, period_end.
- **iam_users**: Tabela canônica de identidade interna (sem IdP externo), com tenant_id opcional (nulo para profile 0), email único por tenant, `password_hash` (Argon2id/bcrypt), e status de conta.
- **iam_sessions**: Tabela canônica de sessão interna com RLS, campos mínimos: id, iam_user_id, opaque_token_ref, issued_at, expires_at, revoked_at, active.

## Integração Frontend-Backend (Costura Final)

### Entrypoint e Contexto
- O `App.tsx` DEVE inicializar a árvore de providers na ordem: `TenantContextProvider` envolvendo `AuthContextProvider`, ambos consumidos pelo `MainTemplate`.
- Após login e eventual seleção de organização, o frontend DEVE carregar o PractitionerRole ativo e popular no contexto, no mínimo: `tenantId`, `organizationId`, `organizationName`, `locationId`, `locationName`, `practitionerId`, `practitionerName`, `profileType`.
- O Header do Shell DEVE consumir esse contexto para exibir clínica e profissional ativos.

### Rotas públicas e protegidas
- Rotas públicas mínimas: `/login`, `/registro-clinica`.
- Rotas protegidas mínimas: `/dashboard`, `/admin/*`, `/usuarios/*`.
- Qualquer rota não autenticada DEVE redirecionar para `/login`.

### Tratamento de erros no Shell
- Falhas de autenticação e autorização DEVEM ser exibidas no frontend usando componente de feedback visual do Shell (Toast/Alert MUI 7), com conteúdo originado de OperationOutcome.
- A camada de apresentação DEVE traduzir mensagens técnicas de validação RNDS para mensagens amigáveis sem perder rastreabilidade (`issue.code` e `issue.diagnostics`).

## Critérios de Aceite Manuais (Smoke Test)

1. Login bem-sucedido com usuário profile 10 exibe no Header, em até 2 segundos após navegação para dashboard, o nome da clínica selecionada e o nome do profissional logado.
2. Login com usuário vinculado a múltiplas organizações exige seleção explícita de organização antes do acesso ao dashboard.
3. Login com usuário vinculado a uma única organização redireciona automaticamente ao dashboard sem tela intermediária.
4. Erro de credencial inválida retorna OperationOutcome e exibe Toast/Alert visível no Shell sem quebrar layout.
5. Registro de clínica cria Organization com CNES válido e primeiro usuário admin vinculado; tentativa com CNES duplicado é bloqueada com OperationOutcome.
6. Rotas protegidas acessadas sem sessão válida redirecionam para `/login`.
7. Logout limpa a sessão do cliente e impede retorno a rota protegida via botão "voltar" do navegador sem novo login.
8. Seleção de location ativa por PractitionerRole reflete imediatamente no contexto exibido no Header.

## Success Criteria

- **SC-001**: 100% dos fluxos de login e registro descritos em US2, US3 e US4 são executáveis manualmente sem dependência de intervenção técnica.
- **SC-002**: 100% dos erros críticos de autenticação, autorização e validação são apresentados ao usuário final em formato visual padronizado no Shell.
- **SC-003**: 100% dos acessos entre tenants distintos são bloqueados para perfis 10/20 durante validações funcionais e de segurança.
- **SC-004**: 100% dos cenários manuais de smoke test desta spec são reproduzíveis em ambiente de homologação.

## Protocolo de Evidência de Aceite Manual

- Ambiente obrigatório: homologação com backend e frontend atualizados para a branch da feature, com dados seed de pelo menos 2 tenants e 1 usuário profile 10 por tenant.
- Evidência mínima por cenário de smoke: gravação de tela curta (ou sequência de screenshots) + payload/resposta da API (quando aplicável) + timestamp.
- Repetição mínima: cada cenário manual deve ser executado 3 vezes, em sessões independentes, com 100% de sucesso para cumprimento de SC-001 e SC-004.
- Registro obrigatório: consolidar resultado no quickstart da feature com indicação de status por cenário (`pass`/`fail`) e referência de evidências.

## Assumptions
- Usuários possuem conexão estável durante login/registro.
- O sistema opera em ambiente web responsivo; mobile nativo está fora do escopo v1.
- O PostgreSQL já está configurado com pgcrypto e RLS.
- O Shell estrutural já está implementado e disponível para integração.
- O padrão atômico de componentes já está estabelecido no frontend.
- O backend já possui suporte a OperationOutcome FHIR.
- O CLI de administração já existe ou será estendido para suportar os fluxos descritos.
- O sistema não precisa suportar múltiplos practitioners simultâneos por sessão.
- O range de profiles (0, 10, 20) pode ser expandido para novos papéis no futuro.
- A publicação e versionamento dos StructureDefinitions RNDS oficiais estão disponíveis no ambiente de validação.
