# Naming Convention v1 - IAM + RNDS/FHIR

## Objetivo
Padronizar nomenclatura entre persistencia, contratos API/CLI e mapeamento FHIR/RNDS para reduzir ambiguidade e drift.

## Padrao Oficial
Padrao hibrido por camada:
- Banco de dados: snake_case
- API/JSON/DTO: camelCase
- Campos canônicos FHIR: manter semantica original FHIR no contrato (identifier, name, telecom, address, meta.profile)

## Regras Gerais
- Nomes de tabela em plural snake_case (ex.: organizations, practitioners, iam_sessions).
- Chaves primarias: id.
- Chaves estrangeiras: <entidade>_id (ex.: organization_id, practitioner_id).
- Campos de auditoria: created_at, updated_at, revoked_at.
- Campos booleanos de dominio devem usar nome semantico explicito (ex.: account_active, fhir_active), evitando variacoes mistas com `is_`.
- Campos estruturados JSON no banco: sufixo _json (ex.: fhir_identifier_json).
- Campos FHIR de apoio no banco devem ser prefixados com fhir_ quando diferirem do modelo de aplicacao (ex.: fhir_meta_profile).

## Regras FHIR/RNDS
- Organization minimo: identifier, name (ou identifier), active, telecom/address quando aplicavel, meta.profile.
- Practitioner minimo: identifier, name, active quando aplicavel, telecom/address quando aplicavel, gender/birthDate quando exigido por perfil, meta.profile.
- Sempre mapear explicitamente no plano/data-model o par:
  - persistencia snake_case -> contrato camelCase/FHIR

## Exemplos

### Organization
Persistencia (DB):
- fhir_identifier_json
- fhir_name
- fhir_active
- telecom_json
- address_json
- fhir_meta_profile

Contrato API (JSON):
- identifier
- name
- active
- telecom
- address
- meta.profile

### Practitioner
Persistencia (DB):
- fhir_identifier_json
- fhir_name_json
- fhir_telecom_json
- fhir_address_json
- fhir_gender
- fhir_birth_date
- fhir_meta_profile
- email
- password_hash
- display_name
- account_active

Contrato API (JSON):
- identifier
- name
- telecom
- address
- gender
- birthDate
- meta.profile
- email

## Tabela Minima de Mapeamento (obrigatoria)
Todo data-model deve incluir tabela de mapeamento para entidades FHIR/RNDS:
- db_column
- api_field
- fhir_path
- obrigatoriedade (required/optional)
- origem da regra (FHIR R4/RNDS profile)

## Referencias Canonicas
- RNDS FHIR Profiles: https://simplifier.net/rnds
- RNDS Integration Manuals: https://www.gov.br/saude/pt-br/composicao/seidigi/rnds
- HL7 FHIR R4: https://hl7.org/fhir/R4/
