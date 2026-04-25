# Weave Backend

`weave-backend` is the Spring Boot backend for the Weave product family.

The backend should act as a product API and orchestration layer, not as a blind proxy for every end-user call to Matrix, Nextcloud, or Keycloak. The Flutter client already has native-first flows for OIDC sign-in, Matrix OAuth, and Nextcloud login/app-password handling. This repository should own the server-side workflows that actually benefit from a backend:

- validating access tokens from Weave clients
- exposing product-specific REST APIs
- orchestrating server-owned workflows across Keycloak, Matrix, and Nextcloud
- running automation, provisioning, and background jobs

## Current baseline

This repository now starts as a JWT-protected Spring Boot API with:

- `/api/health/live` and `/api/health/ready` endpoints for gateway and smoke checks
- `/api/platform/config` and `/api/platform/status` endpoints for client bootstrap and diagnostics
- a canonical `/api/me` endpoint for profile claim inspection and client/backend contract testing
- a compatibility `/api/v1/me` endpoint retained during the transition
- a `/api/v1/workspace/capabilities` endpoint for the first backend-owned client contract
- a `/api/v1/workspace/release-readiness` endpoint for operator-facing Release 1 setup status and remaining actions
- OpenAPI JSON published at `/v3/api-docs`
- actuator health and info endpoints
- first-party JWT issuer, audience, client, and workspace-scope validation
- Gradle wrapper and GitHub Actions CI
- issue-ready alignment drafts for `weave`, `weave-inf`, and `weave-backend`

## Non-goals

The backend should not, by default:

- replace Matrix Native OAuth 2.0 with a custom server-side login proxy
- assume a mobile OIDC bearer token can be reused as a Matrix access token
- assume a Nextcloud bearer token replaces Nextcloud Login Flow v2 or app passwords for all user workflows

## Configuration

Required runtime variables:

- `WEAVE_OIDC_ISSUER_URI`: public issuer URI for the Keycloak realm used by Weave

Optional runtime variables:

- `WEAVE_OIDC_JWK_SET_URI`: internal JWKS URL for backend key discovery when it differs from the public issuer metadata route
- `WEAVE_OIDC_REQUIRED_AUDIENCE`: audience required in access tokens, defaults to `weave-app`
- `WEAVE_CLIENT_ID`: first-party Weave app client ID required in `azp` and/or `client_id`, defaults to `weave-app`
- `WEAVE_WORKSPACE_SHELL_ACCESS_ENABLED`: enable the authenticated shell contract, defaults to `true`
- `WEAVE_WORKSPACE_CHAT_ENABLED`: enable chat in the workspace snapshot, defaults to `true`
- `WEAVE_MATRIX_HOMESERVER_URL`: public Matrix base URL used by chat auto-readiness
- `WEAVE_WORKSPACE_CHAT_READINESS`: optional explicit chat readiness override (`ready`, `degraded`, `blocked`, `unavailable`)
- `WEAVE_WORKSPACE_FILES_ENABLED`: enable files in the workspace snapshot, defaults to `true`
- `WEAVE_NEXTCLOUD_BASE_URL`: public Nextcloud base URL used by files auto-readiness
- `WEAVE_WORKSPACE_FILES_READINESS`: optional explicit files readiness override (`ready`, `degraded`, `blocked`, `unavailable`)
- `WEAVE_WORKSPACE_CALENDAR_ENABLED`: enable the calendar capability, defaults to `false`
- `WEAVE_WORKSPACE_CALENDAR_READINESS`: optional explicit calendar readiness override (`ready`, `degraded`, `blocked`, `unavailable`)
- `WEAVE_WORKSPACE_BOARDS_ENABLED`: enable the boards capability, defaults to `false`
- `WEAVE_WORKSPACE_BOARDS_READINESS`: optional explicit boards readiness override (`ready`, `degraded`, `blocked`, `unavailable`)
- `WEAVE_PUBLIC_BASE_URL`: public product entrypoint, defaults to `https://weave.local`
- `WEAVE_API_BASE_URL`: public backend API base URL, defaults to `https://weave.local/api`
- `WEAVE_AUTH_BASE_URL`: public Keycloak base URL, defaults to `https://auth.weave.local`
- `WEAVE_MATRIX_BASE_URL`: public Matrix base URL, defaults to `https://matrix.weave.local`
- `WEAVE_FILES_PRODUCT_URL`: public files product surface, defaults to `https://weave.local/files`
- `WEAVE_CALENDAR_PRODUCT_URL`: public calendar product surface, defaults to `https://weave.local/calendar`
- `WEAVE_NEXTCLOUD_RAW_BASE_URL`: canonical Nextcloud URL, defaults to `https://files.weave.local`
- `PORT`: HTTP port, defaults to `8080`

Workspace capability source of truth:

- `shellAccess` is `unavailable` when disabled, otherwise `ready` only when JWT validation can be enforced with a configured issuer, audience, and first-party client contract.
- `chat` is configuration-backed. When enabled it follows `WEAVE_WORKSPACE_CHAT_READINESS` if set, otherwise it is `ready` when `WEAVE_MATRIX_HOMESERVER_URL` is configured, `degraded` without that route, and `blocked` if the shell contract itself is blocked.
- `files` is configuration-backed. When enabled it follows `WEAVE_WORKSPACE_FILES_READINESS` if set, otherwise it is `ready` when `WEAVE_NEXTCLOUD_BASE_URL` is configured, `degraded` without that route, and `blocked` if the shell contract itself is blocked.
- `calendar` and `boards` stay contract-stable. They are `unavailable` when disabled, and can intentionally advertise another readiness via their explicit override variables when the workspace wants to surface rollout state.

See [docs/release-operations.md](docs/release-operations.md) for the Release 1 runtime contract, stable error envelope, and minimum operator checks.

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
