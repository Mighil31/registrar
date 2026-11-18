# Registrar

Event registration on Spring Boot and MongoDB. Admins create events and add people to them;
members sign up, browse open events and register.

It's a companion to the [kitchensink migration](https://github.com/Mighil31/kitchensink-spring-mongodb):
that repo ports the JBoss quickstart faithfully; this one asks what the same idea ("register people")
looks like as a modern product. Separate service, separate database, no shared code.

## What it does

- **Members** sign up, browse open events, register and leave, and see their events (upcoming and past).
- **Admins** create, edit, cancel and delete events; add people to any event (past capacity, with a warning,
  and marked on the registration); manage people and admin roles; see a dashboard.
- Rules that hold under concurrency, each in a MongoDB transaction: an event never sells more seats than it has
  (members), and there is always at least one admin.

## Layout

| Path | What |
|---|---|
| `backend/` | Spring Boot 4 (Java 21), Spring Data MongoDB, Spring Security (self-issued JWTs, rotating refresh cookie), Flamingock schema migrations |
| `web/` | React 19 + TypeScript (Vite, TanStack Router and Query, Tailwind), typed from the backend's OpenAPI document |
| `compose.yaml`, `docker/` | Local stack: MongoDB as a single-node replica set with auth, the backend, and the web app behind nginx |
| `docs/adr/` | Architecture decision records |

Backend packages are by feature (`auth`, `event`, `registration`, `user`, `dashboard`, `me`), with shared
plumbing in `db`, `security`, `web` and `migration`.

## Running

Prerequisites: Java 21, Maven, Node 22, Docker.

```bash
# Whole stack in Docker
cp .env.example .env              # then change the passwords
docker compose up --build         # app at http://localhost:8083; API at :8082, probes at /livez and /readyz

# Or just the backend, on a throwaway MongoDB from Testcontainers (with demo data)
REGISTRAR_DEV_SEED=true REGISTRAR_ADMIN_EMAIL=admin@example.com \
  REGISTRAR_ADMIN_PASSWORD=change-me-admin-password mvn -f backend spring-boot:test-run

# The web app in development (proxies /api to the backend on :8082)
cd web && npm ci && npm run dev    # http://localhost:5173
```

`DEV_SEED=true` in `.env` (`REGISTRAR_DEV_SEED`) fills an empty database with demo data: 40 people,
12 events and their registrations. Every seeded person signs in with the password `registrar-demo`
(e.g. `dana.whitfield@registrar.example`, an admin). Never set it in production.

## Tests

```bash
mvn -f backend verify                          # unit + integration tests (Testcontainers), formatting, coverage gate
cd web && npm run typecheck && npm test        # TypeScript + unit tests
cd web && npm run e2e                          # browser journeys (Playwright), against the stack at :8083
```

- **Integration tests** run the real app against a real MongoDB replica set and call it over HTTP.
- **Authorization matrix** (`AuthorizationMatrixIT`): every endpoint × {anonymous, member, admin} → 401 / 403 / 2xx,
  with valid requests so a 403 can only come from the permission check. It also fails if an endpoint in the
  OpenAPI document has no row.
- **Race tests**: `LastSeatIT` (members racing for the last seat) and `LastAdminRaceIT` (two admins revoking each
  other at once; it fails if the transaction stops locking the caller's own document).
- **Browser journeys** (`web/e2e/`): a new member signs up and registers; an admin adds people past capacity;
  admin roles. They need the stack running with `DEV_SEED=true`, and make and delete their own data.

## API

Versioned under `/api/v1`, errors as RFC 9457 problem details. The OpenAPI document is at `/v3/api-docs`
(springdoc). The web app's types (`web/src/api/schema.ts`) are generated from it: `mvn -f backend verify`
writes `backend/target/openapi.json`, then `npm run api` in `web/`. CI fails if the committed types no longer
match the backend.

Endpoints check permissions (`events:manage`, `users:read`, ...), not role names; roles map to permissions in
one place (`Permissions`).

## MongoDB

MongoDB runs as a replica set because the app uses multi-document transactions (the last seat of an
event, the last admin). Registrations are their own collection, with copies of the event and person
fields that lists need. The schema (collections, `$jsonSchema` validators, indexes, a TTL index for
refresh tokens) comes from the migrations in `backend/src/main/java/.../migration/`, applied at startup.
The admin dashboard is one aggregation (`$unionWith` and `$facet`).
