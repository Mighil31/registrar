import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, createFileRoute, useNavigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { ConfirmDialog } from "../../../components/ConfirmDialog";
import { EventStamps } from "../../../components/EventStamps";
import { BackLink, Button, CopyTag, Empty, LoadError, Panel, SeatBar, Skeleton, SkeletonRows, Th, buttonClasses, cx } from "../../../components/ui";
import { type Schemas, api, call } from "../../../lib/api";
import { count, eventCode, plural, timeRange } from "../../../lib/format";
import { eventQuery, keys } from "../../../lib/queries";

export const Route = createFileRoute("/admin/events/$id/")({ component: AdminEvent });

function useRefreshEvent(id: string) {
  const queryClient = useQueryClient();
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: keys.events }),
      queryClient.invalidateQueries({ queryKey: keys.dashboard }),
      queryClient.invalidateQueries({ queryKey: keys.people }),
      queryClient.invalidateQueries({ queryKey: keys.attendees(id) }),
    ]);
}

function AdminEvent() {
  const { id } = Route.useParams();
  const event = useQuery(eventQuery(id));

  return (
    <div className="flex flex-col gap-4.5">
      <BackLink to="/admin/events">All events</BackLink>
      {event.isError ? (
        <LoadError error={event.error} />
      ) : event.isPending ? (
        <Panel>
          <div className="flex flex-col gap-3 p-6">
            <Skeleton className="h-4 w-24" />
            <Skeleton className="h-8 w-80" />
          </div>
        </Panel>
      ) : (
        <>
          <EventHeader event={event.data} />
          <div className="flex flex-wrap items-start gap-4.5">
            <Attendees event={event.data} />
            {event.data.status === "SCHEDULED" && <AddPeople event={event.data} />}
          </div>
        </>
      )}
    </div>
  );
}

function EventHeader({ event }: { event: Schemas["EventResponse"] }) {
  const navigate = useNavigate();
  const refresh = useRefreshEvent(event.id);
  const queryClient = useQueryClient();
  const cancelled = event.status === "CANCELLED";

  const setStatus = useMutation({
    mutationFn: (to: "cancel" | "uncancel") =>
      call(
        to === "cancel"
          ? api.POST("/api/v1/events/{id}/cancel", { params: { path: { id: event.id } } })
          : api.POST("/api/v1/events/{id}/uncancel", { params: { path: { id: event.id } } }),
      ),
    onSuccess: (saved) => {
      queryClient.setQueryData(keys.event(event.id), saved);
      toast.success(saved.status === "CANCELLED" ? "Cancelled. Attendees see it as cancelled in My events." : "Back on. Everyone registered is still registered.");
      void queryClient.invalidateQueries({ queryKey: keys.myEvents });
      return refresh();
    },
  });
  const remove = useMutation({
    mutationFn: () => call(api.DELETE("/api/v1/events/{id}", { params: { path: { id: event.id } } })),
    onSuccess: async () => {
      queryClient.removeQueries({ queryKey: keys.event(event.id) });
      await refresh();
      toast.success(`${event.title} deleted, with its registrations.`);
      await navigate({ to: "/admin/events" });
    },
  });

  return (
    <section
      aria-label="Event"
      className={cx(
        "flex flex-wrap items-end justify-between gap-x-8 gap-y-5 rounded-lg border px-6 py-5",
        cancelled ? "border-dashed border-red bg-pink-row" : "border-line bg-panel",
      )}
    >
      <div className="flex min-w-0 flex-col gap-2">
        <span className="text-[13px] font-bold text-rail">{eventCode(event)}</span>
        <h1 className={cx("m-0 text-3xl font-extrabold tracking-[-0.02em]", cancelled && "line-through decoration-2")}>{event.title}</h1>
        <p className="m-0 text-[15px] text-muted">
          {timeRange(event.startsAt, event.endsAt)} · {event.format === "ONLINE" ? "Online" : event.location ?? "In person"}
          {event.tags.length > 0 && ` · ${event.tags.map((t) => `#${t}`).join(" ")}`}
        </p>
        <div className="mt-1 flex flex-wrap gap-2">
          <EventStamps event={event} />
        </div>
      </div>
      <div className="flex w-full max-w-[340px] flex-col gap-2.5">
        <span className="text-3xl font-extrabold">
          {count(event.registeredCount)}{" "}
          <span className="text-base font-medium text-faint">
            {event.capacity == null ? "registered, no limit" : `/ ${count(event.capacity)} registered`}
          </span>
        </span>
        <SeatBar registered={event.registeredCount} capacity={event.capacity} className="h-2.5" />
        <div className="flex flex-wrap gap-2">
          <Link to="/admin/events/$id/edit" params={{ id: event.id }} className={buttonClasses("secondary")}>
            Edit
          </Link>
          {cancelled ? (
            <Button variant="secondary" busy={setStatus.isPending} onClick={() => setStatus.mutate("uncancel")}>
              Un-cancel
            </Button>
          ) : (
            <ConfirmDialog
              title={`Cancel ${event.title}?`}
              confirmLabel="Cancel event"
              busy={setStatus.isPending}
              onConfirm={() => setStatus.mutateAsync("cancel")}
              trigger={<Button variant="secondary">Cancel event</Button>}
            >
              {plural(event.registeredCount, "person", "people")} will see it as cancelled in My events. Their registrations
              are kept, so un-cancelling brings everyone back.
            </ConfirmDialog>
          )}
          <ConfirmDialog
            title={`Delete ${event.title}?`}
            confirmLabel="Delete event"
            typeToConfirm={event.title}
            busy={remove.isPending}
            onConfirm={() => remove.mutateAsync()}
            trigger={<Button variant="danger">Delete</Button>}
          >
            This removes the event and all {plural(event.registeredCount, "registration")} for good. To keep a record, cancel it
            instead.
          </ConfirmDialog>
        </div>
      </div>
    </section>
  );
}

function attendeesQuery(id: string) {
  return {
    queryKey: keys.attendees(id),
    queryFn: () => call(api.GET("/api/v1/events/{id}/registrations", { params: { path: { id } } })),
  };
}

function Attendees({ event }: { event: Schemas["EventResponse"] }) {
  const attendees = useQuery(attendeesQuery(event.id));
  const refresh = useRefreshEvent(event.id);
  const queryClient = useQueryClient();
  const remove = useMutation({
    mutationFn: (person: Schemas["Attendee"]) =>
      call(api.DELETE("/api/v1/events/{id}/registrations/{userId}", { params: { path: { id: event.id, userId: person.userId } } })),
    onSuccess: (_, person) => {
      toast.success(`${person.name} removed. Their seat is free.`);
      void queryClient.invalidateQueries({ queryKey: keys.event(event.id) });
      return refresh();
    },
  });

  return (
    <Panel title="Registration list" label="Attendees" className="w-full min-w-0 flex-[2_1_560px] overflow-hidden">
      <ol className="m-0 list-none p-0 md:hidden">
        {attendees.data
          ?.map((a, i) => ({ a, n: i + 1 }))
          .reverse()
          .map(({ a, n }) => (
            <li key={a.userId} className="flex items-center gap-3 border-t border-rule px-4 py-3 first:border-t-0">
              <span className="w-6 font-bold text-faint">{n}</span>
              <span className="min-w-0 flex-1">
                <strong className="block">{a.name}</strong>
                <span className="block truncate text-[13px] text-muted">{a.email}</span>
                <span className="mt-1 flex flex-wrap gap-1.5">
                  {a.addedByAdmin ? <CopyTag tone="admin">ADDED BY ADMIN</CopyTag> : <CopyTag tone="self">SELF</CopyTag>}
                  {a.overCapacity && <CopyTag tone="closed">OVER CAPACITY</CopyTag>}
                </span>
              </span>
              <Button
                busy={remove.isPending && remove.variables?.userId === a.userId}
                onClick={() => remove.mutate(a)}
                aria-label={`Remove ${a.name}`}
              >
                Remove
              </Button>
            </li>
          ))}
      </ol>
      <div className="hidden overflow-x-auto md:block">
        <table className="w-full min-w-[640px] border-collapse text-sm">
          <thead>
            <tr>
              <Th>No.</Th>
              <Th>Name</Th>
              <Th>Email</Th>
              <Th>How</Th>
              <Th>
                <span className="sr-only">Remove</span>
              </Th>
            </tr>
          </thead>
          <tbody>
            {attendees.isPending ? (
              <SkeletonRows columns={5} rows={4} />
            ) : attendees.isError ? null : (
              attendees.data
                .map((a, i) => ({ a, n: i + 1 }))
                .reverse()
                .map(({ a, n }) => (
                  <tr key={a.userId} className="border-t border-rule">
                    <td className="px-4 py-2.5 font-bold text-faint">{n}</td>
                    <td className="px-4 py-2.5 font-semibold">{a.name}</td>
                    <td className="px-4 py-2.5 text-muted">{a.email}</td>
                    <td className="px-4 py-2.5">
                      <span className="flex flex-wrap gap-1.5">
                        {a.addedByAdmin ? <CopyTag tone="admin">ADDED BY ADMIN</CopyTag> : <CopyTag tone="self">SELF</CopyTag>}
                        {a.overCapacity && <CopyTag tone="closed">OVER CAPACITY</CopyTag>}
                      </span>
                    </td>
                    <td className="px-4 py-1.5 text-right">
                      <Button
                        className="h-9"
                        busy={remove.isPending && remove.variables?.userId === a.userId}
                        onClick={() => remove.mutate(a)}
                        aria-label={`Remove ${a.name}`}
                      >
                        Remove
                      </Button>
                    </td>
                  </tr>
                ))
            )}
          </tbody>
        </table>
      </div>
      {attendees.isError && (
        <div className="p-4">
          <LoadError error={attendees.error} retry={() => attendees.refetch()} />
        </div>
      )}
      {attendees.data?.length === 0 && (
        <Empty title="Nobody's registered yet">
          {event.mode === "OPEN"
            ? "Members can register from Browse, or add people yourself."
            : "Private events are invisible to members until you add them."}
        </Empty>
      )}
    </Panel>
  );
}

function AddPeople({ event }: { event: Schemas["EventResponse"] }) {
  const [q, setQ] = useState("");
  const [term, setTerm] = useState("");
  const [picked, setPicked] = useState<Map<string, string>>(new Map());
  const attendees = useQuery(attendeesQuery(event.id));
  const refresh = useRefreshEvent(event.id);
  const queryClient = useQueryClient();

  useEffect(() => {
    const id = setTimeout(() => setTerm(q.trim()), 250);
    return () => clearTimeout(id);
  }, [q]);

  const people = useQuery({
    queryKey: [...keys.people, "pick", term],
    queryFn: () => call(api.GET("/api/v1/users", { params: { query: { q: term, size: 8 } } })),
    enabled: term.length > 0,
  });
  const going = new Set(attendees.data?.map((a) => a.userId));

  const add = useMutation({
    mutationFn: () =>
      call(api.POST("/api/v1/events/{id}/registrations", { params: { path: { id: event.id } }, body: { userIds: [...picked.keys()] } })),
    onSuccess: (result) => {
      const skipped = result.alreadyRegistered.length;
      toast.success(
        `Added ${plural(result.added.length, "person", "people")}${skipped ? `; ${plural(skipped, "was", "were")} already going` : ""}.`,
      );
      setPicked(new Map());
      setQ("");
      void queryClient.invalidateQueries({ queryKey: keys.event(event.id) });
      return refresh();
    },
  });

  const after = event.registeredCount + picked.size;
  const over = event.capacity != null && after > event.capacity;

  function toggle(person: Schemas["PersonResponse"]) {
    setPicked((current) => {
      const next = new Map(current);
      if (next.has(person.id)) {
        next.delete(person.id);
      } else {
        next.set(person.id, person.name);
      }
      return next;
    });
  }

  return (
    <section aria-label="Add people" className="flex w-full min-w-0 flex-[1_1_330px] flex-col gap-3 rounded-lg border-2 border-rail bg-panel px-4.5 py-4">
      <h2 className="m-0 text-base font-bold">Add people</h2>
      <label className="flex flex-col gap-1.5 text-sm font-semibold">
        Search members by name or email
        <input
          type="search"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          className="h-11 rounded-md border border-field px-3 text-base font-normal"
        />
      </label>

      {term && (
        <div className="flex flex-col gap-1.5" aria-live="polite">
          {people.isPending ? (
            <Skeleton className="h-14 w-full" />
          ) : people.data?.items.length === 0 ? (
            <p className="m-0 text-sm text-muted">Nobody matches “{term}”.</p>
          ) : (
            people.data?.items.map((person) => {
              const isGoing = going.has(person.id);
              return (
                <label
                  key={person.id}
                  className={cx(
                    "flex items-center gap-2.5 rounded-md border px-3 py-2.5 text-sm",
                    isGoing ? "border-rule bg-quiet text-muted" : "cursor-pointer border-line hover:border-rail",
                    picked.has(person.id) && "border-rail bg-mint",
                  )}
                >
                  <input
                    type="checkbox"
                    className="size-4.5 accent-rail"
                    disabled={isGoing}
                    checked={isGoing || picked.has(person.id)}
                    onChange={() => toggle(person)}
                  />
                  <span className="min-w-0 flex-1">
                    <strong className="block">{person.name}</strong>
                    <span className="block truncate text-[13px] text-muted">{person.email}</span>
                  </span>
                  {isGoing && <span className="text-xs font-bold text-go">GOING</span>}
                </label>
              );
            })
          )}
        </div>
      )}

      {picked.size > 0 && (
        <p className="m-0 text-sm">
          <strong>{plural(picked.size, "person", "people")} picked:</strong> {[...picked.values()].join(", ")}
        </p>
      )}
      {over && (
        <div role="alert" className="rounded border-2 border-red bg-pink px-3 py-2.5 text-sm leading-normal text-red-ink">
          <strong>
            {event.registeredCount >= (event.capacity ?? 0)
              ? `This event is full (${count(event.registeredCount)} / ${count(event.capacity ?? 0)}).`
              : `Only ${plural((event.capacity ?? 0) - event.registeredCount, "seat")} left.`}
          </strong>{" "}
          Adding {picked.size === 1 ? "them" : "these people"} makes it {count(after)} / {count(event.capacity ?? 0)}. Admins may go over capacity;
          it's marked on their registration.
        </div>
      )}
      <Button
        variant={over ? "danger-solid" : "primary"}
        disabled={picked.size === 0}
        busy={add.isPending}
        onClick={() => add.mutate()}
      >
        {picked.size === 0 ? "Pick people to add" : over ? "Add over capacity" : `Add ${plural(picked.size, "person", "people")}`}
      </Button>
    </section>
  );
}
