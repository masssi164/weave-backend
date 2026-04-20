# Weave Backend

`weave-backend` is the Spring Boot backend for the Weave product family.

The backend should act as a product API and orchestration layer, not as a blind proxy for every end-user call to Matrix, Nextcloud, or Keycloak. The Flutter client already has native-first flows for OIDC sign-in, Matrix OAuth, and Nextcloud login/app-password handling. This repository should own the server-side workflows that actually benefit from a backend:

- validating access tokens from Weave clients
- exposing product-specific REST APIs
- orchestrating server-owned workflows across Keycloak, Matrix, and Nextcloud
- running automation, provisioning, and background jobs

## Current baseline

This repository now starts as a JWT-protected Spring Boot API with:

- a `/api/v1/me` endpoint for claim inspection and client/backend contract testing
- a `/api/v1/workspace/capabilities` endpoint for the first backend-owned client contract
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
- `PORT`: HTTP port, defaults to `8080`

See [docs/release-operations.md](docs/release-operations.md) for the Release 1 runtime contract, stable error envelope, and minimum operator checks.

Local first-party token contract:

- `iss` must match `WEAVE_OIDC_ISSUER_URI`.
- `aud` must include `weave-app` unless `WEAVE_OIDC_REQUIRED_AUDIENCE` overrides it.
- `azp` and/or `client_id` must be present and must match `weave-app` unless `WEAVE_CLIENT_ID` overrides it.
- `scope` must include `weave:workspace` for `/api/v1/**`.

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

- `/api/v1/**` returns structured JSON for `401` and `403` responses.
- `401` means the bearer token is missing or fails the first-party Weave token contract.
- `403` means the caller is authenticated but lacks the `weave:workspace` scope.
- `/v3/api-docs` publishes the same error schema so app and operator tooling can rely on it.

## Architecture alignment

See [docs/architecture-alignment.md](/Users/flotterotter/code/weave-backend/docs/architecture-alignment.md) and the issue drafts under [docs/issues](/Users/flotterotter/code/weave-backend/docs/issues).
