# Teste de áudio com dois cliques

1. Na pasta do projeto, dê dois cliques em **Testar audio.cmd**.
2. Cole sua chave OpenAI na janela e pressione Enter. A digitação fica oculta e o atalho não grava a chave em arquivo.
3. Aguarde o navegador abrir em `http://localhost:18082/#voice`.
4. Clique em **Escolher arquivo de áudio**, selecione uma gravação e clique em **Enviar comando**.

Para testar sem gravar um áudio novo, escolha um dos arquivos em `src/test/resources/audio/`, por exemplo `recording-1.m4a`. Depois confira a resposta e a transação cadastrada.

Para gravar seu próprio exemplo, diga: “Gastei trinta reais com cinema hoje, na categoria Lazer”. A aplicação registra o gasto e identifica uma categoria existente.

O teste precisa de Java 17, conexão com a internet e uma chave OpenAI válida com acesso e saldo para os modelos configurados. As chamadas podem gerar custos. O áudio e o comando são enviados ao provedor de IA.

Mantenha a janela do atalho aberta durante o uso. Para encerrar, pressione Ctrl+C nessa janela. Se houver uma janela de teste anterior aberta, encerre-a antes de iniciar outra.

O teste roda na porta 18082, separada da prévia demo (18081), e guarda os gastos em `data/audio-test`. O atalho não verifica a validade da chave antes de abrir a tela; uma chave inválida ou sem saldo será identificada ao enviar o comando. Ele não cria categorias novas por voz.

## Gravar diretamente no navegador

1. Inicie `Testar audio.cmd` e mantenha a janela aberta.
2. Em `http://localhost:18082/#voice`, clique em **Gravar pelo microfone**.
3. Permita o microfone no navegador, fale e clique em **Parar gravação** (limite de 60 segundos).
4. Ouça a gravação no player e clique em **Enviar comando**.

A gravação fica local até clicar em Enviar comando. Se o navegador não disponibilizar o microfone, abra o endereço no Chrome ou Edge ou envie um arquivo. O modo de demonstração não habilita processamento de áudio. A gravação não altera a necessidade de configurar a chave e a disponibilidade da API.

O lançador carrega os arquivos do front-end diretamente de `src/main/resources/static/`. Se já estava aberto antes desta alteração, encerre a execução com Ctrl+C e abra o lançador novamente; depois atualize o navegador com Ctrl+F5.

## Modo gratuito, sem OpenAI

Abra `Testar gratis.cmd` ou selecione **Budgeting — Gratuito (sem OpenAI)** no VS Code e pressione F5. Acesse `http://localhost:18083/#voice`.

Clique em **Ditar gasto sem OpenAI**, permita o microfone e diga “Gastei cinquenta reais no mercado hoje”. Confira o texto, clique em **Revisar gasto**, corrija os dados se necessário e confirme em **Salvar gasto**. Também é possível digitar o comando. O modo gratuito usa regras simples para sugerir valor e categoria; não substitui o assistente Spring AI e não interpreta consultas financeiras nem transcreve arquivos enviados. Revise sempre o valor e a data.

O reconhecimento depende do suporte e do serviço de ditado do navegador. Pode enviar a fala ao serviço do navegador e exigir internet; não é uma solução garantidamente offline. Referência: https://developer.mozilla.org/en-US/docs/Web/API/SpeechRecognition

O perfil `free` desativa os modelos Spring AI, não pede chave e salva os dados em `data/budgeting-free`. A integração paga está dentro de uma seção opcional fechada. As transações deste modo ficam separadas dos bancos de teste de áudio e demonstração.
