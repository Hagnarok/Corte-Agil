# Rastreabilidade dos requisitos dos PDFs

Base: “Definição Incial do Projeto - CorteÁgil.pdf” e “CorteAgil - Especificação de Requisitos de Software.pdf”, fornecidos pelo grupo. A especificação orienta os fluxos; não se afirma que uma validação em navegador substitui a homologação de hardware e ambiente.

## Funcionais

| Requisito                                  | Implementação / situação                                                                           |
| ------------------------------------------ | -------------------------------------------------------------------------------------------------- |
| RF01 — Retirada pelo celular na rede local | `/cliente`, API acessível na LAN; restrição de alcance configurada no roteador/firewall            |
| RF02 — Senha única                         | Sequência e token gerados no servidor dentro da transação JPA                                      |
| RF03 — Inclusão na fila                    | Emissão já insere em `waiting`, com ordem global                                                   |
| RF04 — Posição em tempo real               | SSE dispara consulta individual; posição e quantidade à frente calculadas no backend               |
| RF05 — Número atual                        | Disponível ao cliente e no estado público                                                          |
| RF06 — Próximos atendimentos               | Exibidos nas telas de cliente, funcionário e painel                                                |
| RF07 — Aviso com duas à frente             | Evento persistido e aviso na página; notificação de navegador quando permitida                     |
| RF08 — Aviso com uma à frente              | Mesmo mecanismo, com orientação de retorno ao setor                                                |
| RF09 — Chamar próxima                      | Ação autenticada do funcionário; segue FIFO global                                                 |
| RF10 — Alerta sonoro                       | Web Audio no painel após ativação manual de som                                                    |
| RF11 — Senha chamada no painel             | Atualizada pelo mesmo SSE                                                                          |
| RF12 — Serviço opcional                    | Padrão “Não informado”; não exige seleção adicional                                                |
| RF13 — Visualização do serviço             | Lista e card do atendimento atual                                                                  |
| RF14 — Organização por serviço             | Filtros e contadores; sem reordenar arbitrariamente a fila                                         |
| RF15 — Interface de TV                     | `/painel`, contraste alto e ação de tela cheia                                                     |
| RF16 — Atendimento atual no painel         | Senha em destaque e balcão 01                                                                      |
| RF17 — Próximos no painel                  | Até três senhas seguintes                                                                          |
| RF18 — Emissão pelo totem                  | `/totem`, botões amplos e fluxo sem cadastro                                                       |
| RF19 — Ticket térmico                      | Implementação via impressão do navegador; falta homologação física no modelo de impressora adotado |
| RF20 — QR Code                             | QR real, gerado localmente, apontando ao acompanhamento individual                                 |
| RF21 — Fila única                          | Mesma API, banco e sequência, qualquer que seja a origem                                           |
| RF22 — Contagem de 30 segundos             | Deadline definido no servidor; contagem visual corrigida com horário do servidor                   |
| RF23 — Ausente                             | Ação bloqueada no servidor antes do deadline                                                       |
| RF24 — Tratamento de ausência              | Fica ausente e recebe convite para solicitar retorno; não retorna sem consentimento                |
| RF25 — Aviso de perda da vez               | Evento individual persistido e exibido ao cliente                                                  |
| RF26 — Solicitar retorno                   | Botão “Quero retornar à fila”                                                                      |
| RF27 — Reinserção                          | Endpoint pelo token da senha; valida que o estado é `absent`                                       |
| RF28 — Prioridade reduzida                 | Vai para o final da fila e mantém o identificador                                                  |
| RF29 — Atualização em eventos              | Snapshot enviado após confirmação da transação                                                     |
| RF30 — Sincronização                       | Quatro interfaces conectadas à mesma fonte de verdade                                              |

## Não funcionais

| Grupo                                                | Cobertura e limites                                                                                            |
| ---------------------------------------------------- | -------------------------------------------------------------------------------------------------------------- |
| RNF01, 06, 24 — Rede e implantação local             | Spring Boot na LAN, assets locais e instruções Windows. Equipamentos e firewall precisam de configuração local |
| RNF02, 03, 07 — Atualização, consistência e latência | SSE com reconexão, snapshot integral, banco central. Sem benchmark formal de latência                          |
| RNF04, 05, 18 — Unicidade e concorrência             | Transações JPA com bloqueio pessimista; testes de emissões e chamadas simultâneas                              |
| RNF08, 09, 10, 11, 12, 13 — Usabilidade e interfaces | Material, botões amplos, layout responsivo, painel dedicado, avisos textuais e etiquetas acessíveis            |
| RNF14 — Impressora                                   | CSS para 80 mm e impressão pelo navegador; integração física/driver deve ser validada no equipamento           |
| RNF15, 16 — Persistência e recuperação               | H2 em arquivo ou MySQL, fila e notificações persistidas, teste de reinício; instruções de backup manual        |
| RNF17 — Administração                                | Guard Angular mais autenticação e autorização no servidor; um usuário admin nesta versão                       |
| RNF19, 23 — Escala e desempenho                      | Implementação voltada a uma loja/um processo; testes funcionais, sem ensaio de carga em produção               |
| RNF20 — Manutenção                                   | Módulos por interface, shared, core, domínio puro e API separada                                               |
| RNF21 — Notificações                                 | Eventos persistidos e deduplicados por limiar/ciclo de retorno; página fechada depende de push adicional       |
| RNF22 — Privacidade                                  | Sem dados pessoais de cadastro; tokens não aparecem no estado público                                          |
| RNF25 — Auditoria                                    | Até 2.000 eventos persistidos; últimos 100 exibidos à equipe                                                   |

## Regras de negócio

| Regra                                 | Decisão implementada                                                   |
| ------------------------------------- | ---------------------------------------------------------------------- |
| RN01 — Ordem padrão                   | FIFO; uma senha ativa no balcão por vez                                |
| RN02 — Aviso antecipado               | Com 2 e 1 senhas à frente, contando atendimento ativo                  |
| RN03 — 30 segundos                    | Inicia na chamada e reinicia se o funcionário repetir a chamada        |
| RN04 — Ausência não definitiva        | Senha é mantida como ausente                                           |
| RN05 — Retorno com penalização        | Retorno explícito ao final da fila                                     |
| RN06 — Origem não separa fila         | `app` e `totem` usam a mesma fila                                      |
| RN07 — Serviço opcional               | “Não informado” é válido                                               |
| RN08 — Otimização respeita prioridade | Serviço visível e filtrável; não há chamada fora de ordem nesta versão |

## Decisões a confirmar na evolução

1. **Retorno:** os PDFs citam alternativas (“final da fila” ou “Prioridade Retornada”) sem fórmula exata. Foi adotado o final da fila, com etiqueta de retorno e trilha de auditoria, evitando critérios ocultos de prioridade.
2. **Um balcão:** a primeira versão usa balcão 01; múltiplos atendimentos simultâneos exigem adicionar entidade balcão/estação e estados ativos separados.
3. **Notificações:** a interpretação implementada é acompanhamento ativo na página e notificações do navegador quando permitido. Entrega em tela bloqueada/página fechada permanece uma integração futura.
4. **Impressão:** a interface gera o conteúdo e o QR; impressão silenciosa e comandos nativos da impressora não são simulados.
5. **Serviços rápidos:** a interface auxilia identificação de serviços; não há algoritmo para furar a ordem, pois os documentos não definem limites de reordenação.
