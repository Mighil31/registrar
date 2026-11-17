import type { Schemas } from "../lib/api";
import { fill } from "../lib/format";
import { Stamp } from "./ui";

type EventLike = Pick<Schemas["EventResponse"], "mode" | "status" | "registeredCount" | "capacity" | "startsAt">;

/** Access: who can join. Never mixed with availability, so a full private event still says PRIVATE. */
export function AccessStamp({ event }: { event: Pick<EventLike, "mode"> }) {
  return event.mode === "PRIVATE" ? <Stamp kind="private">PRIVATE</Stamp> : <Stamp kind="open">OPEN</Stamp>;
}

/** Availability: what stops anyone joining right now, or nothing. */
export function AvailabilityStamp({ event }: { event: EventLike }) {
  if (event.status === "CANCELLED") {
    return <Stamp kind="cancelled">CANCELLED</Stamp>;
  }
  if (new Date(event.startsAt).getTime() <= Date.now()) {
    return <Stamp kind="closed">STARTED</Stamp>;
  }
  if (fill(event.registeredCount, event.capacity) === "full") {
    return <Stamp kind="full">FULL</Stamp>;
  }
  return null;
}

/** The event page's stamps: its access, then its availability. */
export function EventStamps({ event }: { event: EventLike }) {
  return (
    <>
      <AccessStamp event={event} />
      <AvailabilityStamp event={event} />
    </>
  );
}
