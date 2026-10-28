# y4kv2-commons: Base compartilhada do Y4Kv2

Tudo que mais de um servidor usa e que muda junto com a versão do Paper.

## O que este repositório faz

- **POM pai** (`net.y4k:y4kv2-parent`): JDK 25, versão fixada da Paper API, JUnit 5, AssertJ e MockBukkit. Todo plugin Java do Y4Kv2 herda dele.
- **`y4k-core`**: marca Y4K, cores, mensagens e idiomas PT/EN/ES, cargos gregos (Theta/Mu/Pi/Sigma, Tau/Delta/Omega) e utilitários sem regra de modo.
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

`pom.xml` (pai), `tests/paper`, `plugins/progression`, `infra/ranks.json`, `scripts/y8k_lib/ranks.py`, `infra/templates/{paper-global,lpcf,progression}.yml`, `assets/branding`.

## Regras

Sistema, contratos de imagem e fluxo de promoção: [projeto raiz y4kv2](https://github.com/ramosbezzu/y4kv2) e seu `AGENTS.md`.
