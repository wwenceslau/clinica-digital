# Research: Fundacao de IAM Nativo e RLS

## Contexto

Esta pesquisa valida decisoes para a fundacao do projeto Clinica Digital com governanca da constitution.md v1.6.0, com foco em:
- Art. XXII (IAM nativo, sem IdP externo)
- Art. 0 (isolamento multi-tenant com RLS)
- Art. VIII (simplicidade)
- Art. XV (degradacao graciosa)
- Art. XI (observabilidade ponta a ponta)

## Decisao 1: Hashing de senha para IAM interno

### Opcao A: Argon2id
- Pro: maior resistencia a ataques com GPU, memory-hard, recomendacao moderna.
- Contra: maior custo operacional inicial e calibracao mais sensivel de parametros para nao degradar login.

### Opcao B: BCrypt
- Pro: suporte nativo consolidado no ecossistema Spring Security, implementacao mais simples e madura.
- Contra: menor resistencia relativa a GPU quando comparado ao Argon2id.

### Decisao
Adotar BCrypt (cost 12) na fundacao, com trilha de migracao planejada para Argon2id em fase posterior.

### Rationale
- Menor risco de complexidade no bootstrap da plataforma (Art. VIII).
- Entrega mais rapida de um IAM interno robusto e auditavel (Art. XXII).
- Permite migracao incremental sem lock-in arquitetural.

### Alternatives considered
- Argon2id desde o inicio (rejeitado na fundacao por custo operacional e tuning inicial).

## Decisao 2: Gestao de sessao interna

### Opcao A: Sessao stateful em banco (token opaco + tabela sessions)
- Pro: revogacao imediata, auditoria natural, isolamento por tenant via RLS na propria tabela de sessao.
- Contra: leitura de sessao em banco por requisicao autenticada.

### Opcao B: JWT assinado com denylist para revogacao
- Pro: validacao local rapida em cenarios puramente stateless.
- Contra: revogacao mais complexa, maior risco operacional para consistencia da denylist, maior complexidade de isolamento por tenant.

### Decisao
Adotar sessao stateful com token opaco armazenado em PostgreSQL 15, sujeito a RLS.

### Rationale
- Revogacao imediata e previsivel para ambiente clinico.
- Melhor aderencia ao isolamento de tenants e governanca de auditoria (Art. 0, Art. XXII).
- Menor acoplamento com infraestrutura adicional na fundacao (Art. VIII).

### Alternatives considered
- JWT + denylist (rejeitado na fundacao por aumento de complexidade e risco de revogacao inconsistente).

## Decisao 3: Estrategia RLS no PostgreSQL 15

### Decisao
Aplicar RLS em todas as tabelas tenant-scoped com:
- tenant_id obrigatorio e FK para tenants
- ENABLE RLS + FORCE RLS
- USING e WITH CHECK baseados em current_setting('app.tenant_id')
- SET LOCAL app.tenant_id por transacao no boundary de cada request/CLI/evento

### Rationale
- Isolamento enforced no banco, reduzindo dependencia de disciplina na aplicacao.
- Alinhamento direto ao Art. 0 e aos gates de seguranca da constituicao.

### Alternatives considered
- Filtro por tenant apenas na camada de aplicacao (rejeitado por risco de bypass humano e nao conformidade constitucional).

## Performance e seguranca de RLS

### Melhores praticas
- Indices compostos iniciando por tenant_id em consultas quentes.
- Partial indexes para estados ativos (ex.: onde revoked_at is null).
- Prepared statements obrigatorios para evitar interpolacao de SQL.
- Proibir operacoes cross-tenant por padrao; excecoes apenas em contexto administrativo segregado e auditado.

### Armadilhas evitadas
- Uso de SET sem LOCAL (vazamento de contexto em pool).
- Tabela tenant-scoped sem policy RLS.
- Policy sem WITH CHECK para escrita.

## Degradacao graciosa (Art. XV)

### Risco: latencia no IAM interno
- Mitigacao: timeout curto na validacao de sessao, respostas padronizadas de degradacao para operacoes nao criticas, politica fail-closed para mutacoes sensiveis.

### Risco: latencia no PostgreSQL
- Mitigacao: circuit breaker por dependencia, retries limitados com jitter para leituras idempotentes, fila/outbox para eventos nao criticos, preservando trilha auditavel.

### Risco: acoplamento excessivo na resiliencia
- Mitigacao: manter fallback inicial simples (degradacao controlada + bloqueio seguro) e evoluir apenas com evidencia de gargalo em producao.

## Impacto no plano tecnico

1. A fundacao inicia com BCrypt + sessao stateful em banco com RLS.
2. A propagacao de tenant_id e trace_id torna-se contrato obrigatorio em HTTP, CLI e mensageria.
3. O baseline de resiliencia privilegia simplicidade: circuit breaker, timeout, retry limitado, fail-closed e auditoria.
4. A migracao para Argon2id e possivel sem alterar os contratos publicos dos modulos.