# Decisões de arquitetura

## Contexto

Uma API de gastos tem regras determinísticas, enquanto transcrição e interpretação por IA dependem de um provedor externo. A demonstração do projeto precisa funcionar sem credenciais pagas.

## 1. Um caso de uso para cada operação

Controllers traduzem HTTP. PersistTransactionUseCase, ListTransactionsUseCase e SummarizeCategoryUseCase representam ações da aplicação. ProcessVoiceCommandUseCase coordena transcrição, ChatClient e síntese. O controller de áudio não configura modelos nem contém esse fluxo.

As ferramentas do ChatClient reutilizam os casos de uso de cadastro, listagem por categoria e resumo. A validação acontece no domínio, protegendo entradas de REST e de ferramentas.

Trade-off: a camada application ainda depende das anotações e interfaces Spring AI, herdadas da base. Não se trata de uma implementação estrita de arquitetura hexagonal. Criar portas para IA seria uma evolução quando houver mais de um provedor.

## 2. Contrato paginado independente de Spring Data

GET /transactions aceita filtro opcional, page a partir de zero e size entre 1 e 100. O adaptador JPA usa paginação no banco e ordenação por UUID. O contrato de domínio não expõe Page ou Pageable do Spring. A ordem é determinística, mas não representa ordem cronológica e páginas podem mudar com inserções concorrentes.

O endpoint legado GET /transactions/{category} continua disponível e não é paginado. O resumo ainda carrega a categoria em memória. Esses limites são explícitos para não atribuir escalabilidade que o projeto ainda não oferece.

## 3. Dinheiro e validação

Valores individuais são long em centavos, sempre positivos. Totais são BigDecimal. A desserialização rejeita valores fracionários para amount para evitar truncamento silencioso de dinheiro. A API apresenta erros de entrada como ProblemDetail.

## 4. Falhas parciais de IA

Uma falha pode ocorrer depois de um gasto já ter sido salvo. Falhas no fluxo de IA retornam 502 com a etapa (transcription, interpretation ou speech), sem expor mensagens do provedor nem segredos. A resposta orienta consultar transações antes de reenviar. O fluxo não implementa rollback distribuído nem idempotência e não promete execução exatamente uma vez.

## 5. Modo demo explícito

O perfil demo desliga modelos e endpoint de áudio e popula quatro gastos fictícios em H2 em memória. Os dados são descartados ao parar. O perfil padrão mantém o banco em arquivo e exige credencial para IA. Nenhuma resposta simulada é apresentada como chamada real de IA.

## 6. Verificação e operação

Testes HTTP exercitam API e JPA reais com H2; modelos de IA são simulados. Testes do perfil demo comprovam inicialização sem modelos. JaCoCo produz relatórios de cobertura sem um limite arbitrário. A CI executa check e bootJar sem segredo OpenAI. Há um Dockerfile em duas etapas com processo final sem root e health check HTTP via Actuator.

## 7. Frontend servido na mesma origem

O Spring Boot serve HTML, CSS e módulos JavaScript em `/`. O frontend chama URLs relativas da API, evitando configuração de CORS ou um segundo servidor de desenvolvimento. Não há CDN, fonte remota ou framework obrigatório para abrir a tela.

O painel consulta os três resumos e a página de transações em paralelo. Um identificador de geração impede que uma resposta antiga substitua um filtro mais recente. Descrições vindas do banco são inseridas via textContent, sem interpretar HTML. O formulário converte reais para centavos com BigInt e valida o limite seguro de números JSON do navegador, sem multiplicação de ponto flutuante.

O frontend consulta apenas sinalizadores públicos em `/api/config`; nenhuma credencial chega ao cliente. O modo demo desativa o upload explicitamente. O envio de áudio tem limite de tamanho no cliente e no servidor, bloqueia repetição durante o processamento e não é repetido automaticamente após falha. O player usa uma URL temporária de Blob, revogada antes de uma nova resposta.

Trade-offs: não há estado compartilhado entre abas, atualização por WebSocket nem cache de dados. Os totais globais e a listagem são consultas separadas e não formam um snapshot transacional. A confirmação de cadastro impede envio duplicado por duplo clique, mas não substitui idempotência no backend.

## Limites para produção

Faltam autenticação, autorização, isolamento de usuários, migrações de banco e idempotência. O H2 e ddl-auto são escolhas para estudo; antes de produção, adotar banco externo com migrações e políticas de segurança. O Dockerfile prepara a execução, mas não equivale a um deploy validado.
