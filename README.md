# Weave Backend

`weave-backend` is the Spring Boot backend for the Weave product family.

The backend should act as a product API and orchestration layer, not as a blind proxy for every end-user call to Matrix, Nextcloud, or Keycloak. Flutter may own native OIDC/PKCE sign-in and Matrix client protocol flows, but MVP product Files and Calendar flows go through this backend at the canonical `/api` surface instead of direct Flutter-to-Nextcloud WebDAV/OCS/CalDAV calls. This repository should own the server-side workflows that actually benefit from a backend:

- validating access tokens from Weave clients
- exposing product-specific REST APIs and normalized errors
- exposing Files and Calendar facade contracts, with Files backed by a fail-closed Nextcloud WebDAV adapter when backend-owned actor credentials are configured
- orchestrating server-owned workflows across Keycloak, Matrix, and Nextcloud
- running automation, provisioning, and background jobs

## Current baseline

This repository now starts as a JWT-protected Spring Boot API with:

- `/api/health/live` and `/api/health/ready` endpoints for gateway and smoke checks
- `/api/platform/config` and `/api/platform/status` endpoints for client bootstrap and diagnostics, including canonical `matrixHomeserverUrl` and `nextcloudBaseUrl` fields
- a canonical `/api/me` endpoint for profile claim inspection and client/backend contract testing
- a `/api/v1/workspace/capabilities` endpoint for the first backend-owned client contract
- a `/api/v1/workspace/release-readiness` endpoint for operator-facing Release 1 setup status and remaining actions
- authenticated Files facade endpoints at `/api/files`, `/api/files/upload`, `/api/files/folders`, `/api/files/{id}/download`, and `/api/files/{id}` backed by Nextcloud WebDAV when configured
- authenticated Calendar facade endpoints at `/api/calendar/events` and `/api/calendar/events/{id}`
- OpenAPI JSON published at `/v3/api-docs`
- actuator health and info endpoints
- first-party JWT issuer, audience, client, and workspace-scope validation
- Gradle wrapper and GitHub Actions CI
- issue-ready alignment drafts for `weave`, `weave-infra`, and `weave-backend`

## Non-goals

The backend should not, by default:

- replace Matrix Native OAuth 2.0 with a custom server-side login proxy
- assume a mobile OIDC bearer token can be reused as a Matrix access token
- make direct Flutter-to-Nextcloud WebDAV/OCS/CalDAV calls the default MVP Files or Calendar product contract

## Configuration

Required runtime variables:

- `WEAVE_OIDC_ISSUER_URI`: public issuer URI for the Keycloak realm used by Weave; canonical local/dev value is `https://auth.weave.local/realms/weave`

Optional runtime variables:

- `WEAVE_OIDC_JWK_SET_URI`: internal JWKS URL for backend key discovery when it differs from the public issuer metadata route
- `WEAVE_OIDC_REQUIRED_AUDIENCE`: audience required in access tokens, defaults to `weave-app`
- `WEAVE_CLIENT_ID`: first-party Weave app client ID required in `azp` and/or `client_id`, defaults to `weave-app`
- `WEAVE_WORKSPACE_SHELL_ACCESS_ENABLED`: enable the authenticated shell contract, defaults to `true`
- `WEAVE_WORKSPACE_CHAT_ENABLED`: enable chat in the workspace snapshot, defaults to `true`
- `WEAVE_MATRIX_HOMESERVER_URL`: public Matrix base URL used by chat auto-readiness
- `WEAVE_WORKSPACE_CHAT_READINESS`: optional explicit chat readiness override (`ready`, `degraded`, `blocked`, `unavailable`)
- `WEAVE_WORKSPACE_FILES_ENABLED`: enable files in the workspace snapshot, defaults to `true`
- `WEAVE_NEXTCLOUD_BASE_URL`: canonical Nextcloud URL used by files auto-readiness and the backend Files adapter
- `WEAVE_WORKSPACE_FILES_READINESS`: optional explicit files readiness override (`ready`, `degraded`, `blocked`, `unavailable`)
- `WEAVE_WORKSPACE_CALENDAR_ENABLED`: enable the calendar capability, defaults to `false`
- `WEAVE_WORKSPACE_CALENDAR_READINESS`: optional explicit calendar readiness override (`ready`, `degraded`, `blocked`, `unavailable`)
- `WEAVE_CALDAV_BASE_URL`: Nextcloud origin used only by the backend CalDAV adapter, defaults to `WEAVE_NEXTCLOUD_BASE_URL` / `https://files.weave.local`
- `WEAVE_CALDAV_CALENDAR_PATH_TEMPLATE`: CalDAV calendar collection template, defaults to `/remote.php/dav/calendars/{user}/personal/`; `{user}` is derived from the authenticated Weave token `preferred_username` claim, falling back to `sub`
- `WEAVE_CALDAV_AUTH_MODE`: backend actor credential mode (`BASIC` or `BEARER`), defaults to `BASIC`
- `WEAVE_CALDAV_BACKEND_USERNAME`: backend actor username for Basic auth; required with `BASIC`
- `WEAVE_CALDAV_BACKEND_TOKEN`: backend actor app password/token or bearer token; required for the CalDAV adapter to call Nextcloud
- `WEAVE_CALDAV_REQUEST_TIMEOUT_SECONDS`: CalDAV request timeout, defaults to `10`
- `WEAVE_WORKSPACE_BOARDS_ENABLED`: enable the boards capability, defaults to `false`
- `WEAVE_WORKSPACE_BOARDS_READINESS`: optional explicit boards readiness override (`ready`, `degraded`, `blocked`, `unavailable`)
- `WEAVE_PUBLIC_BASE_URL`: public product entrypoint, defaults to `https://weave.local`
- `WEAVE_API_BASE_URL`: public backend API base URL, defaults to `https://api.weave.local/api`
- `WEAVE_AUTH_BASE_URL`: public Keycloak base URL, defaults to `https://auth.weave.local`
- `WEAVE_MATRIX_HOMESERVER_URL`: public Matrix homeserver URL, defaults to `https://matrix.weave.local`
- `WEAVE_FILES_PRODUCT_URL`: public files product surface, defaults to `https://weave.local/files`
- `WEAVE_CALENDAR_PRODUCT_URL`: public calendar product surface, defaults to `https://weave.local/calendar`
- `WEAVE_NEXTCLOUD_BASE_URL`: canonical Nextcloud URL, defaults to `https://files.weave.local`
- `WEAVE_NEXTCLOUD_FILES_ACTOR_MODEL`: backend-to-Nextcloud token model, currently only `backend-service-account`; other values fail closed until implemented
- `WEAVE_NEXTCLOUD_FILES_ACTOR_USERNAME`: backend-owned Nextcloud actor username for WebDAV calls; blank keeps the facade unavailable instead of forwarding caller tokens
- `WEAVE_NEXTCLOUD_FILES_ACTOR_TOKEN`: backend-owned Nextcloud app password/token for WebDAV calls; blank keeps the facade unavailable
- `WEAVE_NEXTCLOUD_FILES_APP_PASSWORD`: compatibility alias used when `WEAVE_NEXTCLOUD_FILES_ACTOR_TOKEN` is blank
- `WEAVE_NEXTCLOUD_FILES_WEBDAV_ROOT_PATH`: Nextcloud WebDAV files root path, defaults to `/remote.php/dav/files`
- `PORT`: HTTP port, defaults to `8080`

Files adapter behavior:

- The app never receives raw Nextcloud credentials. The backend uses the configured backend-owned actor for WebDAV calls.
- If the actor model, username, or token is missing, files endpoints fail closed with `nextcloud-adapter-not-configured`.
- Implemented WebDAV operations: folder listing with quota when returned by Nextcloud, folder creation, upload, download, and delete.
- Move/share remain intentionally unsupported until product policy and endpoint contracts are specified.
- No live Nextcloud integration is implied by unit tests; local/live validation still requires a configured `files.weave.local` and backend actor.

Workspace capability source of truth:

- `shellAccess` is `unavailable` when disabled, otherwise `ready` only when JWT validation can be enforced with a configured issuer, audience, and first-party client contract.
- `chat` is configuration-backed. When enabled it follows `WEAVE_WORKSPACE_CHAT_READINESS` if set, otherwise it is `ready` when `WEAVE_MATRIX_HOMESERVER_URL` is configured, `degraded` without that route, and `blocked` if the shell contract itself is blocked.
- `files` is configuration-backed. When enabled it follows `WEAVE_WORKSPACE_FILES_READINESS` if set, otherwise it is `ready` when `WEAVE_NEXTCLOUD_BASE_URL` is configured, `degraded` without that route, and `blocked` if the shell contract itself is blocked.
- `calendar` and `boards` stay contract-stable. They are `unavailable` when disabled, and can intentionally advertise another readiness via their explicit override variables when the workspace wants to surface rollout state.

Calendar facade adapter scope:

- `/api/calendar/events` remains the product API. The backend maps list/create/update/delete operations to Nextcloud CalDAV and normalizes CalDAV failures into Weave error codes.
- The MVP adapter requires an explicitly configured backend actor credential. If `WEAVE_CALDAV_BACKEND_TOKEN` or the Basic username is missing, calendar operations fail closed with `nextcloud-adapter-not-configured` instead of leaking raw CalDAV behavior to clients.
- Recurrence creation/editing/expansion is intentionally deferred: the current DTO has no RRULE contract, and the adapter does not expose raw recurrence fields. Recurring events returned by CalDAV may appear as their source VEVENT only; full recurrence UX and expansion need a later product/API spec.

See [docs/release-operations.md](docs/release-operations.md) for the Release 1 runtime contract, stable error envelope, and minimum operator checks.

Canonical local/dev public contract:

- Backend API base: `https://api.weave.local/api`.
- Keycloak issuer: `https://auth.weave.local/realms/weave`.
- Product shell: `https://weave.local`.
- Matrix homeserver: `https://matrix.weave.local`.
- Weave files/calendar product surfaces: `https://weave.local/files` and `https://weave.local/calendar`.
- Canonical Nextcloud origin: `https://files.weave.local`.

Local first-party token contract:

- `iss` must match `WEAVE_OIDC_ISSUER_URI`.
- `aud` must include `weave-app` unless `WEAVE_OIDC_REQUIRED_AUDIENCE` overrides it.
- `azp` and/or `client_id` must be present and must match `weave-app` unless `WEAVE_CLIENT_ID` overrides it.
- `scope` must include `weave:workspace` for protected `/api/**` routes. Public platform, liveness, and readiness endpoints are unauthenticated.

## Local validation

If Java 17 is installed locally:

```bash
./gradlew test
```

If Java is not installed locally, the same command can be run in Docker:

```bash
docker run --rm \
  -u "$(id -u):$(id -g)" \
  -e HOME=/tmp \
  -e GRADLE_USER_HOME=/tmp/.gradle \
  -v "$PWD:/workspace" \
  -w /workspace \
  eclipse-temurin:17-jdk \
  ./gradlew test
```

To build the local backend image used by `weave-infra` integration runs:

```bash
docker build -t weave-backend:e2e .
```

This Dockerfile-based path is the reproducible local image build for Apple Silicon and other non-x86 hosts.

## Release-grade API behavior

- Protected `/api/**` routes return structured JSON for `401` and `403` responses.
- `401` means the bearer token is missing or fails the first-party Weave token contract.
- `403` means the caller is authenticated but lacks the `weave:workspace` scope.
- The stable error envelope is `code`, `message`, `details`, and `requestId`; the response also includes `X-Request-Id`.
- `/v3/api-docs` publishes the same error schema so app and operator tooling can rely on it.

## Architecture alignment

See [docs/architecture-alignment.md](docs/architecture-alignment.md) and the issue drafts under [docs/issues](docs/issues).
