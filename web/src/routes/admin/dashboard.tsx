import { useQuery } from "@tanstack/react-query";
import { Link, createFileRoute } from "@tanstack/react-router";
import { format } from "date-fns";
import { AccessStamp, AvailabilityStamp } from "../../components/EventStamps";
import { CopyTag, Empty, LoadError, PageHeader, Panel, SeatBar, SeatCount, Skeleton, SkeletonRows, Th, buttonClasses, cx } from "../../components/ui";
import { type Schemas, api, call } from "../../lib/api";
import { count, eventCode, fill, stamp, when } from "../../lib/format";
import { keys } from "../../lib/queries";

export const Route = createFileRoute("/admin/dashboard")({ component: Dashboard });

function Dashboard() {
  const dashboard = useQuery({ queryKey: keys.dashboard, queryFn: () => call(api.GET("/api/v1/dashboard")) });
  const data = dashboard.data;

  return (
    <>
      <PageHeader
        title="Dashboard"
        lead={format(new Date(), "EEEE d MMMM")}
        actions={
          <Link to="/admin/events/new" className={buttonClasses("primary")}>
            New event
          </Link>
        }
      />

      {dashboard.isError ? (
        <LoadError error={dashboard.error} retry={() => dashboard.refetch()} />
      ) : (
        <>
          <dl aria-label="Key numbers" className="m-0 flex flex-wrap gap-x-10 gap-y-2.5 border-b border-line pb-3.5 text-[15px] text-muted">
            <KeyNumber value={data?.members} label="members" />
            <KeyNumber value={data?.upcomingEvents} label="upcoming events" />
            <KeyNumber value={data?.registrationsThisWeek} label="registrations this week" />
          </dl>

          <div className="flex flex-wrap items-start gap-x-6 gap-y-5">
            <Panel
              title="Upcoming events"
              label="Upcoming events"
              className="flex-[2_1_640px] overflow-hidden"
              action={
                <Link to="/admin/events" className="text-sm font-semibold">
                  All events
                </Link>
              }
            >
              <ul className="m-0 list-none p-0 md:hidden">
                {data
                  ? data.events.map((row) => <EventEntry key={row.id} row={row} />)
                  : [0, 1, 2].map((i) => (
                      <li key={i} className="border-t border-rule p-4 first:border-t-0">
                        <Skeleton className="h-16 w-full" />
                      </li>
                    ))}
              </ul>
              <div className="hidden overflow-x-auto md:block">
                <table className="w-full min-w-[680px] border-collapse text-sm">
                  <thead>
                    <tr>
                      <Th>Code</Th>
                      <Th>Event</Th>
                      <Th>When</Th>
                      <Th>Access</Th>
                      <Th>Registered · how full</Th>
                    </tr>
                  </thead>
                  <tbody>
                    {data ? data.events.map((row) => <EventRow key={row.id} row={row} />) : <SkeletonRows columns={5} />}
                  </tbody>
                </table>
              </div>
              {data && data.events.length === 0 && (
                <Empty
                  title="No upcoming events"
                  action={
                    <Link to="/admin/events/new" className={buttonClasses("primary")}>
                      Create one
                    </Link>
                  }
                >
                  Scheduled events that haven't started show here, soonest first, with how full they are.
                </Empty>
              )}
            </Panel>

            <Panel title="Recent registrations" label="Recent registrations" className="flex-[1_1_300px] xl:max-w-[380px]">
              {data ? (
                data.recentRegistrations.length === 0 ? (
                  <Empty title="No registrations yet">They appear here as members register and admins add people.</Empty>
                ) : (
                  <ol className="m-0 list-none p-0">
                    {data.recentRegistrations.map((r) => (
                      <RecentEntry key={`${r.eventId}-${r.userId}`} entry={r} />
                    ))}
                  </ol>
                )
              ) : (
                <div className="flex flex-col gap-4 p-5">
                  {[0, 1, 2, 3].map((i) => (
                    <Skeleton key={i} className="h-9 w-full" />
                  ))}
                </div>
              )}
            </Panel>
          </div>
        </>
      )}
    </>
  );
}

function KeyNumber({ value, label }: { value: number | undefined; label: string }) {
  return (
    <div className="flex items-baseline gap-1.5">
      <dd className="m-0 order-first text-[26px] font-extrabold text-ink">
        {value === undefined ? <Skeleton className="inline-block h-6 w-12 align-middle" /> : count(value)}
      </dd>
      <dt>{label}</dt>
    </div>
  );
}

function EventRow({ row }: { row: Schemas["EventRow"] }) {
  const state = fill(row.registeredCount, row.capacity);
  return (
    <tr className={cx("border-t border-rule", state === "full" && "bg-pink-row")}>
      <td className="px-4 py-3 text-[13px] font-bold whitespace-nowrap text-rail">{eventCode(row)}</td>
      <td className="min-w-48 px-4 py-3">
        <Link to="/admin/events/$id" params={{ id: row.id }} className="font-bold text-ink no-underline hover:underline">
          {row.title}
        </Link>
        <span className="block text-xs text-faint">
          {row.format === "ONLINE" ? "Online" : `${row.location ?? "Room to be announced"} · in person`}
        </span>
      </td>
      <td className="px-4 py-3 whitespace-nowrap">{when(row.startsAt)}</td>
      <td className="px-4 py-3">
        <AccessStamp event={row} />
      </td>
      <td className="w-40 px-4 py-3">
        <div className="flex flex-col gap-1.5">
          <span className="flex items-center justify-between gap-2">
            <SeatCount registered={row.registeredCount} capacity={row.capacity} />
            <AvailabilityStamp event={{ ...row, status: "SCHEDULED" }} />
          </span>
          <SeatBar registered={row.registeredCount} capacity={row.capacity} />
        </div>
      </td>
    </tr>
  );
}

/** A phone-width row: everything the table row says, stacked. */
function EventEntry({ row }: { row: Schemas["EventRow"] }) {
  return (
    <li className={cx("flex flex-col gap-2 border-t border-rule p-4 first:border-t-0", fill(row.registeredCount, row.capacity) === "full" && "bg-pink-row")}>
      <div className="flex justify-between gap-3 text-[13px]">
        <span className="font-bold text-rail">{eventCode(row)}</span>
        <span className="text-muted">{when(row.startsAt)}</span>
      </div>
      <div>
        <Link to="/admin/events/$id" params={{ id: row.id }} className="text-base font-bold text-ink no-underline">
          {row.title}
        </Link>
        <span className="block text-xs text-faint">
          {row.format === "ONLINE" ? "Online" : `${row.location ?? "Room to be announced"} · in person`}
        </span>
      </div>
      <div className="flex flex-wrap items-center gap-2">
        <AccessStamp event={row} />
        <AvailabilityStamp event={{ ...row, status: "SCHEDULED" }} />
      </div>
      <div className="flex items-center gap-3">
        <SeatBar registered={row.registeredCount} capacity={row.capacity} className="flex-1" />
        <SeatCount registered={row.registeredCount} capacity={row.capacity} />
      </div>
    </li>
  );
}

function RecentEntry({ entry }: { entry: Schemas["RecentRegistration"] }) {
  return (
    <li className="grid grid-cols-[64px_minmax(0,1fr)] gap-2.5 border-b border-rule px-4.5 py-2.5 text-[13px] leading-snug last:border-b-0">
      <time dateTime={entry.registeredAt} className="font-bold text-faint">
        {stamp(entry.registeredAt)}
      </time>
      <div>
        {entry.addedByAdmin ? <CopyTag tone="admin">ADDED BY ADMIN</CopyTag> : <CopyTag tone="self">REGISTERED</CopyTag>}
        <span className="mt-0.5 block">
          <strong>{entry.userName}</strong> ·{" "}
          <Link to="/admin/events/$id" params={{ id: entry.eventId }} className="text-ink">
            {entry.eventTitle}
          </Link>
        </span>
      </div>
    </li>
  );
}
