import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useNavigate } from "@tanstack/react-router";
import type { ReactNode } from "react";
import { api, call, clientHeader } from "../lib/api";
import { myEventsQuery } from "../lib/queries";
import { endSession, useSession } from "../lib/session";
import { cx } from "./ui";

function RailLink({ to, children, badge }: { to: string; children: ReactNode; badge?: number }) {
  return (
    <Link
      to={to}
      className="flex min-h-11 shrink-0 items-center justify-between gap-3 rounded-md px-2.5 text-sm font-medium text-rail-ink no-underline transition-colors duration-150 hover:bg-rail-line hover:text-white focus-visible:outline-gold"
      activeProps={{ className: "!bg-gold !text-rail font-bold", "aria-current": "page" }}
    >
      {children}
      {badge !== undefined && badge > 0 && <span className="text-[13px] font-semibold">{badge}</span>}
    </Link>
  );
}

function GroupLabel({ children }: { children: ReactNode }) {
  return <div className="hidden px-2.5 pt-4.5 pb-1.5 text-xs font-semibold text-rail-muted md:block">{children}</div>;
}

/** The green rail on desktop; a top bar with the same links on narrow screens. */
export function Shell({ children }: { children: ReactNode }) {
  const session = useSession();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const upcoming = useQuery({ ...myEventsQuery("UPCOMING"), enabled: !!session });

  async function signOut() {
    await call(api.POST("/api/v1/auth/signout", { headers: clientHeader })).catch(() => undefined);
    endSession("signed-out");
    queryClient.clear();
    await navigate({ to: "/sign-in" });
  }

  if (!session) {
    return null;
  }
  const { user, isAdmin } = session;

  return (
    <div className="min-h-dvh md:grid md:grid-cols-[232px_minmax(0,1fr)]">
      <a
        href="#main"
        className="sr-only z-50 rounded-md bg-gold px-3 py-2 font-bold text-rail focus:not-sr-only focus:fixed focus:top-2 focus:left-2"
      >
        Skip to content
      </a>
      <div className="bg-rail">
      <nav
        aria-label="Main"
        className="flex flex-wrap items-center gap-1 px-3 py-2 text-rail-ink md:sticky md:top-0 md:h-dvh md:flex-col md:flex-nowrap md:items-stretch md:gap-0.5 md:px-4 md:py-6"
      >
        <Link
          to="/"
          className="order-first mr-2 shrink-0 px-2.5 text-[21px] font-extrabold text-white no-underline md:order-none md:mr-0 md:mb-3.5 md:border-b md:border-rail-line md:pb-5.5"
        >
          Registrar
        </Link>
        {isAdmin && (
          <>
            <GroupLabel>Admin</GroupLabel>
            <RailLink to="/admin/dashboard">Dashboard</RailLink>
            <RailLink to="/admin/events">Events</RailLink>
            <RailLink to="/admin/people">People</RailLink>
            <GroupLabel>Member</GroupLabel>
          </>
        )}
        <RailLink to="/browse">Browse</RailLink>
        <RailLink to="/my-events" badge={upcoming.data?.length}>
          My events
        </RailLink>
        <RailLink to="/profile">Profile</RailLink>
        <div className="order-first ml-auto flex shrink-0 items-center gap-3 md:order-none md:mt-auto md:ml-0 md:flex-col md:items-stretch md:gap-2 md:border-t md:border-rail-line md:px-2.5 md:pt-3.5">
          <div className="hidden text-[13px] md:block">
            <span className="block font-bold text-white">{user.name}</span>
            {isAdmin ? "Admin" : "Member"}
          </div>
          <button
            type="button"
            onClick={signOut}
            className={cx(
              "min-h-11 rounded-md border border-rail-line px-3 text-left text-sm font-semibold text-rail-ink",
              "transition-colors duration-150 hover:bg-rail-line hover:text-white focus-visible:outline-gold",
            )}
          >
            Sign out
          </button>
        </div>
        <span aria-hidden="true" className="order-first basis-full md:hidden" />
      </nav>
      </div>
      <main id="main" tabIndex={-1} className="flex min-w-0 flex-col gap-5 px-4 pt-6 pb-10 outline-none md:px-8 md:pt-7">
        {children}
      </main>
    </div>
  );
}
