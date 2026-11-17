import { useQuery } from "@tanstack/react-query";
import { Link, createFileRoute, useNavigate } from "@tanstack/react-router";
import { Button, Empty, LoadError, PageHeader, Skeleton, Stamp, buttonClasses, cx } from "../components/ui";
import { type Schemas } from "../lib/api";
import { count, eventCode, when as formatWhen } from "../lib/format";
import { eventQuery, myEventsQuery } from "../lib/queries";
import { useRegistration } from "../lib/registration";

type When = "UPCOMING" | "PAST";

export const Route = createFileRoute("/my-events")({
  validateSearch: (search: Record<string, unknown>): { when?: When } => (search.when === "PAST" ? { when: "PAST" } : {}),
  component: MyEvents,
});

function MyEvents() {
  const { when = "UPCOMING" } = Route.useSearch();
  const navigate = useNavigate({ from: Route.fullPath });
  const upcoming = useQuery(myEventsQuery("UPCOMING"));
  const past = useQuery(myEventsQuery("PAST"));
  const shown = when === "UPCOMING" ? upcoming : past;

  const tab = (value: When, label: string, n: number | undefined) => (
    <button
      type="button"
      role="tab"
      aria-selected={when === value}
      onClick={() => navigate({ search: value === "PAST" ? { when: "PAST" } : {} })}
      className={cx(
        "h-11 border-0 bg-transparent px-4 text-sm transition-shadow duration-150",
        when === value ? "font-bold text-ink shadow-[inset_0_-3px_0_var(--color-rail)]" : "font-semibold text-muted hover:text-ink",
      )}
    >
      {label}
      {n !== undefined && ` (${n})`}
    </button>
  );

  return (
    <div className="flex max-w-[1180px] flex-col gap-4.5">
      <PageHeader title="My events" lead="Your copy of every registration." />
      <div role="tablist" aria-label="When" className="flex gap-1 border-b border-line">
        {tab("UPCOMING", "Upcoming", upcoming.data?.length)}
        {tab("PAST", "Past", past.data?.length)}
      </div>
      {shown.isError ? (
        <LoadError error={shown.error} retry={() => shown.refetch()} />
      ) : shown.isPending ? (
        <div className="grid grid-cols-[repeat(auto-fill,minmax(min(100%,420px),1fr))] gap-4">
          {[0, 1].map((i) => (
            <Skeleton key={i} className="h-48 rounded" />
          ))}
        </div>
      ) : shown.data.length === 0 ? (
        <div className="rounded-lg border border-line bg-panel">
          {when === "UPCOMING" ? (
            <Empty
              title="You're not registered for anything coming up"
              action={
                <Link to="/browse" className={buttonClasses("primary")}>
                  Browse events
                </Link>
              }
            >
              Register for an open event and your copy appears here. Private events show up once an admin adds you.
            </Empty>
          ) : (
            <Empty title="No past events yet">Events you were registered for move here once they start.</Empty>
          )}
        </div>
      ) : (
        <section
          aria-label={when === "UPCOMING" ? "Upcoming registrations" : "Past registrations"}
          className="grid grid-cols-[repeat(auto-fill,minmax(min(100%,420px),1fr))] gap-4"
        >
          {shown.data.map((registration) => (
            <MemberCopy key={registration.eventId} registration={registration} upcoming={when === "UPCOMING"} />
          ))}
        </section>
      )}
    </div>
  );
}

/** One registration as a carbon copy: canary while it stands, pink once the event is cancelled. */
function MemberCopy({ registration, upcoming }: { registration: Schemas["MyRegistration"]; upcoming: boolean }) {
  const event = useQuery(eventQuery(registration.eventId));
  const { leave } = useRegistration(registration.eventId, registration.title);
  const cancelled = registration.status === "CANCELLED";
  const meta = cancelled ? "text-red-ink" : "text-canary-ink";

  return (
    <article
      className={cx(
        "flex flex-col gap-2.5 rounded border px-5 py-4.5 shadow-[0_1px_2px_rgba(20,32,26,.08)]",
        cancelled ? "border-pink-line bg-pink" : "border-canary-line bg-canary",
      )}
    >
      <div className={cx("flex justify-between gap-3 text-xs font-bold", meta)}>
        <span>{event.data ? eventCode(event.data) : " "}</span>
        <span>REGISTERED {formatWhen(registration.registeredAt).split(" · ")[0]?.toUpperCase()}</span>
      </div>
      <h2 className={cx("m-0 text-[21px] font-extrabold", cancelled && "line-through decoration-2")}>
        <Link to="/events/$id" params={{ id: registration.eventId }} className="text-ink no-underline hover:underline">
          {registration.title}
        </Link>
      </h2>
      <dl className={cx("m-0 grid grid-cols-3 gap-2.5 border-t pt-2.5 text-sm", cancelled ? "border-pink-line" : "border-canary-line")}>
        <div>
          <dt className={cx("text-xs font-semibold", meta)}>When</dt>
          <dd className="m-0">{formatWhen(registration.startsAt)}</dd>
        </div>
        <div>
          <dt className={cx("text-xs font-semibold", meta)}>Where</dt>
          <dd className="m-0">
            {event.data ? (event.data.format === "ONLINE" ? "Online" : event.data.location ?? "In person") : <Skeleton className="mt-1 h-3.5 w-20 bg-black/10" />}
          </dd>
        </div>
        <div>
          <dt className={cx("text-xs font-semibold", meta)}>Going</dt>
          <dd className="m-0">{event.data ? (cancelled ? "—" : count(event.data.registeredCount)) : <Skeleton className="mt-1 h-3.5 w-10 bg-black/10" />}</dd>
        </div>
      </dl>
      <div className="mt-1 flex min-h-11 items-center justify-between gap-2.5">
        {cancelled ? (
          <Stamp kind="cancelled">CANCELLED BY ORGANISERS</Stamp>
        ) : (
          <span className="stamp text-go">{event.data?.mode === "PRIVATE" ? "PRIVATE · REGISTERED" : "REGISTERED"}</span>
        )}
        <div className="flex items-center gap-3">
          {event.data?.meetingLink && upcoming && !cancelled && (
            <a href={event.data.meetingLink} target="_blank" rel="noreferrer" className="text-sm font-semibold">
              Join online
            </a>
          )}
          {upcoming && !cancelled && (
            <Button busy={leave.isPending} onClick={() => leave.mutate()} aria-label={`Leave ${registration.title}`}>
              Leave
            </Button>
          )}
        </div>
      </div>
    </article>
  );
}
