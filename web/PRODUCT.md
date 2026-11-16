# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Stack

React 19 + TypeScript on Vite, TanStack Router / Query / Table, shadcn/ui (Radix + Tailwind v4), react-hook-form + zod, Recharts. Served as a static build by nginx on the same origin as the Spring Boot API (`registrar/`, Java 21, MongoDB). Not built yet.

## Users

- **Members**: anyone who signs up (name, email, password, phone). They browse open events, register for them, see their upcoming and past events, and leave events before they start.
- **Admins**: members granted admin by another admin. They create, edit, cancel and delete events; add people to events by searching the user pool and remove them; list users, filter to admins, grant and revoke admin, delete users; see what's filling up and recent registrations on a dashboard.
- **Evaluators**: a MongoDB interview panel watching a live demo, next to the faithful port of the legacy kitchensink app.

## Product Purpose

Registrar is a modern, product-level take on the JBoss kitchensink quickstart. Kitchensink registers people; Registrar registers people for things. It exists to show what "modernize the product" looks like, beside the port that shows "migrate the code". Success: a demo where both roles' journeys run end to end, live, and every rule (capacity, last admin, private events) holds up under questions.

## Positioning

The faithful port and the product sit side by side in one repo: same spirit (registering people), separate service and database, no shared code. Registrar is the "after" to the port's "before".

## Operating Context

- Whole app is behind a login screen; "New here? Sign up" underneath.
- Events are **Open** (listed in Browse; members join themselves; admins can also add) or **Private** (never listed; only admins add people; a member sees one only once added).
- Members see an event's attendee **count only**; admins see names.
- Capacity is optional; members can't exceed it; admins can, with a warning.
- Events are cancelled (members see "Cancelled"), not silently removed; admins can also hard-delete.
- Vocabulary: **sign up** = create an account; **register** = join an event.
- Demoed on a shared Zoom screen to a panel.

## Capabilities and Constraints

- Event fields: title, description (markdown), format (in person + location, or online + meeting link shown only to attendees), start/end, optional capacity, Open/Private, up to 5 tags, status Scheduled/Cancelled.
- Dashboard numbers from one aggregation; recent registrations load with the page (no live feed).
- Roles: Member, Admin. Always at least one admin; admins can't change their own role or delete themselves.
- Out of scope (decided): waitlist, invites/email, cover images, registration deadlines, fuzzy search, import from the port, live dashboard feed, audit log, sessions list, disabling users, deleting your own account, login rate limiting, dark mode, Spanish/French.
- Language: English only.

## Evidence on Hand

None. No real users, customers, testimonials, usage numbers or press. All screens use clearly fictional sample data; never present numbers as real metrics.

## Product Principles

1. Both roles get a complete product, not an admin console with a token member view.
2. Every rule visible in the UI is enforced on the server and explainable in one sentence.
3. Personal data is shown only to those who need it (counts for members, names for admins).
4. Port and product stay separate: the contrast is the point.

## Accessibility & Inclusion

WCAG 2.2 AA: keyboard access everywhere, visible focus, 4.5:1 text contrast, no colour-only meaning, 44 px touch targets. Light theme only.
