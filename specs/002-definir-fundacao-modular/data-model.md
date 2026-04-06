# Data Model: Tenant e IAM Baseline

## Escopo

Estrutura inicial para fundacao de tenancy e IAM interno no PostgreSQL 15, com RLS obrigatoria em tabelas tenant-scoped.

## Entidade: tenants

### Campos
- id (uuid, pk)
- slug (text, unique)
- legal_name (text)
- status (text: active, suspended, blocked)
- plan_tier (text)
- quota_requests_per_minute (int)
- quota_concurrency (int)
- quota_storage_mb (int)
- created_at (timestamptz)
- updated_at (timestamptz)

### Regras
- slug unico global.
- status define bloqueio operacional do tenant.
- quotas sao aplicadas por adaptadores HTTP/CLI/eventos.

## Entidade: iam_users

### Campos
- id (uuid, pk)
- tenant_id (uuid, fk -> tenants.id)
- username (text)
- email (text)
- password_hash (text)
- password_algo (text: bcrypt)
- is_active (boolean)
- last_login_at (timestamptz null)
- created_at (timestamptz)
- updated_at (timestamptz)

### Regras
- unique (tenant_id, username)
- unique (tenant_id, email)
- password_hash nunca retorna em APIs/CLI.

## Entidade: iam_roles

### Campos
- id (uuid, pk)
- tenant_id (uuid, fk -> tenants.id)
- role_key (text)
- description (text)
- created_at (timestamptz)

### Regras
- unique (tenant_id, role_key)

## Entidade: iam_permissions

### Campos
- id (uuid, pk)
- permission_key (text, unique)
- description (text)

## Entidade: iam_role_permissions

### Campos
- role_id (uuid, fk -> iam_roles.id)
- permission_id (uuid, fk -> iam_permissions.id)

### Regras
- pk composta (role_id, permission_id)

## Entidade: iam_user_roles

### Campos
- user_id (uuid, fk -> iam_users.id)
- role_id (uuid, fk -> iam_roles.id)
- assigned_at (timestamptz)

### Regras
- pk composta (user_id, role_id)

## Entidade: iam_sessions

### Campos
- id (uuid, pk)  # token opaco
- tenant_id (uuid, fk -> tenants.id)
- user_id (uuid, fk -> iam_users.id)
- issued_at (timestamptz)
- expires_at (timestamptz)
- revoked_at (timestamptz null)
- client_ip (inet)
- user_agent (text)
- trace_id (text)

### Regras
- sessao valida: revoked_at is null e expires_at > now().
- unique (tenant_id, id) para lookup rapido por tenant.

## Entidade: iam_audit_events

### Campos
- id (bigserial, pk)
- tenant_id (uuid, fk -> tenants.id)
- actor_user_id (uuid null)
- event_type (text)
- outcome (text)
- trace_id (text)
- metadata_json (jsonb)
- created_at (timestamptz)

### Regras
- tabela append-only.
- sem update/delete para service accounts.

## RLS baseline

RLS obrigatoria para: iam_users, iam_roles, iam_user_roles, iam_sessions, iam_audit_events e quaisquer tabelas clinicas tenant-scoped.

A policy MUST usar USING e WITH CHECK baseados em `current_setting('app.tenant_id')::uuid`. O valor MUST ser injetado antes de qualquer query via `SET LOCAL app.tenant_id = :tenantId` no boundary transacional (ver FR-016). FORCE RLS garante que mesmo roles com BYPASSRLS implicito nao escapem.

### Especificacao por tabela

| Tabela            | Tipo de policy | Role alvo      | Observacao                                   |
|-------------------|---------------|----------------|----------------------------------------------|
| iam_users         | FOR ALL       | app_user       | filtro em tenant_id                          |
| iam_roles         | FOR ALL       | app_user       | filtro em tenant_id                          |
| iam_user_roles    | FOR ALL       | app_user       | filtro via JOIN em iam_roles.tenant_id (ver nota) |
| iam_sessions      | FOR ALL       | app_user       | filtro em tenant_id                          |
| iam_audit_events  | INSERT only   | app_user       | append-only: apenas INSERT permitido; UPDATE/DELETE proibidos por policy + REVOKE |

### iam_role_permissions — excecao documentada de RLS

A tabela `iam_role_permissions` nao possui coluna `tenant_id` propria (primary key composta: role_id, permission_id). O isolamento e herdado indiretamente atraves das policies de `iam_roles` (que restringem os role_id visiveis por tenant). Portanto:
- RLS de linha propria nao e aplicavel a `iam_role_permissions`.
- A policy em `iam_roles` MUST bloquear acesso a roles de outros tenants, garantindo que associacoes de permissao de outros tenants sejam invisiveis por transitividade.
- Esta excecao e intencional e documentada. Nao requer ENABLE RLS na tabela `iam_role_permissions`.

### iam_user_roles — policy via JOIN

A tabela `iam_user_roles` nao possui `tenant_id` diretamente. A RLS MUST usar subselect/JOIN na tabela `iam_roles` para filtrar:

```sql
ALTER TABLE iam_user_roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE iam_user_roles FORCE ROW LEVEL SECURITY;

CREATE POLICY iam_user_roles_tenant_isolation ON iam_user_roles
FOR ALL
USING (
  role_id IN (
    SELECT id FROM iam_roles
    WHERE tenant_id = current_setting('app.tenant_id')::uuid
  )
)
WITH CHECK (
  role_id IN (
    SELECT id FROM iam_roles
    WHERE tenant_id = current_setting('app.tenant_id')::uuid
  )
);
```

### iam_audit_events — append-only enforcement

```sql
ALTER TABLE iam_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE iam_audit_events FORCE ROW LEVEL SECURITY;

CREATE POLICY iam_audit_events_tenant_isolation ON iam_audit_events
FOR ALL
USING (tenant_id = current_setting('app.tenant_id')::uuid)
WITH CHECK (tenant_id = current_setting('app.tenant_id')::uuid);

-- Append-only: revogar UPDATE e DELETE do role de aplicacao
REVOKE UPDATE, DELETE ON iam_audit_events FROM app_user;
```

### Exemplo de policy padrao (iam_sessions — referencia)

```sql
ALTER TABLE iam_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE iam_sessions FORCE ROW LEVEL SECURITY;

CREATE POLICY iam_sessions_tenant_isolation ON iam_sessions
FOR ALL
USING (tenant_id = current_setting('app.tenant_id')::uuid)
WITH CHECK (tenant_id = current_setting('app.tenant_id')::uuid);
```

### Comportamento quando app.tenant_id nao esta definido

Quando `app.tenant_id` nao foi definido via SET LOCAL antes da query, `current_setting('app.tenant_id')` lancat erro se o segundo argumento for omitido, ou retorna `''` com `current_setting('app.tenant_id', true)`. A policy MUST usar a forma sem fallback (lancar erro) para garantir fail-closed (FR-002a, FR-016a). Nenhuma linha deve ser retornada silenciosamente para contexto de tenant nao inicializado.

## Indices iniciais

- iam_users (tenant_id, username)
- iam_users (tenant_id, email)
- iam_sessions (tenant_id, user_id, expires_at) where revoked_at is null
- iam_audit_events (tenant_id, created_at)

## Transicoes de estado

### Tenant
- active -> suspended (administrativo)
- suspended -> active (reativacao)
- active|suspended -> blocked (excesso de cota/seguranca)

### Sessao
- issued -> active
- active -> revoked (logout, troca de credencial, incidente)
- active -> expired (tempo)

## Classificacao de dados (Art. VI)

- PHI/PII sensivel: email, identificadores de acesso, eventos de autenticacao.
- Credenciais: password_hash (nunca plaintext), dados de sessao.
- Todos os eventos de seguranca devem ser auditaveis com tenant_id e trace_id.