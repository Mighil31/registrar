import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, createFileRoute, useNavigate } from "@tanstack/react-router";
import { toast } from "sonner";
import { ConfirmDialog } from "../../components/ConfirmDialog";
import { Button, Chips, Empty, LoadError, PageHeader, Pager, Panel, SearchBox, Skeleton, SkeletonRows, Stamp, Th, cx } from "../../components/ui";
import { type Schemas, api, call } from "../../lib/api";
import { count, plural, when } from "../../lib/format";
import { keys } from "../../lib/queries";
import { useUrlSearch } from "../../lib/search";
import { useSession } from "../../lib/session";

type Role = "ADMIN" | "MEMBER";

interface Search {
  q?: string;
  role?: Role;
  page?: number;
  selected?: string;
}

export const Route = createFileRoute("/admin/people")({
  validateSearch: (search: Record<string, unknown>): Search => ({
    q: typeof search.q === "string" && search.q ? search.q : undefined,
    role: search.role === "ADMIN" || search.role === "MEMBER" ? search.role : undefined,
    page: typeof search.page === "number" && search.page > 0 ? search.page : undefined,
    selected: typeof search.selected === "string" && search.selected ? search.selected : undefined,
  }),
  component: People,
});

const PAGE_SIZE = 25;

function People() {
  const search = Route.useSearch();
  const navigate = useNavigate({ from: Route.fullPath });
  const [q, setQ] = useUrlSearch(search.q, (q) => void navigate({ search: (prev) => ({ ...prev, q, page: undefined }), replace: true }));

  const people = useQuery({
    queryKey: [...keys.people, "list", search.q, search.role, search.page],
    queryFn: () =>
      call(api.GET("/api/v1/users", { params: { query: { q: search.q, role: search.role, page: search.page ?? 0, size: PAGE_SIZE } } })),
    placeholderData: keepPreviousData,
  });
  const select = (id: string | undefined) => navigate({ search: (prev) => ({ ...prev, selected: id }), resetScroll: false });

  return (
    <div className="flex flex-wrap items-start gap-4.5">
      <div className="flex min-w-0 flex-[2_1_600px] flex-col gap-4">
        <PageHeader
          title="People"
          lead={people.data ? `${plural(people.data.total, "person", "people")}${search.q || search.role ? " match" : " have signed up"}` : " "}
        />
        <div className="flex flex-wrap items-center gap-2">
          <SearchBox value={q} onChange={setQ} label="Search people" placeholder="Name or email" />
          <Chips
            label="Role"
            value={search.role ?? ""}
            onChange={(role) => navigate({ search: (prev) => ({ ...prev, role: (role || undefined) as Role | undefined, page: undefined }) })}
            options={[
              { value: "", label: "All" },
              { value: "ADMIN", label: "Admins" },
              { value: "MEMBER", label: "Members" },
            ]}
          />
        </div>
        {people.isError ? (
          <LoadError error={people.error} retry={() => people.refetch()} />
        ) : (
          <Panel label="People">
            {/* Phones: the picked person opens right under their name, not below the whole list. */}
            <ul className="m-0 list-none p-0 md:hidden">
              {people.isPending
                ? [0, 1, 2, 3].map((i) => (
                    <li key={i} className="border-t border-rule p-4 first:border-t-0">
                      <Skeleton className="h-10 w-full" />
                    </li>
                  ))
                : people.data.items.map((person) => {
                    const selected = person.id === search.selected;
                    return (
                      <li key={person.id} className={cx("border-t border-rule first:border-t-0", selected && "bg-canary-row")}>
                        <button
                          type="button"
                          aria-expanded={selected}
                          onClick={() => select(selected ? undefined : person.id)}
                          className="flex min-h-14 w-full cursor-pointer items-center gap-3 border-0 bg-transparent px-4 py-2.5 text-left"
                        >
                          <span className="min-w-0 flex-1">
                            <strong className="block text-ink">{person.name}</strong>
                            <span className="block truncate text-[13px] text-muted">{person.email}</span>
                          </span>
                          <RoleStamp roles={person.roles} />
                          <span className="text-right text-[13px] whitespace-nowrap text-muted">{plural(person.eventCount, "event")}</span>
                        </button>
                        {selected && (
                          <div className="px-3 pb-3">
                            <PersonPanel id={person.id} onClose={() => select(undefined)} inline />
                          </div>
                        )}
                      </li>
                    );
                  })}
            </ul>
            <div className="hidden overflow-x-auto md:block">
              <table className="w-full min-w-[620px] border-collapse text-sm">
                <thead>
                  <tr>
                    <Th>Name</Th>
                    <Th>Email</Th>
                    <Th>Role</Th>
                    <Th align="right">Events</Th>
                  </tr>
                </thead>
                <tbody>
                  {people.isPending ? (
                    <SkeletonRows columns={4} rows={8} />
                  ) : (
                    people.data.items.map((person) => {
                      const selected = person.id === search.selected;
                      return (
                        <tr
                          key={person.id}
                          aria-selected={selected}
                          className={cx("border-t border-rule transition-colors duration-150", selected ? "bg-canary-row" : "hover:bg-ground")}
                        >
                          <td className="px-4 py-2">
                            <button
                              type="button"
                              onClick={() => select(selected ? undefined : person.id)}
                              className="min-h-9 cursor-pointer border-0 bg-transparent p-0 text-left font-bold text-ink hover:underline"
                            >
                              {person.name}
                            </button>
                          </td>
                          <td className="px-4 py-2 text-muted">{person.email}</td>
                          <td className="px-4 py-2">
                            <RoleStamp roles={person.roles} />
                          </td>
                          <td className="px-4 py-2 text-right">{count(person.eventCount)}</td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>
            {people.data?.items.length === 0 && <Empty title="Nobody matches">Search by the start of a name or an email.</Empty>}
            {people.data && (
              <Pager
                page={people.data.page}
                size={people.data.size}
                total={people.data.total}
                onPage={(page) => navigate({ search: (prev) => ({ ...prev, page: page || undefined }) })}
              />
            )}
          </Panel>
        )}
      </div>
      {search.selected ? (
        <div className="hidden min-w-0 flex-[1_1_330px] md:flex">
          <PersonPanel key={search.selected} id={search.selected} onClose={() => select(undefined)} />
        </div>
      ) : (
        <aside className="hidden flex-[1_1_330px] rounded-lg border border-dashed border-field px-5 py-6 text-sm text-muted lg:block">
          Pick a name to see their events and change their role.
        </aside>
      )}
    </div>
  );
}

function RoleStamp({ roles }: { roles: Role[] }) {
  return roles.includes("ADMIN") ? <Stamp kind="admin">ADMIN</Stamp> : <Stamp kind="member">MEMBER</Stamp>;
}

function PersonPanel({ id, onClose, inline }: { id: string; onClose: () => void; inline?: boolean }) {
  const session = useSession();
  const queryClient = useQueryClient();
  const person = useQuery({ queryKey: keys.person(id), queryFn: () => call(api.GET("/api/v1/users/{id}", { params: { path: { id } } })) });
  const registrations = useQuery({
    queryKey: [...keys.person(id), "registrations"],
    queryFn: () => call(api.GET("/api/v1/users/{id}/registrations", { params: { path: { id } } })),
  });
  const isSelf = session?.user.id === id;

  const refresh = () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: keys.people }),
      queryClient.invalidateQueries({ queryKey: keys.dashboard }),
      queryClient.invalidateQueries({ queryKey: keys.events }),
    ]);

  const setRoles = useMutation({
    mutationFn: (roles: Role[]) => call(api.PUT("/api/v1/users/{id}/roles", { params: { path: { id } }, body: { roles } })),
    onSuccess: (saved) => {
      queryClient.setQueryData(keys.person(id), saved);
      toast.success(
        saved.roles.includes("ADMIN")
          ? `${saved.name} is an admin. They'll see admin screens after their next sign-in or refresh.`
          : `${saved.name} is a member again and has been signed out everywhere.`,
      );
      return refresh();
    },
  });
  const remove = useMutation({
    mutationFn: () => call(api.DELETE("/api/v1/users/{id}", { params: { path: { id } } })),
    onSuccess: async () => {
      toast.success(`${person.data?.name ?? "They"} deleted. Their seats are free.`);
      onClose();
      queryClient.removeQueries({ queryKey: keys.person(id) });
      await refresh();
    },
  });

  return (
    <aside
      aria-label={person.data?.name ?? "Person"}
      className={cx(
        "flex w-full min-w-0 flex-col gap-4 rounded-lg border border-line bg-panel p-5",
        !inline && "sticky top-6 self-start",
      )}
    >
      <div className={cx("flex items-start justify-between gap-3", inline && "hidden")}>
        {person.data ? (
          <div className="min-w-0">
            <h2 className="m-0 text-[21px] font-extrabold">{person.data.name}</h2>
            <p className="mt-0.5 mb-0 text-sm break-words text-muted">
              {person.data.email}
              <br />
              {person.data.phoneNumber}
            </p>
          </div>
        ) : person.isError ? (
          <LoadError error={person.error} />
        ) : (
          <Skeleton className="h-12 w-56" />
        )}
        <button
          type="button"
          aria-label="Close"
          onClick={onClose}
          className="grid size-11 shrink-0 place-items-center rounded-md border border-field bg-panel text-ink hover:bg-quiet"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" aria-hidden="true">
            <path d="M6 6l12 12M18 6L6 18" />
          </svg>
        </button>
      </div>

      {inline && person.data && (
        <p className="m-0 text-sm break-words text-muted">
          {person.data.email}
          <br />
          {person.data.phoneNumber}
        </p>
      )}
      {inline && person.isError && <LoadError error={person.error} />}
      {person.data && (
        <>
          <div className="flex flex-wrap gap-2">
            <RoleStamp roles={person.data.roles} />
            {isSelf && <Stamp kind="registered">YOU</Stamp>}
            <span className="text-sm text-muted">Joined {when(person.data.createdAt).split(" · ")[0]}</span>
          </div>
          <div className="flex flex-wrap gap-2">
            {person.data.roles.includes("ADMIN") ? (
              <ConfirmDialog
                title={`Revoke admin from ${person.data.name}?`}
                confirmLabel="Revoke admin"
                busy={setRoles.isPending}
                onConfirm={() => setRoles.mutateAsync(["MEMBER"])}
                trigger={
                  <Button variant="secondary" disabled={isSelf}>
                    Revoke admin
                  </Button>
                }
              >
                They become a member and are signed out everywhere. A token they already hold can still open admin screens for
                up to 10 minutes, but it can't change roles or delete anyone.
              </ConfirmDialog>
            ) : (
              <Button variant="secondary" busy={setRoles.isPending} onClick={() => setRoles.mutate(["MEMBER", "ADMIN"])}>
                Make admin
              </Button>
            )}
            <ConfirmDialog
              title={`Delete ${person.data.name}?`}
              confirmLabel="Delete person"
              typeToConfirm={person.data.name}
              busy={remove.isPending}
              onConfirm={() => remove.mutateAsync()}
              trigger={
                <Button variant="danger" disabled={isSelf}>
                  Delete
                </Button>
              }
            >
              This deletes their account and their {plural(person.data.eventCount, "registration")}, freeing those seats, and
              signs them out. It can't be undone.
            </ConfirmDialog>
          </div>
          <p className="m-0 text-[13px] leading-normal text-muted">
            {isSelf
              ? "This is you. Admins can't change their own role or delete themselves; ask another admin."
              : "There must always be at least one admin. You can't change your own role here."}
          </p>
        </>
      )}

      <div>
        <h3 className="m-0 mb-2 text-[15px] font-bold">
          Registered for{registrations.data ? ` (${registrations.data.length})` : ""}
        </h3>
        {registrations.isPending ? (
          <Skeleton className="h-16 w-full" />
        ) : registrations.isError ? (
          <LoadError error={registrations.error} />
        ) : registrations.data.length === 0 ? (
          <p className="m-0 text-sm text-muted">No events yet.</p>
        ) : (
          <ul className="m-0 list-none p-0">
            {registrations.data.map((r: Schemas["MyRegistration"]) => (
              <li key={r.eventId} className="grid grid-cols-[minmax(0,1fr)_auto] gap-2.5 border-t border-rule py-2 text-sm">
                <Link
                  to="/admin/events/$id"
                  params={{ id: r.eventId }}
                  className={cx("font-semibold text-ink", r.status === "CANCELLED" && "text-muted line-through")}
                >
                  {r.title}
                </Link>
                <span className="text-muted">{when(r.startsAt).split(" · ")[0]}</span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </aside>
  );
}
