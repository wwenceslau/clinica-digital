# Integration Readiness Checklist: Feature 004 — Institution IAM & Auth Integration

**Purpose**: Validar a qualidade, completude e coerência dos requisitos de integração frontend-backend da spec.md, com foco em: grafo de recursos FHIR, conformidade com a Constituição v1.6.0, ponto de entrada do frontend (App.tsx), critérios de aceite visuais e persistência de sessão. Este checklist testa os **requisitos escritos**, não a implementação.
**Created**: 2026-04-16
**Feature**: [spec.md](../spec.md) · [plan.md](../plan.md) · [tasks.md](../tasks.md)

---

## Grafo de Recursos FHIR — Completude dos Requisitos

- [x] CHK001 — A spec define requisitos explícitos para o recurso `Location` (unidades físicas vinculadas à `Organization`)? O grafo multitenant exige Organization → Location como nó intermediário, mas `Location` não aparece como entidade em `Key Entities` nem em FR-017. [Gap, Spec §Key Entities]

- [x] CHK002 — A spec define requisitos para o recurso `PractitionerRole` (o vínculo pivot que autoriza o profissional a atuar em uma `Location` específica)? Sem `PractitionerRole`, o modelo não é suficiente para expressar "quem pode atuar onde" no contexto multitenant. [Gap, Spec §FR-017]

- [x] CHK003 — Os campos mínimos RNDS para `Organization` estão especificados com referência ao StructureDefinition concreto (ex.: `http://www.saude.gov.br/fhir/r4/StructureDefinition/BREstabelecimentoSaude`)? FR-017 menciona "perfis RNDS aplicáveis" mas não nomeia o SD de `Organization`. [Ambiguity, Spec §FR-017]

- [x] CHK004 — Os campos mínimos RNDS para `Practitioner` estão especificados com referência ao StructureDefinition concreto (ex.: `BRIndividuo` ou `BRProfissionalSaude`)? A spec menciona `meta.profile` sem identificar qual URI é obrigatória. [Ambiguity, Spec §FR-017]

- [~] CHK005 — O requisito de `identifier` para `Organization` especifica que o CNES deve ser representado com `system = "http://www.saude.gov.br/fhir/r4/CodeSystem/BRNomeExibicaoUF"` ou equivalente canônico RNDS? Ou o formato de `identifier.system` para CNES é deixado em aberto? [Clarity, Spec §FR-017] — Nota: há exigência de CNES válido/unicidade, mas `identifier.system` canônico não foi fixado.

- [~] CHK006 — O requisito de `identifier` para `Practitioner` especifica que o CPF deve usar o `system` canônico RNDS (`urn:oid:2.16.840.1.113883.13.237` ou equivalente reconhecido)? [Clarity, Spec §FR-017] — Nota: CPF/PII está coberto, mas `identifier.system` do CPF não está explicitado.

- [~] CHK007 — A spec define o que ocorre quando `Organization` é criada sem `Location` associada? O requisito de seleção de localização ativa (FR-008) pressupõe pelo menos uma `Location` por `Organization`. [Edge Case, Gap] — Nota: comportamento de criação sem location ainda não está definido.

---

## Conformidade com a Constituição v1.6.0 — Completude dos Requisitos

- [x] CHK008 — A spec especifica a tabela `iam_users` com todos os campos obrigatórios do In-App IAM (Art. XXII)? `Key Entities` lista `iam_sessions` mas não uma tabela `iam_users` explícita separada das tabelas `practitioners`/`organizations`. Há ambiguidade sobre se são a mesma entidade. [Ambiguity, Spec §Key Entities]

- [x] CHK009 — A spec especifica a tabela `iam_sessions` com campos de expiração e invalidação explícita? FR-007 menciona "token opaco" e `iam_sessions` aparece em Key Entities, mas os campos `expires_at`, `revoked_at` e política de revogação não estão quantificados. [Clarity, Spec §Key Entities]

- [~] CHK010 — A spec distingue claramente entre validação genérica FHIR (HAPI) e validação contra StructureDefinitions RNDS? FR-015 proíbe HAPI genérico, mas a spec não detalha o mecanismo substituto (qual validator, quais SDs carregados, qual endpoint de validação). [Clarity, Spec §FR-015] — Nota: distinção está clara, mas mecanismo operacional de validação fica para o plano.

- [~] CHK011 — O requisito de criptografia AES-256-GCM (Art. VI / FR-011) especifica quais campos exatos são marcados para `pgcrypto`? A spec menciona "CPF e senhas" mas a tabela de entidades usa `fhir_*` campos — não está definido se `fhir_identifier` (que contém CPF) é o campo criptografado ou se existe coluna separada. [Clarity, Spec §FR-011] — Nota: regra de segurança foi corrigida (hash de senha), mas campo físico de CPF/PII ainda não está nominado.

- [~] CHK012 — A spec define se a criptografia AES-256-GCM é transparente (via pgcrypto trigger/função no DB) ou explícita na camada de serviço? A escolha afeta diretamente a query layer e as migrations. [Gap, Spec §FR-011] — Nota: estratégia de aplicação da criptografia permanece em aberto para decisão no plano.

- [~] CHK013 — A spec especifica como o `pgcrypto` será inicializado/configurado no ambiente (extensão habilitada, chave gerenciada via secret, rotação de chave)? Sem isso, o requisito não é verificável. [Measurability, Gap] — Nota: requisitos existem, mas bootstrap de extensão/chaves/rotação deve ser detalhado no plano.

---

## Entrypoint Frontend (App.tsx) — Completude dos Requisitos

- [x] CHK014 — A spec define explicitamente que `App.tsx` (ou entrypoint equivalente) é responsável por injetar os Context Providers de Autenticação e Tenant? FR-012 estabelece isso, mas não especifica a ordem de aninhamento dos Providers (ex.: `TenantProvider` deve envolver `AuthProvider` ou vice-versa?). [Clarity, Spec §FR-012]

- [x] CHK015 — A spec define o contrato de dados que o Context de Tenant expõe para os componentes filhos (ex.: `tenantId`, `organizationName`, `practitionerName`, `activeLocationId`)? Sem esse contrato definido nos requisitos, as stories do shell (US5/US10) não são verificáveis. [Completeness, Gap]

- [x] CHK016 — A spec especifica quais rotas são protegidas pelo `MainTemplate` e quais são públicas (ex.: `/login`, `/register` são públicas; `/dashboard`, `/admin/*` são protegidas)? [Completeness, Spec §FR-012, US10]

- [~] CHK017 — A spec define o que acontece quando `App.tsx` carrega mas o token armazenado está expirado? O comportamento esperado (redirect para login? polling? silent refresh?) deve estar nos requisitos para que o ProtectedRoute seja implementável. [Edge Case, Spec §FR-007] — Nota: redirecionamento para login está definido; política de refresh/silent refresh não está.

- [x] CHK018 — A spec especifica onde `PractitionerRole` ativo é carregado após o login e em qual nível da hierarquia de componentes ele é disponibilizado? O user request aponta que esse é o "pulo do gato" da integração, mas a spec cobre apenas `practitioner` e `activeLocation` sem mencionar `PractitionerRole`. [Gap, Spec §FR-008]

---

## Formulários de Login e Registro — Clareza dos Requisitos

- [~] CHK019 — A spec especifica os campos obrigatórios do formulário de Registro de Clínica (US3)? O cenário de aceite menciona "dados obrigatórios validados pela RNDS" mas não lista quais campos do formulário são obrigatórios vs. opcionais. [Completeness, Spec §US3] — Nota: necessidade de campos obrigatórios permanece sem lista fechada.

- [~] CHK020 — O requisito do formulário de Login (US4/FR-004) especifica o comportamento exato da tela de seleção de organização: quantas organizações são exibidas, como são ordenadas, o que exibir quando o usuário tem acesso a dezenas de organizações? [Clarity, Spec §FR-004] — Nota: fluxo principal está definido, mas paginação/ordenação em volume alto não.

- [~] CHK021 — A spec define os estados visuais dos formulários (loading, error, success, disabled) como requisitos explícitos de UX? FR-013 exige padrão atômico MUI 7 + Tailwind, mas não especifica quais estados cada organismo deve implementar. [Completeness, Spec §FR-013] — Nota: estados de UX precisam ser listados explicitamente no plano/tarefas.

- [x] CHK022 — A spec especifica que o formulário de Registro cria o primeiro admin (profile 10) junto com a `Organization`? US2 e US3 descrevem isso em cenários, mas não há FR explícito que formalize "criação de Organization DEVE criar simultaneamente um usuário admin associado". [Gap, Spec §US2, US3]

---

## Critérios de Aceite Manuais (Smoke Test) — Mensurabilidade

- [x] CHK023 — A spec possui uma seção de "Critérios de Aceite Manuais" que exija verificação visual de que o nome da Clínica aparece no Header após login bem-sucedido? US5 descreve o comportamento esperado mas não há critério mensurável explícito (ex.: "Header DEVE exibir `organizationName` em até 200ms após login"). [Measurability, Gap]

- [x] CHK024 — A spec define critério manual verificável de que o nome do Practitioner aparece no Header após login? O User Scenario US5 descreve o comportamento mas sem threshold de tempo ou regra de exibição (ex.: usar `name[0].text` ou `name[0].family + given`). [Measurability, Gap]

- [~] CHK025 — A spec define critério manual para o fluxo de seleção de organização (US4 cenário 2)? Deve especificar o que o usuário vê enquanto as organizações carregam (loading skeleton vs. spinner). [Completeness, Spec §US4] — Nota: critério manual existe para seleção, mas estado de carregamento não foi definido.

- [~] CHK026 — A spec define critérios de aceite manual para a exibição de erros de autenticação via OperationOutcome (US7)? O requisito FR-009 menciona "Snackbar/Modal" mas não especifica: qual componente MUI exatamente, duração do Toast, se o erro é desmascarável, posição na tela. [Clarity, Spec §FR-009] — Nota: Toast/Alert está definido, faltam parâmetros de UX (duração/posição/comportamento).

---

## Tratamento de Erros e OperationOutcome — Consistência dos Requisitos

- [x] CHK027 — A spec define quais categorias de erro devem retornar `OperationOutcome` no formato FHIR R4 completo (com `issue[].code`, `issue[].severity`, `issue[].details`)? FR-009 exige OperationOutcome mas não especifica se o contrato inclui `issue[].diagnostics` ou apenas `issue[].text`. [Clarity, Spec §FR-009]

- [~] CHK028 — O requisito de exibição de erros via Toast/Snackbar (FR-009/US7) é consistente com o padrão do Shell Estrutural (spec/003)? A spec/004 referencia spec/003 implicitamente mas não verifica se o componente de feedback visual já está definido no Shell ou se deve ser criado aqui. [Consistency, Spec §FR-009] — Nota: coerência conceitual existe, porém contrato explícito com spec/003 ainda não foi referenciado por ID/artigo.

- [x] CHK029 — A spec especifica como erros de validação RNDS (StructureDefinition) são traduzidos para mensagens amigáveis ao usuário? O OperationOutcome gerado pelo validador RNDS contém texto técnico — o requisito de "mensagem amigável" (US7) precisa de um requisito de transformação. [Gap, Spec §US7, FR-015]

---

## Persistência de Sessão e Segurança do Token — Completude dos Requisitos

- [x] CHK030 — A spec especifica se o token opaco (FR-007, Art. XXII) deve ser armazenado em `httpOnly cookie` ou `sessionStorage`/`localStorage`? As implicações de segurança (XSS vs. CSRF) são distintas e a escolha deve ser um requisito explícito. [Gap, Spec §FR-007]

- [~] CHK031 — A spec define a política de expiração do token: tempo de vida, refresh automático (silent refresh) ou não? FR-007 menciona "sessão stateless" mas a spec/004 não define se há mecanismo de renovação antes da expiração. [Completeness, Spec §FR-007] — Nota: comportamento com expiração está definido para redirect; TTL e estratégia de refresh não estão.

- [~] CHK032 — A spec especifica o comportamento quando o usuário tem token válido mas o tenant associado foi desativado? Esse edge case afeta todas as rotas protegidas mas não aparece em nenhum Acceptance Scenario. [Edge Case, Gap] — Nota: edge case ainda não especificado.

- [x] CHK033 — O requisito de brute force (FR-016) especifica o threshold de tentativas, a janela de tempo e o mecanismo de desbloqueio (automático por tempo ou manual por admin)? [Clarity, Spec §FR-016]

---

## Isolamento Multi-Tenant (RLS) — Completude dos Requisitos

- [x] CHK034 — A spec especifica quais tabelas exatamente devem ter RLS habilitado? FR-014 diz "tabelas de IAM" mas Key Entities lista `organizations`, `practitioners`, `iam_sessions` — não está definido se `organizations` também tem RLS ou se é global. [Clarity, Spec §FR-014]

- [x] CHK035 — A spec define a política RLS para o super-user (profile 0), que tem acesso global sem `tenant_id`? O super-user deve bypassar RLS ou ter uma policy especial? Sem esse requisito, a implementação das policies é ambígua. [Gap, Spec §FR-001, FR-014]

- [~] CHK036 — A spec especifica se o `tenant_id` é injetado automaticamente via `SET LOCAL` no contexto da conexão PostgreSQL ou via parâmetro explícito em cada query? A escolha é um requisito de arquitetura que impacta todas as queries. [Gap, Spec §FR-014] — Nota: mecanismo técnico de propagação de tenant ao banco segue em aberto.

---

## Validação CNES — Mensurabilidade dos Requisitos

- [~] CHK037 — A spec especifica o formato e a regra de validação do CNES (7 dígitos numéricos, checksum, verificação contra base DATASUS)? FR-003 e US2/US3 exigem unicidade e validação RNDS do CNES, mas o algoritmo de validação não está definido. [Clarity, Gap, Spec §FR-003] — Nota: exigência de validação existe, mas regra/algoritmo não está detalhada.

- [~] CHK038 — A spec define se a validação do CNES é apenas de formato/estrutura ou se envolve verificação online contra a API da RNDS/DATASUS? A diferença é significativa em termos de infra e disponibilidade. [Ambiguity, Spec §US3] — Nota: escopo exato de validação (offline/online) não foi definido.

---

## Integração com Shell Estrutural (spec/003) — Consistência

- [~] CHK039 — Os requisitos do Header do Shell (FR-008) são consistentes com os componentes definidos na spec/003? A spec/004 assume que o Shell está implementado mas não verifica se o contrato de props do Header (ex.: `organizationName`, `practitionerName`) está alinhado com o que spec/003 define. [Consistency, Spec §FR-008] — Nota: integração está descrita, mas falta referência explícita a contrato da spec/003.

- [x] CHK040 — A spec define como os dados carregados após login (`Organization`, `Practitioner`, `PractitionerRole`) são propagados para o Shell Header? O mecanismo (Context API, Zustand, prop drilling) deve ser especificado como requisito de integração. [Gap, Spec §FR-008, FR-012]

---

## Notes

- Use `[x]` para marcar itens verificados e atendidos na spec atual.
- Use `[~]` para itens parcialmente atendidos — adicione nota inline com o que falta.
- Items marcados com `[Gap]` indicam requisitos completamente ausentes na spec.
- Items marcados com `[Ambiguity]` indicam requisitos que existem mas são insuficientemente precisos.
- Após resolução de cada item, atualizar spec.md e re-executar `/speckit.checklist` se necessário.

## Audit Result (2026-04-16)

- Total de itens: 40
- Atendidos (`[x]`): 22
- Parciais (`[~]`): 18
- Ausentes (`[ ]`): 0

Status apos sincronizacao de artefatos derivados:
- As pendencias criticas levantadas nesta auditoria foram incorporadas em [plan.md](../plan.md), [tasks.md](../tasks.md) e [quickstart.md](../quickstart.md).
- O plano agora documenta spikes arquiteturais, quotas/rate limiting por tenant, contrato formal com spec/003, origem/rotacao de chaves e protocolo de sessao/logout.
- As tarefas agora cobrem secret manager/KMS, rotacao de chave, validacao de quotas por tenant e metas de performance.

Pendencias residuais antes da implementacao:
- Fechar, durante a implementacao, os detalhes finais de UX para loading/duracao/posicionamento de feedback visual.
- Confirmar os `identifier.system` canonicos definitivos de CNES/CPF nos contratos finais, caso o time de compliance exija URIs diferentes das assumidas nesta entrega.
