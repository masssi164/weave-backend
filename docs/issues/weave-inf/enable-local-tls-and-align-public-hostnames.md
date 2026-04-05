# Enable local TLS and align public hostnames

## Problem

The Flutter app requires:

- an HTTPS OIDC issuer
- HTTPS Nextcloud access for the live integration flow

`weave-inf` still defaults to an HTTP ingress on `:8090`, and it exposes Nextcloud on `files.<tenant_domain>` while the app derives `nextcloud.<base-domain>`.

## Proposal

- Add local TLS termination for the browser-facing stack, ideally through Traefik with a dev CA or mkcert-style certificates
- Align public hostnames with the app contract:
  - `auth.<tenant_domain>`
  - `mas.<tenant_domain>`
  - `matrix.<tenant_domain>`
  - `nextcloud.<tenant_domain>`
- Export the final browser-facing URLs as Terraform outputs or generated install metadata for client/backend consumers

## Acceptance criteria

- the stack can be reached over HTTPS with the configured hostnames
- Nextcloud uses the final agreed hostname instead of `files.<tenant_domain>`
- installer output shows the HTTPS URLs that the client and backend should consume
- local bootstrap docs include the trusted certificate/dev CA step needed for mobile or desktop testing
