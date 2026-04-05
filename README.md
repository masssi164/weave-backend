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
- actuator health and info endpoints
- optional audience validation for JWTs
- Gradle wrapper and GitHub Actions CI
- issue-ready alignment drafts for `weave`, `weave-inf`, and `weave-backend`

## Non-goals

The backend should not, by default:

- replace Matrix Native OAuth 2.0 with a custom server-side login proxy
- assume a mobile OIDC bearer token can be reused as a Matrix access token
- assume a Nextcloud bearer token replaces Nextcloud Login Flow v2 or app passwords for all user workflows

## Configuration

Required runtime variables:

- `WEAVE_OIDC_ISSUER_URI`: issuer URI for the Keycloak realm used by Weave

Optional runtime variables:

- `WEAVE_OIDC_REQUIRED_AUDIENCE`: audience to require in access tokens once `weave-inf` exposes the final audience/token-exchange contract
- `PORT`: HTTP port, defaults to `8080`

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

## Architecture alignment

See [docs/architecture-alignment.md](/Users/flotterotter/code/weave-backend/docs/architecture-alignment.md) and the issue drafts under [docs/issues](/Users/flotterotter/code/weave-backend/docs/issues).
