# AGENTS.md: y4kv2-commons

Siga integralmente o `AGENTS.md` do projeto raiz [`y4kv2`](https://github.com/ramosbezzu/y4kv2/blob/main/AGENTS.md): fronteiras dos repositórios, contrato de imagem, versionamento, testes e evidência.

## Regras locais

- Nada aqui pode conhecer regras de um modo específico. Mudança no POM pai ou na imagem base afeta todos os papéis: anotar no CHANGELOG e avisar na promoção da raiz.
- CI local obrigatória antes de commit: `./scripts/ci`.
- Mudou a imagem: registre versão e digest e abra a promoção em `stack.lock.json` na raiz.
