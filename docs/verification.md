# Registro de verificação

Verificação local em 16/10/2026, Windows, Java 17.

## Build e testes

Comando: `gradlew.bat check bootJar --no-daemon`.

Resultado: BUILD SUCCESSFUL, 14 testes executados, zero falhas e zero testes ignorados na suíte padrão. Os exemplos externos `*IT` são excluídos dessa suíte.

| Classe | Testes |
| --- | ---: |
| BudgetingApplicationTests | 1 |
| CategorySummaryTests | 1 |
| DemoProfileTests | 2 |
| TransactionApiTests | 8 |
| TransactionValidationTests | 2 |

JaCoCo registrou 135 de 138 linhas e 31 de 40 ramificações cobertas. Esses números descrevem o escopo atual; não comprovam qualidade da interpretação por IA nem comportamento em produção. O relatório é regenerado a cada execução de testes.

## JAR executável

Comando: `java -jar build/libs/budgeting-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo --server.port=18081`.

Requisições HTTP realizadas contra o processo empacotado:

- `/actuator/health`: status UP.
- `/transactions?page=0&size=2`: dois itens na página e quatro no total.
- `/transactions/GROCERIES/summary`: total em reais igual a 210,40.
- `/openapi.yaml`: HTTP 200.

O processo temporário foi encerrado após a verificação.

## Não executado

Chamadas reais OpenAI, banco MySQL, construção da imagem Docker e workflow no GitHub. Docker não está disponível no ambiente local e o repositório ainda não foi publicado. Esses pontos não são apresentados como integrações validadas.

## Frontend integrado

- `gradlew.bat check bootJar --no-daemon`: 14 testes Java passaram com os arquivos estáticos e `/api/config` incluídos na verificação HTTP.
- `node --test src/test/frontend/money.test.mjs`: três testes passaram, cobrindo conversão exata para centavos, rejeição de entradas inválidas e formatação em reais.
- `node --check src/main/resources/static/app.js`: sintaxe válida.
- Navegador contra o JAR demo: o valor zero foi rejeitado no formulário; um gasto de R$ 12,50 foi cadastrado e refletido nos totais; o filtro Farmácia mostrou somente a categoria selecionada; um segundo gasto fictício permitiu navegar à segunda página e voltar.
- A interface identifica o modo demo e mantém os controles de áudio desativados nesse modo.

Os registros de teste do navegador foram criados apenas no H2 efêmero do processo demo. O upload no navegador com provedor real não foi executado. Os testes do navegador foram manuais por automação de UI e não constituem uma suíte E2E persistida na CI.
