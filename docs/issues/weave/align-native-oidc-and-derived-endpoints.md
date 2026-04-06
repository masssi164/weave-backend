# Align native OIDC registration and derived endpoints

> **Status: Resolved in Terraform source** — `weave-inf` now declares the correct native redirect URIs and `nextcloud.<base-domain>`. A `terraform apply` in `weave-workspace/02-keycloak-setup` is required to propagate the changes to a running Keycloak instance.

## Problem

`weave` expected:

- OIDC redirect: `com.massimotter.weave:/oauthredirect`
- OIDC post-logout redirect: `com.massimotter.weave:/logout`
- derived Nextcloud URL: `https://nextcloud.<base-domain>`

The original `weave-inf` setup had registered `weaveapp://login/callback` for the Keycloak client and exposed Nextcloud on `files.<tenant_domain>`, so the default infrastructure contract did not match the client code.

## Resolution

`weave-inf` was updated to register `com.massimotter.weave:/oauthredirect` and `com.massimotter.weave:/logout` for the `weave-app` Keycloak client and to expose Nextcloud on `nextcloud.<base-domain>`, bringing the infrastructure contract into alignment with the client expectations.

## Proposal

- Align the client-side constants and onboarding/help copy with the final infrastructure contract
- Prefer `nextcloud.<base-domain>` as the default host to match the current app architecture and naming
- Make the redirect URI contract explicit in app docs so infra changes do not silently break native sign-in

## Acceptance criteria

- `weave` and `weave-inf` agree on one redirect URI set for the native app
- `weave` and `weave-inf` agree on one browser-facing Nextcloud hostname convention
- onboarding/help text reflects the final redirect and logout URIs
- app configuration docs mention the backend base URL separately instead of overloading the current issuer-derived defaults
