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

Exemplo de policy:

```sql
ALTER TABLE iam_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE iam_sessions FORCE ROW LEVEL SECURITY;

CREATE POLICY iam_sessions_tenant_isolation ON iam_sessions
FOR ALL
USING (tenant_id = current_setting('app.tenant_id')::uuid)
WITH CHECK (tenant_id = current_setting('app.tenant_id')::uuid);
```

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