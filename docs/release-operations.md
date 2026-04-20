# Release 1 API operations guide

This backend is intentionally small for Release 1, but operators still need a clear runtime contract.

## Runtime inputs

Required:

- `WEAVE_OIDC_ISSUER_URI`: public issuer URI for the Keycloak realm used by Weave

Optional:

- `WEAVE_OIDC_JWK_SET_URI`: internal JWKS URL when the backend cannot use the issuer metadata endpoint directly
- `WEAVE_OIDC_REQUIRED_AUDIENCE`: required audience in Weave access tokens, defaults to `weave-app`
- `WEAVE_CLIENT_ID`: required first-party client identifier in `azp` and/or `client_id`, defaults to `weave-app`
- `PORT`: HTTP listen port, defaults to `8080`

## Protected API behavior

All `/api/v1/**` routes require a bearer token that satisfies the first-party Weave contract:

- `iss` matches `WEAVE_OIDC_ISSUER_URI`
- `aud` includes `WEAVE_OIDC_REQUIRED_AUDIENCE`
- `azp` and/or `client_id` matches `WEAVE_CLIENT_ID`
- `scope` includes `weave:workspace`

Protected endpoints return a stable JSON error envelope on auth failures:

```json
{
  "timestamp": "2026-04-20T11:32:15.123Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Bearer authentication is required and must satisfy the first-party Weave token contract.",
  "path": "/api/v1/workspace/capabilities"
}
```

Use `401` for missing or invalid tokens. Use `403` when the token is authenticated but does not include `weave:workspace`.

## Minimum operator checks

- `GET /actuator/health` should return `200 OK`
- `GET /v3/api-docs` should return the published OpenAPI document
- `GET /api/v1/me` with a valid first-party token should return caller claims
- `GET /api/v1/workspace/capabilities` with a valid first-party token should return Release 1 readiness data

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
