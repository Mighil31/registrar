import { Outlet, createFileRoute, redirect } from "@tanstack/react-router";
import { getSession } from "../../lib/session";

/**
 * Admin screens. This only hides them from members; the API refuses members anyway (403), and
 * that is the check that counts.
 */
export const Route = createFileRoute("/admin")({
  beforeLoad: () => {
    if (!getSession()?.isAdmin) {
      throw redirect({ to: "/browse" });
    }
  },
  component: Outlet,
});
