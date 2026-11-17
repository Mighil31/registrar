import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { Link, createFileRoute, useNavigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { Button, Chips, Empty, LoadError, PageHeader, Pager, Panel, SearchBox, SeatBar, SeatCount, Skeleton, SkeletonRows, Stamp, Th, cx } from "../components/ui";
import { type Schemas, api, call } from "../lib/api";
import { eventCode, fill, when } from "../lib/format";
import { keys } from "../lib/queries";
import { useRegistration } from "../lib/registration";

interface Search {
  q?: string;
  tag?: string;
  page?: number;
}

export const Route = createFileRoute("/browse")({
  validateSearch: (search: Record<string, unknown>): Search => ({
    q: typeof search.q === "string" && search.q ? search.q : undefined,
    tag: typeof search.tag === "string" && search.tag ? search.tag : undefined,
    page: typeof search.page === "number" && search.page > 0 ? search.page : undefined,
  }),
  component: Browse,
});

const PAGE_SIZE = 20;

function Browse() {
  const search = Route.useSearch();
  const navigate = useNavigate({ from: Route.fullPath });
  const [q, setQ] = useState(search.q ?? "");

  // Search as you type, a beat after the last key; the URL keeps it (shareable, back works).
  useEffect(() => {
    const id = setTimeout(() => {
      if ((search.q ?? "") !== q) {
        void navigate({ search: (prev) => ({ ...prev, q: q || undefined, page: undefined }), replace: true });
      }
    }, 250);
    return () => clearTimeout(id);
  }, [q, search.q, navigate]);

  const events = useQuery({
    queryKey: [...keys.events, "browse", search],
    queryFn: () =>
      call(
        api.GET("/api/v1/events", {
          params: { query: { scope: "BROWSE", q: search.q, tag: search.tag, page: search.page ?? 0, size: PAGE_SIZE } },
        }),
      ),
    placeholderData: keepPreviousData,
  });

  // Tag chips come from everything open, not from the filtered page, so they don't vanish as you filter.
  const allOpen = useQuery({
    queryKey: [...keys.events, "browse", "tags"],
    queryFn: () => call(api.GET("/api/v1/events", { params: { query: { scope: "BROWSE", size: 100 } } })),
    staleTime: 60_000,
  });
  const tags = [...new Set(allOpen.data?.items.flatMap((e) => e.tags) ?? [])].sort().slice(0, 8);

  return (
    <>
      <PageHeader
        title="Browse events"
        lead="Open events you can register for. Private events appear in My events once an admin adds you."
      />
      <div className="flex flex-wrap items-center gap-2.5">
        <SearchBox value={q} onChange={setQ} label="Search events" placeholder="Search by title" />
        {tags.length > 0 && (
          <Chips
            label="Filter by tag"
            value={search.tag ?? ""}
            onChange={(tag) => navigate({ search: (prev) => ({ ...prev, tag: tag || undefined, page: undefined }) })}
            options={[{ value: "", label: "All" }, ...tags.map((t) => ({ value: t, label: `#${t}` }))]}
          />
        )}
      </div>

      {events.isError ? (
        <LoadError error={events.error} retry={() => events.refetch()} />
      ) : (
        <Panel label="Open events">
          {/* Phones get stacked entries, so Register never hides behind a sideways scroll. */}
          <ul className="m-0 list-none p-0 md:hidden">
            {events.isPending
              ? [0, 1, 2].map((i) => (
                  <li key={i} className="border-t border-rule p-4 first:border-t-0">
                    <Skeleton className="h-20 w-full" />
                  </li>
                ))
              : events.data.items.map((event) => <BrowseCard key={event.id} event={event} />)}
          </ul>
          <div className="hidden overflow-x-auto md:block">
            <table className="w-full min-w-[880px] border-collapse text-sm">
              <thead>
                <tr>
                  <Th>Code</Th>
                  <Th>Event</Th>
                  <Th>When</Th>
                  <Th>How full</Th>
                  <Th align="right">Registered</Th>
                  <Th>
                    <span className="sr-only">Action</span>
                  </Th>
                </tr>
              </thead>
              <tbody>
                {events.isPending ? (
                  <SkeletonRows columns={6} />
                ) : (
                  events.data.items.map((event) => <BrowseRow key={event.id} event={event} />)
                )}
              </tbody>
            </table>
          </div>
          {events.data && events.data.items.length === 0 && (
            <Empty title={search.q || search.tag ? "Nothing matches" : "No open events right now"}>
              {search.q || search.tag
                ? "Try a shorter search or another tag."
                : "When an admin opens an event for registration, it appears here."}
            </Empty>
          )}
          {events.data && (
            <Pager
              page={events.data.page}
              size={events.data.size}
              total={events.data.total}
              onPage={(page) => navigate({ search: (prev) => ({ ...prev, page: page || undefined }) })}
            />
          )}
        </Panel>
      )}
    </>
  );
}

function Action({ event }: { event: Schemas["EventResponse"] }) {
  const { register, leave, landed, busy } = useRegistration(event.id, event.title);
  if (event.registered) {
    return (
      <div className="flex items-center gap-1.5">
        <span className="flex-1 text-center">
          <Stamp kind="registered" landing={landed}>
            REGISTERED
          </Stamp>
        </span>
        <Button busy={leave.isPending} disabled={busy} onClick={() => leave.mutate()} aria-label={`Leave ${event.title}`}>
          Leave
        </Button>
      </div>
    );
  }
  if (fill(event.registeredCount, event.capacity) === "full") {
    return (
      <span className="block text-center">
        <Stamp kind="full">FULL</Stamp>
      </span>
    );
  }
  return (
    <Button
      variant="primary"
      className="w-full"
      busy={register.isPending}
      disabled={busy}
      onClick={() => register.mutate()}
      aria-label={`Register for ${event.title}`}
    >
      Register
    </Button>
  );
}

function where(event: Schemas["EventResponse"]): string {
  const place = event.format === "ONLINE" ? "Online" : event.location ?? "In person";
  return event.tags.length > 0 ? `${place} · ${event.tags.map((t) => `#${t}`).join(" ")}` : place;
}

function BrowseCard({ event }: { event: Schemas["EventResponse"] }) {
  return (
    <li className={cx("flex flex-col gap-2.5 border-t border-rule p-4 first:border-t-0", event.registered && "bg-canary-row")}>
      <div className="flex justify-between gap-3 text-[13px]">
        <span className="font-bold text-rail">{eventCode(event)}</span>
        <span className="text-muted">{when(event.startsAt)}</span>
      </div>
      <div>
        <Link to="/events/$id" params={{ id: event.id }} className="text-base font-bold text-ink no-underline">
          {event.title}
        </Link>
        <span className="block text-xs text-faint">{where(event)}</span>
      </div>
      <div className="flex items-center gap-3">
        <SeatBar registered={event.registeredCount} capacity={event.capacity} className="flex-1" />
        <SeatCount registered={event.registeredCount} capacity={event.capacity} />
      </div>
      <Action event={event} />
    </li>
  );
}

function BrowseRow({ event }: { event: Schemas["EventResponse"] }) {
  return (
    <tr className={cx("border-t border-rule transition-colors duration-200", event.registered && "bg-canary-row")}>
      <td className="px-4 py-3.5 text-[13px] font-bold whitespace-nowrap text-rail">{eventCode(event)}</td>
      <td className="px-4 py-3.5">
        <Link to="/events/$id" params={{ id: event.id }} className="font-bold text-ink no-underline hover:underline">
          {event.title}
        </Link>
        <span className="block text-xs text-faint">{where(event)}</span>
      </td>
      <td className="px-4 py-3.5 whitespace-nowrap">{when(event.startsAt)}</td>
      <td className="w-44 px-4 py-3.5">
        <SeatBar registered={event.registeredCount} capacity={event.capacity} />
      </td>
      <td className="px-4 py-3.5 text-right">
        <SeatCount registered={event.registeredCount} capacity={event.capacity} />
      </td>
      <td className="w-48 px-4 py-2.5">
        <Action event={event} />
      </td>
    </tr>
  );
}
