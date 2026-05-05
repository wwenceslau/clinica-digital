# Data Model - Gestao Institucional e Autenticacao Nativa

## Overview
Modelo de dados para IAM nativo multi-tenant com segregacao por tenant, RLS obrigatorio, sessao opaca in-app, validacao RNDS por StructureDefinitions versionados e criptografia de PII sensivel.

Referencias canonicas aplicadas nesta modelagem:
- HL7 FHIR R4 Organization: https://hl7.org/fhir/R4/organization.html
- HL7 FHIR R4 Location: https://hl7.org/fhir/R4/location.html
- HL7 FHIR R4 Practitioner: https://hl7.org/fhir/R4/practitioner.html
- HL7 FHIR R4 PractitionerRole: https://hl7.org/fhir/R4/practitionerrole.html
- RNDS Organization profile: http://www.saude.gov.br/fhir/r4/StructureDefinition/BREstabelecimentoSaude
- RNDS Location profile: http://www.saude.gov.br/fhir/r4/StructureDefinition/BRUnidadeSaude
- RNDS Practitioner profile: http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude
- RNDS PractitionerRole profile: http://www.saude.gov.br/fhir/r4/StructureDefinition/BRVinculoProfissionalEstabelecimento

Identifier systems adotados nesta feature:
- CNES: `https://saude.gov.br/sid/cnes`
- CPF: `https://saude.gov.br/sid/cpf`

Naming convention oficial desta feature:
- [naming-convention.md](naming-convention.md)
- Padrao adotado: DB snake_case, API/JSON camelCase, semantica canonica FHIR preservada nas bordas.

## Naming Normalization Applied
- DB: snake_case
- Campos canonicos FHIR persistidos no DB: prefixo `fhir_`
- Estruturas FHIR compostas no DB: sufixo `_json`
- Campos de aplicacao IAM (nao FHIR): sem prefixo `fhir_`
- Chaves estrangeiras: `<entity>_id`
- Termos legados proibidos: `fullName`, `is_active`

## Naming Mapping DB -> API -> FHIR (Base)

| DB (snake_case) | API (camelCase) | FHIR |
|---|---|---|
| organizations.cnes | organization.cnes | Organization.identifier(system=`https://saude.gov.br/sid/cnes`).value |
| organizations.display_name | organization.displayName | Organization.name |
| organizations.account_active | organization.accountActive | Organization.active |
| locations.display_name | location.displayName | Location.name |
| practitioners.display_name | practitioner.displayName | Practitioner.name[0].text |
| practitioners.cpf_encrypted | practitioner.cpfEncrypted | Practitioner.identifier(system=`https://saude.gov.br/sid/cpf`).value |
| iam_users.password_hash | iamUser.passwordHash | N/A (atributo interno IAM) |
| iam_users.account_active | iamUser.accountActive | N/A (atributo interno IAM) |

## Entities

### 1. organizations
Representa o tenant institucional e o recurso FHIR Organization canonico.

| Field | Type | Required | Rules |
|---|---|---|---|
| id | uuid | yes | PK; tambem e o tenant_id de referencia |
| tenant_id | uuid | yes | coluna tenant-raiz; deve espelhar `id` para aderencia constitucional e simplificacao de policies RLS |
| fhir_resource_id | varchar(64) | yes | id logico FHIR |
| fhir_meta_profile | jsonb | yes | deve conter `BREstabelecimentoSaude` |
| fhir_identifier_json | jsonb | yes | array Identifier; deve conter slice CNES com `system=https://saude.gov.br/sid/cnes` |
| fhir_name | varchar(255) | yes | mapeia Organization.name |
| fhir_active | boolean | yes | mapeia Organization.active |
| fhir_type_json | jsonb | no | mapeia Organization.type |
| fhir_alias_json | jsonb | no | mapeia Organization.alias[] |
| fhir_telecom_json | jsonb | no | mapeia Organization.telecom[] |
| fhir_address_json | jsonb | no | mapeia Organization.address[] |
| fhir_part_of_org_id | uuid | no | FK organizations(id); mapeia Organization.partOf |
| fhir_endpoint_refs_json | jsonb | no | mapeia Organization.endpoint[] |
| cnes | varchar(7) | yes | unico; validacao estrutural obrigatoria |
| display_name | varchar(255) | yes | nome de exibicao interno sincronizado com `fhir_name` |
| quota_tier | varchar(32) | yes | default `standard`; usado para overrides de quota/rate limiting por tenant |
| account_active | boolean | yes | default true |
| created_at | timestamptz | yes | default now() |
| updated_at | timestamptz | yes | default now() |

Constraints:
- check(tenant_id = id)
- unique(cnes)
- unique(lower(display_name))
- check(quota_tier in ('standard','premium','enterprise'))
- check(jsonb_array_length(fhir_identifier_json) > 0)
- check(jsonb_array_length(fhir_meta_profile) > 0)
- regra FHIR `org-1`: deve existir `name` ou `identifier` (nesta feature os dois sao mantidos)

### 2. locations
Representa unidades fisicas da organizacao para contexto operacional.

| Field | Type | Required | Rules |
|---|---|---|---|
| id | uuid | yes | PK |
| tenant_id | uuid | yes | FK organizations(id) |
| organization_id | uuid | yes | FK organizations(id); managing organization |
| fhir_resource_id | varchar(64) | yes | id logico FHIR |
| fhir_meta_profile | jsonb | yes | deve conter `BRUnidadeSaude` |
| fhir_identifier_json | jsonb | yes | array Identifier |
| fhir_name | varchar(255) | yes | mapeia Location.name |
| fhir_status | varchar(32) | yes | active|suspended|inactive |
| fhir_mode | varchar(32) | yes | instance|kind |
| fhir_telecom_json | jsonb | no | mapeia Location.telecom[] |
| fhir_address_json | jsonb | no | mapeia Location.address |
| display_name | varchar(255) | yes | alias de exibicao na UI |
| account_active | boolean | yes | default true |
| created_at | timestamptz | yes | default now() |
| updated_at | timestamptz | yes | default now() |

Constraints:
- unique(tenant_id, lower(display_name))
- check(jsonb_array_length(fhir_meta_profile) > 0)
- check(jsonb_array_length(fhir_identifier_json) > 0)
- check(fhir_status in ('active','suspended','inactive'))
- check(fhir_mode in ('instance','kind'))

### 3. practitioners
Representa a identidade clinica/profissional no dominio FHIR. Nao e a fonte canonica de autenticacao.

| Field | Type | Required | Rules |
|---|---|---|---|
| id | uuid | yes | PK |
| tenant_id | uuid | no | null para super-user global; preenchido para perfis tenant-scoped |
| fhir_resource_id | varchar(64) | yes | id logico FHIR |
| fhir_meta_profile | jsonb | yes | deve conter `BRProfissionalSaude` |
| fhir_identifier_json | jsonb | yes | array Identifier sanitizado; pode conter slice CPF mascarada ou tokenizada, nunca o CPF puro em repouso |
| fhir_name_json | jsonb | yes | array HumanName |
| fhir_active | boolean | yes | mapeia Practitioner.active |
| fhir_telecom_json | jsonb | no | mapeia Practitioner.telecom[] |
| fhir_address_json | jsonb | no | mapeia Practitioner.address[] |
| fhir_gender | varchar(16) | no | male|female|other|unknown |
| fhir_birth_date | date | no | mapeia Practitioner.birthDate |
| fhir_qualification_json | jsonb | no | mapeia Practitioner.qualification[] |
| fhir_communication_json | jsonb | no | mapeia Practitioner.communication[] |
| display_name | varchar(255) | yes | helper para exibicao na UI |
| cpf_encrypted | bytea | yes | PII criptografada com pgcrypto |
| encryption_key_version | varchar(32) | yes | versao da chave usada em `cpf_encrypted`; obrigatoria para rotacao sem indisponibilidade |
| account_active | boolean | yes | default true |
| created_at | timestamptz | yes | default now() |
| updated_at | timestamptz | yes | default now() |

Constraints:
- check(jsonb_array_length(fhir_identifier_json) > 0)
- check(jsonb_array_length(fhir_name_json) > 0)
- check(jsonb_array_length(fhir_meta_profile) > 0)
- check(length(encryption_key_version) > 0)

### 4. practitioner_roles
Vinculo pivot entre Practitioner, Organization e Location. Fonte do contexto operacional exibido no Shell.

| Field | Type | Required | Rules |
|---|---|---|---|
| id | uuid | yes | PK |
| tenant_id | uuid | yes | FK organizations(id) |
| organization_id | uuid | yes | FK organizations(id) |
| location_id | uuid | yes | FK locations(id) |
| practitioner_id | uuid | yes | FK practitioners(id) |
| fhir_resource_id | varchar(64) | yes | id logico FHIR |
| fhir_meta_profile | jsonb | yes | deve conter `BRVinculoProfissionalEstabelecimento` |
| fhir_code_json | jsonb | no | mapeia PractitionerRole.code[] |
| fhir_specialty_json | jsonb | no | mapeia PractitionerRole.specialty[] |
| fhir_telecom_json | jsonb | no | mapeia PractitionerRole.telecom[] |
| fhir_available_time_json | jsonb | no | mapeia PractitionerRole.availableTime[] |
| role_code | varchar(64) | yes | papel simplificado de aplicacao |
| active | boolean | yes | default true |
| primary_role | boolean | yes | default false |
| period_start | timestamptz | no | inicio do vinculo |
| period_end | timestamptz | no | fim do vinculo |
| created_at | timestamptz | yes | default now() |
| updated_at | timestamptz | yes | default now() |

Constraints:
- unique(tenant_id, practitioner_id, location_id, role_code)
- no maximo um `primary_role=true` por practitioner dentro do tenant
- check(jsonb_array_length(fhir_meta_profile) > 0)

### 5. iam_users
Tabela canonica de identidade interna. Fonte de autenticacao, status de conta e perfil.

| Field | Type | Required | Rules |
|---|---|---|---|
| id | uuid | yes | PK |
| tenant_id | uuid | no | null para profile 0; obrigatorio para profiles 10/20 |
| practitioner_id | uuid | no | FK practitioners(id); null permitido para bootstrap inicial antes de vinculo completo |
| email | varchar(320) | yes | unico por tenant; comparacao case-insensitive |
| password_hash | text | yes | Argon2id ou bcrypt |
| profile | int | yes | 0, 10 ou 20 |
| account_active | boolean | yes | default true |
| failed_login_count | int | yes | default 0 |
| locked_until | timestamptz | no | bloqueio temporario por brute force |
| last_login_at | timestamptz | no | ultimo login bem-sucedido |
| created_at | timestamptz | yes | default now() |
| updated_at | timestamptz | yes | default now() |

Constraints:
- check(profile in (0,10,20))
- unique(tenant_id, lower(email)) para perfis 10/20
- unique(lower(email)) filtrado para profile 0
- tenant_id obrigatorio para profiles 10/20

### 6. iam_groups
Grupos customizados por tenant.

| Field | Type | Required | Rules |
|---|---|---|---|
| id | uuid | yes | PK |
| tenant_id | uuid | yes | FK organizations(id) |
| name | varchar(120) | yes | unico por tenant |
| description | text | no | opcional |
| created_at | timestamptz | yes | default now() |

Constraints:
- unique(tenant_id, lower(name))

### 7. iam_permissions
Catalogo global de permissoes.

| Field | Type | Required | Rules |
|---|---|---|---|
| id | uuid | yes | PK |
| code | varchar(120) | yes | unico global |
| resource | varchar(120) | yes | ex: appointments, billing |
| action | varchar(60) | yes | ex: read, write |
| description | text | no | opcional |

Constraints:
- unique(code)

### 8. iam_group_permissions
Vinculo N:N entre grupos e permissoes.

| Field | Type | Required | Rules |
|---|---|---|---|
| group_id | uuid | yes | FK iam_groups(id) |
| permission_id | uuid | yes | FK iam_permissions(id) |

Constraints:
- PK(group_id, permission_id)

### 9. iam_user_groups
Vinculo N:N entre usuarios internos e grupos por tenant.

| Field | Type | Required | Rules |
|---|---|---|---|
| iam_user_id | uuid | yes | FK iam_users(id) |
| group_id | uuid | yes | FK iam_groups(id) |
| assigned_at | timestamptz | yes | default now() |
| assigned_by_user_id | uuid | no | FK iam_users(id) |

Constraints:
- PK(iam_user_id, group_id)

### 10. iam_auth_challenges
Estado transitorio do login multi-organizacao antes da emissao da sessao final.

| Field | Type | Required | Rules |
|---|---|---|---|
| id | uuid | yes | PK |
| iam_user_id | uuid | yes | FK iam_users(id) |
| challenge_token_digest | text | yes | hash do challenge token |
| organization_options_json | jsonb | yes | organizacoes acessiveis no momento do login |
| expires_at | timestamptz | yes | challenge curto |
| created_at | timestamptz | yes | default now() |

Constraints:
- check(jsonb_array_length(organization_options_json) > 0)

### 11. iam_sessions
Sessao stateless com token opaco, contexto tenant e role ativa.

| Field | Type | Required | Rules |
|---|---|---|---|
| id | uuid | yes | PK |
| iam_user_id | uuid | yes | FK iam_users(id) |
| tenant_id | uuid | no | null para super-user; obrigatorio para profiles 10/20 |
| organization_id | uuid | no | redundante-controlado para consultas de contexto |
| active_practitioner_role_id | uuid | no | FK practitioner_roles(id); obrigatorio para fluxos clinicos tenant-scoped |
| opaque_token_digest | text | yes | hash do token opaco |
| issued_at | timestamptz | yes | default now() |
| expires_at | timestamptz | yes | TTL inicial definido em planejamento |
| revoked_at | timestamptz | no | revogacao |
| revocation_reason | varchar(64) | no | logout, expired, tenant_disabled, admin_revoked |
| active | boolean | yes | default true |
| created_at | timestamptz | yes | default now() |

Constraints:
- `tenant_id` obrigatorio quando `iam_users.profile` associado for 10 ou 20
- `organization_id` obrigatorio quando `tenant_id` estiver preenchido
- `active_practitioner_role_id` obrigatorio quando `iam_users.profile` associado for 10 ou 20 e o contexto clinico estiver resolvido

### 12. iam_audit_events
Auditoria append-only de operacoes sensiveis.

| Field | Type | Required | Rules |
|---|---|---|---|
| id | uuid | yes | PK |
| tenant_id | uuid | no | null para eventos globais |
| actor_user_id | uuid | no | FK iam_users(id) |
| actor_practitioner_id | uuid | no | FK practitioners(id) |
| event_type | varchar(120) | yes | ex: BOOTSTRAP_SUPERUSER, LOGIN_FAILED |
| payload_json | jsonb | yes | dados minimizados |
| trace_id | varchar(64) | no | correlacao observabilidade |
| created_at | timestamptz | yes | default now() |

Constraints:
- tabela append-only com bloqueio de update/delete via trigger

## Relationships
- organizations 1:N locations
- organizations 1:1 tenant root via `tenant_id = id`
- organizations 1:N practitioners (tenant-scoped)
- organizations 1:N practitioner_roles
- organizations 1:N iam_users (profiles 10/20)
- organizations 1:N iam_groups
- practitioners 1:N practitioner_roles
- locations 1:N practitioner_roles
- iam_users 1:N iam_sessions
- iam_users N:N iam_groups via iam_user_groups
- iam_groups N:N iam_permissions via iam_group_permissions
- iam_users 1:N iam_auth_challenges
- iam_users / practitioners 1:N iam_audit_events

## RLS Strategy
- Tabelas com RLS obrigatorio: organizations, practitioners, locations, practitioner_roles, iam_users, iam_groups, iam_group_permissions, iam_user_groups, iam_sessions, iam_audit_events.
- Politica base para perfil tenant-scoped: `tenant_id = current_setting('app.tenant_id')::uuid`.
- Para tabelas sem `tenant_id` direto, a policy deve derivar por join seguro com entidade tenant-scoped pai.
- Profile 0 usa policy separada de super-user no banco, controlada por contexto de sessao/role tecnica auditada; nao ha bypass fora do RLS.
- Toda migration de tabela tenant-scoped deve incluir policy RLS no mesmo change set.

## Security Classification
- PII sensivel: `cpf_encrypted`; valores sensiveis em `fhir_identifier_json` devem permanecer mascarados, tokenizados ou derivados sob demanda.
- Credencial: `password_hash` (hash unidirecional; nunca armazenar senha em texto puro).
- Sessao: `opaque_token_digest` e `challenge_token_digest` apenas em formato digerido no banco.
- Logs: CPF/CNES devem ser mascarados ou hasheados em observabilidade e auditoria.
- Chaves: `encryption_key_version` e metadados de rotacao nao sao segredos, mas sao obrigatorios para operacao segura de recriptografia.

## RNDS Validation Strategy
- Validacao deve usar pacotes de StructureDefinitions RNDS versionados e carregados localmente no backend.
- Nao e permitido depender apenas de validacao FHIR generica.
- Toda falha de validacao deve ser convertida em `OperationOutcome` amigavel, preservando `issue.code` e `issue.diagnostics` para rastreabilidade.

## FHIR/RNDS Minimum Mapping Rules
- Organization deve manter `identifier[]`, `name`, `active` e `meta.profile` com `BREstabelecimentoSaude`.
- Location deve manter `identifier[]`, `name`, `status`, `mode`, `managingOrganization` e `meta.profile` com `BRUnidadeSaude`.
- Practitioner deve manter `identifier[]`, `name[]`, `active` e `meta.profile` com `BRProfissionalSaude`; o CPF canonico deve ser reidratado a partir de `cpf_encrypted` apenas na borda autorizada.
- PractitionerRole deve manter `practitioner`, `organization`, `location[]`, `active` e `meta.profile` com `BRVinculoProfissionalEstabelecimento`.
- `email` de autenticacao e atributo de aplicacao; quando aplicavel, deve ser refletido em `Practitioner.telecom` com `system=email`.

## Mapping Table - Organization (DB -> API -> FHIR)

| db_column | api_field | fhir_path | required | source_rule |
|---|---|---|---|---|
| fhir_resource_id | id | Organization.id | yes | FHIR R4 Organization |
| fhir_meta_profile | meta.profile | Organization.meta.profile | yes | RNDS `BREstabelecimentoSaude` |
| fhir_identifier_json | identifier | Organization.identifier | yes | CNES obrigatorio |
| fhir_name | name | Organization.name | yes | FHIR org-1 |
| fhir_active | active | Organization.active | yes | FHIR R4 Organization |
| fhir_type_json | type | Organization.type | no | FHIR R4 Organization |
| fhir_telecom_json | telecom | Organization.telecom | no | FHIR R4 Organization |
| fhir_address_json | address | Organization.address | no | FHIR R4 Organization |
| cnes | cnes | Organization.identifier.value (slice CNES) | yes | `system=https://saude.gov.br/sid/cnes` |
| display_name | displayName | N/A (application) | yes | Shell/Header contract |
| quota_tier | quotaTier | N/A (application) | yes | overrides de quota por tenant |
| account_active | accountActive | N/A (application) | yes | IAM domain |

## Mapping Table - Location (DB -> API -> FHIR)

| db_column | api_field | fhir_path | required | source_rule |
|---|---|---|---|---|
| fhir_resource_id | id | Location.id | yes | FHIR R4 Location |
| fhir_meta_profile | meta.profile | Location.meta.profile | yes | RNDS `BRUnidadeSaude` |
| fhir_identifier_json | identifier | Location.identifier | yes | FHIR/RNDS |
| fhir_name | name | Location.name | yes | FHIR R4 Location |
| fhir_status | status | Location.status | yes | FHIR R4 Location |
| fhir_mode | mode | Location.mode | yes | FHIR R4 Location |
| organization_id | organizationId | Location.managingOrganization.reference | yes | Organization reference |
| display_name | displayName | N/A (application) | yes | Shell/Header contract |
| account_active | accountActive | N/A (application) | yes | IAM domain |

## Mapping Table - Practitioner (DB -> API -> FHIR)

| db_column | api_field | fhir_path | required | source_rule |
|---|---|---|---|---|
| fhir_resource_id | id | Practitioner.id | yes | FHIR R4 Practitioner |
| fhir_meta_profile | meta.profile | Practitioner.meta.profile | yes | RNDS `BRProfissionalSaude` |
| fhir_identifier_json | identifier | Practitioner.identifier | yes | slice CPF mascarada em repouso; reidratacao autorizada na borda |
| fhir_name_json | name | Practitioner.name | yes | FHIR R4 Practitioner |
| fhir_active | active | Practitioner.active | yes | FHIR R4 Practitioner |
| fhir_telecom_json | telecom | Practitioner.telecom | no | FHIR R4 Practitioner |
| fhir_address_json | address | Practitioner.address | no | FHIR R4 Practitioner |
| fhir_gender | gender | Practitioner.gender | no | FHIR R4 Practitioner |
| fhir_birth_date | birthDate | Practitioner.birthDate | no | FHIR R4 Practitioner |
| display_name | displayName | N/A (application) | yes | Shell/Header contract |
| encryption_key_version | encryptionKeyVersion | N/A (application/security) | yes | suporte a rotacao de chave |
| account_active | accountActive | N/A (application) | yes | IAM domain |

## Mapping Table - PractitionerRole (DB -> API -> FHIR)

| db_column | api_field | fhir_path | required | source_rule |
|---|---|---|---|---|
| fhir_resource_id | id | PractitionerRole.id | yes | FHIR R4 PractitionerRole |
| fhir_meta_profile | meta.profile | PractitionerRole.meta.profile | yes | RNDS `BRVinculoProfissionalEstabelecimento` |
| practitioner_id | practitionerId | PractitionerRole.practitioner.reference | yes | Practitioner reference |
| organization_id | organizationId | PractitionerRole.organization.reference | yes | Organization reference |
| location_id | locationId | PractitionerRole.location[0].reference | yes | Location reference |
| fhir_code_json | code | PractitionerRole.code | no | FHIR R4 PractitionerRole |
| fhir_specialty_json | specialty | PractitionerRole.specialty | no | FHIR R4 PractitionerRole |
| active | active | PractitionerRole.active | yes | FHIR R4 PractitionerRole |
| period_start | period.start | PractitionerRole.period.start | no | FHIR R4 PractitionerRole |
| period_end | period.end | PractitionerRole.period.end | no | FHIR R4 PractitionerRole |
| role_code | roleCode | N/A (application) | yes | IAM context |

## Mapping Table - IAM User / Session (DB -> API -> App)

| db_column | api_field | app_usage | required | source_rule |
|---|---|---|---|---|
| iam_users.id | userId | identidade interna | yes | IAM in-app |
| tenant_id | tenantId | contexto multi-tenant | conditional | null para profile 0 |
| organization_id | organizationId | contexto institucional | conditional | obrigatorio para sessao tenant-scoped |
| practitioner_id | practitionerId | correlacao com FHIR Practitioner | no | IAM -> FHIR bridge |
| email | email | login | yes | autenticacao por email |
| profile | profileType | autorizacao de alto nivel | yes | 0/10/20 |
| opaque_token_digest | N/A | validacao de sessao | yes | nunca exposto |
| active_practitioner_role_id | practitionerRoleId | contexto do Shell | conditional | obrigatorio em contexto tenant-scoped |
| expires_at | expiresAt | expiracao de sessao | yes | sem silent refresh |

## State Transitions

### Auth flow
1. `iam_user` autenticado por `email/password`.
2. Rate limiting, bloqueio temporal e `account_active` validados.
3. Organizacoes e `PractitionerRole` ativos do usuario sao carregados.
4. Se 0 organizacoes: erro `OperationOutcome` (`forbidden` / `not-found`).
5. Se houver exatamente 1 organizacao e exatamente 1 `PractitionerRole` elegivel: sessao final emitida diretamente.
6. Se houver multiplas organizacoes ou multiplas locations/roles elegiveis dentro da mesma organizacao: `iam_auth_challenge` criado para selecao explicita de contexto.
7. Selecao de organizacao e, quando necessario, de location resolve `active_practitioner_role_id`.
8. `iam_session` criada com `tenant_id`, `organization_id` e `active_practitioner_role_id`.

### Session flow
- ACTIVE -> EXPIRED (`expires_at` atingido)
- ACTIVE -> REVOKED (`revoked_at` por logout/admin)
- ACTIVE -> INVALIDATED (`revocation_reason = tenant_disabled`)

## Mapping Table - iam_sessions (DB -> API)

| db_column | api_field | required | notes |
|---|---|---|---|
| id | sessionId | yes | opaque token digest; nunca exposto em plano |
| iam_user_id | userId | yes | identidade do usuario autenticado |
| tenant_id | tenantId | yes | contexto multi-tenant; null para profile 0 |
| organization_id | organizationId | conditional | obrigatorio em sessao tenant-scoped |
| active_practitioner_role_id | practitionerRoleId | conditional | resolve contexto do Shell |
| expires_at | expiresAt | yes | sem silent refresh; sessao expira ao atingir |
| revoked_at | revokedAt | no | null = sessao ainda ativa |
| revocation_reason | revocationReason | no | `logout`, `admin`, `tenant_disabled` |
| ip_address | N/A | N/A | nunca exposto pela API; auditoria interna |
| user_agent | N/A | N/A | nunca exposto pela API; auditoria interna |

## Mapping Table - iam_audit_events (DB -> API)

| db_column | api_field | required | notes |
|---|---|---|---|
| id | eventId | yes | UUID do evento |
| tenant_id | tenantId | conditional | null para eventos de bootstrap |
| iam_user_id | userId | conditional | null para tentativas pre-autenticacao |
| event_type | eventType | yes | `LOGIN_SUCCESS`, `LOGIN_FAILED`, `LOGOUT`, `SESSION_REVOKED`, `PERMISSION_DENIED`, `TENANT_DISABLED` |
| trace_id | traceId | yes | rastreabilidade distribuida; obrigatorio para todos os eventos |
| ip_address | N/A | N/A | nunca exposto pela API |
| details | details | no | JSON livre com contexto adicional do evento |
| created_at | occurredAt | yes | timestamp imutavel do evento |
| **Invariante** | — | — | tabela append-only; DELETE e UPDATE proibidos via trigger de banco |

## Mapping Table - iam_groups (DB -> API)

| db_column | api_field | required | notes |
|---|---|---|---|
| id | groupId | yes | UUID do grupo |
| tenant_id | tenantId | yes | isolamento por tenant |
| name | name | yes | unico dentro do tenant (`UNIQUE (tenant_id, name)`) |
| description | description | no | descricao livre |
| created_at | createdAt | yes | timestamp de criacao |
| updated_at | updatedAt | yes | timestamp da ultima modificacao |
| **Relacao** | permissions | no | lista de `PermissionSummary` derivada de `iam_group_permissions` |

## Mapping Table - iam_permissions / iam_group_permissions (DB -> API)

| db_column | api_field | required | notes |
|---|---|---|---|
| iam_permissions.id | permissionId | yes | UUID da permissao |
| iam_permissions.permission_code | permissionCode | yes | ex: `ADMIN_GROUPS_READ`, `ADMIN_USERS_WRITE` |
| iam_permissions.description | description | no | descricao legivel |
| iam_group_permissions.group_id | groupId | yes | associacao grupo-permissao |
| **Campo derivado** | grantedVia | yes | `group` ou `direct`; calculado pela consulta de permissoes efetivas |

## Final Constraints Summary

| Regra | Tabela(s) | Mecanismo de Enforcement |
|---|---|---|
| Isolamento multi-tenant (RLS) | Todas | `ALTER TABLE ... ENABLE ROW LEVEL SECURITY` + policies por `tenant_id` |
| Sessoes append-only | iam_sessions | Trigger `prevent_session_update` bloqueia UPDATE/DELETE de sessoes ativas |
| Audit append-only | iam_audit_events | Trigger `prevent_audit_modification` bloqueia UPDATE/DELETE |
| Rate limiting de login | iam_users | Coluna `failed_login_count` + `locked_until`; verificacao na camada de aplicacao |
| CNES unico por tenant | organizations | `UNIQUE (tenant_id, cnes)` |
| Email unico por tenant | iam_users | `UNIQUE (tenant_id, lower(email))` |
| Nome de grupo unico | iam_groups | `UNIQUE (tenant_id, lower(name))` |
| Permissao unica por grupo | iam_group_permissions | `UNIQUE (group_id, permission_id)` |
| CPF criptografado | practitioners | `cpf_encrypted BYTEA NOT NULL`; chave de criptografia versionada |
| Profile RNDS validado | organizations, practitioners, locations, practitioner_roles | Validacao na borda de entrada da API (antes de qualquer persistencia) |
| Sessao requer active_practitioner_role | iam_sessions | Verificacao na emissao de sessao; `active_practitioner_role_id NOT NULL` para tenant-scoped |

## Indexing Recommendations
- idx_orgs_cnes on cnes
- idx_orgs_display_name_lower on lower(display_name)
- idx_locations_tenant_display_name on (tenant_id, lower(display_name))
- idx_practitioner_roles_tenant_practitioner on (tenant_id, practitioner_id, active)
- idx_iam_users_email_tenant on (tenant_id, lower(email))
- idx_sessions_user_expires on (iam_user_id, expires_at)
- idx_sessions_tenant_active on (tenant_id, active, expires_at)
- idx_audit_events_tenant_created on (tenant_id, created_at)
- idx_auth_challenges_expires on expires_at
- idx_groups_tenant_name on (tenant_id, lower(name))
- idx_group_permissions_group on group_id
