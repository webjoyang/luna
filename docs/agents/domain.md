# Domain Docs

How the engineering skills should consume this repo's domain documentation when exploring the codebase.

## Before exploring, read these

This repo uses the **multi-context** layout:

1. **`CONTEXT-MAP.md`** at the repo root: the single entry point. It routes to one `CONTEXT.md` per bounded context and lists cross-context relationships.
2. **`docs/contexts/shared/CONTEXT.md`**: cross-context platform conventions (multi-tenancy, soft delete, frontend/backend module layout, the error-code segment registry). Read it first, regardless of topic.
3. **The domain `CONTEXT.md` file(s) for your topic**, under `docs/contexts/`:
   - `baseinfo` — customer, supplier, product master data
   - `business` — sales contract, shipment
   - `production` — greige/dyeing/yarn/inspection/confirmation/weaving/progress
   - `stock` — stock-in, warehouse/location, stock ledger/balance
4. **`docs/adr/`**: read ADRs that touch the area you're about to work in.

If any of these files don't exist, **proceed silently**. Don't flag their absence; don't suggest creating them upfront. The `/domain-modeling` skill (reached via `/grill-with-docs` and `/improve-codebase-architecture`) creates them lazily when terms or decisions actually get resolved.

## File structure

This repo contains two top-level projects — `luna-srv` (Spring Boot backend) and `luna-ui` (Vue3 frontend) — and each bounded context spans both. Context docs therefore live in a repo-level `docs/contexts/` directory rather than inside either project:

```
/
├── CONTEXT-MAP.md                         ← context index + routing
├── docs/
│   ├── adr/                               ← all ADRs, single global numbering (ADR-xxxx)
│   └── contexts/
│       ├── shared/CONTEXT.md              ← cross-context conventions
│       ├── baseinfo/CONTEXT.md
│       ├── business/CONTEXT.md
│       ├── production/CONTEXT.md
│       └── stock/CONTEXT.md
├── luna-srv/                              ← backend: module lunaerp, packages baseinfo/business/production/stock
└── luna-ui/                               ← frontend
```

ADRs deliberately have **no per-context `adr/` subdirectories**: they are globally numbered in one flat `docs/adr/` because specs and context docs reference them by number across contexts.

## Use the glossary's vocabulary

When your output names a domain concept (in an issue title, a refactor proposal, a hypothesis, a test name), use the term as defined in the relevant context's `CONTEXT.md`. Don't drift to synonyms the glossary explicitly avoids.

If the concept you need isn't in the glossary yet, that's a signal: either you're inventing language the project doesn't use (reconsider) or there's a real gap (note it for `/domain-modeling`).

## Flag ADR conflicts

If your output contradicts an existing ADR, surface it explicitly rather than silently overriding:

> _Contradicts ADR-0007 (event-sourced orders), but worth reopening because…_
