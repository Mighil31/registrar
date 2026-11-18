import type { QueryClient } from "@tanstack/react-query";
import { Link, Outlet, createRootRouteWithContext, redirect, useNavigate, useRouterState } from "@tanstack/react-router";
import { useEffect } from "react";
import { Shell } from "../components/Shell";
import { LoadError, buttonClasses } from "../components/ui";
import { getSession, useSession } from "../lib/session";

const PUBLIC = ["/sign-in", "/sign-up"];

export const Route = createRootRouteWithContext<{ queryClient: QueryClient }>()({
  beforeLoad: ({ location }) => {
    const isPublic = PUBLIC.includes(location.pathname);
    const session = getSession();
    if (!session && !isPublic) {
      throw redirect({ to: "/sign-in", search: { redirect: location.href } });
    }
    if (session && isPublic) {
      throw redirect({ to: "/" });
    }
  },
  component: Root,
  errorComponent: ({ error, reset }) => (
    <Frame>
      <h1 className="m-0 text-3xl font-extrabold">Something went wrong</h1>
      <LoadError error={error} retry={reset} />
    </Frame>
  ),
  // Rendered inside Root's <Outlet />, so already inside the Frame (wrapping it again doubled the
  // rail). The errorComponent replaces Root instead, so it needs its own Frame.
  notFoundComponent: () => (
    <>
      <h1 className="m-0 text-3xl font-extrabold">Not found</h1>
      <p className="m-0 text-muted">There's nothing at this address, or it isn't yours to see.</p>
      <Link to="/" className={buttonClasses("primary")}>
        Go to the start
      </Link>
    </>
  ),
});

function Root() {
  return (
    <Frame>
      <Outlet />
    </Frame>
  );
}

/** Signed-in pages sit in the rail shell; Sign in and Sign up have their own layout. */
function Frame({ children }: { children: React.ReactNode }) {
  const location = useRouterState({ select: (state) => state.location });
  const session = useSession();
  const navigate = useNavigate();
  const isPublic = PUBLIC.includes(location.pathname);

  // The session can end mid-page (refresh refused, signed out elsewhere): back to Sign in, and
  // back here afterwards.
  useEffect(() => {
    if (!session && !isPublic) {
      void navigate({ to: "/sign-in", search: { redirect: location.href } });
    }
  }, [session, isPublic, navigate, location.href]);

  if (isPublic || !session) {
    return <>{children}</>;
  }
  return <Shell>{children}</Shell>;
}
