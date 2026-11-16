---
version: 1
slug: "src-routes-admin-dashboard-tsx"
primary_target: "src/routes/admin/dashboard.tsx"
related_targets: ["src/routes/browse.tsx"]
---

# Surface: Registrar app shell (all screens)

Mode: Operate. Audience: admins running events and members registering; demoed live on a shared Zoom screen. Task: see what's filling, what's full and what just changed; act (new event, add people, register, leave).

## Direction contract

THESIS: Every event is a catalogue section with a code; every registration is a stamped copy of an enrolment form. Refuses the grey KPI-card dashboard with one blue accent.

OWN-WORLD: Registrar-green rail #17392B with gold #E9B23A for the current page; ground #F3F4F1; white panels with 1px #D5DBD6. Form-copy tints carry state: canary #FFF1A8 = your registration, pink #F9CFD9 = closed, cancelled or left, pale green #E3EEE7 = added by admin. Stamps: 2px-bordered caps badges (OPEN, CLOSED, PRIVATE, REGISTERED, CANCELLED). Public Sans throughout, tabular figures.

STORY: An admin sees each event's code, registered count and how full it is, checks recent registrations, and adds people from a search. A member browses the catalogue, registers, and keeps a yellow copy of each registration in My events.

FIRST VIEWPORT: Green rail (admin group, member group). Header "Dashboard", date, primary "New event". One line of three plain numbers. Main: events table (code, event, when, access stamp, registered n / cap, percent-full bar: amber near full, red full). Right: "Recent registrations", latest 10 timestamped REGISTERED / ADDED BY ADMIN / LEFT copies, loaded with the page.

FORM: Enrolment Office (registrar course catalogue + carbonless enrolment forms), position 7 of the grounded list in round 1, assigned by the roll. seed:9e69aecc34ed03e7. Signature move: the stamped form copies (OPEN, CLOSED, PRIVATE, REGISTERED, CANCELLED) and the yellow member copy of each registration. Raises: one face throughout (Design Annual); columns aligned across rows (Grid Specimen); row expands in place (Title Wall).

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance

Chosen sketch: .impeccable/mocks/decision/assigned.html (canvas row E has all six screens)

## Unresolved
- Section codes are display-only (tag abbreviation + MMDD); no stored field.
