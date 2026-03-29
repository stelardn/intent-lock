# IntentLock Agent Instructions

- Always treat this repository as a consumer-facing app for Brazilian Portuguese users.
- PT-BR is the current source language for product copy until localization is introduced.
- All visible UI text must currently use correct PT-BR spelling, including proper accents.
- Never replace correct words such as `Permissões`, `Início`, `está`, `não`, `válida`, `configuração`, `intenção`, and `créditos` with unaccented variants.
- When editing microcopy, review titles, subtitles, labels, states, CTAs, error messages, validations, and empty states.
- Write new UI copy in a localization-ready way and avoid assumptions that would make future multilingual support harder.
- In card and section headers, prioritize text legibility before preserving actions on the same line.
- If a button or CTA in a header starts to squeeze the text, stack the action below the text block on narrow screens.
- Do not accept layouts that break important words badly, such as `Acessibilidade` or `Permissões`, because of avoidable horizontal compression.
- In UI changes, preserve responsiveness on small screens and validate headers, long titles, and chips carefully.
- When creating commits in this repository, use Conventional Commits messages.

## Project Context For Agents

- Before larger changes, read `docs/agent-context/README.md`.
- Open only the documents needed for the task:
- `docs/agent-context/product.md`
- `docs/agent-context/architecture.md`
- `docs/agent-context/runtime.md`
- `docs/agent-context/engineering.md`
- `docs/agent-context/testing.md`

## Product Invariants

- The current default UI language is PT-BR, even when technical documentation is in English.
- Future multilingual support should preserve correct PT-BR defaults and translations.
- Interception, challenge progress, and temporary unlocks must remain aligned with the current saved rule.
- Invalid, disabled, stale, or under-permissioned rules must fail safely.
