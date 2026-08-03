# Como contribuir

Se você nunca trabalhou em equipe com Git, este guia é pra você. Leia uma vez
inteiro antes do primeiro commit — são 5 minutos e evitam a maior parte da dor.

## A regra que explica todas as outras

A branch `main` é sagrada e está protegida: **ninguém commita direto nela**, nem o
tech lead. Tudo entra por Pull Request com pelo menos uma aprovação.

Isso não é burocracia. É o que garante que, em qualquer momento, a `main` funciona —
e que ninguém sobrescreve o trabalho de outra pessoa sem perceber.

## O fluxo, do começo ao fim

### 1. Pegue uma issue

Vá em [Issues](../../issues) e filtre pelo milestone da sprint atual.
Se está começando, procure a etiqueta **`boa primeira tarefa`**.

**Antes de escrever qualquer código**, clique em *Assignees* e coloque seu nome.
Isso avisa aos outros que a tarefa tem dono. Duas pessoas fazendo a mesma coisa é
o desperdício mais comum e o mais fácil de evitar.

Toda issue tem um bloco **"Pronto quando"**. É o combinado do que conta como
terminado — não é sugestão. Se estiver ambíguo, pergunte na própria issue, assim
a resposta fica registrada para quem vier depois.

### 2. Atualize sua `main` e crie a branch

```bash
git checkout main
git pull
git checkout -b 13-scraper-sympla
```

O nome da branch é **número da issue + descrição curta em minúsculas com hífen**.
Olhando a lista de branches, dá para saber o que cada pessoa está fazendo.

Faça isso toda vez. Branch criada a partir de uma `main` velha gera conflito
desnecessário depois.

### 3. Trabalhe em pedaços pequenos

Commits pequenos e frequentes, cada um com uma ideia só:

```bash
git add caminho/do/arquivo
git commit -m "adiciona extração de data do evento"
```

Mensagem no **imperativo** e explicando *o quê*, não *como*:

- ✅ `adiciona extração de data do evento`
- ✅ `corrige fuso horário na listagem`
- ❌ `alterações` · `wip` · `agora vai` · `commit`

Você vai reler essas mensagens daqui a três semanas tentando entender o que
quebrou. Escreva para esse seu eu futuro.

### 4. Suba e abra a Pull Request

```bash
git push -u origin 13-scraper-sympla
```

O GitHub responde com um link para abrir a PR. O template já pergunta o que
importa: o que mudou, como testar, qual issue fecha.

Escreva `Closes #13` na descrição — a issue fecha sozinha quando a PR entrar.

**PR pequena é revisada rápido; PR gigante fica dias parada.** Se a tarefa é
grande, quebre em duas ou três PRs que entram em sequência.

### 5. Revisão

Alguém do time revisa. Comentário em revisão é sobre o código, nunca sobre a
pessoa — e receber comentário é normal, acontece com todo mundo, inclusive com
quem tem dez anos de experiência.

Se pedirem mudanças: faça na mesma branch, commit, push. A PR atualiza sozinha.

### 6. Depois do merge

```bash
git checkout main
git pull
git branch -d 13-scraper-sympla
```

Branch que já entrou não serve para mais nada. Apagar mantém a lista limpa.

## Segurança — isto aqui não tem meio-termo

**Este repositório é público.** Qualquer pessoa na internet lê o que você subir.

Nunca comite senha, token, chave de API ou string de conexão de banco. Nem
"temporariamente", nem "só para testar". Uma vez enviado, está no histórico do Git
para sempre, mesmo que você apague no commit seguinte — e bots varrem o GitHub
procurando exatamente isso, em minutos.

O que fazer:

- Localmente, use um arquivo `.env` — que está no `.gitignore` e nunca sobe
- Em produção e no CI, use **Settings → Secrets and variables**
- Comitou algo por engano? **Avise imediatamente**, sem vergonha nenhuma. A chave
  precisa ser revogada e trocada, e quanto mais cedo, menor o estrago. Apagar em
  silêncio é muito pior do que avisar.

## Sobre usar IA

Pode usar. Ninguém aqui é contra.

O combinado é: **você é responsável pelo que envia**. Se não sabe explicar o que o
código faz na revisão, ele ainda não está pronto para a PR. IA acerta muito e erra
com uma confiança impressionante — a revisão é sua, não dela.

## Ficou travado?

Pergunte. Na issue, na PR ou no grupo. Ninguém aqui espera que você já saiba tudo —
o projeto existe também para aprender. Travar três horas calado é o único erro de
verdade.
