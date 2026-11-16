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
| `web/` | React app (not built yet) |
| `compose.yaml`, `docker/` | Local stack: MongoDB as a single-node replica set with auth, plus the backend |

## Running

Prerequisites: Java 21, Maven, Docker.

```bash
# Whole stack in Docker
cp .env.example .env              # then change the passwords
docker compose up --build         # http://localhost:8082, probes at /livez and /readyz

# Or just the backend, on a throwaway MongoDB from Testcontainers
mvn -f backend spring-boot:test-run

# Build + unit and integration tests
mvn -f backend verify
```

MongoDB runs as a replica set because the app uses multi-document transactions (the last seat of an
event, the last admin). The schema (collections, `$jsonSchema` validators, indexes, a TTL index for
refresh tokens) comes from the migrations in `backend/src/main/java/.../migration/`, applied at startup.
