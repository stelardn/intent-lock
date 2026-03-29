# Agent Context Index

This folder contains project context intended for coding agents working on Intent Lock.

Read this index first, then open only the documents needed for the task at hand.

## Recommended reading order

1. `product.md`
2. `architecture.md`
3. `runtime.md`
4. `engineering.md`
5. `testing.md`

## Documents

- `product.md`
  - current product shape, scope, constraints, and implementation status
- `architecture.md`
  - main modules, source-of-truth files, persistence model, and key collaborators
- `runtime.md`
  - save, interception, challenge, and unlock flows
- `engineering.md`
  - technical facts, build outputs, guardrails for safe changes, and forward-looking notes
- `testing.md`
  - regression priorities and validation guidance

## Quick summary

Intent Lock is a rule-driven Android intervention app. The most important invariant is that interception, challenge progress, and temporary unlocks must stay aligned to the current saved rule and must fail safely when rules become invalid, stale, disabled, or under-permissioned.
