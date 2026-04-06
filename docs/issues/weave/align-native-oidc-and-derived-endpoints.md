# Align native OIDC registration and derived endpoints

## Problem

`weave` currently expects:

- OIDC redirect: `com.massimotter.weave:/oauthredirect`
- OIDC post-logout redirect: `com.massimotter.weave:/logout`
- derived Nextcloud URL: `https://nextcloud.<base-domain>`

The current `weave-inf` setup now registers `com.massimotter.weave:/oauthredirect` and `com.massimotter.weave:/logout` for the `weave-app` Keycloak client and exposes Nextcloud on `nextcloud.<tenant_domain>`, so the infrastructure contract now matches the client code.

## Proposal

- Align the client-side constants and onboarding/help copy with the final infrastructure contract
- Prefer `nextcloud.<base-domain>` as the default host to match the current app architecture and naming
- Make the redirect URI contract explicit in app docs so infra changes do not silently break native sign-in

## Acceptance criteria

- `weave` and `weave-inf` agree on one redirect URI set for the native app
- `weave` and `weave-inf` agree on one browser-facing Nextcloud hostname convention
- onboarding/help text reflects the final redirect and logout URIs
- app configuration docs mention the backend base URL separately instead of overloading the current issuer-derived defaults
