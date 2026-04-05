# Contracts: CLI por modulo da fundacao

## Convencoes gerais

- Toda CLI deve suportar modo estruturado com --json.
- Erros devem retornar codigo nao-zero e payload padronizado.
- Campos obrigatorios de observabilidade no output: tenant_id, trace_id, operation, outcome.

## 1. Modulo tenant-core-cli

### Comando
`tenant create`

### Entrada
- --slug (obrigatorio)
- --legal-name (obrigatorio)
- --plan-tier (obrigatorio)
- --json (opcional, recomendado)

### Saida de sucesso (JSON)
```json
{
  "tenant_id": "uuid",
  "slug": "acme",
  "status": "active",
  "trace_id": "string",
  "operation": "tenant.create",
  "outcome": "success"
}
```

## 2. Modulo iam-core-cli

### Comando
`auth login`

### Entrada
- --tenant (obrigatorio)
- --username (obrigatorio)
- --password (obrigatorio)
- --json (opcional, recomendado)

### Saida de sucesso (JSON)
```json
{
  "session_id": "uuid",
  "tenant_id": "uuid",
  "user_id": "uuid",
  "expires_at": "2026-04-05T12:00:00Z",
  "trace_id": "string",
  "operation": "auth.login",
  "outcome": "success"
}
```

### Comando
`auth logout`

### Entrada
- --session-id (obrigatorio)
- --json (opcional)

### Saida de sucesso (JSON)
```json
{
  "session_id": "uuid",
  "revoked": true,
  "trace_id": "string",
  "operation": "auth.logout",
  "outcome": "success"
}
```

## 3. Modulo observability-cli

### Comando
`trace validate`

### Entrada
- --trace-id (opcional)
- --tenant-id (obrigatorio)
- --json (opcional)

### Saida de sucesso (JSON)
```json
{
  "trace_id": "string",
  "tenant_id": "uuid",
  "propagation_status": "valid",
  "operation": "trace.validate",
  "outcome": "success"
}
```

## Erro padrao

```json
{
  "issue": [
    {
      "severity": "error",
      "code": "forbidden",
      "diagnostics": "tenant context missing or invalid"
    }
  ],
  "trace_id": "string",
  "operation": "auth.login",
  "outcome": "failure"
}
```

## Regras constitucionais obrigatorias

- Proibido qualquer dependencia de Keycloak/Auth0/Okta/Cognito/Azure AD B2C.
- Todos os comandos validam tenant na fronteira.
- Comandos administrativos cross-tenant exigem contexto separado e auditoria por acesso.