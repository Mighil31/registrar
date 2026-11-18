# 0002. JWT access tokens with rotating refresh cookies

**Status:** Accepted (2026-11-18)

## Context

Every API call has to say who is calling and what they may do. The client is a single-page app
served from the same origin as the API. Sign-ins should survive a page reload, a stolen token
should stop working, and an admin whose role is revoked should lose admin powers.

## Options

| | Server sessions | Long-lived JWT | Short JWT + rotating refresh token |
|---|---|---|---|
| Per-request check | Session lookup in a store | Signature only | Signature only |
| Revoking access | Instant (delete the session) | Not possible until expiry | Refresh revoked at once; access token runs out within 10 min |
| Stolen token | Valid until the session ends | Valid for its whole life | Access: 10 min. Refresh: rotation detects reuse |
| Complexity | Low; framework support | Low | Highest: rotation, reuse detection, two tokens |
| Fits Spring Security | Session management | Resource server | Resource server for the API, a small token store for refresh |

An external identity provider (Keycloak, Auth0) would do the third option for us; it was out of
scope for a self-contained demo.

## Decision

Short-lived access tokens and rotating refresh tokens:

- **Access token:** a JWT signed by the app (RS256), with the user's id and permissions, valid
  10 minutes. The API checks it as a Spring Security resource server. The SPA keeps it in memory
  only, not in localStorage.
- **Refresh token:** 32 random bytes in a cookie (HttpOnly, Secure, SameSite=Strict, path
  `/api/v1/auth`), valid 14 days. Only its SHA-256 hash is stored, in `refreshTokens` (TTL index).
- **Rotation:** each refresh retires the token and issues the next in the same family, in one
  atomic `findAndModify`, so a token works once.
- **Reuse detection:** a retired token used again more than 10 s later means it was copied; the
  whole family is revoked. Within 10 s it is only refused, because two tabs refreshing together
  is normal.
- Cookie endpoints also require an `X-Requested-With` header (CSRF defence on top of SameSite).

## Consequences

- Most requests need no database lookup for authentication; any instance can check a token.
- **Revocation lag:** after a role is revoked or a user deleted, their refresh tokens stop at
  once, but an access token already issued works for up to 10 minutes. It is limited two ways:
  short expiry, and the dangerous operations (role changes, deleting people) re-read the caller
  from the database inside their transaction, so a stale admin token can't perform them.
- Password change ends every other session; the current one continues on a new refresh token.
- More moving parts than sessions: two tokens, a token collection, rotation and reuse logic, and
  the SPA's refresh-and-retry on 401. Each is covered by integration tests (`AuthFlowIT`,
  `RefreshGraceIT`).
- Signing keys are generated per run in development; production must supply a key pair
  (`REGISTRAR_JWT_PRIVATE_KEY` / `PUBLIC_KEY`).
- Honest note: for one same-origin SPA, server sessions would also have worked. This design was
  chosen for stateless checks, permissions carried in the token, and to show token rotation done
  properly.
