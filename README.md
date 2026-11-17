# Registrar

Event registration on Spring Boot and MongoDB. Admins create events and add people to them;
members sign up, browse open events and register.

It's a companion to the [kitchensink migration](https://github.com/Mighil31/kitchensink-spring-mongodb):
that repo ports the JBoss quickstart faithfully; this one asks what the same idea ("register people")
looks like as a modern product. Separate service, separate database, no shared code.

> Work in progress. Phase 1 (backend skeleton, schema, local stack) is in progress.

## Layout

| Path | What |
|---|---|
| `backend/` | Spring Boot 4 (Java 21), Spring Data MongoDB, Flamingock schema migrations |
| `web/` | React 19 + TypeScript (Vite, TanStack Router and Query, Tailwind), typed from the backend's OpenAPI document |
| `compose.yaml`, `docker/` | Local stack: MongoDB as a single-node replica set with auth, the backend, and the web app behind nginx |

## Running

Prerequisites: Java 21, Maven, Docker.

```bash
# Whole stack in Docker
cp .env.example .env              # then change the passwords
docker compose up --build         # app at http://localhost:8083; API at :8082, probes at /livez and /readyz

# Or just the backend, on a throwaway MongoDB from Testcontainers (with demo data)
REGISTRAR_DEV_SEED=true REGISTRAR_ADMIN_EMAIL=admin@example.com \
  REGISTRAR_ADMIN_PASSWORD=change-me-admin-password mvn -f backend spring-boot:test-run

# The web app in development (proxies /api to the backend on :8082)
cd web && npm ci && npm run dev    # http://localhost:5173

# Build + unit and integration tests
mvn -f backend verify
cd web && npm run typecheck && npm test
```

The API is described by an OpenAPI document at `/v3/api-docs` (springdoc). The web app's types
(`web/src/api/schema.ts`) are generated from it: `mvn -f backend verify` writes `backend/target/openapi.json`,
then `npm run api` in `web/`. CI fails if the committed types no longer match the backend.

`REGISTRAR_DEV_SEED=true` (`DEV_SEED` in `.env`) fills an empty database with demo data: 40 people,
12 events and their registrations. Every seeded person signs in with the password `registrar-demo`
(e.g. `dana.whitfield@registrar.example`, an admin). Never set it in production.

MongoDB runs as a replica set because the app uses multi-document transactions (the last seat of an
event, the last admin). The schema (collections, `$jsonSchema` validators, indexes, a TTL index for
refresh tokens) comes from the migrations in `backend/src/main/java/.../migration/`, applied at startup.
