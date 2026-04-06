# Enable local TLS and align public hostnames

## Problem

`weave-inf` still defaults to an HTTP-only local ingress. Note: per the app AGENTS.md, local development stacks may legitimately use `http://` issuers and service URLs; HTTPS remains recommended for production deployments and real device testing.

## Proposal

- Add local TLS termination for the browser-facing stack, ideally through Traefik with a dev CA or mkcert-style certificates
- Align public hostnames with the app contract:
  - `auth.<tenant_domain>`
  - `mas.<tenant_domain>`
  - `matrix.<tenant_domain>`
  - `nextcloud.<tenant_domain>`

## Acceptance criteria

- the stack can be reached over HTTPS with the configured hostnames
- browser-facing URLs (`auth.<tenant_domain>`, `mas.<tenant_domain>`, `matrix.<tenant_domain>`, `nextcloud.<tenant_domain>`) are exported as Terraform outputs or written to the generated install metadata
- ~~Nextcloud uses `nextcloud.<tenant_domain>`~~ (resolved by `weave-inf` Terraform update; requires `terraform apply` to propagate)
- installer output shows the HTTPS URLs that the client and backend should consume
- local bootstrap docs include the trusted certificate/dev CA step needed for mobile or desktop testing
