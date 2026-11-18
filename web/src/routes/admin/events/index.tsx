import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { Link, createFileRoute, useNavigate } from "@tanstack/react-router";
import { AccessStamp, AvailabilityStamp } from "../../../components/EventStamps";
import { Chips, Empty, LoadError, PageHeader, Pager, Panel, SearchBox, SeatBar, SeatCount, Skeleton, SkeletonRows, Th, buttonClasses, cx } from "../../../components/ui";
import { api, call } from "../../../lib/api";
import { eventCode, when } from "../../../lib/format";
import { keys } from "../../../lib/queries";
import { useUrlSearch } from "../../../lib/search";

type Mode = "OPEN" | "PRIVATE";
type Status = "SCHEDULED" | "CANCELLED";

interface Search {
  q?: string;
  mode?: Mode;
  status?: Status;
  page?: number;
}

export const Route = createFileRoute("/admin/events/")({
  validateSearch: (search: Record<string, unknown>): Search => ({
    q: typeof search.q === "string" && search.q ? search.q : undefined,
    mode: search.mode === "OPEN" || search.mode === "PRIVATE" ? search.mode : undefined,
    status: search.status === "SCHEDULED" || search.status === "CANCELLED" ? search.status : undefined,
    page: typeof search.page === "number" && search.page > 0 ? search.page : undefined,
  }),
  component: AdminEvents,
});

const PAGE_SIZE = 20;

function AdminEvents() {
  const search = Route.useSearch();
  const navigate = useNavigate({ from: Route.fullPath });
  const [q, setQ] = useUrlSearch(search.q, (q) => void navigate({ search: (prev) => ({ ...prev, q, page: undefined }), replace: true }));

  const events = useQuery({
    queryKey: [...keys.events, "all", search],
    queryFn: () =>
      call(
        api.GET("/api/v1/events", {
          params: {
            query: { scope: "ALL", q: search.q, mode: search.mode, status: search.status, page: search.page ?? 0, size: PAGE_SIZE },
          },
        }),
      ),
    placeholderData: keepPreviousData,
  });
  const filtered = Boolean(search.q || search.mode || search.status);

  return (
    <>
      <PageHeader
        title="Events"
        lead="Every event, open and private, latest first."
        actions={
          <Link to="/admin/events/new" className={buttonClasses("primary")}>
            New event
          </Link>
        }
      />
      <div className="flex flex-wrap items-center gap-2.5">
        <SearchBox value={q} onChange={setQ} label="Search events" placeholder="Search by title" />
        <Chips
          label="Access"
          value={search.mode ?? ""}
          onChange={(mode) => navigate({ search: (prev) => ({ ...prev, mode: (mode || undefined) as Mode | undefined, page: undefined }) })}
          options={[
            { value: "", label: "Any access" },
            { value: "OPEN", label: "Open" },
            { value: "PRIVATE", label: "Private" },
          ]}
        />
        <Chips
          label="Status"
          value={search.status ?? ""}
          onChange={(status) =>
            navigate({ search: (prev) => ({ ...prev, status: (status || undefined) as Status | undefined, page: undefined }) })
          }
          options={[
            { value: "", label: "Any status" },
            { value: "SCHEDULED", label: "Scheduled" },
            { value: "CANCELLED", label: "Cancelled" },
          ]}
        />
      </div>

      {events.isError ? (
        <LoadError error={events.error} retry={() => events.refetch()} />
      ) : (
        <Panel label="Events">
          <ul className="m-0 list-none p-0 md:hidden">
            {events.isPending
              ? [0, 1, 2].map((i) => (
                  <li key={i} className="border-t border-rule p-4 first:border-t-0">
                    <Skeleton className="h-16 w-full" />
                  </li>
                ))
              : events.data.items.map((event) => (
                  <li key={event.id} className="flex flex-col gap-2 border-t border-rule p-4 first:border-t-0">
                    <div className="flex justify-between gap-3 text-[13px]">
                      <span className="font-bold text-rail">{eventCode(event)}</span>
                      <span className="text-muted">{when(event.startsAt)}</span>
                    </div>
                    <div>
                      <Link
                        to="/admin/events/$id"
                        params={{ id: event.id }}
                        className={cx("text-base font-bold no-underline", event.status === "CANCELLED" ? "text-muted line-through" : "text-ink")}
                      >
                        {event.title}
                      </Link>
                      <span className="block text-xs text-faint">
                        {event.format === "ONLINE" ? "Online" : event.location ?? "In person"}
                      </span>
                    </div>
                    <div className="flex flex-wrap items-center gap-2">
                      <AccessStamp event={event} />
                      <AvailabilityStamp event={event} />
                    </div>
                    <div className="flex items-center gap-3">
                      <SeatBar registered={event.registeredCount} capacity={event.capacity} className="flex-1" />
                      <SeatCount registered={event.registeredCount} capacity={event.capacity} />
                    </div>
                  </li>
                ))}
          </ul>
          <div className="hidden overflow-x-auto md:block">
            <table className="w-full min-w-[860px] border-collapse text-sm">
              <thead>
                <tr>
                  <Th>Code</Th>
                  <Th>Event</Th>
                  <Th>When</Th>
                  <Th>Access</Th>
                  <Th align="right">Registered</Th>
                  <Th>How full</Th>
                </tr>
              </thead>
              <tbody>
                {events.isPending ? (
                  <SkeletonRows columns={6} />
                ) : (
                  events.data.items.map((event) => {
                    const past = new Date(event.startsAt).getTime() <= Date.now();
                    return (
                      <tr key={event.id} className={cx("border-t border-rule", (past || event.status === "CANCELLED") && "text-muted")}>
                        <td className="px-4 py-3 text-[13px] font-bold whitespace-nowrap text-rail">{eventCode(event)}</td>
                        <td className="px-4 py-3">
                          <Link
                            to="/admin/events/$id"
                            params={{ id: event.id }}
                            className={cx("font-bold no-underline hover:underline", event.status === "CANCELLED" ? "text-muted line-through" : "text-ink")}
                          >
                            {event.title}
                          </Link>
                          <span className="block text-xs text-faint">
                            {event.format === "ONLINE" ? "Online" : event.location ?? "In person"}
                            {past && " · past"}
                          </span>
                        </td>
                        <td className="px-4 py-3 whitespace-nowrap">{when(event.startsAt)}</td>
                        <td className="px-4 py-3">
                          <AccessStamp event={event} />
                        </td>
                        <td className="px-4 py-3 text-right">
                          <span className="flex items-center justify-end gap-2">
                            <AvailabilityStamp event={event} />
                            <SeatCount registered={event.registeredCount} capacity={event.capacity} />
                          </span>
                        </td>
                        <td className="w-44 px-4 py-3">
                          <SeatBar registered={event.registeredCount} capacity={event.capacity} />
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
          {events.data && events.data.items.length === 0 && (
            <Empty
              title={filtered ? "No events match" : "No events yet"}
              action={
                !filtered && (
                  <Link to="/admin/events/new" className={buttonClasses("primary")}>
                    Create the first one
                  </Link>
                )
              }
            >
              {filtered ? "Clear a filter or shorten the search." : "Create an event, then open it for registration or add people yourself."}
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
