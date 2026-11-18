# 0004. MongoDB runs as a replica set, for transactions

**Status:** Accepted (2026-11-18)

## Context

Several rules change more than one document and must not be left half-done:

- **Taking a seat:** `$inc registeredCount` on the event only if it isn't full, then insert the
  registration (a unique index refuses duplicates). If the insert fails, the seat must not stay counted.
- **Leaving, admin add and remove:** registrations and the event's count change together.
- **Deleting an event or a user:** the document, its registrations, and the affected counts.
- **Editing an event, renaming a user:** the document and the copies in registrations (0003).
- **Role changes and user deletion:** always at least one admin. The caller's own document is
  re-read and written so that two admins revoking each other conflict (write skew otherwise).

MongoDB only allows multi-document transactions on a replica set or a sharded cluster; a
standalone server refuses them.

## Options

| | Single-document atomicity only | Compensating writes | Transactions on a replica set |
|---|---|---|---|
| Data model | Everything that changes together in one document (attendees embedded; rejected in 0003) | Separate collections | Separate collections |
| Partial failure | Not possible | Undo code for every step; a crash between steps leaves bad data | All or nothing |
| Concurrent writers | Atomic per document | Races between the steps | Write conflict; one transaction re-runs |
| Local setup | Standalone | Standalone | Replica set, even with one member |

## Decision

MongoDB always runs as a replica set: one member locally (compose and Testcontainers), three or
more in production (Atlas is a replica set by default). Every multi-document rule runs in a
transaction through `@MongoTransaction`, which is Spring's `@Transactional` plus Spring 7's
`@Retryable`. It re-runs the whole method, up to 20 times with a short jittered delay, only when
MongoDB labels the error `TransientTransactionError` (a write conflict) or
`UnknownTransactionCommitResult` (the commit's reply was lost).

## Consequences

- No half-done writes: a seat is counted only if its registration exists, and copies change with
  their source. `LastSeatIT` and `LastAdminRaceIT` race real concurrent requests against this.
- **Transactional methods must be safe to run twice:** database work only, no emails or calls to
  other services. A re-run after a lost commit reply sees the first run's result (e.g. "already
  registered").
- MongoDB doesn't queue writers: under heavy contention (a rush for one event's last seats) many
  attempts conflict and retry. If retries run out, that request fails. Fine at this scale; a
  very hot event would need a different design (e.g. a queue).
- Single-document rules stay single-document where possible: the capacity check is a condition
  on the event's own `findAndModify`, not a read followed by a write.
- Local setup is more involved: a key file because auth is on, `rs.initiate` in the compose
  healthcheck, `withReplicaSet()` in Testcontainers, and `directConnection=true` for tools such
  as Compass on the host (the member advertises the Docker hostname `mongo`).
- Transactions cost some performance and have limits (60 s by default); every transaction here is
  a handful of small writes.
