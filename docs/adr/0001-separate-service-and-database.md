# 0001. Registrar is a separate service with its own database

**Status:** Accepted (2026-11-18)

## Context

The kitchensink port is a faithful migration of the JBoss quickstart to Spring Boot and MongoDB.
Its value is that it behaves like the original: a contract suite checks it against the legacy app.

The new requirement is different: show what the same idea ("register people") looks like as a
modern product, without feature parity. Registrar has a different domain (events, registrations,
accounts with passwords and roles) where the port has one collection of `members` with no login.

The question is where that product lives.

## Options

| | Extend the port (same app, same database) | Separate service, shared database | Separate service, own database |
|---|---|---|---|
| Risk to the port | High: every change can break parity | Medium: schema changes affect both | None: the port stays frozen |
| Schema | `members` has to become accounts with roles | Both apps depend on each other's collections | Each app owns its collections, validators and migrations |
| Deploys and migrations | Together | Separate apps, but migrations must agree | Independent |
| Cost | Least code | Some duplication | Patterns are copied, not shared; two stacks to run |

## Decision

Registrar is a separate Spring Boot service in its own repo, with its own `registrar` database and
a database user that can only reach it. No code is shared with the port.

## Consequences

- The port can't be broken by Registrar work; its contract suite stays meaningful.
- Each service owns its data, so each can change its schema, migrations and deploys alone. At
  scale the two databases can move to separate clusters with a configuration change.
- Some patterns are duplicated (problem-detail errors, security headers, Flamingock setup). A
  shared library would couple the two again, so copying is deliberate.
- A kitchensink member and a Registrar user are different records; nothing links them.
- If the two ever need to talk, it is through the port's REST API, never by reading its database.
  No bridge is built now: it would be a lot of work for little value in this exercise.
- Locally there are two stacks to run (MongoDB on 27017 and 27018, apps on 8081 and 8082).
