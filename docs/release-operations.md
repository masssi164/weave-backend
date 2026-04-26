# Release 1 API operations guide

This backend is intentionally small for Release 1, but operators still need a clear runtime contract.

## Runtime inputs

Required:

- `WEAVE_OIDC_ISSUER_URI`: public issuer URI for the Keycloak realm used by Weave; canonical local/dev value is `https://auth.weave.local/realms/weave`

Optional:

- `WEAVE_OIDC_JWK_SET_URI`: internal JWKS URL when the backend cannot use the issuer metadata endpoint directly
- `WEAVE_OIDC_REQUIRED_AUDIENCE`: required audience in Weave access tokens, defaults to `weave-app`
- `WEAVE_CLIENT_ID`: required first-party client identifier in `azp` and/or `client_id`, defaults to `weave-app`
- `WEAVE_PUBLIC_BASE_URL`: public product entrypoint, defaults to `https://weave.local`
- `WEAVE_API_BASE_URL`: public backend API base URL, defaults to `https://api.weave.local/api`
- `WEAVE_AUTH_BASE_URL`: public Keycloak base URL, defaults to `https://auth.weave.local`
- `WEAVE_MATRIX_HOMESERVER_URL`: public Matrix homeserver URL, defaults to `https://matrix.weave.local` (`WEAVE_MATRIX_BASE_URL` remains a compatibility alias)
- `WEAVE_FILES_PRODUCT_URL`: public files product surface, defaults to `https://weave.local/files`
- `WEAVE_CALENDAR_PRODUCT_URL`: public calendar product surface, defaults to `https://weave.local/calendar`
- `WEAVE_NEXTCLOUD_BASE_URL`: canonical Nextcloud URL, defaults to `https://files.weave.local` (`WEAVE_NEXTCLOUD_RAW_BASE_URL` remains a compatibility alias)
- `PORT`: HTTP listen port, defaults to `8080`

## Local/dev public contract

The backend's canonical local/dev API base is `https://api.weave.local/api`. The product shell remains `https://weave.local`; `https://weave.local/api` is compatibility-only when `weave-infra` explicitly routes it and must behave identically to the canonical API base.

The canonical local/dev Keycloak issuer is `https://auth.weave.local/realms/weave`. Keep any private JWKS route in `WEAVE_OIDC_JWK_SET_URI`; do not replace token issuer validation with an internal service URL.

## Protected API behavior

Protected `/api/**` routes require a bearer token that satisfies the first-party Weave contract:

- `iss` matches `WEAVE_OIDC_ISSUER_URI`
- `aud` includes `WEAVE_OIDC_REQUIRED_AUDIENCE`
- `azp` and/or `client_id` matches `WEAVE_CLIENT_ID`
- `scope` includes `weave:workspace`

`/api/health/live`, `/api/health/ready`, `/api/platform/config`, and `/api/platform/status` are public diagnostics/bootstrap endpoints.

Protected endpoints return a stable JSON error envelope on auth failures and include the same request id in `X-Request-Id`:

```json
{
  "code": "unauthorized",
  "message": "Bearer authentication is required and must satisfy the first-party Weave token contract.",
  "details": {
    "status": 401,
    "path": "/api/v1/workspace/capabilities",
    "error": "Unauthorized"
  },
  "requestId": "01HV..."
}
```

Use `401` for missing or invalid tokens. Use `403` when the token is authenticated but does not include `weave:workspace`.

`/api/v1/workspace/release-readiness` is the backend-owned operator snapshot for Release 1. It rolls auth, Matrix chat, and Nextcloud files into one response and lists the exact remaining setup actions when the workspace is still degraded or blocked.

## Minimum operator checks

- `GET /api/health/ready` should return `200 OK`
- `GET /v3/api-docs` should return the published OpenAPI document
- `GET /api/platform/config` should return public product URLs, `matrixHomeserverUrl`, canonical `nextcloudBaseUrl`, and module flags; legacy `matrixBaseUrl`/`nextcloudRawBaseUrl` remain compatibility aliases
- `GET /api/platform/status` should return module status for smoke and diagnostics
- `GET /api/me` with a valid first-party token should return caller claims
- `GET /api/v1/workspace/capabilities` with a valid first-party token should return the client-facing capability snapshot
- `GET /api/v1/workspace/release-readiness` with a valid first-party token should return operator-facing Release 1 setup status and remaining actions

## Logging and audit baseline

For Release 1, keep at least:

- reverse proxy access logs with request path, status, latency, and request id
- application logs from stdout/stderr collected by the deployment target
- retained auth failure logs long enough to diagnose issuer, audience, or scope drift

Do not log raw bearer tokens.

## Deployment expectations

- Run behind TLS termination
- Keep the backend on an internal/private port when a reverse proxy is present
- Keep issuer and JWKS URLs aligned with the public auth contract exposed to the app
- Treat issuer, JWKS, and client/audience values as release contract, not per-developer convenience settings
