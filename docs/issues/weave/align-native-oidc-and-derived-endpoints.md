# Align native OIDC registration and derived endpoints

## Problem

`weave` currently expects:

- OIDC redirect: `com.massimotter.weave:/oauthredirect`
- OIDC post-logout redirect: `com.massimotter.weave:/logout`
- derived raw Nextcloud fallback URL: `https://files.<base-domain>`
- derived backend API URL: `https://<base-domain>/api`

Older `weave-inf` setup registered `weaveapp://login/callback` for the Keycloak client and older app defaults derived service-specific hosts that no longer match the final product topology.

## Proposal

- Align the client-side constants and onboarding/help copy with the final infrastructure contract
- Prefer `files.<base-domain>` for the raw Nextcloud fallback and `<base-domain>/api` for the product API route
- Make the redirect URI contract explicit in app docs so infra changes do not silently break native sign-in

## Acceptance criteria

- `weave` and `weave-inf` agree on one redirect URI set for the native app
- `weave` and `weave-inf` agree on one browser-facing Nextcloud hostname convention
- onboarding/help text reflects the final redirect and logout URIs
- app configuration docs mention the backend base URL separately instead of overloading the current issuer-derived defaults
