# CLI Contracts - IAM Nativo

## 1) bootstrap-super-user
Cria o primeiro super-user (profile 0) apenas se inexistente.

Command shape:
```bash
bootstrap-super-user --email <email> --password <secret> --name <text>
```

Observacao FHIR/RNDS:
- O super-user tambem deve possuir representacao minima de Practitioner com `identifier[]`, `name[]` e `meta.profile = http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude`.

Success JSON:
```json
{
  "status": "created",
  "profile": 0,
  "practitionerId": "uuid",
  "meta": { "profile": ["http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude"] },
  "auditEventId": "uuid"
}
```

Error JSON (already exists):
```json
{
  "resourceType": "OperationOutcome",
  "issue": [
    {
      "severity": "error",
      "code": "conflict",
      "diagnostics": "Super-user already exists"
    }
  ]
}
```

## 2) create-tenant-admin
Cria organization e admin (profile 10).

Command shape:
```bash
create-tenant-admin --tenant-name <text> --cnes <7digits> --admin-name <text> --admin-email <email> --admin-cpf <11digits> --admin-password <secret>
```

Forma padronizada recomendada:
```bash
create-tenant-admin --tenant-name <text> --tenant-display-name <text> --cnes <7digits> --admin-display-name <text> --admin-email <email> --admin-cpf <11digits> --admin-password <secret>
```

Observacao FHIR/RNDS:
- Organization criada deve incluir `identifier[]` com `system=https://saude.gov.br/sid/cnes`, `name`, `active`, `meta.profile = http://www.saude.gov.br/fhir/r4/StructureDefinition/BREstabelecimentoSaude`.
- Practitioner admin deve incluir `identifier[]` com `system=https://saude.gov.br/sid/cpf`, `name[]`, `meta.profile = http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude`.

Success JSON:
```json
{
  "status": "created",
  "tenantId": "uuid",
  "adminPractitionerId": "uuid",
  "organization": {
    "displayName": "Clinica Central",
    "accountActive": true,
    "identifiers": [{"system": "https://saude.gov.br/sid/cnes", "value": "1234567"}],
    "meta": {"profile": ["http://www.saude.gov.br/fhir/r4/StructureDefinition/BREstabelecimentoSaude"]}
  },
  "adminPractitioner": {
    "displayName": "Admin Clinica Central",
    "accountActive": true,
    "identifiers": [{"system": "https://saude.gov.br/sid/cpf", "value": "12345678901"}],
    "meta": {"profile": ["http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude"]}
  },
  "auditEventId": "uuid"
}
```

Error JSON:
- duplicidade de nome/CNES/email retorna OperationOutcome com code conflict.

## 3) login
Autentica por email/password e devolve sessao direta (1 tenant) ou desafio de selecao (>1 tenant).

Command shape:
```bash
login --email <email> --password <secret>
```

Success JSON (single org):
```json
{
  "mode": "single",
  "session": {
    "expiresAt": "2026-04-15T12:00:00Z",
    "practitioner": {
      "id": "uuid",
      "displayName": "Dra. Maria Silva",
      "profileType": 10
    },
    "tenant": {
      "id": "uuid",
      "name": "Clinica Central",
      "displayName": "Clinica Central",
      "accountActive": true,
      "cnes": "1234567",
      "identifiers": [{"system": "https://saude.gov.br/sid/cnes", "value": "1234567"}]
    },
    "activePractitionerRole": {
      "id": "uuid",
      "roleCode": "MEDICO_ASSISTENTE",
      "active": true,
      "location": {
        "id": "uuid",
        "displayName": "Unidade Centro"
      }
    }
  }
}
```

Success JSON (multiple org):
```json
{
  "mode": "multiple",
  "challengeToken": "challenge",
  "organizations": [
    { "organizationId": "uuid-1", "displayName": "Clinica A", "accountActive": true, "cnes": "1234567" },
    { "organizationId": "uuid-2", "displayName": "Clinica B", "accountActive": true, "cnes": "7654321" }
  ]
}
```

## 4) select-organization
Finaliza autenticacao no modo multiplo.

Command shape:
```bash
select-organization --challenge-token <token> --organization-id <uuid>
```

Success JSON:
```json
{
  "status": "authenticated",
  "expiresAt": "2026-04-15T12:00:00Z",
  "tenantId": "uuid",
  "practitionerRoleId": "uuid"
}
```

## 5) logout
Invalida a sessao atual no cliente e no backend.

Command shape:
```bash
logout
```

Success JSON:
```json
{
  "status": "revoked"
}
```

## Common Contract Rules
- Todos os erros sao retornados em formato FHIR OperationOutcome.
- Toda saida de sucesso deve ser JSON valido.
- Operacoes sensiveis devem registrar auditEventId quando aplicavel.
- Mensagens nao devem expor segredos, hashes ou dados pessoais completos.
- O token opaco e persistido prioritariamente em cookie seguro em producao; o CLI apenas reflete o resultado da emissao/invalidação da sessao, sem imprimir segredos persistidos em log.
