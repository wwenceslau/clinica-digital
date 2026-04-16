# Feature Specification: The Shell Estrutural da Clínica Digital

**Feature Branch**: `003-shell-layout-estrutural`  
**Created**: 2026-04-10  
**Status**: Draft  
**Input**: User description: "Contexto: Use a constitution.md v1.6.0 e a Tabela de Recursos como referências normativas. O projeto utiliza React 19, TypeScript, MUI 7 e Tailwind CSS com padrão Atomic Components. Objetivo: Execute /specify para criar a spec.md do layout estrutural (The Shell) da aplicação Clínica Digital."

## Clarifications

### Session 2026-04-10

- Q: Qual o comportamento da Sidebar para itens sem permissão — ocultar ou desabilitar? → A: Manter visível desabilitado (opacidade reduzida + tooltip explicativo), mantendo estrutura de domínios intacta.
- Q: Persistência da unidade selecionada no Header — como deve funcionar entre sessões? → A: Persistir a última unidade selecionada por usuário via localStorage ou preferência de perfil; reseta apenas se a unidade deixar de existir no tenant.
- Q: Qual o nível de acessibilidade alvo para o Shell? → A: WCAG 2.1 nível AA (navegação por teclado, contraste mínimo 4.5:1, ARIA landmarks e foco visível).
- Q: Como `trace_id` e `tenant_id` devem ser expostos na UI? → A: Rodapé fixo visível apenas em modo debug/diagnóstico (flag de ambiente ou role de suporte); nunca exposto a todos os usuários em produção.
- Q: Qual a meta de performance de carregamento inicial do Shell? → A: LCP ≤ 1,5s em conexão 4G simulada / hardware mediano, verificável via Lighthouse/Playwright.
- Q: (Adicional do usuário) Suporte a Dark/Light mode no Shell — é apropriado nesta fase? → A: Sim. Arquitetar tokens CSS para dualidade de tema (Light + Dark) agora com `data-theme` e MUI 7 ThemeProvider; UI de alternância (botão de toggle) deferida para iteração futura.
- Q: (Adicional do usuário) Internacionalização (pt-BR + en-US) no Shell — é apropriado nesta fase? → A: Sim. Arquitetar estrutura i18n agora (namespace de chaves, i18nProvider); preenchimento completo de traduções deferido para iteração futura.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Navegação Estrutural da Área Autenticada (Priority: P1)

Como usuário interno autenticado da clínica, quero acessar a aplicação por um layout único com menu lateral por domínios, para encontrar rapidamente os módulos clínicos e administrativos sem ambiguidade.

**Why this priority**: A navegação estrutural é a base para qualquer operação dentro da área autenticada e desbloqueia todos os demais fluxos.

**Independent Test**: Pode ser testada abrindo a área autenticada e verificando se os grupos e itens do menu lateral aparecem de forma completa, organizada e navegável.

**Acceptance Scenarios**:

1. **Given** um usuário autenticado dentro de um tenant válido, **When** o Shell é carregado, **Then** o sistema exibe o `MainTemplate` com `Sidebar` e `Header` em uma estrutura consistente.
2. **Given** o menu lateral visível, **When** o usuário expande os domínios de navegação, **Then** cada domínio apresenta os recursos esperados da Tabela de Recursos.
3. **Given** o domínio de Segurança, **When** o usuário acessa essa seção, **Then** o menu inclui Gestão de Usuários Internos, Perfis de Acesso e Auditoria.

---

### User Story 2 - Contexto Operacional no Header (Priority: P2)

Como profissional da clínica, quero ver no cabeçalho o contexto de tenant, unidade e perfil profissional, para confirmar que estou operando no contexto correto e evitar erros de atuação.

**Why this priority**: Reduz risco operacional e reforça o isolamento de contexto exigido para multi-tenant.

**Independent Test**: Pode ser testada validando o conteúdo do cabeçalho para diferentes usuários e unidades dentro do mesmo tenant, sem depender de integração de domínio.

**Acceptance Scenarios**:

1. **Given** um usuário autenticado com contexto válido, **When** o cabeçalho é renderizado, **Then** exibe identificação da clínica (tenant), seletor de unidade e perfil do profissional.
2. **Given** múltiplas unidades disponíveis no tenant, **When** o usuário altera a unidade no seletor, **Then** o contexto visual da unidade é atualizado no cabeçalho sem violar isolamento entre tenants.

---

### User Story 3 - Conformidade Visual e Observabilidade no Shell (Priority: P3)

Como equipe de produto e compliance, queremos que o Shell tenha regras de estilização centralizadas e metadados de rastreabilidade visíveis, para garantir consistência visual e aderência observável.

**Why this priority**: Garante governança de interface e rastreabilidade operacional como base para evolução segura.

**Independent Test**: Pode ser testada por inspeção estrutural do layout, verificando origem dos estilos e presença de `trace_id` e `tenant_id` no rodapé ou metadados de tela.

**Acceptance Scenarios**:

1. **Given** qualquer tela renderizada dentro do Shell, **When** o estilo é aplicado, **Then** as regras visuais vêm de CSS global e tokens padronizados, sem CSS específico por página.
2. **Given** a área autenticada carregada, **When** o usuário visualiza rodapé ou metadados, **Then** `trace_id` e `tenant_id` estão disponíveis para diagnóstico e auditoria operacional.

### Edge Cases

- Usuário autenticado sem permissões para alguns domínios: o item de navegação correspondente MUST ser exibido desabilitado (opacidade reduzida + tooltip explicativo), mantendo a estrutura de domínios intacta; o item NÃO deve ser ocultado.
- Tenant com apenas uma unidade: o seletor de unidade MUST exibir a única unidade como contexto ativo sem abrir dropdown, e a preferência armazenada deve ser ignorada se a unidade não existir mais no tenant.
- Falta temporária de metadados de rastreamento no carregamento inicial: o layout deve exibir estado neutro e atualizar assim que os metadados estiverem disponíveis.
- Lista extensa de itens de navegação: a barra lateral deve manter legibilidade e permitir acesso a todos os itens sem perda de contexto.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST prover um `MainTemplate` como container principal da área autenticada, com regiões definidas para `Sidebar`, `Header`, conteúdo principal e rodapé/metadados. O `MainTemplate` MUST importar `ThemeProvider` (MUI 7) e um `i18nProvider` (ou equivalente de contexto i18n) que envolva toda a estrutura do Shell, preparando a aplicação para suporte a múltiplos idiomas.
- **FR-002**: O sistema MUST apresentar a `Sidebar` como organismo de navegação dinâmica agrupada pelos domínios: Administração, Profissionais, Pacientes, Agenda, Atendimento, Diagnóstico & Terapêutica, Prevenção, Financeiro & Faturamento e Segurança.
- **FR-003**: O sistema MUST incluir, em cada domínio da `Sidebar`, os recursos de navegação correspondentes à Tabela de Recursos normativa.
- **FR-004**: O domínio Segurança MUST conter opções de Gestão de Usuários Internos, Perfis de Acesso e Auditoria, alinhadas ao Princípio XXII.
- **FR-005**: O sistema MUST exibir no `Header` a identificação do tenant (nome da clínica), seletor de unidade (`Location`) e identificação do perfil do usuário (`Practitioner`).
- **FR-006**: O sistema MUST preservar o contexto de tenant em toda a estrutura visual do Shell e impedir qualquer indicação visual de dados de outro tenant, alinhado ao Princípio 0.
- **FR-007**: O sistema MUST expor `trace_id` e `tenant_id` em rodapé fixo visível exclusivamente quando uma flag de modo debug/diagnóstico estiver ativa ou quando o perfil do usuário possuir role de suporte; em produção para perfis sem essa role, esses metadados MUST NOT ser renderizados na UI mas MUST estar disponíveis via atributos `data-*` no DOM para ferramentas de diagnóstico. (Princípio XI)
- **FR-008**: O sistema MUST centralizar a estilização estrutural em `globals.css`, incluindo diretrizes globais de utilitários, tokens visuais duais (Light e Dark via `data-theme` ou equivalente), e MUST proibir CSS específico por página. O `MainTemplate` MUST importar MUI 7 ThemeProvider configurado com suporte a dois temas (light, dark), aplicável a todos os componentes filhos do Shell.
- **FR-009**: A estrutura de componentes MUST seguir estritamente a organização `atoms/`, `molecules/`, `organisms/` e `templates/` para os elementos do Shell.
- **FR-010**: Esta entrega MUST se limitar ao layout estrutural, sem inclusão de lógica de negócio, APIs de domínio ou gerenciamento avançado de estado.
- **FR-011**: Itens de navegação na `Sidebar` para os quais o perfil ativo não possui permissão MUST ser renderizados no estado desabilitado (opacidade reduzida), com tooltip indicando acesso restrito, e MUST NOT ser ocultados da estrutura de domínios.
- **FR-012**: O `Header` MUST persistir a última unidade (`Location`) selecionada pelo usuário entre sessões via `localStorage` ou equivalente de preferência de perfil; se a unidade armazenada não existir mais no tenant, o Shell MUST selecionar a primeira unidade disponível como fallback.
- **FR-013**: Todos os componentes estruturais do Shell (Sidebar, Header, MainTemplate e rodapé de metadados) MUST atender WCAG 2.1 nível AA, incluindo: navegação completa por teclado, contraste mínimo de 4.5:1, ARIA landmarks semânticos e indicador de foco visível em todos os elementos interativos.
- **FR-014**: O Shell MUST atingir LCP (Largest Contentful Paint) ≤ 1,5s em conexão 4G simulada e hardware mediano, medido via Lighthouse; este critério é aplicável ao layout estrutural em isolamento, sem dados de domínio.
- **FR-015**: Os tokens visuais do Shell (cores, espaçamento, tipografia, sombras) MUST ser projetados com suporte dual a temas Light e Dark via `data-theme` CSS custom properties ou equivalente sem duplicação de regras; o `MainTemplate` MUST aplicar o tema correto via atributo `data-theme` na raiz HTML/MainTemplate. A UI de alternância de tema (botão de toggle) está fora de escopo desta entrega.
- **FR-016**: Todos os rótulos do Shell (domínios da Sidebar, labels do Header, tooltips, metadados de rodapé) MUST usar chaves de tradução estruturadas em namespace (ex.: `sidebar.administration`, `header.location`, `telemetry.trace-id`) vinculadas a um i18nProvider compatível com pt-BR e en-US; a estrutura deve estar em lugar, mas o conteúdo efetivo das traduções está fora de escopo desta entrega.

### Key Entities *(include if feature involves data)*

- **MainTemplate**: Estrutura mestre da área autenticada; define regiões fixas e comportamento estrutural comum.
- **SidebarDomainGroup**: Grupo de navegação por domínio funcional; contém rótulo do domínio e coleção de recursos navegáveis.
- **SidebarResourceItem**: Item de navegação associado a um recurso funcional; contém identificação, domínio de origem e destino de navegação.
- **HeaderContext**: Contexto operacional visível do usuário na sessão; inclui tenant, unidade ativa (persistida por usuário via localStorage/preferência de perfil) e perfil profissional.
- **ShellTelemetryMetadata**: Metadados de rastreabilidade do Shell; inclui `trace_id` e `tenant_id`. Renderizados visualmente em rodapé fixo somente em modo debug/diagnóstico (flag de ambiente ou role de suporte); sempre presentes como atributos `data-*` no DOM para uso por ferramentas de observabilidade.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% dos domínios e recursos definidos para o Shell estão visíveis na navegação da área autenticada conforme estrutura normativa aprovada.
- **SC-002**: Em testes funcionais de interface, 95% dos usuários internos localizam o domínio correto para sua tarefa em até 10 segundos.
- **SC-003**: Em validação de conformidade do layout, 100% das telas que usam o Shell exibem tenant, unidade e perfil no cabeçalho.
- **SC-004**: Em validação de observabilidade, 100% das telas que usam o Shell expõem `trace_id` e `tenant_id` como atributos `data-*` no DOM; em modo debug/diagnóstico ativo, esses valores são adicionalmente visíveis no rodapé fixo.
- **SC-005**: Em revisão estrutural de frontend, 100% dos componentes do Shell respeitam a organização `atoms/`, `molecules/`, `organisms/` e `templates/`.
- **SC-006**: Em auditoria de acessibilidade automatizada (ex.: axe-core), o Shell não apresenta violações de nível AA do WCAG 2.1; contraste mínimo de 4.5:1 verificado em 100% dos elementos de texto interativo.
- **SC-007**: Em auditoria Lighthouse com perfil 4G simulado, o Shell apresenta LCP ≤ 1,5s; medição executada sobre o layout em isolamento, sem mocks de APIs de domínio.
- **SC-008**: Em validação de conformidade visual, 100% dos tokens CSS do Shell (cores, espaçamento, tipografia, sombras) estão organizados em `globals.css` com suporte comprovado a Light e Dark themes; ambos os temas atendem SC-006 (WCAG 2.1 AA, contraste mínimo 4.5:1).
- **SC-009**: Em auditoria de estrutura i18n, 100% dos rótulos e strings do Shell (domínios, labels, tooltips) usam chaves de tradução de namespace estruturado; i18nProvider está integrado ao `MainTemplate` e testado com pt-BR como locale padrão.

## Assumptions

- O escopo desta especificação cobre somente a estrutura visual e a experiência de navegação da área autenticada.
- Regras de autorização detalhadas por perfil serão tratadas em etapa posterior, mantendo nesta fase apenas a estrutura necessária para suportá-las.
- A Tabela de Recursos e a Constitution v1.6.0 são referências normativas vigentes para nomenclatura de domínios e requisitos de isolamento/segurança.
- A solução seguirá o padrão de componentes atômicos já adotado pelo projeto e as diretrizes de design system existentes.
