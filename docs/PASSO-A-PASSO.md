# Passo a passo para entregar o T5

Roteiro para a equipe colocar o pipeline no ar e coletar as evidências pedidas (uma execução com sucesso e uma falha detectada). Tempo estimado: 30–40 minutos.

## 1. Criar o repositório da equipe

1. No GitHub, crie um repositório novo (ex.: `biblioteca-t5`) **sem** README, .gitignore ou licença.
2. Público é o mais simples para o professor acessar. Se for privado, adicione o professor em **Settings → Collaborators**.

## 2. Enviar o código

Descompacte o arquivo `biblioteca-t5.zip` e, dentro da pasta:

```bash
git remote remove origin 2>/dev/null
git remote add origin https://github.com/<OWNER>/<REPO>.git
git push -u origin master
```

O histórico já vem com o commit do professor e o commit do T5.

> **Windows:** o `mvnw` precisa continuar executável no Git. Isso já está registrado no commit; não use "copiar e colar" de arquivos pelo navegador do GitHub, use `git push`.

## 3. Primeira execução (evidência de sucesso)

1. O push dispara o pipeline automaticamente. Abra a aba **Actions → CI - Biblioteca**.
2. Aguarde os 3 jobs ficarem verdes (leva por volta de 3–5 minutos na primeira vez; depois o cache acelera).
3. Tire os prints para a seção **11.1** do `docs/T5-PIPELINE.md`:
   - o grafo com os 3 jobs verdes;
   - o **Summary** da execução (tabela de testes, artefato gerado e imagem);
   - a lista de **Artifacts** no fim da página (`biblioteca-<sha7>` e `relatorio-testes-<sha7>`).
4. Copie o link da execução e o commit para o documento.

A imagem fica em **Packages** no perfil/organização. Por padrão o pacote é privado; se quiser que o professor veja, abra o pacote → **Package settings** → mude a visibilidade ou conceda acesso.

## 4. Exigir o pipeline para fazer merge (porta de qualidade)

Em **Settings → Branches** (ou **Settings → Rules → Rulesets**), crie uma regra para `master`:

- marque **Require a pull request before merging**;
- marque **Require status checks to pass** e adicione os checks `1. Validacao inicial` e `2. Build, testes e artefato` (eles só aparecem na busca depois da primeira execução).

## 5. Provocar uma falha (evidência de falha detectada)

Use um PR, que é o cenário do slide 54: o defeito é descoberto **antes** do merge.

```bash
git checkout -b demo/falha-senha
```

Abra `src/main/java/com/unialfa/biblioteca/service/UsuarioService.java` e, no método `salvar`, comente a linha que codifica a senha:

```java
            if (!(pwd.startsWith("$2a$") || pwd.startsWith("$2b$") || pwd.startsWith("$2y$"))) {
                // usuario.setPassword(passwordEncoder.encode(pwd));
            }
```

Ou seja: um "ajuste" que passaria a gravar senhas em texto puro.

```bash
git commit -am "Demo: remove codificacao da senha (deve falhar no CI)"
git push -u origin demo/falha-senha
```

No GitHub, abra um **Pull Request** de `demo/falha-senha` para `master`. Resultado esperado:

- job **2. Build, testes e artefato** vermelho, no step **Build e testes**;
- job **3** marcado como *skipped* (nenhum artefato nem imagem gerados);
- o **Summary** mostra `UsuarioServiceTest.codificaSenhaEmTextoPuro` com a mensagem do erro;
- o PR mostra o check falhando e o merge bloqueado (se fez o passo 4).

Tire os prints para a seção **11.2** e anote o link. Depois **feche o PR sem fazer merge** e apague a branch.

### Alternativa: falha na detecção de segredos

Se quiserem mostrar a etapa 1 falhando, em outra branch crie um arquivo com uma credencial **falsa**:

```bash
git checkout master && git checkout -b demo/falha-segredo
# a chave e montada em partes para que ESTE documento nao seja apontado pelo gitleaks
printf 'aws_access_key_id = %s%s\naws_secret_access_key = %s%s\n' \
  AKIA Z7Q3H4XWP2LMN6RT q8Vt3kP0zR7mXc2L wYb9Hn4Jd6Sa1Fe5Gu0Ti8Ok > credenciais.txt
git add credenciais.txt && git commit -m "Demo: credencial falsa (deve falhar no CI)"
git push -u origin demo/falha-segredo
```

O job **1. Validacao inicial** falha no step **Detectar segredos** e os jobs 2 e 3 ficam *skipped*. **Não faça merge** desta branch: como o gitleaks varre o histórico, um commit desses na `master` faria toda execução futura falhar.

## 6. (Opcional) Gerar uma versão

```bash
git tag v1.0.0 && git push origin v1.0.0
```

A imagem recebe também a tag `v1.0.0`.

## 7. Finalizar o documento

Em `docs/T5-PIPELINE.md`:

- preencha os nomes da equipe e a URL do repositório;
- cole os prints e links nas seções 11.1 e 11.2;
- confirme a primeira linha do checklist (acesso do professor).

Faça commit desse documento na `master`; isso gera mais uma execução verde, que também serve de evidência.

## 8. Rodar a aplicação na sua máquina

Agora a chave do JWT vem de variável de ambiente.

- **Linux/macOS:** `export JWT_SECRET="$(openssl rand -base64 48)"` e depois `./mvnw spring-boot:run`
- **Windows (PowerShell):** `$env:JWT_SECRET = [Convert]::ToBase64String((1..48 | % { Get-Random -Max 256 }))` e depois `.\mvnw.cmd spring-boot:run`
- **IntelliJ:** *Run → Edit Configurations → Environment variables* → `JWT_SECRET=<uma string com pelo menos 32 caracteres>`

Os testes (`./mvnw test`) não precisam da variável.
