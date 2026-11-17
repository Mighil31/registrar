import { useQuery } from "@tanstack/react-query";
import { Link, createFileRoute } from "@tanstack/react-router";
import Markdown from "react-markdown";
import { AccessStamp, AvailabilityStamp } from "../components/EventStamps";
import { BackLink, Button, LoadError, Panel, SeatBar, Skeleton, Stamp, buttonClasses } from "../components/ui";
import { type Schemas } from "../lib/api";
import { count, eventCode, fill, timeRange } from "../lib/format";
import { eventQuery } from "../lib/queries";
import { useRegistration } from "../lib/registration";
import { useSession } from "../lib/session";

export const Route = createFileRoute("/events/$id")({ component: EventPage });

function EventPage() {
  const { id } = Route.useParams();
  const event = useQuery(eventQuery(id));

  return (
    <div className="flex flex-col gap-4.5">
      <BackLink to="/browse">Browse events</BackLink>
      {event.isError ? (
        <LoadError error={event.error} />
      ) : event.isPending ? (
        <Panel>
          <div className="flex flex-col gap-3 p-6">
            <Skeleton className="h-4 w-24" />
            <Skeleton className="h-8 w-80" />
            <Skeleton className="h-4 w-64" />
          </div>
        </Panel>
      ) : (
        <EventDetail event={event.data} />
      )}
    </div>
  );
}

function EventDetail({ event }: { event: Schemas["EventResponse"] }) {
  const session = useSession();
  const { register, leave, landed, busy } = useRegistration(event.id, event.title);
  const started = new Date(event.startsAt).getTime() <= Date.now();
  const closed = started || event.status === "CANCELLED";
  const full = fill(event.registeredCount, event.capacity) === "full";

  return (
    <>
      <section
        aria-label="Event"
        className={
          event.registered
            ? `${landed ? "copy-tint " : ""}flex flex-wrap items-end justify-between gap-x-8 gap-y-5 rounded-lg border border-canary-line bg-canary-row px-6 py-5`
            : "flex flex-wrap items-end justify-between gap-x-8 gap-y-5 rounded-lg border border-line bg-panel px-6 py-5"
        }
      >
        <div className="flex min-w-0 flex-col gap-2">
          <span className="text-[13px] font-bold text-rail">{eventCode(event)}</span>
          <h1 className="m-0 text-3xl font-extrabold tracking-[-0.02em]">{event.title}</h1>
          <p className="m-0 text-[15px] text-muted">
            {timeRange(event.startsAt, event.endsAt)} · {event.format === "ONLINE" ? "Online" : event.location ?? "In person"}
            {event.tags.length > 0 && ` · ${event.tags.map((t) => `#${t}`).join(" ")}`}
          </p>
          <div className="mt-1 flex flex-wrap gap-2">
            {/* Members only reach open events through Browse, so OPEN says nothing here; PRIVATE does. */}
            {event.mode === "PRIVATE" && <AccessStamp event={event} />}
            <AvailabilityStamp event={event} />
            {event.registered && (
              <Stamp kind="registered" landing={landed}>
                REGISTERED
              </Stamp>
            )}
          </div>
        </div>
        <div className="flex w-full max-w-[320px] flex-col gap-2.5">
          <span className="text-3xl font-extrabold">
            {count(event.registeredCount)}{" "}
            <span className="text-base font-medium text-faint">
              {event.capacity == null ? "registered" : `/ ${count(event.capacity)} registered`}
            </span>
          </span>
          <SeatBar registered={event.registeredCount} capacity={event.capacity} className="h-2.5" />
          <div className="flex flex-wrap gap-2">
            {event.registered ? (
              <Button busy={leave.isPending} disabled={busy || closed} onClick={() => leave.mutate()}>
                Leave event
              </Button>
            ) : (
              !closed &&
              event.mode === "OPEN" &&
              !full && (
                <Button variant="primary" busy={register.isPending} disabled={busy} onClick={() => register.mutate()}>
                  Register
                </Button>
              )
            )}
            {session?.isAdmin && (
              <Link to="/admin/events/$id" params={{ id: event.id }} className={buttonClasses("secondary")}>
                Manage event
              </Link>
            )}
          </div>
          {event.registered && closed && event.status !== "CANCELLED" && (
            <p className="m-0 text-sm text-muted">It has started, so you can't leave it any more.</p>
          )}
        </div>
      </section>

      <div className="flex flex-wrap items-start gap-4.5">
        <Panel title="About this event" className="flex-[2_1_520px]">
          <div className="max-w-[70ch] px-6 py-4 text-[15px] leading-relaxed [&_a]:font-semibold [&_h1]:text-xl [&_h2]:text-lg [&_li]:my-1 [&_p]:my-3">
            {event.description ? <Markdown>{event.description}</Markdown> : <p className="text-muted">No description yet.</p>}
          </div>
        </Panel>
        {event.format === "ONLINE" && (
          <Panel title="Joining online" className="flex-[1_1_300px]">
            <div className="flex flex-col gap-2 px-5 py-4 text-sm">
              {event.meetingLink ? (
                <>
                  <span>The meeting link, for people going:</span>
                  <a href={event.meetingLink} target="_blank" rel="noreferrer" className="font-semibold break-all">
                    {event.meetingLink}
                  </a>
                </>
              ) : (
                <span className="text-muted">The meeting link shows here once you're registered.</span>
              )}
            </div>
          </Panel>
        )}
      </div>
    </>
  );
}
