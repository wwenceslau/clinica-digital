# Quickstart: Fundacao Modular Clinica Digital

## Objetivo

Validar rapidamente os contratos de modularidade, IAM nativo e isolamento de tenants definidos para a fundacao.

## Pre-requisitos

- Windows 11
- Java 21
- Maven 3.9+
- Node 22+
- PostgreSQL 15+ instalado nativamente no Windows 11 com extensoes necessarias

## Estrutura alvo

- Backend Maven multi-module (um modulo por biblioteca com API publica).
- Frontend React 19 + TypeScript com Atomic Components.

## Passos

### 1. Build do backend modular

```bash
mvn -q -DskipTests=false clean verify
```

### 2. Configurar banco local nativo (PostgreSQL 15+)

```bash
# Confirmar que o servico PostgreSQL local esta ativo no Windows
# Exemplo (PowerShell):
# Get-Service postgresql*
```

Criar um banco local para desenvolvimento (ex.: `clinica_dev`) e garantir usuario com permissao para migrations.

### 3. Configurar segredos locais (nao versionados)

As credenciais de banco **nao** devem ser gravadas em arquivos de configuracao versionados.

Use uma das opcoes abaixo:

- Opcao A: criar arquivo `.env` local (ignorado pelo Git) com `DB_URL`, `DB_USER`, `DB_PASSWORD`.
- Opcao B: configurar variaveis de ambiente no Windows (`DB_URL`, `DB_USER`, `DB_PASSWORD`).

Referencia de placeholders esperados no Spring profile `dev`: `application-dev.yml.template`.

### 4. Executar backend com perfil de desenvolvimento

```bash
# Exemplo com Maven/Spring Boot
mvn -pl backend/clinic-gateway-app spring-boot:run -Dspring-boot.run.profiles=dev
```

Alternativamente, exportar `SPRING_PROFILES_ACTIVE=dev` no ambiente e executar o comando padrao de run do modulo.

### 5. Aplicar migrations com RLS

```bash
# Exemplo: executar ferramenta de migration adotada no projeto
# deve criar tabelas tenants e IAM + policies RLS
```

### 6. Executar smoke de contratos CLI (JSON)

```bash
# Exemplos de contrato esperado
clinic-tenant-cli tenant create --slug acme --json
clinic-iam-cli auth login --tenant acme --username admin --password '***' --json
clinic-iam-cli auth whoami --json
```

### 7. Validar propagacao de contexto

Checklist minimo:
- tenant_id presente e validado na fronteira.
- trace_id gerado/preservado e propagado ate persistencia.
- logs JSON contendo tenant_id, trace_id, operation, outcome.

### 8. Testes obrigatorios

- Unit: regras de dominio e autorizacao de servico.
- Integration: RLS com PostgreSQL real via Testcontainers (obrigatorio, mesmo com banco local de dev).
- Contract: contratos CLI e APIs publicas de modulo.

## Criterios de aceite da fundacao

- Nenhum fluxo autenticado usa IdP externo.
- Operacao cross-tenant bloqueada por padrao.
- Excesso de cota afeta apenas tenant infrator.
- Erros seguem formato FHIR OperationOutcome quando aplicavel.