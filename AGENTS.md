# Weave Backend Repository Instructions

`weave-backend` is a Spring Boot JWT resource server for the Weave product family. It validates access tokens issued by the Keycloak realm and owns server-side product APIs and orchestration workflows. It must not become a generic proxy for Matrix, Nextcloud, or Keycloak end-user traffic that the Flutter client already handles natively.

Placement rules:
- `config/` for Spring configuration and security setup
- `controller/` for REST endpoints and request/response mapping only — keep controllers thin
- `model/` for response and request DTOs
- `service/` for business logic and orchestration when the domain grows beyond single-class controllers

Ownership boundary:
- The backend validates the OIDC issuer always and audience optionally; do not remove either check
- Do not add endpoints that blindly relay user bearer tokens to Matrix, Nextcloud, or Keycloak
- Backend integrations with downstream services must use service-account credentials or documented token exchange, not forwarded user tokens
- New endpoints belong under the existing `/api/v1/` namespace unless there is a versioning reason to break that

App-config alignment:
- OIDC issuer is configured via `WEAVE_OIDC_ISSUER_URI`; optional audience validation via `WEAVE_OIDC_REQUIRED_AUDIENCE`
- App redirect URIs (for cross-repo reference): `com.massimotter.weave:/oauthredirect` and `com.massimotter.weave:/logout`
- Local development stacks may legitimately use `http://` issuers and service URLs

Validation:
- `./gradlew test`
- `./gradlew check`
