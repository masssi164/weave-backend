# Runtime configuration

This document is the backend runtime reference for Release 1 operators and local integration runs. The top-level README keeps the product boundary short; this file keeps the full environment-variable contract in one place.

## Required runtime variable

- `WEAVE_OIDC_ISSUER_URI`: public issuer URI for the Keycloak realm used by Weave. Canonical local/dev value: `https://auth.weave.local/realms/weave`.

## Optional platform and auth variables

- `WEAVE_OIDC_JWK_SET_URI`: internal JWKS URL for backend key discovery when it differs from the public issuer metadata route.
- `WEAVE_OIDC_REQUIRED_AUDIENCE`: audience required in access tokens, defaults to `weave-app`.
- `WEAVE_CLIENT_ID`: first-party Weave app client ID required in `azp` and/or `client_id`, defaults to `weave-app`.
- `WEAVE_PUBLIC_BASE_URL`: public product entrypoint, defaults to `https://weave.local`.
- `WEAVE_API_BASE_URL`: public backend API base URL, defaults to `https://api.weave.local/api`.
- `WEAVE_AUTH_BASE_URL`: public Keycloak base URL, defaults to `https://auth.weave.local`.
- `WEAVE_MATRIX_HOMESERVER_URL`: public Matrix homeserver URL, defaults to `https://matrix.weave.local`.
- `WEAVE_FILES_PRODUCT_URL`: public files product surface, defaults to `https://weave.local/files`.
- `WEAVE_CALENDAR_PRODUCT_URL`: public calendar product surface, defaults to `https://weave.local/calendar`.
- `WEAVE_NEXTCLOUD_BASE_URL`: canonical raw Nextcloud technical/admin/protocol URL, defaults to `https://files.weave.local`.
- `WEAVE_TARGET_MOBILE`: advertise mobile as a supported client target, defaults to `true`.
- `WEAVE_TARGET_DESKTOP`: advertise desktop as a supported client target, defaults to `true`.
- `WEAVE_TARGET_WEB`: advertise web as a supported client target, defaults to `false`.
- `PORT`: HTTP listen port, defaults to `8080`.

## Workspace capability variables

- `WEAVE_WORKSPACE_SHELL_ACCESS_ENABLED`: enable the authenticated shell contract, defaults to `true`.
- `WEAVE_WORKSPACE_CHAT_ENABLED`: enable chat in the workspace snapshot, defaults to `true`.
- `WEAVE_WORKSPACE_CHAT_READINESS`: optional explicit chat readiness override (`ready`, `degraded`, `blocked`, `unavailable`).
- `WEAVE_WORKSPACE_FILES_ENABLED`: enable files in the workspace snapshot, defaults to `true`.
- `WEAVE_WORKSPACE_FILES_READINESS`: optional explicit files readiness override (`ready`, `degraded`, `blocked`, `unavailable`).
- `WEAVE_WORKSPACE_CALENDAR_ENABLED`: enable the calendar capability, defaults to `false`.
- `WEAVE_WORKSPACE_CALENDAR_READINESS`: optional explicit calendar readiness override (`ready`, `degraded`, `blocked`, `unavailable`).
- `WEAVE_WORKSPACE_BOARDS_ENABLED`: enable the boards capability, defaults to `false`.
- `WEAVE_WORKSPACE_BOARDS_READINESS`: optional explicit boards readiness override (`ready`, `degraded`, `blocked`, `unavailable`).

Capability readiness is intentionally conservative:

- `shellAccess` is `unavailable` when disabled, otherwise `ready` only when JWT validation can be enforced with issuer, audience, and client contract.
- `chat` follows `WEAVE_WORKSPACE_CHAT_READINESS` when set; otherwise it is `ready` when `WEAVE_MATRIX_HOMESERVER_URL` is configured, `degraded` without that route, and `blocked` if shell access is blocked.
- `files` follows `WEAVE_WORKSPACE_FILES_READINESS` when set; otherwise it is `ready` when `WEAVE_NEXTCLOUD_BASE_URL` is configured, `degraded` without that route, and `blocked` if shell access is blocked.
- `calendar` and `boards` are stable contract slots. They are `unavailable` when disabled and may advertise rollout state through explicit readiness overrides.

## Files facade and Nextcloud WebDAV adapter

The app never sends raw Nextcloud credentials to this backend. Files operations use a backend-owned actor for WebDAV calls.

- `WEAVE_NEXTCLOUD_FILES_ACTOR_MODEL`: backend-to-Nextcloud token model, currently only `backend-service-account`; other values fail closed until implemented.
- `WEAVE_NEXTCLOUD_FILES_ACTOR_USERNAME`: backend-owned Nextcloud actor username for WebDAV calls. Blank keeps the facade unavailable.
- `WEAVE_NEXTCLOUD_FILES_ACTOR_TOKEN`: backend-owned Nextcloud app password/token for WebDAV calls. Blank keeps the facade unavailable.
- `WEAVE_NEXTCLOUD_FILES_APP_PASSWORD`: compatibility alias used when `WEAVE_NEXTCLOUD_FILES_ACTOR_TOKEN` is blank.
- `WEAVE_NEXTCLOUD_FILES_WEBDAV_ROOT_PATH`: Nextcloud WebDAV files root path, defaults to `/remote.php/dav/files`.

If the actor model, username, or token is missing, files endpoints fail closed with `nextcloud-adapter-not-configured`. Implemented Release 1 WebDAV operations are folder listing with quota when returned by Nextcloud, folder creation, upload, download, and delete. Move/share remain unsupported until product policy and endpoint contracts are specified.

## Calendar facade and CalDAV adapter

Calendar product operations stay on `/api`; this backend is the only component that talks to Nextcloud CalDAV.

- `WEAVE_CALDAV_BASE_URL`: Nextcloud origin used by the backend CalDAV adapter, defaults to `WEAVE_NEXTCLOUD_BASE_URL` or `https://files.weave.local`.
- `WEAVE_CALDAV_CALENDAR_PATH_TEMPLATE`: CalDAV calendar collection template, defaults to `/remote.php/dav/calendars/{user}/personal/`; `{user}` is derived from authenticated token `preferred_username`, falling back to `sub`.
- `WEAVE_CALDAV_AUTH_MODE`: backend actor credential mode (`BASIC` or `BEARER`), defaults to `BASIC`.
- `WEAVE_CALDAV_BACKEND_USERNAME`: backend actor username for Basic auth; required with `BASIC`.
- `WEAVE_CALDAV_BACKEND_TOKEN`: backend actor app password/token or bearer token; required for the CalDAV adapter to call Nextcloud.
- `WEAVE_CALDAV_REQUEST_TIMEOUT_SECONDS`: CalDAV request timeout, defaults to `10`.

When required actor credentials are missing, calendar operations fail closed with `nextcloud-adapter-not-configured`. Recurrence creation, editing, and expansion are deferred: the current DTO has no RRULE contract, and the adapter does not expose raw recurrence fields. Recurring events returned by CalDAV may appear as their source VEVENT only until a later product/API spec defines full recurrence UX.

## Profile and onboarding variables

- `WEAVE_PROFILE_STORAGE_PATH`: durable JSON file path for mutable `PATCH /api/profile` overrides, defaults to `./data/profile-overrides.json`.
- `WEAVE_ONBOARDING_MATRIX_PROVISIONING_STATE`: optional first-run Matrix provisioning override (`not_configured`, `pending`, `ready`, `degraded`, `failed`); blank derives status from chat capability.
- `WEAVE_ONBOARDING_NEXTCLOUD_PROVISIONING_STATE`: optional first-run Nextcloud provisioning override (`not_configured`, `pending`, `ready`, `degraded`, `failed`); blank derives status from files/calendar capability and Nextcloud route configuration.

Profile facade endpoints are protected by the same first-party bearer-token contract as `/api/me`. `PATCH /api/profile` accepts partial updates for `displayName`, `avatar`, `locale`, `timezone`, `accessibilityPreferences`, and `profileVisibility`. Set `WEAVE_PROFILE_STORAGE_PATH` to mounted durable storage for containerized Release 1 runs.

Onboarding status returns identity, roles, groups, invite status, profile completeness, and module provisioning states. Downstream states must remain frontend-safe and must not expose stack traces, tokens, secrets, or raw service errors.
