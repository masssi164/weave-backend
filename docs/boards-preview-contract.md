# Boards/Tasks preview contract

Boards/Tasks is a provider-neutral, post-Release-1 preview slice. It is **not** a Release 1 product surface, has no published backend routes, and must remain hidden/feature-flagged until a later promotion spec defines API routes, auth scopes, DTO compatibility, OpenAPI publication, runtime provider setup, smoke/E2E, and accessibility gates.

This document is intentionally separate from Release 1 screenshots and product-surface documentation. Do not add Boards/Tasks screenshots to the main Product screenshots section unless the module is fully navigable and release-enabled by a later spec.

## Scope in this backend slice

Implemented now:

- Provider-neutral Java domain records for projects, boards, columns, tasks, labels, comments, attachments, provider refs, capabilities, and normalized events.
- A repository port that hides provider pagination, IDs, vocabulary, and raw errors from callers.
- A support-safe error vocabulary aligned with the workspace spec.
- A Vikunja HTTP/status error mapper that converts provider failures into Weave codes without leaking raw provider messages, URLs, or tokens.
- A no-op event publisher boundary plus preview JSON/OpenAPI schema artifacts under `src/main/resources/contracts/`.
- A first Vikunja adapter boundary and mapper that translates Vikunja projects/buckets/tasks into Weave concepts.
- A fail-closed Vikunja repository placeholder that advertises preview capabilities but does not perform runtime HTTP calls.

Not implemented now:

- No Spring MVC controller routes.
- No Release 1 navigation or product enablement.
- No provider runtime configuration, secrets, Caddy routes, or smoke checks.
- No user-facing screenshots.
- No notifications, audit streams, webhooks, or automation consumers.

## Provider-neutral model

The Weave model follows `../specs/14-boards-tasks-domain-and-provider-adapter-contract.md`:

- `WeaveProject` maps to a provider project/workspace grouping.
- `Board` is the Weave-owned board concept.
- `BoardColumn` maps provider lists/buckets/statuses into `not_started`, `in_progress`, `blocked`, `done`, or `archived`.
- `TaskItem` is the provider-neutral task/card shape consumed by future API/UI work.
- `ProviderRef` preserves provider IDs, URLs, etags, versions, and sync timestamps for diagnostics, export, migration, and conflict handling without making those references the primary product identity.

## Vikunja first adapter boundary

Vikunja is the first strategic adapter boundary because it is lightweight, self-hostable, and API-oriented. The current mapper translates:

- Vikunja project → Weave project and board
- Vikunja bucket → Weave board column
- Vikunja task → Weave task item

The repository placeholder fails closed with `provider_unavailable` until a promotion spec defines authentication, HTTP client behavior, persistence/sync expectations, route DTOs, and validation gates. The adapter boundary now also contains the support-safe HTTP/status mapping expected once real Vikunja calls are introduced: unauthorized, forbidden, not found, conflict, validation, rate limit, offline, provider unavailable, and unknown failures are normalized before reaching product/API code.

## Contract artifacts

- `src/main/resources/contracts/boards-preview.openapi.yaml` contains preview schema components only and intentionally has `paths: {}`.
- `src/main/resources/contracts/task-board-event.schema.json` contains the preview normalized event envelope.

These artifacts are open contract drafts for implementation alignment; they are not a published Release 1 API surface.
