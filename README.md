# y4kv2-commons: Base compartilhada do Y4Kv2

Tudo que mais de um servidor usa e que muda junto com a versão do Paper.

## O que este repositório faz

- **POM pai** (`net.y4k:y4kv2-parent`): JDK 25, versão fixada da Paper API, JUnit 5, AssertJ e MockBukkit. Todo plugin Java do Y4Kv2 herda dele.
- **`y4k-core`**: identidade Year4000 (nome, logo, paleta original, aviso `&7[&e!&7]`, moldura `&7&m****`), mensagens e idiomas PT/EN/ES, cargos gregos (Theta/Mu/Pi/Sigma, Tau/Delta/Omega) e utilitários sem regra de modo. Criado em F0.5; os papéis dependem de `net.y4k:y4k-core:2.0.0-alpha.1`.
- **`y4k-progression`**: plugin de cargos persistentes com LuckPerms/MariaDB, contextos por servidor e primeiro spawn limitado.
- **`paper-test-environment`**: harness MockBukkit compartilhado pelos testes de adaptadores.
- **Imagem `ghcr.io/ramosbezzu/y4kv2-paper-base`**: itzg/minecraft-server fixada + Paper fixado + LuckPerms + ViaVersion + `y4k-core` + `paper-global.yml` e config LuckPerms. Todas as imagens Paper do Y4Kv2 fazem `FROM` dela.

## Publica

Pacotes Maven no GitHub Packages e a imagem `y4kv2-paper-base`.

## Atualizar o Paper da rede inteira

1. Trocar Paper API no POM pai e o JAR em `versions.lock.json` (origem, build, SHA-256).
2. CI verde, release `vX.Y.Z`, imagem publicada.
3. Cada papel sobe o `FROM` e a dependência; a raiz promove os digests.

## Vem do monorepo y8k

### Conteúdo atual (fase 1)

Migrado do monorepo `y8k` nesta fase, só código Java e arquivos de build/CI:

| Aqui | Origem no `y8k` | Observação |
| --- | --- | --- |
| `pom.xml` (`net.y4k:y4kv2-parent:2.0.0-alpha.1`) | `pom.xml` raiz | Mesmas propriedades (Java 25, JUnit, AssertJ, MockBukkit, Paper API de teste), repositório PaperMC, `pluginManagement` e Surefire com `failIfNoTests`. Também agrega os módulos deste repositório e publica no GitHub Packages. |
| `paper-test-environment/` | `tests/paper` | Harness MockBukkit, 2 testes. Pacote `net.y8k.testing` renomeado para `net.y4k.testing` em 2026-10-28. |
| `y4k-core/` | Novo (F0.5) | Biblioteca compartilhada `net.y4k:y4k-core`: `Brand` (nome "Year4000", paleta e formatos originais: rótulo/valor, aviso, erro, título de menu, item clicável, conexão do linker, logo com shimmer; cores hex antigas mantidas como `@Deprecated` por compatibilidade; leitura de `[game] version` do `config.ini`), `rank.Rank` (cargos gregos com símbolo, cor e classificação VIP/staff de `infra/ranks.json`) e `i18n.Messages` (`.properties` UTF-8 por locale PT/EN/ES, fallback pt -> en -> `[chave]`, placeholders `{}` e `{0}`, cores legadas para Adventure). 37 testes, sem MockBukkit. |
| `y4k-progression/` | `plugins/progression` | Plugin de cargos e primeiro spawn, 25 testes; textos ao jogador em PT/EN/ES na paleta Year4000 (`ProgressionText`). `artifactId` `y8k-progression` e pacotes `net.y8k.*` renomeados para `y4k-progression` e `net.y4k.*` em 2026-10-28. |
| `mvnw`, `mvnw.cmd`, `.mvn/` | Maven Wrapper do monorepo | Maven 3.9.11. |
| `scripts/ci`, `scripts/check-java`, `.github/workflows/ci.yml` | Novo | Preflight `check-java` que exige JDK 25 (respeita `JAVA_HOME`, testado em `scripts/test_check_java.py`), `clean verify` e recusa módulo sem testes, com falha, erro ou skip. Em tag `v*` a CI publica com `./mvnw deploy`. |

A versão dos artefatos acompanha `config.ini` da raiz `y4kv2`. Os repositórios de papel herdam o pai com `<relativePath/>` vazio e o resolvem pelo GitHub Packages (`https://maven.pkg.github.com/ramosbezzu/y4kv2-commons`, id `github`). Localmente, `./scripts/ci clean install` instala o pai e os módulos em `~/.m2`.

### Adiado para a fase F1

- Imagem `ghcr.io/ramosbezzu/y4kv2-paper-base` e `versions.lock.json`.
- `infra/ranks.json`, `scripts/y8k_lib/ranks.py`, `infra/templates/{paper-global,lpcf,progression}.yml` e `assets/branding`: continuam na raiz `y4kv2`, porque a CLI instala o runtime atual de lá e os docs da raiz são a fonte de verdade.

## Regras

Sistema, contratos de imagem e fluxo de promoção: [projeto raiz y4kv2](https://github.com/ramosbezzu/y4kv2) e seu `AGENTS.md`.
