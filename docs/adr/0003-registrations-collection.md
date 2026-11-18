# 0003. Registrations are their own collection, with extended references

**Status:** Accepted (2026-11-18)

## Context

People and events are many-to-many: a person registers for many events, and an event has up to
its capacity of people (up to 100,000). The relationship is read from both sides:

- a member's **My events**: their registrations, by date;
- an admin's **attendee list**: an event's registrations, in order;
- the **dashboard**: the latest registrations across all events.

A person must not be registered twice, and an event must not take more people than its capacity.
MongoDB has no joins in the SQL sense, so where the relationship lives decides how each read works.

## Options

| | Attendees embedded in the event | Events embedded in the user | `registrations` collection |
|---|---|---|---|
| Growth | Unbounded array in one document (16 MB limit) | Unbounded array per user | One small document per registration |
| Attendee list | One read | Scan of all users | One indexed query |
| My events | Search inside every event | One read | One indexed query |
| No double registration | `$addToSet` plus care | Same, on the other side | Unique index on `(eventId, userId)` |
| Writes in a rush for seats | Every registration rewrites one growing, hot document | Two documents to change | Insert plus `$inc` on the event's count |

A plain link collection (`eventId`, `userId` only) would need a `$lookup` for every list.

## Decision

A `registrations` collection with one document per person per event, carrying **extended
references**: copies of the fields the lists show.

- `user: {name, email}` for attendee lists; `event: {title, startsAt, status}` for My events.
- Indexes (migration `_0003`): unique `(eventId, userId)`; `(userId, event.startsAt)` for My events;
  `(eventId, registeredAt)` for attendee lists; `registeredAt` descending for the dashboard.
- The event keeps a `registeredCount`, changed only by `$inc` in the same transaction as the
  registration insert or delete, so the capacity check is a single-document condition (see 0004).

## Consequences

- Each list is one indexed query with no join, and documents stay small however big an event gets.
- **Copies can go stale, so every change that affects them rewrites them in the same
  transaction:** an event edit, cancel or un-cancel updates its copies (`Registrations.copyEvent`);
  a name change updates the user's copies (`copyUserName`). Email is copied but can't be changed.
  A registration made during an event edit can't keep the old title: both write the event document,
  so one transaction hits a write conflict and is retried.
- **Known gap:** registering only reads the user, so a name change committing at the same moment
  can leave that one new registration with the old name (write skew). Fix if needed: bump the
  user's `version` during registration, as role changes do for the caller.
- Writes cost more: a big event's edit rewrites one copy per attendee. Fine at this scale; at very
  large scale the copy could be updated asynchronously, accepting brief staleness.
- `registeredCount` is a second copy of a fact (the number of registrations). It's kept right by
  changing both in one transaction, and tests check they match after concurrent registrations
  (`LastSeatIT`).
- Deleting a user or event has to clean up its registrations and fix the counts, also in a
  transaction.
- Only fields the lists need are copied. Anything else (tags, location) is read from the event,
  which is why My events loads each event's details separately; widening the copy is the fix if
  that becomes slow.
